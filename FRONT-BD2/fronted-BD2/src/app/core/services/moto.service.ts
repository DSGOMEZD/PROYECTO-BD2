import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoMoto, Motocicleta, MotoRequest } from '../models/motocicleta.model';
import { Seguro } from '../models/seguro.model';
import { DOCUMENTO_ARRENDADOR_PRUEBA, MOTOS_PRUEBA, NOMBRE_ARRENDADOR_PRUEBA } from '../mocks/datos-prueba';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
/**
 * Servicio de motocicletas.
 * Por ahora trabaja sobre los datos simulados en memoria; cuando exista el back,
 * inyecta HttpClient y reemplaza cada método por la llamada del TODO.
 */
@Injectable({ providedIn: 'root' })
export class MotoService {

  private apiUrl = `${environment.apiUrl}/motos`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}
  // ===== Arrendatario =====

  // TODO: return this.http.get<Motocicleta[]>(`${this.apiUrl}?estado=DISPONIBLE`);
  listarDisponibles(fechaInicio: string, fechaFin: string): Observable<Motocicleta[]> {
    return this.http.get<Motocicleta[]>(`${this.apiUrl}/disponibles`, {
      params: { fechaInicio, fechaFin }
    });
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Motocicleta[]>(`${this.apiUrl}/mias`);
  listarMias(): Observable<Motocicleta[]> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    return this.http.get<Motocicleta[]>(`${this.apiUrl}/mias`, {
      params: {
        documentoArrendador: usuario.numeroDocumento
      }
    });
  }

  // TODO: return this.http.post<Motocicleta>(this.apiUrl, datos);
  publicar(datos: MotoRequest): Observable<number> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    return this.http.post<number>(this.apiUrl, datos, {
      params: {
        documentoArrendador: usuario.numeroDocumento
      }
    });
  }

  // TODO: return this.http.put<Motocicleta>(`${this.apiUrl}/${placa}`, datos);
  actualizar(placa: string, datos: MotoRequest): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    return this.http.put<void>(
      `${this.apiUrl}/${encodeURIComponent(placa)}`,
      datos,
      {
        params: {
          documentoArrendador: usuario.numeroDocumento
        }
      }
    );
  }

  // TODO: return this.http.patch<Motocicleta>(`${this.apiUrl}/${placa}/estado`, { estado });
  cambiarEstado(placa: string, estado: EstadoMoto): Observable<void> {
    const usuario = this.authService.obtenerUsuario();

    if (!usuario) {
      return throwError(() => new Error('Debes iniciar sesión.'));
    }

    return this.http.patch<void>(
      `${this.apiUrl}/${encodeURIComponent(placa)}/estado`,
      { estado },
      {
        params: {
          documentoArrendador: usuario.numeroDocumento
        }
      }
    );
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
