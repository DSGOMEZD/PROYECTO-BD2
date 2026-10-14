import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

// Barra superior que reutilizan los 3 dashboards
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar.component.html'
})
export class NavbarComponent {
  @Input() rol = '';
}
