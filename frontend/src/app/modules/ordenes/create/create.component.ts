import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrdenesService } from '../services/ordenes.service';
import { ClienteRepository } from '../../clientes/repositories/cliente.repository';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './create.component.html',
  styleUrls: ['./create.component.scss']
})
export class CreateComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loading = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private ordenesService: OrdenesService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.inicializarForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private inicializarForm(): void {
    this.form = this.fb.group({
      clienteId: ['', Validators.required],
      equipoId: ['', Validators.required],
      tecnicoId: [''],
      fallaReportada: ['', [Validators.required, Validators.minLength(10)]],
      prioridad: ['NORMAL', Validators.required]
    });
  }

  onSubmit(): void {
    if (!this.form.valid) {
      this.snackBar.open('Por favor, completa todos los campos requeridos', 'Cerrar', { duration: 3000 });
      return;
    }

    this.loading = true;
    this.ordenesService.crearOrden(this.form.value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (orden) => {
          this.snackBar.open('Orden creada exitosamente', 'Cerrar', { duration: 2000 });
          this.router.navigate(['/ordenes', orden.id]);
        },
        error: () => {
          this.snackBar.open('Error al crear la orden', 'Cerrar', { duration: 3000 });
          this.loading = false;
        }
      });
  }
}
