import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { EstadoSolicitud, Solicitud } from '../../../core/models/solicitud.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';
import { diasEntre } from '../../../core/utils/fechas';

@Component({
  selector: 'app-mis-solicitudes',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './mis-solicitudes.component.html'
})
export class MisSolicitudesComponent implements OnInit {

  solicitudes: Solicitud[] = [];
  cargando = true;
  filtro: EstadoSolicitud | 'TODAS' = 'TODAS';
  estados: (EstadoSolicitud | 'TODAS')[] = ['TODAS', 'PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA'];

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

  cancelar(s: Solicitud) {
    if (!confirm(`¿Cancelar la solicitud de la ${s.moto.marca} ${s.moto.modelo}?`)) return;
    this.solicitudService.cancelar(s.idSolicitud).subscribe(() => this.cargar());
  }

  private cargar() {
    this.solicitudService.listarMias().subscribe(solicitudes => {
      this.solicitudes = solicitudes.sort((a, b) => b.fechaSolicitud.localeCompare(a.fechaSolicitud));
      this.cargando = false;
    });
  }
}
