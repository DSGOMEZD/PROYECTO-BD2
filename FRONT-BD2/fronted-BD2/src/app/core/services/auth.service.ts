import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Servicio de autenticación: aquí irán las llamadas HTTP al back
 * para login y registro (usando HttpClient).
 *
 * Por ahora está vacío a propósito; los componentes todavía no lo usan.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private apiUrl = `${environment.apiUrl}/auth`;

  // TODO: login(datos: LoginRequest): Observable<LoginResponse>
  // TODO: registrar(datos: RegistroRequest): Observable<Usuario>
  // TODO: logout()
}
