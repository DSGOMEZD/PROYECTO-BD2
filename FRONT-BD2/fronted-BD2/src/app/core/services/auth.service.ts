import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegistroRequest } from '../models/auth.model';
import { Usuario } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly usuarioKey = 'usuario';

  constructor(private http: HttpClient) {}

  login(datos: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, datos).pipe(
      tap(usuario => localStorage.setItem(this.usuarioKey, JSON.stringify(usuario)))
    );
  }

  registrar(datos: RegistroRequest): Observable<void> {
    const telefono = Number(datos.telefono);
    const [primerNombre, segundoNombre] = this.separarPrimeroYSegundo(datos.nombre);
    const [primerApellido, segundoApellido] = this.separarPrimeroYSegundo(datos.apellido);

    const body = {
      tipoDocumento: datos.tipoDocumento,
      numeroDocumento: datos.numeroDocumento,
      primerNombre,
      segundoNombre,
      primerApellido,
      segundoApellido,
      correo: datos.correo.trim(),
      telefono,
      contrasena: datos.contrasena,
      rol: datos.rol
    };

    return this.http.post<void>(`${this.apiUrl}/registro`, body);
  }

  private separarPrimeroYSegundo(texto: string): [string, string | null] {
    const limpio = (texto ?? '').trim().replace(/\s+/g, ' ');
    if (!limpio) {
      return ['', null];
    }
    const partes = limpio.split(' ');
    const primero = partes[0];
    const segundo = partes.length > 1 ? partes.slice(1).join(' ') : null;
    return [primero, segundo];
  }

  obtenerUsuario(): Usuario | null {
    const texto = localStorage.getItem(this.usuarioKey);
    return texto ? JSON.parse(texto) as Usuario : null;
  }

  cerrarSesion(): void {
    localStorage.removeItem(this.usuarioKey);
  }
}
