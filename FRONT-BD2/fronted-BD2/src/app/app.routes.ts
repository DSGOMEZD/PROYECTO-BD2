import { Routes } from '@angular/router';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegistroComponent } from './pages/auth/registro/registro.component';
import { DashboardArrendatarioComponent } from './pages/arrendatario/dashboard/dashboard-arrendatario.component';
import { DashboardArrendadorComponent } from './pages/arrendador/dashboard/dashboard-arrendador.component';
import { DashboardAdminComponent } from './pages/admin/dashboard/dashboard-admin.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Autenticación
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },

  // Dashboards por rol
  { path: 'arrendatario', component: DashboardArrendatarioComponent },
  { path: 'arrendador', component: DashboardArrendadorComponent },
  { path: 'admin', component: DashboardAdminComponent },

  // Cualquier ruta desconocida vuelve al login
  { path: '**', redirectTo: 'login' }
];
