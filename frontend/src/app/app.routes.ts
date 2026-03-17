import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/graph/graph-page.component').then(m => m.GraphPageComponent),
  },
];
