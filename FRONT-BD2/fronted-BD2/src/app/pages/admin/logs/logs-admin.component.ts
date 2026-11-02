import { Component, OnInit } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LogService } from '../../../core/services/log.service';
import { LogEvento, MODULOS_LOG, ModuloLog, ValorDetalle } from '../../../core/models/log.model';
import { hoyISO, sumarDias } from '../../../core/utils/fechas';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';

// Campo del detalle listo para mostrar
interface CampoDetalle {
  etiqueta: string;
  valor: string;
  hijos?: CampoDetalle[];   // objetos anidados (moto, seguro)
  lista?: CampoDetalle[][]; // listas de objetos (pagos generados)
}

// Campos que son dinero y se muestran como $1.234.567
const CAMPOS_DINERO = ['monto', 'valorTotal', 'valorEstimado', 'deposito', 'precioDia', 'precioDiaAnterior'];

@Component({
  selector: 'app-logs-admin',
  standalone: true,
  imports: [FormsModule, NgTemplateOutlet, EstadoBadgeComponent],
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

  // El módulo y el texto se filtran en el front para mostrar el conteo de cada chip
  modulos = MODULOS_LOG;
  moduloFiltro: ModuloLog | 'TODOS' = 'TODOS';
  texto = '';

  // Evento con el detalle abierto
  abierto: string | null = null;

  private formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

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
    this.abierto = null;
    this.logService.listar({ desde: this.desde, hasta: this.hasta }).subscribe({
      next: logs => {
        this.logs = logs;
        this.cargando = false;
      },
      error: err => {
        this.error = err?.error?.mensaje ?? err?.message ?? 'No se pudieron cargar los logs.';
        this.cargando = false;
      }
    });
  }

  // Logs que pasan el filtro de texto (los chips cuentan sobre estos)
  private get logsPorTexto(): LogEvento[] {
    const t = this.texto.trim().toLowerCase();
    if (!t) return this.logs;
    return this.logs.filter(l =>
      [l.nombreUsuario, l.referencia, l.descripcion, l.accion, String(l.documentoUsuario)]
        .some(campo => campo?.toLowerCase().includes(t)));
  }

  get logsFiltrados(): LogEvento[] {
    return this.logsPorTexto.filter(l => this.moduloFiltro === 'TODOS' || l.modulo === this.moduloFiltro);
  }

  get total(): number {
    return this.logsPorTexto.length;
  }

  cantidad(modulo: ModuloLog): number {
    return this.logsPorTexto.filter(l => l.modulo === modulo).length;
  }

  textoModulo(modulo: ModuloLog): string {
    return this.modulos.find(m => m.valor === modulo)?.texto ?? modulo;
  }

  alternar(id: string) {
    this.abierto = this.abierto === id ? null : id;
  }

  // ---- Detalle ----

  campos(detalle: { [clave: string]: ValorDetalle } | null | undefined): CampoDetalle[] {
    if (!detalle) return [];
    return Object.entries(detalle).map(([clave, valor]) => this.campo(clave, valor));
  }

  private campo(clave: string, valor: ValorDetalle): CampoDetalle {
    const etiqueta = this.etiqueta(clave);
    if (Array.isArray(valor)) {
      const objetos = valor.filter(v => v !== null && typeof v === 'object' && !Array.isArray(v)) as { [k: string]: ValorDetalle }[];
      return objetos.length === valor.length && objetos.length > 0
        ? { etiqueta, valor: `${valor.length}`, lista: objetos.map(o => this.campos(o)) }
        : { etiqueta, valor: valor.length ? valor.join(', ') : '—' };
    }
    if (valor !== null && typeof valor === 'object') {
      return { etiqueta, valor: '', hijos: this.campos(valor) };
    }
    return { etiqueta, valor: this.formatear(clave, valor) };
  }

  private formatear(clave: string, valor: string | number | boolean | null): string {
    if (valor === null || valor === '') return '—';
    if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
    if (typeof valor === 'number' && CAMPOS_DINERO.includes(clave)) return this.formatoPesos.format(valor);
    if (typeof valor === 'number') return valor.toLocaleString('es-CO');
    return valor;
  }

  // "fechaInicioVigencia" -> "Fecha inicio vigencia"
  private etiqueta(clave: string): string {
    const texto = clave.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
