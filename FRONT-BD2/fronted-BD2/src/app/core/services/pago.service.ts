import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contrato } from '../models/contrato.model';
import { Pago } from '../models/pago.model';
import {
  CONTRATOS_PRUEBA, DOCUMENTO_ARRENDADOR_PRUEBA, DOCUMENTO_ARRENDATARIO_PRUEBA, PAGOS_PRUEBA
} from '../mocks/datos-prueba';

/** Servicio de pagos de los contratos (datos simulados por ahora). */
@Injectable({ providedIn: 'root' })
export class PagoService {

  private apiUrl = `${environment.apiUrl}/pagos`;

  // ===== Arrendatario =====

  // TODO: return this.http.get<Pago[]>(`${this.apiUrl}/mios`);
  listarMios(): Observable<Pago[]> {
    return of(this.pagosDe(c => c.documentoArrendatario === DOCUMENTO_ARRENDATARIO_PRUEBA)).pipe(delay(300));
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Pago[]>(`${this.apiUrl}/recibidos`);
  listarRecibidos(): Observable<Pago[]> {
    return of(this.pagosDe(c => c.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA)).pipe(delay(300));
  }

  // TEMPORAL: en el back esto es un JOIN entre PAGO y CONTRATO
  private pagosDe(filtro: (c: Contrato) => boolean): Pago[] {
    const ids = CONTRATOS_PRUEBA.filter(filtro).map(c => c.idContrato);
    return PAGOS_PRUEBA.filter(p => ids.includes(p.idContrato));
  }
}
