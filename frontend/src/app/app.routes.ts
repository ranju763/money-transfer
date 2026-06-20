import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransactionListComponent } from './components/transaction-list/transaction-list.component';
import { TransferComponent } from './components/transfer/transfer.component';
import { RewardsComponent } from './components/rewards/rewards.component';
import { LoginComponent } from './components/login/login.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
    { path: 'transactions', component: TransactionListComponent, canActivate: [authGuard] },
    { path: 'transfer', component: TransferComponent, canActivate: [authGuard] },
    { path: 'rewards', component: RewardsComponent, canActivate: [authGuard] },
    { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
    { path: '**', redirectTo: '/login' }
];
