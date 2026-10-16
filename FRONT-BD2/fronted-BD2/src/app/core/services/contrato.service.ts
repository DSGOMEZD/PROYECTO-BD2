import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contrato } from '../models/contrato.model';
import { CONTRATOS_PRUEBA } from '../mocks/datos-prueba';

/** Servicio de contratos del arrendatario (datos simulados por ahora). */
@Injectable({ providedIn: 'root' })
export class ContratoService {

  private apiUrl = `${environment.apiUrl}/contratos`;

  // TODO: return this.http.get<Contrato[]>(`${this.apiUrl}/mios`);
  listarMios(): Observable<Contrato[]> {
    return of(CONTRATOS_PRUEBA).pipe(delay(300));
  }
}
