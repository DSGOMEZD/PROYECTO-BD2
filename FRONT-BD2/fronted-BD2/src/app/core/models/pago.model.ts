export type EstadoPago = 'PENDIENTE' | 'PAGADO' | 'VENCIDO' | 'CANCELADO';
export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'PSE' | 'NEQUI';

// Tabla PAGO
export interface Pago {
  idPago: number;
  monto: number;
  fechaVencimiento: string;
  fechaPago?: string;
  estado: EstadoPago;
  metodoPago?: MetodoPago;
  referencia?: string;
  idContrato: number;
}
