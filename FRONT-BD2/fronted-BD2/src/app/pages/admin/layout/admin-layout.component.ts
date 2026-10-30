import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

// Estructura común de todas las páginas del administrador: navbar + pestañas
@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [NavbarComponent, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html'
})
export class AdminLayoutComponent {

  secciones = [
    { ruta: 'inicio', texto: 'Inicio' },
    { ruta: 'logs', texto: 'Logs de actividad' }
  ];
}
