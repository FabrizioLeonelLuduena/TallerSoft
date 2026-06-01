# MÓDULO 2: CLIENTES/EQUIPOS Y AUTENTICACIÓN/USUARIOS - DISEÑO TÉCNICO

**Versión:** 1.0  
**Fecha:** Junio 2026  
**Objetivo:** Documentación de referencia para patrones y convenciones implementados en módulos completados

---

## INTRODUCCIÓN

Este documento describe en detalle la arquitectura, estructura, convenciones y patrones de implementación utilizados en los módulos de **Autenticación/Usuarios** y **Clientes/Equipos** del sistema TallerSoft. Estos módulos completados servirán como referencia para desarrollar futuros módulos, especialmente el **Módulo 4 (Stock de Repuestos)**.

El documento es **exhaustivamente específico**: documenta exactamente lo que está implementado en el código, no lo que "debería" estar. Cada sección incluye nombres reales de clases, métodos, caminos de URL, y ejemplos específicos del proyecto.

---

## ÍNDICE

1. [Módulo de Autenticación y Usuarios](#módulo-de-autenticación-y-usuarios)
2. [Módulo de Clientes y Equipos](#módulo-de-clientes-y-equipos)
3. [Patrones y Convenciones Observadas](#patrones-y-convenciones-observadas)

---

# MÓDULO DE AUTENTICACIÓN Y USUARIOS

## BACKEND (Spring Boot)

### Entidades JPA

#### `Usuario` (`com.tallersoft.model.Usuario`)

**Tabla:** `usuarios`  
**Características:**
- Implementa `UserDetails` para integración con Spring Security
- Gestión de seguridad integrada en la entidad

**Campos y Anotaciones:**

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                           // PK auto-generada

@Column(name = "nombre", nullable = false, length = 100)
private String nombre;                     // Nombre completo del usuario (requerido)

@Column(name = "email", nullable = false, length = 150, unique = true)
private String email;                      // Email único (requerido)

@Column(name = "password", nullable = false, length = 255)
private String password;                   // Contraseña hasheada (requerido)

@Enumerated(EnumType.STRING)
@Column(name = "rol", nullable = false, length = 20)
private Rol rol;                           // Enum: ADMIN, TECNICO, RECEPCION (requerido)

@Column(name = "activo", nullable = false)
private boolean activo;                    // Estado activo (por defecto true)

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;           // Timestamp de creación (auto-generado)

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();       // Auto-set al crear
}
```

**Métodos de UserDetails implementados:**
- `getAuthorities()`: Retorna colección de `SimpleGrantedAuthority` con rol prefijado "ROLE_"
- `getPassword()`: Retorna password hasheada
- `getUsername()`: Retorna email (usado como identificador)
- `isAccountNonExpired()`: Retorna `true` (sin expiración)
- `isAccountNonLocked()`: Retorna `true` (nunca bloqueado)
- `isCredentialsNonExpired()`: Retorna `true` (sin expiración)
- `isEnabled()`: Retorna valor de `activo` (considera estado para login)

#### `Rol` (Enumeración)

```java
public enum Rol {
    ADMIN("ADMIN"),
    TECNICO("TECNICO"),
    RECEPCION("RECEPCION");
    
    // Método para convertir de String a Enum
    public static Rol fromString(String value) throws IllegalArgumentException
}
```

### DTOs

#### `AuthRequest`
**Usado en:** `POST /auth/login`

```java
@Data
public class AuthRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
```

#### `AuthResponse`
**Retornado por:** `POST /auth/login`, `POST /auth/register`

```java
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;          // JWT token (puede dura hasta 24h por defecto)
    private Long userId;           // ID del usuario autenticado
    private String email;          // Email del usuario
    private String rol;            // Rol del usuario (ej: "ADMIN")
}
```

#### `UsuarioRequest`
**Usado en:** `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `POST /auth/register`

```java
@Data
public class UsuarioRequest {
    @NotBlank(message = "El nombre es requerido")
    private String nombre;         // Nombre completo (requerido)
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;          // Email (requerido, único)
    
    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "...")
    private String password;       // Contraseña (mín. 8 caracteres)
    
    @NotNull(message = "El rol es requerido")
    private String rol;            // Rol en String: "ADMIN", "TECNICO", "RECEPCION"
}
```

#### `UsuarioResponse`
**Retornado en:** Listados, obtención, creación, edición de usuarios

```java
@Data
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private String rol;            // String (ej: "ADMIN")
    private boolean activo;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
```

### Mappers MapStruct

#### `UsuarioMapper`

```java
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    
    Usuario toEntity(UsuarioRequest request);
    // Convierte DTO request a entidad Usuario
    
    UsuarioResponse toResponse(Usuario entity);
    // Convierte entidad Usuario a DTO response
    
    List<UsuarioResponse> toResponseList(List<Usuario> entities);
    // Convierte lista de entidades a lista de responses
}
```

**Configuración:** `componentModel = "spring"` genera bean de Spring automáticamente

### Services

#### `UsuarioService` (`com.tallersoft.service.UsuarioService`)

```java
@Slf4j
@Service
public class UsuarioService {
    
    /**
     * Crear nuevo usuario (solo ADMIN)
     * - Valida email único
     * - Hashea contraseña con PasswordEncoder
     * - Convierte rol de String a enum Rol
     * - Log de creación
     */
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request)
        throws IllegalArgumentException, EntityNotFoundException;
    
    /**
     * Obtener usuario por ID
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuario(Long id)
        throws EntityNotFoundException;
    
    /**
     * Listar todos los usuarios activos
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios();
    
    /**
     * Listar usuarios filtrados por rol
     * @param rol String: "ADMIN", "TECNICO" o "RECEPCION"
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorRol(String rol)
        throws IllegalArgumentException;
    
    /**
     * Actualizar usuario existente
     * - Valida email único (si cambió)
     * - Solo actualiza password si se proporciona
     * - Valida rol si se proporciona
     */
    @Transactional
    public UsuarioResponse editarUsuario(Long id, UsuarioRequest request)
        throws EntityNotFoundException, IllegalArgumentException;
    
    /**
     * Eliminar usuario (hard delete)
     */
    @Transactional
    public void desactivarUsuario(Long id)
        throws EntityNotFoundException;
    
    /**
     * Activar usuario (sets activo=true)
     */
    @Transactional
    public void activarUsuario(Long id)
        throws EntityNotFoundException;
    
    /**
     * Obtener usuario por email (para autenticación)
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorEmail(String email)
        throws EntityNotFoundException;
}
```

#### `AuthService` (`com.tallersoft.service.AuthService`)

```java
@Slf4j
@Service
public class AuthService {
    
    /**
     * Login: valida credenciales y genera JWT token
     * - Busca usuario por email
     * - Valida contraseña con PasswordEncoder.matches()
     * - Verifica que usuario esté activo
     * - Genera token JWT con userId, email, rol
     * @throws InvalidCredentialsException
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request)
        throws InvalidCredentialsException;
    
    /**
     * Registrar nuevo usuario
     * - Valida email único
     * - Hashea contraseña
     * - Usa rol del request
     * @throws IllegalArgumentException si email existe
     */
    @Transactional
    public UsuarioResponse register(UsuarioRequest request)
        throws IllegalArgumentException;
}
```

### Controllers

#### `AuthController` (`com.tallersoft.controller.AuthController`)

**Ruta base:** `/auth`  
**Rutas públicas** (sin autenticación)

```java
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    /**
     * POST /auth/login
     * Body: AuthRequest { email, password }
     * Response: AuthResponse { token, userId, email, rol }
     * Status: 200 OK
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody AuthRequest request
    );
    
    /**
     * POST /auth/register
     * Body: UsuarioRequest { nombre, email, password, rol }
     * Response: UsuarioResponse
     * Status: 201 CREATED
     * Public endpoint - permite auto-registro
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(
        @Valid @RequestBody UsuarioRequest request
    );
}
```

#### `UsuariosController` (`com.tallersoft.controller.UsuariosController`)

**Ruta base:** `/api/usuarios`  
**Todas las rutas requieren:** Autenticación JWT + Rol ADMIN

```java
@Slf4j
@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {
    
    /**
     * GET /api/usuarios
     * @param rol (opcional) filtrar por rol: "ADMIN", "TECNICO", "RECEPCION"
     * Response: List<UsuarioResponse>
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
        @RequestParam(required = false) String rol
    );
    
    /**
     * GET /api/usuarios/{id}
     * Response: UsuarioResponse
     * Status: 200 OK
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id);
    
    /**
     * POST /api/usuarios
     * Body: UsuarioRequest
     * Response: UsuarioResponse
     * Status: 201 CREATED
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(
        @Valid @RequestBody UsuarioRequest request
    );
    
    /**
     * PUT /api/usuarios/{id}
     * Body: UsuarioRequest (parcial o completo)
     * Response: UsuarioResponse
     * Status: 200 OK
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> editar(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioRequest request
    );
    
    /**
     * DELETE /api/usuarios/{id}
     * Response: empty (204)
     * Status: 204 NO_CONTENT
     * @PreAuthorize("hasRole('ADMIN')")
     * Nota: Hard delete (elimina de BD)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id);
    
    /**
     * PATCH /api/usuarios/{id}/activar
     * Response: UsuarioResponse
     * Status: 200 OK
     * @PreAuthorize("hasRole('ADMIN')")
     */
    @PatchMapping("/{id}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id);
}
```

### Repositories

#### `UsuarioRepository`

```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);
    // Query generada: SELECT * FROM usuarios WHERE email = ?
    
    List<Usuario> findByRol(Rol rol);
    // Query generada: SELECT * FROM usuarios WHERE rol = ?
    
    List<Usuario> findByRolAndActivoTrue(Rol rol);
    // Unused en código actual, pero disponible
    
    boolean existsByEmail(String email);
    // Query generada: SELECT EXISTS(SELECT 1 FROM usuarios WHERE email = ?)
}
```

### Excepciones Custom

#### `InvalidCredentialsException`

```java
// Lanzada por AuthService.login() cuando:
// - Email no existe
// - Contraseña no coincide
// - Usuario está inactivo
// HTTP Status manejado por GlobalExceptionHandler: 401 UNAUTHORIZED
```

### Seguridad

#### `JwtUtil` (`com.tallersoft.security.JwtUtil`)

```java
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:clave_secreta_minimo_256_bits_...}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")  // 24h por defecto
    private long jwtExpiration;
    
    /**
     * Generar JWT token
     * Claims: userId (Long), email (String), rol (String)
     * Subject: email
     * Algorithm: HS256
     * Expiration: configurable (86400000ms = 24h)
     */
    public String generateToken(Long userId, String email, String rol);
    
    /**
     * Validar token JWT
     */
    public boolean validateToken(String token);
    
    /**
     * Extraer userId del token
     */
    public Long extractUserId(String token);
    
    /**
     * Extraer email del token
     */
    public String extractEmail(String token);
    
    /**
     * Extraer rol del token
     */
    public String extractRole(String token);
}
```

#### `JwtAuthenticationFilter` (`com.tallersoft.security.JwtAuthenticationFilter`)

```java
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    /**
     * Se ejecuta UNA VEZ por request
     * 1. Extrae token de header "Authorization: Bearer <token>"
     * 2. Valida token con JwtUtil
     * 3. Extrae email y rol
     * 4. Carga UserDetails con UserDetailsService
     * 5. Crea UsernamePasswordAuthenticationToken
     * 6. Lo establece en SecurityContext
     * 7. Continúa con filterChain
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException;
}
```

#### `SecurityConfig` (`com.tallersoft.config.SecurityConfig`)

```java
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    // Password encoder: BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() -> new BCryptPasswordEncoder();
    
    // Authentication provider: DAO-based
    @Bean
    public DaoAuthenticationProvider authenticationProvider();
    
    // CORS Configuration:
    // - Allowed origins: localhost:4200, localhost:4201, localhost:3000, localhost:8080
    // - Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
    // - Allow credentials: true
    // - Max age: 3600s
    
    // Security Filter Chain:
    // - CSRF disabled
    // - CORS enabled
    // - Session: STATELESS (no sessions, JWT-based)
    // - Public endpoints:
    //   - /auth/login
    //   - /auth/register
    //   - /api/usuarios/debug/**
    //   - /api/pagos/webhook
    //   - /swagger-ui/**, /v3/api-docs/**
    //   - /actuator/**
    // - Otros endpoints: requieren autenticación
    // - Filter orden: JwtAuthenticationFilter antes de UsernamePasswordAuthenticationFilter
}
```

#### `GlobalExceptionHandler` (`com.tallersoft.exception.GlobalExceptionHandler`)

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // InvalidCredentialsException -> 401 UNAUTHORIZED
    // EntityNotFoundException -> 404 NOT_FOUND
    // InvalidStateTransitionException -> 409 CONFLICT
    // InsufficientStockException -> 409 CONFLICT
    // MissingDiagnosticException -> 400 BAD_REQUEST
    // AccessDeniedException -> 403 FORBIDDEN (rol insuficiente)
    // MethodArgumentNotValidException -> 400 BAD_REQUEST (validación de campos)
    // Exception (genérica) -> 500 INTERNAL_SERVER_ERROR
    
    // Retorna ErrorResponse { status, error, message }
}
```

---

## FRONTEND (Angular)

### Estructura de Carpetas

```
frontend/src/app/
├── modules/
│   ├── auth/
│   │   ├── login/
│   │   │   ├── login.component.ts
│   │   │   ├── login.component.html
│   │   │   └── login.component.scss
│   │   └── auth.routes.ts
│   └── usuarios/
│       ├── list/
│       │   ├── list.component.ts
│       │   ├── list.component.html
│       │   └── list.component.scss
│       ├── create/
│       │   ├── create.component.ts
│       │   ├── create.component.html
│       │   └── create.component.scss
│       ├── dialogs/
│       │   └── create-user-dialog/
│       │       ├── create-user-dialog.component.ts
│       │       ├── create-user-dialog.component.html
│       │       └── create-user-dialog.component.scss
│       ├── services/
│       │   └── usuario.service.ts
│       └── usuarios.routes.ts
├── core/
│   ├── auth/
│   │   ├── auth.service.ts
│   │   ├── auth.guard.ts
│   │   └── role.guard.ts
│   └── interceptors/
│       └── jwt.interceptor.ts
├── shared/
│   ├── dialogs/
│   │   └── confirm-dialog.component.ts
│   └── layout/
│       ├── layout.component.ts
│       ├── layout.component.html
│       └── layout.component.scss
└── app.routes.ts
```

### Componentes

#### `LoginComponent` (`modules/auth/login/login.component.ts`)

**Selector:** `app-login`  
**Standalone:** `true`

**Funcionalidad:**
- Formulario ReactiveForm con email y password
- Validaciones: email requerido y válido, password requerido (mín. 8 caracteres)
- Llama a `AuthService.login()` en submit
- Guarda token en sessionStorage automáticamente (vía AuthService)
- Redirige a `/dashboard` en éxito
- Muestra error genérico "Email o contraseña incorrectos"
- Redirige a dashboard si ya está logueado (ngOnInit)

**Imports Material:**
- MatFormFieldModule
- MatInputModule
- MatButtonModule
- MatIconModule
- MatCardModule

**Métodos públicos:**
```typescript
ngOnInit(): void
onSubmit(): void
togglePasswordVisibility(): void
get email: FormControl
get password: FormControl
```

#### `UsuariosComponent` (`modules/usuarios/usuarios.component.ts`)

**Selector:** `app-usuarios`  
**Standalone:** `true`

**Funcionalidad:**
- Componente contenedor simple
- Solo importa y renderiza `ListComponent`
- Template: `<app-usuarios-list></app-usuarios-list>`

#### `ListComponent` (Usuarios) (`modules/usuarios/list/list.component.ts`)

**Selector:** `app-usuarios-list`  
**Standalone:** `true`

**Funcionalidad:**
- Carga lista de usuarios via `UsuarioService.listarUsuarios()`
- Verifica rol actual: solo ADMIN puede ver esta página
- Renderiza tabla de usuarios con columnas: nombre, email, rol, fecha creación, estado
- Filtro por búsqueda: aún no implementado (TODO)
- Botón "Nuevo Usuario" navega a `/usuarios/nuevo`
- Botón eliminar: abre modal de confirmación, luego llama a `deleteUsuario(id)`
- Maneja loading state
- Muestra errores en snackbar

**Métodos públicos:**
```typescript
ngOnInit(): void
loadUsuarios(): void              // Carga datos
onSearch(event: Event): void      // Busca usuarios (TODO)
onCreateClick(): void             // Navega a crear
openDeleteUserModal(usuario): void
confirmDeleteUser(): void         // Elimina usuario
cancelDeleteUser(): void
getInitials(nombre): string       // Primer carácter
formatDate(dateStr): string       // Formato es-AR
getRolBadgeClass(rol): string     // CSS class para badge
getRolDisplayText(rol): string    // "ADMIN" -> "ADMIN", "TECNICO" -> "TÉCNICO"
```

**Propiedades:**
```typescript
usuarios: UsuarioResponse[] = []
isLoading = true
currentRole: string | null
searchTerm = ''
showDeleteUserModal = false
userToDelete: any = null
```

#### `CreateComponent` (Usuarios) (`modules/usuarios/create/create.component.ts`)

**Selector:** `app-usuarios-create`  
**Standalone:** `true`

**Funcionalidad:**
- Formulario ReactiveForm para crear nuevo usuario
- Campos: nombre, email, password, rol (selector de 3 botones)
- Validaciones: nombre requerido, email requerido+válido, password requerido (mín. 8)
- Password visible/invisible (toggle icon)
- Selección de rol: ADMIN, TECNICO, RECEPCION (3 botones visuales)
- Envía a `UsuarioService.crearUsuario()`
- Redirige a `/usuarios` en éxito
- Maneja errores: si es "email existe", marca control con error
- Loading state desactiva formulario y botones

**Métodos públicos:**
```typescript
selectRole(rol: string): void
togglePassword(): void
onCancel(): void
onSubmit(): void
get nombreControl: FormControl
get emailControl: FormControl
get passwordControl: FormControl
get rolControl: FormControl
```

**Propiedades:**
```typescript
isLoading = false
passwordVisible = false
selectedRole = 'TECNICO'
form: FormGroup
```

### Servicios

#### `UsuarioService` (`modules/usuarios/services/usuario.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class UsuarioService {
    private apiUrl = `${environment.apiUrl}/api/usuarios`;
    private authApi = `${environment.apiUrl}/auth`;
    
    /**
     * Obtener lista de todos los usuarios
     * @param rol (opcional) filtrar por rol
     * @returns Observable<UsuarioResponse[]>
     * GET /api/usuarios?rol=ADMIN (si rol proporcionado)
     */
    listarUsuarios(rol?: string): Observable<UsuarioResponse[]>;
    
    /**
     * Crear nuevo usuario
     * Usa endpoint /auth/register (NO /api/usuarios POST)
     * @param data UsuarioRequest
     * @returns Observable<UsuarioResponse>
     * POST /auth/register
     */
    crearUsuario(data: UsuarioRequest): Observable<UsuarioResponse>;
    
    /**
     * Eliminar usuario por ID
     * DELETE /api/usuarios/{usuarioId}
     */
    deleteUsuario(usuarioId: number): Observable<any>;
}
```

**Interfaces:**
```typescript
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
```

### Auth & Security

#### `AuthService` (`core/auth/auth.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
    
    /**
     * Login con email y contraseña
     * - Llama POST /auth/login
     * - Recibe AuthResponse con token
     * - Guarda token en sessionStorage automáticamente
     * - Emite cambio en currentUser$ BehaviorSubject
     * @returns Observable<LoginResponse>
     */
    login(email: string, password: string): Observable<LoginResponse>;
    
    /**
     * Logout
     * - Elimina token de sessionStorage
     * - Emite null en currentUser$ BehaviorSubject
     */
    logout(): void;
    
    /**
     * Guardar token en sessionStorage (nunca localStorage)
     */
    setToken(token: string): void;
    
    /**
     * Obtener token de sessionStorage
     */
    getToken(): string | null;
    
    /**
     * Verificar si usuario está logueado
     * @returns boolean (true si token existe)
     */
    isLoggedIn(): boolean;
    
    /**
     * Obtener usuario actual decodificando JWT
     * @returns CurrentUser | null { userId, email, rol }
     */
    getCurrentUser(): CurrentUser | null;
    
    /**
     * Obtener rol del usuario actual
     * @returns string | null (ej: "ADMIN")
     */
    getCurrentRole(): string | null;
}

export interface LoginResponse {
    token: string;
    userId: number;
    email: string;
    rol: string;
}

export interface CurrentUser {
    userId: number;
    email: string;
    rol: string;
}
```

**Detalles de implementación:**
- Token se guarda en **sessionStorage** (no localStorage)
- Token se decodifica manualmente (base64 decode del payload)
- JWT payload contiene: userId, email, rol, iat, exp
- currentUser$ es BehaviorSubject para reactividad

#### `AuthGuard` (`core/auth/auth.guard.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
    
    /**
     * Verifica si usuario está autenticado
     * - Usa AuthService.isLoggedIn()
     * - Si es true: permite acceso
     * - Si es false: redirige a /login con queryParam returnUrl
     */
    canActivate(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): boolean;
}
```

#### `RoleGuard` (`core/auth/role.guard.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
    
    /**
     * Verifica si usuario tiene rol requerido
     * - Lee route.data['roles'] (array de roles permitidos)
     * - Obtiene rol actual con AuthService.getCurrentRole()
     * - Si está incluido: permite acceso
     * - Si no: redirige a /unauthorized
     * 
     * Uso en rutas:
     * {
     *   path: 'admin',
     *   component: AdminComponent,
     *   canActivate: [RoleGuard],
     *   data: { roles: ['ADMIN'] }
     * }
     */
    canActivate(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): boolean;
}
```

**NOTA:** En el código actual, RoleGuard no está siendo usado. Los roles se verifican con `@PreAuthorize` en backend.

#### `JwtInterceptor` (`core/interceptors/jwt.interceptor.ts`)

```typescript
/**
 * HTTP Interceptor Funcional (Angular 17+)
 * Se ejecuta en TODOS los requests HTTP
 * 
 * 1. Obtiene token de AuthService.getToken()
 * 2. Si existe: clona el request y agrega header "Authorization: Bearer <token>"
 * 3. Pasar al siguiente handler (next)
 * 4. Captura errores HTTP 401: logout y redirige a /login
 */
export const jwtInterceptor: HttpInterceptorFn = (
    request: HttpRequest<unknown>,
    next: HttpHandlerFn
): Observable<HttpEvent<unknown>>;

// Registrado en app.config.ts con HTTP_INTERCEPTORS provider
```

### Rutas

#### `auth.routes.ts`

```typescript
export const authRoutes: Routes = [
    { path: '', component: LoginComponent },
    { path: 'login', component: LoginComponent }
];

// Integración en app.routes.ts:
// {
//   path: 'login',
//   loadChildren: () => import('./modules/auth/auth.routes')
//     .then(m => m.authRoutes)
// }
```

#### `usuarios.routes.ts`

```typescript
export const USUARIOS_ROUTES: Routes = [
    { path: '', component: ListComponent },
    { path: 'nuevo', component: CreateComponent }
];

// Integración en app.routes.ts:
// {
//   path: 'usuarios',
//   loadChildren: () => import('./modules/usuarios/usuarios.routes')
//     .then(m => m.USUARIOS_ROUTES),
//   canActivate: [AuthGuard]
// }
```

### Componentes Compartidos

#### `ConfirmDialogComponent` (`shared/dialogs/confirm-dialog.component.ts`)

**Selector:** `app-confirm-dialog`  
**Standalone:** `true`

**Funcionalidad:**
- Dialog Material reutilizable para confirmaciones
- Inyecta `MAT_DIALOG_DATA` con configuración
- Retorna `true` si confirma, `false` si cancela

**Interfaces:**
```typescript
export interface ConfirmDialogData {
    title: string;
    message: string;
    confirmText?: string;      // Defecto: "Confirmar"
    cancelText?: string;       // Defecto: "Cancelar"
}
```

**Métodos:**
```typescript
onCancel(): void    // Cierra con false
onConfirm(): void   // Cierra con true
```

**Uso (en ListComponent):**
```typescript
const dialogRef = this.dialog.open(ConfirmDialogComponent, {
    width: '400px',
    data: {
        title: 'Eliminar usuario',
        message: '¿Estás seguro?',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar'
    }
});

dialogRef.afterClosed().subscribe(result => {
    if (result) { /* confirmar */ }
});
```

#### `LayoutComponent` (`shared/layout/layout.component.ts`)

**Selector:** `app-layout`  
**Standalone:** `true`

**Funcionalidad:**
- Componente contenedor principal de la aplicación (después del login)
- Renderiza sidebar con navegación
- Renderiza toolbar con usuario actual y logout
- Filtra items de navegación según rol del usuario
- Responsive: colapsa sidebar en mobile (< 768px)

**Propiedades:**
```typescript
currentUser: CurrentUser | null
sidebarCollapsed = false
isMobile = false
profileDropdownOpen = false
hasNotifications = true

navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: [...] },
    { label: 'Clientes', icon: 'people', route: '/clientes', roles: ['ADMIN', 'RECEPCION'] },
    { label: 'Órdenes de Trabajo', icon: 'build', route: '/ordenes', roles: [...] },
    { label: 'Inventario', icon: 'inventory_2', route: '/inventario', roles: [...] },
    { label: 'Reportes', icon: 'bar_chart', route: '/reportes', roles: ['ADMIN'] },
    { label: 'Usuarios', icon: 'admin_panel_settings', route: '/usuarios', roles: ['ADMIN'] }
];
```

**Métodos:**
```typescript
ngOnInit(): void
checkScreenSize(): void
getVisibleNavItems(): NavItem[]     // Filtra por rol actual
logout(): void
getRolLabel(): string               // "ADMIN" -> "Administrador"
getUserInitials(): string           // Primeros 2 caracteres del email
getUserName(): string               // Parte anterior a @
updateModuleName(url: string): void
```

---

# MÓDULO DE CLIENTES Y EQUIPOS

## BACKEND (Spring Boot)

### Entidades JPA

#### `Cliente` (`com.tallersoft.model.Cliente`)

**Tabla:** `clientes`

**Campos y Anotaciones:**

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(name = "nombre", nullable = false, length = 100)
private String nombre;                    // Nombre cliente (requerido)

@Column(name = "telefono", length = 20)
private String telefono;                  // Teléfono (opcional)

@Column(name = "email", length = 150)
private String email;                     // Email (opcional)

@Column(name = "direccion", length = 200)
private String direccion;                 // Dirección (opcional)

@Column(name = "activo", nullable = false)
private boolean activo;                   // Estado (default true)

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;          // Timestamp creación (auto)

@OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
private List<Equipo> equipos;             // Relación 1:N con Equipo
```

**Relaciones:**
- `@OneToMany equipos`: Lazy loading, cascade all (si se elimina cliente, se elimina equipos)

#### `Equipo` (`com.tallersoft.model.Equipo`)

**Tabla:** `equipos`

**Campos y Anotaciones:**

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "cliente_id", nullable = false)
private Cliente cliente;                  // FK a Cliente (requerido)

@Column(name = "tipo", nullable = false, length = 50)
private String tipo;                      // Tipo equipo (requerido): Laptop, PC, Impresora, etc.

@Column(name = "marca", length = 50)
private String marca;                     // Marca (opcional)

@Column(name = "modelo", length = 100)
private String modelo;                    // Modelo (opcional)

@Column(name = "numero_serie", length = 100)
private String numeroSerie;               // Número de serie (opcional)

@Column(name = "observaciones", columnDefinition = "TEXT")
private String observaciones;             // Observaciones (opcional, TEXT)
```

**Relaciones:**
- `@ManyToOne cliente`: Lazy loading, FK no nulo

### DTOs

#### `ClienteRequest`

**Usado en:** `POST /api/clientes`, `PUT /api/clientes/{id}`

```java
@Data
public class ClienteRequest {
    @NotBlank(message = "El nombre es requerido")
    private String nombre;              // (requerido)
    
    private String telefono;            // (opcional)
    
    @Email(message = "El email debe ser válido")
    private String email;               // (opcional, si se proporciona debe ser válido)
    
    private String direccion;           // (opcional)
}
```

#### `ClienteResponse`

**Retornado en:** Listados, obtención, creación, edición

```java
@Data
@AllArgsConstructor
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;
    private boolean activo;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
```

#### `EquipoRequest`

**Usado en:** `POST /api/equipos`, `PUT /api/equipos/{id}`

```java
@Data
public class EquipoRequest {
    @NotNull(message = "El cliente ID es requerido")
    private Long clienteId;             // (requerido)
    
    @NotBlank(message = "El tipo de equipo es requerido")
    private String tipo;                // (requerido): "Laptop", "PC", "Impresora", etc.
    
    private String marca;               // (opcional)
    
    private String modelo;              // (opcional)
    
    private String numeroSerie;         // (opcional)
    
    private String observaciones;       // (opcional)
}
```

#### `EquipoResponse`

**Retornado en:** Listados, obtención, creación, edición

```java
@Data
@AllArgsConstructor
public class EquipoResponse {
    private Long id;
    private Long clienteId;             // ID del cliente propietario
    private String tipo;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String observaciones;
}
```

### Mappers MapStruct

#### `ClienteMapper`

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper {
    Cliente toEntity(ClienteRequest request);
    ClienteResponse toResponse(Cliente entity);
    List<ClienteResponse> toResponseList(List<Cliente> entities);
}
```

#### `EquipoMapper`

```java
@Mapper(componentModel = "spring")
public interface EquipoMapper {
    Equipo toEntity(EquipoRequest request);
    EquipoResponse toResponse(Equipo entity);
    List<EquipoResponse> toResponseList(List<Equipo> entities);
}
```

### Services

#### `ClienteService` (`com.tallersoft.service.ClienteService`)

```java
@Slf4j
@Service
public class ClienteService {
    
    /**
     * Crear nuevo cliente
     * - Establece activo = true
     * - Crea timestamp createdAt
     */
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request);
    
    /**
     * Obtener cliente por ID
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long id)
        throws EntityNotFoundException;
    
    /**
     * Listar clientes activos, opcionalmente filtrados por nombre
     * @param nombre (opcional) busca ILIKE (case-insensitive)
     * @return solo clientes con activo=true
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes(String nombre);
    
    /**
     * Actualizar cliente existente
     * - Actualiza todos los campos
     */
    @Transactional
    public ClienteResponse editarCliente(Long id, ClienteRequest request)
        throws EntityNotFoundException;
    
    /**
     * Soft delete cliente
     * - Sets activo = false
     * - NO elimina de BD
     */
    @Transactional
    public void eliminarCliente(Long id)
        throws EntityNotFoundException;
}
```

#### `EquipoService` (`com.tallersoft.service.EquipoService`)

```java
@Slf4j
@Service
public class EquipoService {
    
    /**
     * Crear nuevo equipo
     * - Valida que cliente existe (EntityNotFoundException si no)
     * - Construye entidad Equipo con Cliente
     */
    @Transactional
    public EquipoResponse crearEquipo(EquipoRequest request)
        throws EntityNotFoundException;
    
    /**
     * Listar todos equipos de un cliente
     */
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarEquiposDelCliente(Long clienteId);
    
    /**
     * Actualizar equipo existente
     */
    @Transactional
    public EquipoResponse editarEquipo(Long id, EquipoRequest request)
        throws EntityNotFoundException;
    
    /**
     * Eliminar equipo (hard delete)
     */
    @Transactional
    public void eliminarEquipo(Long id)
        throws EntityNotFoundException;
}
```

### Controllers

#### `ClienteController` (`com.tallersoft.controller.ClienteController`)

**Ruta base:** `/api/clientes`  
**Autenticación:** Requerida en todos  
**Autorización:** Roles especificados con `@PreAuthorize`

```java
@Slf4j
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    
    /**
     * GET /api/clientes
     * @param nombre (opcional) filtro por nombre
     * Response: List<ClienteResponse>
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(
        @RequestParam(required = false) String nombre
    );
    
    /**
     * GET /api/clientes/{id}
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id);
    
    /**
     * POST /api/clientes
     * Body: ClienteRequest
     * Status: 201 CREATED
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @PostMapping
    public ResponseEntity<ClienteResponse> crear(
        @Valid @RequestBody ClienteRequest request
    );
    
    /**
     * PUT /api/clientes/{id}
     * Body: ClienteRequest
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> editar(
        @PathVariable Long id,
        @Valid @RequestBody ClienteRequest request
    );
    
    /**
     * DELETE /api/clientes/{id}
     * Status: 204 NO_CONTENT
     * @PreAuthorize("hasRole('ADMIN')")
     * Nota: Soft delete (activo=false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id);
}
```

#### `EquipoController` (`com.tallersoft.controller.EquipoController`)

**Ruta base:** `/api/equipos`

```java
@Slf4j
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {
    
    /**
     * GET /api/equipos/cliente/{clienteId}
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'TECNICO')")
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<EquipoResponse>> listarPorCliente(
        @PathVariable Long clienteId
    );
    
    /**
     * POST /api/equipos
     * Body: EquipoRequest
     * Status: 201 CREATED
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @PostMapping
    public ResponseEntity<EquipoResponse> crear(
        @Valid @RequestBody EquipoRequest request
    );
    
    /**
     * PUT /api/equipos/{id}
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @PutMapping("/{id}")
    public ResponseEntity<EquipoResponse> editar(
        @PathVariable Long id,
        @Valid @RequestBody EquipoRequest request
    );
    
    /**
     * DELETE /api/equipos/{id}
     * Status: 204 NO_CONTENT
     * @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id);
}
```

### Repositories

#### `ClienteRepository`

```java
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
    // Query: SELECT * FROM clientes WHERE LOWER(nombre) LIKE LOWER(?) AND activo=true
    
    List<Cliente> findByTelefonoContainingAndActivoTrue(String telefono);
    // Usado internamente pero no visible en Controllers actuales
    
    List<Cliente> findByActivoTrue();
    // Query: SELECT * FROM clientes WHERE activo=true
}
```

#### `EquipoRepository`

```java
@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    List<Equipo> findByClienteId(Long clienteId);
    // Query: SELECT * FROM equipos WHERE cliente_id = ?
}
```

---

## FRONTEND (Angular)

### Estructura de Carpetas

```
frontend/src/app/modules/clientes/
├── list/
│   ├── list.component.ts
│   ├── list.component.html
│   └── list.component.scss
├── create/
│   ├── create.component.ts
│   ├── create.component.html
│   └── create.component.scss
├── detail/
│   ├── detail.component.ts
│   ├── detail.component.html
│   └── detail.component.scss
├── modals/
│   ├── edit-cliente/
│   │   ├── edit-cliente.modal.ts
│   │   ├── edit-cliente.modal.html
│   │   └── edit-cliente.modal.scss
│   ├── add-equipo/
│   │   ├── add-equipo.modal.ts
│   │   ├── add-equipo.modal.html
│   │   └── add-equipo.modal.scss
│   ├── edit-equipo/
│   │   └── edit-equipo.modal.ts
│   ├── delete-equipo/
│   │   └── delete-equipo.modal.ts
│   └── delete-confirm/
│       └── delete-confirm.modal.ts
├── services/
│   └── cliente.service.ts
├── clientes-list.component.ts      (contenedor padre - casi vacío)
└── clientes.routes.ts
```

### Componentes

#### `ListComponent` (Clientes) (`modules/clientes/list/list.component.ts`)

**Selector:** `app-clientes-list`  
**Standalone:** `true`

**Funcionalidad:**
- Carga y renderiza lista de clientes con búsqueda
- Implementa **debounce de búsqueda**: espera 300ms sin escribir antes de llamar backend
- Usa RxJS `Subject` con `debounceTime`, `distinctUntilChanged`, `switchMap`
- Verifica rol actual: solo ADMIN y RECEPCION pueden ver
- Tabla con columnas: nombre, teléfono, email, dirección, acciones
- Botón "Nuevo Cliente" navega a `/clientes/nuevo`
- Botón eliminar: abre `ConfirmDialogComponent`, luego soft-deletes
- Estados: loading, error (snackbar)

**Métodos públicos:**
```typescript
ngOnInit(): void
onSearch(event: Event): void        // Input search debounced
private triggerSearch(value: string): void
onDelete(clienteId: number): void   // Abre confirm dialog
onCreateClick(): void               // Navega a crear
onFilterClick(): void               // TODO: implement
getInitials(name: string): string
getClienteOrdenesCount(clienteId): number
navigateToClient(clienteId): void
```

**Propiedades clave:**
```typescript
searchTerm = ''
isLoading = false
clientes: ClienteResponse[] = []
filteredClientes$: Subject<ClienteResponse[]>
private searchSubject = new Subject<string>()

get currentRole(): string | null
```

**RxJS Pipeline:**
```typescript
this.searchSubject.pipe(
    debounceTime(300),              // Espera 300ms sin nuevos valores
    distinctUntilChanged(),         // No emite si valor es igual
    switchMap(query => {
        this.isLoading = true;
        return this.clienteService.listarClientes(query || undefined)
            .pipe(finalize(() => this.isLoading = false));
    }),
    takeUntilDestroyed(this.destroyRef)
).subscribe({ ... });
```

#### `CreateComponent` (Clientes) (`modules/clientes/create/create.component.ts`)

**Selector:** `app-clientes-create`  
**Standalone:** `true`

**Funcionalidad:**
- Formulario ReactiveForm para crear cliente + equipo inicial (opcional)
- Campos cliente: nombre (req), teléfono, email, dirección
- Campos equipo: tipo, marca, modelo, numeroSerie, observaciones
- Selector visual de tipo equipo (botones con icons)
- Envía ClienteRequest a `ClienteService.crearCliente()`
- Si tipo equipo está seleccionado, crea también equipo con `EquipoService.crearEquipo()`
- Redirige a `/clientes` en éxito

**Propiedades:**
```typescript
form: FormGroup
isLoading = false
selectedEquipmentType: string | null = null
```

**Control de formulario:**
```typescript
this.form = this.fb.group({
    nombre: ['', Validators.required],
    telefono: [''],
    email: ['', Validators.email],
    direccion: [''],
    marca: [''],
    modelo: [''],
    numeroSerie: [''],
    observaciones: ['']
});
```

#### `DetailComponent` (Clientes) (`modules/clientes/detail/detail.component.ts`)

**Selector:** `app-clientes-detail`  
**Standalone:** `true`

**Funcionalidad:**
- Página de detalle de un cliente específico
- Carga cliente + lista de equipos en paralelo con `forkJoin`
- Renderiza datos del cliente con botones de editar/eliminar
- Tab "Equipos": lista de equipos del cliente
- Tab "Historial": TODO (órdenes de trabajo del cliente)
- Modales para: editar cliente, agregar equipo, editar equipo, eliminar equipo
- Abre modales con flags `isEditModalOpen`, `isAddEquipoModalOpen`, etc.

**Métodos públicos:**
```typescript
ngOnInit(): void
loadCliente(id: number): void       // forkJoin cliente + equipos
populateEditForm(): void            // Deprecated con modales
onEdit(): void
onEditSaved(updatedCliente): void
onEditCancelled(): void
onDelete(): void
onDeleteConfirmed(): void
onDeleteCancelled(): void
```

**Propiedades:**
```typescript
isLoading = false
isEditing = false
loadingEquipos = false
loadingOrdenes = false
activeTab: 'equipos' | 'historial' = 'equipos'

isDeleteModalOpen = false
isEditModalOpen = false
isAddEquipoModalOpen = false
isEditEquipoModalOpen = false
isDeleteEquipoModalOpen = false

cliente: ClienteResponse | null = null
equipos: EquipoResponse[] = []
ordenes: any[] = []

get currentRole(): string | null
```

**RxJS Pattern:**
```typescript
forkJoin({
    cliente: this.clienteService.obtenerCliente(id),
    equipos: this.equipoService.listarEquiposDelCliente(id)
}).pipe(
    takeUntilDestroyed(this.destroyRef),
    finalize(() => this.isLoading = false)
).subscribe({ ... });
```

### Modales

#### `EditClienteModal` (`modals/edit-cliente/edit-cliente.modal.ts`)

**Selector:** `app-edit-cliente-modal`  
**Standalone:** `true`

**Funcionalidad:**
- Modal para editar datos del cliente
- Formulario ReactiveForm con campos: nombre, email, teléfono, dirección
- Overlay oscuro con click outside cierra modal
- Llamadas a `ClienteService.editarCliente()`
- Emite evento `@Output() onEditSaved` con cliente actualizado
- Emite evento `@Output() onEditCancelled`

**Inputs:**
```typescript
@Input() isOpen: boolean
@Input() cliente: ClienteResponse | null
```

**Outputs:**
```typescript
@Output() onEditSaved = new EventEmitter<ClienteResponse>();
@Output() onEditCancelled = new EventEmitter<void>();
```

**Métodos:**
```typescript
onCancel(): void
onSubmit(): void
get nombreControl: FormControl
get emailControl: FormControl
get telefonoControl: FormControl
get direccionControl: FormControl
```

#### `AddEquipoModal` (`modals/add-equipo/add-equipo.modal.ts`)

**Selector:** `app-add-equipo-modal`  
**Standalone:** `true`

**Funcionalidad:**
- Modal para agregar nuevo equipo a un cliente
- Selector visual de tipo equipo: Laptop, PC, Impresora, Tablet, etc. (con icons)
- Campos: marca, modelo, numeroSerie, observaciones
- Llamadas a `EquipoService.crearEquipo()`
- Valida que se seleccione al menos un tipo
- Emite evento `@Output() onEquipoAdded`

**Inputs:**
```typescript
@Input() isOpen: boolean
@Input() clienteId: number | null
```

**Outputs:**
```typescript
@Output() onEquipoAdded = new EventEmitter<EquipoResponse>();
@Output() onCancelled = new EventEmitter<void>();
```

**Selector de tipo:**
```typescript
equipmentTypes = [
    { value: 'Laptop', label: 'Laptop', icon: 'laptop' },
    { value: 'PC', label: 'Computadora', icon: 'desktop_mac' },
    { value: 'Impresora', label: 'Impresora', icon: 'print' },
    { value: 'Tablet', label: 'Tablet', icon: 'tablet' },
    // ... más tipos
];

selectedType: string | null = null;

selectType(type: string): void {
    this.selectedType = type;
    this.form.get('tipo')?.setValue(type);
}
```

#### Otros Modales

- `EditEquipoModal`: Similar a EditClienteModal, edita datos de equipo
- `DeleteEquipoModal`: Solicita confirmación antes de eliminar
- `DeleteConfirmModal`: Modal genérico de confirmación de delete

### Servicios

#### `ClienteService` (`modules/clientes/services/cliente.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class ClienteService {
    private api = `${environment.apiUrl}/api/clientes`;
    
    /**
     * Listar clientes activos, opcionalmente filtrados por nombre
     * GET /api/clientes?nombre=...
     * @param nombre (opcional)
     */
    listarClientes(nombre?: string): Observable<ClienteResponse[]>;
    
    /**
     * Obtener cliente por ID
     * GET /api/clientes/{id}
     */
    obtenerCliente(id: number): Observable<ClienteResponse>;
    
    /**
     * Crear nuevo cliente
     * POST /api/clientes
     */
    crearCliente(data: ClienteRequest): Observable<ClienteResponse>;
    
    /**
     * Actualizar cliente
     * PUT /api/clientes/{id}
     */
    editarCliente(id: number, data: ClienteRequest): Observable<ClienteResponse>;
    
    /**
     * Eliminar cliente (soft delete)
     * DELETE /api/clientes/{id}
     */
    eliminarCliente(id: number): Observable<void>;
}

export interface ClienteRequest {
    nombre: string;
    telefono?: string | null;
    email?: string | null;
    direccion?: string | null;
}

export interface ClienteResponse {
    id: number;
    nombre: string;
    telefono: string | null;
    email: string | null;
    direccion: string | null;
    activo: boolean;
    createdAt: string;
}
```

#### `EquipoService` (`modules/ordenes/services/equipo.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class EquipoService {
    private api = `${environment.apiUrl}/api/equipos`;
    
    /**
     * Listar equipos de un cliente
     * GET /api/equipos/cliente/{clienteId}
     */
    listarEquiposDelCliente(clienteId: number): Observable<EquipoResponse[]>;
    
    /**
     * Crear nuevo equipo
     * POST /api/equipos
     */
    crearEquipo(equipo: any): Observable<EquipoResponse>;
    
    /**
     * Actualizar equipo
     * PUT /api/equipos/{id}
     */
    editarEquipo(id: number, equipo: any): Observable<EquipoResponse>;
    
    /**
     * Eliminar equipo
     * DELETE /api/equipos/{id}
     */
    eliminarEquipo(id: number): Observable<void>;
}

export interface EquipoResponse {
    id: number;
    clienteId: number;
    tipo: string;
    marca?: string | null;
    modelo?: string | null;
    numeroSerie?: string | null;
    observaciones?: string | null;
    descripcion?: string | null;
}
```

### Rutas

#### `clientes.routes.ts`

```typescript
export const clientesRoutes: Routes = [
    { path: '', component: ListComponent },
    { path: 'nuevo', component: CreateComponent },
    { path: ':id', component: DetailComponent }
];

// Integración en app.routes.ts:
// {
//   path: 'clientes',
//   loadChildren: () => import('./modules/clientes/clientes.routes')
//     .then(m => m.clientesRoutes),
//   canActivate: [AuthGuard]
// }
```

---

# PATRONES Y CONVENCIONES OBSERVADAS

## Arquitectura General del Módulo Angular

### Estructura Típica

Cada módulo completado sigue este patrón:

```
modules/<nombre>/
├── list/
│   ├── list.component.ts
│   ├── list.component.html
│   └── list.component.scss
├── create/
│   ├── create.component.ts
│   ├── create.component.html
│   └── create.component.scss
├── detail/  (opcional, solo si se muestran detalles)
│   ├── detail.component.ts
│   ├── detail.component.html
│   └── detail.component.scss
├── modals/  (opcional, modales específicas del módulo)
│   └── ...
├── services/
│   └── <nombre>.service.ts
└── <nombre>.routes.ts
```

**Componente contenedor opcional** (ej: `clientes-list.component.ts`):
- Es casi un "shell" que solo importa el componente principal
- Usado cuando se necesita aplicar estilos o lógica global del módulo
- A menudo solo renderiza: `<app-<nombre>-list></app-<nombre>-list>`

### Convención de Nombres

**Archivos:**
- Componentes: `[nombre].component.ts`
- Servicios: `[nombre].service.ts`
- Modales: `[nombre].modal.ts`
- Rutas: `[nombre].routes.ts`

**Selectores (Componentes):**
- Lista: `app-<nombre>-list`
- Crear: `app-<nombre>-create`
- Detalle: `app-<nombre>-detail`
- Modal: `app-<nombre>-modal` o `app-<acción>-modal`

**Interfaces:**
- Request DTO: `<Entidad>Request`
- Response DTO: `<Entidad>Response`
- Ejemplo: `ClienteRequest`, `ClienteResponse`

**Enumeraciones:**
- Status de órdenes: `EstadoOrden` (PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO)
- Roles: `Rol` (ADMIN, TECNICO, RECEPCION)
- Prioridad: `Prioridad` (BAJA, MEDIA, ALTA, URGENTE)

## Manejo de HTTP y Llamadas a Endpoints

### Patrón Base

```typescript
// En el servicio
listarClientes(nombre?: string): Observable<ClienteResponse[]> {
    let params = new HttpParams();
    if (nombre) {
        params = params.set('nombre', nombre);
    }
    return this.http.get<ClienteResponse[]>(
        `${this.api}`,
        { params }
    ).pipe(
        tap(response => console.log('[Service] Response:', response)),
        catchError(err => {
            console.error('[Service] Error:', err);
            throw err;
        })
    );
}

// En el componente
this.clienteService.listarClientes(search).subscribe({
    next: (data) => {
        this.items = data;
        this.isLoading = false;
    },
    error: (err) => {
        console.error('Error loading items:', err);
        this.isLoading = false;
        this.snackBar.open(
            err.error?.message || 'Error al cargar',
            'Cerrar',
            { duration: 3000, horizontalPosition: 'right', verticalPosition: 'bottom' }
        );
    }
});
```

### Manejo de Errores

- **Backend retorna:** `ErrorResponse { status, error, message }`
- **Frontend accede:** `error.error.message`
- **Mostrado en:** `MatSnackBar` con posición bottom-right, duración 3000ms
- **Estados:** Loading state (`isLoading` flag) desactiva botones/inputs

### Token JWT

- **Almacenamiento:** `sessionStorage` (nunca localStorage)
- **Header:** `Authorization: Bearer <token>`
- **Agregado automáticamente por:** `jwtInterceptor` en todos los requests
- **Expiración:** 24 horas por defecto (`jwt.expiration: 86400000`)

### Timeout y Limpieza

- **Memory leak prevention:** Todos usan `takeUntilDestroyed(this.destroyRef)`
- **Pattern RxJS:**
```typescript
private destroyRef = inject(DestroyRef);

this.service.getData()
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({ ... });
```

## Formularios

### Componentes Formulario

- **Tipo:** ReactiveForm (NO Template-driven)
- **Constructor:** Inyectar `FormBuilder`, crear FormGroup en constructor o ngOnInit

**Patrón base:**
```typescript
form = this.fb.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['']  // Sin validadores = opcional
});

get nombreControl() { return this.form.get('nombre'); }

onSubmit() {
    if (this.form.invalid) {
        this.form.markAllAsTouched();  // Muestra errores
        return;
    }
    // enviar...
}
```

### Validación

- **Validadores:** `Validators.required`, `Validators.email`, `Validators.minLength(8)`
- **Mostrado:** Errores como `<span class="error-text" *ngIf="control?.invalid && control?.touched">`
- **Botón submit:** Deshabilitado si `form.invalid`

### Estados de Carga

```typescript
isLoading = false;

// En submit o action
this.isLoading = true;
this.service.action(data).pipe(
    finalize(() => this.isLoading = false)
).subscribe({ ... });

// En template
<button [disabled]="isLoading">
    <span *ngIf="!isLoading">Guardar</span>
    <span *ngIf="isLoading">Guardando...</span>
</button>
```

## Modales y Diálogos

### Patrón: Input/Output con Flags

Usado en clientes/modales:

```typescript
// Modal component
@Input() isOpen: boolean;
@Input() item: ItemResponse | null;
@Output() onSaved = new EventEmitter<ItemResponse>();
@Output() onCancelled = new EventEmitter<void>();

onCancel() { this.onCancelled.emit(); }
onSubmit() { /* enviar */ this.onSaved.emit(result); }

// En template
<div class="modal-overlay" *ngIf="isOpen" (click)="onCancel()">
    ...
</div>
```

**Uso desde parent:**
```typescript
isEditModalOpen = false;
selectedItem: ItemResponse | null = null;

onEdit(item) {
    this.selectedItem = item;
    this.isEditModalOpen = true;
}

onEditSaved(updated) {
    this.selectedItem = updated;
    this.isEditModalOpen = false;
    this.loadData();  // Recargar lista
}
```

### Material Dialog (Confirmación)

Usado en ListComponent:

```typescript
const dialogRef = this.dialog.open(ConfirmDialogComponent, {
    width: '400px',
    data: {
        title: 'Eliminar cliente',
        message: '¿Estás seguro?',
        confirmText: 'Eliminar',
        cancelText: 'Cancelar'
    }
});

dialogRef.afterClosed().subscribe(result => {
    if (result) {
        // Usuario confirmó
        this.deleteItem();
    }
});
```

## Control de Visibilidad por Rol

### En Backend

```java
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
public ResponseEntity<List<ClienteResponse>> listar(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> eliminar(...) { ... }
```

### En Frontend

**Verificar rol actual:**
```typescript
get currentRole(): string | null {
    return this.authService.getCurrentRole();
}

// En template
<button *ngIf="currentRole === 'ADMIN'">Eliminar</button>

// O para múltiples roles
<button *ngIf="['ADMIN', 'RECEPCION'].includes(currentRole)">
    Editar
</button>
```

**Guards (no usado actualmente, pero disponible):**
```typescript
{
    path: 'admin',
    component: AdminComponent,
    canActivate: [RoleGuard],
    data: { roles: ['ADMIN'] }
}
```

## Paginación y Filtrado

### Filtrado Simple (Búsqueda)

**Backend:**
```java
// Repository query
List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);
```

**Frontend - Sin debounce:**
```typescript
onSearch(term: string) {
    this.searchTerm = term;
    this.loadData(term);
}
```

**Frontend - Con debounce (PATRÓN OBSERVADO EN CLIENTES):**
```typescript
private searchSubject = new Subject<string>();

ngOnInit() {
    this.searchSubject.pipe(
        debounceTime(300),                  // Espera 300ms
        distinctUntilChanged(),             // No repite búsquedas iguales
        switchMap(query =>
            this.service.listarClientes(query || undefined).pipe(
                finalize(() => this.isLoading = false)
            )
        ),
        takeUntilDestroyed(this.destroyRef)
    ).subscribe({
        next: (data) => {
            this.items = data;
            this.filteredItems$.next(data);
        },
        error: (err) => { /* ... */ }
    });
}

onSearch(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm = value;
    this.searchSubject.next(value);  // Dispara búsqueda debounced
}
```

### Paginación

**NO implementada en módulos actuales**, pero patrón esperado:
- Backend: `@RequestParam int page`, `@RequestParam int size` retorna `Page<T>`
- Frontend: Directiva o componente `<mat-paginator>`
- RxJS: `switchMap` para cada cambio de página

## Material Design Components Utilizados

### Components Observados

- **MatTableModule**: NO usado (se usan divs y CSS personalizados)
- **MatFormFieldModule**: Envolver inputs
- **MatInputModule**: `<input matInput>`
- **MatButtonModule**: `<button mat-raised-button>`, `<button mat-stroked-button>`
- **MatIconModule**: `<mat-icon>`, `<span class="material-symbols-outlined">`
- **MatSelectModule**: NO usado para roles (se usan botones visuales)
- **MatDialogModule**: Para ConfirmDialogComponent
- **MatSnackBarModule**: Notificaciones de éxito/error
- **MatTabsModule**: Tabs en DetailComponent
- **MatTooltipModule**: Hints en botones
- **MatMenuModule**: Menú usuario en header
- **MatDividerModule**: Separadores visuales

### Tema

- **Color scheme:** Dark theme (fondo #141824)
- **Font:** Material Symbols Outlined para icons
- **Variables CSS:** `--color-text-primary`, `--color-text-secondary`, `--color-accent`

## Patrones de Estado

### Loading State

```typescript
isLoading = false;

// Activar
this.isLoading = true;

// Desactivar automáticamente con finalize
this.service.method().pipe(
    finalize(() => this.isLoading = false)
).subscribe({ ... });
```

### Boolean Flags para Modales

```typescript
isDeleteModalOpen = false;
isEditModalOpen = false;
isAddEquipoModalOpen = false;

// Usar *ngIf="isAddEquipoModalOpen" en template
// Modal emite evento onCancelled/onSaved para cerrar
```

### BehaviorSubject para UI State

```typescript
currentUser$ = new BehaviorSubject<CurrentUser | null>(null);

// En template: async pipe
{{ (currentUser$ | async)?.email }}
```

## Implementación de Búsqueda Debounce

**Ubicación:** `modules/clientes/list/list.component.ts`

**Implementación:**
1. `private searchSubject = new Subject<string>()`
2. En ngOnInit:
   - `searchSubject.pipe(debounceTime(300), distinctUntilChanged(), switchMap(...))`
   - switchMap llama al servicio
   - finalize resetea isLoading
3. En onSearch: `searchSubject.next(value)`

**Ventajas:**
- No hace request en cada keystroke
- Cancela pending requests si el usuario escribe de nuevo
- Evita duplicados si el query es igual

## Shared Components & Helpers

### Componentes Compartidos

1. **ConfirmDialogComponent** (`shared/dialogs/confirm-dialog.component.ts`)
   - Dialog genérico para confirmaciones
   - Inyecta data con: title, message, confirmText, cancelText
   - Retorna true/false al cerrar

2. **LayoutComponent** (`shared/layout/layout.component.ts`)
   - Shell principal (sidebar + header)
   - Filtra nav items por rol
   - Render: `<router-outlet>`

### Servicios Core

1. **AuthService** (`core/auth/auth.service.ts`)
   - Login, logout, token management
   - BehaviorSubject currentUser$
   - Decodifica JWT manualmente

2. **JwtInterceptor** (`core/interceptors/jwt.interceptor.ts`)
   - Agrega header Authorization a todos requests
   - Maneja 401 (logout + redirect /login)

## Configuración de Rutas

### App Routes (`app.routes.ts`)

```typescript
export const routes: Routes = [
    {
        path: 'login',
        loadChildren: () => import('./modules/auth/auth.routes')
            .then(m => m.authRoutes)
    },
    {
        path: '',
        component: LayoutComponent,
        canActivate: [AuthGuard],
        children: [
            {
                path: 'clientes',
                loadChildren: () => import('./modules/clientes/clientes.routes')
                    .then(m => m.clientesRoutes),
                canActivate: [AuthGuard]
            },
            {
                path: 'usuarios',
                loadChildren: () => import('./modules/usuarios/usuarios.routes')
                    .then(m => m.USUARIOS_ROUTES),
                canActivate: [AuthGuard]
            },
            // ... más modules
        ]
    }
];
```

**Pattern:**
- `login` es pública
- Todo dentro del LayoutComponent requiere AuthGuard
- Lazy loading con `loadChildren`
- Cada módulo define sus propias rutas en `[nombre].routes.ts`

---

## Checklist para Implementar Nuevo Módulo (Stock)

Basado en los patrones observados, para crear el **Módulo 4 (Stock de Repuestos)**:

### Backend

- [ ] Crear entidad `Repuesto` con: id, nombre, descripcion, precioUnitario, stockActual, codigoInterno, activo, createdAt
- [ ] Crear DTOs: `RepuestoRequest`, `RepuestoResponse`
- [ ] Crear Mapper: `RepuestoMapper`
- [ ] Crear Service: `RepuestoService` con métodos: crear, obtener, listar, listarPorStockBajo, editar, eliminar
- [ ] Crear Controller: `RepuestoController` con endpoints CRUD + filtrado por nombre/código
- [ ] Crear Repository: `RepuestoRepository` con queries: `findByNombreContainingIgnoreCaseAndActivoTrue`, `findByStockActualLessThan`
- [ ] Agregar validaciones en DTOs
- [ ] Registrar excepciones en GlobalExceptionHandler (si es necesario nuevas como `InsufficientStockException`)
- [ ] Aplicar `@PreAuthorize` según roles permitidos

### Frontend

- [ ] Crear módulo en `modules/stock/`
- [ ] Crear componentes: `ListComponent`, `CreateComponent`, `DetailComponent` (opcional)
- [ ] Crear servicio: `RepuestoService` con métodos que llamen a `/api/repuestos`
- [ ] Crear rutas: `stock.routes.ts`
- [ ] Implementar tabla de listado con búsqueda debounced
- [ ] Implementar formulario de creación con validaciones
- [ ] Agregar botones editar/eliminar con confirmación
- [ ] Usar ConfirmDialogComponent para delete
- [ ] Implementar indicador visual de stock bajo (rojo si < mínimo)
- [ ] Usar MatSnackBar para feedback
- [ ] Implementar loading states
- [ ] Control de visibilidad por rol

### Extras

- [ ] Translations i18n si aplica
- [ ] Tests unitarios
- [ ] Documentación HTML/SCSS

---

## Dependencias y Versiones Clave

**Backend:**
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT (JJWT)
- MapStruct
- Lombok
- JUnit (tests)

**Frontend:**
- Angular 17+
- RxJS 7+
- Angular Material
- TypeScript

---

## Conclusión

Este documento describe exhaustivamente la implementación real de dos módulos completados. Al replicar estos patrones en nuevos módulos, se asegura:

1. **Consistencia** en la arquitectura y convenciones
2. **Mantenibilidad** mediante código predecible
3. **Escalabilidad** para agregar nuevas funcionalidades
4. **Experiencia de usuario** uniforme (UI/UX, manejo de errores)
5. **Seguridad** mediante validación, autenticación y autorización

Los patrones documentados aquí deben aplicarse como referencia al construir el **Módulo 4 (Stock)** y módulos futuros.
