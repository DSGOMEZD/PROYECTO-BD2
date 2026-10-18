import { Seguro, SeguroRequest } from './seguro.model';

export type EstadoMoto = 'DISPONIBLE' | 'ARRENDADA' | 'MANTENIMIENTO' | 'INACTIVA';

// Tabla MOTOCICLETA (+ nombre del arrendador para mostrarlo en la tarjeta y su seguro)
export interface Motocicleta {
  placa: string;
  marca: string;
  modelo: string;
  anio: number;
  cilindraje: number;
  color: string;
  kilometraje: number;
  precioDia: number;
  descripcion?: string;
  imagenUrl?: string;
  estado: EstadoMoto;
  documentoArrendador: number;
  nombreArrendador: string;
  seguro?: Seguro;
}

// Lo que el arrendador ENVÍA para publicar o editar una moto
export interface MotoRequest {
  placa: string;
  marca: string;
  modelo: string;
  anio: number | null;
  cilindraje: number | null;
  color: string;
  kilometraje: number | null;
  precioDia: number | null;
  descripcion?: string;
  seguro: SeguroRequest;
}
