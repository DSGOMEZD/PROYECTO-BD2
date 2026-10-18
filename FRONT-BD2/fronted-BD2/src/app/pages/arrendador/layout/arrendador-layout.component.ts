import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

// Estructura común de todas las páginas del arrendador: navbar + pestañas
@Component({
  selector: 'app-arrendador-layout',
  standalone: true,
  imports: [NavbarComponent, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './arrendador-layout.component.html'
})
export class ArrendadorLayoutComponent {

  secciones = [
    { ruta: 'inicio', texto: 'Inicio' },
    { ruta: 'motos', texto: 'Mis motos' },
    { ruta: 'solicitudes', texto: 'Solicitudes recibidas' },
    { ruta: 'contratos', texto: 'Contratos' },
    { ruta: 'pagos', texto: 'Pagos' }
  ];
}
