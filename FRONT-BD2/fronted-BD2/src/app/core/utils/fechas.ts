// Días de alquiler entre dos fechas YYYY-MM-DD (ambos días incluidos)
export function diasEntre(inicio: string, fin: string): number {
  if (!inicio || !fin) return 0;
  const ms = new Date(fin).getTime() - new Date(inicio).getTime();
  return ms < 0 ? 0 : Math.round(ms / 86_400_000) + 1;
}

// Fecha de hoy en formato YYYY-MM-DD (hora local)
export function hoyISO(): string {
  return aISO(new Date());
}

// Suma meses a una fecha YYYY-MM-DD
export function sumarMeses(fecha: string, meses: number): string {
  const [y, m, d] = fecha.split('-').map(Number);
  return aISO(new Date(y, m - 1 + meses, d));
}

// Suma (o resta, si es negativo) días a una fecha YYYY-MM-DD
export function sumarDias(fecha: string, dias: number): string {
  const [y, m, d] = fecha.split('-').map(Number);
  return aISO(new Date(y, m - 1, d + dias));
}

// true si los periodos [inicioA, finA] y [inicioB, finB] tienen algún día en común
export function periodosSeCruzan(inicioA: string, finA: string, inicioB: string, finB: string): boolean {
  return inicioA <= finB && inicioB <= finA;
}

function aISO(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
