import { Rol } from './rol.model';

export type TipoDocumento = 'CC' | 'CE' | 'TI' | 'PAS';

// Tabla USUARIO (sin la contraseña, que nunca viaja de vuelta al front)
export interface Usuario {
  numeroDocumento: number;
  tipoDocumento: TipoDocumento;
  nombre: string;
  apellido: string;
  correo: string;
  telefono: number | null;
  estado?: string;
  rol: Rol;
}
