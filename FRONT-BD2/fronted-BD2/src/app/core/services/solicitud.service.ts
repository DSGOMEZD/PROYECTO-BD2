import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NuevaSolicitudRequest, Solicitud } from '../models/solicitud.model';
import { DOCUMENTO_ARRENDATARIO_PRUEBA, MOTOS_PRUEBA, SOLICITUDES_PRUEBA } from '../mocks/datos-prueba';
import { hoyISO } from '../utils/fechas';

/**
 * Servicio de solicitudes de arrendamiento.
 * Por ahora trabaja sobre una copia en memoria de los datos simulados.
 */
@Injectable({ providedIn: 'root' })
export class SolicitudService {

  private apiUrl = `${environment.apiUrl}/solicitudes`;

  // TEMPORAL: copia en memoria para poder crear/cancelar sin back
  private solicitudes = [...SOLICITUDES_PRUEBA];

  // TODO: return this.http.get<Solicitud[]>(`${this.apiUrl}/mias`);
  listarMias(): Observable<Solicitud[]> {
    return of([...this.solicitudes]).pipe(delay(300));
  }

  // TODO: return this.http.post<Solicitud>(this.apiUrl, datos);
  crear(datos: NuevaSolicitudRequest): Observable<Solicitud> {
    const moto = MOTOS_PRUEBA.find(m => m.placa === datos.placa);
    if (!moto) return throwError(() => new Error('La moto no existe'));

    const nueva: Solicitud = {
      idSolicitud: Math.max(0, ...this.solicitudes.map(s => s.idSolicitud)) + 1,
      fechaSolicitud: hoyISO(),
      fechaInicio: datos.fechaInicio,
      fechaFin: datos.fechaFin,
      mensaje: datos.mensaje,
      estado: 'PENDIENTE',
      documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
      moto: { placa: moto.placa, marca: moto.marca, modelo: moto.modelo, precioDia: moto.precioDia }
    };
    this.solicitudes = [nueva, ...this.solicitudes];
    return of(nueva).pipe(delay(300));
  }

  // TODO: return this.http.patch<Solicitud>(`${this.apiUrl}/${id}/cancelar`, {});
  cancelar(id: number): Observable<Solicitud> {
    const solicitud = this.solicitudes.find(s => s.idSolicitud === id);
    if (!solicitud) return throwError(() => new Error('La solicitud no existe'));
    solicitud.estado = 'CANCELADA';
    return of(solicitud).pipe(delay(300));
  }
}
