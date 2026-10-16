import { Routes } from '@angular/router';
import { LoginComponent } from './pages/auth/login/login.component';
import { RegistroComponent } from './pages/auth/registro/registro.component';
import { ArrendatarioLayoutComponent } from './pages/arrendatario/layout/arrendatario-layout.component';
import { DashboardArrendatarioComponent } from './pages/arrendatario/dashboard/dashboard-arrendatario.component';
import { MotosDisponiblesComponent } from './pages/arrendatario/motos/motos-disponibles.component';
import { MisSolicitudesComponent } from './pages/arrendatario/solicitudes/mis-solicitudes.component';
import { MisContratosComponent } from './pages/arrendatario/contratos/mis-contratos.component';
import { MisPagosComponent } from './pages/arrendatario/pagos/mis-pagos.component';
import { DashboardArrendadorComponent } from './pages/arrendador/dashboard/dashboard-arrendador.component';
import { DashboardAdminComponent } from './pages/admin/dashboard/dashboard-admin.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Autenticación
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },

  // Arrendatario (layout con pestañas)
  {
    path: 'arrendatario',
    component: ArrendatarioLayoutComponent,
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: DashboardArrendatarioComponent },
      { path: 'motos', component: MotosDisponiblesComponent },
      { path: 'solicitudes', component: MisSolicitudesComponent },
      { path: 'contratos', component: MisContratosComponent },
      { path: 'pagos', component: MisPagosComponent }
    ]
  },

  // Dashboards de los otros roles
  { path: 'arrendador', component: DashboardArrendadorComponent },
  { path: 'admin', component: DashboardAdminComponent },

  // Cualquier ruta desconocida vuelve al login
  { path: '**', redirectTo: 'login' }
];
