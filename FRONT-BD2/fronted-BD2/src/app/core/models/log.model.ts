// Módulo de la app que generó el evento
export type ModuloLog = 'PAGO' | 'MOTO' | 'SOLICITUD' | 'CONTRATO';

export const MODULOS_LOG: { valor: ModuloLog; texto: string }[] = [
  { valor: 'PAGO', texto: 'Pagos' },
  { valor: 'MOTO', texto: 'Motos' },
  { valor: 'SOLICITUD', texto: 'Solicitudes' },
  { valor: 'CONTRATO', texto: 'Contratos' }
];

// Valor de un campo del detalle (MongoDB guarda texto, números, booleanos, objetos y listas)
export type ValorDetalle = string | number | boolean | null | ValorDetalle[] | { [clave: string]: ValorDetalle };

// Evento que devuelve GET /api/admin/logs (sacado de su cubeta en MongoDB)
export interface LogEvento {
  id: string;
  fecha: string;            // fecha y hora ISO (YYYY-MM-DDTHH:mm:ss.SSS)
  modulo: ModuloLog;
  accion: string;           // CREADA, APROBADA, GENERADO, PAGADO, PUBLICADA, FINALIZADO...
  documentoUsuario: number; // quién hizo la acción
  nombreUsuario: string;
  rolUsuario: string;
  referencia: string;       // registro afectado (placa, Solicitud #5, Contrato #3, Pago #2)
  descripcion: string;
  detalle: { [clave: string]: ValorDetalle }; // snapshot de los datos al momento del evento
  ip?: string;
  userAgent?: string;
  endpoint?: string;        // ej. "PATCH /api/pagos/2/pagar"
}

// Filtros que el front envía como query params
export interface FiltroLogs {
  desde: string;            // YYYY-MM-DD
  hasta: string;            // YYYY-MM-DD
  modulo?: ModuloLog;
}
