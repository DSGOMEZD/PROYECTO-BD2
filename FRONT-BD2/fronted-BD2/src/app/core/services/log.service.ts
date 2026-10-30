import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiltroLogs, LogEvento } from '../models/log.model';
import { LOGS_PRUEBA } from '../mocks/logs-prueba';
import { AuthService } from './auth.service';

/**
 * Logs de actividad para el administrador (pagos, motos, solicitudes y contratos).
 * Mientras no exista el endpoint en el back se simula la
 * respuesta con LOGS_PRUEBA; para conectarlo, reemplaza el cuerpo por el TODO.
 */
@Injectable({ providedIn: 'root' })
export class LogService {
  private apiUrl = `${environment.apiUrl}/admin/logs`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // TODO: return this.http.get<LogEvento[]>(this.apiUrl, { params });
  listar(filtro: FiltroLogs): Observable<LogEvento[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    let params = new HttpParams()
      .set('documentoAdmin', usuario.numeroDocumento)
      .set('desde', filtro.desde)
      .set('hasta', filtro.hasta);
    if (filtro.modulo) params = params.set('modulo', filtro.modulo);

    // Simulación de lo que hará el back: filtra por rango de fechas (ambos días incluidos)
    const logs = LOGS_PRUEBA
      .filter(l => l.fecha.slice(0, 10) >= filtro.desde && l.fecha.slice(0, 10) <= filtro.hasta)
      .filter(l => !filtro.modulo || l.modulo === filtro.modulo)
      .sort((a, b) => b.fecha.localeCompare(a.fecha));

    return of(logs).pipe(delay(300));
  }
}
