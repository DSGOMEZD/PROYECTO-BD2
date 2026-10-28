-- ================================================================
-- Conversión de Oracle SQL Developer Data Modeler a MySQL 8 / MariaDB
-- Modelo: sistema de alquiler de motocicletas
--
-- Antes de ejecutar: seleccione su base de datos en MySQL Workbench
-- o agregue una línea USE nombre_de_la_base_de_datos;
-- ================================================================

SET NAMES utf8mb4;

CREATE TABLE `USUARIO` (
    `Tipo_documento`   CHAR(20) NOT NULL COMMENT 'Tipo de documento de identificacion (CC, CE, etc.).',
    `Numero_documento` INT NOT NULL COMMENT 'Numero del documento de identificacion unico para identificar al usuario.',
    `Primer_nombre`    CHAR(50) NOT NULL COMMENT 'Primer nombre del usuario.',
    `Segundo_nombre`   CHAR(50) NULL COMMENT 'Segundo nombre del usuario.',
    `Primer_apellido`  CHAR(50) NOT NULL COMMENT 'Primer apellido del usuario.',
    `Segundo_apellido` CHAR(50) NULL COMMENT 'Segundo apellido del usuario.',
    `Telefono`         INT NOT NULL COMMENT 'Telefono de contacto del usuario.',
    `Correo`           CHAR(50) NOT NULL COMMENT 'Correo para registro, inicio de sesion y contacto.',
    `Fecha_registro`   DATE NOT NULL COMMENT 'Fecha en la que se registro el usuario.',
    `Estado`           CHAR(20) NOT NULL COMMENT 'Estado en el que se encuentra el perfil del usuario.',
    `Contraseña`       VARCHAR(255) NOT NULL COMMENT 'Contraseña o hash de contraseña del usuario.',
    `Rol_usuario`      CHAR(20) NOT NULL COMMENT 'Rol con el que ingreso el usuario al sistema.',
    CONSTRAINT `USUARIO_PK` PRIMARY KEY (`Numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ADMINISTRADOR` (
    `Numero_documento` INT NOT NULL COMMENT 'Numero del documento de identificacion unico para identificar al usuario.',
    CONSTRAINT `ADMINISTRADOR_PK` PRIMARY KEY (`Numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ARRENDADOR` (
    `Numero_documento` INT NOT NULL COMMENT 'Numero del documento de identificacion unico para identificar al usuario.',
    CONSTRAINT `ARRENDADOR_PK` PRIMARY KEY (`Numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ARRENDATARIO` (
    `Numero_documento` INT NOT NULL COMMENT 'Numero del documento de identificacion unico para identificar al usuario.',
    CONSTRAINT `ARRENDATARIO_PK` PRIMARY KEY (`Numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `SEGURO` (
    `Estado_poliza`         CHAR(20) NOT NULL COMMENT 'Estado en el que se encuentra la poliza actualmente.',
    `Id_seguro`             INT NOT NULL COMMENT 'Numero de identificacion del seguro en el sistema.',
    `numero_poliza`         INT NOT NULL COMMENT 'Numero de la poliza del seguro.',
    `Aseguradora`           CHAR(50) NOT NULL COMMENT 'Nombre de la aseguradora.',
    `Fecha_inicio_vigencia` DATE NOT NULL COMMENT 'Fecha de inicio de la vigencia del seguro.',
    CONSTRAINT `SEGURO_PK` PRIMARY KEY (`Id_seguro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `MOTOCICLETA` (
    `Estado_motocicleta` CHAR(20) NOT NULL COMMENT 'Estado actual de la motocicleta.',
    `Kilometraje_actual` INT NOT NULL COMMENT 'Kilometraje actual de la motocicleta despues de cada mantenimiento.',
    `Precio_alquiler`    INT NOT NULL COMMENT 'Precio de alquiler establecido por el arrendador.',
    `Cilindraje`         DECIMAL(10,2) NOT NULL COMMENT 'Cilindraje de la motocicleta.',
    `Año`                INT NOT NULL COMMENT 'Año de creacion de la motocicleta.',
    `modelo`             CHAR(50) NOT NULL COMMENT 'Modelo de la motocicleta.',
    `Marca`              CHAR(50) NOT NULL COMMENT 'Marca de la motocicleta.',
    `Placa`              CHAR(10) NOT NULL COMMENT 'Numero y caracteres por los cuales se identifica una motocicleta.',
    `Numero_documento`   INT NOT NULL COMMENT 'Documento del arrendador propietario de la motocicleta.',
    `Id_seguro`          INT NOT NULL COMMENT 'Identificador del seguro de la motocicleta.',
    CONSTRAINT `MOTOCICLETA_PK` PRIMARY KEY (`Placa`, `Numero_documento`),
    KEY `MOTOCICLETA_ARRENDADOR_IX` (`Numero_documento`),
    KEY `MOTOCICLETA_SEGURO_IX` (`Id_seguro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `SOLICITUD` (
    `Id_solicitud`       INT NOT NULL COMMENT 'Identificacion de la solicitud.',
    `Fecha_solicitud`    DATE NOT NULL COMMENT 'Fecha en que se realizo la solicitud del alquiler de la moto.',
    `Estado_solicitud`   CHAR(20) NOT NULL COMMENT 'Estado actual de la solicitud.',
    `Mensaje_solicitud`  CHAR(200) NOT NULL COMMENT 'Mensaje asociado a la solicitud.',
    `Numero_documento`   INT NOT NULL COMMENT 'Documento del arrendatario que solicita el alquiler.',
    `Placa`              CHAR(10) NOT NULL COMMENT 'Placa de la motocicleta solicitada.',
    `id_contrato`        INT NOT NULL COMMENT 'Identificador del contrato asociado.',
    `Numero_documento1`  INT NOT NULL,
    `Numero_documento11` INT NOT NULL,
    `Numero_documento2`  INT NOT NULL,
    CONSTRAINT `SOLICITUD_PK` PRIMARY KEY (`Id_solicitud`, `Numero_documento`, `Placa`, `Numero_documento2`),
    UNIQUE KEY `SOLICITUD__IDX` (`id_contrato`, `Numero_documento1`, `Numero_documento11`),
    KEY `SOLICITUD_ARRENDATARIO_IX` (`Numero_documento`),
    KEY `SOLICITUD_MOTOCICLETA_IX` (`Placa`, `Numero_documento2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `CONTRATO` (
    `id_contrato`        INT NOT NULL COMMENT 'Numero de identificacion del contrato.',
    `Fecha_inicio`       DATE NOT NULL COMMENT 'Fecha de inicio del contrato.',
    `Fecha_fin`          DATE NOT NULL COMMENT 'Fecha de finalizacion del contrato.',
    `Estado_contrato`    CHAR(20) NOT NULL COMMENT 'Estado actual del contrato (finalizado, en vigencia, cancelado, etc.).',
    `Numero_documento`   INT NOT NULL COMMENT 'Documento del arrendatario.',
    `Numero_documento1`  INT NOT NULL COMMENT 'Documento del arrendador.',
    `Id_solicitud`       INT NOT NULL,
    `Numero_documento2`  INT NOT NULL,
    `Placa`              CHAR(10) NOT NULL,
    `Numero_documento21` INT NOT NULL,
    CONSTRAINT `CONTRATO_PK` PRIMARY KEY (`id_contrato`, `Numero_documento`, `Numero_documento1`),
    UNIQUE KEY `CONTRATO__IDX` (`Id_solicitud`, `Numero_documento2`, `Placa`, `Numero_documento21`),
    KEY `CONTRATO_ARRENDADOR_IX` (`Numero_documento1`),
    KEY `CONTRATO_SOLICITUD_IX` (`Id_solicitud`, `Numero_documento2`, `Placa`, `Numero_documento21`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `PAGO` (
    `Id_pago`           INT NOT NULL COMMENT 'Identificador del pago en el sistema.',
    `fecha_pago`        DATE NOT NULL COMMENT 'Fecha en la que se realizo el pago.',
    `Monto`             INT NOT NULL COMMENT 'Monto pagado.',
    `Fecha_vencimiento` DATE NOT NULL COMMENT 'Fecha de vencimiento del pago.',
    `Estado`            CHAR(20) NOT NULL COMMENT 'Estado actual del pago (pendiente, pagado, cancelado, etc.).',
    `Metodo_pago`       CHAR(20) NOT NULL COMMENT 'Metodo por el cual se realiza el pago.',
    `id_contrato`       INT NOT NULL,
    `Numero_documento`  INT NOT NULL,
    `Numero_documento1` INT NOT NULL,
    CONSTRAINT `PAGO_PK` PRIMARY KEY (`Id_pago`, `id_contrato`, `Numero_documento`, `Numero_documento1`),
    KEY `PAGO_CONTRATO_IX` (`id_contrato`, `Numero_documento`, `Numero_documento1`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `INFO_MANTENIMIENTO` (
    `Id_mantenimiento`          INT NOT NULL COMMENT 'Identificador de la informacion del mantenimiento.',
    `Fecha_mantenimiento`       DATE NOT NULL COMMENT 'Fecha en la que se realizo el mantenimiento.',
    `Tipo_mantenimiento`        CHAR(50) NOT NULL COMMENT 'Tipo de mantenimiento a realizar.',
    `Kilometraje_al_momento`    INT NOT NULL COMMENT 'Kilometraje de la moto al momento del mantenimiento.',
    `Descripcion_mantenimiento` VARCHAR(500) NOT NULL COMMENT 'Descripcion del mantenimiento realizado.',
    `Costo`                     INT NOT NULL COMMENT 'Costo del mantenimiento realizado.',
    `Placa`                     CHAR(10) NOT NULL,
    `Numero_documento`          INT NOT NULL,
    CONSTRAINT `INFO_MANTENIMIENTO_PK` PRIMARY KEY (`Id_mantenimiento`, `Placa`, `Numero_documento`),
    KEY `INFO_MANTENIMIENTO_MOTO_IX` (`Placa`, `Numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- ================================================================
-- Columnas adicionales requeridas por la aplicacion
-- ================================================================
ALTER TABLE MOTOCICLETA
    ADD COLUMN Color VARCHAR(30) NULL,
    ADD COLUMN Descripcion VARCHAR(200) NULL;

ALTER TABLE `CONTRATO`
    ADD COLUMN `Deposito` INT NOT NULL DEFAULT 0;

-- ================================================================
-- Llaves foraneas: se agregan despues de crear todas las tablas.
-- ================================================================

ALTER TABLE `ADMINISTRADOR`
    ADD CONSTRAINT `ADMINISTRADOR_USUARIO_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `USUARIO` (`Numero_documento`);

ALTER TABLE `ARRENDADOR`
    ADD CONSTRAINT `ARRENDADOR_USUARIO_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `USUARIO` (`Numero_documento`);

ALTER TABLE `ARRENDATARIO`
    ADD CONSTRAINT `ARRENDATARIO_USUARIO_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `USUARIO` (`Numero_documento`);

ALTER TABLE `CONTRATO`
    ADD CONSTRAINT `CONTRATO_ARRENDADOR_FK`
    FOREIGN KEY (`Numero_documento1`) REFERENCES `ARRENDADOR` (`Numero_documento`);

ALTER TABLE `CONTRATO`
    ADD CONSTRAINT `CONTRATO_ARRENDATARIO_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `ARRENDATARIO` (`Numero_documento`);

ALTER TABLE `CONTRATO`
    ADD CONSTRAINT `CONTRATO_SOLICITUD_FK`
    FOREIGN KEY (`Id_solicitud`, `Numero_documento2`, `Placa`, `Numero_documento21`)
    REFERENCES `SOLICITUD` (`Id_solicitud`, `Numero_documento`, `Placa`, `Numero_documento2`);

ALTER TABLE `INFO_MANTENIMIENTO`
    ADD CONSTRAINT `INFO_MANTENIMIENTO_MOTOCICLETA_FK`
    FOREIGN KEY (`Placa`, `Numero_documento`)
    REFERENCES `MOTOCICLETA` (`Placa`, `Numero_documento`);

ALTER TABLE `MOTOCICLETA`
    ADD CONSTRAINT `MOTOCICLETA_ARRENDADOR_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `ARRENDADOR` (`Numero_documento`);

ALTER TABLE `MOTOCICLETA`
    ADD CONSTRAINT `MOTOCICLETA_SEGURO_FK`
    FOREIGN KEY (`Id_seguro`) REFERENCES `SEGURO` (`Id_seguro`);

ALTER TABLE `PAGO`
    ADD CONSTRAINT `PAGO_CONTRATO_FK`
    FOREIGN KEY (`id_contrato`, `Numero_documento`, `Numero_documento1`)
    REFERENCES `CONTRATO` (`id_contrato`, `Numero_documento`, `Numero_documento1`);

ALTER TABLE `SOLICITUD`
    ADD CONSTRAINT `SOLICITUD_ARRENDATARIO_FK`
    FOREIGN KEY (`Numero_documento`) REFERENCES `ARRENDATARIO` (`Numero_documento`);

ALTER TABLE `SOLICITUD`
    ADD CONSTRAINT `SOLICITUD_CONTRATO_FK`
    FOREIGN KEY (`id_contrato`, `Numero_documento1`, `Numero_documento11`)
    REFERENCES `CONTRATO` (`id_contrato`, `Numero_documento`, `Numero_documento1`);

ALTER TABLE `SOLICITUD`
    ADD CONSTRAINT `SOLICITUD_MOTOCICLETA_FK`
    FOREIGN KEY (`Placa`, `Numero_documento2`)
    REFERENCES `MOTOCICLETA` (`Placa`, `Numero_documento`);

-- ================================================================
-- Notas de conversion:
-- 1. Oracle NUMBER sin precision se convirtió a DECIMAL(10,2) en Cilindraje.
-- 2. Contraseña UNKNOWN se convirtió a VARCHAR(255), porque UNKNOWN no es un
--    tipo de dato MySQL. Se recomienda almacenar un hash, nunca texto plano.
-- 3. Descripcion_mantenimiento CHAR(500) pasó a VARCHAR(500), ya que MySQL
--    limita CHAR a 255 caracteres.
-- 4. Se conservaron las PK, UK y FK del modelo original. CONTRATO y SOLICITUD
--    tienen referencias circulares obligatorias (NOT NULL); esto complica la
--    inserción de nuevos registros en MySQL. Considere quitar una de esas FK,
--    permitir NULL temporalmente en la relación opcional o rediseñar el modelo.
-- 5. Las columnas ID no se declararon AUTO_INCREMENT porque el modelo de origen
--    no define esa estrategia de generación.
-- 6. Recomendación de diseño: cambiar Telefono de INT a VARCHAR(20) para admitir
--    prefijos internacionales y ceros iniciales.
