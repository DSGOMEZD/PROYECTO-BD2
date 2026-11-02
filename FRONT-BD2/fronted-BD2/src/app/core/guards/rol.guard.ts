import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/rol.model';
import { AuthService } from '../services/auth.service';
import { rutaDashboard } from '../utils/rutas';

/**
 * Deja entrar a la sección solo al rol indicado.
 * Sin sesión -> login. Con otro rol -> su propio dashboard
 * (así solo el ADMIN llega a /admin y a los logs).
 */
export function rolGuard(rol: Rol): CanActivateFn {
  return () => {
    const router = inject(Router);
    const usuario = inject(AuthService).obtenerUsuario();

    if (!usuario) return router.createUrlTree(['/login']);
    if (usuario.rol !== rol) return router.createUrlTree([rutaDashboard(usuario.rol)]);
    return true;
  };
}
