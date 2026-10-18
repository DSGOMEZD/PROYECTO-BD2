import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AprobarSolicitudRequest, NuevaSolicitudRequest, Solicitud } from '../models/solicitud.model';
import { Contrato } from '../models/contrato.model';
import {
  CONTRATOS_PRUEBA, DOCUMENTO_ARRENDADOR_PRUEBA, DOCUMENTO_ARRENDATARIO_PRUEBA, MOTOS_PRUEBA,
  NOMBRE_ARRENDATARIO_PRUEBA, PAGOS_PRUEBA, SOLICITUDES_PRUEBA
} from '../mocks/datos-prueba';
import { diasEntre, hoyISO, periodosSeCruzan } from '../utils/fechas';
import { planDePagos } from '../utils/pagos';

/**
 * Servicio de solicitudes de arrendamiento.
 * Por ahora trabaja sobre los datos simulados en memoria.
 */
@Injectable({ providedIn: 'root' })
export class SolicitudService {

  private apiUrl = `${environment.apiUrl}/solicitudes`;

  // ===== Arrendatario =====

  // TODO: return this.http.get<Solicitud[]>(`${this.apiUrl}/mias`);
  listarMias(): Observable<Solicitud[]> {
    return of(SOLICITUDES_PRUEBA.filter(s => s.documentoArrendatario === DOCUMENTO_ARRENDATARIO_PRUEBA)).pipe(delay(300));
  }

  // TODO: return this.http.post<Solicitud>(this.apiUrl, datos);
  crear(datos: NuevaSolicitudRequest): Observable<Solicitud> {
    const moto = MOTOS_PRUEBA.find(m => m.placa === datos.placa);
    if (!moto) return throwError(() => new Error('La moto no existe'));

    const nueva: Solicitud = {
      idSolicitud: Math.max(0, ...SOLICITUDES_PRUEBA.map(s => s.idSolicitud)) + 1,
      fechaSolicitud: hoyISO(),
      fechaInicio: datos.fechaInicio,
      fechaFin: datos.fechaFin,
      mensaje: datos.mensaje,
      estado: 'PENDIENTE',
      documentoArrendatario: DOCUMENTO_ARRENDATARIO_PRUEBA,
      nombreArrendatario: NOMBRE_ARRENDATARIO_PRUEBA,
      documentoArrendador: moto.documentoArrendador,
      moto: { placa: moto.placa, marca: moto.marca, modelo: moto.modelo, precioDia: moto.precioDia }
    };
    SOLICITUDES_PRUEBA.unshift(nueva);
    return of(nueva).pipe(delay(300));
  }

  // TODO: return this.http.patch<Solicitud>(`${this.apiUrl}/${id}/cancelar`, {});
  cancelar(id: number): Observable<Solicitud> {
    const solicitud = SOLICITUDES_PRUEBA.find(s => s.idSolicitud === id);
    if (!solicitud) return throwError(() => new Error('La solicitud no existe'));
    solicitud.estado = 'CANCELADA';
    return of(solicitud).pipe(delay(300));
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Solicitud[]>(`${this.apiUrl}/recibidas`);
  listarRecibidas(): Observable<Solicitud[]> {
    return of(SOLICITUDES_PRUEBA.filter(s => s.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA)).pipe(delay(300));
  }

  /**
   * Aprueba la solicitud: el back debe crear el contrato, generar los pagos,
   * marcar la moto como ARRENDADA y rechazar las otras solicitudes pendientes
   * de la misma moto que se crucen en fechas.
   */
  // TODO: return this.http.post<Contrato>(`${this.apiUrl}/${id}/aprobar`, datos);
  aprobar(id: number, datos: AprobarSolicitudRequest): Observable<Contrato> {
    const solicitud = this.buscarRecibida(id);
    if (!solicitud || solicitud.estado !== 'PENDIENTE') {
      return throwError(() => new Error('La solicitud ya no está pendiente.'));
    }
    const choque = CONTRATOS_PRUEBA.some(c => c.moto.placa === solicitud.moto.placa && c.estado === 'VIGENTE'
      && periodosSeCruzan(c.fechaInicio, c.fechaFin, solicitud.fechaInicio, solicitud.fechaFin));
    if (choque) {
      return throwError(() => new Error('La moto ya tiene un contrato vigente en esas fechas.'));
    }

    // --- TEMPORAL: todo esto lo hará el back ---
    const hoy = hoyISO();
    solicitud.estado = 'APROBADA';
    solicitud.fechaRespuesta = hoy;

    SOLICITUDES_PRUEBA
      .filter(s => s !== solicitud && s.estado === 'PENDIENTE' && s.moto.placa === solicitud.moto.placa
        && periodosSeCruzan(s.fechaInicio, s.fechaFin, solicitud.fechaInicio, solicitud.fechaFin))
      .forEach(s => { s.estado = 'RECHAZADA'; s.fechaRespuesta = hoy; });

    const moto = MOTOS_PRUEBA.find(m => m.placa === solicitud.moto.placa)!;
    moto.estado = 'ARRENDADA';

    const contrato: Contrato = {
      idContrato: Math.max(0, ...CONTRATOS_PRUEBA.map(c => c.idContrato)) + 1,
      fechaFirma: hoy,
      fechaInicio: solicitud.fechaInicio,
      fechaFin: solicitud.fechaFin,
      valorTotal: diasEntre(solicitud.fechaInicio, solicitud.fechaFin) * solicitud.moto.precioDia,
      deposito: datos.deposito,
      estado: 'VIGENTE',
      idSolicitud: solicitud.idSolicitud,
      documentoArrendatario: solicitud.documentoArrendatario,
      nombreArrendatario: solicitud.nombreArrendatario,
      documentoArrendador: moto.documentoArrendador,
      nombreArrendador: moto.nombreArrendador,
      moto: { placa: moto.placa, marca: moto.marca, modelo: moto.modelo }
    };
    CONTRATOS_PRUEBA.push(contrato);

    let idPago = Math.max(0, ...PAGOS_PRUEBA.map(p => p.idPago));
    planDePagos(contrato.fechaInicio, contrato.fechaFin, contrato.valorTotal).forEach(cuota =>
      PAGOS_PRUEBA.push({ idPago: ++idPago, idContrato: contrato.idContrato, estado: 'PENDIENTE', ...cuota }));

    return of(contrato).pipe(delay(300));
  }

  // TODO: return this.http.patch<Solicitud>(`${this.apiUrl}/${id}/rechazar`, {});
  rechazar(id: number): Observable<Solicitud> {
    const solicitud = this.buscarRecibida(id);
    if (!solicitud || solicitud.estado !== 'PENDIENTE') {
      return throwError(() => new Error('La solicitud ya no está pendiente.'));
    }
    solicitud.estado = 'RECHAZADA';
    solicitud.fechaRespuesta = hoyISO();
    return of(solicitud).pipe(delay(300));
  }

  private buscarRecibida(id: number): Solicitud | undefined {
    return SOLICITUDES_PRUEBA.find(s => s.idSolicitud === id && s.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA);
  }
}
