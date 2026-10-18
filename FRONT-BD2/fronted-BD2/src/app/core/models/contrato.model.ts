import { Motocicleta } from './motocicleta.model';

export type EstadoContrato = 'VIGENTE' | 'FINALIZADO' | 'CANCELADO';

// Tabla CONTRATO (+ datos de la moto, del arrendador y del arrendatario)
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
  nombreArrendatario: string;
  documentoArrendador: number;
  nombreArrendador: string;
  moto: Pick<Motocicleta, 'placa' | 'marca' | 'modelo'>;
}
