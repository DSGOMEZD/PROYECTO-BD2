-- ================================================================
-- Objetos programables - Sistema de alquiler de motocicletas (MySQL 8)
--
-- Contenido:
--   0. Catalogos de estados y codigos de error (excepciones)
--   1. Funciones        (8)
--   2. Triggers         (10)
--   3. Procedimientos   (10, tres de ellos con cursores)
--   4. Evento programado (opcional)
--   5. Ejemplos de uso para el backend
--
-- Requisitos:
--   - Ejecutar DESPUES del script de creacion de tablas.
--   - Seleccionar la base de datos antes (USE nombre_de_la_base;).
--   - Ejecutar en MySQL Workbench como script completo (usa DELIMITER).
--
-- Reglas generales:
--   - Los IDs no son AUTO_INCREMENT en el modelo, por eso los procedimientos
--     los generan con MAX()+1 bloqueando la tabla dentro de la transaccion.
--   - Los procedimientos de escritura abren su propia transaccion
--     (START TRANSACTION / COMMIT). El backend NO debe llamarlos dentro de
--     otra transaccion abierta, porque START TRANSACTION la confirmaria.
--   - La contrasena llega ya hasheada desde el backend (bcrypt/argon2).
--     La base de datos nunca hashea ni compara contrasenas.
-- ================================================================


-- ================================================================
-- 0. CATALOGOS
-- ================================================================
-- Estados (alineados con los modelos del frontend):
--   USUARIO.Estado ............ ACTIVO | INACTIVO | BLOQUEADO
--   USUARIO.Rol_usuario ....... ADMIN | ARRENDADOR | ARRENDATARIO
--   USUARIO.Tipo_documento .... CC | CE | TI | PAS
--   MOTOCICLETA ............... DISPONIBLE | ARRENDADA | MANTENIMIENTO | INACTIVA
--   SEGURO.Estado_poliza ...... VIGENTE | VENCIDA | CANCELADA
--   SOLICITUD ................. PENDIENTE | APROBADA | RECHAZADA | CANCELADA
--   CONTRATO .................. PENDIENTE | VIGENTE | FINALIZADO | CANCELADO
--   PAGO.Estado ............... PENDIENTE | PAGADO | VENCIDO | CANCELADO
--   PAGO.Metodo_pago .......... EFECTIVO | TARJETA | TRANSFERENCIA | PSE | NEQUI
--                               | POR_DEFINIR (solo mientras el pago esta pendiente)
--
-- Transiciones permitidas (las hacen cumplir los triggers):
--   CONTRATO : PENDIENTE -> VIGENTE | CANCELADO
--              VIGENTE   -> FINALIZADO | CANCELADO
--   PAGO     : PENDIENTE -> PAGADO | VENCIDO | CANCELADO
--              VENCIDO   -> PAGADO | CANCELADO
--
-- Cascadas automaticas (triggers):
--   USUARIO pasa a INACTIVO/BLOQUEADO -> sus solicitudes PENDIENTES se cancelan
--   SOLICITUD RECHAZADA/CANCELADA     -> su contrato PENDIENTE se cancela
--   CONTRATO CANCELADO                -> sus pagos PENDIENTES/VENCIDOS se cancelan
--   CONTRATO FINALIZADO               -> la moto ARRENDADA vuelve a DISPONIBLE
--   SEGURO deja de estar VIGENTE      -> la moto DISPONIBLE pasa a INACTIVA
--   PAGO pasa a PAGADO                -> fecha_pago se fija en la fecha actual
--   Nuevo INFO_MANTENIMIENTO          -> se actualiza el kilometraje de la moto
--
-- Por que existe el contrato PENDIENTE:
--   SOLICITUD.id_contrato y CONTRATO.Id_solicitud son FK obligatorias
--   (NOT NULL) en ambos sentidos. Una solicitud no puede existir sin
--   contrato ni al reves. Por eso sp_crear_solicitud crea las dos filas
--   juntas: la solicitud PENDIENTE y su contrato PENDIENTE (que guarda las
--   fechas pedidas). Al aprobar, el contrato pasa a VIGENTE; al rechazar o
--   cancelar la solicitud, el trigger pasa el contrato a CANCELADO.
--
-- Codigos de error propios (SIGNAL SQLSTATE '45000', MYSQL_ERRNO):
--   45001 Datos invalidos
--   45002 Usuario inexistente o inactivo
--   45003 Registro duplicado
--   45004 Registro no encontrado
--   45005 Motocicleta no disponible
--   45006 Fechas invalidas
--   45007 Operacion no autorizada para este usuario
--   45008 Transicion o estado invalido
--   45009 Contrato con saldo pendiente
--   45010 Llaves inconsistentes
--
-- El backend puede leer MYSQL_ERRNO (ej. SQLException.getErrorCode() en
-- Java, err.errno en Node) y traducirlo a un HTTP 400/403/404/409.
-- ================================================================


DELIMITER $$

-- ================================================================
-- 1. FUNCIONES
-- ================================================================

-- 1 si el usuario existe y esta ACTIVO, 0 en otro caso.
DROP FUNCTION IF EXISTS `fn_usuario_activo`$$
CREATE FUNCTION `fn_usuario_activo`(p_documento INT)
RETURNS TINYINT
READS SQL DATA
BEGIN
    RETURN EXISTS (
        SELECT 1
          FROM `USUARIO`
         WHERE `Numero_documento` = p_documento
           AND `Estado` = 'ACTIVO'
    );
END$$

-- Nombre completo del usuario, o NULL si no existe.
DROP FUNCTION IF EXISTS `fn_nombre_completo`$$
CREATE FUNCTION `fn_nombre_completo`(p_documento INT)
RETURNS VARCHAR(210)
READS SQL DATA
BEGIN
    RETURN (
        SELECT CONCAT_WS(' ',
                   `Primer_nombre`,
                   NULLIF(TRIM(`Segundo_nombre`), ''),
                   `Primer_apellido`,
                   NULLIF(TRIM(`Segundo_apellido`), ''))
          FROM `USUARIO`
         WHERE `Numero_documento` = p_documento
    );
END$$

-- Dias cobrables entre dos fechas. Recoger y devolver el mismo dia cuenta
-- como 1 dia; del 10 al 12 son 2 dias.
DROP FUNCTION IF EXISTS `fn_dias_alquiler`$$
CREATE FUNCTION `fn_dias_alquiler`(p_fecha_inicio DATE, p_fecha_fin DATE)
RETURNS INT
DETERMINISTIC
NO SQL
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL OR p_fecha_fin < p_fecha_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45006;
    END IF;

    RETURN GREATEST(DATEDIFF(p_fecha_fin, p_fecha_inicio), 1);
END$$

-- Valor del alquiler: Precio_alquiler (por dia) * dias.
DROP FUNCTION IF EXISTS `fn_calcular_valor_alquiler`$$
CREATE FUNCTION `fn_calcular_valor_alquiler`(
    p_placa        CHAR(10),
    p_arrendador   INT,
    p_fecha_inicio DATE,
    p_fecha_fin    DATE
)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_precio INT DEFAULT NULL;

    SET v_precio = (
        SELECT `Precio_alquiler`
          FROM `MOTOCICLETA`
         WHERE `Placa` = p_placa
           AND `Numero_documento` = p_arrendador
    );

    IF v_precio IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no existe.', MYSQL_ERRNO = 45004;
    END IF;

    RETURN v_precio * fn_dias_alquiler(p_fecha_inicio, p_fecha_fin);
END$$

-- 1 si la poliza esta VIGENTE y ya habia iniciado en la fecha dada.
-- Nota: el modelo no tiene fecha de fin de vigencia, por eso solo se
-- valida el estado y la fecha de inicio.
DROP FUNCTION IF EXISTS `fn_seguro_vigente`$$
CREATE FUNCTION `fn_seguro_vigente`(p_id_seguro INT, p_fecha DATE)
RETURNS TINYINT
READS SQL DATA
BEGIN
    RETURN EXISTS (
        SELECT 1
          FROM `SEGURO`
         WHERE `Id_seguro` = p_id_seguro
           AND `Estado_poliza` = 'VIGENTE'
           AND `Fecha_inicio_vigencia` <= p_fecha
    );
END$$

-- 1 si la moto se puede alquilar en el rango de fechas:
--   - existe, su estado es DISPONIBLE o ARRENDADA (arrendada en otras fechas),
--   - su arrendador esta activo,
--   - su seguro esta vigente,
--   - no se cruza con otro contrato VIGENTE (fechas inclusivas).
-- p_contrato_excluido permite ignorar un contrato (el que se esta aprobando).
DROP FUNCTION IF EXISTS `fn_moto_disponible`$$
CREATE FUNCTION `fn_moto_disponible`(
    p_placa             CHAR(10),
    p_arrendador        INT,
    p_fecha_inicio      DATE,
    p_fecha_fin         DATE,
    p_contrato_excluido INT
)
RETURNS TINYINT
READS SQL DATA
BEGIN
    DECLARE v_estado    CHAR(20) DEFAULT NULL;
    DECLARE v_id_seguro INT DEFAULT NULL;

    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL OR p_fecha_fin < p_fecha_inicio THEN
        RETURN 0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `MOTOCICLETA`
                    WHERE `Placa` = p_placa AND `Numero_documento` = p_arrendador) THEN
        RETURN 0;
    END IF;

    SELECT `Estado_motocicleta`, `Id_seguro`
      INTO v_estado, v_id_seguro
      FROM `MOTOCICLETA`
     WHERE `Placa` = p_placa
       AND `Numero_documento` = p_arrendador;

    IF v_estado NOT IN ('DISPONIBLE', 'ARRENDADA') THEN
        RETURN 0;
    END IF;

    IF fn_usuario_activo(p_arrendador) = 0 THEN
        RETURN 0;
    END IF;

    IF fn_seguro_vigente(v_id_seguro, p_fecha_inicio) = 0 THEN
        RETURN 0;
    END IF;

    RETURN NOT EXISTS (
        SELECT 1
          FROM `CONTRATO` c
         WHERE c.`Placa` = p_placa
           AND c.`Numero_documento21` = p_arrendador
           AND c.`Estado_contrato` = 'VIGENTE'
           AND c.`id_contrato` <> IFNULL(p_contrato_excluido, -1)
           AND c.`Fecha_inicio` <= p_fecha_fin
           AND c.`Fecha_fin` >= p_fecha_inicio
    );
END$$

-- Total de pagos en estado PAGADO de un contrato.
DROP FUNCTION IF EXISTS `fn_total_pagado`$$
CREATE FUNCTION `fn_total_pagado`(p_id_contrato INT)
RETURNS INT
READS SQL DATA
BEGIN
    RETURN (
        SELECT COALESCE(SUM(`Monto`), 0)
          FROM `PAGO`
         WHERE `id_contrato` = p_id_contrato
           AND `Estado` = 'PAGADO'
    );
END$$

-- Total pendiente (PENDIENTE o VENCIDO) de un contrato.
DROP FUNCTION IF EXISTS `fn_saldo_pendiente`$$
CREATE FUNCTION `fn_saldo_pendiente`(p_id_contrato INT)
RETURNS INT
READS SQL DATA
BEGIN
    RETURN (
        SELECT COALESCE(SUM(`Monto`), 0)
          FROM `PAGO`
         WHERE `id_contrato` = p_id_contrato
           AND `Estado` IN ('PENDIENTE', 'VENCIDO')
    );
END$$


-- ================================================================
-- 2. TRIGGERS
-- ================================================================

-- 1) Normaliza y valida los datos de un usuario nuevo.
DROP TRIGGER IF EXISTS `trg_usuario_bi`$$
CREATE TRIGGER `trg_usuario_bi`
BEFORE INSERT ON `USUARIO`
FOR EACH ROW
BEGIN
    SET NEW.`Correo`         = LOWER(TRIM(NEW.`Correo`));
    SET NEW.`Tipo_documento` = UPPER(TRIM(NEW.`Tipo_documento`));
    SET NEW.`Estado`         = UPPER(TRIM(NEW.`Estado`));
    SET NEW.`Rol_usuario`    = UPPER(TRIM(NEW.`Rol_usuario`));

    IF NEW.`Tipo_documento` NOT IN ('CC', 'CE', 'TI', 'PAS') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tipo de documento invalido (CC, CE, TI, PAS).', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Numero_documento` <= 0 OR NEW.`Telefono` <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Documento y telefono deben ser numeros positivos.', MYSQL_ERRNO = 45001;
    END IF;

    IF TRIM(NEW.`Primer_nombre`) = '' OR TRIM(NEW.`Primer_apellido`) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El primer nombre y el primer apellido son obligatorios.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Correo` NOT REGEXP '^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El correo no tiene un formato valido.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Estado` NOT IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')
       OR NEW.`Rol_usuario` NOT IN ('ADMIN', 'ARRENDADOR', 'ARRENDATARIO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado o rol de usuario invalido.', MYSQL_ERRNO = 45001;
    END IF;

    IF EXISTS (SELECT 1 FROM `USUARIO` WHERE `Correo` = NEW.`Correo`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El correo ya esta registrado.', MYSQL_ERRNO = 45003;
    END IF;
END$$

-- 2) Si un usuario queda INACTIVO o BLOQUEADO, se cancelan sus solicitudes
--    PENDIENTES, tanto las que hizo como arrendatario como las que recibio
--    como arrendador. (trg_solicitud_au cancela despues sus contratos.)
DROP TRIGGER IF EXISTS `trg_usuario_au`$$
CREATE TRIGGER `trg_usuario_au`
AFTER UPDATE ON `USUARIO`
FOR EACH ROW
BEGIN
    IF NEW.`Estado` IN ('INACTIVO', 'BLOQUEADO') AND OLD.`Estado` = 'ACTIVO' THEN
        UPDATE `SOLICITUD`
           SET `Estado_solicitud` = 'CANCELADA'
         WHERE `Estado_solicitud` = 'PENDIENTE'
           AND (`Numero_documento` = NEW.`Numero_documento`
                OR `Numero_documento2` = NEW.`Numero_documento`);
    END IF;
END$$

-- 3) Si la poliza deja de estar VIGENTE, la moto que la usa y estaba
--    DISPONIBLE pasa a INACTIVA (una moto arrendada termina su contrato).
DROP TRIGGER IF EXISTS `trg_seguro_au`$$
CREATE TRIGGER `trg_seguro_au`
AFTER UPDATE ON `SEGURO`
FOR EACH ROW
BEGIN
    IF OLD.`Estado_poliza` = 'VIGENTE' AND NEW.`Estado_poliza` <> 'VIGENTE' THEN
        UPDATE `MOTOCICLETA`
           SET `Estado_motocicleta` = 'INACTIVA'
         WHERE `Id_seguro` = NEW.`Id_seguro`
           AND `Estado_motocicleta` = 'DISPONIBLE';
    END IF;
END$$

-- 4) Normaliza y valida una motocicleta nueva; placa y seguro unicos.
DROP TRIGGER IF EXISTS `trg_motocicleta_bi`$$
CREATE TRIGGER `trg_motocicleta_bi`
BEFORE INSERT ON `MOTOCICLETA`
FOR EACH ROW
BEGIN
    SET NEW.`Placa`              = UPPER(TRIM(NEW.`Placa`));
    SET NEW.`Estado_motocicleta` = UPPER(TRIM(NEW.`Estado_motocicleta`));

    IF NEW.`Placa` = '' OR TRIM(NEW.`Marca`) = '' OR TRIM(NEW.`modelo`) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Placa, marca y modelo son obligatorios.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Año` < 1950 OR NEW.`Año` > YEAR(CURDATE()) + 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El anio de la motocicleta esta fuera de rango.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Cilindraje` <= 0 OR NEW.`Precio_alquiler` <= 0 OR NEW.`Kilometraje_actual` < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cilindraje, precio o kilometraje invalidos.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Estado_motocicleta` NOT IN ('DISPONIBLE', 'ARRENDADA', 'MANTENIMIENTO', 'INACTIVA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de motocicleta invalido.', MYSQL_ERRNO = 45001;
    END IF;

    -- La PK es (Placa, Numero_documento), pero una placa identifica una sola moto.
    IF EXISTS (SELECT 1 FROM `MOTOCICLETA` WHERE `Placa` = NEW.`Placa`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe una motocicleta con esa placa.', MYSQL_ERRNO = 45003;
    END IF;

    -- Cada poliza cubre una sola motocicleta.
    IF EXISTS (SELECT 1 FROM `MOTOCICLETA` WHERE `Id_seguro` = NEW.`Id_seguro`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El seguro ya esta asociado a otra motocicleta.', MYSQL_ERRNO = 45003;
    END IF;
END$$

-- 5) Completa la fecha de la solicitud y verifica las columnas redundantes.
DROP TRIGGER IF EXISTS `trg_solicitud_bi`$$
CREATE TRIGGER `trg_solicitud_bi`
BEFORE INSERT ON `SOLICITUD`
FOR EACH ROW
BEGIN
    SET NEW.`Fecha_solicitud`   = CURDATE();
    SET NEW.`Estado_solicitud`  = 'PENDIENTE';
    SET NEW.`Placa`             = UPPER(TRIM(NEW.`Placa`));
    SET NEW.`Mensaje_solicitud` = IFNULL(NEW.`Mensaje_solicitud`, '');

    -- Columnas redundantes heredadas del modelo:
    --   Numero_documento  = arrendatario = Numero_documento1  (lado CONTRATO)
    --   Numero_documento2 = arrendador   = Numero_documento11 (lado CONTRATO)
    IF NEW.`Numero_documento1` <> NEW.`Numero_documento`
       OR NEW.`Numero_documento11` <> NEW.`Numero_documento2` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Documentos de la solicitud y del contrato no coinciden.', MYSQL_ERRNO = 45010;
    END IF;

    IF NEW.`Numero_documento` = NEW.`Numero_documento2` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un usuario no puede alquilar su propia motocicleta.', MYSQL_ERRNO = 45007;
    END IF;
END$$

-- 6) Si una solicitud se RECHAZA o se CANCELA, su contrato PENDIENTE
--    se cancela automaticamente.
DROP TRIGGER IF EXISTS `trg_solicitud_au`$$
CREATE TRIGGER `trg_solicitud_au`
AFTER UPDATE ON `SOLICITUD`
FOR EACH ROW
BEGIN
    IF NEW.`Estado_solicitud` IN ('RECHAZADA', 'CANCELADA')
       AND OLD.`Estado_solicitud` = 'PENDIENTE' THEN
        UPDATE `CONTRATO`
           SET `Estado_contrato` = 'CANCELADO'
         WHERE `id_contrato` = NEW.`id_contrato`
           AND `Numero_documento` = NEW.`Numero_documento1`
           AND `Numero_documento1` = NEW.`Numero_documento11`
           AND `Estado_contrato` = 'PENDIENTE';
    END IF;
END$$

-- 7) Controla las transiciones de estado y las fechas del contrato.
DROP TRIGGER IF EXISTS `trg_contrato_bu`$$
CREATE TRIGGER `trg_contrato_bu`
BEFORE UPDATE ON `CONTRATO`
FOR EACH ROW
BEGIN
    IF NEW.`Estado_contrato` <> OLD.`Estado_contrato` AND NOT (
           (OLD.`Estado_contrato` = 'PENDIENTE' AND NEW.`Estado_contrato` IN ('VIGENTE', 'CANCELADO'))
        OR (OLD.`Estado_contrato` = 'VIGENTE'   AND NEW.`Estado_contrato` IN ('FINALIZADO', 'CANCELADO'))
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transicion de estado de contrato no permitida.', MYSQL_ERRNO = 45008;
    END IF;

    IF (NEW.`Fecha_inicio` <> OLD.`Fecha_inicio` OR NEW.`Fecha_fin` <> OLD.`Fecha_fin`)
       AND OLD.`Estado_contrato` <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las fechas solo se modifican en contratos PENDIENTES.', MYSQL_ERRNO = 45008;
    END IF;

    IF NEW.`Fecha_fin` < NEW.`Fecha_inicio` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de fin no puede ser anterior a la de inicio.', MYSQL_ERRNO = 45006;
    END IF;
END$$

-- 8) Efectos de cerrar un contrato:
--    CANCELADO  -> sus pagos PENDIENTES o VENCIDOS se cancelan.
--    FINALIZADO -> la moto ARRENDADA vuelve a DISPONIBLE.
DROP TRIGGER IF EXISTS `trg_contrato_au`$$
CREATE TRIGGER `trg_contrato_au`
AFTER UPDATE ON `CONTRATO`
FOR EACH ROW
BEGIN
    IF NEW.`Estado_contrato` <> OLD.`Estado_contrato` THEN
        IF NEW.`Estado_contrato` = 'CANCELADO' THEN
            UPDATE `PAGO`
               SET `Estado` = 'CANCELADO'
             WHERE `id_contrato` = NEW.`id_contrato`
               AND `Numero_documento` = NEW.`Numero_documento`
               AND `Numero_documento1` = NEW.`Numero_documento1`
               AND `Estado` IN ('PENDIENTE', 'VENCIDO');
        ELSEIF NEW.`Estado_contrato` = 'FINALIZADO' THEN
            UPDATE `MOTOCICLETA`
               SET `Estado_motocicleta` = 'DISPONIBLE'
             WHERE `Placa` = NEW.`Placa`
               AND `Numero_documento` = NEW.`Numero_documento21`
               AND `Estado_motocicleta` = 'ARRENDADA';
        END IF;
    END IF;
END$$

-- 9) Controla las transiciones del pago y, al quedar PAGADO, fija
--    automaticamente la fecha de pago.
DROP TRIGGER IF EXISTS `trg_pago_bu`$$
CREATE TRIGGER `trg_pago_bu`
BEFORE UPDATE ON `PAGO`
FOR EACH ROW
BEGIN
    SET NEW.`Metodo_pago` = UPPER(TRIM(NEW.`Metodo_pago`));

    IF NEW.`Monto` <> OLD.`Monto` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El monto de un pago no se puede modificar.', MYSQL_ERRNO = 45001;
    END IF;

    IF NEW.`Estado` <> OLD.`Estado` AND NOT (
           (OLD.`Estado` = 'PENDIENTE' AND NEW.`Estado` IN ('PAGADO', 'VENCIDO', 'CANCELADO'))
        OR (OLD.`Estado` = 'VENCIDO'   AND NEW.`Estado` IN ('PAGADO', 'CANCELADO'))
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transicion de estado de pago no permitida.', MYSQL_ERRNO = 45008;
    END IF;

    IF NEW.`Estado` = 'PAGADO' AND OLD.`Estado` <> 'PAGADO' THEN
        IF NEW.`Metodo_pago` NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'PSE', 'NEQUI') THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Metodo de pago invalido.', MYSQL_ERRNO = 45001;
        END IF;
        SET NEW.`fecha_pago` = CURDATE();
    END IF;
END$$

-- 10) Al registrar un mantenimiento se actualiza el kilometraje de la moto
--     (nunca lo disminuye).
DROP TRIGGER IF EXISTS `trg_mantenimiento_ai`$$
CREATE TRIGGER `trg_mantenimiento_ai`
AFTER INSERT ON `INFO_MANTENIMIENTO`
FOR EACH ROW
BEGIN
    UPDATE `MOTOCICLETA`
       SET `Kilometraje_actual` = GREATEST(`Kilometraje_actual`, NEW.`Kilometraje_al_momento`)
     WHERE `Placa` = NEW.`Placa`
       AND `Numero_documento` = NEW.`Numero_documento`;
END$$


-- ================================================================
-- 3. PROCEDIMIENTOS
-- ================================================================
-- Patron de excepciones usado en todos los que escriben:
--   DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
-- Cualquier error (de un SIGNAL propio, de un trigger o del motor) deshace
-- la transaccion y se re-lanza al backend con su mensaje y MYSQL_ERRNO.

-- 1) Registra el usuario y su fila de subtipo segun el rol.
DROP PROCEDURE IF EXISTS `sp_registrar_usuario`$$
CREATE PROCEDURE `sp_registrar_usuario`(
    IN p_tipo_documento   CHAR(20),
    IN p_numero_documento INT,
    IN p_primer_nombre    CHAR(50),
    IN p_segundo_nombre   CHAR(50),
    IN p_primer_apellido  CHAR(50),
    IN p_segundo_apellido CHAR(50),
    IN p_telefono         INT,
    IN p_correo           CHAR(50),
    IN p_contrasena_hash  VARCHAR(255),
    IN p_rol              CHAR(20)
)
BEGIN
    DECLARE v_rol CHAR(20);

    DECLARE e_llave_duplicada CONDITION FOR 1062;

    DECLARE EXIT HANDLER FOR e_llave_duplicada
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe un usuario con ese numero de documento.', MYSQL_ERRNO = 45003;
    END;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_rol = UPPER(TRIM(p_rol));

    IF v_rol IS NULL OR v_rol NOT IN ('ADMIN', 'ARRENDADOR', 'ARRENDATARIO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rol invalido (ADMIN, ARRENDADOR, ARRENDATARIO).', MYSQL_ERRNO = 45001;
    END IF;

    IF p_contrasena_hash IS NULL OR TRIM(p_contrasena_hash) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La contrasena (hash) es obligatoria.', MYSQL_ERRNO = 45001;
    END IF;

    START TRANSACTION;

    INSERT INTO `USUARIO`
        (`Tipo_documento`, `Numero_documento`, `Primer_nombre`, `Segundo_nombre`,
         `Primer_apellido`, `Segundo_apellido`, `Telefono`, `Correo`,
         `Fecha_registro`, `Estado`, `Contraseña`, `Rol_usuario`)
    VALUES
        (p_tipo_documento, p_numero_documento, p_primer_nombre, NULLIF(TRIM(p_segundo_nombre), ''),
         p_primer_apellido, NULLIF(TRIM(p_segundo_apellido), ''), p_telefono, p_correo,
         CURDATE(), 'ACTIVO', p_contrasena_hash, v_rol);

    CASE v_rol
        WHEN 'ADMIN' THEN
            INSERT INTO `ADMINISTRADOR` (`Numero_documento`) VALUES (p_numero_documento);
        WHEN 'ARRENDADOR' THEN
            INSERT INTO `ARRENDADOR` (`Numero_documento`) VALUES (p_numero_documento);
        WHEN 'ARRENDATARIO' THEN
            INSERT INTO `ARRENDATARIO` (`Numero_documento`) VALUES (p_numero_documento);
    END CASE;

    COMMIT;
END$$

-- 2) Un administrador activa, inactiva o bloquea a un usuario.
--    Al inactivar/bloquear, trg_usuario_au cancela sus solicitudes pendientes.
DROP PROCEDURE IF EXISTS `sp_cambiar_estado_usuario`$$
CREATE PROCEDURE `sp_cambiar_estado_usuario`(
    IN p_documento_admin INT,
    IN p_documento       INT,
    IN p_estado          CHAR(20)
)
BEGIN
    DECLARE v_estado CHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_estado = UPPER(TRIM(p_estado));

    IF v_estado IS NULL OR v_estado NOT IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado invalido (ACTIVO, INACTIVO, BLOQUEADO).', MYSQL_ERRNO = 45001;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `ADMINISTRADOR` WHERE `Numero_documento` = p_documento_admin)
       OR fn_usuario_activo(p_documento_admin) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo un administrador activo puede cambiar estados.', MYSQL_ERRNO = 45007;
    END IF;

    IF p_documento_admin = p_documento THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un administrador no puede cambiar su propio estado.', MYSQL_ERRNO = 45007;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `USUARIO` WHERE `Numero_documento` = p_documento) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El usuario no existe.', MYSQL_ERRNO = 45002;
    END IF;

    START TRANSACTION;

    UPDATE `USUARIO`
       SET `Estado` = v_estado
     WHERE `Numero_documento` = p_documento;

    COMMIT;
END$$

-- 3) Registra el seguro y la motocicleta juntos (el front los envia juntos).
DROP PROCEDURE IF EXISTS `sp_registrar_motocicleta`$$
CREATE PROCEDURE `sp_registrar_motocicleta`(
    IN  p_arrendador            INT,
    IN  p_placa                 CHAR(10),
    IN  p_marca                 CHAR(50),
    IN  p_modelo                CHAR(50),
    IN  p_anio                  INT,
    IN  p_cilindraje            DECIMAL(10,2),
    IN  p_kilometraje           INT,
    IN  p_precio_alquiler       INT,
    IN  p_numero_poliza         INT,
    IN  p_aseguradora           CHAR(50),
    IN  p_fecha_inicio_vigencia DATE,
    IN  p_estado_poliza         CHAR(20),
    OUT p_id_seguro             INT
)
BEGIN
    DECLARE v_estado_poliza CHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_estado_poliza = UPPER(TRIM(p_estado_poliza));

    IF NOT EXISTS (SELECT 1 FROM `ARRENDADOR` WHERE `Numero_documento` = p_arrendador)
       OR fn_usuario_activo(p_arrendador) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El arrendador no existe o no esta activo.', MYSQL_ERRNO = 45002;
    END IF;

    IF v_estado_poliza IS NULL OR v_estado_poliza NOT IN ('VIGENTE', 'VENCIDA', 'CANCELADA')
       OR p_numero_poliza IS NULL OR p_numero_poliza <= 0
       OR p_aseguradora IS NULL OR TRIM(p_aseguradora) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Datos del seguro invalidos.', MYSQL_ERRNO = 45001;
    END IF;

    START TRANSACTION;

    -- Bloquea SEGURO antes de verificar la poliza y generar el id.
    SELECT COALESCE(MAX(`Id_seguro`), 0) + 1
      INTO p_id_seguro
      FROM `SEGURO`
       FOR UPDATE;

    IF EXISTS (SELECT 1 FROM `SEGURO`
                WHERE `Aseguradora` = TRIM(p_aseguradora)
                  AND `numero_poliza` = p_numero_poliza) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La poliza ya esta registrada para esa aseguradora.', MYSQL_ERRNO = 45003;
    END IF;

    INSERT INTO `SEGURO`
        (`Estado_poliza`, `Id_seguro`, `numero_poliza`, `Aseguradora`, `Fecha_inicio_vigencia`)
    VALUES
        (v_estado_poliza, p_id_seguro, p_numero_poliza, TRIM(p_aseguradora), p_fecha_inicio_vigencia);

    -- trg_motocicleta_bi valida los datos, la placa y el seguro unicos.
    INSERT INTO `MOTOCICLETA`
        (`Estado_motocicleta`, `Kilometraje_actual`, `Precio_alquiler`, `Cilindraje`, `Año`,
         `modelo`, `Marca`, `Placa`, `Numero_documento`, `Id_seguro`)
    VALUES
        ('DISPONIBLE', p_kilometraje, p_precio_alquiler, p_cilindraje, p_anio,
         p_modelo, p_marca, p_placa, p_arrendador, p_id_seguro);

    COMMIT;
END$$

-- 4) Catalogo de motos que se pueden alquilar en un rango de fechas.
DROP PROCEDURE IF EXISTS `sp_listar_motos_disponibles`$$
CREATE PROCEDURE `sp_listar_motos_disponibles`(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin    DATE
)
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL
       OR p_fecha_inicio < CURDATE() OR p_fecha_fin < p_fecha_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45006;
    END IF;

    SELECT m.`Placa`,
           m.`Marca`,
           m.`modelo`              AS `Modelo`,
           m.`Año`                 AS `Anio`,
           m.`Cilindraje`,
           m.`Kilometraje_actual`,
           m.`Precio_alquiler`,
           m.`Estado_motocicleta`,
           m.`Numero_documento`    AS `Documento_arrendador`,
           fn_nombre_completo(m.`Numero_documento`) AS `Nombre_arrendador`,
           fn_calcular_valor_alquiler(m.`Placa`, m.`Numero_documento`,
                                      p_fecha_inicio, p_fecha_fin) AS `Valor_estimado`
      FROM `MOTOCICLETA` m
     WHERE fn_moto_disponible(m.`Placa`, m.`Numero_documento`,
                              p_fecha_inicio, p_fecha_fin, NULL) = 1
     ORDER BY m.`Precio_alquiler`;
END$$

-- 5) Crea la solicitud y su contrato PENDIENTE en una sola transaccion.
--
-- Por la FK circular obligatoria SOLICITUD <-> CONTRATO, ninguna de las dos
-- filas puede insertarse primero con las FK activas. Por eso se desactiva
-- FOREIGN_KEY_CHECKS solo en esta sesion y solo durante los dos INSERT.
-- Mientras esta desactivado, MySQL no valida las FK; por eso TODAS las
-- referencias (arrendatario, moto, arrendador) se validan antes a mano, y
-- las dos filas se apuntan entre si con los mismos valores.
-- Si algo falla, el handler hace ROLLBACK y restaura FOREIGN_KEY_CHECKS.
DROP PROCEDURE IF EXISTS `sp_crear_solicitud`$$
CREATE PROCEDURE `sp_crear_solicitud`(
    IN  p_arrendatario INT,
    IN  p_placa        CHAR(10),
    IN  p_fecha_inicio DATE,
    IN  p_fecha_fin    DATE,
    IN  p_mensaje      CHAR(200),
    OUT p_id_solicitud INT,
    OUT p_id_contrato  INT
)
BEGIN
    DECLARE v_fk_previo  TINYINT;
    DECLARE v_placa      CHAR(10);
    DECLARE v_arrendador INT DEFAULT NULL;
    DECLARE v_motos      INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET FOREIGN_KEY_CHECKS = v_fk_previo;
        RESIGNAL;
    END;

    -- Primera instruccion: guardar el valor actual para poder restaurarlo.
    SET v_fk_previo = @@SESSION.foreign_key_checks;
    SET v_placa     = UPPER(TRIM(p_placa));

    IF NOT EXISTS (SELECT 1 FROM `ARRENDATARIO` WHERE `Numero_documento` = p_arrendatario)
       OR fn_usuario_activo(p_arrendatario) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El arrendatario no existe o no esta activo.', MYSQL_ERRNO = 45002;
    END IF;

    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL
       OR p_fecha_inicio < CURDATE() OR p_fecha_fin < p_fecha_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45006;
    END IF;

    START TRANSACTION;

    -- Bloquea la moto: serializa solicitudes y aprobaciones sobre la misma moto.
    SELECT COUNT(*), MIN(`Numero_documento`)
      INTO v_motos, v_arrendador
      FROM `MOTOCICLETA`
     WHERE `Placa` = v_placa
       FOR UPDATE;

    IF v_motos = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no existe.', MYSQL_ERRNO = 45004;
    ELSEIF v_motos > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La placa esta registrada a mas de un arrendador.', MYSQL_ERRNO = 45010;
    END IF;

    IF v_arrendador = p_arrendatario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un usuario no puede alquilar su propia motocicleta.', MYSQL_ERRNO = 45007;
    END IF;

    IF fn_moto_disponible(v_placa, v_arrendador, p_fecha_inicio, p_fecha_fin, NULL) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no esta disponible en esas fechas.', MYSQL_ERRNO = 45005;
    END IF;

    IF EXISTS (SELECT 1 FROM `SOLICITUD`
                WHERE `Numero_documento` = p_arrendatario
                  AND `Placa` = v_placa
                  AND `Numero_documento2` = v_arrendador
                  AND `Estado_solicitud` = 'PENDIENTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya tiene una solicitud pendiente para esta motocicleta.', MYSQL_ERRNO = 45003;
    END IF;

    SELECT COALESCE(MAX(`Id_solicitud`), 0) + 1
      INTO p_id_solicitud
      FROM `SOLICITUD`
       FOR UPDATE;

    SELECT COALESCE(MAX(`id_contrato`), 0) + 1
      INTO p_id_contrato
      FROM `CONTRATO`
       FOR UPDATE;

    SET FOREIGN_KEY_CHECKS = 0;

    INSERT INTO `CONTRATO`
        (`id_contrato`, `Fecha_inicio`, `Fecha_fin`, `Estado_contrato`,
         `Numero_documento`, `Numero_documento1`,
         `Id_solicitud`, `Numero_documento2`, `Placa`, `Numero_documento21`)
    VALUES
        (p_id_contrato, p_fecha_inicio, p_fecha_fin, 'PENDIENTE',
         p_arrendatario, v_arrendador,
         p_id_solicitud, p_arrendatario, v_placa, v_arrendador);

    -- trg_solicitud_bi completa Fecha_solicitud y Estado_solicitud.
    INSERT INTO `SOLICITUD`
        (`Id_solicitud`, `Fecha_solicitud`, `Estado_solicitud`, `Mensaje_solicitud`,
         `Numero_documento`, `Placa`, `Numero_documento2`,
         `id_contrato`, `Numero_documento1`, `Numero_documento11`)
    VALUES
        (p_id_solicitud, CURDATE(), 'PENDIENTE', p_mensaje,
         p_arrendatario, v_placa, v_arrendador,
         p_id_contrato, p_arrendatario, v_arrendador);

    SET FOREIGN_KEY_CHECKS = v_fk_previo;

    COMMIT;
END$$

-- 6) El arrendador aprueba o rechaza una solicitud.
--   RECHAZADA: solicitud RECHAZADA (trg_solicitud_au cancela su contrato).
--   APROBADA : solicitud APROBADA, contrato VIGENTE, se genera el PAGO
--              PENDIENTE por el valor total, la moto pasa a ARRENDADA si el
--              alquiler empieza hoy, y con un CURSOR se rechazan las demas
--              solicitudes PENDIENTES de la misma moto que se cruzan en fechas.
-- p_id_pago devuelve el pago generado (NULL si se rechazo).
--
-- Nota: PAGO.fecha_pago y PAGO.Metodo_pago son NOT NULL en el modelo. Mientras
-- el pago esta PENDIENTE, fecha_pago guarda la fecha en que se genero y
-- Metodo_pago vale 'POR_DEFINIR'; al pagar, trg_pago_bu fija la fecha real.
DROP PROCEDURE IF EXISTS `sp_responder_solicitud`$$
CREATE PROCEDURE `sp_responder_solicitud`(
    IN  p_id_solicitud INT,
    IN  p_arrendador   INT,
    IN  p_decision     CHAR(20),
    OUT p_id_pago      INT
)
BEGIN
    DECLARE v_decision     CHAR(20);
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendatario INT;
    DECLARE v_arrendador   INT;
    DECLARE v_placa        CHAR(10);
    DECLARE v_id_contrato  INT;
    DECLARE v_fecha_inicio DATE;
    DECLARE v_fecha_fin    DATE;
    DECLARE v_monto        INT;
    DECLARE v_bloqueo      INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_id_pago  = NULL;
    SET v_decision = UPPER(TRIM(p_decision));

    IF v_decision IS NULL OR v_decision NOT IN ('APROBADA', 'RECHAZADA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La decision debe ser APROBADA o RECHAZADA.', MYSQL_ERRNO = 45001;
    END IF;

    START TRANSACTION;

    SELECT `Estado_solicitud`, `Numero_documento`, `Numero_documento2`, `Placa`, `id_contrato`
      INTO v_estado, v_arrendatario, v_arrendador, v_placa, v_id_contrato
      FROM `SOLICITUD`
     WHERE `Id_solicitud` = p_id_solicitud;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no existe.', MYSQL_ERRNO = 45004;
    END IF;

    IF v_arrendador <> p_arrendador THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no corresponde a una moto de este arrendador.', MYSQL_ERRNO = 45007;
    END IF;

    -- Orden de bloqueo: MOTOCICLETA -> SOLICITUD -> CONTRATO (igual que
    -- sp_crear_solicitud, que bloquea la moto primero) para evitar deadlocks.
    SELECT COUNT(*)
      INTO v_bloqueo
      FROM `MOTOCICLETA`
     WHERE `Placa` = v_placa
       AND `Numero_documento` = v_arrendador
       FOR UPDATE;

    -- Se relee con bloqueo: otro proceso pudo cambiar el estado entre tanto.
    SELECT `Estado_solicitud`
      INTO v_estado
      FROM `SOLICITUD`
     WHERE `Id_solicitud` = p_id_solicitud
       FOR UPDATE;

    IF v_estado <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud ya fue respondida o cancelada.', MYSQL_ERRNO = 45008;
    END IF;

    SELECT `Fecha_inicio`, `Fecha_fin`
      INTO v_fecha_inicio, v_fecha_fin
      FROM `CONTRATO`
     WHERE `id_contrato` = v_id_contrato
       FOR UPDATE;

    IF v_decision = 'RECHAZADA' THEN

        UPDATE `SOLICITUD`
           SET `Estado_solicitud` = 'RECHAZADA'
         WHERE `Id_solicitud` = p_id_solicitud;

    ELSE

        IF v_fecha_inicio < CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La fecha de inicio ya paso; la solicitud expiro.', MYSQL_ERRNO = 45006;
        END IF;

        IF fn_usuario_activo(v_arrendatario) = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El arrendatario ya no esta activo.', MYSQL_ERRNO = 45002;
        END IF;

        IF fn_moto_disponible(v_placa, v_arrendador, v_fecha_inicio, v_fecha_fin, v_id_contrato) = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La motocicleta ya no esta disponible en esas fechas.', MYSQL_ERRNO = 45005;
        END IF;

        UPDATE `SOLICITUD`
           SET `Estado_solicitud` = 'APROBADA'
         WHERE `Id_solicitud` = p_id_solicitud;

        UPDATE `CONTRATO`
           SET `Estado_contrato` = 'VIGENTE'
         WHERE `id_contrato` = v_id_contrato;

        IF v_fecha_inicio <= CURDATE() THEN
            UPDATE `MOTOCICLETA`
               SET `Estado_motocicleta` = 'ARRENDADA'
             WHERE `Placa` = v_placa
               AND `Numero_documento` = v_arrendador
               AND `Estado_motocicleta` = 'DISPONIBLE';
        END IF;

        -- Pago unico por el valor total, vence el dia de inicio del alquiler.
        SET v_monto = fn_calcular_valor_alquiler(v_placa, v_arrendador, v_fecha_inicio, v_fecha_fin);

        SELECT COALESCE(MAX(`Id_pago`), 0) + 1
          INTO p_id_pago
          FROM `PAGO`
           FOR UPDATE;

        INSERT INTO `PAGO`
            (`Id_pago`, `fecha_pago`, `Monto`, `Fecha_vencimiento`, `Estado`, `Metodo_pago`,
             `id_contrato`, `Numero_documento`, `Numero_documento1`)
        VALUES
            (p_id_pago, CURDATE(), v_monto, v_fecha_inicio, 'PENDIENTE', 'POR_DEFINIR',
             v_id_contrato, v_arrendatario, v_arrendador);

        -- CURSOR: rechaza las otras solicitudes pendientes que se cruzan
        -- (trg_solicitud_au cancela el contrato de cada una).
        -- Va en un bloque propio para que el handler NOT FOUND solo afecte
        -- al FETCH del cursor y no a los SELECT ... INTO de arriba.
        BEGIN
            DECLARE v_fin            TINYINT DEFAULT 0;
            DECLARE v_otra_solicitud INT;

            DECLARE cur_conflictos CURSOR FOR
                SELECT s.`Id_solicitud`
                  FROM `SOLICITUD` s
                  JOIN `CONTRATO` c
                    ON c.`id_contrato`       = s.`id_contrato`
                   AND c.`Numero_documento`  = s.`Numero_documento1`
                   AND c.`Numero_documento1` = s.`Numero_documento11`
                 WHERE s.`Placa` = v_placa
                   AND s.`Numero_documento2` = v_arrendador
                   AND s.`Estado_solicitud` = 'PENDIENTE'
                   AND s.`Id_solicitud` <> p_id_solicitud
                   AND c.`Fecha_inicio` <= v_fecha_fin
                   AND c.`Fecha_fin` >= v_fecha_inicio;

            DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;

            OPEN cur_conflictos;

            lectura: LOOP
                FETCH cur_conflictos INTO v_otra_solicitud;
                IF v_fin = 1 THEN
                    LEAVE lectura;
                END IF;

                UPDATE `SOLICITUD`
                   SET `Estado_solicitud` = 'RECHAZADA'
                 WHERE `Id_solicitud` = v_otra_solicitud;
            END LOOP lectura;

            CLOSE cur_conflictos;
        END;

    END IF;

    COMMIT;
END$$

-- 7) El arrendatario paga un pago PENDIENTE o VENCIDO de un contrato VIGENTE.
--    trg_pago_bu valida la transicion y fija fecha_pago.
DROP PROCEDURE IF EXISTS `sp_registrar_pago`$$
CREATE PROCEDURE `sp_registrar_pago`(
    IN p_id_pago      INT,
    IN p_arrendatario INT,
    IN p_metodo_pago  CHAR(20)
)
BEGIN
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendatario INT;
    DECLARE v_id_contrato  INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT `Estado`, `Numero_documento`, `id_contrato`
      INTO v_estado, v_arrendatario, v_id_contrato
      FROM `PAGO`
     WHERE `Id_pago` = p_id_pago
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El pago no existe.', MYSQL_ERRNO = 45004;
    END IF;

    IF v_arrendatario <> p_arrendatario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El pago no pertenece a este arrendatario.', MYSQL_ERRNO = 45007;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `CONTRATO`
                    WHERE `id_contrato` = v_id_contrato
                      AND `Estado_contrato` = 'VIGENTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato del pago no esta VIGENTE.', MYSQL_ERRNO = 45008;
    END IF;

    UPDATE `PAGO`
       SET `Estado`      = 'PAGADO',
           `Metodo_pago` = p_metodo_pago
     WHERE `Id_pago` = p_id_pago;

    COMMIT;
END$$

-- 8) Arrendador finaliza, o cualquiera de las partes cancela, un contrato VIGENTE.
--   FINALIZADO: solo el arrendador, contrato ya iniciado, sin saldo pendiente;
--               registra el kilometraje de devolucion.
--   CANCELADO : arrendatario o arrendador, solo si el contrato aun no inicia.
--               (los pagos PAGADOS se conservan; la devolucion de dinero se
--               gestiona fuera de la base de datos).
-- Efectos automaticos (trg_contrato_au): al FINALIZAR la moto vuelve a
-- DISPONIBLE; al CANCELAR los pagos pendientes/vencidos se cancelan.
DROP PROCEDURE IF EXISTS `sp_cerrar_contrato`$$
CREATE PROCEDURE `sp_cerrar_contrato`(
    IN p_id_contrato       INT,
    IN p_documento         INT,
    IN p_accion            CHAR(20),
    IN p_kilometraje_final INT
)
BEGIN
    DECLARE v_accion       CHAR(20);
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendatario INT;
    DECLARE v_arrendador   INT;
    DECLARE v_placa        CHAR(10);
    DECLARE v_fecha_inicio DATE;
    DECLARE v_km_actual    INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_accion = UPPER(TRIM(p_accion));

    IF v_accion IS NULL OR v_accion NOT IN ('FINALIZADO', 'CANCELADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La accion debe ser FINALIZADO o CANCELADO.', MYSQL_ERRNO = 45001;
    END IF;

    START TRANSACTION;

    SELECT `Estado_contrato`, `Numero_documento`, `Numero_documento1`, `Placa`, `Fecha_inicio`
      INTO v_estado, v_arrendatario, v_arrendador, v_placa, v_fecha_inicio
      FROM `CONTRATO`
     WHERE `id_contrato` = p_id_contrato
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato no existe.', MYSQL_ERRNO = 45004;
    END IF;

    IF v_estado <> 'VIGENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se pueden cerrar contratos VIGENTES.', MYSQL_ERRNO = 45008;
    END IF;

    IF v_accion = 'FINALIZADO' THEN

        IF p_documento <> v_arrendador THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Solo el arrendador puede finalizar el contrato.', MYSQL_ERRNO = 45007;
        END IF;

        IF v_fecha_inicio > CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El contrato aun no ha iniciado; debe cancelarse.', MYSQL_ERRNO = 45008;
        END IF;

        IF fn_saldo_pendiente(p_id_contrato) > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El contrato tiene pagos pendientes.', MYSQL_ERRNO = 45009;
        END IF;

        SELECT `Kilometraje_actual`
          INTO v_km_actual
          FROM `MOTOCICLETA`
         WHERE `Placa` = v_placa
           AND `Numero_documento` = v_arrendador
           FOR UPDATE;

        IF p_kilometraje_final IS NULL OR p_kilometraje_final < v_km_actual THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El kilometraje final no puede ser menor al actual.', MYSQL_ERRNO = 45001;
        END IF;

        UPDATE `MOTOCICLETA`
           SET `Kilometraje_actual` = p_kilometraje_final
         WHERE `Placa` = v_placa
           AND `Numero_documento` = v_arrendador;

    ELSE

        IF p_documento NOT IN (v_arrendatario, v_arrendador) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El usuario no es parte del contrato.', MYSQL_ERRNO = 45007;
        END IF;

        IF v_fecha_inicio <= CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El contrato ya inicio y no puede cancelarse.', MYSQL_ERRNO = 45008;
        END IF;

    END IF;

    UPDATE `CONTRATO`
       SET `Estado_contrato` = v_accion
     WHERE `id_contrato` = p_id_contrato;

    COMMIT;
END$$

-- 9) Proceso diario (lo puede llamar el evento de la seccion 4 o el backend):
--   1. Pagos PENDIENTES cuya fecha de vencimiento ya paso -> VENCIDO.
--   2. CURSOR: solicitudes PENDIENTES cuya fecha de inicio ya paso
--      -> CANCELADA (trg_solicitud_au cancela su contrato).
--   3. Motos DISPONIBLES con un contrato VIGENTE que ya inicio -> ARRENDADA.
DROP PROCEDURE IF EXISTS `sp_actualizar_estados_diarios`$$
CREATE PROCEDURE `sp_actualizar_estados_diarios`(
    OUT p_pagos_vencidos        INT,
    OUT p_solicitudes_expiradas INT,
    OUT p_motos_arrendadas      INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_solicitudes_expiradas = 0;

    START TRANSACTION;

    UPDATE `PAGO`
       SET `Estado` = 'VENCIDO'
     WHERE `Estado` = 'PENDIENTE'
       AND `Fecha_vencimiento` < CURDATE();

    SET p_pagos_vencidos = ROW_COUNT();

    BEGIN
        DECLARE v_fin          TINYINT DEFAULT 0;
        DECLARE v_id_solicitud INT;

        DECLARE cur_expiradas CURSOR FOR
            SELECT s.`Id_solicitud`
              FROM `SOLICITUD` s
              JOIN `CONTRATO` c
                ON c.`id_contrato`       = s.`id_contrato`
               AND c.`Numero_documento`  = s.`Numero_documento1`
               AND c.`Numero_documento1` = s.`Numero_documento11`
             WHERE s.`Estado_solicitud` = 'PENDIENTE'
               AND c.`Fecha_inicio` < CURDATE();

        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;

        OPEN cur_expiradas;

        lectura: LOOP
            FETCH cur_expiradas INTO v_id_solicitud;
            IF v_fin = 1 THEN
                LEAVE lectura;
            END IF;

            UPDATE `SOLICITUD`
               SET `Estado_solicitud` = 'CANCELADA'
             WHERE `Id_solicitud` = v_id_solicitud;

            SET p_solicitudes_expiradas = p_solicitudes_expiradas + 1;
        END LOOP lectura;

        CLOSE cur_expiradas;
    END;

    UPDATE `MOTOCICLETA` m
      JOIN `CONTRATO` c
        ON c.`Placa` = m.`Placa`
       AND c.`Numero_documento21` = m.`Numero_documento`
       SET m.`Estado_motocicleta` = 'ARRENDADA'
     WHERE c.`Estado_contrato` = 'VIGENTE'
       AND c.`Fecha_inicio` <= CURDATE()
       AND c.`Fecha_fin` >= CURDATE()
       AND m.`Estado_motocicleta` = 'DISPONIBLE';

    SET p_motos_arrendadas = ROW_COUNT();

    COMMIT;
END$$

-- 10) Reporte por moto de un arrendador en un rango de fechas, armado con
--     CURSOR: contratos, ingresos (pagos PAGADOS), costos de mantenimiento
--     y utilidad.
DROP PROCEDURE IF EXISTS `sp_reporte_ingresos_arrendador`$$
CREATE PROCEDURE `sp_reporte_ingresos_arrendador`(
    IN p_arrendador INT,
    IN p_desde      DATE,
    IN p_hasta      DATE
)
BEGIN
    DECLARE v_fin       TINYINT DEFAULT 0;
    DECLARE v_placa     CHAR(10);
    DECLARE v_marca     CHAR(50);
    DECLARE v_modelo    CHAR(50);
    DECLARE v_contratos INT;
    DECLARE v_ingresos  BIGINT;
    DECLARE v_costos    BIGINT;

    DECLARE cur_motos CURSOR FOR
        SELECT `Placa`, `Marca`, `modelo`
          FROM `MOTOCICLETA`
         WHERE `Numero_documento` = p_arrendador
         ORDER BY `Placa`;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;

    IF NOT EXISTS (SELECT 1 FROM `ARRENDADOR` WHERE `Numero_documento` = p_arrendador) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El arrendador no existe.', MYSQL_ERRNO = 45002;
    END IF;

    IF p_desde IS NULL OR p_hasta IS NULL OR p_hasta < p_desde THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45006;
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_reporte_ingresos`;
    CREATE TEMPORARY TABLE `tmp_reporte_ingresos` (
        `Placa`                CHAR(10),
        `Marca`                CHAR(50),
        `Modelo`               CHAR(50),
        `Contratos`            INT,
        `Ingresos`             BIGINT,
        `Costos_mantenimiento` BIGINT,
        `Utilidad`             BIGINT
    );

    OPEN cur_motos;

    lectura: LOOP
        FETCH cur_motos INTO v_placa, v_marca, v_modelo;
        IF v_fin = 1 THEN
            LEAVE lectura;
        END IF;

        SET v_contratos = (
            SELECT COUNT(*)
              FROM `CONTRATO`
             WHERE `Placa` = v_placa
               AND `Numero_documento21` = p_arrendador
               AND `Estado_contrato` IN ('VIGENTE', 'FINALIZADO')
               AND `Fecha_inicio` <= p_hasta
               AND `Fecha_fin` >= p_desde
        );

        SET v_ingresos = (
            SELECT COALESCE(SUM(p.`Monto`), 0)
              FROM `PAGO` p
              JOIN `CONTRATO` c
                ON c.`id_contrato`       = p.`id_contrato`
               AND c.`Numero_documento`  = p.`Numero_documento`
               AND c.`Numero_documento1` = p.`Numero_documento1`
             WHERE c.`Placa` = v_placa
               AND c.`Numero_documento21` = p_arrendador
               AND p.`Estado` = 'PAGADO'
               AND p.`fecha_pago` BETWEEN p_desde AND p_hasta
        );

        SET v_costos = (
            SELECT COALESCE(SUM(`Costo`), 0)
              FROM `INFO_MANTENIMIENTO`
             WHERE `Placa` = v_placa
               AND `Numero_documento` = p_arrendador
               AND `Fecha_mantenimiento` BETWEEN p_desde AND p_hasta
        );

        INSERT INTO `tmp_reporte_ingresos`
        VALUES (v_placa, v_marca, v_modelo, v_contratos, v_ingresos, v_costos, v_ingresos - v_costos);
    END LOOP lectura;

    CLOSE cur_motos;

    SELECT * FROM `tmp_reporte_ingresos` ORDER BY `Utilidad` DESC;

    DROP TEMPORARY TABLE IF EXISTS `tmp_reporte_ingresos`;
END$$


-- ================================================================
-- 4. EVENTO PROGRAMADO (opcional)
-- Requiere activar el programador una vez (usuario con privilegios):
--   SET GLOBAL event_scheduler = ON;
-- ================================================================

DROP EVENT IF EXISTS `ev_actualizacion_diaria`$$
CREATE EVENT `ev_actualizacion_diaria`
ON SCHEDULE EVERY 1 DAY
STARTS (TIMESTAMP(CURRENT_DATE) + INTERVAL 1 DAY + INTERVAL 5 MINUTE)
DO
    CALL sp_actualizar_estados_diarios(@ev_pagos, @ev_solicitudes, @ev_motos)$$

DELIMITER ;


-- ================================================================
-- 5. EJEMPLOS DE USO (para el backend; no se ejecutan, estan comentados)
-- ================================================================
-- CALL sp_registrar_usuario('CC', 1001, 'Ana', NULL, 'Rojas', NULL,
--                           300123456, 'ana@correo.com', '<hash>', 'ARRENDADOR');
-- CALL sp_registrar_usuario('CC', 2002, 'Luis', NULL, 'Diaz', NULL,
--                           310123456, 'luis@correo.com', '<hash>', 'ARRENDATARIO');
--
-- CALL sp_registrar_motocicleta(1001, 'ABC12D', 'Yamaha', 'NMAX', 2023, 155, 1200, 80000,
--                               987654, 'Sura', '2026-01-01', 'VIGENTE', @id_seguro);
--
-- CALL sp_listar_motos_disponibles('2026-11-01', '2026-11-05');
--
-- CALL sp_crear_solicitud(2002, 'ABC12D', '2026-11-01', '2026-11-05',
--                         'La necesito para viajar', @id_sol, @id_con);
-- SELECT @id_sol, @id_con;
--
-- CALL sp_responder_solicitud(@id_sol, 1001, 'APROBADA', @id_pago);
-- CALL sp_registrar_pago(@id_pago, 2002, 'NEQUI');
-- CALL sp_cerrar_contrato(@id_con, 1001, 'FINALIZADO', 1650);
-- CALL sp_cerrar_contrato(@id_con, 2002, 'CANCELADO', NULL);
--
-- CALL sp_reporte_ingresos_arrendador(1001, '2026-01-01', '2026-12-31');
-- CALL sp_actualizar_estados_diarios(@p, @s, @m); SELECT @p, @s, @m;
--
-- Los mantenimientos se insertan directamente en INFO_MANTENIMIENTO;
-- trg_mantenimiento_ai actualiza solo el kilometraje de la moto.
--
-- Nota sobre Telefono INT: un celular colombiano de 10 digitos (3001234567)
-- supera el maximo de INT (2147483647). Se recomienda cambiar la columna a
-- VARCHAR(20) o BIGINT en el script de tablas antes de poblar datos reales.
