import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pago } from '../models/pago.model';
import { PAGOS_PRUEBA } from '../mocks/datos-prueba';

/** Servicio de pagos de los contratos del arrendatario (datos simulados por ahora). */
@Injectable({ providedIn: 'root' })
export class PagoService {

  private apiUrl = `${environment.apiUrl}/pagos`;

  // TODO: return this.http.get<Pago[]>(`${this.apiUrl}/mios`);
  listarMios(): Observable<Pago[]> {
    return of(PAGOS_PRUEBA).pipe(delay(300));
  }
}
