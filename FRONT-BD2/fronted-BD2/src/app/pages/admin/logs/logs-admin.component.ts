import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LogService } from '../../../core/services/log.service';
import { LogEvento, MODULOS_LOG, ModuloLog } from '../../../core/models/log.model';
import { hoyISO, sumarDias } from '../../../core/utils/fechas';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';

@Component({
  selector: 'app-logs-admin',
  standalone: true,
  imports: [FormsModule, EstadoBadgeComponent],
  templateUrl: './logs-admin.component.html'
})
export class LogsAdminComponent implements OnInit {

  logs: LogEvento[] = [];
  cargando = false;
  error = '';

  // Rango de fechas (por defecto, los últimos 30 días)
  hoy = hoyISO();
  desde = sumarDias(this.hoy, -29);
  hasta = this.hoy;

  // El módulo se filtra en el front para mostrar el conteo de cada chip
  modulos = MODULOS_LOG;
  moduloFiltro: ModuloLog | 'TODOS' = 'TODOS';

  constructor(
    private logService: LogService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const modulo = this.route.snapshot.queryParamMap.get('modulo') as ModuloLog | null;
    if (modulo && this.modulos.some(m => m.valor === modulo)) this.moduloFiltro = modulo;
    this.buscar();
  }

  buscar() {
    if (!this.desde || !this.hasta) {
      this.error = 'Selecciona las dos fechas.';
      return;
    }
    if (this.desde > this.hasta) {
      this.error = 'La fecha inicial no puede ser posterior a la final.';
      return;
    }

    this.error = '';
    this.cargando = true;
    this.logService.listar({ desde: this.desde, hasta: this.hasta }).subscribe({
      next: logs => {
        this.logs = logs;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los logs.';
        this.cargando = false;
      }
    });
  }

  get logsFiltrados(): LogEvento[] {
    return this.logs.filter(l => this.moduloFiltro === 'TODOS' || l.modulo === this.moduloFiltro);
  }

  cantidad(modulo: ModuloLog): number {
    return this.logs.filter(l => l.modulo === modulo).length;
  }

  textoModulo(modulo: ModuloLog): string {
    return this.modulos.find(m => m.valor === modulo)?.texto ?? modulo;
  }
}
