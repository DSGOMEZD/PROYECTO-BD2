import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize, forkJoin, switchMap } from 'rxjs';

import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Contrato } from '../../../core/models/contrato.model';
import {
  EstadoPago,
  MetodoPago,
  Pago
} from '../../../core/models/pago.model';
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

  contratoFiltro: number | 'TODOS' = 'TODOS';
  estadoFiltro: EstadoPago | 'TODOS' = 'TODOS';
  estados: EstadoPago[] = ['PAGADO', 'PENDIENTE', 'VENCIDO', 'CANCELADO'];

  metodosPago: MetodoPago[] = [
    'EFECTIVO',
    'TARJETA',
    'TRANSFERENCIA',
    'PSE',
    'NEQUI'
  ];

  metodoSeleccionado: Record<number, MetodoPago | ''> = {};
  registrandoPagoId: number | null = null;
  errorPago = '';

  constructor(
    private pagoService: PagoService,
    private contratoService: ContratoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const contrato = Number(this.route.snapshot.queryParamMap.get('contrato'));
    if (contrato) this.contratoFiltro = contrato;

    forkJoin({
      pagos: this.pagoService.listarMios(),
      contratos: this.contratoService.listarMios()
    }).subscribe({
      next: ({ pagos, contratos }) => {
        this.pagos = this.ordenarPagos(pagos);
        this.contratos = contratos;
        this.cargando = false;
      },
      error: () => {
        this.errorPago = 'No se pudieron cargar los pagos.';
        this.cargando = false;
      }
    });
  }

  pagar(pago: Pago): void {
    const metodo = this.metodoSeleccionado[pago.idPago];

    if (pago.estado !== 'PENDIENTE' || !metodo) return;

    this.registrandoPagoId = pago.idPago;
    this.errorPago = '';

    this.pagoService.registrar(pago.idPago, metodo).pipe(
      switchMap(() => this.pagoService.listarMios()),
      finalize(() => this.registrandoPagoId = null)
    ).subscribe({
      next: pagos => {
        this.pagos = this.ordenarPagos(pagos);
      },
      error: err => {
        this.errorPago =
          err?.error?.mensaje ?? 'No se pudo registrar el pago.';
      }
    });
  }

  get pagosFiltrados(): Pago[] {
    return this.pagos
      .filter(p =>
        this.contratoFiltro === 'TODOS' ||
        p.idContrato === +this.contratoFiltro
      )
      .filter(p =>
        this.estadoFiltro === 'TODOS' ||
        p.estado === this.estadoFiltro
      );
  }

  get totalPagado(): number {
    return this.sumar(this.pagosFiltrados.filter(p => p.estado === 'PAGADO'));
  }

  get totalPendiente(): number {
    return this.sumar(
      this.pagosFiltrados.filter(
        p => p.estado === 'PENDIENTE' || p.estado === 'VENCIDO'
      )
    );
  }

  get proximoVencimiento(): Pago | undefined {
    return this.pagosFiltrados
      .filter(p => p.estado === 'PENDIENTE')
      .sort((a, b) =>
        a.fechaVencimiento.localeCompare(b.fechaVencimiento)
      )[0];
  }

  nombreContrato(idContrato: number): string {
    const contrato = this.contratos.find(c => c.idContrato === idContrato);
    return contrato
      ? `${contrato.moto.marca} ${contrato.moto.modelo}`
      : `Contrato #${idContrato}`;
  }

  private sumar(pagos: Pago[]): number {
    return pagos.reduce((suma, pago) => suma + pago.monto, 0);
  }

  private ordenarPagos(pagos: Pago[]): Pago[] {
    return [...pagos].sort((a, b) =>
      b.fechaVencimiento.localeCompare(a.fechaVencimiento)
    );
  }
}
