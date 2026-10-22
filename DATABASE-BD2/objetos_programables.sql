-- ================================================================
-- Objetos programables - Sistema de alquiler de motocicletas (MySQL 8)
--
-- Contenido:
--   0. Catalogos de estados y codigos de error (excepciones)
--   1. Tabla auxiliar de auditoria
--   2. Procedimientos de validacion y auditoria (usados por triggers)
--   3. Funciones
--   4. Triggers
--   5. Procedimientos de negocio (con transacciones, excepciones y cursores)
--   6. Evento programado (opcional)
--   7. Ejemplos de uso para el backend
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
--   - Para auditoria, el backend puede ejecutar antes de cada CALL:
--       SET @usuario_app = <numero_documento del usuario logueado>;
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
--   SOLICITUD: PENDIENTE -> APROBADA | RECHAZADA | CANCELADA
--   CONTRATO : PENDIENTE -> VIGENTE | CANCELADO
--              VIGENTE   -> FINALIZADO | CANCELADO
--   PAGO     : PENDIENTE -> PAGADO | VENCIDO | CANCELADO
--              VENCIDO   -> PAGADO | CANCELADO
--
-- Por que existe el contrato PENDIENTE:
--   SOLICITUD.id_contrato y CONTRATO.Id_solicitud son FK obligatorias
--   (NOT NULL) en ambos sentidos. Una solicitud no puede existir sin
--   contrato ni al reves. Por eso sp_crear_solicitud crea las dos filas
--   juntas: la solicitud PENDIENTE y su contrato PENDIENTE (que guarda las
--   fechas pedidas). Al aprobar, el contrato pasa a VIGENTE; al rechazar o
--   cancelar, pasa a CANCELADO.
--
-- Codigos de error propios (SIGNAL SQLSTATE '45000', MYSQL_ERRNO):
--   45001 Usuario inexistente o inactivo
--   45002 Rol invalido o no modificable
--   45003 Tipo de documento invalido
--   45004 Datos de usuario invalidos (correo, telefono, nombres)
--   45005 Correo ya registrado
--   45006 Numero de documento ya registrado
--   45007 Estado invalido
--   45010 Motocicleta inexistente
--   45011 Placa duplicada
--   45012 Datos de motocicleta invalidos
--   45013 El kilometraje no puede disminuir
--   45014 Motocicleta no disponible
--   45015 Seguro no vigente
--   45016 Datos de seguro invalidos o duplicados
--   45020 Solicitud inexistente
--   45021 Transicion de solicitud invalida
--   45022 Fechas invalidas
--   45023 Un usuario no puede alquilar su propia motocicleta
--   45024 Solicitud duplicada
--   45025 Operacion no autorizada para este usuario
--   45026 Inconsistencia en llaves / llaves no modificables
--   45030 Contrato inexistente
--   45031 Transicion o estado de contrato invalido
--   45032 Contrato con saldo pendiente
--   45033 El contrato ya inicio y no puede cancelarse
--   45040 Pago inexistente
--   45041 Transicion de pago invalida
--   45042 Monto o metodo de pago invalido
--   45050 Datos de mantenimiento invalidos
--
-- El backend puede leer MYSQL_ERRNO (ej. SQLException.getErrorCode() en
-- Java, err.errno en Node) y traducirlo a un HTTP 400/403/404/409.
-- ================================================================


-- ================================================================
-- 1. TABLA AUXILIAR DE AUDITORIA
-- ================================================================

CREATE TABLE IF NOT EXISTS `AUDITORIA_ESTADOS` (
    `Id_auditoria`    INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del registro de auditoria.',
    `Tabla`           VARCHAR(30) NOT NULL COMMENT 'Tabla en la que cambio el estado.',
    `Llave_registro`  VARCHAR(100) NOT NULL COMMENT 'Llave del registro modificado.',
    `Estado_anterior` VARCHAR(20) NULL COMMENT 'Estado antes del cambio.',
    `Estado_nuevo`    VARCHAR(20) NOT NULL COMMENT 'Estado despues del cambio.',
    `Fecha_cambio`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del cambio.',
    `Usuario_bd`      VARCHAR(100) NOT NULL COMMENT 'Usuario de conexion a MySQL.',
    `Usuario_app`     INT NULL COMMENT 'Documento del usuario de la aplicacion (@usuario_app).',
    CONSTRAINT `AUDITORIA_ESTADOS_PK` PRIMARY KEY (`Id_auditoria`),
    KEY `AUDITORIA_ESTADOS_REGISTRO_IX` (`Tabla`, `Llave_registro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


DELIMITER $$

-- ================================================================
-- 2. PROCEDIMIENTOS DE VALIDACION Y AUDITORIA (usados por triggers)
-- ================================================================

-- Registra un cambio de estado. Solo inserta si el estado realmente cambio.
DROP PROCEDURE IF EXISTS `sp_auditar_estado`$$
CREATE PROCEDURE `sp_auditar_estado`(
    IN p_tabla    VARCHAR(30),
    IN p_llave    VARCHAR(100),
    IN p_anterior VARCHAR(20),
    IN p_nuevo    VARCHAR(20)
)
BEGIN
    IF NOT (p_anterior <=> p_nuevo) THEN
        INSERT INTO `AUDITORIA_ESTADOS`
            (`Tabla`, `Llave_registro`, `Estado_anterior`, `Estado_nuevo`, `Usuario_bd`, `Usuario_app`)
        VALUES
            (p_tabla, p_llave, p_anterior, p_nuevo, USER(), @usuario_app);
    END IF;
END$$

-- Reglas de USUARIO compartidas por los triggers de INSERT y UPDATE.
DROP PROCEDURE IF EXISTS `sp_validar_datos_usuario`$$
CREATE PROCEDURE `sp_validar_datos_usuario`(
    IN p_tipo_documento   CHAR(20),
    IN p_numero_documento INT,
    IN p_primer_nombre    CHAR(50),
    IN p_primer_apellido  CHAR(50),
    IN p_telefono         INT,
    IN p_correo           CHAR(50),
    IN p_contrasena       VARCHAR(255),
    IN p_estado           CHAR(20),
    IN p_rol              CHAR(20)
)
BEGIN
    IF p_tipo_documento NOT IN ('CC', 'CE', 'TI', 'PAS') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tipo de documento invalido (CC, CE, TI, PAS).', MYSQL_ERRNO = 45003;
    END IF;

    IF p_numero_documento <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El numero de documento debe ser positivo.', MYSQL_ERRNO = 45004;
    END IF;

    IF TRIM(p_primer_nombre) = '' OR TRIM(p_primer_apellido) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El primer nombre y el primer apellido son obligatorios.', MYSQL_ERRNO = 45004;
    END IF;

    IF p_telefono <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El telefono debe ser un numero positivo.', MYSQL_ERRNO = 45004;
    END IF;

    IF p_correo NOT REGEXP '^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El correo no tiene un formato valido.', MYSQL_ERRNO = 45004;
    END IF;

    IF TRIM(p_contrasena) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La contrasena (hash) es obligatoria.', MYSQL_ERRNO = 45004;
    END IF;

    IF p_estado NOT IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de usuario invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF p_rol NOT IN ('ADMIN', 'ARRENDADOR', 'ARRENDATARIO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rol invalido (ADMIN, ARRENDADOR, ARRENDATARIO).', MYSQL_ERRNO = 45002;
    END IF;
END$$

-- Reglas de MOTOCICLETA compartidas por los triggers de INSERT y UPDATE.
DROP PROCEDURE IF EXISTS `sp_validar_datos_motocicleta`$$
CREATE PROCEDURE `sp_validar_datos_motocicleta`(
    IN p_placa       CHAR(10),
    IN p_marca       CHAR(50),
    IN p_modelo      CHAR(50),
    IN p_anio        INT,
    IN p_cilindraje  DECIMAL(10,2),
    IN p_kilometraje INT,
    IN p_precio      INT,
    IN p_estado      CHAR(20)
)
BEGIN
    IF TRIM(p_placa) = '' OR TRIM(p_marca) = '' OR TRIM(p_modelo) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Placa, marca y modelo son obligatorios.', MYSQL_ERRNO = 45012;
    END IF;

    IF p_anio < 1950 OR p_anio > YEAR(CURDATE()) + 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El anio de la motocicleta esta fuera de rango.', MYSQL_ERRNO = 45012;
    END IF;

    IF p_cilindraje <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El cilindraje debe ser mayor que cero.', MYSQL_ERRNO = 45012;
    END IF;

    IF p_kilometraje < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El kilometraje no puede ser negativo.', MYSQL_ERRNO = 45012;
    END IF;

    IF p_precio <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El precio de alquiler debe ser mayor que cero.', MYSQL_ERRNO = 45012;
    END IF;

    IF p_estado NOT IN ('DISPONIBLE', 'ARRENDADA', 'MANTENIMIENTO', 'INACTIVA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de motocicleta invalido.', MYSQL_ERRNO = 45007;
    END IF;
END$$


-- ================================================================
-- 3. FUNCIONES
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
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45022;
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
            SET MESSAGE_TEXT = 'La motocicleta no existe.', MYSQL_ERRNO = 45010;
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
-- 4. TRIGGERS
-- ================================================================

-- ---------------- USUARIO ----------------

DROP TRIGGER IF EXISTS `trg_usuario_bi`$$
CREATE TRIGGER `trg_usuario_bi`
BEFORE INSERT ON `USUARIO`
FOR EACH ROW
BEGIN
    SET NEW.`Correo`         = LOWER(TRIM(NEW.`Correo`));
    SET NEW.`Tipo_documento` = UPPER(TRIM(NEW.`Tipo_documento`));
    SET NEW.`Estado`         = UPPER(TRIM(NEW.`Estado`));
    SET NEW.`Rol_usuario`    = UPPER(TRIM(NEW.`Rol_usuario`));

    CALL sp_validar_datos_usuario(
        NEW.`Tipo_documento`, NEW.`Numero_documento`, NEW.`Primer_nombre`,
        NEW.`Primer_apellido`, NEW.`Telefono`, NEW.`Correo`, NEW.`Contraseña`,
        NEW.`Estado`, NEW.`Rol_usuario`);

    IF EXISTS (SELECT 1 FROM `USUARIO` WHERE `Correo` = NEW.`Correo`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El correo ya esta registrado.', MYSQL_ERRNO = 45005;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_usuario_bu`$$
CREATE TRIGGER `trg_usuario_bu`
BEFORE UPDATE ON `USUARIO`
FOR EACH ROW
BEGIN
    SET NEW.`Correo`         = LOWER(TRIM(NEW.`Correo`));
    SET NEW.`Tipo_documento` = UPPER(TRIM(NEW.`Tipo_documento`));
    SET NEW.`Estado`         = UPPER(TRIM(NEW.`Estado`));
    SET NEW.`Rol_usuario`    = UPPER(TRIM(NEW.`Rol_usuario`));

    IF NEW.`Numero_documento` <> OLD.`Numero_documento` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El numero de documento no se puede modificar.', MYSQL_ERRNO = 45026;
    END IF;

    -- El rol esta atado a la tabla de subtipo (ADMINISTRADOR/ARRENDADOR/ARRENDATARIO).
    IF NEW.`Rol_usuario` <> OLD.`Rol_usuario` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El rol de un usuario no se puede modificar.', MYSQL_ERRNO = 45002;
    END IF;

    CALL sp_validar_datos_usuario(
        NEW.`Tipo_documento`, NEW.`Numero_documento`, NEW.`Primer_nombre`,
        NEW.`Primer_apellido`, NEW.`Telefono`, NEW.`Correo`, NEW.`Contraseña`,
        NEW.`Estado`, NEW.`Rol_usuario`);

    IF EXISTS (SELECT 1 FROM `USUARIO`
                WHERE `Correo` = NEW.`Correo`
                  AND `Numero_documento` <> OLD.`Numero_documento`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El correo ya esta registrado.', MYSQL_ERRNO = 45005;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_usuario_au`$$
CREATE TRIGGER `trg_usuario_au`
AFTER UPDATE ON `USUARIO`
FOR EACH ROW
BEGIN
    CALL sp_auditar_estado('USUARIO', CAST(NEW.`Numero_documento` AS CHAR),
                           OLD.`Estado`, NEW.`Estado`);
END$$

-- ---------------- SEGURO ----------------

DROP TRIGGER IF EXISTS `trg_seguro_bi`$$
CREATE TRIGGER `trg_seguro_bi`
BEFORE INSERT ON `SEGURO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_poliza` = UPPER(TRIM(NEW.`Estado_poliza`));
    SET NEW.`Aseguradora`   = TRIM(NEW.`Aseguradora`);

    IF NEW.`Estado_poliza` NOT IN ('VIGENTE', 'VENCIDA', 'CANCELADA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de poliza invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF NEW.`numero_poliza` <= 0 OR NEW.`Aseguradora` = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Numero de poliza y aseguradora son obligatorios.', MYSQL_ERRNO = 45016;
    END IF;

    IF EXISTS (SELECT 1 FROM `SEGURO`
                WHERE `Aseguradora` = NEW.`Aseguradora`
                  AND `numero_poliza` = NEW.`numero_poliza`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La poliza ya esta registrada para esa aseguradora.', MYSQL_ERRNO = 45016;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_seguro_bu`$$
CREATE TRIGGER `trg_seguro_bu`
BEFORE UPDATE ON `SEGURO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_poliza` = UPPER(TRIM(NEW.`Estado_poliza`));
    SET NEW.`Aseguradora`   = TRIM(NEW.`Aseguradora`);

    IF NEW.`Id_seguro` <> OLD.`Id_seguro` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El id del seguro no se puede modificar.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Estado_poliza` NOT IN ('VIGENTE', 'VENCIDA', 'CANCELADA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de poliza invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF NEW.`numero_poliza` <= 0 OR NEW.`Aseguradora` = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Numero de poliza y aseguradora son obligatorios.', MYSQL_ERRNO = 45016;
    END IF;

    IF EXISTS (SELECT 1 FROM `SEGURO`
                WHERE `Aseguradora` = NEW.`Aseguradora`
                  AND `numero_poliza` = NEW.`numero_poliza`
                  AND `Id_seguro` <> OLD.`Id_seguro`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La poliza ya esta registrada para esa aseguradora.', MYSQL_ERRNO = 45016;
    END IF;
END$$

-- ---------------- MOTOCICLETA ----------------

DROP TRIGGER IF EXISTS `trg_motocicleta_bi`$$
CREATE TRIGGER `trg_motocicleta_bi`
BEFORE INSERT ON `MOTOCICLETA`
FOR EACH ROW
BEGIN
    SET NEW.`Placa`              = UPPER(TRIM(NEW.`Placa`));
    SET NEW.`Estado_motocicleta` = UPPER(TRIM(NEW.`Estado_motocicleta`));

    CALL sp_validar_datos_motocicleta(
        NEW.`Placa`, NEW.`Marca`, NEW.`modelo`, NEW.`Año`, NEW.`Cilindraje`,
        NEW.`Kilometraje_actual`, NEW.`Precio_alquiler`, NEW.`Estado_motocicleta`);

    -- La PK es (Placa, Numero_documento), pero una placa identifica una sola moto.
    IF EXISTS (SELECT 1 FROM `MOTOCICLETA` WHERE `Placa` = NEW.`Placa`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe una motocicleta con esa placa.', MYSQL_ERRNO = 45011;
    END IF;

    -- Cada poliza cubre una sola motocicleta.
    IF EXISTS (SELECT 1 FROM `MOTOCICLETA` WHERE `Id_seguro` = NEW.`Id_seguro`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El seguro ya esta asociado a otra motocicleta.', MYSQL_ERRNO = 45016;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_motocicleta_bu`$$
CREATE TRIGGER `trg_motocicleta_bu`
BEFORE UPDATE ON `MOTOCICLETA`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_motocicleta` = UPPER(TRIM(NEW.`Estado_motocicleta`));

    IF NEW.`Placa` <> OLD.`Placa` OR NEW.`Numero_documento` <> OLD.`Numero_documento` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La placa y el propietario no se pueden modificar.', MYSQL_ERRNO = 45026;
    END IF;

    CALL sp_validar_datos_motocicleta(
        NEW.`Placa`, NEW.`Marca`, NEW.`modelo`, NEW.`Año`, NEW.`Cilindraje`,
        NEW.`Kilometraje_actual`, NEW.`Precio_alquiler`, NEW.`Estado_motocicleta`);

    IF NEW.`Kilometraje_actual` < OLD.`Kilometraje_actual` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El kilometraje no puede disminuir.', MYSQL_ERRNO = 45013;
    END IF;

    IF NEW.`Id_seguro` <> OLD.`Id_seguro`
       AND EXISTS (SELECT 1 FROM `MOTOCICLETA` WHERE `Id_seguro` = NEW.`Id_seguro`) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El seguro ya esta asociado a otra motocicleta.', MYSQL_ERRNO = 45016;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_motocicleta_au`$$
CREATE TRIGGER `trg_motocicleta_au`
AFTER UPDATE ON `MOTOCICLETA`
FOR EACH ROW
BEGIN
    CALL sp_auditar_estado('MOTOCICLETA', NEW.`Placa`,
                           OLD.`Estado_motocicleta`, NEW.`Estado_motocicleta`);
END$$

-- ---------------- SOLICITUD ----------------

DROP TRIGGER IF EXISTS `trg_solicitud_bi`$$
CREATE TRIGGER `trg_solicitud_bi`
BEFORE INSERT ON `SOLICITUD`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_solicitud` = UPPER(TRIM(NEW.`Estado_solicitud`));
    SET NEW.`Placa`            = UPPER(TRIM(NEW.`Placa`));

    IF NEW.`Estado_solicitud` <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Una solicitud nueva debe crearse en estado PENDIENTE.', MYSQL_ERRNO = 45021;
    END IF;

    -- Columnas redundantes heredadas del modelo:
    --   Numero_documento   = arrendatario   = Numero_documento1  (lado CONTRATO)
    --   Numero_documento2  = arrendador     = Numero_documento11 (lado CONTRATO)
    IF NEW.`Numero_documento1` <> NEW.`Numero_documento`
       OR NEW.`Numero_documento11` <> NEW.`Numero_documento2` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Documentos de la solicitud y del contrato no coinciden.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Numero_documento` = NEW.`Numero_documento2` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un usuario no puede alquilar su propia motocicleta.', MYSQL_ERRNO = 45023;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_solicitud_bu`$$
CREATE TRIGGER `trg_solicitud_bu`
BEFORE UPDATE ON `SOLICITUD`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_solicitud` = UPPER(TRIM(NEW.`Estado_solicitud`));

    IF NEW.`Id_solicitud` <> OLD.`Id_solicitud`
       OR NEW.`Numero_documento` <> OLD.`Numero_documento`
       OR NEW.`Placa` <> OLD.`Placa`
       OR NEW.`Numero_documento2` <> OLD.`Numero_documento2`
       OR NEW.`id_contrato` <> OLD.`id_contrato`
       OR NEW.`Numero_documento1` <> OLD.`Numero_documento1`
       OR NEW.`Numero_documento11` <> OLD.`Numero_documento11` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las llaves de la solicitud no se pueden modificar.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Estado_solicitud` NOT IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de solicitud invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF NEW.`Estado_solicitud` <> OLD.`Estado_solicitud`
       AND OLD.`Estado_solicitud` <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo una solicitud PENDIENTE puede cambiar de estado.', MYSQL_ERRNO = 45021;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_solicitud_au`$$
CREATE TRIGGER `trg_solicitud_au`
AFTER UPDATE ON `SOLICITUD`
FOR EACH ROW
BEGIN
    CALL sp_auditar_estado('SOLICITUD', CAST(NEW.`Id_solicitud` AS CHAR),
                           OLD.`Estado_solicitud`, NEW.`Estado_solicitud`);
END$$

-- ---------------- CONTRATO ----------------

DROP TRIGGER IF EXISTS `trg_contrato_bi`$$
CREATE TRIGGER `trg_contrato_bi`
BEFORE INSERT ON `CONTRATO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_contrato` = UPPER(TRIM(NEW.`Estado_contrato`));
    SET NEW.`Placa`           = UPPER(TRIM(NEW.`Placa`));

    IF NEW.`Estado_contrato` <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un contrato nuevo debe crearse en estado PENDIENTE.', MYSQL_ERRNO = 45031;
    END IF;

    -- Columnas redundantes heredadas del modelo:
    --   Numero_documento  = arrendatario = Numero_documento2  (lado SOLICITUD)
    --   Numero_documento1 = arrendador   = Numero_documento21 (lado SOLICITUD)
    IF NEW.`Numero_documento2` <> NEW.`Numero_documento`
       OR NEW.`Numero_documento21` <> NEW.`Numero_documento1` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Documentos del contrato y de la solicitud no coinciden.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Numero_documento` = NEW.`Numero_documento1` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un usuario no puede alquilar su propia motocicleta.', MYSQL_ERRNO = 45023;
    END IF;

    IF NEW.`Fecha_fin` < NEW.`Fecha_inicio` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de fin no puede ser anterior a la de inicio.', MYSQL_ERRNO = 45022;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_contrato_bu`$$
CREATE TRIGGER `trg_contrato_bu`
BEFORE UPDATE ON `CONTRATO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado_contrato` = UPPER(TRIM(NEW.`Estado_contrato`));

    IF NEW.`id_contrato` <> OLD.`id_contrato`
       OR NEW.`Numero_documento` <> OLD.`Numero_documento`
       OR NEW.`Numero_documento1` <> OLD.`Numero_documento1`
       OR NEW.`Id_solicitud` <> OLD.`Id_solicitud`
       OR NEW.`Numero_documento2` <> OLD.`Numero_documento2`
       OR NEW.`Placa` <> OLD.`Placa`
       OR NEW.`Numero_documento21` <> OLD.`Numero_documento21` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las llaves del contrato no se pueden modificar.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Estado_contrato` NOT IN ('PENDIENTE', 'VIGENTE', 'FINALIZADO', 'CANCELADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de contrato invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF NEW.`Estado_contrato` <> OLD.`Estado_contrato` AND NOT (
           (OLD.`Estado_contrato` = 'PENDIENTE' AND NEW.`Estado_contrato` IN ('VIGENTE', 'CANCELADO'))
        OR (OLD.`Estado_contrato` = 'VIGENTE'   AND NEW.`Estado_contrato` IN ('FINALIZADO', 'CANCELADO'))
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transicion de estado de contrato no permitida.', MYSQL_ERRNO = 45031;
    END IF;

    -- Las fechas solo se pueden cambiar mientras el contrato esta PENDIENTE.
    IF (NEW.`Fecha_inicio` <> OLD.`Fecha_inicio` OR NEW.`Fecha_fin` <> OLD.`Fecha_fin`)
       AND OLD.`Estado_contrato` <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las fechas solo se modifican en contratos PENDIENTES.', MYSQL_ERRNO = 45031;
    END IF;

    IF NEW.`Fecha_fin` < NEW.`Fecha_inicio` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de fin no puede ser anterior a la de inicio.', MYSQL_ERRNO = 45022;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_contrato_au`$$
CREATE TRIGGER `trg_contrato_au`
AFTER UPDATE ON `CONTRATO`
FOR EACH ROW
BEGIN
    CALL sp_auditar_estado('CONTRATO', CAST(NEW.`id_contrato` AS CHAR),
                           OLD.`Estado_contrato`, NEW.`Estado_contrato`);
END$$

-- ---------------- PAGO ----------------

DROP TRIGGER IF EXISTS `trg_pago_bi`$$
CREATE TRIGGER `trg_pago_bi`
BEFORE INSERT ON `PAGO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado`      = UPPER(TRIM(NEW.`Estado`));
    SET NEW.`Metodo_pago` = UPPER(TRIM(NEW.`Metodo_pago`));

    IF NEW.`Estado` NOT IN ('PENDIENTE', 'PAGADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un pago nuevo debe ser PENDIENTE o PAGADO.', MYSQL_ERRNO = 45041;
    END IF;

    IF NEW.`Monto` <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El monto del pago debe ser mayor que cero.', MYSQL_ERRNO = 45042;
    END IF;

    IF NEW.`Metodo_pago` NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'PSE', 'NEQUI', 'POR_DEFINIR')
       OR (NEW.`Estado` = 'PAGADO' AND NEW.`Metodo_pago` = 'POR_DEFINIR') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Metodo de pago invalido.', MYSQL_ERRNO = 45042;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `CONTRATO`
                    WHERE `id_contrato` = NEW.`id_contrato`
                      AND `Numero_documento` = NEW.`Numero_documento`
                      AND `Numero_documento1` = NEW.`Numero_documento1`
                      AND `Estado_contrato` = 'VIGENTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se registran pagos de contratos VIGENTES.', MYSQL_ERRNO = 45031;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_pago_bu`$$
CREATE TRIGGER `trg_pago_bu`
BEFORE UPDATE ON `PAGO`
FOR EACH ROW
BEGIN
    SET NEW.`Estado`      = UPPER(TRIM(NEW.`Estado`));
    SET NEW.`Metodo_pago` = UPPER(TRIM(NEW.`Metodo_pago`));

    IF NEW.`Id_pago` <> OLD.`Id_pago`
       OR NEW.`id_contrato` <> OLD.`id_contrato`
       OR NEW.`Numero_documento` <> OLD.`Numero_documento`
       OR NEW.`Numero_documento1` <> OLD.`Numero_documento1` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las llaves del pago no se pueden modificar.', MYSQL_ERRNO = 45026;
    END IF;

    IF NEW.`Monto` <> OLD.`Monto` THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El monto de un pago no se puede modificar.', MYSQL_ERRNO = 45042;
    END IF;

    IF NEW.`Estado` NOT IN ('PENDIENTE', 'PAGADO', 'VENCIDO', 'CANCELADO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado de pago invalido.', MYSQL_ERRNO = 45007;
    END IF;

    IF NEW.`Estado` <> OLD.`Estado` AND NOT (
           (OLD.`Estado` = 'PENDIENTE' AND NEW.`Estado` IN ('PAGADO', 'VENCIDO', 'CANCELADO'))
        OR (OLD.`Estado` = 'VENCIDO'   AND NEW.`Estado` IN ('PAGADO', 'CANCELADO'))
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transicion de estado de pago no permitida.', MYSQL_ERRNO = 45041;
    END IF;

    IF NEW.`Metodo_pago` NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'PSE', 'NEQUI', 'POR_DEFINIR')
       OR (NEW.`Estado` = 'PAGADO' AND NEW.`Metodo_pago` = 'POR_DEFINIR') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Metodo de pago invalido.', MYSQL_ERRNO = 45042;
    END IF;
END$$

DROP TRIGGER IF EXISTS `trg_pago_au`$$
CREATE TRIGGER `trg_pago_au`
AFTER UPDATE ON `PAGO`
FOR EACH ROW
BEGIN
    CALL sp_auditar_estado('PAGO', CAST(NEW.`Id_pago` AS CHAR),
                           OLD.`Estado`, NEW.`Estado`);
END$$

-- ---------------- INFO_MANTENIMIENTO ----------------

DROP TRIGGER IF EXISTS `trg_mantenimiento_bi`$$
CREATE TRIGGER `trg_mantenimiento_bi`
BEFORE INSERT ON `INFO_MANTENIMIENTO`
FOR EACH ROW
BEGIN
    IF NEW.`Kilometraje_al_momento` < 0 OR NEW.`Costo` < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Kilometraje y costo no pueden ser negativos.', MYSQL_ERRNO = 45050;
    END IF;

    IF NEW.`Fecha_mantenimiento` > CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha del mantenimiento no puede ser futura.', MYSQL_ERRNO = 45050;
    END IF;

    IF TRIM(NEW.`Tipo_mantenimiento`) = '' OR TRIM(NEW.`Descripcion_mantenimiento`) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Tipo y descripcion del mantenimiento son obligatorios.', MYSQL_ERRNO = 45050;
    END IF;
END$$

-- Mantiene actualizado el kilometraje de la moto (nunca lo disminuye).
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
-- 5. PROCEDIMIENTOS DE NEGOCIO
-- ================================================================
-- Patron de excepciones usado en todos:
--   DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
-- Cualquier error (de un SIGNAL propio, de un trigger o del motor) deshace
-- la transaccion y se re-lanza al backend con su mensaje y MYSQL_ERRNO.

-- ---------------- USUARIOS ----------------

-- Registra el usuario y su fila de subtipo segun el rol, en una transaccion.
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
            SET MESSAGE_TEXT = 'Ya existe un usuario con ese numero de documento.', MYSQL_ERRNO = 45006;
    END;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_rol = UPPER(TRIM(p_rol));

    IF v_rol IS NULL OR v_rol NOT IN ('ADMIN', 'ARRENDADOR', 'ARRENDATARIO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rol invalido (ADMIN, ARRENDADOR, ARRENDATARIO).', MYSQL_ERRNO = 45002;
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

-- Devuelve los datos para el login. El backend compara el hash.
-- Devuelve 0 filas si el correo no existe: el backend debe responder el
-- mismo mensaje generico que para una contrasena incorrecta.
DROP PROCEDURE IF EXISTS `sp_obtener_credenciales`$$
CREATE PROCEDURE `sp_obtener_credenciales`(IN p_correo CHAR(50))
BEGIN
    SELECT `Numero_documento`,
           `Tipo_documento`,
           `Contraseña` AS `Contrasena_hash`,
           `Rol_usuario`,
           `Estado`,
           fn_nombre_completo(`Numero_documento`) AS `Nombre_completo`
      FROM `USUARIO`
     WHERE `Correo` = LOWER(TRIM(p_correo));
END$$

-- Un administrador activa, inactiva o bloquea a un usuario.
DROP PROCEDURE IF EXISTS `sp_cambiar_estado_usuario`$$
CREATE PROCEDURE `sp_cambiar_estado_usuario`(
    IN p_documento_admin INT,
    IN p_documento       INT,
    IN p_estado          CHAR(20)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF NOT EXISTS (SELECT 1 FROM `ADMINISTRADOR` WHERE `Numero_documento` = p_documento_admin)
       OR fn_usuario_activo(p_documento_admin) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo un administrador activo puede cambiar estados.', MYSQL_ERRNO = 45025;
    END IF;

    IF p_documento_admin = p_documento THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un administrador no puede cambiar su propio estado.', MYSQL_ERRNO = 45025;
    END IF;

    START TRANSACTION;

    UPDATE `USUARIO`
       SET `Estado` = p_estado
     WHERE `Numero_documento` = p_documento;

    IF ROW_COUNT() = 0 AND NOT EXISTS (SELECT 1 FROM `USUARIO` WHERE `Numero_documento` = p_documento) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El usuario no existe.', MYSQL_ERRNO = 45001;
    END IF;

    COMMIT;
END$$

-- ---------------- MOTOCICLETAS ----------------

-- Registra el seguro y la motocicleta juntos (el front los envia juntos).
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
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF NOT EXISTS (SELECT 1 FROM `ARRENDADOR` WHERE `Numero_documento` = p_arrendador)
       OR fn_usuario_activo(p_arrendador) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El arrendador no existe o no esta activo.', MYSQL_ERRNO = 45001;
    END IF;

    START TRANSACTION;

    SELECT COALESCE(MAX(`Id_seguro`), 0) + 1
      INTO p_id_seguro
      FROM `SEGURO`
       FOR UPDATE;

    INSERT INTO `SEGURO`
        (`Estado_poliza`, `Id_seguro`, `numero_poliza`, `Aseguradora`, `Fecha_inicio_vigencia`)
    VALUES
        (p_estado_poliza, p_id_seguro, p_numero_poliza, p_aseguradora, p_fecha_inicio_vigencia);

    INSERT INTO `MOTOCICLETA`
        (`Estado_motocicleta`, `Kilometraje_actual`, `Precio_alquiler`, `Cilindraje`, `Año`,
         `modelo`, `Marca`, `Placa`, `Numero_documento`, `Id_seguro`)
    VALUES
        ('DISPONIBLE', p_kilometraje, p_precio_alquiler, p_cilindraje, p_anio,
         p_modelo, p_marca, p_placa, p_arrendador, p_id_seguro);

    COMMIT;
END$$

-- El arrendador pone su moto DISPONIBLE, en MANTENIMIENTO o INACTIVA.
-- ARRENDADA solo la asignan los procesos de contrato.
DROP PROCEDURE IF EXISTS `sp_cambiar_estado_motocicleta`$$
CREATE PROCEDURE `sp_cambiar_estado_motocicleta`(
    IN p_arrendador INT,
    IN p_placa      CHAR(10),
    IN p_estado     CHAR(20)
)
BEGIN
    DECLARE v_placa  CHAR(10);
    DECLARE v_nuevo  CHAR(20);
    DECLARE v_actual CHAR(20) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_placa = UPPER(TRIM(p_placa));
    SET v_nuevo = UPPER(TRIM(p_estado));

    IF v_nuevo IS NULL OR v_nuevo NOT IN ('DISPONIBLE', 'MANTENIMIENTO', 'INACTIVA') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado no permitido (DISPONIBLE, MANTENIMIENTO, INACTIVA).', MYSQL_ERRNO = 45007;
    END IF;

    START TRANSACTION;

    SELECT `Estado_motocicleta`
      INTO v_actual
      FROM `MOTOCICLETA`
     WHERE `Placa` = v_placa
       AND `Numero_documento` = p_arrendador
       FOR UPDATE;

    IF v_actual IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no existe para este arrendador.', MYSQL_ERRNO = 45010;
    END IF;

    IF v_actual = 'ARRENDADA' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta tiene un contrato en curso.', MYSQL_ERRNO = 45014;
    END IF;

    IF v_nuevo = 'INACTIVA' AND EXISTS (
        SELECT 1 FROM `CONTRATO`
         WHERE `Placa` = v_placa
           AND `Numero_documento21` = p_arrendador
           AND `Estado_contrato` = 'VIGENTE'
           AND `Fecha_fin` >= CURDATE()) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede inactivar: tiene contratos vigentes futuros.', MYSQL_ERRNO = 45014;
    END IF;

    UPDATE `MOTOCICLETA`
       SET `Estado_motocicleta` = v_nuevo
     WHERE `Placa` = v_placa
       AND `Numero_documento` = p_arrendador;

    COMMIT;
END$$

-- Catalogo de motos que se pueden alquilar en un rango de fechas.
DROP PROCEDURE IF EXISTS `sp_listar_motos_disponibles`$$
CREATE PROCEDURE `sp_listar_motos_disponibles`(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin    DATE
)
BEGIN
    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL
       OR p_fecha_inicio < CURDATE() OR p_fecha_fin < p_fecha_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45022;
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

-- Registra un mantenimiento. El trigger trg_mantenimiento_ai actualiza el
-- kilometraje de la moto.
DROP PROCEDURE IF EXISTS `sp_registrar_mantenimiento`$$
CREATE PROCEDURE `sp_registrar_mantenimiento`(
    IN  p_arrendador  INT,
    IN  p_placa       CHAR(10),
    IN  p_fecha       DATE,
    IN  p_tipo        CHAR(50),
    IN  p_kilometraje INT,
    IN  p_descripcion VARCHAR(500),
    IN  p_costo       INT,
    OUT p_id_mantenimiento INT
)
BEGIN
    DECLARE v_placa CHAR(10);
    DECLARE v_existe TINYINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_placa = UPPER(TRIM(p_placa));

    START TRANSACTION;

    -- Bloquea la moto mientras se registra el mantenimiento.
    SELECT COUNT(*)
      INTO v_existe
      FROM `MOTOCICLETA`
     WHERE `Placa` = v_placa
       AND `Numero_documento` = p_arrendador
       FOR UPDATE;

    IF v_existe = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no existe para este arrendador.', MYSQL_ERRNO = 45010;
    END IF;

    SELECT COALESCE(MAX(`Id_mantenimiento`), 0) + 1
      INTO p_id_mantenimiento
      FROM `INFO_MANTENIMIENTO`
       FOR UPDATE;

    INSERT INTO `INFO_MANTENIMIENTO`
        (`Id_mantenimiento`, `Fecha_mantenimiento`, `Tipo_mantenimiento`, `Kilometraje_al_momento`,
         `Descripcion_mantenimiento`, `Costo`, `Placa`, `Numero_documento`)
    VALUES
        (p_id_mantenimiento, IFNULL(p_fecha, CURDATE()), p_tipo, p_kilometraje,
         p_descripcion, p_costo, v_placa, p_arrendador);

    COMMIT;
END$$

-- ---------------- SOLICITUDES ----------------

-- Crea la solicitud y su contrato PENDIENTE en una sola transaccion.
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
            SET MESSAGE_TEXT = 'El arrendatario no existe o no esta activo.', MYSQL_ERRNO = 45001;
    END IF;

    IF p_fecha_inicio IS NULL OR p_fecha_fin IS NULL
       OR p_fecha_inicio < CURDATE() OR p_fecha_fin < p_fecha_inicio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45022;
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
            SET MESSAGE_TEXT = 'La motocicleta no existe.', MYSQL_ERRNO = 45010;
    ELSEIF v_motos > 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La placa esta registrada a mas de un arrendador.', MYSQL_ERRNO = 45011;
    END IF;

    IF v_arrendador = p_arrendatario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un usuario no puede alquilar su propia motocicleta.', MYSQL_ERRNO = 45023;
    END IF;

    IF fn_moto_disponible(v_placa, v_arrendador, p_fecha_inicio, p_fecha_fin, NULL) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La motocicleta no esta disponible en esas fechas.', MYSQL_ERRNO = 45014;
    END IF;

    IF EXISTS (SELECT 1 FROM `SOLICITUD`
                WHERE `Numero_documento` = p_arrendatario
                  AND `Placa` = v_placa
                  AND `Numero_documento2` = v_arrendador
                  AND `Estado_solicitud` = 'PENDIENTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya tiene una solicitud pendiente para esta motocicleta.', MYSQL_ERRNO = 45024;
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

    INSERT INTO `SOLICITUD`
        (`Id_solicitud`, `Fecha_solicitud`, `Estado_solicitud`, `Mensaje_solicitud`,
         `Numero_documento`, `Placa`, `Numero_documento2`,
         `id_contrato`, `Numero_documento1`, `Numero_documento11`)
    VALUES
        (p_id_solicitud, CURDATE(), 'PENDIENTE', IFNULL(p_mensaje, ''),
         p_arrendatario, v_placa, v_arrendador,
         p_id_contrato, p_arrendatario, v_arrendador);

    SET FOREIGN_KEY_CHECKS = v_fk_previo;

    COMMIT;
END$$

-- El arrendatario cancela su propia solicitud PENDIENTE.
DROP PROCEDURE IF EXISTS `sp_cancelar_solicitud`$$
CREATE PROCEDURE `sp_cancelar_solicitud`(
    IN p_id_solicitud INT,
    IN p_arrendatario INT
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

    SELECT `Estado_solicitud`, `Numero_documento`, `id_contrato`
      INTO v_estado, v_arrendatario, v_id_contrato
      FROM `SOLICITUD`
     WHERE `Id_solicitud` = p_id_solicitud
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no existe.', MYSQL_ERRNO = 45020;
    END IF;

    IF v_arrendatario <> p_arrendatario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no pertenece a este arrendatario.', MYSQL_ERRNO = 45025;
    END IF;

    IF v_estado <> 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se pueden cancelar solicitudes PENDIENTES.', MYSQL_ERRNO = 45021;
    END IF;

    UPDATE `SOLICITUD`
       SET `Estado_solicitud` = 'CANCELADA'
     WHERE `Id_solicitud` = p_id_solicitud;

    UPDATE `CONTRATO`
       SET `Estado_contrato` = 'CANCELADO'
     WHERE `id_contrato` = v_id_contrato;

    COMMIT;
END$$

-- El arrendador aprueba o rechaza una solicitud.
--   RECHAZADA: solicitud RECHAZADA, contrato CANCELADO.
--   APROBADA : solicitud APROBADA, contrato VIGENTE, se genera el PAGO
--              PENDIENTE por el valor total, la moto pasa a ARRENDADA si el
--              alquiler empieza hoy, y con un CURSOR se rechazan las demas
--              solicitudes PENDIENTES de la misma moto que se cruzan en fechas.
-- p_id_pago devuelve el pago generado (NULL si se rechazo).
--
-- Nota: PAGO.fecha_pago y PAGO.Metodo_pago son NOT NULL en el modelo. Mientras
-- el pago esta PENDIENTE, fecha_pago guarda la fecha en que se genero y
-- Metodo_pago vale 'POR_DEFINIR'; sp_registrar_pago los actualiza al pagar.
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
            SET MESSAGE_TEXT = 'La decision debe ser APROBADA o RECHAZADA.', MYSQL_ERRNO = 45021;
    END IF;

    START TRANSACTION;

    SELECT `Estado_solicitud`, `Numero_documento`, `Numero_documento2`, `Placa`, `id_contrato`
      INTO v_estado, v_arrendatario, v_arrendador, v_placa, v_id_contrato
      FROM `SOLICITUD`
     WHERE `Id_solicitud` = p_id_solicitud;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no existe.', MYSQL_ERRNO = 45020;
    END IF;

    IF v_arrendador <> p_arrendador THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La solicitud no corresponde a una moto de este arrendador.', MYSQL_ERRNO = 45025;
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
            SET MESSAGE_TEXT = 'La solicitud ya fue respondida o cancelada.', MYSQL_ERRNO = 45021;
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

        UPDATE `CONTRATO`
           SET `Estado_contrato` = 'CANCELADO'
         WHERE `id_contrato` = v_id_contrato;

    ELSE

        IF v_fecha_inicio < CURDATE() THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La fecha de inicio ya paso; la solicitud expiro.', MYSQL_ERRNO = 45022;
        END IF;

        IF fn_usuario_activo(v_arrendatario) = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'El arrendatario ya no esta activo.', MYSQL_ERRNO = 45001;
        END IF;

        IF fn_moto_disponible(v_placa, v_arrendador, v_fecha_inicio, v_fecha_fin, v_id_contrato) = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La motocicleta ya no esta disponible en esas fechas.', MYSQL_ERRNO = 45014;
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

        -- CURSOR: rechaza las otras solicitudes pendientes que se cruzan.
        -- Va en un bloque propio para que el handler NOT FOUND solo afecte
        -- al FETCH del cursor y no a los SELECT ... INTO de arriba.
        BEGIN
            DECLARE v_fin           TINYINT DEFAULT 0;
            DECLARE v_otra_solicitud INT;
            DECLARE v_otro_contrato  INT;

            DECLARE cur_conflictos CURSOR FOR
                SELECT s.`Id_solicitud`, s.`id_contrato`
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
                FETCH cur_conflictos INTO v_otra_solicitud, v_otro_contrato;
                IF v_fin = 1 THEN
                    LEAVE lectura;
                END IF;

                UPDATE `SOLICITUD`
                   SET `Estado_solicitud` = 'RECHAZADA'
                 WHERE `Id_solicitud` = v_otra_solicitud;

                UPDATE `CONTRATO`
                   SET `Estado_contrato` = 'CANCELADO'
                 WHERE `id_contrato` = v_otro_contrato;
            END LOOP lectura;

            CLOSE cur_conflictos;
        END;

    END IF;

    COMMIT;
END$$

-- ---------------- PAGOS ----------------

-- El arrendatario paga un pago PENDIENTE o VENCIDO de un contrato VIGENTE.
DROP PROCEDURE IF EXISTS `sp_registrar_pago`$$
CREATE PROCEDURE `sp_registrar_pago`(
    IN p_id_pago      INT,
    IN p_arrendatario INT,
    IN p_metodo_pago  CHAR(20)
)
BEGIN
    DECLARE v_metodo       CHAR(20);
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendatario INT;
    DECLARE v_id_contrato  INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET v_metodo = UPPER(TRIM(p_metodo_pago));

    IF v_metodo IS NULL OR v_metodo NOT IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'PSE', 'NEQUI') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Metodo de pago invalido.', MYSQL_ERRNO = 45042;
    END IF;

    START TRANSACTION;

    SELECT `Estado`, `Numero_documento`, `id_contrato`
      INTO v_estado, v_arrendatario, v_id_contrato
      FROM `PAGO`
     WHERE `Id_pago` = p_id_pago
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El pago no existe.', MYSQL_ERRNO = 45040;
    END IF;

    IF v_arrendatario <> p_arrendatario THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El pago no pertenece a este arrendatario.', MYSQL_ERRNO = 45025;
    END IF;

    IF v_estado NOT IN ('PENDIENTE', 'VENCIDO') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se pueden pagar pagos PENDIENTES o VENCIDOS.', MYSQL_ERRNO = 45041;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM `CONTRATO`
                    WHERE `id_contrato` = v_id_contrato
                      AND `Estado_contrato` = 'VIGENTE') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato del pago no esta VIGENTE.', MYSQL_ERRNO = 45031;
    END IF;

    UPDATE `PAGO`
       SET `Estado`      = 'PAGADO',
           `Metodo_pago` = v_metodo,
           `fecha_pago`  = CURDATE()
     WHERE `Id_pago` = p_id_pago;

    COMMIT;
END$$

-- ---------------- CONTRATOS ----------------

-- El arrendador finaliza un contrato VIGENTE ya iniciado y totalmente pagado,
-- registrando el kilometraje con que se devolvio la moto.
DROP PROCEDURE IF EXISTS `sp_finalizar_contrato`$$
CREATE PROCEDURE `sp_finalizar_contrato`(
    IN p_id_contrato       INT,
    IN p_arrendador        INT,
    IN p_kilometraje_final INT
)
BEGIN
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendador   INT;
    DECLARE v_placa        CHAR(10);
    DECLARE v_fecha_inicio DATE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_kilometraje_final IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El kilometraje final es obligatorio.', MYSQL_ERRNO = 45012;
    END IF;

    START TRANSACTION;

    SELECT `Estado_contrato`, `Numero_documento1`, `Placa`, `Fecha_inicio`
      INTO v_estado, v_arrendador, v_placa, v_fecha_inicio
      FROM `CONTRATO`
     WHERE `id_contrato` = p_id_contrato
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato no existe.', MYSQL_ERRNO = 45030;
    END IF;

    IF v_arrendador <> p_arrendador THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato no pertenece a este arrendador.', MYSQL_ERRNO = 45025;
    END IF;

    IF v_estado <> 'VIGENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se pueden finalizar contratos VIGENTES.', MYSQL_ERRNO = 45031;
    END IF;

    IF v_fecha_inicio > CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato aun no ha iniciado; use cancelar.', MYSQL_ERRNO = 45031;
    END IF;

    IF fn_saldo_pendiente(p_id_contrato) > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato tiene pagos pendientes.', MYSQL_ERRNO = 45032;
    END IF;

    UPDATE `CONTRATO`
       SET `Estado_contrato` = 'FINALIZADO'
     WHERE `id_contrato` = p_id_contrato;

    -- trg_motocicleta_bu rechaza un kilometraje menor al actual.
    UPDATE `MOTOCICLETA`
       SET `Kilometraje_actual` = p_kilometraje_final,
           `Estado_motocicleta` = IF(`Estado_motocicleta` = 'ARRENDADA', 'DISPONIBLE', `Estado_motocicleta`)
     WHERE `Placa` = v_placa
       AND `Numero_documento` = p_arrendador;

    COMMIT;
END$$

-- Arrendatario o arrendador cancelan un contrato VIGENTE que aun no inicia.
-- Los pagos PENDIENTES/VENCIDOS se cancelan; los PAGADOS se conservan
-- (la devolucion de dinero se gestiona fuera de la base de datos).
DROP PROCEDURE IF EXISTS `sp_cancelar_contrato`$$
CREATE PROCEDURE `sp_cancelar_contrato`(
    IN p_id_contrato INT,
    IN p_documento   INT
)
BEGIN
    DECLARE v_estado       CHAR(20) DEFAULT NULL;
    DECLARE v_arrendatario INT;
    DECLARE v_arrendador   INT;
    DECLARE v_fecha_inicio DATE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT `Estado_contrato`, `Numero_documento`, `Numero_documento1`, `Fecha_inicio`
      INTO v_estado, v_arrendatario, v_arrendador, v_fecha_inicio
      FROM `CONTRATO`
     WHERE `id_contrato` = p_id_contrato
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato no existe.', MYSQL_ERRNO = 45030;
    END IF;

    IF p_documento NOT IN (v_arrendatario, v_arrendador) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El usuario no es parte del contrato.', MYSQL_ERRNO = 45025;
    END IF;

    IF v_estado <> 'VIGENTE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se pueden cancelar contratos VIGENTES.', MYSQL_ERRNO = 45031;
    END IF;

    IF v_fecha_inicio <= CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El contrato ya inicio y no puede cancelarse.', MYSQL_ERRNO = 45033;
    END IF;

    UPDATE `CONTRATO`
       SET `Estado_contrato` = 'CANCELADO'
     WHERE `id_contrato` = p_id_contrato;

    UPDATE `PAGO`
       SET `Estado` = 'CANCELADO'
     WHERE `id_contrato` = p_id_contrato
       AND `Estado` IN ('PENDIENTE', 'VENCIDO');

    COMMIT;
END$$

-- ---------------- PROCESOS Y REPORTES ----------------

-- Proceso diario (lo puede llamar el evento de la seccion 6 o el backend):
--   1. Pagos PENDIENTES cuya fecha de vencimiento ya paso -> VENCIDO.
--   2. CURSOR: solicitudes PENDIENTES cuya fecha de inicio ya paso
--      -> solicitud CANCELADA y contrato CANCELADO.
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
        DECLARE v_id_contrato  INT;

        DECLARE cur_expiradas CURSOR FOR
            SELECT s.`Id_solicitud`, s.`id_contrato`
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
            FETCH cur_expiradas INTO v_id_solicitud, v_id_contrato;
            IF v_fin = 1 THEN
                LEAVE lectura;
            END IF;

            UPDATE `SOLICITUD`
               SET `Estado_solicitud` = 'CANCELADA'
             WHERE `Id_solicitud` = v_id_solicitud;

            UPDATE `CONTRATO`
               SET `Estado_contrato` = 'CANCELADO'
             WHERE `id_contrato` = v_id_contrato;

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

-- Reporte por moto de un arrendador en un rango de fechas, armado con CURSOR:
-- contratos, ingresos (pagos PAGADOS), costos de mantenimiento y utilidad.
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
            SET MESSAGE_TEXT = 'El arrendador no existe.', MYSQL_ERRNO = 45001;
    END IF;

    IF p_desde IS NULL OR p_hasta IS NULL OR p_hasta < p_desde THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rango de fechas invalido.', MYSQL_ERRNO = 45022;
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
-- 6. EVENTO PROGRAMADO (opcional)
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
-- 7. EJEMPLOS DE USO (para el backend; no se ejecutan, estan comentados)
-- ================================================================
-- SET @usuario_app = 1001;
--
-- CALL sp_registrar_usuario('CC', 1001, 'Ana', NULL, 'Rojas', NULL,
--                           300123456, 'ana@correo.com', '<hash>', 'ARRENDADOR');
-- CALL sp_registrar_usuario('CC', 2002, 'Luis', NULL, 'Diaz', NULL,
--                           310123456, 'luis@correo.com', '<hash>', 'ARRENDATARIO');
--
-- CALL sp_obtener_credenciales('ana@correo.com');
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
-- CALL sp_finalizar_contrato(@id_con, 1001, 1650);
--
-- CALL sp_reporte_ingresos_arrendador(1001, '2026-01-01', '2026-12-31');
-- CALL sp_actualizar_estados_diarios(@p, @s, @m); SELECT @p, @s, @m;
--
-- Nota sobre Telefono INT: un celular colombiano de 10 digitos (3001234567)
-- supera el maximo de INT (2147483647). Se recomienda cambiar la columna a
-- VARCHAR(20) o BIGINT en el script de tablas antes de poblar datos reales.
