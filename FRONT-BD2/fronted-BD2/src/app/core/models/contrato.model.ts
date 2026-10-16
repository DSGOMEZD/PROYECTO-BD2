import { Motocicleta } from './motocicleta.model';

export type EstadoContrato = 'VIGENTE' | 'FINALIZADO' | 'CANCELADO';

// Tabla CONTRATO (+ datos de la moto y del arrendador)
export interface Contrato {
  idContrato: number;
  fechaFirma: string;
  fechaInicio: string;
  fechaFin: string;
  valorTotal: number;
  deposito: number;
  estado: EstadoContrato;
  idSolicitud: number;
  documentoArrendatario: number;
  documentoArrendador: number;
  nombreArrendador: string;
  moto: Pick<Motocicleta, 'placa' | 'marca' | 'modelo'>;
}
