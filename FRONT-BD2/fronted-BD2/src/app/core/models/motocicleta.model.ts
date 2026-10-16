export type EstadoMoto = 'DISPONIBLE' | 'ARRENDADA' | 'MANTENIMIENTO' | 'INACTIVA';

// Tabla MOTOCICLETA (+ nombre del arrendador para mostrarlo en la tarjeta)
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
}
