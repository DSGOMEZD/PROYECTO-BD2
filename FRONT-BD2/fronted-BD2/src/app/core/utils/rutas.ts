import { Rol } from '../models/rol.model';

// Devuelve la ruta del dashboard según el rol del usuario
export function rutaDashboard(rol: Rol): string {
  switch (rol) {
    case Rol.ARRENDADOR: return '/arrendador';
    case Rol.ADMIN: return '/admin';
    default: return '/arrendatario';
  }
}
