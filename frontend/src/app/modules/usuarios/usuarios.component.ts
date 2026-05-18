import { Component } from '@angular/core';
import { UsuariosListComponent } from './list/list.component';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [UsuariosListComponent],
  template: `
    <app-usuarios-list></app-usuarios-list>
  `
})
export class UsuariosComponent {}
