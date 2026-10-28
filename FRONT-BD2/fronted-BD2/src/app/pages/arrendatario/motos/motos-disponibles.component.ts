import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MotoService } from '../../../core/services/moto.service';
import { SolicitudService } from '../../../core/services/solicitud.service';
import { Motocicleta } from '../../../core/models/motocicleta.model';
import { NuevaSolicitudRequest } from '../../../core/models/solicitud.model';
import { diasEntre, hoyISO } from '../../../core/utils/fechas';

type RangoCilindraje = 'TODAS' | 'BAJO' | 'MEDIO' | 'ALTO';

@Component({
  selector: 'app-motos-disponibles',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DecimalPipe],
  templateUrl: './motos-disponibles.component.html'
})
export class MotosDisponiblesComponent implements OnInit {

  motos: Motocicleta[] = [];
  cargando = true;

  // Filtros
  busqueda = '';
  cilindraje: RangoCilindraje = 'TODAS';
  precioMaximo: number | null = null;
  orden: 'PRECIO_ASC' | 'PRECIO_DESC' = 'PRECIO_ASC';

  // Modal de solicitud
  motoSeleccionada?: Motocicleta;
  solicitud: NuevaSolicitudRequest = this.solicitudVacia();
  error = '';
  enviando = false;
  mensajeExito = '';

  hoy = hoyISO();
  fechaInicioBusqueda = '';
  fechaFinBusqueda = '';
  errorCatalogo = '';
  catalogoConsultado = false;

  constructor(
    private motoService: MotoService,
    private solicitudService: SolicitudService
  ) {}

  ngOnInit() {
    this.cargando = false;
  }

  get motosFiltradas(): Motocicleta[] {
    const texto = this.busqueda.trim().toLowerCase();
    return this.motos
      .filter(m => !texto || `${m.marca} ${m.modelo}`.toLowerCase().includes(texto))
      .filter(m => this.cumpleCilindraje(m.cilindraje))
      .filter(m => !this.precioMaximo || m.precioDia <= this.precioMaximo)
      .sort((a, b) => this.orden === 'PRECIO_ASC' ? a.precioDia - b.precioDia : b.precioDia - a.precioDia);
  }

  limpiarFiltros() {
    this.busqueda = '';
    this.cilindraje = 'TODAS';
    this.precioMaximo = null;
  }

  abrirSolicitud(moto: Motocicleta) {
    this.motoSeleccionada = moto;
    this.solicitud = {
      ...this.solicitudVacia(),
      placa: moto.placa,
      fechaInicio: this.fechaInicioBusqueda,
      fechaFin: this.fechaFinBusqueda
    };
    this.error = '';
  }

  cerrarSolicitud() {
    this.motoSeleccionada = undefined;
  }

  get dias(): number {
    return diasEntre(this.solicitud.fechaInicio, this.solicitud.fechaFin);
  }

  get totalEstimado(): number {
    return this.dias * (this.motoSeleccionada?.precioDia ?? 0);
  }

  enviarSolicitud() {
    const { fechaInicio, fechaFin } = this.solicitud;
    if (!fechaInicio || !fechaFin) {
      this.error = 'Selecciona la fecha de inicio y la de fin.';
      return;
    }
    if (fechaInicio < this.hoy) {
      this.error = 'La fecha de inicio no puede ser anterior a hoy.';
      return;
    }
    if (fechaFin < fechaInicio) {
      this.error = 'La fecha de fin debe ser igual o posterior a la de inicio.';
      return;
    }

    this.enviando = true;
    this.solicitudService.crear(this.solicitud).subscribe({
      next: () => {
        this.mensajeExito = `Solicitud enviada para la ${this.motoSeleccionada?.marca} ${this.motoSeleccionada?.modelo}.`;
        this.enviando = false;
        this.cerrarSolicitud();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.enviando = false;
      }
    });
  }

  private cumpleCilindraje(cc: number): boolean {
    switch (this.cilindraje) {
      case 'BAJO': return cc <= 150;
      case 'MEDIO': return cc > 150 && cc <= 300;
      case 'ALTO': return cc > 300;
      default: return true;
    }
  }

  private solicitudVacia(): NuevaSolicitudRequest {
    return { placa: '', fechaInicio: '', fechaFin: '', mensaje: '' };
  }

  buscarDisponibles() {
    this.errorCatalogo = '';

    if (!this.fechaInicioBusqueda || !this.fechaFinBusqueda) {
      this.errorCatalogo = 'Selecciona las fechas de inicio y fin.';
      return;
    }

    if (this.fechaInicioBusqueda <= this.hoy) {
      this.errorCatalogo = 'La fecha de inicio debe ser posterior a hoy.';
      return;
    }

    if (this.fechaFinBusqueda <= this.fechaInicioBusqueda) {
      this.errorCatalogo = 'La fecha de fin debe ser posterior a la fecha de inicio.';
      return;
    }

    this.cargando = true;

    this.motoService.listarDisponibles(
      this.fechaInicioBusqueda,
      this.fechaFinBusqueda
    ).subscribe({
      next: motos => {
        this.motos = motos;
        this.catalogoConsultado = true;
        this.cargando = false;
      },
      error: err => {
        this.errorCatalogo =
          err.error?.mensaje ?? 'No se pudo cargar el catálogo.';
        this.cargando = false;
      }
    });
  }
}
