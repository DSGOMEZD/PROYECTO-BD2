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
  selector: 'app-mis-pagos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './mis-pagos.component.html'
})
export class MisPagosComponent implements OnInit {

  pagos: Pago[] = [];
  contratos: Contrato[] = [];
  cargando = true;

  // Filtros (el contrato puede venir desde "Ver pagos" en Mis contratos)
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
      pagos: this.pagoService.listarMios(),
      contratos: this.contratoService.listarMios()
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

  get totalPagado(): number {
    return this.sumar(this.pagosFiltrados.filter(p => p.estado === 'PAGADO'));
  }

  get totalPendiente(): number {
    return this.sumar(this.pagosFiltrados.filter(p => p.estado === 'PENDIENTE' || p.estado === 'VENCIDO'));
  }

  get proximoVencimiento(): Pago | undefined {
    return this.pagosFiltrados
      .filter(p => p.estado === 'PENDIENTE')
      .sort((a, b) => a.fechaVencimiento.localeCompare(b.fechaVencimiento))[0];
  }

  nombreContrato(idContrato: number): string {
    const c = this.contratos.find(x => x.idContrato === idContrato);
    return c ? `${c.moto.marca} ${c.moto.modelo}` : `Contrato #${idContrato}`;
  }

  private sumar(pagos: Pago[]): number {
    return pagos.reduce((suma, p) => suma + p.monto, 0);
  }
}
