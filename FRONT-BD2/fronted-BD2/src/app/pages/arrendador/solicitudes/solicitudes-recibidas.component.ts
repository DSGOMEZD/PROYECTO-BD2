import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { EstadoSolicitud, Solicitud } from '../../../core/models/solicitud.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';
import { diasEntre, periodosSeCruzan } from '../../../core/utils/fechas';
import { Cuota, planDePagos } from '../../../core/utils/pagos';

@Component({
  selector: 'app-solicitudes-recibidas',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './solicitudes-recibidas.component.html'
})
export class SolicitudesRecibidasComponent implements OnInit {

  solicitudes: Solicitud[] = [];
  cargando = true;
  filtro: EstadoSolicitud | 'TODAS' = 'PENDIENTE';
  estados: (EstadoSolicitud | 'TODAS')[] = ['PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA', 'TODAS'];
  mensajeExito = '';

  // Modal de aprobación
  aprobando?: Solicitud;
  deposito: number | null = 0;
  error = '';
  enviando = false;

  constructor(private solicitudService: SolicitudService) {}

  ngOnInit() {
    this.cargar();
  }

  get solicitudesFiltradas(): Solicitud[] {
    return this.filtro === 'TODAS'
      ? this.solicitudes
      : this.solicitudes.filter(s => s.estado === this.filtro);
  }

  contar(estado: EstadoSolicitud | 'TODAS'): number {
    return estado === 'TODAS'
      ? this.solicitudes.length
      : this.solicitudes.filter(s => s.estado === estado).length;
  }

  dias(s: Solicitud): number {
    return diasEntre(s.fechaInicio, s.fechaFin);
  }

  total(s: Solicitud): number {
    return this.dias(s) * s.moto.precioDia;
  }

  // Otras solicitudes pendientes de la misma moto que se cruzan en fechas (se rechazarán al aprobar)
  enConflicto(s: Solicitud): Solicitud[] {
    return this.solicitudes.filter(o => o !== s && o.estado === 'PENDIENTE' && o.moto.placa === s.moto.placa
      && periodosSeCruzan(o.fechaInicio, o.fechaFin, s.fechaInicio, s.fechaFin));
  }

  get cuotas(): Cuota[] {
    return this.aprobando ? planDePagos(this.aprobando.fechaInicio, this.aprobando.fechaFin, this.total(this.aprobando)) : [];
  }

  abrirAprobar(s: Solicitud) {
    this.aprobando = s;
    this.deposito = 0;
    this.error = '';
  }

  cerrarAprobar() {
    this.aprobando = undefined;
  }

  confirmarAprobar() {
    const s = this.aprobando;
    if (!s) return;
    if (this.deposito === null || this.deposito < 0) {
      this.error = 'El depósito no puede ser negativo.';
      return;
    }

    this.enviando = true;
    this.solicitudService.aprobar(s.idSolicitud, { deposito: this.deposito }).subscribe({
      next: c => {
        this.mensajeExito = `Aprobaste la solicitud de ${s.nombreArrendatario}. Se generó el contrato #${c.idContrato}.`;
        this.enviando = false;
        this.cerrarAprobar();
        this.cargar();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.enviando = false;
      }
    });
  }

  rechazar(s: Solicitud) {
    if (!confirm(`¿Rechazar la solicitud de ${s.nombreArrendatario} para la ${s.moto.marca} ${s.moto.modelo}?`)) return;
    this.solicitudService.rechazar(s.idSolicitud).subscribe({
      next: () => {
        this.mensajeExito = `Rechazaste la solicitud de ${s.nombreArrendatario}.`;
        this.cargar();
      },
      error: (e: Error) => alert(e.message)
    });
  }

  private cargar() {
    this.solicitudService.listarRecibidas().subscribe(solicitudes => {
      this.solicitudes = solicitudes.sort((a, b) => b.fechaSolicitud.localeCompare(a.fechaSolicitud));
      this.cargando = false;
    });
  }
}
