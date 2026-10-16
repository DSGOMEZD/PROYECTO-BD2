import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Motocicleta } from '../models/motocicleta.model';
import { MOTOS_PRUEBA } from '../mocks/datos-prueba';

/**
 * Servicio de motocicletas.
 * Por ahora devuelve datos simulados; cuando exista el back,
 * inyecta HttpClient y reemplaza cada método por la llamada del TODO.
 */
@Injectable({ providedIn: 'root' })
export class MotoService {

  private apiUrl = `${environment.apiUrl}/motos`;

  // TODO: return this.http.get<Motocicleta[]>(`${this.apiUrl}?estado=DISPONIBLE`);
  listarDisponibles(): Observable<Motocicleta[]> {
    return of(MOTOS_PRUEBA.filter(m => m.estado === 'DISPONIBLE')).pipe(delay(300));
  }
}
