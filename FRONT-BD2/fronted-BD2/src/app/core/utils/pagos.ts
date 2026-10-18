import { diasEntre, sumarMeses } from './fechas';

export interface Cuota {
  monto: number;
  fechaVencimiento: string;
}

// Divide el valor del contrato en cuotas mensuales (una por cada 30 días o fracción).
// La primera vence el día de inicio; la última absorbe el redondeo.
export function planDePagos(fechaInicio: string, fechaFin: string, valorTotal: number): Cuota[] {
  const n = Math.max(1, Math.ceil(diasEntre(fechaInicio, fechaFin) / 30));
  const base = Math.round(valorTotal / n);
  return Array.from({ length: n }, (_, i) => ({
    monto: i === n - 1 ? valorTotal - base * (n - 1) : base,
    fechaVencimiento: sumarMeses(fechaInicio, i)
  }));
}
