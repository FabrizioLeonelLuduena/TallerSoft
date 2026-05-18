import { Component, OnInit, Inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RepuestosService, Repuesto } from '../../ordenes/services/repuestos.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-repuesto-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './repuesto-dialog.component.html',
  styleUrls: ['./repuesto-dialog.component.scss']
})
export class RepuestoDialogComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  loading = false;
  isEditMode = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private repuestosService: RepuestosService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<RepuestoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data?: Repuesto
  ) {}

  ngOnInit(): void {
    this.inicializarForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private inicializarForm(): void {
    this.isEditMode = !!this.data;
    
    this.form = this.fb.group({
      nombre: [this.data?.nombre || '', [Validators.required, Validators.minLength(3)]],
      categoria: [this.data?.categoria || '', Validators.required],
      precio: [this.data?.precio || '', [Validators.required, Validators.min(0.01)]],
      stockActual: [this.data?.stockActual || 0, [Validators.required, Validators.min(0)]],
      stockMinimo: [this.data?.stockMinimo || 5, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (!this.form.valid) {
      this.snackBar.open('Por favor, completa todos los campos correctamente', 'Cerrar', { duration: 3000 });
      return;
    }

    this.loading = true;
    const formValue = this.form.value;

    if (this.isEditMode) {
      this.repuestosService.editarRepuesto(this.data!.id, formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.snackBar.open('Repuesto actualizado exitosamente', 'Cerrar', { duration: 2000 });
            this.dialogRef.close(true);
          },
          error: () => {
            this.snackBar.open('Error al actualizar repuesto', 'Cerrar', { duration: 3000 });
            this.loading = false;
          }
        });
    } else {
      this.repuestosService.crearRepuesto(formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.snackBar.open('Repuesto creado exitosamente', 'Cerrar', { duration: 2000 });
            this.dialogRef.close(true);
          },
          error: () => {
            this.snackBar.open('Error al crear repuesto', 'Cerrar', { duration: 3000 });
            this.loading = false;
          }
        });
    }
  }
}
