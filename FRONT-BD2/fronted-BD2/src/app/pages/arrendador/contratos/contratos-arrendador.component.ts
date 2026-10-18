import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ContratoService } from '../../../core/services/contrato.service';
import { PagoService } from '../../../core/services/pago.service';
import { Contrato, EstadoContrato } from '../../../core/models/contrato.model';
import { Pago } from '../../../core/models/pago.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';
import { diasEntre, hoyISO } from '../../../core/utils/fechas';

@Component({
  selector: 'app-contratos-arrendador',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, EstadoBadgeComponent],
  templateUrl: './contratos-arrendador.component.html'
})
export class ContratosArrendadorComponent implements OnInit {

  contratos: Contrato[] = [];
  pagos: Pago[] = [];
  cargando = true;
  filtro: EstadoContrato | 'TODOS' = 'VIGENTE';
  estados: (EstadoContrato | 'TODOS')[] = ['VIGENTE', 'FINALIZADO', 'CANCELADO', 'TODOS'];
  mensajeExito = '';
  cancelando?: number;

  constructor(
    private contratoService: ContratoService,
    private pagoService: PagoService
  ) {}

  ngOnInit() {
    this.cargar();
  }

  get contratosFiltrados(): Contrato[] {
    return this.filtro === 'TODOS' ? this.contratos : this.contratos.filter(c => c.estado === this.filtro);
  }

  contar(estado: EstadoContrato | 'TODOS'): number {
    return estado === 'TODOS' ? this.contratos.length : this.contratos.filter(c => c.estado === estado).length;
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

  cancelar(c: Contrato) {
    const aviso = `¿Cancelar el contrato #${c.idContrato} con ${c.nombreArrendatario}?\n\n`
      + `La ${c.moto.marca} ${c.moto.modelo} quedará disponible otra vez y los pagos pendientes se cancelarán.`;
    if (!confirm(aviso)) return;

    this.cancelando = c.idContrato;
    this.contratoService.cancelar(c.idContrato).subscribe({
      next: () => {
        this.mensajeExito = `Cancelaste el contrato #${c.idContrato}.`;
        this.cancelando = undefined;
        this.cargar();
      },
      error: (e: Error) => {
        alert(e.message);
        this.cancelando = undefined;
      }
    });
  }

  private cargar() {
    forkJoin({
      contratos: this.contratoService.listarComoArrendador(),
      pagos: this.pagoService.listarRecibidos()
    }).subscribe(({ contratos, pagos }) => {
      this.contratos = contratos.sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio));
      this.pagos = pagos;
      this.cargando = false;
    });
  }
}
