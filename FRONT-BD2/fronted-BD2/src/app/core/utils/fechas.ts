// Días de alquiler entre dos fechas YYYY-MM-DD (ambos días incluidos)
export function diasEntre(inicio: string, fin: string): number {
  if (!inicio || !fin) return 0;
  const ms = new Date(fin).getTime() - new Date(inicio).getTime();
  return ms < 0 ? 0 : Math.round(ms / 86_400_000) + 1;
}

// Fecha de hoy en formato YYYY-MM-DD (hora local)
export function hoyISO(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
