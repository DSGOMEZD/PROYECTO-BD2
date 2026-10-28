import {Injectable} from '@angular/core';
import {Observable, delay, of, throwError} from 'rxjs';
import {environment} from '../../../environments/environment';
import {Contrato} from '../models/contrato.model';
import {
  CONTRATOS_PRUEBA, DOCUMENTO_ARRENDADOR_PRUEBA, DOCUMENTO_ARRENDATARIO_PRUEBA, MOTOS_PRUEBA, PAGOS_PRUEBA
} from '../mocks/datos-prueba';

import {HttpClient, HttpParams} from '@angular/common/http';
import {AuthService} from './auth.service';

/** Servicio de contratos (datos simulados por ahora). */
@Injectable({providedIn: 'root'})
export class ContratoService {

  private apiUrl = `${environment.apiUrl}/contratos`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
  }

  // ===== Arrendatario =====

  // TODO: return this.http.get<Contrato[]>(`${this.apiUrl}/mios`);
  listarMios(): Observable<Contrato[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.get<Contrato[]>(`${this.apiUrl}/mios`, {params});
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Contrato[]>(`${this.apiUrl}/arrendador`);
  listarComoArrendador(): Observable<Contrato[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.get<Contrato[]>(`${this.apiUrl}/arrendador`, {params});
  }

  /**
   * Cancela un contrato vigente: el back debe liberar la moto (DISPONIBLE)
   * y cancelar los pagos que aún no se han hecho.
   */
  // TODO: return this.http.patch<Contrato>(`${this.apiUrl}/${id}/cancelar`, {});
  cancelar(id: number): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documento', usuario.numeroDocumento);

    return this.http.patch<void>(
      `${this.apiUrl}/${id}/cancelar`,
      {},
      {params}
    );
  }

  finalizar(idContrato: number, kilometrajeFinal: number): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.patch<void>(
      `${this.apiUrl}/${idContrato}/finalizar`,
      {kilometrajeFinal},
      {params}
    );
  }
}
