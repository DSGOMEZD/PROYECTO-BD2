import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Contrato } from '../../../core/models/contrato.model';
import { Pago } from '../../../core/models/pago.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';
import { diasEntre, hoyISO } from '../../../core/utils/fechas';

@Component({
  selector: 'app-mis-contratos',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './mis-contratos.component.html'
})
export class MisContratosComponent implements OnInit {

  contratos: Contrato[] = [];
  pagos: Pago[] = [];
  cargando = true;
  soloVigentes = true;

  constructor(
    private contratoService: ContratoService,
    private pagoService: PagoService
  ) {}

  ngOnInit() {
    forkJoin({
      contratos: this.contratoService.listarMios(),
      pagos: this.pagoService.listarMios()
    }).subscribe(({ contratos, pagos }) => {
      this.contratos = contratos;
      this.pagos = pagos;
      this.cargando = false;
    });
  }

  get contratosFiltrados(): Contrato[] {
    return this.soloVigentes ? this.contratos.filter(c => c.estado === 'VIGENTE') : this.contratos;
  }

  totalPagado(c: Contrato): number {
    return this.pagos
      .filter(p => p.idContrato === c.idContrato && p.estado === 'PAGADO')
      .reduce((suma, p) => suma + p.monto, 0);
  }

  porcentajePagado(c: Contrato): number {
    return c.valorTotal ? Math.min(100, Math.round(this.totalPagado(c) * 100 / c.valorTotal)) : 0;
  }

  diasRestantes(c: Contrato): number {
    return Math.max(0, diasEntre(hoyISO(), c.fechaFin) - 1);
  }
}
