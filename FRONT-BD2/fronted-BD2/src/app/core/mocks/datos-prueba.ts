import { Contrato } from '../models/contrato.model';
import { Motocicleta } from '../models/motocicleta.model';
import { Pago } from '../models/pago.model';
import { Solicitud } from '../models/solicitud.model';

// TEMPORAL: datos simulados mientras no existe el back.
// Los servicios los modifican en memoria (se reinician al recargar la página).
// Borra este archivo cuando los servicios llamen a la API.

// Usuarios "con sesión iniciada" mientras no hay login real
export const DOCUMENTO_ARRENDATARIO_PRUEBA = 1001;
export const NOMBRE_ARRENDATARIO_PRUEBA = 'Juan Martínez';
export const DOCUMENTO_ARRENDADOR_PRUEBA = 2003;
export const NOMBRE_ARRENDADOR_PRUEBA = 'Andrés Pérez';

export const MOTOS_PRUEBA: Motocicleta[] = [
  { placa: 'ABC12D', marca: 'Yamaha', modelo: 'MT-03', anio: 2023, cilindraje: 321, color: 'Azul',
    kilometraje: 8200, precioDia: 120000, estado: 'DISPONIBLE', documentoArrendador: 2001,
    nombreArrendador: 'Carlos Ruiz', descripcion: 'Naked deportiva, ideal para ciudad y carretera.',
    seguro: { idSeguro: 1, numeroPoliza: 70011234, aseguradora: 'Sura', fechaInicioVigencia: '2026-01-15', estadoPoliza: 'VIGENTE' } },
  { placa: 'XYZ98K', marca: 'Honda', modelo: 'CB 190R', anio: 2022, cilindraje: 184, color: 'Rojo',
    kilometraje: 15400, precioDia: 70000, estado: 'DISPONIBLE', documentoArrendador: 2001,
    nombreArrendador: 'Carlos Ruiz', descripcion: 'Económica y cómoda para el día a día.',
    seguro: { idSeguro: 2, numeroPoliza: 70011235, aseguradora: 'Sura', fechaInicioVigencia: '2026-03-01', estadoPoliza: 'VIGENTE' } },
  { placa: 'KTM39A', marca: 'KTM', modelo: 'Duke 390', anio: 2024, cilindraje: 373, color: 'Naranja',
    kilometraje: 3100, precioDia: 150000, estado: 'DISPONIBLE', documentoArrendador: 2002,
    nombreArrendador: 'Laura Gómez', descripcion: 'Potente y ligera, con ABS y pantalla TFT.',
    seguro: { idSeguro: 3, numeroPoliza: 55002981, aseguradora: 'Bolívar', fechaInicioVigencia: '2026-02-10', estadoPoliza: 'VIGENTE' } },
  { placa: 'BJA45F', marca: 'Bajaj', modelo: 'Pulsar NS 200', anio: 2021, cilindraje: 199, color: 'Negro',
    kilometraje: 22800, precioDia: 65000, estado: 'DISPONIBLE', documentoArrendador: 2002,
    nombreArrendador: 'Laura Gómez',
    seguro: { idSeguro: 4, numeroPoliza: 55002990, aseguradora: 'Bolívar', fechaInicioVigencia: '2025-11-20', estadoPoliza: 'VIGENTE' } },
  { placa: 'SUZ77G', marca: 'Suzuki', modelo: 'V-Strom 250', anio: 2023, cilindraje: 248, color: 'Amarillo',
    kilometraje: 9600, precioDia: 110000, estado: 'ARRENDADA', documentoArrendador: 2003,
    nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA, descripcion: 'Doble propósito, perfecta para viajes largos.',
    seguro: { idSeguro: 5, numeroPoliza: 81003344, aseguradora: 'Allianz', fechaInicioVigencia: '2026-04-05', estadoPoliza: 'VIGENTE' } },
  { placa: 'AKT10H', marca: 'AKT', modelo: 'NKD 125', anio: 2022, cilindraje: 125, color: 'Gris',
    kilometraje: 18000, precioDia: 45000, estado: 'DISPONIBLE', documentoArrendador: 2003,
    nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA,
    seguro: { idSeguro: 6, numeroPoliza: 81003345, aseguradora: 'Allianz', fechaInicioVigencia: '2026-04-05', estadoPoliza: 'VIGENTE' } },
  { placa: 'HND55J', marca: 'Honda', modelo: 'XR 150L', anio: 2020, cilindraje: 149, color: 'Blanco',
    kilometraje: 31200, precioDia: 55000, estado: 'MANTENIMIENTO', documentoArrendador: 2003,
    nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA, descripcion: 'Todo terreno, resistente para trocha.',
    seguro: { idSeguro: 7, numeroPoliza: 81002210, aseguradora: 'Mapfre', fechaInicioVigencia: '2025-09-01', estadoPoliza: 'VENCIDA' } }
];

export const SOLICITUDES_PRUEBA: Solicitud[] = [
  { idSolicitud: 1, fechaSolicitud: '2026-09-01', fechaInicio: '2026-09-10', fechaFin: '2026-11-10',
    estado: 'APROBADA', fechaRespuesta: '2026-09-02', documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
    nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA, documentoArrendador: 2003,
    moto: { placa: 'SUZ77G', marca: 'Suzuki', modelo: 'V-Strom 250', precioDia: 110000 },
    mensaje: 'La necesito para ir a trabajar.' },
  { idSolicitud: 2, fechaSolicitud: '2026-07-15', fechaInicio: '2026-07-20', fechaFin: '2026-08-20',
    estado: 'APROBADA', fechaRespuesta: '2026-07-16', documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
    nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA, documentoArrendador: 2002,
    moto: { placa: 'BJA45F', marca: 'Bajaj', modelo: 'Pulsar NS 200', precioDia: 65000 } },
  { idSolicitud: 3, fechaSolicitud: '2026-10-12', fechaInicio: '2026-10-20', fechaFin: '2026-10-27',
    estado: 'PENDIENTE', documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
    nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA, documentoArrendador: 2002,
    moto: { placa: 'KTM39A', marca: 'KTM', modelo: 'Duke 390', precioDia: 150000 } },
  { idSolicitud: 4, fechaSolicitud: '2026-10-05', fechaInicio: '2026-10-08', fechaFin: '2026-10-15',
    estado: 'RECHAZADA', fechaRespuesta: '2026-10-06', documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
    nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA, documentoArrendador: 2001,
    moto: { placa: 'ABC12D', marca: 'Yamaha', modelo: 'MT-03', precioDia: 120000 } },
  // Solicitudes que le llegan al arrendador de prueba (2003) desde otros arrendatarios
  { idSolicitud: 5, fechaSolicitud: '2026-10-14', fechaInicio: '2026-10-25', fechaFin: '2026-11-08',
    estado: 'PENDIENTE', documentoArrendatario: 1002, nombreArrendatario: 'Sofía Ramírez', documentoArrendador: 2003,
    moto: { placa: 'AKT10H', marca: 'AKT', modelo: 'NKD 125', precioDia: 45000 },
    mensaje: 'Es para hacer domicilios dos semanas.' },
  { idSolicitud: 6, fechaSolicitud: '2026-10-16', fechaInicio: '2026-11-01', fechaFin: '2026-11-30',
    estado: 'PENDIENTE', documentoArrendatario: 1003, nombreArrendatario: 'Pedro Castillo', documentoArrendador: 2003,
    moto: { placa: 'AKT10H', marca: 'AKT', modelo: 'NKD 125', precioDia: 45000 } },
  { idSolicitud: 7, fechaSolicitud: '2026-09-15', fechaInicio: '2026-09-20', fechaFin: '2026-09-30',
    estado: 'RECHAZADA', fechaRespuesta: '2026-09-15', documentoArrendatario: 1004,
    nombreArrendatario: 'Camila Herrera', documentoArrendador: 2003,
    moto: { placa: 'SUZ77G', marca: 'Suzuki', modelo: 'V-Strom 250', precioDia: 110000 } },
  { idSolicitud: 8, fechaSolicitud: '2026-05-20', fechaInicio: '2026-06-01', fechaFin: '2026-06-30',
    estado: 'APROBADA', fechaRespuesta: '2026-05-21', documentoArrendatario: 1005,
    nombreArrendatario: 'Valentina Rojas', documentoArrendador: 2003,
    moto: { placa: 'AKT10H', marca: 'AKT', modelo: 'NKD 125', precioDia: 45000 } }
];

export const CONTRATOS_PRUEBA: Contrato[] = [
  { idContrato: 1, fechaFirma: '2026-09-03', fechaInicio: '2026-09-10', fechaFin: '2026-11-10',
    valorTotal: 6820000, deposito: 500000, estado: 'VIGENTE', idSolicitud: 1,
    documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA, nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA,
    documentoArrendador: 2003, nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA,
    moto: { placa: 'SUZ77G', marca: 'Suzuki', modelo: 'V-Strom 250' } },
  { idContrato: 2, fechaFirma: '2026-07-17', fechaInicio: '2026-07-20', fechaFin: '2026-08-20',
    valorTotal: 2080000, deposito: 300000, estado: 'FINALIZADO', idSolicitud: 2,
    documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA, nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA,
    documentoArrendador: 2002, nombreArrendador: 'Laura Gómez',
    moto: { placa: 'BJA45F', marca: 'Bajaj', modelo: 'Pulsar NS 200' } },
  { idContrato: 3, fechaFirma: '2026-05-22', fechaInicio: '2026-06-01', fechaFin: '2026-06-30',
    valorTotal: 1350000, deposito: 200000, estado: 'FINALIZADO', idSolicitud: 8,
    documentoArrendatario: 1005, nombreArrendatario: 'Valentina Rojas',
    documentoArrendador: 2003, nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA,
    moto: { placa: 'AKT10H', marca: 'AKT', modelo: 'NKD 125' } }
];

export const PAGOS_PRUEBA: Pago[] = [
  { idPago: 1, idContrato: 1, monto: 2280000, fechaVencimiento: '2026-09-10', fechaPago: '2026-09-09',
    estado: 'PAGADO', metodoPago: 'TRANSFERENCIA', referencia: 'TRX-88213' },
  { idPago: 2, idContrato: 1, monto: 2270000, fechaVencimiento: '2026-10-10', fechaPago: '2026-10-10',
    estado: 'PAGADO', metodoPago: 'NEQUI', referencia: 'NQ-55102' },
  { idPago: 3, idContrato: 1, monto: 2270000, fechaVencimiento: '2026-11-10', estado: 'PENDIENTE' },
  { idPago: 4, idContrato: 2, monto: 2080000, fechaVencimiento: '2026-07-20', fechaPago: '2026-07-19',
    estado: 'PAGADO', metodoPago: 'PSE', referencia: 'PSE-10293' },
  { idPago: 5, idContrato: 3, monto: 1350000, fechaVencimiento: '2026-06-01', fechaPago: '2026-06-01',
    estado: 'PAGADO', metodoPago: 'EFECTIVO' }
];
