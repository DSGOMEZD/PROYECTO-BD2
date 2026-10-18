import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contrato } from '../models/contrato.model';
import {
  CONTRATOS_PRUEBA, DOCUMENTO_ARRENDADOR_PRUEBA, DOCUMENTO_ARRENDATARIO_PRUEBA, MOTOS_PRUEBA, PAGOS_PRUEBA
} from '../mocks/datos-prueba';

/** Servicio de contratos (datos simulados por ahora). */
@Injectable({ providedIn: 'root' })
export class ContratoService {

  private apiUrl = `${environment.apiUrl}/contratos`;

  // ===== Arrendatario =====

  // TODO: return this.http.get<Contrato[]>(`${this.apiUrl}/mios`);
  listarMios(): Observable<Contrato[]> {
    return of(CONTRATOS_PRUEBA.filter(c => c.documentoArrendatario === DOCUMENTO_ARRENDATARIO_PRUEBA)).pipe(delay(300));
  }

  // ===== Arrendador =====

  // TODO: return this.http.get<Contrato[]>(`${this.apiUrl}/arrendador`);
  listarComoArrendador(): Observable<Contrato[]> {
    return of(CONTRATOS_PRUEBA.filter(c => c.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA)).pipe(delay(300));
  }

  /**
   * Cancela un contrato vigente: el back debe liberar la moto (DISPONIBLE)
   * y cancelar los pagos que aún no se han hecho.
   */
  // TODO: return this.http.patch<Contrato>(`${this.apiUrl}/${id}/cancelar`, {});
  cancelar(id: number): Observable<Contrato> {
    const contrato = CONTRATOS_PRUEBA.find(c => c.idContrato === id && c.documentoArrendador === DOCUMENTO_ARRENDADOR_PRUEBA);
    if (!contrato || contrato.estado !== 'VIGENTE') {
      return throwError(() => new Error('Solo se pueden cancelar contratos vigentes.'));
    }

    // --- TEMPORAL: todo esto lo hará el back ---
    contrato.estado = 'CANCELADO';
    const moto = MOTOS_PRUEBA.find(m => m.placa === contrato.moto.placa);
    if (moto?.estado === 'ARRENDADA') moto.estado = 'DISPONIBLE';
    PAGOS_PRUEBA
      .filter(p => p.idContrato === id && (p.estado === 'PENDIENTE' || p.estado === 'VENCIDO'))
      .forEach(p => p.estado = 'CANCELADO');

    return of(contrato).pipe(delay(300));
  }
}
