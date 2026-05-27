# TallerSoft — Prompt Fase 3: Módulo Órdenes de Trabajo (UI + Integración Backend)

You are an expert UI/UX designer and senior Angular developer working on TallerSoft. The shell, design system, Clientes module (Phase 1) and Usuarios module (Phase 2) are fully implemented and the build passes with zero errors.

Your task is to build **Phase 3: the complete Órdenes de Trabajo module** — 4 screens with full backend integration in a single pass.

---

## MANDATORY — Read these files FIRST, in this exact order:

1. `frontend/src/styles.scss` — CSS custom properties. Zero hardcoded hex values.
2. `frontend/src/app/modules/clientes/list/list.component.html` — established card/table patterns.
3. `frontend/src/app/modules/clientes/list/list.component.ts` — established service/subscription pattern.
4. `frontend/src/app/modules/clientes/list/list.component.scss` — established SCSS patterns.
5. `frontend/src/app/modules/usuarios/list/list.component.ts` — how dialogs are opened and results handled.
6. `frontend/src/app/core/auth/auth.service.ts` — getCurrentRole() and getCurrentUser().
7. `frontend/src/app/modules/clientes/services/cliente.service.ts` — pattern for HTTP services.
8. `frontend/src/environments/environment.ts` — base apiUrl.
9. `frontend/src/app/app.routes.ts` — existing routes before adding new ones.

Do NOT proceed until you have read all 9 files.

---

## Backend API reference

**Base URL:** `environment.apiUrl`
**Auth:** handled automatically by JwtInterceptor.

### Endpoints

| Method | Endpoint | Used in |
|--------|----------|---------|
| `GET` | `/api/ordenes` | List — all orders with optional filters |
| `GET` | `/api/ordenes?estado={e}` | List — filter by estado |
| `GET` | `/api/ordenes?tecnicoId={id}` | List — filter by tecnico |
| `GET` | `/api/ordenes/activas` | Kanban — all non-ENTREGADO orders |
| `GET` | `/api/ordenes/mis-ordenes` | List — TECNICO only, their own orders |
| `GET` | `/api/ordenes/{id}` | Detail — single order |
| `POST` | `/api/ordenes` | Create — new order |
| `PUT` | `/api/ordenes/{id}/estado` | Kanban + Detail — state transition |
| `PUT` | `/api/ordenes/{id}/diagnostico` | Detail — save diagnosis |
| `POST` | `/api/ordenes/{id}/repuestos` | Detail — add part to order |
| `GET` | `/api/clientes?nombre={q}` | Create — autocomplete search |
| `GET` | `/api/equipos/cliente/{id}` | Create — load equipment after client selected |
| `GET` | `/api/repuestos?critico=false` | Detail dialog — search parts |

### Response shapes

```typescript
interface OrdenTrabajoResponse {
  id: number;
  equipoId: number;
  clienteId: number;
  clienteNombre: string;
  tecnicoId: number | null;
  tecnicoNombre: string | null;
  fallaReportada: string;
  diagnostico: string | null;
  estado: 'PENDIENTE' | 'EN_PROCESO' | 'LISTO' | 'ENTREGADO';
  prioridad: 'BAJA' | 'NORMAL' | 'ALTA';
  presupuesto: number;
  createdAt: string;
  updatedAt: string;
  repuestos: OrdenRepuestoResponse[];
}

interface OrdenRepuestoResponse {
  id: number;
  repuestoId: number;
  nombreRepuesto: string;
  cantidad: number;
  precioUnit: number;
  total: number;
}

interface OrdenTrabajoRequest {
  equipoId: number;
  clienteId: number;
  tecnicoId?: number;
  fallaReportada: string;
  prioridad: 'BAJA' | 'NORMAL' | 'ALTA';
}

interface CambiarEstadoRequest {
  nuevoEstado: 'PENDIENTE' | 'EN_PROCESO' | 'LISTO' | 'ENTREGADO';
}

interface DiagnosticoRequest {
  diagnostico: string;
}

interface AgregarRepuestoRequest {
  repuestoId: number;
  cantidad: number;
}

// Error response
interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
```

### State machine — valid transitions ONLY:
```
PENDIENTE → EN_PROCESO
EN_PROCESO → LISTO        (requires diagnostico != null)
LISTO → ENTREGADO
```
Any other transition returns `409 Conflict` from backend.

---

## Module structure to create

```
frontend/src/app/modules/ordenes/
├── list/
│   ├── list.component.ts
│   ├── list.component.html
│   └── list.component.scss
├── kanban/
│   ├── kanban.component.ts
│   ├── kanban.component.html
│   └── kanban.component.scss
├── detail/
│   ├── detail.component.ts
│   ├── detail.component.html
│   └── detail.component.scss
├── create/
│   ├── create.component.ts
│   ├── create.component.html
│   └── create.component.scss
├── dialogs/
│   └── add-repuesto-dialog/
│       ├── add-repuesto-dialog.component.ts
│       ├── add-repuesto-dialog.component.html
│       └── add-repuesto-dialog.component.scss
├── services/
│   ├── ordenes.service.ts
│   └── repuestos.service.ts
└── ordenes.routes.ts
```

---

## SERVICES

### ordenes.service.ts

```typescript
@Injectable({ providedIn: 'root' })
export class OrdenesService {
  private api = `${environment.apiUrl}/api/ordenes`;

  listarOrdenes(filtros?: { estado?: string; tecnicoId?: number }): Observable<OrdenTrabajoResponse[]>
  listarOrdenesActivas(): Observable<OrdenTrabajoResponse[]>  // GET /api/ordenes/activas
  listarMisOrdenes(): Observable<OrdenTrabajoResponse[]>      // GET /api/ordenes/mis-ordenes
  obtenerOrden(id: number): Observable<OrdenTrabajoResponse>
  crearOrden(data: OrdenTrabajoRequest): Observable<OrdenTrabajoResponse>
  cambiarEstado(id: number, nuevoEstado: string): Observable<OrdenTrabajoResponse>
  agregarDiagnostico(id: number, diagnostico: string): Observable<OrdenTrabajoResponse>
  agregarRepuesto(ordenId: number, data: AgregarRepuestoRequest): Observable<OrdenTrabajoResponse>
}
```

### repuestos.service.ts

```typescript
@Injectable({ providedIn: 'root' })
export class RepuestosService {
  private api = `${environment.apiUrl}/api/repuestos`;

  listarRepuestos(): Observable<RepuestoResponse[]>
  buscarRepuestos(nombre: string): Observable<RepuestoResponse[]>  // GET /api/repuestos?nombre={q} if available, else filter client-side
  obtenerRepuesto(id: number): Observable<RepuestoResponse>
}

interface RepuestoResponse {
  id: number;
  nombre: string;
  categoria: string | null;
  precio: number;
  stockActual: number;
  stockMinimo: number;
  critico: boolean;
}
```

---

## SCREEN 1 — `/ordenes` — Order List

### Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Órdenes de Trabajo              [Kanban] [+ Nueva Orden]       │
│  Seguimiento de reparaciones                                     │
├─────────────────────────────────────────────────────────────────┤
│  [Todos ▾] [PENDIENTE] [EN PROCESO] [LISTO] [ENTREGADO]        │  ← filter chips
│  [Técnico: Todos ▾]  [Desde: ──] [Hasta: ──]                   │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ #   │ Cliente    │ Estado      │ Técnico  │ Presup. │ Fecha│ │
│  ├──── ┼────────────┼─────────────┼──────────┼─────────┼─────┤ │
│  │ #01 │ C. García  │ [EN PROC.]  │ J. Mart. │ $11.400 │ hoy │ │
│  │ #02 │ M. López   │ [PENDIENTE] │ —        │ $0      │ ayer│ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Header
- Title "Órdenes de Trabajo", subtitle "Seguimiento de reparaciones"
- **[Kanban]** button — outlined accent — `routerLink="/ordenes/kanban"`
- **[+ Nueva Orden]** button — accent filled — `routerLink="/ordenes/nueva"` — only for ADMIN and RECEPCION

### Filter bar
- **Estado chips:** "Todos" (selected by default) + one chip per estado. Clicking a chip filters the table. Active chip: accent background + white text. Inactive: surface-2 background + muted text.
- **Técnico select:** mat-select dark styled — only visible for ADMIN. Shows list of unique technicians from current orders. "Todos los técnicos" default option.
- TECNICO role: skip filters entirely, call `listarMisOrdenes()` and show only their orders with a subtle "Mis órdenes" label.

### Table

Same dark table pattern as Usuarios module:
- Card wrapper: `var(--color-surface)`, `var(--radius-card)`, `var(--shadow-card)`
- Header: muted uppercase 0.75rem
- Row hover: `var(--color-surface-2)`
- Last row no border-bottom

**Column: #** — `#` + id zero-padded to 4 digits (`#0042`), font-family monospace, `var(--color-text-muted)`, 0.85rem

**Column: Cliente** — clienteNombre bold, below it equipo tipo in muted text 0.8rem

**Column: Estado** — chip matching estado:
```
PENDIENTE  → rgba(59,130,246,0.15)  var(--color-info)
EN_PROCESO → rgba(249,115,22,0.15) var(--color-accent)
LISTO      → rgba(34,197,94,0.15)  var(--color-success)
ENTREGADO  → rgba(75,85,99,0.2)    var(--color-text-muted)
```

Row left border (3px solid) matching estado color — same colors as above but as solid borders.

**Column: Prioridad** — small badge:
```
ALTA   → var(--color-danger)   "ALTA"
NORMAL → var(--color-warning)  "NORMAL"
BAJA   → var(--color-text-muted) "BAJA"
```

**Column: Técnico** — tecnicoNombre or "—" if null, `var(--color-text-secondary)`

**Column: Presupuesto** — `$X.XXX` ARS format, font-family monospace, align right

**Column: Fecha** — relative time: "hace 2 horas", "ayer", "hace 3 días" — use a simple helper:
```typescript
getRelativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const hours = Math.floor(diff / 3600000);
  if (hours < 1) return 'hace un momento';
  if (hours < 24) return `hace ${hours}h`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'ayer';
  return `hace ${days} días`;
}
```

**Column: Acciones** — single eye icon button (`visibility`), routes to `/ordenes/{id}`

**Row click** — entire row navigates to `/ordenes/{id}`

**Skeleton:** 6 skeleton rows while loading
**Empty state:** mat-icon `assignment` + "No hay órdenes registradas" + "Nueva Orden" button

### TypeScript — list.component.ts

```typescript
ordenes: OrdenTrabajoResponse[] = [];
filteredOrdenes: OrdenTrabajoResponse[] = [];
isLoading = true;
selectedEstado = '';      // '' = Todos
selectedTecnicoId: number | null = null;
currentRole = '';

ngOnInit():
  currentRole = authService.getCurrentRole()
  if currentRole === 'TECNICO':
    call listarMisOrdenes()
  else:
    call listarOrdenes()

filterByEstado(estado: string):
  selectedEstado = estado
  filteredOrdenes = estado ? ordenes.filter(o => o.estado === estado) : [...ordenes]

filterByTecnico(tecnicoId: number | null):
  apply filter on top of estado filter

navigateToDetail(id: number):
  router.navigate(['/ordenes', id])

formatCurrency(amount: number): string
getRelativeTime(dateStr: string): string
padOrderId(id: number): string  // returns '#0042' format
```

---

## SCREEN 2 — `/ordenes/kanban` — Kanban Board

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  Kanban                              [← Lista]  [+ Nueva Orden]  │
├──────────────────────────────────────────────────────────────────┤
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────────┐  │
│  │ PENDIENTE  │ │ EN PROCESO │ │   LISTO    │ │  ENTREGADO  │  │
│  │    (2)     │ │    (3)     │ │    (1)     │ │    (4)      │  │
│  ├────────────┤ ├────────────┤ ├────────────┤ ├─────────────┤  │
│  │ ┌────────┐ │ │ ┌────────┐ │ │ ┌────────┐ │ │ ┌─────────┐ │ │
│  │ │ card  │ │ │ │ card  │ │ │ │ card  │ │ │ │ │ card   │ │ │
│  │ └────────┘ │ │ └────────┘ │ │ └────────┘ │ │ └─────────┘ │ │
│  └────────────┘ └────────────┘ └────────────┘ └─────────────┘  │
│                                                      [+ Nueva]  │  ← FAB
└──────────────────────────────────────────────────────────────────┘
```

### Column headers

```
┌─────────────────────────────┐
│ ● PENDIENTE            (2) │   ← colored dot + name + count badge
└─────────────────────────────┘
```

- Column header background: subtle color tint per estado:
  - PENDIENTE: `rgba(59,130,246,0.08)` border-bottom `2px solid var(--color-info)`
  - EN_PROCESO: `rgba(249,115,22,0.08)` border-bottom `2px solid var(--color-accent)`
  - LISTO: `rgba(34,197,94,0.08)` border-bottom `2px solid var(--color-success)`
  - ENTREGADO: `rgba(75,85,99,0.08)` border-bottom `2px solid var(--color-text-muted)`
- Count badge: small pill, same color as estado, shows number of cards in that column
- Column width: equal flex distribution (`flex: 1`), min-width 220px, scroll horizontally if needed

### Kanban card

```
┌─────────────────────────────────┐
│  Carlos García          [ALTA] │  ← client name + prioridad chip (top-right)
│  Notebook · Lenovo             │  ← equipo tipo + marca (muted)
│  ─────────────────────────────  │
│  No enciende al presionar...   │  ← falla truncated to 2 lines
│                                 │
│  ● JM          hace 2 días →  │  ← tecnico avatar + time + arrow
└─────────────────────────────────┘
```

- Background: `var(--color-surface)`
- Left border: `3px solid` matching estado color
- Border-radius: `var(--radius-card)`
- Padding: 14px 16px
- Box-shadow: `var(--shadow-card)`
- Margin-bottom: 12px
- Cursor: grab

**Prioridad chip (top-right):**
- ALTA: `var(--color-danger)` background with 15% opacity, text `var(--color-danger)`, font-size 0.7rem
- NORMAL: `var(--color-warning)` background with 15% opacity, text `var(--color-warning)`
- BAJA: `var(--color-text-muted)` background with 15% opacity, text `var(--color-text-muted)`

**Falla reportada:** `var(--color-text-secondary)`, 0.82rem, max 2 lines (`-webkit-line-clamp: 2`), font-style italic

**Técnico avatar:** 24px circle, initials, `var(--color-accent)` tinted. If no tecnico: show "Sin asignar" in muted text.

**Relative time:** `var(--color-text-muted)`, 0.75rem, right-aligned

**While dragging:** `box-shadow: var(--shadow-elevated)`, `transform: rotate(1.5deg)`, `opacity: 0.9`

### Empty column state

```
┌────────────────────────┐
│                        │
│   ┌ ─ ─ ─ ─ ─ ─ ┐    │   ← dashed border
│     Sin órdenes        │
│   └ ─ ─ ─ ─ ─ ─ ┘    │
│                        │
└────────────────────────┘
```
- Dashed border: `2px dashed var(--color-border)`
- Icon: mat-icon `inbox` 36px `var(--color-text-muted)`
- Text: "Sin órdenes" `var(--color-text-muted)` 0.85rem

### FAB button (bottom-right fixed)

```scss
.fab {
  position: fixed;
  bottom: 32px;
  right: 32px;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: 28px;
  padding: 14px 22px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(249, 115, 22, 0.4);
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 100;

  &:hover { transform: translateY(-2px); box-shadow: 0 6px 28px rgba(249,115,22,0.5); }
}
```

### Drag and Drop — Angular CDK

Import `DragDropModule` from `@angular/cdk/drag-drop`.

Template pattern:
```html
<div cdkDropListGroup>
  <div *ngFor="let col of columns"
       cdkDropList
       [cdkDropListData]="col.ordenes"
       [id]="col.estado"
       [cdkDropListConnectedTo]="connectedLists"
       (cdkDropListDropped)="onDrop($event)">
    <div *ngFor="let orden of col.ordenes"
         cdkDrag
         [cdkDragData]="orden">
      <!-- card content -->
    </div>
  </div>
</div>
```

### TypeScript — kanban.component.ts

```typescript
interface KanbanColumn {
  estado: string;
  label: string;
  ordenes: OrdenTrabajoResponse[];
}

columns: KanbanColumn[] = [
  { estado: 'PENDIENTE',  label: 'Pendiente',  ordenes: [] },
  { estado: 'EN_PROCESO', label: 'En Proceso', ordenes: [] },
  { estado: 'LISTO',      label: 'Listo',      ordenes: [] },
  { estado: 'ENTREGADO',  label: 'Entregado',  ordenes: [] },
];

connectedLists = ['PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO'];

ngOnInit():
  call ordenesService.listarOrdenesActivas()
  distribute orders into columns by estado

onDrop(event: CdkDragDrop<OrdenTrabajoResponse[]>):
  if event.previousContainer === event.container: return (same column, no-op)

  // 1. Save original column for rollback
  const originalPrevItems = [...event.previousContainer.data];
  const originalCurrItems = [...event.container.data];

  // 2. Optimistic update — move card visually immediately
  transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);

  // 3. Get the new estado from container id
  const nuevoEstado = event.container.id;
  const ordenId = event.item.data.id;

  // 4. Call API
  this.ordenesService.cambiarEstado(ordenId, nuevoEstado).subscribe({
    next: () => { /* update orden.estado in the array */ },
    error: (err) => {
      // ROLLBACK — revert the visual move
      event.previousContainer.data.splice(0, event.previousContainer.data.length, ...originalPrevItems);
      event.container.data.splice(0, event.container.data.length, ...originalCurrItems);

      // Show snackbar with backend error message
      const msg = err.error?.message || 'No se puede realizar esta transición';
      this.snackBar.open(msg, 'Cerrar', {
        duration: 4000,
        horizontalPosition: 'right',
        verticalPosition: 'bottom'
      });
    }
  });
```

---

## SCREEN 3 — `/ordenes/:id` — Order Detail

### Layout

```
┌───────────────────────────────────────────────────────────┐
│  ← Volver                                                 │
│                                                           │
│  ┌────────────────────────────────────────────────────┐   │
│  │  #0042               [EN PROCESO]  [ALTA]         │   │  ← header card
│  │  Carlos García · Notebook Lenovo ThinkPad         │   │
│  │  Creada hace 2 días · Actualizada hace 1 hora     │   │
│  │                              Presupuesto: $11.400 │   │
│  └────────────────────────────────────────────────────┘   │
│                                                           │
│  ┌──────────────────────────────┐ ┌──────────────────┐   │
│  │  DIAGNÓSTICO                 │ │  REPUESTOS       │   │
│  │                              │ │                  │   │
│  │  [textarea editable si       │ │  nombre  cant.   │   │
│  │   TECNICO y no ENTREGADO]    │ │  $precio total   │   │
│  │                              │ │  ────────────    │   │
│  │  TIMELINE DE ESTADOS         │ │  Total: $11.400  │   │
│  │  ✓ Creada      15/05 10:00   │ │                  │   │
│  │  ✓ En Proceso  15/05 14:30   │ │  [+ Agregar]     │   │
│  │  ○ Listo                     │ │                  │   │
│  │  ○ Entregado                 │ │  CAMBIAR ESTADO  │   │
│  └──────────────────────────────┘ │  [select] [→]   │   │
│                                   └──────────────────┘   │
└───────────────────────────────────────────────────────────┘
```

### Header card

- Background: gradient `linear-gradient(135deg, var(--color-surface) 0%, var(--color-surface-2) 100%)`
- Padding: 28px 32px
- Border-radius: `var(--radius-card)`
- Box-shadow: `var(--shadow-card)`

Content:
- **Order number** `#0042` — font-family monospace, 1.1rem, `var(--color-text-muted)`
- **Estado chip + Prioridad badge** — inline next to order number
- **Client name** — 1.4rem, font-weight 700, `var(--color-text-primary)`
- **Equipment info** — tipo + marca + modelo — `var(--color-text-secondary)`, 0.9rem
- **Dates** — "Creada hace X" · "Actualizada hace Y" — `var(--color-text-muted)`, 0.8rem
- **Presupuesto** — right-aligned, label "Presupuesto total" muted, value `$11.400` in `var(--color-accent)`, 1.8rem, font-weight 700

### Left panel — Diagnóstico + Timeline (65% width)

**Diagnóstico section:**
- Label "Diagnóstico" — section title, 0.85rem uppercase muted
- If `currentRole === 'TECNICO'` AND `orden.estado !== 'ENTREGADO'`:
  - Textarea: dark styled, `var(--color-surface-2)` background, `var(--color-border)` border, min-height 120px, resize vertical, full width
  - Below: "Guardado automáticamente" hint or `[Guardar diagnóstico]` button
  - On blur or button click: call `agregarDiagnostico(id, value)` — show success snackbar
- Else: read-only styled div showing diagnostico text or "Sin diagnóstico registrado" in muted italic

**Estado timeline:**
- Label "Historial de estados" — section title
- Four steps: PENDIENTE, EN_PROCESO, LISTO, ENTREGADO
- Completed steps: accent colored dot (`var(--color-accent)`), solid line connecting to next, timestamp shown
- Current step: pulsing accent dot
- Future steps: muted dot, dashed line
- Each step: icon on left, label + timestamp on right

Timeline HTML pattern:
```html
<div class="timeline">
  <div class="timeline-step" [class.completed]="isCompleted('PENDIENTE')" [class.current]="isCurrent('PENDIENTE')">
    <div class="timeline-dot"></div>
    <div class="timeline-content">
      <span class="timeline-label">Creada</span>
      <span class="timeline-time">{{ formatDate(orden.createdAt) }}</span>
    </div>
  </div>
  <!-- repeat for EN_PROCESO, LISTO, ENTREGADO -->
</div>
```

### Right panel — Repuestos + Cambiar Estado (35% width)

**Repuestos table:**
- Card: `var(--color-surface-2)` background, `var(--radius-card)`, padding 16px
- Header: "Repuestos utilizados" — muted uppercase 0.75rem
- Table columns: Nombre, Cant., Precio unit., Total
- Total row: bold, border-top `var(--color-border)`, accent color for the amount
- Empty state: "No se han agregado repuestos" — muted text centered
- **[+ Agregar Repuesto]** button — outlined accent — opens `AddRepuestoDialog` — only for ADMIN and TECNICO if not ENTREGADO

**Cambiar Estado section:**
- Only show for ADMIN and TECNICO if orden.estado !== 'ENTREGADO'
- Label "Cambiar estado" — muted uppercase 0.75rem
- Show only the next valid state as a button (not a select, since there's only one valid next step):
  - If PENDIENTE: button "Iniciar reparación →" (goes to EN_PROCESO)
  - If EN_PROCESO: button "Marcar como Listo →" (goes to LISTO)
  - If LISTO: button "Entregar al cliente →" (goes to ENTREGADO)
- Button: accent filled, full width
- On click: call `cambiarEstado(id, nextEstado)` — on success reload orden, on error show snackbar with `err.error.message`
- If EN_PROCESO and diagnostico is null: disable the "Marcar como Listo" button and show hint "Agregá un diagnóstico antes de continuar"

### TypeScript — detail.component.ts

```typescript
orden: OrdenTrabajoResponse | null = null;
isLoading = true;
isSavingDiagnostico = false;
isChangingEstado = false;
currentRole = '';
diagnosticoText = '';

ngOnInit():
  id = +route.snapshot.paramMap.get('id')
  currentRole = authService.getCurrentRole()
  load orden via obtenerOrden(id)
  on success: orden = data; diagnosticoText = data.diagnostico ?? ''
  on error: snackbar + navigate to /ordenes

getNextEstado(current: string): string | null:
  const map: Record<string,string> = {
    PENDIENTE: 'EN_PROCESO',
    EN_PROCESO: 'LISTO',
    LISTO: 'ENTREGADO'
  };
  return map[current] ?? null;

canSaveDiagnostico(): boolean:
  return currentRole === 'ADMIN' || currentRole === 'TECNICO'
    && orden?.estado !== 'ENTREGADO'

canChangeEstado(): boolean:
  return (currentRole === 'ADMIN' || currentRole === 'TECNICO')
    && orden?.estado !== 'ENTREGADO'

canAddRepuesto(): boolean:
  return (currentRole === 'ADMIN' || currentRole === 'TECNICO')
    && orden?.estado !== 'ENTREGADO'

onSaveDiagnostico():
  call agregarDiagnostico(id, diagnosticoText)
  on success: orden.diagnostico = diagnosticoText; snackbar "Diagnóstico guardado"
  on error: snackbar err.error?.message

onChangeEstado():
  const next = getNextEstado(orden.estado)
  call cambiarEstado(id, next)
  on success: reload orden
  on error: snackbar err.error?.message

openAddRepuestoDialog():
  open AddRepuestoDialogComponent with MatDialog
  pass { ordenId: orden.id } via MAT_DIALOG_DATA
  after close with result: reload orden (to get updated presupuesto and repuestos list)

formatCurrency(amount): '$' + amount.toLocaleString('es-AR')
formatDate(dateStr): new Date(dateStr).toLocaleDateString('es-AR', {...})
isCompleted(estado): check if current orden estado is past this estado
isCurrent(estado): check if this is the current estado
```

---

## SCREEN 4 — `/ordenes/nueva` — Create Order

### Layout

```
┌──────────────────────────────────────────────────────┐
│  ← Volver                                            │
│  Nueva Orden de Trabajo                              │
│  Registrá una nueva reparación                       │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │  Cliente *  [Buscar cliente...       🔍]       │  │  ← autocomplete
│  │             [Carlos García          ✓ ]        │  │  ← selected result
│  │                                                │  │
│  │  Equipo *   [Notebook Lenovo ThinkPad ▾]      │  │  ← select (loads after client)
│  │                                                │  │
│  │  Técnico    [Juan Técnico            ▾]       │  │  ← optional
│  │                                                │  │
│  │  Prioridad  ○ BAJA  ● NORMAL  ○ ALTA         │  │  ← radio group
│  │                                                │  │
│  │  Falla reportada *                             │  │
│  │  [Textarea: Describí el problema...]          │  │
│  │                                                │  │
│  │           [Cancelar]  [Crear Orden →]         │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### Cliente autocomplete

- Input with search icon, dark styled (same as list search)
- On input change (debounce 300ms, distinctUntilChanged):
  - Call `clienteService.listarClientes(value)`
  - Show dropdown with results: client name + email below
  - On selection: set clienteId, load equipos for that client, clear equipo selection
- Use a simple custom dropdown (not mat-autocomplete, to avoid style conflicts):
  - Position absolute below input
  - Background `var(--color-surface)`, border `var(--color-border)`, border-radius `var(--radius-card)`
  - Each result: padding 10px 14px, hover `var(--color-surface-2)`
  - Click selects the client and closes dropdown

### Equipo select

- Disabled until a client is selected
- Populated from `equipoService.listarEquiposDelCliente(clienteId)`
- Shows: `{tipo} · {marca} {modelo}` as option label
- Use dark-styled mat-select with `::ng-deep` overrides OR plain `<select>` dark styled

### Técnico select

- Optional field — show "Sin asignar" as default option
- Populated from the known list of TECNICO users
- Since `GET /api/usuarios` may not exist: load technicians from any available source, or hardcode a `// TODO` comment and leave the select empty with note to wire when endpoint exists

### Prioridad radio group

Three styled radio options: BAJA / NORMAL / ALTA
```scss
.prioridad-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-btn);
  cursor: pointer;
  transition: var(--transition);

  &.selected {
    border-color: var(--color-accent);
    background: rgba(249, 115, 22, 0.08);
  }
}
```
- NORMAL selected by default

### Falla reportada

- Textarea: dark styled, min-height 120px, resize vertical, full width
- Placeholder: "Describí el problema reportado por el cliente..."
- Required — show error "La descripción de la falla es requerida" if submitted empty

### TypeScript — create.component.ts

```typescript
form = fb.group({
  clienteId: [null, Validators.required],
  equipoId:  [null, Validators.required],
  tecnicoId: [null],
  prioridad:  ['NORMAL', Validators.required],
  fallaReportada: ['', Validators.required]
});

clientes: ClienteResponse[] = [];          // search results
selectedCliente: ClienteResponse | null = null;
equipos: EquipoResponse[] = [];            // loads after client selected
showClienteDropdown = false;
isLoading = false;
private searchSubject = new Subject<string>();

ngOnInit():
  searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(q => clienteService.listarClientes(q)),
    takeUntilDestroyed(destroyRef)
  ).subscribe(results => { clientes = results; showClienteDropdown = results.length > 0; });

onClienteSearch(value: string):
  searchSubject.next(value)

onClienteSelect(cliente: ClienteResponse):
  selectedCliente = cliente
  form.patchValue({ clienteId: cliente.id, equipoId: null })
  showClienteDropdown = false
  equipoService.listarEquiposDelCliente(cliente.id).subscribe(e => equipos = e)

onSubmit():
  if form.invalid: markAllAsTouched(); return
  isLoading = true
  ordenesService.crearOrden(form.value).subscribe({
    next: (orden) => router.navigate(['/ordenes', orden.id]),
    error: (err) => {
      isLoading = false
      snackBar.open(err.error?.message || 'Error al crear la orden', 'Cerrar', { duration: 3000 })
    }
  })
```

---

## DIALOG — Add Repuesto Dialog

### Layout

```
┌──────────────────────────────────────────┐
│  Agregar Repuesto                  [✕]   │
├──────────────────────────────────────────┤
│  Buscar repuesto                         │
│  [Pantalla LCD...            🔍]         │
│  ┌─────────────────────────────────────┐ │  ← search results
│  │  Pantalla LCD 15.6    Stock: 9      │ │
│  │  $9.200               Pantallas     │ │
│  └─────────────────────────────────────┘ │
│                                          │
│  ✓ Pantalla LCD 15.6 seleccionada       │
│    Stock disponible: 9 unidades          │
│                                          │
│  Cantidad *                              │
│  [  1  ]  (máx. 9)                      │
│                                          │
│  ⚠ Stock insuficiente  (if error)        │
│                                          │
│         [Cancelar]  [Agregar →]          │
└──────────────────────────────────────────┘
```

### Behavior

- Search input: debounce 300ms, calls `repuestosService.listarRepuestos()` then filters client-side by name — or use `buscarRepuestos(q)` if endpoint supports name filter
- Results dropdown: shows nombre, precio, stockActual, categoria
- Results with `critico: true` show a subtle red warning: "Stock crítico"
- On select: show selected repuesto info, set max for cantidad input to `repuesto.stockActual`
- Cantidad input: type number, min 1, max = stockActual
- On submit:
  - If cantidad > stockActual: show inline error "Stock insuficiente — disponibles: X unidades"
  - Call `ordenesService.agregarRepuesto(ordenId, { repuestoId, cantidad })`
  - On success: `dialogRef.close(result)` — parent reloads orden
  - On 409 error from backend: show inline error with `err.error.message`

### TypeScript — add-repuesto-dialog.component.ts

```typescript
// Inject: MAT_DIALOG_DATA: { ordenId: number }, MatDialogRef, RepuestosService, OrdenesService

repuestosCache: RepuestoResponse[] = [];  // all repuestos loaded on init
filteredRepuestos: RepuestoResponse[] = [];
selectedRepuesto: RepuestoResponse | null = null;
cantidad = 1;
isLoading = false;
stockError = '';
private searchSubject = new Subject<string>();

ngOnInit():
  repuestosService.listarRepuestos().subscribe(r => { repuestosCache = r; })
  searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(destroyRef))
    .subscribe(q => {
      filteredRepuestos = repuestosCache.filter(r =>
        r.nombre.toLowerCase().includes(q.toLowerCase())
      );
    });

onSearch(value: string): searchSubject.next(value)

onSelectRepuesto(repuesto: RepuestoResponse):
  selectedRepuesto = repuesto
  cantidad = 1
  stockError = ''
  filteredRepuestos = []

onSubmit():
  if !selectedRepuesto: return
  if cantidad > selectedRepuesto.stockActual:
    stockError = `Stock insuficiente — disponibles: ${selectedRepuesto.stockActual} unidades`
    return
  isLoading = true
  ordenesService.agregarRepuesto(data.ordenId, { repuestoId: selectedRepuesto.id, cantidad })
    .subscribe({
      next: (orden) => dialogRef.close(orden),
      error: (err) => {
        isLoading = false
        stockError = err.error?.message || 'Error al agregar el repuesto'
      }
    })
```

---

## Routing

**File:** `frontend/src/app/modules/ordenes/ordenes.routes.ts`

```typescript
export const ORDENES_ROUTES: Routes = [
  { path: '',        component: ListComponent   },
  { path: 'kanban',  component: KanbanComponent },
  { path: 'nueva',   component: CreateComponent },
  { path: ':id',     component: DetailComponent },
];
```

**Update `app.routes.ts`:**
```typescript
{
  path: 'ordenes',
  canActivate: [AuthGuard],
  loadChildren: () => import('./modules/ordenes/ordenes.routes').then(m => m.ORDENES_ROUTES)
}
```

**Important:** The route `nueva` must come BEFORE `:id` to avoid Angular matching "nueva" as an ID parameter.

---

## Critical rules

1. **Zero hardcoded hex values** — every color from `var(--color-*)`.
2. **No `.ts` file modifications outside the ordenes module** — do not touch auth, clientes, or usuarios files.
3. **Kanban rollback is mandatory** — if `cambiarEstado` returns 409, revert the array mutation before showing the snackbar.
4. **Route order matters** — `nueva` must be defined before `:id` in `ordenes.routes.ts`.
5. **forkJoin / takeUntilDestroyed** — use on all parallel calls, always handle errors.
6. **Dialog panel class** — always `panelClass: 'dark-dialog'` (already defined in styles.scss from Phase 2).
7. **`GET /api/usuarios` probably doesn't exist** — for the técnico select in create form, use a `// TODO` and leave it empty or use locally known users. Do not fabricate API calls.
8. **CSS budget** — each component `.scss` file must stay under 4 kB. Apply the same compression techniques used in the budget fix step.
9. **Build must pass** — run `ng build --configuration development` and fix all errors before finishing.
10. **Import `DragDropModule`** from `@angular/cdk/drag-drop` in the kanban component imports array — it is already installed.

---

## After completing all files

Run `ng build --configuration development` and confirm zero errors. Then print:

```
PHASE 3 — ÓRDENES MODULE — COMPLETED

SCREEN 1 — /ordenes (List)
  ✅ list.component.ts   — filters, role-based loading (TECNICO vs others), relative time
  ✅ list.component.html — table with estado chips, left border, filter bar
  ✅ list.component.scss — X kB (under 4 kB budget)

SCREEN 2 — /ordenes/kanban (Kanban)
  ✅ kanban.component.ts   — CDK DragDrop, optimistic update, 409 rollback
  ✅ kanban.component.html — 4 columns, cards, FAB, empty states
  ✅ kanban.component.scss — X kB (under 4 kB budget)

SCREEN 3 — /ordenes/:id (Detail)
  ✅ detail.component.ts   — forkJoin load, edit diagnostico, change estado, add repuesto
  ✅ detail.component.html — header card, 2-column layout, timeline, repuestos table
  ✅ detail.component.scss — X kB (under 4 kB budget)

SCREEN 4 — /ordenes/nueva (Create)
  ✅ create.component.ts   — client autocomplete, equipo load, form submit
  ✅ create.component.html — autocomplete dropdown, prioridad radio, falla textarea
  ✅ create.component.scss — X kB (under 4 kB budget)

DIALOG — Add Repuesto
  ✅ add-repuesto-dialog.component.ts   — search, select, stock validation, submit
  ✅ add-repuesto-dialog.component.html — search input, results, cantidad, stock error
  ✅ add-repuesto-dialog.component.scss — X kB (under 4 kB budget)

SERVICES
  ✅ ordenes.service.ts  — all 8 methods wired to real endpoints
  ✅ repuestos.service.ts — listarRepuestos, buscarRepuestos

ROUTING
  ✅ ordenes.routes.ts — 4 routes in correct order (nueva before :id)
  ✅ app.routes.ts     — lazy /ordenes route confirmed

BUILD
  ✅ ng build passes with zero errors
  ✅ All .scss files under 4 kB budget
```