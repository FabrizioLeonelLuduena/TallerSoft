# Frontend — TallerSoft Angular PWA

## Stack y Versiones

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Angular | 17 | Framework frontend (Standalone Components) |
| Angular Material | 17 | Componentes UI (Material Design) |
| Angular CDK | 17 | Drag and Drop (Kanban) |
| ApexCharts | 3.x | Gráficos del Dashboard |
| RxJS | 7.x | Programación reactiva |
| TypeScript | 5.x | Tipado estático |
| Karma + Jasmine | — | Tests unitarios del frontend |

---

## Estructura de Carpetas

```
src/app/
├── app.component.ts           — Componente raíz
├── app.routes.ts              — Rutas principales (lazy loading)
├── core/
│   ├── auth/
│   │   ├── auth.service.ts    — Login, logout, token management
│   │   ├── auth.guard.ts      — Protege rutas que requieren login
│   │   └── role.guard.ts      — Protege rutas por rol
│   ├── interceptors/
│   │   └── jwt.interceptor.ts — Adjunta Authorization header a todos los requests
│   └── services/
│       ├── analytics.service.ts     — Llama al Analytics Service
│       ├── chat-history.service.ts  — Historial de chat del asistente IA
│       ├── profile.service.ts       — Preferencias de perfil del usuario
│       └── usuarios.service.ts      — CRUD de usuarios (admin)
├── modules/
│   ├── auth/                  — Login
│   ├── dashboard/             — KPIs y gráficos
│   ├── ordenes/               — Kanban + lista + detalle + crear
│   ├── clientes/              — Lista, detalle, crear, editar clientes
│   ├── stock/                 — Inventario de repuestos
│   ├── caja/                  — Cobros y caja diaria
│   ├── asistente/             — Chat con IA
│   ├── usuarios/              — Gestión de usuarios (solo ADMIN)
│   ├── inventario/            — Vista de inventario extendida
│   └── reportes/              — Reportes (en desarrollo)
└── shared/
    └── components/
        └── chat-flotante/     — Widget de chat flotante (acceso rápido al asistente)
```

---

## Módulos de la Aplicación

| Módulo | Ruta | Descripción | Roles |
|--------|------|-------------|-------|
| Auth | `/login` | Pantalla de login | Público |
| Dashboard | `/dashboard` | KPIs, gráficos, alertas | Todos |
| Ordenes | `/ordenes` | Kanban y lista de órdenes | Todos |
| Clientes | `/clientes` | CRUD de clientes | ADMIN, RECEPCION |
| Stock | `/stock` | Inventario de repuestos | ADMIN, RECEPCION |
| Caja | `/caja` | Cobros y caja diaria | ADMIN, RECEPCION |
| Asistente | `/asistente` | Chat con Claude AI | Todos |
| Usuarios | `/usuarios` | Gestión de usuarios | ADMIN |

---

## Flujo de Autenticación

```
1. Usuario ingresa email + contraseña en LoginComponent
   │
2. LoginComponent llama authService.login(email, password)
   │
3. AuthService hace POST /auth/login al Gateway
   │
4. Gateway → Core Service valida credenciales y retorna JWT
   │
5. AuthService.setToken(token) → guarda en sessionStorage
   │  (NUNCA en localStorage)
   │
6. currentUserSubject emite el usuario decodificado del JWT
   │
7. Redirección a /dashboard
   │
8. En cada request HTTP:
   │  JwtInterceptor lee el token de sessionStorage
   │  → Agrega header Authorization: Bearer <token>
   │
9. Si el servidor retorna 401:
   │  JwtInterceptor llama authService.logout()
   │  → limpia sessionStorage y redirige a /login
```

---

## Seguridad de Tokens

**¿Por qué `sessionStorage` y no `localStorage`?**

- `sessionStorage` solo persiste durante la pestaña del navegador. Al cerrar la pestaña, el token se elimina automáticamente.
- `localStorage` persiste entre pestañas y sesiones del navegador, lo que amplía la ventana de ataque en caso de XSS.
- Para un sistema de gestión de taller (entorno controlado, mismo dispositivo), la usabilidad de `sessionStorage` es aceptable.

**Regla:** Ningún archivo del frontend debe llamar a `localStorage.setItem` con el token JWT. El historial de chat y las preferencias de perfil pueden usar `localStorage` porque no son datos de seguridad.

---

## AuthGuard

`AuthGuard` implementa `CanActivate` y protege todas las rutas privadas:

```typescript
canActivate(route, state): boolean {
  if (this.authService.isLoggedIn()) {
    return true;  // Tiene token → accede
  }
  this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;   // Sin token → redirige a login
}
```

**`isLoggedIn()`** devuelve `true` si existe un token en `sessionStorage` (no valida la expiración en el cliente — eso lo hace el servidor).

## RoleGuard

`RoleGuard` protege rutas según el rol del usuario autenticado:

```typescript
canActivate(route, state): boolean {
  const requiredRoles = route.data['roles'] as string[];
  const currentRole = this.authService.getCurrentRole();
  
  if (requiredRoles.includes(currentRole)) {
    return true;
  }
  this.router.navigate(['/unauthorized']);
  return false;
}
```

Ejemplo de uso en rutas:
```typescript
{
  path: 'usuarios',
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['ADMIN'] },
  ...
}
```

---

## JwtInterceptor

El interceptor funcional (`HttpInterceptorFn`) intercepta todos los requests HTTP salientes:

1. Llama `authService.getToken()` → lee de `sessionStorage`
2. Si hay token, clona el request y agrega `Authorization: Bearer <token>`
3. Si no hay token, deja pasar el request sin modificar
4. En el `catchError`: si el servidor retorna 401, llama `authService.logout()` y redirige a `/login`

---

## Módulo Kanban

El Kanban muestra las órdenes en 4 columnas: **Pendiente**, **En Proceso**, **Listo**, **Entregado**.

- Implementado con `@angular/cdk/drag-drop`
- Las órdenes se distribuyen en columnas según su `estado`
- Al hacer drag & drop de una card:
  1. La card se mueve optimistamente a la nueva columna
  2. Se llama `ordenesService.cambiarEstado(id, nuevoEstado)`
  3. Si el backend retorna error: la card vuelve a su columna original y se muestra un snackbar de error
- La columna ENTREGADO muestra solo las 3 órdenes más recientes

---

## Módulo Asistente IA

El módulo de asistente IA permite consultar al chatbot en lenguaje natural.

**Flujo del chat:**
1. Usuario escribe una pregunta en el input
2. El componente llama `analyticsService.consultarAsistente(pregunta)`
3. Angular POST `{gateway}/analytics/asistente/consulta`
4. El Analytics Service responde con `{ respuesta: "...", contexto_utilizado: {...} }`
5. La respuesta se muestra en el chat y se guarda en `chat-history.service.ts` (localStorage)

El historial de chat persiste entre sesiones de la pestaña.

---

## Dashboard — KPIs con ApexCharts

El Dashboard muestra los siguientes indicadores en tiempo real (consultando el Analytics Service):

- Órdenes por estado (barras)
- Evolución mensual de ingresos (área)
- Rendimiento de técnicos (barras)
- Stock crítico (tabla de alertas)
- Resumen de caja del día (totales por medio de pago)
- Alertas de órdenes sin movimiento

---

## Configuración de Entornos

**`environment.ts`** (desarrollo):
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

**`environment.prod.ts`** (producción):
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://tu-dominio.com'
};
```

---

## Cómo Correr el Proyecto

```bash
cd frontend
npm install
ng serve
# Acceder en http://localhost:4200
```

## Cómo Correr los Tests

```bash
cd frontend
ng test                          # Modo watch (desarrollo)
ng test --watch=false --browsers=ChromeHeadless  # CI / una sola ejecución
ng test --code-coverage          # Genera reporte de cobertura en coverage/
```
