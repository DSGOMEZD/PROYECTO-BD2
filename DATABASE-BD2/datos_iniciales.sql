-- ================================================================
-- Datos iniciales (de prueba) - Sistema de alquiler de motocicletas
--
-- Orden de ejecucion:
--   1. Esquema.sql
--   2. objetos_programables.sql
--   3. datos_iniciales.sql   <- este archivo
--
-- - Ejecutar sobre una base recien creada (solo con el ADMIN que crea el
--   backend al arrancar). Si se ejecuta dos veces falla por duplicados.
-- - Los datos se cargan con los MISMOS procedimientos que usa el backend,
--   asi pasan por todas las validaciones y triggers, y los IDs se generan
--   igual que en la aplicacion.
-- - Las fechas son relativas a CURDATE(), asi el script sirve cualquier dia
--   (los procedimientos no aceptan solicitudes con fechas pasadas).
-- - Estos datos van directo a MySQL: NO generan logs en MongoDB. Los logs
--   solo se crean cuando las acciones se hacen a traves del backend.
--
-- Contrasena de TODOS los usuarios de prueba: Clave123*
-- (hash BCrypt, igual que el que genera el backend)
-- El ADMIN (admin@motogo.com / Admin123*) lo crea el backend, no este script.
-- ================================================================

USE alquiler_motos;

SET @clave = '$2a$10$r3/ftV8iGtMIEKmapWXdAe3gOiCy4/TWJk9ghPo6/6MIO6eCTa4pK';


-- ================================================================
-- 1. USUARIOS
-- sp_registrar_usuario(tipo_doc, documento, nombre1, nombre2, apellido1,
--                      apellido2, telefono, correo, hash, rol)
-- ================================================================

-- Arrendadores (propietarios de motos)
CALL sp_registrar_usuario('CC', 1001, 'Carlos', 'Andres', 'Ramirez', 'Diaz',    310100100, 'carlos.ramirez@motogo.com', @clave, 'ARRENDADOR');
CALL sp_registrar_usuario('CC', 1002, 'Laura',  NULL,     'Gomez',   'Rojas',   310100200, 'laura.gomez@motogo.com',    @clave, 'ARRENDADOR');
CALL sp_registrar_usuario('CE', 1003, 'Andres', 'Felipe', 'Torres',  NULL,      310100300, 'andres.torres@motogo.com',  @clave, 'ARRENDADOR');

-- Arrendatarios (clientes que alquilan)
CALL sp_registrar_usuario('CC', 2001, 'Juan',   'Pablo',  'Perez',     'Castro', 320200100, 'juan.perez@motogo.com',     @clave, 'ARRENDATARIO');
CALL sp_registrar_usuario('CC', 2002, 'Maria',  'Jose',   'Lopez',     NULL,     320200200, 'maria.lopez@motogo.com',    @clave, 'ARRENDATARIO');
CALL sp_registrar_usuario('TI', 2003, 'Sofia',  NULL,     'Martinez',  'Vega',   320200300, 'sofia.martinez@motogo.com', @clave, 'ARRENDATARIO');
CALL sp_registrar_usuario('PAS',2004, 'Diego',  NULL,     'Hernandez', 'Mora',   320200400, 'diego.hernandez@motogo.com',@clave, 'ARRENDATARIO');


-- ================================================================
-- 2. MOTOCICLETAS + SEGUROS
-- sp_registrar_motocicleta(arrendador, placa, marca, modelo, color,
--     descripcion, anio, cilindraje, kilometraje, precio_dia,
--     numero_poliza, aseguradora, inicio_vigencia, estado_poliza, OUT id_seguro)
-- ================================================================

CALL sp_registrar_motocicleta(1001, 'ABC12D', 'Yamaha',   'FZ 2.0',       'Negro',  'Moto urbana, muy economica en combustible.', 2022, 150.00, 12000,  60000, 500001, 'Sura',          DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 'VIGENTE', @seg1);
CALL sp_registrar_motocicleta(1001, 'XYZ34E', 'Honda',    'CB 190R',      'Rojo',   'Deportiva, ideal para ciudad y carretera.',  2023, 190.00,  8000,  75000, 500002, 'Sura',          DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 'VIGENTE', @seg2);
CALL sp_registrar_motocicleta(1002, 'KLM56F', 'Suzuki',   'Gixxer 150',   'Azul',   'Comoda para trayectos diarios.',             2021, 155.00, 20000,  55000, 700001, 'Seguros Bolivar',DATE_SUB(CURDATE(), INTERVAL 1 YEAR),  'VIGENTE', @seg3);
CALL sp_registrar_motocicleta(1002, 'QWE78G', 'Bajaj',    'Pulsar NS200', 'Blanco', 'Buena potencia, frenos ABS.',                2023, 200.00,  5000,  80000, 700002, 'Seguros Bolivar',DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'VIGENTE', @seg4);
CALL sp_registrar_motocicleta(1003, 'RTY90H', 'AKT',      'CR4 125',      'Gris',   'Ligera, perfecta para principiantes.',       2024, 125.00,  1500,  45000, 900001, 'Mapfre',        DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 'VIGENTE', @seg5);
CALL sp_registrar_motocicleta(1003, 'UIO12J', 'Kawasaki', 'Z400',         'Verde',  'Naked de media cilindrada, para viajes.',    2022, 400.00,  9000, 150000, 900002, 'Mapfre',        DATE_SUB(CURDATE(), INTERVAL 4 MONTH), 'VIGENTE', @seg6);

-- La AKT queda en mantenimiento (no aparece en el catalogo de disponibles).
CALL sp_cambiar_estado_motocicleta('RTY90H', 1003, 'MANTENIMIENTO');


-- ================================================================
-- 3. MANTENIMIENTOS
-- No hay procedimiento para esta tabla; se inserta directo.
-- trg_mantenimiento_ai actualiza el kilometraje de la moto.
-- ================================================================

INSERT INTO `INFO_MANTENIMIENTO`
    (`Id_mantenimiento`, `Fecha_mantenimiento`, `Tipo_mantenimiento`, `Kilometraje_al_momento`,
     `Descripcion_mantenimiento`, `Costo`, `Placa`, `Numero_documento`)
VALUES
    (1, DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 'PREVENTIVO', 11500, 'Cambio de aceite y filtro.',            85000, 'ABC12D', 1001),
    (2, DATE_SUB(CURDATE(), INTERVAL 20 DAY),  'CORRECTIVO', 19800, 'Cambio de pastillas de freno.',         120000, 'KLM56F', 1002),
    (3, CURDATE(),                             'PREVENTIVO',  1600, 'Revision general de los 1500 km.',      60000, 'RTY90H', 1003);


-- ================================================================
-- 4. SOLICITUDES, CONTRATOS Y PAGOS
-- sp_crear_solicitud(arrendatario, placa, inicio, fin, mensaje, OUT id_sol, OUT id_contrato)
--   -> crea la solicitud PENDIENTE y su contrato PENDIENTE.
-- sp_responder_solicitud(id_sol, arrendador, 'APROBADA'|'RECHAZADA', deposito, OUT id_pago)
--   -> al aprobar: contrato VIGENTE + pagos (una cuota cada 30 dias).
-- ================================================================

-- 4.1 Juan alquila la Yamaha (aprobada) y paga la primera cuota.
CALL sp_crear_solicitud(2001, 'ABC12D', DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
                        'Hola, la necesito para un viaje corto el fin de semana.', @sol1, @con1);
CALL sp_responder_solicitud(@sol1, 1001, 'APROBADA', 100000, @pago1);
CALL sp_registrar_pago(@pago1, 2001, 'NEQUI');

-- 4.2 Maria alquila la Honda desde hoy (aprobada): la moto queda ARRENDADA.
CALL sp_crear_solicitud(2002, 'XYZ34E', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
                        'La uso para ir al trabajo esta semana.', @sol2, @con2);
CALL sp_responder_solicitud(@sol2, 1001, 'APROBADA', 150000, @pago2);

-- 4.3 Diego alquila la Suzuki por 40 dias (aprobada): se generan 2 cuotas pendientes.
CALL sp_crear_solicitud(2004, 'KLM56F', DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 54 DAY),
                        'Alquiler largo, me mude temporalmente a la ciudad.', @sol3, @con3);
CALL sp_responder_solicitud(@sol3, 1002, 'APROBADA', 200000, @pago3);

-- 4.4 Sofia pide la Suzuki (queda PENDIENTE para que el arrendador responda).
CALL sp_crear_solicitud(2003, 'KLM56F', DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 9 DAY),
                        'Me interesa para unas vacaciones.', @sol4, @con4);

-- 4.5 Juan pide la Kawasaki (queda PENDIENTE).
CALL sp_crear_solicitud(2001, 'UIO12J', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY),
                        'Quiero probar una moto de mayor cilindraje.', @sol5, @con5);

-- 4.6 Diego pide la Bajaj y el arrendador la RECHAZA (su contrato queda CANCELADO).
CALL sp_crear_solicitud(2004, 'QWE78G', DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
                        'La necesito para hacer domicilios.', @sol6, @con6);
CALL sp_responder_solicitud(@sol6, 1002, 'RECHAZADA', 0, @pago6);

-- 4.7 Sofia pide la Yamaha y luego CANCELA su solicitud.
CALL sp_crear_solicitud(2003, 'ABC12D', DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY),
                        'Para un paseo, pero aun no estoy segura.', @sol7, @con7);
CALL sp_cancelar_solicitud(@sol7, 2003);


-- ================================================================
-- 5. VERIFICACION
-- ================================================================
SELECT `Rol_usuario`, COUNT(*) AS usuarios FROM `USUARIO` GROUP BY `Rol_usuario`;
SELECT `Placa`, `Marca`, `modelo`, `Estado_motocicleta`, `Kilometraje_actual` FROM `MOTOCICLETA`;
SELECT `Id_solicitud`, `Numero_documento` AS arrendatario, `Placa`, `Estado_solicitud` FROM `SOLICITUD` ORDER BY `Id_solicitud`;
SELECT `id_contrato`, `Placa`, `Fecha_inicio`, `Fecha_fin`, `Estado_contrato`, `Deposito` FROM `CONTRATO` ORDER BY `id_contrato`;
SELECT `Id_pago`, `id_contrato`, `Monto`, `Fecha_vencimiento`, `Estado`, `Metodo_pago` FROM `PAGO` ORDER BY `Id_pago`;
