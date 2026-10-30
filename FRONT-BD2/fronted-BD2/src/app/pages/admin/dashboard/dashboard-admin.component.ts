import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LogService } from '../../../core/services/log.service';
import { LogEvento, MODULOS_LOG, ModuloLog } from '../../../core/models/log.model';
import { hoyISO, sumarDias } from '../../../core/utils/fechas';

@Component({
  selector: 'app-dashboard-admin',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard-admin.component.html'
})
export class DashboardAdminComponent implements OnInit {

  logs: LogEvento[] = [];
  cargando = true;
  modulos = MODULOS_LOG;

  constructor(private logService: LogService) {}

  ngOnInit() {
    // Resumen de la última semana
    this.logService.listar({ desde: sumarDias(hoyISO(), -6), hasta: hoyISO() })
      .subscribe(logs => {
        this.logs = logs;
        this.cargando = false;
      });
  }

  cantidad(modulo: ModuloLog): number {
    return this.logs.filter(l => l.modulo === modulo).length;
  }

  get recientes(): LogEvento[] {
    return this.logs.slice(0, 5);
  }
}
