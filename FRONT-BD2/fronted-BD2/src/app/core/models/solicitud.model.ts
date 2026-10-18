import { Motocicleta } from './motocicleta.model';

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA';

// Tabla SOLICITUD (+ datos básicos de la moto y del arrendatario para mostrarla)
export interface Solicitud {
  idSolicitud: number;
  fechaSolicitud: string;
  fechaInicio: string;
  fechaFin: string;
  mensaje?: string;
  estado: EstadoSolicitud;
  fechaRespuesta?: string;
  documentoArrendatario: number;
  nombreArrendatario: string;
  documentoArrendador: number;
  moto: Pick<Motocicleta, 'placa' | 'marca' | 'modelo' | 'precioDia'>;
}

// Lo que el front ENVÍA para crear una solicitud
export interface NuevaSolicitudRequest {
  placa: string;
  fechaInicio: string;
  fechaFin: string;
  mensaje?: string;
}

// Lo que el arrendador ENVÍA al aprobar una solicitud (se genera el contrato)
export interface AprobarSolicitudRequest {
  deposito: number;
}
