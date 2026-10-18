import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoMoto, Motocicleta, MotoRequest } from '../models/motocicleta.model';
import { Seguro } from '../models/seguro.model';
import { DOCUMENTO_ARRENDADOR_PRUEBA, MOTOS_PRUEBA, NOMBRE_ARRENDADOR_PRUEBA } from '../mocks/datos-prueba';

/**
 * Servicio de motocicletas.
 * Por ahora trabaja sobre los datos simulados en memoria; cuando exista el back,
 * inyecta HttpClient y reemplaza cada método por la llamada del TODO.
 */
@Injectable({ providedIn: 'root' })
export class MotoService {

  private apiUrl = `${environment.apiUrl}/motos`;

  // ===== Arrendatario =====

  // TODO: return this.http.get<Motocicleta[]>(`${this.apiUrl}?estado=DISPONIBLE`);
  listarDisponibles(): Observable<Motocicleta[]> {
    return of(MOTOS_PRUEBA.filter(m => m.estado === 'DISPONIBLE')).pipe(delay(300));
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Motocicleta[]>(`${this.apiUrl}/mias`);
  listarMias(): Observable<Motocicleta[]> {
    return of(MOTOS_PRUEBA.filter(m => m.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA)).pipe(delay(300));
  }

  // TODO: return this.http.post<Motocicleta>(this.apiUrl, datos);
  publicar(datos: MotoRequest): Observable<Motocicleta> {
    if (MOTOS_PRUEBA.some(m => m.placa === datos.placa)) {
      return throwError(() => new Error(`Ya existe una moto con la placa ${datos.placa}.`));
    }
    const idSeguro = Math.max(0, ...MOTOS_PRUEBA.map(m => m.seguro?.idSeguro ?? 0)) + 1;
    const nueva: Motocicleta = {
      ...this.aMoto(datos),
      estado: 'DISPONIBLE',
      documentoArrendador: DOCUMENTO_ARRENDADOR_PRUEBA,
      nombreArrendador: NOMBRE_ARRENDADOR_PRUEBA,
      seguro: this.aSeguro(datos, idSeguro)
    };
    MOTOS_PRUEBA.push(nueva);
    return of(nueva).pipe(delay(300));
  }

  // TODO: return this.http.put<Motocicleta>(`${this.apiUrl}/${placa}`, datos);
  actualizar(placa: string, datos: MotoRequest): Observable<Motocicleta> {
    const moto = this.buscarMia(placa);
    if (!moto) return throwError(() => new Error('La moto no existe'));
    Object.assign(moto, this.aMoto(datos), {
      placa, // la placa es la llave, no se cambia
      seguro: this.aSeguro(datos, moto.seguro?.idSeguro ?? 0)
    });
    return of(moto).pipe(delay(300));
  }

  // TODO: return this.http.patch<Motocicleta>(`${this.apiUrl}/${placa}/estado`, { estado });
  cambiarEstado(placa: string, estado: EstadoMoto): Observable<Motocicleta> {
    const moto = this.buscarMia(placa);
    if (!moto) return throwError(() => new Error('La moto no existe'));
    if (moto.estado === 'ARRENDADA') {
      return throwError(() => new Error('No puedes cambiar el estado de una moto con un contrato vigente.'));
    }
    moto.estado = estado;
    return of(moto).pipe(delay(300));
  }

  private buscarMia(placa: string): Motocicleta | undefined {
    return MOTOS_PRUEBA.find(m => m.placa === placa && m.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA);
  }

  private aMoto(datos: MotoRequest) {
    return {
      placa: datos.placa,
      marca: datos.marca,
      modelo: datos.modelo,
      anio: datos.anio ?? 0,
      cilindraje: datos.cilindraje ?? 0,
      color: datos.color,
      kilometraje: datos.kilometraje ?? 0,
      precioDia: datos.precioDia ?? 0,
      descripcion: datos.descripcion || undefined
    };
  }

  private aSeguro(datos: MotoRequest, idSeguro: number): Seguro {
    return { ...datos.seguro, idSeguro, numeroPoliza: datos.seguro.numeroPoliza ?? 0 };
  }
}
