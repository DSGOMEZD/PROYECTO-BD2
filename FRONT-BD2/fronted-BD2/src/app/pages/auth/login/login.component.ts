import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LoginRequest } from '../../../core/models/auth.model';
import { Rol } from '../../../core/models/rol.model';
import { rutaDashboard } from '../../../core/utils/rutas';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  datos: LoginRequest = { correo: '', contrasena: '' };

  // TEMPORAL: mientras no hay back, se elige a mano a qué dashboard entrar.
  // Cuando exista el AuthService, el rol vendrá en la respuesta del login.
  rolTemporal: Rol = Rol.ARRENDATARIO;
  roles = Object.values(Rol);

  constructor(private router: Router) {}

  iniciarSesion() {
    // TODO: llamar a authService.login(this.datos) y usar el rol que responda el back
    this.router.navigate([rutaDashboard(this.rolTemporal)]);
  }
}
