# TallerSoft — Prompt Fase 2: Módulo Usuarios (UI + Integración Backend)

You are an expert UI/UX designer and senior Angular developer working on TallerSoft. The shell layout, design system, login screen, and Clientes module (Phase 1) are already fully implemented and working.

Your task is to build **Phase 2: the complete Usuarios module** — 2 screens with full backend integration, done in a single pass. Unlike Phase 1, you will build UI and wire the API at the same time — do not separate them.

---

## MANDATORY — Read these files FIRST, in this exact order, before writing a single line of code:

1. `frontend/src/styles.scss` — all CSS custom properties. Zero hardcoded hex values allowed.
2. `frontend/src/app/modules/auth/login/login.component.html` — reference for dark input styling.
3. `frontend/src/app/modules/auth/login/login.component.scss` — reference for SCSS patterns.
4. `frontend/src/app/modules/clientes/list/list.component.html` — reference for card/table patterns already established.
5. `frontend/src/app/modules/clientes/list/list.component.scss` — reference for skeleton and button patterns.
6. `frontend/src/app/modules/clientes/list/list.component.ts` — reference for the service/subscription pattern used in Phase 1.
7. `frontend/src/app/core/auth/auth.service.ts` — understand getCurrentRole() and getCurrentUser().
8. `frontend/src/environments/environment.ts` — get the base apiUrl.
9. `frontend/src/app/app.routes.ts` — verify existing routes before adding new ones.

Do NOT proceed until you have read all 9 files.

---

## Context — What already exists

- Shell layout (sidebar + header) is complete
- Design system CSS variables are defined in `styles.scss`
- JWT interceptor automatically attaches Bearer token to every request
- AuthGuard and RoleGuard are implemented
- The sidebar already has a "Usuarios" link — verify where it navigates to and match that path
- Phase 1 (Clientes) established the patterns: skeleton loaders, dark inputs, estado chips, role guards, snackbars

---

## Design System — Rules that apply to every file in this phase

- All colors → `var(--color-*)` from `styles.scss`. Zero hardcoded hex values.
- All inputs → dark background `var(--color-surface-2)`, border `var(--color-border)`, focus `var(--color-accent)` — same as login
- All cards/panels → `var(--color-surface)` background, `var(--radius-card)`, `var(--shadow-card)`
- Hover on rows → `var(--color-surface-2)` background, `transition: var(--transition)`
- Loading → skeleton pulse, never mat-spinner as primary state
- Buttons → use the exact `.btn-primary`, `.btn-ghost`, `.btn-outlined` patterns from Phase 1
- Toast → MatSnackBar, bottom-right, 3s, `'Cerrar'` action
- This module is **ADMIN only** — guard every route and every button

---

## Backend API reference

**Base URL:** `environment.apiUrl`
**Auth:** handled by JwtInterceptor automatically.

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/register` | Create new user |
| `POST` | `/auth/login` | Used only to verify credentials — NOT used here |

> **Important:** The backend does not expose a `GET /api/usuarios` endpoint yet.
> The list component must fetch users from a source that IS available.
> Check if `GET /api/usuarios` exists in the backend by looking at the controllers already built.
> If it does not exist, the list must work with **locally managed state** (users added in the session are shown immediately, persisted in memory). Add a comment in the code: `// TODO: replace with GET /api/usuarios when endpoint is available`.
> Do NOT fabricate an API call to an endpoint that doesn't exist.

### Request / Response shapes

```typescript
// POST /auth/register — request body
interface UsuarioRequest {
  nombre: string;       // required, min 1 char
  email: string;        // required, valid email format
  password: string;     // required, min 8 chars
  rol: 'ADMIN' | 'TECNICO' | 'RECEPCION';  // required
}

// POST /auth/register — response
interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'TECNICO' | 'RECEPCION';
  activo: boolean;
  createdAt: string;  // ISO datetime
}

// Error response
interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
```

---

## Module structure to create

```
frontend/src/app/modules/usuarios/
├── list/
│   ├── list.component.ts
│   ├── list.component.html
│   └── list.component.scss
├── dialogs/
│   └── create-user-dialog/
│       ├── create-user-dialog.component.ts
│       ├── create-user-dialog.component.html
│       └── create-user-dialog.component.scss
├── services/
│   └── usuario.service.ts
└── usuarios.routes.ts
```

---

## SCREEN 1 — `/usuarios` — User List

### Layout

```
┌──────────────────────────────────────────────────────────────┐
│  Usuarios                          [+ Nuevo Usuario]         │  ← page header
│  Gestioná los usuarios del sistema                           │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Usuario          │ Email            │ Rol    │ Estado │  │  ← table header
│  ├────────────────────────────────────────────────────────┤  │
│  │  ● Admin          │ admin@...        │ ADMIN  │ ● Act. │  │
│  │  ● Juan Técnico   │ juan@...         │ TÉC.   │ ● Act. │  │
│  │  ● Ana Recep.     │ ana@...          │ RECEP. │ ● Act. │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Page header
- Title "Usuarios" — 1.4rem, font-weight 700, `var(--color-text-primary)`
- Subtitle "Gestioná los usuarios del sistema" — 0.875rem, `var(--color-text-secondary)`
- **"+ Nuevo Usuario" button** — top-right, `.btn-primary` style, opens `CreateUserDialogComponent` via MatDialog
- Only render this entire page if `currentRole === 'ADMIN'`. If not ADMIN, show a centered "Acceso denegado" state with a lock icon and navigate to `/dashboard` after 2 seconds.

### Table

Wrap the table in a card: `var(--color-surface)` background, `var(--radius-card)`, `var(--shadow-card)`, padding 0 (table fills the card edge to edge).

**Table header row:**
- Background `var(--color-surface)`, border-bottom `1px solid var(--color-border)`
- Text: `var(--color-text-muted)`, 0.75rem, font-weight 500, uppercase, letter-spacing 0.05em
- Padding: 12px 20px per cell

**Table data rows:**
- Background transparent, hover background `var(--color-surface-2)`, `transition: var(--transition)`
- Padding: 14px 20px per cell
- Border-bottom: `1px solid var(--color-border)` (last row no border)
- Cursor: default (rows are not clickable — no detail screen for users)

**Column: Usuario**
- Avatar circle 36px: background `var(--color-accent)` with 20% opacity (`rgba(249,115,22,0.15)`), color `var(--color-accent)`, initials (first letter of nombre), font-weight 600, font-size 0.85rem
- Next to avatar: nombre in `var(--color-text-primary)`, 0.9rem, font-weight 500
- Below nombre: createdAt formatted as "Miembro desde DD/MM/YYYY" in `var(--color-text-muted)`, 0.75rem

**Column: Email**
- `var(--color-text-secondary)`, 0.875rem, font-family monospace (so emails align cleanly)

**Column: Rol** — badge chip per role:
```
ADMIN     → background rgba(239,68,68,0.15)   color var(--color-danger)   text "ADMIN"
TECNICO   → background rgba(59,130,246,0.15)  color var(--color-info)     text "TÉCNICO"
RECEPCION → background rgba(34,197,94,0.15)   color var(--color-success)  text "RECEPCIÓN"
```
- Border-radius 20px, padding 3px 10px, font-size 0.75rem, font-weight 500

**Column: Estado**
- Green dot + "Activo" text if `activo === true`: dot `var(--color-success)`, text `var(--color-text-secondary)`, 0.85rem
- Red dot + "Inactivo" if false: dot `var(--color-danger)`
- Dot: 8px circle, inline-block, margin-right 6px

**Skeleton loader (while isLoading):**
- Show 5 skeleton rows inside the table structure (not cards)
- Each row: avatar circle skeleton + two lines + pill + dot
- Pulse animation 1.5s infinite

**Empty state (no users yet):**
- Centered inside the table area (not full page)
- mat-icon `group` 48px `var(--color-text-muted)`
- "No hay usuarios registrados" — `var(--color-text-secondary)`
- "+ Agregar el primer usuario" button → opens dialog

### TypeScript — list.component.ts

```typescript
// Properties needed:
usuarios: UsuarioResponse[] = [];
isLoading = true;
currentRole = '';

// On init:
// 1. currentRole = this.authService.getCurrentRole()
// 2. If currentRole !== 'ADMIN': navigate to /dashboard immediately
// 3. Load usuarios:
//    - If GET /api/usuarios exists in backend: call usuarioService.listarUsuarios()
//    - If NOT: initialize with empty array, show list of users added in the current session
//    Comment: // TODO: replace with GET /api/usuarios when endpoint is available

// openCreateDialog():
// Open MatDialog with CreateUserDialogComponent
// Dialog config: width '480px', panelClass 'dark-dialog', disableClose: false
// After dialog closes, if result is a UsuarioResponse:
//   this.usuarios.push(result)  ← add to list immediately (optimistic update)
//   show snackbar "Usuario creado correctamente"

// getInitials(nombre: string): return nombre.charAt(0).toUpperCase()

// formatDate(dateStr: string): 
//   return new Date(dateStr).toLocaleDateString('es-AR', { day:'2-digit', month:'2-digit', year:'numeric' })

// Use takeUntilDestroyed(this.destroyRef) for all subscriptions
// private destroyRef = inject(DestroyRef)
```

---

## SCREEN 2 — Create User Dialog

### Layout

```
┌─────────────────────────────────────────────┐
│  Nuevo Usuario                         [✕]  │  ← dialog header
│  Completá los datos del nuevo usuario       │
├─────────────────────────────────────────────┤
│                                             │
│  Nombre completo *                          │
│  [____________________________________]     │
│                                             │
│  Email *                                    │
│  [____________________________________]     │
│                                             │
│  Contraseña *                               │
│  [__________________________] [👁]          │
│  Mínimo 8 caracteres                        │
│                                             │
│  Rol *                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │  ADMIN   │ │ TÉCNICO  │ │ RECEPCIÓN│   │  ← role selector cards
│  └──────────┘ └──────────┘ └──────────┘   │
│                                             │
│           [Cancelar]    [Crear Usuario →]  │
└─────────────────────────────────────────────┘
```

### Dialog container styling

Override Angular Material dialog styles via `::ng-deep` in the component SCSS or in `styles.scss`:

```scss
.dark-dialog .mat-mdc-dialog-container {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-elevated);
  padding: 0;
}

.cdk-overlay-backdrop.cdk-overlay-dark-backdrop {
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
}
```

Add scale-in animation to dialog:
```scss
@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}
.dark-dialog .mat-mdc-dialog-container {
  animation: dialogIn 0.18s ease forwards;
}
```

### Dialog header

- Padding: 24px 28px 0
- Title: "Nuevo Usuario", 1.1rem, font-weight 600, `var(--color-text-primary)`
- Subtitle: "Completá los datos del nuevo usuario", 0.85rem, `var(--color-text-secondary)`, margin-top 4px
- Close button: top-right, mat-icon-button with `close` icon, `var(--color-text-muted)`, hover `var(--color-text-primary)`, `position: absolute`, top 16px, right 16px

### Form fields (padding 24px 28px)

All inputs follow the exact same dark pattern as the login screen:
- Label: `var(--color-text-secondary)`, 0.8rem, `font-weight: 500`, `display: block`, margin-bottom 6px
- Input: full width, `var(--color-surface-2)` background, `1px solid var(--color-border)` border, `var(--radius-input)` border-radius, padding 12px 16px, `var(--color-text-primary)` color, 0.9rem font-size
- Focus: `border-color: var(--color-accent)`, `outline: none`
- Error state: `border-color: var(--color-danger)`
- Error text: `var(--color-danger)`, 0.78rem, margin-top 4px, `animation: fadeInDown 0.2s ease`
- Field spacing: `margin-bottom: 20px` between fields

**Nombre field:**
- Validation: required
- Error: "El nombre es requerido"

**Email field:**
- Type: email
- Validation: required + Validators.email
- Errors:
  - "El email es requerido" if empty
  - "Ingresá un email válido" if format invalid
  - "Este email ya está registrado" if backend returns 4xx with message about duplicate email

**Password field:**
- Type: password (toggle with show/hide eye icon)
- Eye icon: `mat-icon-button` positioned absolute inside input wrapper, right 8px, `var(--color-text-muted)`, toggles between `visibility` and `visibility_off`
- Input wrapper: `position: relative`
- Input: `padding-right: 48px` to avoid text overlapping the eye icon
- Validation: required + Validators.minLength(8)
- Errors:
  - "La contraseña es requerida" if empty
  - "Mínimo 8 caracteres" if too short — show as hint in `var(--color-text-muted)` when not touched, in `var(--color-danger)` when touched and invalid

**Rol selector (custom card-based, NOT mat-select):**

Three cards in a row (equal width, flex):
```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  🛡️              │  │  🔧              │  │  📋              │
│  ADMIN           │  │  TÉCNICO         │  │  RECEPCIÓN       │
│  Acceso total    │  │  Órdenes y stock │  │  Clientes y OT   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

- Unselected: `var(--color-surface-2)` background, `1px solid var(--color-border)` border, `var(--radius-card)` border-radius, padding 14px 12px, cursor pointer, `transition: var(--transition)`
- Selected: `1px solid var(--color-accent)` border, `rgba(249,115,22,0.08)` background, text `var(--color-accent)`
- Hover (unselected): `var(--color-border)` border darkens slightly, `translateY(-1px)`
- Icon: mat-icon 24px, centered above text
- Role name: 0.85rem, font-weight 600
- Description: 0.75rem, `var(--color-text-muted)`, margin-top 4px

Role icons mapping:
- ADMIN → `admin_panel_settings`
- TECNICO → `engineering`
- RECEPCION → `support_agent`

Role descriptions:
- ADMIN → "Acceso total al sistema"
- TECNICO → "Órdenes y stock"
- RECEPCION → "Clientes y órdenes"

The selected rol value feeds into `formGroup.get('rol')` via `(click)="selectRol('ADMIN')"` pattern:
```typescript
selectRol(rol: string) {
  this.form.get('rol')?.setValue(rol);
}
```

Do NOT use mat-radio or mat-select for the role selector — use the card approach above.

**Validation error for rol:** if submitted without selecting a role: small red text below the cards "Seleccioná un rol"

### Dialog footer (padding 20px 28px 24px)

- Border-top: `1px solid var(--color-border)`
- Flex row, justify-content space-between (or flex-end with gap)
- **Cancelar:** `.btn-ghost` style, `(click)="onCancel()"`
- **Crear Usuario →:** `.btn-primary` style, `[disabled]="isLoading"`, `(click)="onSubmit()"`
- Loading state on button: replace text with animated dots or show `mat-progress-spinner` diameter 16px inline, white color

### TypeScript — create-user-dialog.component.ts

```typescript
// Inject: MatDialogRef<CreateUserDialogComponent>, UsuarioService, MatSnackBar, DestroyRef

// Form:
form = this.fb.group({
  nombre:   ['', [Validators.required]],
  email:    ['', [Validators.required, Validators.email]],
  password: ['', [Validators.required, Validators.minLength(8)]],
  rol:      ['', [Validators.required]]
});

// Properties:
isLoading = false;
showPassword = false;
selectedRol = '';

// Methods:
selectRol(rol: string): void
  // sets selectedRol = rol
  // form.get('rol')?.setValue(rol)

togglePassword(): void
  // showPassword = !showPassword

onCancel(): void
  // dialogRef.close()  ← closes without result

onSubmit(): void
  // 1. If form.invalid: markAllAsTouched() and return
  // 2. isLoading = true
  // 3. Call usuarioService.crearUsuario(form.value)
  // 4. On success:
  //    isLoading = false
  //    dialogRef.close(result)  ← passes UsuarioResponse back to parent
  // 5. On error:
  //    isLoading = false
  //    If err.error?.message contains 'email' or 'exist':
  //      set email control error: form.get('email')?.setErrors({ duplicate: true })
  //    Else:
  //      show snackbar err.error?.message || 'Error al crear el usuario'

// Getters:
get nombreControl() { return this.form.get('nombre'); }
get emailControl()  { return this.form.get('email');  }
get passwordControl() { return this.form.get('password'); }
get rolControl()    { return this.form.get('rol');    }
```

---

## STEP — UsuarioService

**File:** `frontend/src/app/modules/usuarios/services/usuario.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface UsuarioRequest {
  nombre: string;
  email: string;
  password: string;
  rol: string;
}

export interface UsuarioResponse {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'TECNICO' | 'RECEPCION';
  activo: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private authApi = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  crearUsuario(data: UsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.authApi}/register`, data);
  }

  // TODO: add listarUsuarios() when GET /api/usuarios endpoint is available
}
```

---

## STEP — Routes

**File:** `frontend/src/app/modules/usuarios/usuarios.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { ListComponent } from './list/list.component';

export const USUARIOS_ROUTES: Routes = [
  { path: '', component: ListComponent }
];
```

**Update `app.routes.ts`** — add lazy route for usuarios if not already present:
```typescript
{
  path: 'usuarios',
  canActivate: [AuthGuard],
  loadChildren: () => import('./modules/usuarios/usuarios.routes')
    .then(m => m.USUARIOS_ROUTES)
}
```

---

## SCSS patterns — use these consistently

```scss
// Role badge chips
.rol-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;

  &.admin     { background: rgba(239,68,68,0.15);  color: var(--color-danger);  }
  &.tecnico   { background: rgba(59,130,246,0.15); color: var(--color-info);    }
  &.recepcion { background: rgba(34,197,94,0.15);  color: var(--color-success); }
}

// Status dot
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;

  &.active   { background: var(--color-success); }
  &.inactive { background: var(--color-danger);  }
}

// Table styles
.data-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: 12px 20px;
    text-align: left;
    color: var(--color-text-muted);
    font-size: 0.75rem;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    border-bottom: 1px solid var(--color-border);
  }

  td {
    padding: 14px 20px;
    border-bottom: 1px solid var(--color-border);
    color: var(--color-text-primary);
    font-size: 0.875rem;
  }

  tr:last-child td { border-bottom: none; }

  tbody tr {
    transition: var(--transition);
    &:hover { background: var(--color-surface-2); }
  }
}

// Avatar initials circle
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(249, 115, 22, 0.15);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
}

// Dark input (identical to login and Phase 1)
.field-input {
  width: 100%;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-input);
  padding: 12px 16px;
  color: var(--color-text-primary);
  font-size: 0.9rem;
  transition: var(--transition);
  outline: none;
  font-family: inherit;

  &::placeholder { color: var(--color-text-muted); }
  &:focus         { border-color: var(--color-accent); }
  &.error         { border-color: var(--color-danger); }
}

// Rol selector card
.rol-card {
  flex: 1;
  padding: 14px 12px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  cursor: pointer;
  transition: var(--transition);
  text-align: center;

  &:hover:not(.selected) { transform: translateY(-1px); }

  &.selected {
    border-color: var(--color-accent);
    background: rgba(249, 115, 22, 0.08);

    .rol-name { color: var(--color-accent); }
    mat-icon  { color: var(--color-accent); }
  }

  mat-icon  { font-size: 24px; color: var(--color-text-secondary); margin-bottom: 8px; }
  .rol-name { font-size: 0.85rem; font-weight: 600; color: var(--color-text-primary); }
  .rol-desc { font-size: 0.75rem; color: var(--color-text-muted); margin-top: 4px; }
}
```

---

## Critical rules

1. **Zero `.ts` file modifications outside the usuarios module** — do not touch auth.service.ts, jwt.interceptor.ts, auth.guard.ts or any Phase 1 files.
2. **Zero hardcoded hex values** in any `.scss` file. Every color must be `var(--color-*)`.
3. **No `mat-form-field`** — use plain HTML inputs styled with `.field-input` class, keeping all Angular reactive form bindings intact.
4. **Role guard in component** — if `currentRole !== 'ADMIN'`, navigate to `/dashboard` immediately in `ngOnInit`. Do not rely only on the route guard.
5. **Dialog panel class** — always pass `panelClass: 'dark-dialog'` when opening MatDialog, and define `.dark-dialog` styles in `styles.scss` (not in component SCSS) so they are globally available.
6. **Close dialog with result** — `dialogRef.close(result)` on success so the parent list can update immediately without a full page reload.
7. **Do NOT call `GET /api/usuarios`** if that endpoint doesn't exist in the backend — use local state and add the TODO comment.
8. **Duplicate email error** — detect it from the backend error message and set it as a form control error, not just a snackbar. The user should see the error inline on the email field.
9. **Password field** must start as `type="password"` and toggle to `type="text"` — bind `[type]="showPassword ? 'text' : 'password'"`.
10. **Build must pass** — run `ng build --configuration development` and fix every error before finishing.

---

## After completing all files

Run `ng build --configuration development` and confirm zero errors. Then print this checklist:

```
PHASE 2 — USUARIOS MODULE — COMPLETED

SCREEN 1 — /usuarios (List)
  ✅ list.component.ts  — [one line description]
  ✅ list.component.html — [one line description]
  ✅ list.component.scss — [one line description]

SCREEN 2 — Create User Dialog
  ✅ create-user-dialog.component.ts   — [one line description]
  ✅ create-user-dialog.component.html — [one line description]
  ✅ create-user-dialog.component.scss — [one line description]

SERVICES
  ✅ usuario.service.ts — crearUsuario() wired to POST /auth/register

ROUTING
  ✅ usuarios.routes.ts — single route to ListComponent
  ✅ app.routes.ts      — lazy route /usuarios added

STYLES
  ✅ styles.scss — .dark-dialog panel styles added globally

DESIGN SYSTEM COMPLIANCE
  ✅ Zero hardcoded hex values
  ✅ Dark inputs follow login pattern
  ✅ Role badge chips consistent with Phase 1 estado chips
  ✅ Skeleton loader on list
  ✅ Dialog scale-in animation
  ✅ Role selector cards (not mat-select)
  ✅ ADMIN-only guard applied in component

BUILD
  ✅ ng build passes with zero errors
```