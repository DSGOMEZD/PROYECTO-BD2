import { Component, Input } from '@angular/core';

// Etiqueta de color para los estados de motos, solicitudes, contratos y pagos
@Component({
  selector: 'app-estado-badge',
  standalone: true,
  template: `<span class="estado" [class]="'estado estado-' + tono">{{ texto }}</span>`
})
export class EstadoBadgeComponent {
  @Input({ required: true }) estado = '';

  get texto(): string {
    return this.estado.charAt(0) + this.estado.slice(1).toLowerCase();
  }

  get tono(): string {
    switch (this.estado) {
      case 'DISPONIBLE':
      case 'APROBADA':
      case 'VIGENTE':
      case 'PAGADO':
        return 'ok';
      case 'PENDIENTE':
        return 'espera';
      case 'RECHAZADA':
      case 'VENCIDO':
        return 'error';
      default:
        return 'neutro'; // CANCELADA, FINALIZADO, ARRENDADA, etc.
    }
  }
}
