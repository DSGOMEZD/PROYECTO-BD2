import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MotoService } from '../../../core/services/moto.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Pago } from '../../../core/models/pago.model';

// Página de inicio del arrendatario: resumen de su actividad
@Component({
  selector: 'app-dashboard-arrendatario',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './dashboard-arrendatario.component.html'
})
export class DashboardArrendatarioComponent implements OnInit {

  cargando = true;
  motosDisponibles = 0;
  solicitudesPendientes = 0;
  contratosVigentes = 0;
  proximoPago?: Pago;

  constructor(
    private motoService: MotoService,
    private solicitudService: SolicitudService,
    private contratoService: ContratoService,
    private pagoService: PagoService
  ) {}

  ngOnInit() {
    forkJoin({
      motos: this.motoService.listarDisponibles(),
      solicitudes: this.solicitudService.listarMias(),
      contratos: this.contratoService.listarMios(),
      pagos: this.pagoService.listarMios()
    }).subscribe(({ motos, solicitudes, contratos, pagos }) => {
      this.motosDisponibles = motos.length;
      this.solicitudesPendientes = solicitudes.filter(s => s.estado === 'PENDIENTE').length;
      this.contratosVigentes = contratos.filter(c => c.estado === 'VIGENTE').length;
      this.proximoPago = pagos
        .filter(p => p.estado === 'PENDIENTE')
        .sort((a, b) => a.fechaVencimiento.localeCompare(b.fechaVencimiento))[0];
      this.cargando = false;
    });
  }
}
