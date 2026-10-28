import {Injectable} from '@angular/core';
import {Observable, delay, of, throwError} from 'rxjs';
import {environment} from '../../../environments/environment';
import {AprobarSolicitudRequest, NuevaSolicitudRequest, Solicitud} from '../models/solicitud.model';
import {Contrato} from '../models/contrato.model';
import {
  CONTRATOS_PRUEBA, DOCUMENTO_ARRENDADOR_PRUEBA, DOCUMENTO_ARRENDATARIO_PRUEBA, MOTOS_PRUEBA,
  NOMBRE_ARRENDATARIO_PRUEBA, PAGOS_PRUEBA, SOLICITUDES_PRUEBA
} from '../mocks/datos-prueba';
import {diasEntre, hoyISO, periodosSeCruzan} from '../utils/fechas';
import {planDePagos} from '../utils/pagos'
import {HttpClient, HttpParams} from '@angular/common/http';
import {AuthService} from './auth.service';

/**
 * Servicio de solicitudes de arrendamiento.
 * Por ahora trabaja sobre los datos simulados en memoria.
 */
@Injectable({providedIn: 'root'})
export class SolicitudService {

  private apiUrl = `${environment.apiUrl}/solicitudes`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
  }

  // ===== Arrendatario =====

  // TODO: return this.http.get<Solicitud[]>(`${this.apiUrl}/mias`);
  listarMias(): Observable<Solicitud[]> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.get<Solicitud[]>(`${this.apiUrl}/mias`, {params});
  }

  // TODO: return this.http.post<Solicitud>(this.apiUrl, datos);
  crear(datos: NuevaSolicitudRequest): Observable<Solicitud> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.post<Solicitud>(this.apiUrl, datos, {params});
  }

  // TODO: return this.http.patch<Solicitud>(`${this.apiUrl}/${id}/cancelar`, {});
  cancelar(id: number): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.patch<void>(
      `${this.apiUrl}/${id}/cancelar`,
      {},
      {params}
    );
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Solicitud[]>(`${this.apiUrl}/recibidas`);
  listarRecibidas(): Observable<Solicitud[]> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.get<Solicitud[]>(`${this.apiUrl}/recibidas`, {params});
  }

  /**
   * Aprueba la solicitud: el back debe crear el contrato, generar los pagos,
   * marcar la moto como ARRENDADA y rechazar las otras solicitudes pendientes
   * de la misma moto que se crucen en fechas.
   */
  // TODO: return this.http.post<Contrato>(`${this.apiUrl}/${id}/aprobar`, datos);
  aprobar(id: number, datos: AprobarSolicitudRequest): Observable<Contrato> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.post<Contrato>(
      `${this.apiUrl}/${id}/aprobar`,
      datos,
      {params}
    );
  }

  // TODO: return this.http.patch<Solicitud>(`${this.apiUrl}/${id}/rechazar`, {});
  rechazar(id: number): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.patch<void>(
      `${this.apiUrl}/${id}/rechazar`,
      {},
      {params}
    );
  }

  private buscarRecibida(id: number): Solicitud | undefined {
    return SOLICITUDES_PRUEBA.find(s => s.idSolicitud === id && s.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA);
  }
}
