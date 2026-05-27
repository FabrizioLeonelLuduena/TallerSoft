# TallerSoft — Prompt Fase 1: Módulo Clientes (Frontend)

You are an expert UI/UX designer and senior Angular developer working on the TallerSoft project. The design system, shell layout (sidebar + header), login screen, and global styles are already fully implemented.

Your task is to build **Phase 1: the complete Clientes module** — 3 screens, fully connected to the real backend API.

---

## MANDATORY — Read these files FIRST, in this exact order, before writing a single line of code:

1. `frontend/src/styles.scss` — all CSS custom properties are defined here. Every color, radius, shadow and transition you use MUST come from this file. Zero hardcoded hex values.
2. `frontend/src/app/modules/auth/login/login.component.html` — reference for how dark inputs and forms are styled in this project.
3. `frontend/src/app/modules/auth/login/login.component.scss` — reference for SCSS patterns used.
4. `frontend/src/app/shared/components/layout/sidebar/sidebar.component.html` — understand the existing shell before touching routes.
5. `frontend/src/app/app.routes.ts` — verify the `/clientes` lazy route already exists before creating new routes.

Do NOT proceed until you have read all 5 files above.

---

## Design System — Rules that apply to every file in this phase

- All colors → CSS custom properties from `styles.scss` (`var(--color-bg)`, `var(--color-accent)`, etc.)
- All inputs → dark background `var(--color-surface-2)`, border `var(--color-border)`, focus border `var(--color-accent)`, same style as login inputs
- All cards → `var(--color-surface)` background, `var(--radius-card)` border-radius, `var(--shadow-card)` shadow
- Hover on cards → `transform: translateY(-3px)`, `var(--shadow-elevated)`, `transition: var(--transition)`
- Loading state → skeleton loaders with animated gray pulse, NEVER mat-spinner as primary loader
- Empty state → centered icon (mat-icon) + descriptive text + action button
- Toast notifications → use MatSnackBar, bottom-right position, 3s duration
- Typography → Inter font (already imported), headings weight 600, body weight 400
- Role-based visibility → use `*ngIf` bound to `AuthService.getCurrentRole()` to show/hide elements per role

---

## Screens to build

---

### SCREEN 1 — `/clientes` — Client List

**Files to create:**
- `frontend/src/app/modules/clientes/list/list.component.html`
- `frontend/src/app/modules/clientes/list/list.component.scss`

**Files to update:**
- `frontend/src/app/modules/clientes/clientes.routes.ts` — add the `/clientes` → `ListComponent` route if not present

**Layout:**

```
┌─────────────────────────────────────────────────────────┐
│  [🔍 Buscar cliente por nombre...]         [+ Nuevo]    │  ← sticky top bar
├─────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Avatar   │  │ Avatar   │  │ Avatar   │  ...          │  ← card grid
│  │ Nombre   │  │ Nombre   │  │ Nombre   │              │
│  │ Tel/Mail │  │ Tel/Mail │  │ Tel/Mail │              │
│  │ X órd.   │  │ X órd.   │  │ X órd.   │              │
│  └──────────┘  └──────────┘  └──────────┘              │
└─────────────────────────────────────────────────────────┘
```

**Search bar:**
- Full width input with search icon prefix
- `var(--color-surface)` background, `var(--radius-input)` border-radius, padding 12px 16px
- Placeholder: "Buscar cliente por nombre..."
- Debounce 300ms using `rxjs/operators` — calls `ClienteService.listarClientes(nombre)`
- On clear (input empty) → reload all clients

**"+ Nuevo Cliente" button:**
- Top-right, accent filled (`var(--color-accent)` background, white text)
- Only visible for roles ADMIN and RECEPCION
- On click → navigate to `/clientes/nuevo`

**Card grid:**
- CSS Grid: `repeat(auto-fill, minmax(280px, 1fr))`, gap 20px
- Each card:

```
┌────────────────────────────────┐
│  ●   Carlos García             │  ← avatar circle (initials) + name bold white
│      carlos@gmail.com          │  ← email in --color-text-secondary
│      351-555-0001              │  ← phone in --color-text-secondary
│                                │
│  [2 órdenes activas]  [  →  ] │  ← badge + navigate button
└────────────────────────────────┘
```

- **Avatar circle:** 48px diameter, `var(--color-accent)` background, white initials (first letter of nombre), font-weight 600, font-size 1.1rem
- **Name:** 1rem, font-weight 600, `var(--color-text-primary)`, truncate with ellipsis if long
- **Email and phone:** 0.85rem, `var(--color-text-secondary)`, show mat-icon (email/phone) inline before text, 14px icon
- **"X órdenes activas" badge:** only show if > 0. Background `rgba(249, 115, 22, 0.12)`, color `var(--color-accent)`, border-radius 20px, font-size 0.75rem, padding 3px 10px
- **Navigate button:** ghost style, right side of card footer, mat-icon `arrow_forward`, on click → navigate to `/clientes/{id}`
- **Card hover:** `transform: translateY(-3px)`, `box-shadow: var(--shadow-elevated)`, `transition: var(--transition)`, cursor pointer
- **Card click** (anywhere except button) also navigates to `/clientes/{id}`

**Skeleton loader:**
- Show 6 skeleton cards while data loads
- Each skeleton card: same dimensions as real card, animated gray pulse
- Pulse animation: `@keyframes pulse { 0%,100%{opacity:0.4} 50%{opacity:0.8} }`, 1.5s infinite
- Skeleton elements: circle (avatar) + 3 lines (name, email, phone) + bottom bar

**Empty state (no results):**
- Centered in the grid area
- mat-icon `people_outline`, 64px, `var(--color-text-muted)`
- Text "No se encontraron clientes" in `var(--color-text-secondary)`
- If search is active: sub-text "Intentá con otro nombre"
- If no search: button "+ Agregar primer cliente" → navigate to `/clientes/nuevo`

**API integration:**
```typescript
// On init:
this.clienteService.listarClientes().subscribe(...)

// On search input (debounce 300ms):
this.clienteService.listarClientes(searchValue).subscribe(...)

// On delete (ADMIN only — show confirm dialog first):
this.clienteService.eliminarCliente(id).subscribe(...)
```

**Role guards:**
- Delete button → only render with `*ngIf="currentRole === 'ADMIN'"`
- New client button → only render with `*ngIf="currentRole === 'ADMIN' || currentRole === 'RECEPCION'"`

---

### SCREEN 2 — `/clientes/nuevo` — Create Client

**Files to create:**
- `frontend/src/app/modules/clientes/create/create.component.html`
- `frontend/src/app/modules/clientes/create/create.component.scss`

**Files to update:**
- `frontend/src/app/modules/clientes/clientes.routes.ts` — add `/clientes/nuevo` → `CreateComponent`

**Layout:**
```
┌──────────────────────────────────────────┐
│  ← Volver a clientes                     │  ← back link
│                                          │
│  Nuevo Cliente                           │  ← page title
│  Completá los datos del nuevo cliente   │  ← subtitle
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  Nombre *           [____________] │  │
│  │  Teléfono           [____________] │  │
│  │  Email              [____________] │  │
│  │  Dirección          [____________] │  │
│  │                                    │  │
│  │           [Cancelar]  [Guardar →]  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

**Form card:**
- Background `var(--color-surface)`, border-radius `var(--radius-card)`, padding 32px 40px
- Max-width 600px, centered horizontally
- Box-shadow `var(--shadow-card)`

**Fields:**
- **Nombre** (required): `@NotBlank` — show inline error "El nombre es requerido" if submitted empty
- **Teléfono** (optional): tel type input
- **Email** (optional): email type input — show inline error "Ingresá un email válido" if format invalid
- **Dirección** (optional): text input

**Input styling** (same as login screen):
- Label: `var(--color-text-secondary)`, 0.8rem, above the input
- Input: full width, `var(--color-surface-2)` background, `1px solid var(--color-border)` border, `var(--radius-input)` border-radius, padding 12px 16px, `var(--color-text-primary)` color
- Focus: `border-color: var(--color-accent)`, `outline: none`, `transition: var(--transition)`
- Placeholder: `var(--color-text-muted)`
- Error state: `border-color: var(--color-danger)`, error text in `var(--color-danger)` 0.8rem below input, fade-in animation

**Inline validation error style:**
```scss
.field-error {
  color: var(--color-danger);
  font-size: 0.8rem;
  margin-top: 4px;
  animation: fadeInDown 0.2s ease forwards;
}
```

**Buttons:**
- **Cancelar:** ghost style — transparent background, `var(--color-text-secondary)` text, hover `var(--color-surface-2)` background
- **Guardar →:** accent filled — `var(--color-accent)` background, white text, font-weight 600, padding 12px 28px, `var(--radius-btn)` border-radius. On hover: `var(--color-accent-hover)`, `translateY(-1px)`. Loading state: disabled + spinner dots

**Back link:**
- "← Volver a clientes" in `var(--color-text-secondary)`, hover `var(--color-text-primary)`, navigate to `/clientes`

**On submit:**
```typescript
this.clienteService.crearCliente(formData).subscribe({
  next: (cliente) => {
    // Show success snackbar: "Cliente creado correctamente"
    // Navigate to /clientes/{cliente.id}
  },
  error: (err) => {
    // Show error snackbar: err.error.message or "Error al crear el cliente"
  }
})
```

**Do NOT modify `create.component.ts`** — only `.html` and `.scss`.

---

### SCREEN 3 — `/clientes/:id` — Client Detail

**Files to create:**
- `frontend/src/app/modules/clientes/detail/detail.component.html`
- `frontend/src/app/modules/clientes/detail/detail.component.scss`

**Files to update:**
- `frontend/src/app/modules/clientes/clientes.routes.ts` — add `/clientes/:id` → `DetailComponent`

**Layout:**
```
┌──────────────────────────────────────────────────────┐
│  ← Volver                                            │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │  ●  Carlos García              [Editar] [🗑]   │  │  ← header card
│  │     carlos@gmail.com  |  351-555-0001          │  │
│  │     Av. Colón 1234, Córdoba                    │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  [ Equipos (2) ]  [ Historial de Órdenes (5) ]      │  ← tabs
│  ─────────────────────────────────────────────────  │
│                                                      │
│  TAB EQUIPOS:                                        │
│  ┌──────────┐  ┌──────────┐                         │
│  │ Notebook │  │ Celular  │                         │
│  │ Lenovo   │  │ Samsung  │                         │
│  │ ThinkPad │  │ A52      │                         │
│  └──────────┘  └──────────┘                         │
│                                                      │
│  TAB HISTORIAL:                                      │
│  [ tabla de órdenes del cliente ]                    │
└──────────────────────────────────────────────────────┘
```

**Header card:**
- Background `var(--color-surface)`, border-radius `var(--radius-card)`, padding 28px 32px
- Left: avatar circle 64px + initials, next to it: name (1.3rem, font-weight 700), email and phone in muted text with icons, address below in muted text
- Right side: "Editar" button (outlined accent) + delete button (ghost red) — delete only for ADMIN
- On "Editar": fields become editable inline (inputs appear replacing text), buttons change to "Guardar" / "Cancelar"
- Editing saves via `clienteService.editarCliente(id, data)`, shows success toast

**Tabs:**
- Use Angular Material `mat-tab-group` styled dark:
  - Tab bar background: `var(--color-surface)`, border-bottom `var(--color-border)`
  - Active tab: `var(--color-accent)` underline and text color
  - Inactive: `var(--color-text-secondary)`
- Tab labels include count in parentheses, loaded from data

**Tab 1 — Equipos:**

Equipment card grid (2-3 columns):
```
┌───────────────────────────┐
│  💻  Notebook              │  ← type icon (use mat-icon)
│      Lenovo ThinkPad E15   │  ← marca + modelo
│      SN: SN-LNVO-0001     │  ← número de serie (muted)
│      "Pantalla rota"       │  ← observaciones (muted, italic)
└───────────────────────────┘
```
- Card: `var(--color-surface-2)` background, `var(--radius-card)` border-radius, padding 16px 20px
- Type icon: mat-icon mapped from tipo (`laptop_mac` for Notebook, `smartphone` for Celular, `devices_other` for others), 28px, `var(--color-accent)`
- Equipment type: bold white, 0.9rem
- Marca + modelo: `var(--color-text-secondary)`, 0.85rem
- Serial: `var(--color-text-muted)`, 0.8rem, font-family monospace
- Observaciones: `var(--color-text-muted)`, 0.8rem, italic — hide if empty
- Empty state: "Este cliente no tiene equipos registrados" with mat-icon `devices`

**Tab 2 — Historial de Órdenes:**

Table:
```
| ID    | Equipo     | Estado          | Presupuesto  | Fecha      | →  |
|-------|------------|-----------------|--------------|------------|----|
| #0042 | Notebook   | [EN PROCESO]    | $11.400      | 15/05/2026 | →  |
| #0038 | Celular    | [ENTREGADO]     | $2.200       | 10/05/2026 | →  |
```
- Table background: `var(--color-surface)`, border-radius `var(--radius-card)`
- Header row: `var(--color-text-muted)`, 0.8rem, font-weight 500, uppercase, border-bottom `var(--color-border)`
- Rows: `var(--color-text-primary)`, hover `var(--color-surface-2)` background
- Row left border (3px) matching estado color:
  - PENDIENTE → `var(--color-info)`
  - EN_PROCESO → `var(--color-accent)`
  - LISTO → `var(--color-success)`
  - ENTREGADO → `var(--color-text-muted)`
- **Estado chip:**
  - PENDIENTE: blue bg `rgba(59,130,246,0.15)`, text `var(--color-info)`
  - EN_PROCESO: orange bg `rgba(249,115,22,0.15)`, text `var(--color-accent)`
  - LISTO: green bg `rgba(34,197,94,0.15)`, text `var(--color-success)`
  - ENTREGADO: gray bg `rgba(75,85,99,0.2)`, text `var(--color-text-muted)`
- Presupuesto: format as `$X.XXX` ARS, font-family monospace, align right
- Navigate arrow → routes to `/ordenes/{id}`
- Empty state: "Este cliente no tiene órdenes registradas"

**API calls in this component:**
```typescript
// On init (use route.snapshot.paramMap.get('id')):
this.clienteService.obtenerCliente(id).subscribe(...)
this.equipoService.listarEquiposDelCliente(id).subscribe(...)
this.ordenService.listarOrdenes({ clienteId: id }).subscribe(...)

// On edit save:
this.clienteService.editarCliente(id, data).subscribe(...)

// On delete (confirm dialog first, ADMIN only):
this.clienteService.eliminarCliente(id).subscribe({
  next: () => { /* snackbar + navigate to /clientes */ }
})
```

**Do NOT modify any `.ts` files.** Only `.html` and `.scss`.

---

## SCSS Patterns — apply consistently across all 3 screens

```scss
// Skeleton pulse animation
@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50%       { opacity: 0.8; }
}

.skeleton {
  background: var(--color-surface-2);
  border-radius: 4px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

// Estado chips
.estado-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;

  &.pendiente  { background: rgba(59,130,246,0.15);  color: var(--color-info);         }
  &.en-proceso { background: rgba(249,115,22,0.15);  color: var(--color-accent);       }
  &.listo      { background: rgba(34,197,94,0.15);   color: var(--color-success);      }
  &.entregado  { background: rgba(75,85,99,0.2);     color: var(--color-text-muted);   }
}

// Dark input (same as login)
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

// Primary button
.btn-primary {
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-btn);
  padding: 12px 24px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: var(--transition);

  &:hover    { background: var(--color-accent-hover); transform: translateY(-1px); }
  &:active   { transform: translateY(0); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

// Ghost button
.btn-ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border: none;
  border-radius: var(--radius-btn);
  padding: 10px 16px;
  cursor: pointer;
  transition: var(--transition);

  &:hover { background: var(--color-surface-2); color: var(--color-text-primary); }
}

// Outlined accent button
.btn-outlined {
  background: transparent;
  color: var(--color-accent);
  border: 1px solid var(--color-accent);
  border-radius: var(--radius-btn);
  padding: 8px 16px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);

  &:hover { background: rgba(249,115,22,0.08); }
}
```

---

## Critical rules — violations will break the design system

1. **Zero `.ts` file modifications.** Only touch `.html` and `.scss` files in the clientes module.
2. **Zero hardcoded hex values** in `.scss` files. Every color must be `var(--color-...)`.
3. **No `mat-form-field`** for inputs in this module — use plain HTML inputs styled with the `.field-input` class above, keeping all Angular reactive form bindings (`formControlName`, `[formGroup]`, etc.) intact.
4. **Skeleton loaders** on every screen during data load — never use `mat-spinner` as the only loading state.
5. **All 3 screens must be responsive:** cards wrap to 1 column on mobile, table scrolls horizontally on small screens.
6. **Role-based visibility** must be applied: delete buttons only for ADMIN, create button only for ADMIN/RECEPCION.
7. **`takeUntilDestroyed()`** or `AsyncPipe` must be used — no memory leaks from open subscriptions.
8. **Error handling** on every API call — use MatSnackBar to show error messages, never let errors fail silently.
9. **Estado chips** must use the exact color scheme defined above — no deviations.
10. **Tab underline accent** in the detail screen must match `var(--color-accent)` — override Angular Material's default blue via `::ng-deep`.

---

## After completing all files

Print a checklist in this exact format:

```
PHASE 1 — CLIENTES MODULE — COMPLETED FILES

SCREEN 1 — /clientes (List)
  ✅ list.component.html — [one line description of what changed]
  ✅ list.component.scss — [one line description of what changed]
  ✅ clientes.routes.ts  — [one line description of what changed]

SCREEN 2 — /clientes/nuevo (Create)
  ✅ create.component.html — [one line description]
  ✅ create.component.scss — [one line description]
  ✅ clientes.routes.ts    — [one line description]

SCREEN 3 — /clientes/:id (Detail)
  ✅ detail.component.html — [one line description]
  ✅ detail.component.scss — [one line description]
  ✅ clientes.routes.ts    — [one line description]

DESIGN SYSTEM COMPLIANCE
  ✅ Zero hardcoded hex values
  ✅ All inputs follow login screen pattern
  ✅ Skeleton loaders on all 3 screens
  ✅ Role-based visibility applied
  ✅ No .ts files modified
```