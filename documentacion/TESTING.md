# Testing — TallerSoft

## Estrategia de Testing

El proyecto cubre tres capas de testing:

| Capa | Herramienta | Ubicación | Qué cubre |
|------|------------|-----------|-----------|
| Backend — Unitarios | JUnit 5 + Mockito | `backend/src/test/` | Lógica de negocio de cada servicio |
| Backend — Integración | Spring Boot Test + MockMvc | `backend/src/test/integration/` | Endpoints REST end-to-end |
| Analytics (Python) | pytest + pytest-mock | `analytics/tests/` | Endpoints FastAPI y servicios |
| Frontend | Jasmine + Karma | `frontend/src/**/*.spec.ts` | Servicios Angular, guards, interceptores, componentes |

---

## Tests Unitarios del Backend (JUnit 5)

### Convenciones

- **Ubicación:** `backend/src/test/java/com/tallersoft/service/`
- **Anotación:** `@ExtendWith(MockitoExtension.class)` en cada clase
- **Nomenclatura de métodos:** `metodo_descripcionEscenario_resultadoEsperado()`
- **Estructura:** Patrón `// Arrange / Act / Assert`
- Cada método de servicio tiene mínimo **dos tests**: caso exitoso y caso de error
- `@DisplayName` describe el comportamiento en cada test

### Suite de Tests

| Clase de Test | Servicio Testeado | Cobertura Principal |
|--------------|-----------------|---------------------|
| `AuthServiceTest` | `AuthService` | Login válido/inválido, registro, usuario inactivo |
| `UsuarioServiceTest` | `UsuarioService` | CRUD de usuarios, validación de email duplicado, rol inválido |
| `ClienteServiceTest` | `ClienteService` | CRUD, búsqueda, baja lógica |
| `OrdenTrabajoServiceTest` | `OrdenTrabajoService` | Crear orden, cambiar estado, diagnóstico faltante, agregar repuesto, rollback por stock |
| `RepuestoServiceTest` | `RepuestoService` + `OrdenTrabajoService` | Crear, editar, stock crítico, agregar a orden |
| `CajaServiceTest` | `CobrosService` | EFECTIVO/TARJETA/MERCADOPAGO, idempotencia webhook, pago aprobado |
| `MercadoPagoServiceTest` | `MercadoPagoService` + `MercadoPagoWebhookValidator` | Validación HMAC, error sin token |
| `JwtUtilTest` | `JwtUtil` | Generación, validación, extracción de claims |

### Cómo Correr

```bash
cd backend
mvn test
# Con reporte detallado:
mvn test -Dsurefire.failIfNoSpecifiedTests=false
# Solo una clase:
mvn test -Dtest=OrdenTrabajoServiceTest
```

---

## Tests de Integración del Backend

### Convenciones

- **Ubicación:** `backend/src/test/java/com/tallersoft/integration/`
- **Anotaciones:** `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- Usan H2 in-memory o un PostgreSQL de test (requiere perfil `test` en `application-test.yml`)

### Suite de Tests

| Clase | Endpoints Cubiertos |
|-------|-------------------|
| `AuthControllerIntegrationTest` | `POST /auth/login` → 200, 401, 400 |
| `WebhookControllerIntegrationTest` | `POST /api/pagos/webhook` → accesible sin JWT, error sin firma |

---

## Tests del Microservicio Analytics (pytest)

### Convenciones

- **Ubicación:** `analytics/tests/`
- `conftest.py` configura SQLite in-memory para tests sin depender de PostgreSQL
- Se mockean los servicios externos (Groq) con `unittest.mock.patch`
- Una clase o archivo por router testeado

### Suite de Tests

| Archivo | Endpoints Cubiertos |
|---------|-------------------|
| `test_ordenes.py` | `GET /analytics/ordenes/resumen`, `/por-periodo`, `/tecnicos/rendimiento` |
| `test_stock.py` | `GET /analytics/stock/critico`, `/mas-usados` |
| `test_caja.py` | `GET /analytics/caja/resumen-diario`, `/evolucion-mensual` |
| `test_asistente.py` | `POST /analytics/asistente/consulta` — estructura, validación, error handling |

### Cómo Correr

```bash
cd analytics
source .venv/bin/activate
python -m pytest tests/ -v --tb=short
# Con cobertura:
python -m pytest tests/ -v --cov=app --cov-report=html
# Un archivo:
python -m pytest tests/test_ordenes.py -v
```

---

## Tests del Frontend (Jasmine + Karma)

### Convenciones

- **Ubicación:** mismo directorio que el archivo testeado, con sufijo `.spec.ts`
- `HttpClientTestingModule` para mockear requests HTTP
- `RouterTestingModule` para tests con navegación
- `jasmine.createSpyObj` para mockear servicios

### Suite de Tests

| Spec | Cubre |
|------|-------|
| `auth.service.spec.ts` | Login, logout, sessionStorage (nunca localStorage), getToken, getCurrentRole |
| `ordenes.service.spec.ts` | GET órdenes con filtros, PUT estado, GET activas |
| `jwt.interceptor.spec.ts` | Adjuntar Bearer token, no token = no header, 401 → logout + redirect |
| `auth.guard.spec.ts` | Token válido → accede; sin token → redirige; RoleGuard por rol |
| `kanban.component.spec.ts` | 4 columnas, distribución de órdenes, error en loadOrdenes |
| `cobrar-orden.component.spec.ts` | Selección de medio, cálculo de vuelto, validación monto insuficiente |
| `repuesto.service.spec.ts` | Tests del servicio de repuestos (existente) |

### Cómo Correr

```bash
cd frontend
ng test                                         # Modo watch
ng test --watch=false --browsers=ChromeHeadless # CI / una sola corrida
ng test --code-coverage                          # Reporte de cobertura
# El reporte se genera en: frontend/coverage/index.html
```

---

## Colección de Postman

### Cómo Importar

1. Abrir Postman
2. File → Import → subir el archivo `docs/TallerSoft.postman_collection.json` (si existe) o crear manualmente
3. Crear un environment `TallerSoft Local` con variable `baseUrl = http://localhost:8080`

### Variables de Entorno

| Variable | Valor |
|----------|-------|
| `baseUrl` | `http://localhost:8080` |
| `token` | *(se setea automáticamente con el script de login)* |

### Correr la Colección Completa

En Postman: Collection → Run → seleccionar environment → Run.

---

## Casos de Prueba Manuales Críticos

Verificar antes de cada deploy:

| # | Escenario | Pasos | Resultado Esperado |
|---|-----------|-------|--------------------|
| 1 | Login con ADMIN | Ingresar con admin@tallersoft.com | Accede al dashboard, ve menú de Usuarios |
| 2 | Login con TECNICO | Ingresar con carlos@tallersoft.com | Accede al dashboard, NO ve menú de Usuarios |
| 3 | Login con RECEPCION | Ingresar con maria@tallersoft.com | Accede, ve Clientes y Caja, NO ve Usuarios |
| 4 | Flujo completo de orden | Crear → asignar técnico → EN_PROCESO → agregar diagnóstico → LISTO → cobrar EFECTIVO → verificar ENTREGADO | Orden aparece en ENTREGADO, cobro APROBADO en caja diaria |
| 5 | Flujo cobro MERCADOPAGO | Crear cobro MP → aparece QR → simular webhook → orden ENTREGADA | Orden pasa a ENTREGADO tras recibir webhook |
| 6 | Cliente inactivo | Dar de baja a un cliente, intentar crear orden para él | Error 422 "cliente inactivo" |
| 7 | Stock insuficiente | Agregar repuesto con stock=0 a una orden | Error 409, stock no se modifica |
| 8 | Kanban drag & drop error | Intentar mover orden a LISTO sin diagnóstico | Card vuelve a su posición, snackbar de error visible |
| 9 | Asistente IA responde en español | Preguntar "¿Cuántas órdenes hay?" en el chat | Respuesta en español con datos reales del taller |
| 10 | Asistente IA no inventa datos | Preguntar sobre datos que no existen | Responde que no tiene esa información, no inventa |
| 11 | JWT expirado | Esperar expiración (o setear exp en 1 min), hacer request | Respuesta 401 con mensaje "TOKEN_EXPIRADO" amigable |
| 12 | Webhook idempotente | Enviar el mismo webhook dos veces | Segunda vez retorna 200 sin duplicar el cobro |

---

## Deuda Técnica — Pendiente de Implementación

| ID | Módulo | Descripción | Prioridad |
|----|--------|-------------|-----------|
| TD-01 | Backend | Refresh token: implementar endpoint `/auth/refresh` para renovar JWT sin re-login | Alta |
| TD-02 | Backend | `@Lock(PESSIMISTIC_WRITE)` en repositorio de repuestos para prevenir race conditions en alta concurrencia | Media |
| TD-03 | Frontend | Tests de componentes de creación de órdenes (`create.component.spec.ts`) | Media |
| TD-04 | Frontend | Tests de `dashboard.component.spec.ts` (gráficos CSS personalizados) | Media |
| TD-05 | Analytics | Rate limiting en `/analytics/asistente/consulta` para evitar costos excesivos de API | Alta |
| TD-06 | Integración | Tests de integración con `@SpringBootTest` y base de datos H2 (requiere `application-test.yml`) | Media |
| TD-07 | Backend | WebSockets para actualización en tiempo real del Kanban entre múltiples usuarios | Baja |
| TD-08 | Deploy | Pipeline CI/CD con GitHub Actions (build, test, push a registry) | Alta |

---

## Bugs Encontrados y Corregidos Durante el Sprint 6

| Bug | Componente | Descripción | Fix Aplicado |
|-----|-----------|-------------|-------------|
| B-01 | Backend GlobalExceptionHandler | `ExpiredJwtException` no era capturada; el usuario recibía 500 | Agregado handler específico que retorna 401 con mensaje `TOKEN_EXPIRADO` |
| B-02 | OrdenTrabajoService | Se podían crear órdenes para clientes con `activo = false` | Agregada validación en `crearOrden()` que lanza 422 si el cliente está inactivo |
| B-03 | Analytics asistente router | Si la API de IA fallaba, retornaba 500 crudo | Envuelto en try/except con mensaje amigable en lugar de propagar el error |
