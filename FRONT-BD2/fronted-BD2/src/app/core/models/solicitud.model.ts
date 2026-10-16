import { Motocicleta } from './motocicleta.model';

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA';

// Tabla SOLICITUD (+ datos básicos de la moto para mostrarla)
export interface Solicitud {
  idSolicitud: number;
  fechaSolicitud: string;
  fechaInicio: string;
  fechaFin: string;
  mensaje?: string;
  estado: EstadoSolicitud;
  fechaRespuesta?: string;
  documentoArrendatario: number;
  moto: Pick<Motocicleta, 'placa' | 'marca' | 'modelo' | 'precioDia'>;
}

// Lo que el front ENVÍA para crear una solicitud
export interface NuevaSolicitudRequest {
  placa: string;
  fechaInicio: string;
  fechaFin: string;
  mensaje?: string;
}
