import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrdenesService } from '../services/ordenes.service';
import { ClienteService } from '../../clientes/services/cliente.service';

interface ClienteResponse {
  id: number;
  nombre: string;
  email?: string | null;
}

interface EquipoResponse {
  id: number;
  marca: string;
  modelo: string;
  tipo: string;
}

@Component({
  selector: 'app-create',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './create.component.html',
  styleUrls: ['./create.component.scss']
})
export class CreateComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private snackbar = inject(MatSnackBar);
  private ordenesService = inject(OrdenesService);
  private clienteService = inject(ClienteService);
  private destroyRef = inject(DestroyRef);

  form!: FormGroup;
  clientes: ClienteResponse[] = [];
  selectedCliente: ClienteResponse | null = null;
  equipos: EquipoResponse[] = [];
  tecnicos: ClienteResponse[] = [];
  showClienteDropdown = false;
  isLoading = false;
  isSubmitting = false;
  clienteSearchTerm = '';
  private searchSubject = new Subject<string>();

  get equipoIdControl() {
    return this.form.get('equipoId');
  }

  get fallaReportadaControl() {
    return this.form.get('fallaReportada');
  }

  ngOnInit() {
    this.form = this.fb.group({
      clienteId: [null, Validators.required],
      equipoId: [null, Validators.required],
      tecnicoId: [null],
      prioridad: ['NORMAL', Validators.required],
      presupuesto: [null],
      fallaReportada: ['', Validators.required]
    });

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => this.clienteService.listarClientes(q)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (results) => {
        this.clientes = results;
        this.showClienteDropdown = results.length > 0;
      },
      error: () => {
        this.snackbar.open('Error al buscar clientes', 'Cerrar', { duration: 3000 });
      }
    });
  }

  onClienteSearch(event: Event | string) {
    const value = typeof event === 'string' ? event : (event.target as HTMLInputElement).value;
    this.clienteSearchTerm = value;
    this.showClienteDropdown = value.length > 0;
    if (value.length > 0) {
      this.searchSubject.next(value);
    } else {
      this.clientes = [];
      this.showClienteDropdown = false;
    }
  }

  onClienteSelect(cliente: ClienteResponse) {
    this.selectedCliente = cliente;
    this.form.patchValue({ clienteId: cliente.id, equipoId: null });
    this.showClienteDropdown = false;
    this.clienteSearchTerm = cliente.nombre;
    // TODO: Load equipos if equipo service available
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackbar.open('Completá todos los campos requeridos', 'Cerrar', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;
    const formData = { ...this.form.value };

    this.ordenesService.crearOrden(formData)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (orden) => {
          this.snackbar.open('Orden creada exitosamente', 'Cerrar', { duration: 2000 });
          this.router.navigate(['/ordenes', orden.id]);
        },
        error: (err) => {
          this.isSubmitting = false;
          const message = err.error?.message || 'Error al crear la orden';
          this.snackbar.open(message, 'Cerrar', { duration: 3000 });
        }
      });
  }

  navigateBack() {
    this.router.navigate(['/ordenes']);
  }

  selectPrioridad(prioridad: 'BAJA' | 'NORMAL' | 'ALTA') {
    this.form.patchValue({ prioridad });
  }
}
