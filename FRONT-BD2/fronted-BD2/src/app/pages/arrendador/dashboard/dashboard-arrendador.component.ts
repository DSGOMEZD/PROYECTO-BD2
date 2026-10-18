import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MotoService } from '../../../core/services/moto.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Motocicleta } from '../../../core/models/motocicleta.model';
import { Solicitud } from '../../../core/models/solicitud.model';
import { diasEntre } from '../../../core/utils/fechas';

// Página de inicio del arrendador: resumen de sus motos y alquileres
@Component({
  selector: 'app-dashboard-arrendador',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './dashboard-arrendador.component.html'
})
export class DashboardArrendadorComponent implements OnInit {

  cargando = true;
  totalMotos = 0;
  motosDisponibles = 0;
  contratosVigentes = 0;
  totalRecibido = 0;
  porCobrar = 0;
  pendientes: Solicitud[] = [];
  motosSinSeguro: Motocicleta[] = [];

  constructor(
    private motoService: MotoService,
    private solicitudService: SolicitudService,
    private contratoService: ContratoService,
    private pagoService: PagoService
  ) {}

  ngOnInit() {
    forkJoin({
      motos: this.motoService.listarMias(),
      solicitudes: this.solicitudService.listarRecibidas(),
      contratos: this.contratoService.listarComoArrendador(),
      pagos: this.pagoService.listarRecibidos()
    }).subscribe(({ motos, solicitudes, contratos, pagos }) => {
      this.totalMotos = motos.length;
      this.motosDisponibles = motos.filter(m => m.estado === 'DISPONIBLE').length;
      this.motosSinSeguro = motos.filter(m => m.seguro?.estadoPoliza !== 'VIGENTE');
      this.pendientes = solicitudes
        .filter(s => s.estado === 'PENDIENTE')
        .sort((a, b) => a.fechaSolicitud.localeCompare(b.fechaSolicitud));
      this.contratosVigentes = contratos.filter(c => c.estado === 'VIGENTE').length;
      this.totalRecibido = pagos.filter(p => p.estado === 'PAGADO').reduce((s, p) => s + p.monto, 0);
      this.porCobrar = pagos
        .filter(p => p.estado === 'PENDIENTE' || p.estado === 'VENCIDO')
        .reduce((s, p) => s + p.monto, 0);
      this.cargando = false;
    });
  }

  total(s: Solicitud): number {
    return diasEntre(s.fechaInicio, s.fechaFin) * s.moto.precioDia;
  }
}
