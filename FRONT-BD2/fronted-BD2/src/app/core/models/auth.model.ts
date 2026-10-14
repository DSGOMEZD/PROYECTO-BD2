import { Rol } from './rol.model';
import { Usuario } from './usuario.model';

// Lo que el front ENVÍA al back para iniciar sesión
export interface LoginRequest {
  correo: string;
  contrasena: string;
}

// Lo que el back RESPONDE al iniciar sesión
export interface LoginResponse {
  token: string;
  usuario: Usuario;
}

// Lo que el front ENVÍA al back para registrar un usuario
export interface RegistroRequest {
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  contrasena: string;
  rol: Rol;
}
