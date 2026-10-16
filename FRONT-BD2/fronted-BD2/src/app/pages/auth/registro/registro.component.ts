import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RegistroRequest } from '../../../core/models/auth.model';
import { Rol } from '../../../core/models/rol.model';
import { TipoDocumento } from '../../../core/models/usuario.model';
import { rutaDashboard } from '../../../core/utils/rutas';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.component.html'
})
export class RegistroComponent {

  Rol = Rol; // para poder usar el enum en el HTML

  tiposDocumento: { valor: TipoDocumento; texto: string }[] = [
    { valor: 'CC', texto: 'Cédula de ciudadanía' },
    { valor: 'CE', texto: 'Cédula de extranjería' },
    { valor: 'TI', texto: 'Tarjeta de identidad' },
    { valor: 'PAS', texto: 'Pasaporte' }
  ];

  datos: RegistroRequest = {
    tipoDocumento: 'CC',
    numeroDocumento: null,
    nombre: '',
    apellido: '',
    correo: '',
    telefono: '',
    contrasena: '',
    rol: Rol.ARRENDATARIO
  };

  constructor(private router: Router) {}

  registrar() {
    // TODO: llamar a authService.registrar(this.datos) antes de navegar
    this.router.navigate([rutaDashboard(this.datos.rol)]);
  }
}
