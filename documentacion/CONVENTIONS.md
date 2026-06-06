# TallerSoft Development Conventions

This document defines the coding standards, naming conventions, and best practices for the TallerSoft project.

## Table of Contents

1. [Git Workflow](#git-workflow)
2. [Commit Messages](#commit-messages)
3. [Backend Conventions](#backend-conventions)
4. [Frontend Conventions](#frontend-conventions)
5. [Python/Analytics Conventions](#pythonanalytics-conventions)
6. [Code Style](#code-style)
7. [File Organization](#file-organization)

---

## Git Workflow

### Branch Naming

Follow these naming conventions for branches:

```
main               → Production release branch (protected, reviewed)
develop            → Integration branch (staging, reviewed)
feature/FEATURE-*  → New features (e.g., feature/auth-jwt, feature/payment-integration)
fix/BUG-*          → Bug fixes (e.g., fix/incorrect-stock-calculation)
docs/*             → Documentation (e.g., docs/api-guide)
refactor/*         → Code refactoring (e.g., refactor/service-layer)
test/*             → Tests (e.g., test/order-service)
```

### Workflow

```
1. Create feature branch from develop:
   git checkout -b feature/new-feature develop

2. Make commits with conventional messages (see below)
   git commit -m "feat(ordenes): add order status change validation"

3. Push to remote:
   git push origin feature/new-feature

4. Create Pull Request to develop
   - Requires 1 code review
   - All CI checks must pass

5. After merge to develop, create PR to main for release
   - Requires 2 code reviews
   - Must pass all tests
```

---

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

### Types

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that don't affect code meaning (formatting, semicolons, etc.)
- **refactor**: Code change that neither fixes a bug nor adds a feature
- **perf**: Code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **chore**: Changes to build process, dependencies, tooling
- **ci**: Changes to CI/CD configuration
- **build**: Changes to build system or external dependencies

### Scopes

Scopes should indicate the affected feature:

**Backend:** `auth`, `usuarios`, `clientes`, `equipos`, `ordenes`, `stock`, `caja`, `pagos`, `db`, `config`, `security`

**Frontend:** `auth`, `dashboard`, `ordenes`, `clientes`, `stock`, `caja`, `asistente`, `shared`, `routing`

**Analytics:** `ordenes`, `stock`, `caja`, `asistente`, `db`

### Examples

```bash
# New feature
git commit -m "feat(ordenes): implement Kanban board with drag-and-drop"

# Bug fix
git commit -m "fix(stock): prevent negative inventory on order cancellation"

# Documentation
git commit -m "docs(readme): add deployment section"

# Code cleanup
git commit -m "refactor(auth): extract JWT validation to utility class"

# Test improvement
git commit -m "test(cobros): add webhook signature validation tests"

# Dependencies
git commit -m "chore: upgrade Spring Boot to 3.2.5"

# With body and footer
git commit -m "feat(pagos): integrate MercadoPago payment gateway

- Add MercadoPagoService for link generation
- Implement webhook endpoint for payment confirmation
- Store payment ID for order tracking

Closes #42"
```

---

## Backend Conventions

### Java Package Structure

```
com.tallersoft.
├── TallerSoftApplication.java      (entry point)
├── config/                          (Spring configurations)
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── AppConfig.java
├── controller/                      (REST endpoints)
│   ├── AuthController.java
│   ├── ClienteController.java
│   ├── OrdenTrabajoController.java
│   └── ...
├── service/                         (business logic)
│   ├── AuthService.java
│   ├── ClienteService.java
│   ├── OrdenTrabajoService.java
│   └── ...
├── repository/                      (data access)
│   ├── UsuarioRepository.java
│   ├── ClienteRepository.java
│   └── ...
├── model/                           (JPA entities)
│   ├── Usuario.java
│   ├── Cliente.java
│   ├── OrdenTrabajo.java
│   └── ...
├── dto/                             (data transfer objects)
│   ├── ClienteRequest.java
│   ├── ClienteResponse.java
│   ├── OrdenTrabajoRequest.java
│   ├── OrdenTrabajoResponse.java
│   └── ...
├── mapper/                          (MapStruct mappers)
│   ├── ClienteMapper.java
│   ├── OrdenTrabajoMapper.java
│   └── ...
├── security/                        (JWT, auth handlers)
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── CurrentUser.java
└── exception/                       (custom exceptions)
    ├── GlobalExceptionHandler.java
    ├── InvalidStateTransitionException.java
    └── ...
```

### Entity Naming

- Entity class names should be singular nouns: `Usuario`, `Cliente`, `Equipo`, `OrdenTrabajo`, `Cobro`
- Repository names: `UsuarioRepository`, `ClienteRepository`
- Service names: `UsuarioService`, `ClienteService`
- Controller names: `UsuarioController`, `ClienteController`

### DTO Naming

**CRITICAL:** Always create separate Request and Response DTOs:

```java
// Request DTO - for incoming data (POST/PUT)
public class ClienteRequest {
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;
}

// Response DTO - for outgoing data
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDateTime createdAt;
}
```

### MapStruct Mapper Usage

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    ClienteResponse toResponse(Cliente entity);
    
    Cliente toEntity(ClienteRequest dto);
    
    List<ClienteResponse> toResponseList(List<Cliente> entities);
}
```

### Service Layer Patterns

```java
@Service
public class ClienteService {
    
    private final ClienteRepository repository;
    private final ClienteMapper mapper;
    
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        Cliente entity = mapper.toEntity(request);
        entity.setActivo(true);
        Cliente saved = repository.save(entity);
        return mapper.toResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long id) {
        Cliente entity = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        return mapper.toResponse(entity);
    }
}
```

### REST Controller Patterns

```java
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    
    private final ClienteService service;
    private final ClienteMapper mapper;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<List<ClienteResponse>> listar(
        @RequestParam(required = false) String nombre
    ) {
        List<ClienteResponse> clientes = service.listarClientes(nombre);
        return ResponseEntity.ok(clientes);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<ClienteResponse> crear(
        @Valid @RequestBody ClienteRequest request
    ) {
        ClienteResponse response = service.crearCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
        InvalidStateTransitionException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("CONFLICT", ex.getMessage()));
    }
}
```

---

## Frontend Conventions

### TypeScript/Angular File Structure

```
src/app/
├── app.component.ts
├── app.routes.ts
├── core/
│   ├── auth/
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   ├── role.guard.ts
│   │   └── current-user.directive.ts
│   ├── interceptors/
│   │   └── jwt.interceptor.ts
│   └── services/
│       ├── http.service.ts
│       └── notification.service.ts
├── shared/
│   ├── components/
│   │   ├── header/
│   │   ├── sidebar/
│   │   ├── kpi-card/
│   │   └── ...
│   ├── pipes/
│   │   └── search.pipe.ts
│   └── directives/
│       └── ...
└── modules/
    ├── auth/
    │   ├── login/
    │   ├── register/
    │   └── auth.routes.ts
    ├── dashboard/
    │   ├── dashboard.component.ts
    │   ├── services/
    │   └── dashboard.routes.ts
    ├── ordenes/
    ├── clientes/
    ├── stock/
    ├── caja/
    └── asistente/
```

### Naming Conventions

- **Services:** `NombreService` (e.g., `ClienteService`, `OrdenesService`)
- **Components:** `NombreComponent` (e.g., `LoginComponent`, `KanbanComponent`)
- **Guards:** `NombreGuard` (e.g., `AuthGuard`, `RoleGuard`)
- **Interceptors:** `NombreInterceptor` (e.g., `JwtInterceptor`)
- **Pipes:** `NombrePipe` (e.g., `SearchPipe`, `CurrencyFormatPipe`)

### Service Example

```typescript
@Injectable({
    providedIn: 'root'
})
export class ClienteService {
    
    private apiUrl = '/api/clientes';
    
    constructor(private http: HttpClient) {}
    
    listar(filtros?: any): Observable<ClienteResponse[]> {
        return this.http.get<ClienteResponse[]>(this.apiUrl, {
            params: filtros
        });
    }
    
    obtener(id: number): Observable<ClienteResponse> {
        return this.http.get<ClienteResponse>(`${this.apiUrl}/${id}`);
    }
    
    crear(data: ClienteRequest): Observable<ClienteResponse> {
        return this.http.post<ClienteResponse>(this.apiUrl, data);
    }
}
```

### Component Example

```typescript
@Component({
    selector: 'app-cliente-list',
    templateUrl: './cliente-list.component.html',
    styleUrls: ['./cliente-list.component.scss']
})
export class ClienteListComponent implements OnInit, OnDestroy {
    
    clientes$: Observable<ClienteResponse[]>;
    private destroy$ = new Subject<void>();
    
    constructor(private service: ClienteService) {}
    
    ngOnInit(): void {
        this.cargarClientes();
    }
    
    private cargarClientes(): void {
        this.clientes$ = this.service.listar().pipe(
            takeUntil(this.destroy$)
        );
    }
    
    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }
}
```

---

## Python/Analytics Conventions

### FastAPI File Structure

```
analytics/
├── app/
│   ├── __init__.py
│   ├── main.py                  (FastAPI app)
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── ordenes.py
│   │   ├── stock.py
│   │   ├── caja.py
│   │   └── asistente.py
│   ├── services/
│   │   ├── __init__.py
│   │   ├── analytics_service.py
│   │   └── claude_service.py
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── ordenes.py
│   │   ├── stock.py
│   │   └── caja.py
│   └── db/
│       ├── __init__.py
│       └── database.py
├── requirements.txt
├── Dockerfile
└── .env
```

### Naming Conventions

- **Router files:** snake_case (e.g., `ordenes.py`, `stock.py`)
- **Router variable:** `router` in each file
- **Function names:** snake_case (e.g., `obtener_ordenes()`, `get_resumen()`)
- **Class names:** PascalCase (e.g., `ResumenOrdenes`, `RepuestoCritico`)

### Router Example

```python
from fastapi import APIRouter
from app.db.database import get_db

router = APIRouter()

@router.get("/resumen")
async def resumen_ordenes(db: Session = Depends(get_db)):
    """Get order summary by status"""
    # Implementation
    return {"pending": 5, "in_process": 3}
```

---

## Code Style

### Java Code Style

- **Indentation:** 4 spaces
- **Line length:** Max 120 characters
- **Naming:**
  - Classes: PascalCase
  - Methods/variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Packages: lowercase.with.dots

### TypeScript/JavaScript Code Style

- **Indentation:** 2 spaces
- **Semicolons:** Always use
- **Line length:** Max 100 characters
- **Quotes:** Single quotes preferred
- **Naming:**
  - Classes: PascalCase
  - Functions/variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Files: kebab-case (e.g., `cliente-list.component.ts`)

### Python Code Style

- **Indentation:** 4 spaces
- **Line length:** Max 100 characters
- **Naming:**
  - Classes: PascalCase
  - Functions/variables: snake_case
  - Constants: UPPER_SNAKE_CASE
  - Files: snake_case

---

## File Organization

### One Class Per File

Each class should be in its own file:

```
- ClienteService.java (not ClienteService.java and ClienteRepository.java)
- ClienteRepository.java
- ClienteRequest.java (DTO)
- ClienteResponse.java (DTO)
```

### Barrel Exports (Angular)

Use `index.ts` for barrel exports:

```typescript
// src/app/modules/clientes/services/index.ts
export * from './cliente.service';
export * from './equipo.service';

// Usage
import { ClienteService } from 'app/modules/clientes/services';
```

---

## Checklist Before Committing

- [ ] Code follows naming conventions
- [ ] No hardcoded values (use environment variables)
- [ ] Error handling implemented
- [ ] Logging added for debugging
- [ ] Unit tests written (if applicable)
- [ ] Code formatted properly (indentation, line length)
- [ ] No console.log(), System.out.println(), or print() statements
- [ ] Comments added for complex logic
- [ ] Commit message follows Conventional Commits
- [ ] Branch name follows naming convention

---

*Last Updated: May 15, 2026*
