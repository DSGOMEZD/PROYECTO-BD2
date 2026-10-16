import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

// Estructura común de todas las páginas del arrendatario: navbar + pestañas
@Component({
  selector: 'app-arrendatario-layout',
  standalone: true,
  imports: [NavbarComponent, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './arrendatario-layout.component.html'
})
export class ArrendatarioLayoutComponent {

  secciones = [
    { ruta: 'inicio', texto: 'Inicio' },
    { ruta: 'motos', texto: 'Motos disponibles' },
    { ruta: 'solicitudes', texto: 'Mis solicitudes' },
    { ruta: 'contratos', texto: 'Mis contratos' },
    { ruta: 'pagos', texto: 'Mis pagos' }
  ];
}
