export type EstadoPoliza = 'VIGENTE' | 'VENCIDA' | 'CANCELADA';

// Tabla SEGURO (cada moto tiene su póliza)
export interface Seguro {
  idSeguro: number;
  numeroPoliza: number;
  aseguradora: string;
  fechaInicioVigencia: string;
  estadoPoliza: EstadoPoliza;
}

// Lo que el front ENVÍA del seguro al publicar o editar una moto (el id lo asigna el back)
export interface SeguroRequest {
  numeroPoliza: number | null;
  aseguradora: string;
  fechaInicioVigencia: string;
  estadoPoliza: EstadoPoliza;
}
