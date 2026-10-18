import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Contrato } from '../../../core/models/contrato.model';
import { EstadoPago, Pago } from '../../../core/models/pago.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';

@Component({
  selector: 'app-pagos-recibidos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './pagos-recibidos.component.html'
})
export class PagosRecibidosComponent implements OnInit {

  pagos: Pago[] = [];
  contratos: Contrato[] = [];
  cargando = true;

  // Filtros (el contrato puede venir desde "Ver pagos" en Contratos)
  contratoFiltro: number | 'TODOS' = 'TODOS';
  estadoFiltro: EstadoPago | 'TODOS' = 'TODOS';
  estados: EstadoPago[] = ['PAGADO', 'PENDIENTE', 'VENCIDO', 'CANCELADO'];

  constructor(
    private pagoService: PagoService,
    private contratoService: ContratoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const contrato = Number(this.route.snapshot.queryParamMap.get('contrato'));
    if (contrato) this.contratoFiltro = contrato;

    forkJoin({
      pagos: this.pagoService.listarRecibidos(),
      contratos: this.contratoService.listarComoArrendador()
    }).subscribe(({ pagos, contratos }) => {
      this.pagos = pagos.sort((a, b) => b.fechaVencimiento.localeCompare(a.fechaVencimiento));
      this.contratos = contratos;
      this.cargando = false;
    });
  }

  get pagosFiltrados(): Pago[] {
    return this.pagos
      .filter(p => this.contratoFiltro === 'TODOS' || p.idContrato === +this.contratoFiltro)
      .filter(p => this.estadoFiltro === 'TODOS' || p.estado === this.estadoFiltro);
  }

  get totalRecibido(): number {
    return this.sumar(this.pagosFiltrados.filter(p => p.estado === 'PAGADO'));
  }

  get totalPorCobrar(): number {
    return this.sumar(this.pagosFiltrados.filter(p => p.estado === 'PENDIENTE' || p.estado === 'VENCIDO'));
  }

  get vencidos(): number {
    return this.pagosFiltrados.filter(p => p.estado === 'VENCIDO').length;
  }

  contrato(idContrato: number): Contrato | undefined {
    return this.contratos.find(c => c.idContrato === idContrato);
  }

  private sumar(pagos: Pago[]): number {
    return pagos.reduce((suma, p) => suma + p.monto, 0);
  }
}
