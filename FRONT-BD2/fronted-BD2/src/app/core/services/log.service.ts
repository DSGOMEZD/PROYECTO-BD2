import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiltroLogs, LogEvento } from '../models/log.model';
import { Rol } from '../models/rol.model';
import { AuthService } from './auth.service';

/**
 * Logs de actividad (pagos, motos, solicitudes y contratos) guardados en MongoDB.
 * Solo el administrador puede consultarlos: el back valida el rol con documentoAdmin.
 */
@Injectable({ providedIn: 'root' })
export class LogService {
  private apiUrl = `${environment.apiUrl}/admin/logs`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  listar(filtro: FiltroLogs): Observable<LogEvento[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));
    if (usuario.rol !== Rol.ADMIN) return throwError(() => new Error('Solo el administrador puede ver los logs.'));

    let params = new HttpParams()
      .set('documentoAdmin', usuario.numeroDocumento)
      .set('desde', filtro.desde)
      .set('hasta', filtro.hasta);
    if (filtro.modulo) params = params.set('modulo', filtro.modulo);

    return this.http.get<LogEvento[]>(this.apiUrl, { params });
  }
}
