import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { MotoService } from '../../../core/services/moto.service';
import { EstadoMoto, Motocicleta, MotoRequest } from '../../../core/models/motocicleta.model';
import { EstadoPoliza } from '../../../core/models/seguro.model';
import { EstadoBadgeComponent } from '../../../shared/components/estado-badge/estado-badge.component';
import { hoyISO } from '../../../core/utils/fechas';

// Placa de moto en Colombia: 3 letras, 2 números y 1 letra (ej. ABC12D)
const PLACA_MOTO = /^[A-Z]{3}\d{2}[A-Z]$/;
// Máximo de una columna INT en MySQL (numero_poliza, precio, kilometraje)
const INT_MAX = 2_147_483_647;

@Component({
  selector: 'app-mis-motos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe, EstadoBadgeComponent],
  templateUrl: './mis-motos.component.html'
})
export class MisMotosComponent implements OnInit {

  motos: Motocicleta[] = [];
  cargando = true;
  filtro: EstadoMoto | 'TODAS' = 'TODAS';
  estados: (EstadoMoto | 'TODAS')[] = ['TODAS', 'DISPONIBLE', 'ARRENDADA', 'MANTENIMIENTO', 'INACTIVA'];
  estadosPoliza: EstadoPoliza[] = ['VIGENTE', 'VENCIDA', 'CANCELADA'];
  mensajeExito = '';

  // Modal para publicar / editar
  formAbierto = false;
  placaEditando?: string; // undefined = publicando una moto nueva
  form: MotoRequest = this.formVacio();
  error = '';
  guardando = false;

  hoy = hoyISO();
  anioMaximo = new Date().getFullYear() + 1;

  constructor(private motoService: MotoService) {}

  ngOnInit() {
    this.cargar();
  }

  get motosFiltradas(): Motocicleta[] {
    return this.filtro === 'TODAS' ? this.motos : this.motos.filter(m => m.estado === this.filtro);
  }

  contar(estado: EstadoMoto | 'TODAS'): number {
    return estado === 'TODAS' ? this.motos.length : this.motos.filter(m => m.estado === estado).length;
  }

  textoEstado(e: string): string {
    return e === 'TODAS' ? 'Todas' : e.charAt(0) + e.slice(1).toLowerCase();
  }

  abrirPublicar() {
    this.placaEditando = undefined;
    this.form = this.formVacio();
    this.error = '';
    this.formAbierto = true;
  }

  abrirEditar(m: Motocicleta) {
    this.placaEditando = m.placa;
    this.form = {
      placa: m.placa, marca: m.marca, modelo: m.modelo, anio: m.anio, cilindraje: m.cilindraje,
      color: m.color, kilometraje: m.kilometraje, precioDia: m.precioDia, descripcion: m.descripcion ?? '',
      seguro: m.seguro
        ? { numeroPoliza: m.seguro.numeroPoliza, aseguradora: m.seguro.aseguradora,
            fechaInicioVigencia: m.seguro.fechaInicioVigencia, estadoPoliza: m.seguro.estadoPoliza }
        : this.formVacio().seguro
    };
    this.error = '';
    this.formAbierto = true;
  }

  cerrarForm() {
    this.formAbierto = false;
  }

  guardar() {
    this.form.placa = this.form.placa.trim().toUpperCase();
    this.error = this.validar();
    if (this.error) return;

    const peticion: Observable<Motocicleta> = this.placaEditando
      ? this.motoService.actualizar(this.placaEditando, this.form)
      : this.motoService.publicar(this.form);

    this.guardando = true;
    peticion.subscribe({
      next: m => {
        this.mensajeExito = this.placaEditando
          ? `Se actualizó la ${m.marca} ${m.modelo}.`
          : `Publicaste la ${m.marca} ${m.modelo}. Ya aparece en el catálogo de motos disponibles.`;
        this.guardando = false;
        this.cerrarForm();
        this.cargar();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.guardando = false;
      }
    });
  }

  cambiarEstado(m: Motocicleta, estado: EstadoMoto) {
    const textos: Record<string, string> = {
      DISPONIBLE: 'volverá a aparecer en el catálogo',
      MANTENIMIENTO: 'quedará en mantenimiento y no se podrá solicitar',
      INACTIVA: 'dejará de aparecer en el catálogo'
    };
    if (!confirm(`La ${m.marca} ${m.modelo} (${m.placa}) ${textos[estado]}. ¿Continuar?`)) return;
    this.motoService.cambiarEstado(m.placa, estado).subscribe({
      next: () => this.cargar(),
      error: (e: Error) => alert(e.message)
    });
  }

  private validar(): string {
    const f = this.form;
    const s = f.seguro;
    if (!PLACA_MOTO.test(f.placa)) return 'La placa debe tener el formato ABC12D (3 letras, 2 números y 1 letra).';
    if (!f.marca.trim() || !f.modelo.trim()) return 'Escribe la marca y el modelo.';
    if (!f.anio || f.anio < 1980 || f.anio > this.anioMaximo) return `El año debe estar entre 1980 y ${this.anioMaximo}.`;
    if (!f.cilindraje || f.cilindraje <= 0) return 'El cilindraje debe ser mayor a 0.';
    if (f.kilometraje === null || f.kilometraje < 0) return 'El kilometraje no puede ser negativo.';
    if (!f.precioDia || f.precioDia <= 0) return 'El precio por día debe ser mayor a 0.';
    if (f.kilometraje > INT_MAX || f.precioDia > INT_MAX) return 'El kilometraje o el precio son demasiado grandes.';
    if (!s.aseguradora.trim()) return 'Escribe el nombre de la aseguradora.';
    if (!s.numeroPoliza || s.numeroPoliza <= 0 || !Number.isInteger(s.numeroPoliza)) return 'Escribe el número de la póliza.';
    if (s.numeroPoliza > INT_MAX) return 'El número de póliza es demasiado largo.';
    if (!s.fechaInicioVigencia) return 'Selecciona la fecha de inicio de vigencia del seguro.';
    if (s.fechaInicioVigencia > this.hoy && s.estadoPoliza === 'VIGENTE') {
      return 'Una póliza vigente no puede empezar en una fecha futura.';
    }
    return '';
  }

  private cargar() {
    this.motoService.listarMias().subscribe(motos => {
      this.motos = motos;
      this.cargando = false;
    });
  }

  private formVacio(): MotoRequest {
    return {
      placa: '', marca: '', modelo: '', anio: null, cilindraje: null, color: '',
      kilometraje: null, precioDia: null, descripcion: '',
      seguro: { numeroPoliza: null, aseguradora: '', fechaInicioVigencia: '', estadoPoliza: 'VIGENTE' }
    };
  }
}
