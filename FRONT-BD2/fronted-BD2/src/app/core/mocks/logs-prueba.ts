import { LogEvento } from '../models/log.model';

// TEMPORAL: logs simulados mientras no existe GET /api/admin/logs.
// Las fechas se calculan desde hoy para que siempre haya datos en el rango por defecto.
// Borra este archivo cuando LogService llame a la API.

function haceDias(dias: number, hora: string): string {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}T${hora}`;
}

export const LOGS_PRUEBA: LogEvento[] = [
  { id: '6720a1f0c1', fecha: haceDias(0, '09:12:44'), modulo: 'PAGO', accion: 'PAGADO',
    documentoUsuario: 1001, nombreUsuario: 'Juan Martínez', rolUsuario: 'ARRENDATARIO',
    referencia: 'Pago #2', descripcion: 'Pagó $2.270.000 del contrato #1 por NEQUI.' },
  { id: '6720a1f0c2', fecha: haceDias(1, '16:40:02'), modulo: 'SOLICITUD', accion: 'CREADA',
    documentoUsuario: 1002, nombreUsuario: 'Sofía Ramírez', rolUsuario: 'ARRENDATARIO',
    referencia: 'Solicitud #5', descripcion: 'Solicitó la moto AKT10H del 25/10 al 08/11.' },
  { id: '6720a1f0c3', fecha: haceDias(2, '11:05:19'), modulo: 'MOTO', accion: 'PUBLICADA',
    documentoUsuario: 2003, nombreUsuario: 'Andrés Pérez', rolUsuario: 'ARRENDADOR',
    referencia: 'AKT10H', descripcion: 'Publicó la moto AKT NKD 125 a $45.000/día.' },
  { id: '6720a1f0c4', fecha: haceDias(3, '08:22:51'), modulo: 'SOLICITUD', accion: 'APROBADA',
    documentoUsuario: 2003, nombreUsuario: 'Andrés Pérez', rolUsuario: 'ARRENDADOR',
    referencia: 'Solicitud #1', descripcion: 'Aprobó la solicitud de Juan Martínez con depósito de $300.000.' },
  { id: '6720a1f0c5', fecha: haceDias(3, '08:22:52'), modulo: 'CONTRATO', accion: 'CREADO',
    documentoUsuario: 2003, nombreUsuario: 'Andrés Pérez', rolUsuario: 'ARRENDADOR',
    referencia: 'Contrato #1', descripcion: 'Se generó el contrato #1 para la moto SUZ77G.' },
  { id: '6720a1f0c6', fecha: haceDias(5, '19:47:30'), modulo: 'SOLICITUD', accion: 'RECHAZADA',
    documentoUsuario: 2001, nombreUsuario: 'Carlos Ruiz', rolUsuario: 'ARRENDADOR',
    referencia: 'Solicitud #4', descripcion: 'Rechazó la solicitud de Juan Martínez para la moto ABC12D.' },
  { id: '6720a1f0c7', fecha: haceDias(7, '14:10:08'), modulo: 'MOTO', accion: 'MANTENIMIENTO',
    documentoUsuario: 2003, nombreUsuario: 'Andrés Pérez', rolUsuario: 'ARRENDADOR',
    referencia: 'HND55J', descripcion: 'Cambió el estado de la moto HND55J a MANTENIMIENTO.' },
  { id: '6720a1f0c8', fecha: haceDias(9, '10:33:12'), modulo: 'PAGO', accion: 'VENCIDO',
    documentoUsuario: 1002, nombreUsuario: 'Sofía Ramírez', rolUsuario: 'ARRENDATARIO',
    referencia: 'Pago #7', descripcion: 'El pago de $450.000 del contrato #4 quedó vencido.' },
  { id: '6720a1f0c9', fecha: haceDias(12, '17:58:45'), modulo: 'CONTRATO', accion: 'FINALIZADO',
    documentoUsuario: 2002, nombreUsuario: 'Laura Gómez', rolUsuario: 'ARRENDADOR',
    referencia: 'Contrato #2', descripcion: 'Finalizó el contrato #2 de la moto BJA45F (km final 22.800).' },
  { id: '6720a1f0ca', fecha: haceDias(15, '12:01:27'), modulo: 'MOTO', accion: 'PUBLICADA',
    documentoUsuario: 2002, nombreUsuario: 'Laura Gómez', rolUsuario: 'ARRENDADOR',
    referencia: 'KTM39A', descripcion: 'Publicó la moto KTM Duke 390 a $150.000/día.' },
  { id: '6720a1f0cb', fecha: haceDias(18, '09:44:03'), modulo: 'SOLICITUD', accion: 'CANCELADA',
    documentoUsuario: 1001, nombreUsuario: 'Juan Martínez', rolUsuario: 'ARRENDATARIO',
    referencia: 'Solicitud #6', descripcion: 'Canceló su solicitud para la moto XYZ98K.' },
  { id: '6720a1f0cc', fecha: haceDias(22, '15:16:39'), modulo: 'PAGO', accion: 'PAGADO',
    documentoUsuario: 1001, nombreUsuario: 'Juan Martínez', rolUsuario: 'ARRENDATARIO',
    referencia: 'Pago #1', descripcion: 'Pagó $2.280.000 del contrato #1 por TRANSFERENCIA.' },
  { id: '6720a1f0cd', fecha: haceDias(27, '18:25:50'), modulo: 'CONTRATO', accion: 'CANCELADO',
    documentoUsuario: 2001, nombreUsuario: 'Carlos Ruiz', rolUsuario: 'ARRENDADOR',
    referencia: 'Contrato #3', descripcion: 'Canceló el contrato #3 antes de su inicio.' },
  { id: '6720a1f0ce', fecha: haceDias(40, '07:50:14'), modulo: 'MOTO', accion: 'PUBLICADA',
    documentoUsuario: 2001, nombreUsuario: 'Carlos Ruiz', rolUsuario: 'ARRENDADOR',
    referencia: 'ABC12D', descripcion: 'Publicó la moto Yamaha MT-03 a $120.000/día.' },
  { id: '6720a1f0cf', fecha: haceDias(55, '13:37:21'), modulo: 'PAGO', accion: 'PAGADO',
    documentoUsuario: 1003, nombreUsuario: 'Mateo López', rolUsuario: 'ARRENDATARIO',
    referencia: 'Pago #5', descripcion: 'Pagó $1.350.000 del contrato #3 en EFECTIVO.' }
];
