# Módulo 5 — Caja y Facturación
## Plan de Desarrollo — TallerSoft

> **Contexto:** Este documento es la guía completa de implementación del Módulo 5 de TallerSoft. Los módulos 1 al 4 (Auth, Clientes, Órdenes, Stock) ya están implementados. Este módulo se desarrolla desde cero sobre esa base.

---

## Índice

- [Resumen del módulo](#resumen-del-módulo)
- [Entidades y base de datos](#entidades-y-base-de-datos)
- [Decisiones de diseño previas](#decisiones-de-diseño-previas)
- [Bloque 1 — Backend base](#bloque-1--backend-base-entidad-repositorio-y-lógica-de-cobros)
- [Bloque 2 — Integración MercadoPago](#bloque-2--integración-mercadopago)
- [Bloque 3 — Generación y previsualización de PDF](#bloque-3--generación-y-previsualización-de-pdf)
- [Bloque 4 — Frontend Angular](#bloque-4--frontend-angular)
- [Reglas de negocio](#reglas-de-negocio)
- [Endpoints del módulo](#endpoints-del-módulo)
- [Tests requeridos](#tests-requeridos)
- [Orden de implementación](#orden-de-implementación)
- [Variables de entorno nuevas](#variables-de-entorno-nuevas)
- [Checklist de completitud](#checklist-de-completitud)

---

## Resumen del módulo

El Módulo 5 cubre el ciclo de cobro de una orden de trabajo y la emisión de presupuestos en PDF. Es el paso final del flujo operativo del taller: una orden llega en estado `LISTO` y, al registrarse un cobro aprobado, pasa automáticamente a `ENTREGADO`.

**Medios de pago soportados:**

| Medio | Flujo de aprobación |
|---|---|
| `EFECTIVO` | Inmediato. El recepcionista ingresa el monto recibido, el sistema calcula el vuelto y aprueba el cobro en el acto. |
| `TARJETA` | Inmediato. El recepcionista confirma manualmente que el posnet aprobó. |
| `MERCADOPAGO` | Asíncrono. El sistema genera un link/QR de pago. El cobro queda `PENDIENTE` hasta que MercadoPago confirma via webhook. |

---

## Entidades y base de datos

La tabla `cobros` ya existe en el schema. No requiere migraciones adicionales salvo las indicadas abajo.

```sql
-- Tabla existente (referencia)
CREATE TABLE cobros (
    id              BIGSERIAL PRIMARY KEY,
    orden_id        BIGINT REFERENCES ordenes_trabajo(id),
    monto           NUMERIC(10,2) NOT NULL,
    medio_pago      VARCHAR(20) NOT NULL,      -- EFECTIVO | TARJETA | MERCADOPAGO
    estado_pago     VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE | APROBADO | RECHAZADO
    mp_payment_id   VARCHAR(100),              -- ID de pago en MercadoPago (si aplica)
    created_at      TIMESTAMP DEFAULT NOW()
);
```

**Campos adicionales recomendados** — agregar via migración o al crear la entidad JPA:

```sql
ALTER TABLE cobros ADD COLUMN monto_recibido NUMERIC(10,2);   -- Solo para EFECTIVO (monto que entregó el cliente)
ALTER TABLE cobros ADD COLUMN vuelto         NUMERIC(10,2);   -- Solo para EFECTIVO
ALTER TABLE cobros ADD COLUMN mp_link_pago   VARCHAR(500);    -- Link/QR generado por MercadoPago
```

---

## Decisiones de diseño previas

Antes de comenzar el desarrollo, definir estas dos cuestiones:

**1. Datos del taller en el PDF**
El presupuesto PDF necesita el nombre, dirección y teléfono del taller. Usar variables de entorno:
```env
TALLER_NOMBRE=Nombre del Taller
TALLER_DIRECCION=Dirección completa
TALLER_TELEFONO=0351-XXXXXXX
TALLER_EMAIL=contacto@taller.com
```
Inyectar con `@Value` en el `PresupuestoPdfService`.

**2. Unicidad del cobro por orden**
Una orden solo puede tener un cobro aprobado. Agregar validación en el `CobrosService`: si ya existe un cobro con `estado_pago = APROBADO` para esa orden, rechazar el nuevo cobro con una excepción descriptiva (`CobrosException: "La orden ya tiene un cobro aprobado"`).

---

## Bloque 1 — Backend base: entidad, repositorio y lógica de cobros

### 1.1 Entidad JPA `Cobro`

**Archivo:** `backend/src/main/java/com/tallersoft/model/Cobro.java`

```java
@Entity
@Table(name = "cobros")
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "monto_recibido")
    private BigDecimal montoRecibido;      // Solo EFECTIVO

    @Column
    private BigDecimal vuelto;             // Solo EFECTIVO

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;           // Enum: EFECTIVO, TARJETA, MERCADOPAGO

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago")
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;  // Enum: PENDIENTE, APROBADO, RECHAZADO

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    @Column(name = "mp_link_pago")
    private String mpLinkPago;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // getters y setters
}
```

**Enums necesarios:**

```java
// MedioPago.java
public enum MedioPago { EFECTIVO, TARJETA, MERCADOPAGO }

// EstadoPago.java
public enum EstadoPago { PENDIENTE, APROBADO, RECHAZADO }
```

### 1.2 DTOs

**Archivo:** `backend/src/main/java/com/tallersoft/dto/cobros/`

```java
// CobrarOrdenRequest.java
public class CobrarOrdenRequest {
    @NotNull
    private Long ordenId;

    @NotNull @Positive
    private BigDecimal monto;

    private BigDecimal montoRecibido;  // Requerido si medioPago == EFECTIVO

    @NotNull
    private MedioPago medioPago;
}

// CobroResponse.java
public class CobroResponse {
    private Long id;
    private Long ordenId;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private MedioPago medioPago;
    private EstadoPago estadoPago;
    private String mpLinkPago;         // Populated si medioPago == MERCADOPAGO
    private LocalDateTime createdAt;
}

// CajaDiariaResponse.java
public class CajaDiariaResponse {
    private LocalDate fecha;
    private BigDecimal totalDia;
    private Integer cantidadOrdenes;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTarjeta;
    private BigDecimal totalMercadoPago;
    private List<CobroResponse> cobrosDelDia;
}
```

### 1.3 Repository

**Archivo:** `backend/src/main/java/com/tallersoft/repository/CobrosRepository.java`

```java
public interface CobrosRepository extends JpaRepository<Cobro, Long> {

    Optional<Cobro> findByOrdenId(Long ordenId);

    List<Cobro> findByCreatedAtBetween(LocalDateTime desde, LocalDateTime hasta);

    Optional<Cobro> findByOrdenIdAndEstadoPago(Long ordenId, EstadoPago estado);

    Optional<Cobro> findByMpPaymentId(String mpPaymentId);

    List<Cobro> findByEstadoPagoAndCreatedAtBetween(
        EstadoPago estado, LocalDateTime desde, LocalDateTime hasta
    );
}
```

### 1.4 Service — lógica de negocio

**Archivo:** `backend/src/main/java/com/tallersoft/service/CobrosService.java`

El servicio es el corazón del módulo. Implementar los siguientes métodos:

**`registrarCobro(CobrarOrdenRequest request)`**

```
1. Validar que la orden existe y está en estado LISTO (si no, lanzar excepción)
2. Validar que no existe ya un cobro APROBADO para esa orden
3. Según medioPago:
   - EFECTIVO:
       a. Validar que montoRecibido >= monto (si no, lanzar excepción)
       b. Crear Cobro con estadoPago = APROBADO
       c. Calcular vuelto = montoRecibido - monto
       d. Cambiar estado de la orden a ENTREGADO
       (Todo en @Transactional)
   - TARJETA:
       a. Crear Cobro con estadoPago = APROBADO
       b. Cambiar estado de la orden a ENTREGADO
       (Todo en @Transactional)
   - MERCADOPAGO:
       a. Crear Cobro con estadoPago = PENDIENTE
       b. Llamar a MercadoPagoService.generarLinkPago(ordenId, monto, descripcion)
       c. Guardar el link en cobro.mpLinkPago
       d. NO cambiar el estado de la orden (queda en LISTO)
       (Todo en @Transactional)
4. Guardar cobro y retornar CobroResponse
```

**`confirmarPagoManual(Long cobroId)`** — solo para TARJETA si se necesita confirmación posterior

**`getCajaDiaria(LocalDate fecha)`**

```
1. Calcular desde = fecha a las 00:00:00, hasta = fecha a las 23:59:59
2. Buscar todos los cobros con estadoPago = APROBADO en ese rango
3. Calcular totales por medio de pago
4. Retornar CajaDiariaResponse
```

**`procesarPagoAprobadoMercadoPago(String mpPaymentId)`** — llamado desde el webhook (ver Bloque 2)

```
1. Consultar la API de MercadoPago con el mpPaymentId para obtener estado y external_reference
2. Si estado == "approved":
    a. Buscar cobro por mpPaymentId o por ordenId (external_reference)
    b. Actualizar cobro.estadoPago = APROBADO y cobro.mpPaymentId
    c. Cambiar estado de la orden a ENTREGADO
    (Todo en @Transactional)
3. Si estado == "rejected":
    a. Actualizar cobro.estadoPago = RECHAZADO
```

### 1.5 Controller

**Archivo:** `backend/src/main/java/com/tallersoft/controller/CobrosController.java`

```java
@RestController
@RequestMapping("/api/cobros")
public class CobrosController {

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CobroResponse> registrarCobro(@Valid @RequestBody CobrarOrdenRequest request) { ... }

    @GetMapping("/caja-diaria")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CajaDiariaResponse> getCajaDiaria(
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fecha
        // Si fecha es null, usar LocalDate.now()
    ) { ... }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public ResponseEntity<CobroResponse> confirmarPagoManual(@PathVariable Long id) { ... }

    @GetMapping("/ordenes/{ordenId}/presupuesto-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'TECNICO')")
    public ResponseEntity<byte[]> generarPresupuestoPdf(@PathVariable Long ordenId) { ... }
}
```

**Controller del webhook** — archivo separado:

```java
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    // SIN @PreAuthorize — endpoint público
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
        @RequestParam String type,
        @RequestParam("data.id") String paymentId,
        @RequestHeader("x-signature") String signature,
        @RequestHeader("x-request-id") String requestId
    ) { ... }
}
```

### 1.6 Excepciones custom

Crear en `backend/src/main/java/com/tallersoft/exception/`:

```java
// CobrosException.java — para errores de negocio del módulo
public class CobrosException extends RuntimeException {
    public CobrosException(String message) { super(message); }
}
```

Registrar en el `GlobalExceptionHandler` existente para que devuelva `400 Bad Request` con mensaje descriptivo.

---

## Bloque 2 — Integración MercadoPago

### 2.1 Configuración de credenciales

**Dependencia Maven** (agregar en `backend/pom.xml`):

```xml
<dependency>
    <groupId>com.mercadopago</groupId>
    <artifactId>sdk-java</artifactId>
    <version>2.1.7</version>
</dependency>
```

**Variables de entorno** para desarrollo con sandbox:
```env
MP_ACCESS_TOKEN=TEST-XXXXXXXXXXXX   # Token de PRUEBA (empieza con TEST-)
MP_PUBLIC_KEY=TEST-XXXXXXXXXXXX
MP_WEBHOOK_SECRET=tu_webhook_secret
WEBHOOK_BASE_URL=https://abc123.ngrok.io  # URL pública temporal (ver sección ngrok)
```

Para producción, reemplazar los tokens `TEST-` por los tokens productivos.

### 2.2 Configuración de ngrok para pruebas del webhook

El webhook de MercadoPago necesita una URL pública accesible desde internet. En desarrollo local usar ngrok:

```bash
# Instalar ngrok
# macOS: brew install ngrok
# Windows/Linux: descargar desde https://ngrok.com/download

# Exponer el gateway local (puerto 8080)
ngrok http 8080

# ngrok muestra algo como:
# Forwarding  https://abc123.ngrok-free.app -> http://localhost:8080

# Copiar la URL https y setearla en WEBHOOK_BASE_URL del .env
```

> **Importante:** cada vez que se reinicia ngrok, la URL cambia. Actualizar `WEBHOOK_BASE_URL` en el `.env` y reiniciar el backend. Alternativamente, ngrok tiene planes pagos con URL fija.

### 2.3 `MercadoPagoService`

**Archivo:** `backend/src/main/java/com/tallersoft/service/MercadoPagoService.java`

```java
@Service
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${webhook.base-url}")
    private String webhookBaseUrl;

    public String generarLinkPago(Long ordenId, BigDecimal monto, String descripcion) {
        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
            .title(descripcion)
            .quantity(1)
            .unitPrice(monto)
            .build();

        PreferenceRequest request = PreferenceRequest.builder()
            .items(List.of(item))
            .externalReference(String.valueOf(ordenId))
            .notificationUrl(webhookBaseUrl + "/api/pagos/webhook")
            .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(request);

        // En sandbox usar getSandboxInitPoint(), en producción getInitPoint()
        return preference.getSandboxInitPoint();
    }

    public Map<String, Object> consultarPago(String paymentId) {
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        Payment payment = client.get(Long.parseLong(paymentId));
        return Map.of(
            "status", payment.getStatus(),
            "externalReference", payment.getExternalReference(),
            "mpPaymentId", payment.getId().toString()
        );
    }
}
```

### 2.4 Validación de firma del webhook

La firma viene en el header `x-signature` con formato `ts=<timestamp>,v1=<hash>`. Implementar la validación con HMAC-SHA256:

```java
@Component
public class MercadoPagoWebhookValidator {

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    public void validar(String signature, String requestId, String paymentId) {
        // Extraer ts y v1 del header x-signature
        String ts = extraerValor(signature, "ts");
        String v1 = extraerValor(signature, "v1");

        // Construir el mensaje a verificar
        String mensaje = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";

        // Calcular HMAC-SHA256
        String hashCalculado = calcularHmac(mensaje, webhookSecret);

        if (!hashCalculado.equals(v1)) {
            throw new SecurityException("Firma de webhook de MercadoPago inválida");
        }
    }

    private String calcularHmac(String mensaje, String secreto) {
        // Usar javax.crypto.Mac con "HmacSHA256"
        // Retornar resultado en hexadecimal
    }
}
```

> Si la firma no es válida, el controller debe retornar `400 Bad Request` y NO procesar el pago.

### 2.5 Flujo completo del webhook

```
MercadoPago POST /api/pagos/webhook
        ↓
PagoController.recibirWebhook()
    1. Validar firma x-signature (lanzar excepción si inválida)
    2. Verificar que type == "payment"
    3. Llamar a cobrosService.procesarPagoAprobadoMercadoPago(paymentId)
        ↓
CobrosService.procesarPagoAprobadoMercadoPago()
    1. Consultar MercadoPagoService.consultarPago(paymentId)
    2. Obtener status y externalReference (= ordenId)
    3. Si status == "approved":
        - Buscar cobro por mp_payment_id o por orden_id
        - cobro.estadoPago = APROBADO
        - cobro.mpPaymentId = paymentId
        - orden.estado = ENTREGADO
        - Guardar en @Transactional
    4. Si status == "rejected":
        - cobro.estadoPago = RECHAZADO
        ↓
PagoController retorna 200 OK (siempre, para que MP no reintente)
```

> MercadoPago reintenta el webhook si no recibe un `200 OK`. Siempre retornar `200` aunque el pago haya sido rechazado.

---

## Bloque 3 — Generación y previsualización de PDF

### 3.1 Dependencia iText 7

Agregar en `backend/pom.xml`:

```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```

### 3.2 `PresupuestoPdfService`

**Archivo:** `backend/src/main/java/com/tallersoft/service/PresupuestoPdfService.java`

El método principal genera el PDF en memoria y retorna `byte[]`. El controller lo envuelve en `ResponseEntity<byte[]>`.

**Contenido del PDF:**

```
┌─────────────────────────────────────┐
│  LOGO/NOMBRE DEL TALLER             │
│  Dirección | Teléfono | Email        │
│                        Fecha: XX/XX  │
├─────────────────────────────────────┤
│  PRESUPUESTO N° {ordenId}           │
├─────────────────────────────────────┤
│  CLIENTE                            │
│  Nombre: ...  Teléfono: ...         │
│  Email: ...                         │
├─────────────────────────────────────┤
│  EQUIPO                             │
│  Tipo: ...  Marca: ...  Modelo: ... │
│  N° Serie: ...                      │
├─────────────────────────────────────┤
│  DESCRIPCIÓN DEL TRABAJO            │
│  Falla reportada: ...               │
│  Diagnóstico: ...                   │
├─────────────────────────────────────┤
│  REPUESTOS Y MANO DE OBRA           │
│  Item | Cant | Precio Unit | Subtotal│
│  ...                                │
├─────────────────────────────────────┤
│                  TOTAL: $XXXXX      │
└─────────────────────────────────────┘
```

```java
@Service
public class PresupuestoPdfService {

    @Value("${taller.nombre}")
    private String tallerNombre;

    @Value("${taller.direccion}")
    private String tallerDireccion;

    @Value("${taller.telefono}")
    private String tallerTelefono;

    @Value("${taller.email}")
    private String tallerEmail;

    public byte[] generarPresupuesto(Long ordenId) {
        // 1. Obtener la orden con cliente, equipo y repuestos (usar OrdenRepository)
        // 2. Crear PdfWriter con ByteArrayOutputStream
        // 3. Construir el documento con iText
        // 4. Retornar baos.toByteArray()
    }
}
```

**Esqueleto iText:**

```java
ByteArrayOutputStream baos = new ByteArrayOutputStream();
PdfWriter writer = new PdfWriter(baos);
PdfDocument pdf = new PdfDocument(writer);
Document document = new Document(pdf, PageSize.A4);
document.setMargins(40, 40, 40, 40);

// Encabezado del taller
document.add(new Paragraph(tallerNombre)
    .setFontSize(18).setBold());
document.add(new Paragraph(tallerDireccion + " | Tel: " + tallerTelefono)
    .setFontSize(10));

// Tabla de repuestos
Table tabla = new Table(new float[]{4, 1, 2, 2});
tabla.setWidth(UnitValue.createPercentValue(100));
// ... agregar filas

document.add(tabla);
document.close();
return baos.toByteArray();
```

### 3.3 Endpoint del PDF en el controller

```java
@GetMapping("/ordenes/{ordenId}/presupuesto-pdf")
public ResponseEntity<byte[]> generarPresupuestoPdf(@PathVariable Long ordenId) {
    byte[] pdf = presupuestoPdfService.generarPresupuesto(ordenId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"presupuesto-orden-" + ordenId + ".pdf\"")
        .body(pdf);
}
```

> Usar `Content-Disposition: inline` (no `attachment`) para que el browser lo muestre en lugar de descargarlo directamente. El usuario puede elegir descargarlo desde el visor.

### 3.4 Previsualización en Angular

**En el service Angular** (`cobros.service.ts`):

```typescript
generarPresupuesto(ordenId: number): Observable<Blob> {
  return this.http.get(`/api/cobros/ordenes/${ordenId}/presupuesto-pdf`, {
    responseType: 'blob'
  });
}
```

**En el componente** (`cobrar-orden.component.ts`):

```typescript
pdfUrl: SafeResourceUrl | null = null;

previsualizarPDF(ordenId: number): void {
  this.cobrosService.generarPresupuesto(ordenId).subscribe({
    next: (blob) => {
      const objectUrl = URL.createObjectURL(blob);
      this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
    },
    error: () => this.snackBar.open('Error al generar el presupuesto', 'Cerrar')
  });
}

descargarPDF(ordenId: number): void {
  this.cobrosService.generarPresupuesto(ordenId).subscribe({
    next: (blob) => {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `presupuesto-orden-${ordenId}.pdf`;
      link.click();
    }
  });
}
```

**En el template** (`cobrar-orden.component.html`):

```html
<!-- Botones de acción -->
<button mat-stroked-button (click)="previsualizarPDF(orden.id)">
  <mat-icon>visibility</mat-icon> Ver Presupuesto
</button>
<button mat-stroked-button (click)="descargarPDF(orden.id)">
  <mat-icon>download</mat-icon> Descargar PDF
</button>

<!-- Previsualización embebida -->
<div *ngIf="pdfUrl" class="pdf-preview-container">
  <iframe [src]="pdfUrl" width="100%" height="600px" frameborder="0"></iframe>
</div>
```

---

## Bloque 4 — Frontend Angular

### 4.1 Estructura del módulo

```
frontend/src/app/modules/caja/
├── caja.routes.ts
├── components/
│   ├── cobrar-orden/
│   │   ├── cobrar-orden.component.ts
│   │   ├── cobrar-orden.component.html
│   │   └── cobrar-orden.component.scss
│   ├── caja-diaria/
│   │   ├── caja-diaria.component.ts
│   │   ├── caja-diaria.component.html
│   │   └── caja-diaria.component.scss
│   └── estado-pago-mp/              ← Badge/dialog para cobros PENDIENTE de MP
│       ├── estado-pago-mp.component.ts
│       └── estado-pago-mp.component.html
└── services/
    └── cobros.service.ts
```

### 4.2 Routing

**Archivo:** `frontend/src/app/modules/caja/caja.routes.ts`

```typescript
export const cajaRoutes: Routes = [
  { path: 'cobrar/:ordenId', component: CobrarOrdenComponent, canActivate: [AuthGuard] },
  { path: 'caja-diaria',     component: CajaDiariaComponent,  canActivate: [AuthGuard] },
];
```

Registrar en `app.routes.ts`:
```typescript
{
  path: 'caja',
  loadChildren: () => import('./modules/caja/caja.routes').then(m => m.cajaRoutes)
}
```

### 4.3 `CobrosService` Angular

**Archivo:** `frontend/src/app/modules/caja/services/cobros.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class CobrosService {

  constructor(private http: HttpClient) {}

  registrarCobro(request: CobrarOrdenRequest): Observable<CobroResponse> {
    return this.http.post<CobroResponse>('/api/cobros', request);
  }

  getCajaDiaria(fecha?: string): Observable<CajaDiariaResponse> {
    const params = fecha ? { fecha } : {};
    return this.http.get<CajaDiariaResponse>('/api/cobros/caja-diaria', { params });
  }

  generarPresupuesto(ordenId: number): Observable<Blob> {
    return this.http.get(`/api/cobros/ordenes/${ordenId}/presupuesto-pdf`, {
      responseType: 'blob'
    });
  }
}
```

### 4.4 Vista 1 — Cobrar orden (`cobrar-orden.component`)

Esta vista se accede desde el detalle de una orden en estado `LISTO`. Recibe el `ordenId` por parámetro de ruta.

**Comportamiento según medio de pago:**

```
EFECTIVO:
  - Campo "Monto a cobrar" (prellenado con orden.presupuesto)
  - Campo "Monto recibido" (input del recepcionista)
  - Display reactivo del vuelto = montoRecibido - monto (actualizar con valueChanges)
  - Botón "Registrar cobro" → POST /api/cobros → éxito → navegar a detalle de orden

TARJETA:
  - Campo "Monto a cobrar" (prellenado)
  - Texto: "Confirmar que el posnet aprobó el pago"
  - Botón "Confirmar cobro" → POST /api/cobros → éxito → navegar a detalle de orden

MERCADOPAGO:
  - Campo "Monto a cobrar" (prellenado)
  - Botón "Generar link de pago" → POST /api/cobros → recibir CobroResponse con mpLinkPago
  - Mostrar QR o link en un mat-dialog
  - El cobro queda PENDIENTE hasta recibir confirmación de MP
  - Badge visual en la orden indicando "Pago pendiente de confirmación"
```

**Lógica del formulario (template-driven o reactive form):**

```typescript
medioPagoSeleccionado: MedioPago | null = null;
vuelto: number = 0;

onMontoRecibidoChange(montoRecibido: number): void {
  this.vuelto = montoRecibido - this.orden.presupuesto;
}

onSubmit(): void {
  const request: CobrarOrdenRequest = {
    ordenId: this.orden.id,
    monto: this.orden.presupuesto,
    montoRecibido: this.form.get('montoRecibido')?.value,
    medioPago: this.medioPagoSeleccionado!
  };

  this.cobrosService.registrarCobro(request).subscribe({
    next: (cobro) => {
      if (cobro.medioPago === 'MERCADOPAGO') {
        this.abrirDialogMercadoPago(cobro.mpLinkPago!);
      } else {
        this.snackBar.open('Cobro registrado exitosamente', '', { duration: 3000 });
        this.router.navigate(['/ordenes', this.orden.id]);
      }
    },
    error: (err) => this.snackBar.open(err.error.message || 'Error al registrar cobro', 'Cerrar')
  });
}
```

### 4.5 Vista 2 — Caja diaria (`caja-diaria.component`)

```html
<!-- Selector de fecha -->
<mat-form-field>
  <mat-label>Fecha</mat-label>
  <input matInput [matDatepicker]="picker" [(ngModel)]="fechaSeleccionada"
         (dateChange)="cargarCajaDiaria()">
  <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
  <mat-datepicker #picker></mat-datepicker>
</mat-form-field>

<!-- Cards de resumen -->
<div class="resumen-cards">
  <mat-card>
    <mat-card-title>Total del día</mat-card-title>
    <mat-card-content>{{ cajaDiaria?.totalDia | currency:'ARS' }}</mat-card-content>
  </mat-card>
  <mat-card>
    <mat-card-title>Efectivo</mat-card-title>
    <mat-card-content>{{ cajaDiaria?.totalEfectivo | currency:'ARS' }}</mat-card-content>
  </mat-card>
  <mat-card>
    <mat-card-title>Tarjeta</mat-card-title>
    <mat-card-content>{{ cajaDiaria?.totalTarjeta | currency:'ARS' }}</mat-card-content>
  </mat-card>
  <mat-card>
    <mat-card-title>MercadoPago</mat-card-title>
    <mat-card-content>{{ cajaDiaria?.totalMercadoPago | currency:'ARS' }}</mat-card-content>
  </mat-card>
</div>

<!-- Tabla de cobros del día -->
<mat-table [dataSource]="cajaDiaria?.cobrosDelDia">
  <ng-container matColumnDef="orden">...</ng-container>
  <ng-container matColumnDef="cliente">...</ng-container>
  <ng-container matColumnDef="monto">...</ng-container>
  <ng-container matColumnDef="medioPago">...</ng-container>
  <ng-container matColumnDef="estado">
    <!-- Badge con color según estadoPago -->
  </ng-container>
</mat-table>
```

### 4.6 Acceso desde el módulo de Órdenes

En el componente de detalle de orden (`ordenes/`), agregar un botón que solo se muestre cuando `orden.estado === 'LISTO'`:

```html
<button mat-raised-button color="primary"
        *ngIf="orden.estado === 'LISTO'"
        [routerLink]="['/caja/cobrar', orden.id]">
  <mat-icon>payments</mat-icon> Registrar cobro
</button>

<button mat-stroked-button
        [routerLink]="['/caja/cobrar', orden.id]"
        *ngIf="orden.estado !== 'ENTREGADO'">
  <mat-icon>description</mat-icon> Ver / Descargar presupuesto
</button>
```

---

## Reglas de negocio

Estas reglas deben implementarse en el `CobrosService` y lanzar excepciones descriptivas si no se cumplen:

| Regla | Dónde validar |
|---|---|
| Solo se puede cobrar una orden en estado `LISTO` | `CobrosService.registrarCobro()` |
| No puede existir ya un cobro `APROBADO` para la misma orden | `CobrosService.registrarCobro()` |
| Para EFECTIVO: `montoRecibido` >= `monto` | `CobrosService.registrarCobro()` |
| Un cobro `APROBADO` no puede modificarse ni eliminarse | `CobrosService` (validar antes de cualquier update) |
| Al aprobar un cobro, la orden pasa a `ENTREGADO` en la misma transacción | `CobrosService` con `@Transactional` |
| El webhook de MP debe validar la firma `x-signature` antes de procesar | `MercadoPagoWebhookValidator` |
| El endpoint del webhook es público (sin JWT) | `SecurityConfig` — ya configurado en el README original |

---

## Endpoints del módulo

### Core Service (Spring Boot — puerto 8081, ruteado por Gateway en 8080)

```
POST   /api/cobros                              → Registrar cobro (EFECTIVO | TARJETA | MERCADOPAGO)
GET    /api/cobros/caja-diaria?fecha=YYYY-MM-DD → Resumen del día (fecha opcional, default hoy)
POST   /api/cobros/{id}/confirmar               → Confirmar pago manual
GET    /api/cobros/ordenes/{ordenId}/presupuesto-pdf → Generar PDF (retorna application/pdf)
POST   /api/pagos/webhook                       → Webhook MercadoPago (público, sin JWT)
```

### Analytics Service (Python — puerto 8082)

Los siguientes endpoints ya estaban planificados y deben alimentarse con datos de cobros:

```
GET    /analytics/caja/resumen-diario      → Ingresos del día por medio de pago
GET    /analytics/caja/evolucion-mensual   → Ingresos mes a mes (para el dashboard)
```

---

## Tests requeridos

### Backend (JUnit 5 + Mockito)

**`CobrosServiceTest.java`** — al menos los siguientes casos:

```
✓ registrarCobro_efectivo_exitoso()
    → orden en LISTO, montoRecibido suficiente
    → cobro aprobado, vuelto calculado, orden en ENTREGADO

✓ registrarCobro_efectivo_montoInsuficiente()
    → montoRecibido < monto → lanzar CobrosException

✓ registrarCobro_ordenNoLista()
    → orden en estado PENDIENTE o EN_PROCESO → lanzar excepción

✓ registrarCobro_cobroYaAprobado()
    → ya existe cobro APROBADO para la orden → lanzar CobrosException

✓ registrarCobro_tarjeta_exitoso()
    → cobro aprobado, orden en ENTREGADO

✓ registrarCobro_mercadopago_exitoso()
    → cobro en PENDIENTE, link generado, orden sigue en LISTO

✓ procesarPagoAprobadoMercadoPago_approved()
    → cobro pasa a APROBADO, orden a ENTREGADO

✓ procesarPagoAprobadoMercadoPago_rejected()
    → cobro pasa a RECHAZADO, orden no cambia

✓ getCajaDiaria_retornaTotalesCorrectos()
    → verificar suma y desglose por medio de pago
```

**`MercadoPagoWebhookValidatorTest.java`**

```
✓ validar_firmaCorrecta_noLanzaExcepcion()
✓ validar_firmaIncorrecta_lanzaSecurityException()
```

### Frontend (Jasmine + Karma)

```
✓ CobrosService: registrarCobro() hace POST a /api/cobros con body correcto
✓ CobrosService: generarPresupuesto() hace GET con responseType: blob
✓ CobrarOrdenComponent: calcula vuelto reactivamente al cambiar montoRecibido
✓ CajaDiariaComponent: muestra totales correctamente al recibir CajaDiariaResponse
```

---

## Orden de implementación

| Día | Tarea |
|---|---|
| **1** | Entidad `Cobro`, enums `MedioPago`/`EstadoPago`, campos adicionales en BD |
| **2** | DTOs, `CobrosRepository`, estructura de `CobrosService` (métodos vacíos con sus firmas) |
| **3** | Implementar lógica de EFECTIVO y TARJETA en `CobrosService` + `CobrosController` |
| **3** | Tests unitarios de los casos de EFECTIVO y TARJETA |
| **4** | Configurar ngrok + credenciales sandbox de MercadoPago |
| **4** | `MercadoPagoService`: `generarLinkPago()` y `consultarPago()` |
| **5** | `PagoController` + `MercadoPagoWebhookValidator` + lógica del webhook en `CobrosService` |
| **5** | Probar webhook end-to-end con ngrok y sandbox de MP |
| **6** | `PresupuestoPdfService` con iText 7 (contenido completo del PDF) |
| **6** | Endpoint `GET /api/cobros/ordenes/{ordenId}/presupuesto-pdf` |
| **7** | `CobrosService` Angular + `CobrarOrdenComponent` (los tres medios de pago) |
| **8** | Previsualización del PDF en Angular (`iframe` + `SafeResourceUrl`) |
| **8** | `CajaDiariaComponent` con selector de fecha y tabla de cobros |
| **9** | Integrar botón "Registrar cobro" en el detalle de orden (módulo Órdenes) |
| **9** | Tests de frontend + ajustes de UX |
| **10** | Testing integral end-to-end + bug fixing + documentación |

---

## Variables de entorno nuevas

Agregar al `.env` y al `.env.example`:

```env
# ── Datos del taller (para el PDF) ─────────────────
TALLER_NOMBRE=TallerSoft Demo
TALLER_DIRECCION=Av. Colón 1234, Córdoba
TALLER_TELEFONO=0351-4123456
TALLER_EMAIL=contacto@tallersoft.com

# ── MercadoPago (ya existentes, agregar WEBHOOK_BASE_URL) ──
MP_ACCESS_TOKEN=TEST-XXXXXXXXXXXX
MP_PUBLIC_KEY=TEST-XXXXXXXXXXXX
MP_WEBHOOK_SECRET=tu_webhook_secret
WEBHOOK_BASE_URL=https://abc123.ngrok-free.app   # Cambiar con cada sesión de ngrok
```

Agregar en `backend/src/main/resources/application.yml`:

```yaml
taller:
  nombre: ${TALLER_NOMBRE}
  direccion: ${TALLER_DIRECCION}
  telefono: ${TALLER_TELEFONO}
  email: ${TALLER_EMAIL}

mercadopago:
  access-token: ${MP_ACCESS_TOKEN}
  webhook-secret: ${MP_WEBHOOK_SECRET}

webhook:
  base-url: ${WEBHOOK_BASE_URL}
```

---

## Checklist de completitud

Usar esta lista para verificar que el módulo está terminado antes de pasar al Sprint 4:

### Backend
- [ ] Entidad `Cobro` con todos los campos y relaciones
- [ ] Enums `MedioPago` y `EstadoPago`
- [ ] `CobrosRepository` con queries custom
- [ ] `CobrosService` — flujo EFECTIVO completo con vuelto
- [ ] `CobrosService` — flujo TARJETA completo
- [ ] `CobrosService` — flujo MERCADOPAGO (genera link, cobro PENDIENTE)
- [ ] `CobrosService` — `procesarPagoAprobadoMercadoPago()` completo
- [ ] `CobrosService` — `getCajaDiaria()` con totales y desglose
- [ ] `CobrosController` con todos los endpoints
- [ ] `MercadoPagoService` — `generarLinkPago()` y `consultarPago()`
- [ ] `PagoController` — webhook público
- [ ] `MercadoPagoWebhookValidator` — validación HMAC-SHA256
- [ ] `PresupuestoPdfService` — PDF completo con todos los datos requeridos
- [ ] `GlobalExceptionHandler` actualizado con `CobrosException`
- [ ] Transaccionalidad correcta (`@Transactional`) en todos los flujos
- [ ] Tests unitarios de `CobrosService` (todos los casos listados)
- [ ] Tests de `MercadoPagoWebhookValidator`

### Frontend
- [ ] `CobrosService` Angular con todos los métodos
- [ ] `CobrarOrdenComponent` — formulario EFECTIVO con cálculo de vuelto
- [ ] `CobrarOrdenComponent` — formulario TARJETA
- [ ] `CobrarOrdenComponent` — flujo MERCADOPAGO con dialog de link/QR
- [ ] Previsualización de PDF con `iframe` y `SafeResourceUrl`
- [ ] Descarga directa del PDF
- [ ] `CajaDiariaComponent` — selector de fecha funcional
- [ ] `CajaDiariaComponent` — cards de resumen y tabla de cobros
- [ ] Botón "Registrar cobro" en detalle de orden (solo visible en estado LISTO)
- [ ] Badge visual para cobros MercadoPago en estado PENDIENTE
- [ ] Tests de Angular para `CobrosService` y componentes principales

### Integración
- [ ] Webhook de MP probado end-to-end con ngrok y sandbox
- [ ] PDF generado correctamente con datos reales de una orden
- [ ] Flujo completo EFECTIVO: cobro → orden ENTREGADO
- [ ] Flujo completo TARJETA: cobro → orden ENTREGADO
- [ ] Flujo completo MERCADOPAGO: cobro PENDIENTE → webhook → APROBADO → orden ENTREGADO
- [ ] Endpoint de caja diaria refleja correctamente los cobros del día
- [ ] Variables de entorno documentadas en `.env.example`

---

*TallerSoft — Trabajo Final Integrador — Tecnicatura Universitaria en Programación — UTN FRC — 2026*