// Módulo de la app que generó el evento
export type ModuloLog = 'PAGO' | 'MOTO' | 'SOLICITUD' | 'CONTRATO';

export const MODULOS_LOG: { valor: ModuloLog; texto: string }[] = [
  { valor: 'PAGO', texto: 'Pagos' },
  { valor: 'MOTO', texto: 'Motos' },
  { valor: 'SOLICITUD', texto: 'Solicitudes' },
  { valor: 'CONTRATO', texto: 'Contratos' }
];

// Evento que devuelve GET /api/admin/logs
export interface LogEvento {
  id: string;
  fecha: string;            // fecha y hora ISO (YYYY-MM-DDTHH:mm:ss)
  modulo: ModuloLog;
  accion: string;           // CREADA, APROBADA, PAGADO, PUBLICADA, FINALIZADO...
  documentoUsuario: number; // quién hizo la acción
  nombreUsuario: string;
  rolUsuario: string;
  referencia: string;       // id del registro afectado (placa, #solicitud, #contrato, #pago)
  descripcion: string;
}

// Filtros que el front envía como query params
export interface FiltroLogs {
  desde: string;            // YYYY-MM-DD
  hasta: string;            // YYYY-MM-DD
  modulo?: ModuloLog;
}
