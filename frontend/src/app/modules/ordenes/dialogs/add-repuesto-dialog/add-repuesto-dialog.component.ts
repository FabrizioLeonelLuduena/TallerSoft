import { Component, OnInit, Inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { RepuestosService, Repuesto } from '../../ordenes/services/repuestos.service';
import { OrdenesService } from '../../ordenes/services/ordenes.service';
import { Subject, Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, startWith, map } from 'rxjs/operators';

@Component({
  selector: 'app-add-repuesto-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatCardModule
  ],
  templateUrl: './add-repuesto-dialog.component.html',
  styleUrls: ['./add-repuesto-dialog.component.scss']
})
export class AddRepuestoDialogComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loading = false;
  selectedRepuesto: Repuesto | null = null;
  filteredRepuestos$!: Observable<Repuesto[]>;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private repuestosService: RepuestosService,
    private ordenesService: OrdenesService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<AddRepuestoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { ordenId: number }
  ) {}

  ngOnInit(): void {
    this.inicializarForm();
    this.setupAutocomplete();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private inicializarForm(): void {
    this.form = this.fb.group({
      repuesto: ['', Validators.required],
      cantidad: ['', [Validators.required, Validators.min(1)]]
    });
  }

  private setupAutocomplete(): void {
    this.filteredRepuestos$ = this.form.get('repuesto')!.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(value => {
        if (typeof value === 'string' && value.length > 0) {
          return this.repuestosService.listarRepuestos().pipe(
            map(repuestos => 
              repuestos.filter(r => 
                r.nombre.toLowerCase().includes(value.toLowerCase())
              )
            )
          );
        }
        return this.repuestosService.listarRepuestos();
      }),
      takeUntil(this.destroy$)
    );
  }

  onRepuestoSelected(repuesto: Repuesto): void {
    this.selectedRepuesto = repuesto;
    this.form.patchValue({ repuesto: repuesto.nombre });
  }

  onSubmit(): void {
    if (!this.form.valid || !this.selectedRepuesto) {
      this.snackBar.open('Por favor, selecciona un repuesto y especifica cantidad', 'Cerrar', { duration: 3000 });
      return;
    }

    const cantidad = this.form.get('cantidad')?.value;

    if (cantidad > this.selectedRepuesto.stockActual) {
      this.snackBar.open('Stock insuficiente disponible', 'Cerrar', { duration: 3000 });
      return;
    }

    this.loading = true;
    const request = {
      repuestoId: this.selectedRepuesto.id,
      cantidad: cantidad
    };

    this.ordenesService.agregarRepuesto(this.data.ordenId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('Repuesto agregado exitosamente', 'Cerrar', { duration: 2000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          if (error.status === 409) {
            this.snackBar.open('Stock insuficiente', 'Cerrar', { duration: 3000 });
          } else {
            this.snackBar.open('Error al agregar repuesto', 'Cerrar', { duration: 3000 });
          }
          this.loading = false;
        }
      });
  }

  getDisplayName(repuesto: any): string {
    return repuesto && typeof repuesto === 'object' ? repuesto.nombre : repuesto;
  }
}
