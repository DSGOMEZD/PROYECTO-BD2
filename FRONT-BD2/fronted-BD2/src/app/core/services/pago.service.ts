import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pago, MetodoPago } from '../models/pago.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PagoService {
  private apiUrl = `${environment.apiUrl}/pagos`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  listarMios(): Observable<Pago[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.get<Pago[]>(`${this.apiUrl}/mios`, { params });
  }

  listarRecibidos(): Observable<Pago[]> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    const params = new HttpParams()
      .set('documentoArrendador', usuario.numeroDocumento);

    return this.http.get<Pago[]>(`${this.apiUrl}/recibidos`, { params });
  }

  registrar(idPago: number, metodoPago: MetodoPago): Observable<void> {
    const usuario = this.authService.obtenerUsuario();
    if (!usuario) return throwError(() => new Error('Debes iniciar sesión.'));

    const params = new HttpParams()
      .set('documentoArrendatario', usuario.numeroDocumento);

    return this.http.patch<void>(
      `${this.apiUrl}/${idPago}/pagar`,
      { metodoPago },
      { params }
    );
  }
}
