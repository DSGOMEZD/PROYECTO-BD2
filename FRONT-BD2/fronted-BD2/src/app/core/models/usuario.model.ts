import { Rol } from './rol.model';

// Representa a un usuario tal como lo devuelve la API.
// Ajusta los campos cuando tengas definido el back.
export interface Usuario {
  id: number;
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  rol: Rol;
}
