import { Component } from '@angular/core';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-dashboard-admin',
  standalone: true,
  imports: [NavbarComponent],
  templateUrl: './dashboard-admin.component.html'
})
export class DashboardAdminComponent {}
