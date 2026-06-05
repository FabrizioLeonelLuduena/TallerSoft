# Plan de Desarrollo — KPIs Extendidos + Notificaciones + Nuevo Dashboard
## TallerSoft · Sprint 4 (extensión)

> **Contexto:** El microservicio de analítica ya está funcionando con los 7 endpoints originales.
> Este plan agrega 7 nuevos KPIs, un sistema de notificaciones en el backend y rediseña el dashboard Angular.
> No se modifican los endpoints existentes. Solo se agregan capas nuevas.

---

## Índice

1. [Resumen de cambios](#1-resumen-de-cambios)
2. [Nuevos KPIs — Backend Python](#2-nuevos-kpis--backend-python)
3. [Sistema de notificaciones y alertas](#3-sistema-de-notificaciones-y-alertas)
4. [Nuevo Dashboard Angular](#4-nuevo-dashboard-angular)
5. [Actualizar contexto del Asistente IA](#5-actualizar-contexto-del-asistente-ia)
6. [Testing](#6-testing)
7. [Checklist final](#7-checklist-final)

---

## 1. Resumen de cambios

### Nuevos endpoints analytics (7)

| Endpoint | Dominio | KPI |
|---|---|---|
| `GET /analytics/ordenes/alta-prioridad` | Órdenes | Alta prioridad pendientes |
| `GET /analytics/ordenes/sin-movimiento` | Órdenes | Órdenes estancadas |
| `GET /analytics/ordenes/tiempo-por-estado` | Órdenes | Embudo de reparación |
| `GET /analytics/caja/rechazos` | Caja | Cobros rechazados |
| `GET /analytics/caja/conversion-presupuesto` | Caja | Conversión presupuesto→cobro |
| `GET /analytics/clientes/recurrencia` | Clientes | Clientes nuevos vs recurrentes |
| `GET /analytics/stock/por-categoria` | Stock | Top categorías de consumo |

### Nuevos endpoints notificaciones (3)

| Endpoint | Descripción |
|---|---|
| `GET /analytics/alertas/activas` | Lista de alertas vigentes priorizadas |
| `GET /analytics/alertas/resumen` | Conteo de alertas por tipo (para el badge del topbar) |
| `POST /analytics/alertas/{id}/marcar-leida` | Marcar una alerta como leída (estado en memoria/BD) |

### Cambios en frontend Angular

- Rediseño completo de `dashboard.component` con tabs temáticos
- Nuevo `AlertasBannerComponent` (barra de chips de alerta)
- Nuevo `NotificacionesPanelComponent` (panel lateral con campana)
- Actualización de `AnalyticsService` con los nuevos endpoints

---

## 2. Nuevos KPIs — Backend Python

### Tarea 2.1 — Nuevos schemas Pydantic

Agregar los siguientes modelos en los schemas existentes:

**`analytics/app/schemas/ordenes_schema.py`** — agregar al final:

```python
class OrdenAltaPrioridad(BaseModel):
    id: int
    cliente_nombre: str
    equipo_tipo: str
    equipo_marca: str | None
    tecnico_nombre: str | None
    estado: str
    dias_sin_avanzar: int        # días desde updated_at

class OrdenSinMovimiento(BaseModel):
    id: int
    cliente_nombre: str
    equipo_tipo: str
    estado: str
    dias_estancada: int          # días desde updated_at
    prioridad: str

class TiempoPorEstado(BaseModel):
    estado: str
    promedio_dias: float
    porcentaje_del_total: float  # % del tiempo total que representa este estado
```

**`analytics/app/schemas/caja_schema.py`** — agregar al final:

```python
class RechazoMedioPago(BaseModel):
    medio_pago: str
    cantidad_rechazos: int
    monto_total_rechazado: float

class ResumenRechazos(BaseModel):
    periodo_dias: int
    total_rechazado: float
    cantidad_rechazos: int
    por_medio: List[RechazoMedioPago]

class ConversionPresupuesto(BaseModel):
    total_con_presupuesto: int
    total_cobradas: int
    total_no_cobradas: int
    tasa_conversion_pct: float   # porcentaje redondeado a 1 decimal
```

**`analytics/app/schemas/clientes_schema.py`** — crear archivo nuevo:

```python
from pydantic import BaseModel

class RecurrenciaClientes(BaseModel):
    mes: str
    total_ordenes: int
    clientes_recurrentes: int    # cliente con más de 1 orden histórica
    clientes_nuevos: int         # primera orden en el sistema
    porcentaje_recurrentes: float
```

**`analytics/app/schemas/alertas_schema.py`** — crear archivo nuevo:

```python
from pydantic import BaseModel
from datetime import datetime
from typing import Literal

TipoAlerta = Literal["danger", "warn", "info", "success"]

class Alerta(BaseModel):
    id: str                      # ej: "orden-sin-movimiento-204"
    tipo: TipoAlerta
    titulo: str
    descripcion: str | None = None
    modulo: str                  # "ordenes" | "stock" | "caja" | "sistema"
    created_at: datetime
    leida: bool = False
    datos_extra: dict = {}       # Para navegación desde la notif (ej: {"orden_id": 204})

class ResumenAlertas(BaseModel):
    total: int
    sin_leer: int
    por_tipo: dict               # {"danger": 2, "warn": 3, "info": 1}
```

---

### Tarea 2.2 — Nuevas funciones en `analytics_service.py`

Agregar al final del archivo `analytics/app/services/analytics_service.py`:

```python
# ─── NUEVOS KPIs ─────────────────────────────────────────────────────────────

def ordenes_alta_prioridad(db: Session, dias_minimos: int = 1) -> list[dict]:
    """
    Órdenes con prioridad ALTA que llevan más de N días sin cambiar de estado
    y no están en estado ENTREGADO.
    """
    sql = f"""
        SELECT
            ot.id,
            c.nombre AS cliente_nombre,
            e.tipo   AS equipo_tipo,
            e.marca  AS equipo_marca,
            u.nombre AS tecnico_nombre,
            ot.estado,
            EXTRACT(DAY FROM NOW() - ot.updated_at)::int AS dias_sin_avanzar
        FROM ordenes_trabajo ot
        JOIN clientes c ON c.id = ot.cliente_id
        JOIN equipos  e ON e.id = ot.equipo_id
        LEFT JOIN usuarios u ON u.id = ot.tecnico_id
        WHERE ot.prioridad = 'ALTA'
          AND ot.estado != 'ENTREGADO'
          AND EXTRACT(DAY FROM NOW() - ot.updated_at) >= {dias_minimos}
        ORDER BY dias_sin_avanzar DESC
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")


def ordenes_sin_movimiento(db: Session, dias_umbral: int = 5) -> list[dict]:
    """
    Todas las órdenes activas (no ENTREGADO) cuyo updated_at lleva más de N días.
    Por defecto: 5 días.
    """
    sql = f"""
        SELECT
            ot.id,
            c.nombre AS cliente_nombre,
            e.tipo   AS equipo_tipo,
            ot.estado,
            ot.prioridad,
            EXTRACT(DAY FROM NOW() - ot.updated_at)::int AS dias_estancada
        FROM ordenes_trabajo ot
        JOIN clientes c ON c.id = ot.cliente_id
        JOIN equipos  e ON e.id = ot.equipo_id
        WHERE ot.estado != 'ENTREGADO'
          AND EXTRACT(DAY FROM NOW() - ot.updated_at) >= {dias_umbral}
        ORDER BY dias_estancada DESC
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")


def tiempo_promedio_por_estado(db: Session) -> list[dict]:
    """
    Tiempo promedio en días que las órdenes pasan en cada estado.
    Usa updated_at como proxy del momento de cambio de estado.
    Solo considera órdenes ENTREGADAS (ciclo completo).

    Nota: Esta métrica es una estimación basada en timestamps disponibles.
    Para mayor precisión se podría agregar una tabla de historial de estados en el futuro.
    """
    # Estimación por estado usando rangos de tiempo
    # PENDIENTE: desde created_at hasta primera actualización
    # EN_PROCESO: mayor concentración de updated_at por estado
    # Para simplificar, calculamos el promedio total y estimamos proporciones
    # a partir de las órdenes actuales en cada estado con su updated_at

    sql = """
        SELECT
            estado,
            AVG(EXTRACT(DAY FROM updated_at - created_at)) AS promedio_dias_acum
        FROM ordenes_trabajo
        WHERE estado = 'ENTREGADO'
        GROUP BY estado

        UNION ALL

        SELECT
            'PENDIENTE_actual' AS estado,
            AVG(EXTRACT(DAY FROM NOW() - created_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'PENDIENTE'

        UNION ALL

        SELECT
            'EN_PROCESO_actual' AS estado,
            AVG(EXTRACT(DAY FROM NOW() - updated_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'EN_PROCESO'

        UNION ALL

        SELECT
            'LISTO_actual' AS estado,
            AVG(EXTRACT(DAY FROM NOW() - updated_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'LISTO'
    """
    df = pd.read_sql(sql, db.bind)
    if df.empty:
        return []

    total = df["promedio_dias_acum"].sum()
    df["porcentaje_del_total"] = round((df["promedio_dias_acum"] / total * 100), 1) if total > 0 else 0
    df = df.rename(columns={"estado": "estado", "promedio_dias_acum": "promedio_dias"})
    df["promedio_dias"] = df["promedio_dias"].round(1)
    return df.to_dict(orient="records")


def rechazos_cobros(db: Session, dias: int = 7) -> dict:
    """
    Cobros en estado RECHAZADO en los últimos N días, agrupados por medio de pago.
    """
    desde = date.today() - timedelta(days=dias)
    sql = f"""
        SELECT medio_pago, monto
        FROM cobros
        WHERE estado_pago = 'RECHAZADO'
          AND created_at >= '{desde}'
    """
    df = pd.read_sql(sql, db.bind)
    if df.empty:
        return {
            "periodo_dias": dias,
            "total_rechazado": 0.0,
            "cantidad_rechazos": 0,
            "por_medio": []
        }

    por_medio = (
        df.groupby("medio_pago")
          .agg(cantidad_rechazos=("monto", "count"), monto_total_rechazado=("monto", "sum"))
          .reset_index()
          .to_dict(orient="records")
    )
    return {
        "periodo_dias":      dias,
        "total_rechazado":   float(df["monto"].sum()),
        "cantidad_rechazos": int(len(df)),
        "por_medio":         por_medio,
    }


def conversion_presupuesto(db: Session) -> dict:
    """
    De las órdenes que tienen presupuesto cargado, cuántas tienen un cobro APROBADO.
    """
    sql = """
        SELECT
            COUNT(*) AS total_con_presupuesto,
            COUNT(DISTINCT c.orden_id) AS total_cobradas
        FROM ordenes_trabajo ot
        LEFT JOIN cobros c ON c.orden_id = ot.id AND c.estado_pago = 'APROBADO'
        WHERE ot.presupuesto IS NOT NULL
    """
    df = pd.read_sql(sql, db.bind)
    total = int(df["total_con_presupuesto"].iloc[0])
    cobradas = int(df["total_cobradas"].iloc[0])
    no_cobradas = total - cobradas
    tasa = round((cobradas / total * 100), 1) if total > 0 else 0.0
    return {
        "total_con_presupuesto": total,
        "total_cobradas":        cobradas,
        "total_no_cobradas":     no_cobradas,
        "tasa_conversion_pct":   tasa,
    }


def recurrencia_clientes(db: Session, meses: int = 6) -> list[dict]:
    """
    Por mes, cuántos clientes tenían historial previo (recurrentes) vs cuántos era su primera orden.
    Un cliente es 'recurrente' en un mes si ya tenía al menos una orden anterior a ese mes.
    """
    desde = date.today() - timedelta(days=meses * 30)
    sql = f"""
        SELECT
            ot.id AS orden_id,
            ot.cliente_id,
            ot.created_at,
            DATE_TRUNC('month', ot.created_at) AS mes,
            (
                SELECT COUNT(*) FROM ordenes_trabajo prev
                WHERE prev.cliente_id = ot.cliente_id
                  AND prev.created_at < ot.created_at
            ) AS ordenes_previas
        FROM ordenes_trabajo ot
        WHERE ot.created_at >= '{desde}'
    """
    df = pd.read_sql(sql, db.bind, parse_dates=["created_at", "mes"])
    if df.empty:
        return []

    df["es_recurrente"] = df["ordenes_previas"] > 0
    df["mes_str"] = df["mes"].dt.to_period("M").astype(str)

    resultado = (
        df.groupby("mes_str")
          .agg(
              total_ordenes=("orden_id", "count"),
              clientes_recurrentes=("es_recurrente", "sum"),
          )
          .reset_index()
    )
    resultado["clientes_nuevos"] = resultado["total_ordenes"] - resultado["clientes_recurrentes"]
    resultado["porcentaje_recurrentes"] = round(
        resultado["clientes_recurrentes"] / resultado["total_ordenes"] * 100, 1
    )
    resultado["clientes_recurrentes"] = resultado["clientes_recurrentes"].astype(int)
    return resultado.rename(columns={"mes_str": "mes"}).to_dict(orient="records")


def stock_por_categoria(db: Session) -> list[dict]:
    """
    Top categorías de repuestos por unidades consumidas en órdenes.
    """
    sql = """
        SELECT
            COALESCE(r.categoria, 'Sin categoría') AS categoria,
            SUM(ore.cantidad) AS total_unidades,
            COUNT(DISTINCT ore.orden_id) AS ordenes_count,
            COUNT(DISTINCT r.id) AS tipos_repuesto
        FROM orden_repuestos ore
        JOIN repuestos r ON r.id = ore.repuesto_id
        GROUP BY r.categoria
        ORDER BY total_unidades DESC
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")
```

---

### Tarea 2.3 — Nuevos routers

**Agregar a `analytics/app/routers/ordenes.py`:**

```python
@router.get("/alta-prioridad")
def ordenes_alta_prioridad(
    dias_minimos: int = Query(1, ge=0),
    db: Session = Depends(get_db)
):
    """Órdenes de ALTA prioridad que llevan N+ días sin avanzar."""
    return svc.ordenes_alta_prioridad(db, dias_minimos)

@router.get("/sin-movimiento")
def ordenes_sin_movimiento(
    dias_umbral: int = Query(5, ge=1),
    db: Session = Depends(get_db)
):
    """Órdenes activas sin cambios en más de N días."""
    return svc.ordenes_sin_movimiento(db, dias_umbral)

@router.get("/tiempo-por-estado")
def tiempo_por_estado(db: Session = Depends(get_db)):
    """Tiempo promedio en días por estado (embudo de reparación)."""
    return svc.tiempo_promedio_por_estado(db)
```

**Agregar a `analytics/app/routers/caja.py`:**

```python
@router.get("/rechazos")
def rechazos_cobros(
    dias: int = Query(7, ge=1, le=90),
    db: Session = Depends(get_db)
):
    """Cobros rechazados en los últimos N días agrupados por medio de pago."""
    return svc.rechazos_cobros(db, dias)

@router.get("/conversion-presupuesto")
def conversion_presupuesto(db: Session = Depends(get_db)):
    """Tasa de conversión de órdenes con presupuesto a cobros aprobados."""
    return svc.conversion_presupuesto(db)
```

**Crear `analytics/app/routers/clientes.py`:**

```python
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()

@router.get("/recurrencia")
def recurrencia_clientes(
    meses: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db)
):
    """Clientes recurrentes vs nuevos por mes."""
    return svc.recurrencia_clientes(db, meses)
```

**Agregar a `analytics/app/routers/stock.py`:**

```python
@router.get("/por-categoria")
def stock_por_categoria(db: Session = Depends(get_db)):
    """Consumo de repuestos agrupado por categoría."""
    return svc.stock_por_categoria(db)
```

---

### Tarea 2.4 — Registrar nuevos routers en `main.py`

```python
# Agregar este import al bloque existente:
from app.routers import ordenes, stock, caja, asistente, clientes, alertas

# Agregar estas líneas después de los routers existentes:
app.include_router(clientes.router, prefix="/analytics/clientes", tags=["Clientes"])
app.include_router(alertas.router,  prefix="/analytics/alertas",  tags=["Alertas"])
```

---

## 3. Sistema de notificaciones y alertas

### Qué tipo de alertas agregar y por qué

El sistema genera alertas automáticamente leyendo los datos en cada request. No hay workers ni cron jobs — las alertas se calculan en tiempo real y son stateless (excepto el estado "leída", que se guarda en memoria). Esto mantiene la arquitectura simple y sin dependencias nuevas.

| Alerta | Trigger | Tipo | Por qué es útil |
|---|---|---|---|
| Orden sin movimiento | `dias_estancada >= 5` | `danger` | Evita que trabajos queden olvidados |
| Orden ALTA prioridad parada | `dias_sin_avanzar >= 2` | `danger` | El cliente con prioridad alta espera más que otros |
| Stock crítico | `stock_actual <= stock_minimo` | `warn` | Evita frenar una reparación por falta de repuesto |
| Cobro rechazado | Nuevo cobro con `estado = RECHAZADO` | `danger` | Requiere acción inmediata para recuperar el cobro |
| Conversión baja | `tasa_conversion < 60%` | `warn` | Indica que muchos presupuestos no se convierten en plata |
| Resumen semanal | Cada lunes (basado en fecha del request) | `info` | Visión ejecutiva sin buscarla manualmente |

---

### Tarea 3.1 — Servicio de alertas

**Crear `analytics/app/services/alertas_service.py`:**

```python
# app/services/alertas_service.py
from sqlalchemy.orm import Session
from datetime import datetime
from app.services import analytics_service as svc

# Estado de alertas leídas en memoria (simple dict)
# En una versión futura se puede persistir en BD o Redis
_alertas_leidas: set[str] = set()

def _leida(alerta_id: str) -> bool:
    return alerta_id in _alertas_leidas

def marcar_leida(alerta_id: str) -> None:
    _alertas_leidas.add(alerta_id)

def generar_alertas(db: Session) -> list[dict]:
    """
    Genera la lista de alertas activas leyendo los datos del taller.
    Cada alerta tiene un ID determinístico basado en sus datos para evitar duplicados.
    """
    alertas = []
    now = datetime.now()

    # ── 1. Órdenes sin movimiento (≥ 5 días) ─────────────────────────────────
    sin_mov = svc.ordenes_sin_movimiento(db, dias_umbral=5)
    for o in sin_mov:
        alerta_id = f"sin-movimiento-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger" if o["dias_estancada"] >= 7 else "warn",
            "titulo":      f"Orden #{o['id']} sin movimiento hace {o['dias_estancada']} días",
            "descripcion": f"{o['equipo_tipo']} — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # ── 2. Alta prioridad paradas (≥ 2 días) ─────────────────────────────────
    alta_prio = svc.ordenes_alta_prioridad(db, dias_minimos=2)
    for o in alta_prio:
        alerta_id = f"alta-prioridad-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"Orden #{o['id']} — ALTA prioridad sin avanzar",
            "descripcion": f"{o['dias_sin_avanzar']} días parada — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # ── 3. Stock crítico ──────────────────────────────────────────────────────
    criticos = svc.stock_critico(db)
    for r in criticos:
        alerta_id = f"stock-critico-{r['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Stock crítico: {r['nombre']}",
            "descripcion": f"Quedan {r['stock_actual']} unidades (mínimo: {r['stock_minimo']})",
            "modulo":      "stock",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {"repuesto_id": r["id"]},
        })

    # ── 4. Cobros rechazados del día ──────────────────────────────────────────
    rechazos = svc.rechazos_cobros(db, dias=1)
    if rechazos["cantidad_rechazos"] > 0:
        alerta_id = f"rechazos-hoy-{now.strftime('%Y-%m-%d')}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"{rechazos['cantidad_rechazos']} cobro(s) rechazado(s) hoy",
            "descripcion": f"Total: ${rechazos['total_rechazado']:,.0f} — Revisar en Caja",
            "modulo":      "caja",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {},
        })

    # ── 5. Conversión baja (< 60%) ────────────────────────────────────────────
    conv = svc.conversion_presupuesto(db)
    if conv["tasa_conversion_pct"] < 60 and conv["total_con_presupuesto"] >= 5:
        alerta_id = f"conversion-baja-{now.strftime('%Y-%m')}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Tasa de conversión baja: {conv['tasa_conversion_pct']}%",
            "descripcion": f"{conv['total_no_cobradas']} presupuestos sin cobrar este mes",
            "modulo":      "caja",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {},
        })

    # Ordenar: danger primero, luego warn, luego info
    orden_tipo = {"danger": 0, "warn": 1, "info": 2, "success": 3}
    alertas.sort(key=lambda a: orden_tipo.get(a["tipo"], 99))

    return alertas


def resumen_alertas(db: Session) -> dict:
    alertas = generar_alertas(db)
    sin_leer = [a for a in alertas if not a["leida"]]
    por_tipo = {}
    for a in sin_leer:
        por_tipo[a["tipo"]] = por_tipo.get(a["tipo"], 0) + 1
    return {
        "total":    len(alertas),
        "sin_leer": len(sin_leer),
        "por_tipo": por_tipo,
    }
```

---

### Tarea 3.2 — Router de alertas

**Crear `analytics/app/routers/alertas.py`:**

```python
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.alertas_service import generar_alertas, resumen_alertas, marcar_leida

router = APIRouter()

@router.get("/activas")
def alertas_activas(db: Session = Depends(get_db)):
    """Lista completa de alertas vigentes ordenadas por severidad."""
    return generar_alertas(db)

@router.get("/resumen")
def alertas_resumen(db: Session = Depends(get_db)):
    """Conteo rápido para el badge de la campana en el topbar."""
    return resumen_alertas(db)

@router.post("/{alerta_id}/marcar-leida")
def marcar_alerta_leida(alerta_id: str):
    """
    Marca una alerta como leída (persiste en memoria mientras el servicio esté corriendo).
    En una versión futura se puede persistir en BD con una tabla 'alertas_leidas'.
    """
    marcar_leida(alerta_id)
    return {"ok": True, "alerta_id": alerta_id}
```

> **Nota:** El estado "leída" se guarda en un `set` en memoria Python. Si el servicio se reinicia, todas las alertas vuelven a aparecer como no leídas. Para una PYME esto es aceptable. Si se quiere persistencia real, agregar una tabla `alertas_leidas(alerta_id VARCHAR, usuario_id BIGINT, created_at TIMESTAMP)` y consultar esa tabla en `alertas_service.py`.

---

## 4. Nuevo Dashboard Angular

### Tarea 4.1 — Actualizar `AnalyticsService`

Agregar los métodos nuevos al archivo existente `frontend/src/app/core/services/analytics.service.ts`:

```typescript
// Órdenes — nuevos métodos
getOrdenesSinMovimiento(diasUmbral = 5): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/ordenes/sin-movimiento`, {
    params: new HttpParams().set('dias_umbral', diasUmbral)
  });
}

getOrdenesAltaPrioridad(diasMinimos = 1): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/ordenes/alta-prioridad`, {
    params: new HttpParams().set('dias_minimos', diasMinimos)
  });
}

getTiempoPorEstado(): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/ordenes/tiempo-por-estado`);
}

// Caja — nuevos métodos
getRechazos(dias = 7): Observable<any> {
  return this.http.get(`${this.base}/caja/rechazos`, {
    params: new HttpParams().set('dias', dias)
  });
}

getConversionPresupuesto(): Observable<any> {
  return this.http.get(`${this.base}/caja/conversion-presupuesto`);
}

getTicketPromedio(): Observable<any> {
  // Calculado en frontend combinando resumen-diario + cantidad de cobros
  // o agregar endpoint específico si se prefiere
  return this.http.get(`${this.base}/caja/resumen-diario`);
}

// Clientes
getRecurrenciaClientes(meses = 6): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/clientes/recurrencia`, {
    params: new HttpParams().set('meses', meses)
  });
}

// Stock
getStockPorCategoria(): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/stock/por-categoria`);
}

// Alertas
getAlertasActivas(): Observable<any[]> {
  return this.http.get<any[]>(`${this.base}/alertas/activas`);
}

getResumenAlertas(): Observable<any> {
  return this.http.get(`${this.base}/alertas/resumen`);
}

marcarAlertaLeida(alertaId: string): Observable<any> {
  return this.http.post(`${this.base}/alertas/${alertaId}/marcar-leida`, {});
}
```

---

### Tarea 4.2 — Estructura de archivos del dashboard

```
frontend/src/app/modules/dashboard/
├── dashboard.component.ts          # Componente principal — tabs y carga de datos
├── dashboard.component.html        # Template con tabs y secciones
├── dashboard.component.scss        # Estilos del dashboard
└── components/
    ├── alertas-banner/
    │   ├── alertas-banner.component.ts
    │   └── alertas-banner.component.html
    ├── notificaciones-panel/
    │   ├── notificaciones-panel.component.ts
    │   └── notificaciones-panel.component.html
    ├── kpi-card/
    │   ├── kpi-card.component.ts    # Componente genérico reutilizable
    │   └── kpi-card.component.html
    └── embudo-reparacion/
        ├── embudo-reparacion.component.ts
        └── embudo-reparacion.component.html
```

---

### Tarea 4.3 — `dashboard.component.ts`

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { forkJoin, interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { AnalyticsService } from '@core/services/analytics.service';

type Tab = 'operativo' | 'financiero' | 'clientes' | 'stock';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  tabActiva: Tab = 'operativo';
  loading = true;
  error = false;

  // Datos de alertas (polled cada 2 minutos)
  alertasActivas: any[] = [];
  resumenAlertas: any = {};
  panelAbierto = false;

  // Tab: Operativo
  resumenOrdenes: any;
  ordenesAltaPrioridad: any[] = [];
  ordenesSinMovimiento: any[] = [];
  tiempoPorEstado: any[] = [];

  // Tab: Financiero
  cajaDiaria: any;
  rechazos: any;
  conversionPresupuesto: any;
  evolucionMensual: any[] = [];
  chartEvolucion: any;

  // Tab: Clientes
  recurrencia: any[] = [];
  chartRecurrencia: any;

  // Tab: Stock
  stockCritico: any[] = [];
  stockPorCategoria: any[] = [];

  private alertasSub!: Subscription;

  constructor(private analytics: AnalyticsService) {}

  ngOnInit(): void {
    this.cargarDashboard();
    // Poll de alertas cada 2 minutos
    this.alertasSub = interval(120_000).pipe(startWith(0), switchMap(() =>
      forkJoin({
        activas: this.analytics.getAlertasActivas(),
        resumen: this.analytics.getResumenAlertas(),
      })
    )).subscribe(({ activas, resumen }) => {
      this.alertasActivas = activas;
      this.resumenAlertas = resumen;
    });
  }

  ngOnDestroy(): void {
    this.alertasSub?.unsubscribe();
  }

  cargarDashboard(): void {
    this.loading = true;
    forkJoin({
      resumenOrdenes:        this.analytics.getResumenOrdenes(),
      altaPrioridad:         this.analytics.getOrdenesAltaPrioridad(),
      sinMovimiento:         this.analytics.getOrdenesSinMovimiento(),
      tiempoPorEstado:       this.analytics.getTiempoPorEstado(),
      cajaDiaria:            this.analytics.getResumenCajaDiario(),
      rechazos:              this.analytics.getRechazos(7),
      conversion:            this.analytics.getConversionPresupuesto(),
      evolucion:             this.analytics.getEvolucionMensual(6),
      recurrencia:           this.analytics.getRecurrenciaClientes(6),
      stockCritico:          this.analytics.getStockCritico(),
      stockPorCategoria:     this.analytics.getStockPorCategoria(),
    }).subscribe({
      next: (d) => {
        this.resumenOrdenes         = d.resumenOrdenes;
        this.ordenesAltaPrioridad   = d.altaPrioridad;
        this.ordenesSinMovimiento   = d.sinMovimiento;
        this.tiempoPorEstado        = d.tiempoPorEstado;
        this.cajaDiaria             = d.cajaDiaria;
        this.rechazos               = d.rechazos;
        this.conversionPresupuesto  = d.conversion;
        this.evolucionMensual       = d.evolucion;
        this.recurrencia            = d.recurrencia;
        this.stockCritico           = d.stockCritico;
        this.stockPorCategoria      = d.stockPorCategoria;
        this.buildCharts(d.evolucion, d.recurrencia);
        this.loading = false;
      },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  setTab(tab: Tab): void { this.tabActiva = tab; }

  togglePanel(): void { this.panelAbierto = !this.panelAbierto; }

  marcarLeida(id: string): void {
    this.analytics.marcarAlertaLeida(id).subscribe(() => {
      const a = this.alertasActivas.find(x => x.id === id);
      if (a) a.leida = true;
      this.resumenAlertas.sin_leer = Math.max(0, (this.resumenAlertas.sin_leer || 1) - 1);
    });
  }

  // Helpers de template
  get alertasDanger(): any[] {
    return this.alertasActivas.filter(a => a.tipo === 'danger' && !a.leida);
  }
  get alertasWarn(): any[] {
    return this.alertasActivas.filter(a => a.tipo === 'warn' && !a.leida);
  }
  get tiempoTotalDias(): number {
    return this.tiempoPorEstado.reduce((s, e) => s + (e.promedio_dias || 0), 0);
  }
  get embudo(): any[] {
    // Ordena el embudo en el orden lógico del flujo
    const orden = ['PENDIENTE_actual', 'EN_PROCESO_actual', 'LISTO_actual', 'ENTREGADO'];
    return [...this.tiempoPorEstado].sort((a, b) =>
      orden.indexOf(a.estado) - orden.indexOf(b.estado)
    );
  }
  estadoLabel(estado: string): string {
    const map: Record<string, string> = {
      'PENDIENTE_actual': 'Pendiente',
      'EN_PROCESO_actual': 'En proceso',
      'LISTO_actual': 'Listo',
      'ENTREGADO': 'Total (cerradas)',
    };
    return map[estado] || estado;
  }

  private buildCharts(evolucion: any[], recurrencia: any[]): void {
    this.chartEvolucion = {
      series: [{ name: 'Ingresos', data: evolucion.map(e => e.total_ingresos) }],
      chart: { type: 'bar', height: 260, toolbar: { show: false }, background: 'transparent' },
      theme: { mode: 'dark' },
      colors: ['#f97316'],
      plotOptions: { bar: { borderRadius: 4, columnWidth: '55%' } },
      xaxis: { categories: evolucion.map(e => e.mes), labels: { style: { colors: '#6b7280' } } },
      yaxis: { labels: { formatter: (v: number) => `$${(v/1000).toFixed(0)}k`, style: { colors: '#6b7280' } } },
      grid: { borderColor: '#2a3045' },
      tooltip: { y: { formatter: (v: number) => `$${v.toLocaleString('es-AR')}` } },
    };

    this.chartRecurrencia = {
      series: [
        { name: 'Recurrentes', data: recurrencia.map(r => r.clientes_recurrentes) },
        { name: 'Nuevos',      data: recurrencia.map(r => r.clientes_nuevos) },
      ],
      chart: { type: 'bar', stacked: true, height: 260, toolbar: { show: false }, background: 'transparent' },
      theme: { mode: 'dark' },
      colors: ['#22c55e', '#3b82f6'],
      plotOptions: { bar: { borderRadius: 4, columnWidth: '55%' } },
      xaxis: { categories: recurrencia.map(r => r.mes), labels: { style: { colors: '#6b7280' } } },
      grid: { borderColor: '#2a3045' },
    };
  }
}
```

---

### Tarea 4.4 — `dashboard.component.html` (estructura)

El template implementa la estructura definida en el mockup. Las secciones clave:

**Barra de alertas** (siempre visible, sobre los tabs):
```html
<!-- Chips de alertas activas -->
<div class="alert-bar" *ngIf="alertasDanger.length || alertasWarn.length">
  <div class="alert-chip danger" *ngIf="alertasDanger.length">
    <mat-icon>error</mat-icon>
    {{ alertasDanger.length }} alertas críticas
  </div>
  <div class="alert-chip warn" *ngIf="alertasWarn.length">
    <mat-icon>warning</mat-icon>
    {{ alertasWarn.length }} advertencias
  </div>
</div>
```

**Tabs de navegación:**
```html
<mat-tab-group [(selectedIndex)]="tabIndex" animationDuration="150ms">
  <mat-tab label="Operativo">  <!-- Tab 1 --></mat-tab>
  <mat-tab label="Financiero"> <!-- Tab 2 --></mat-tab>
  <mat-tab label="Clientes">   <!-- Tab 3 --></mat-tab>
  <mat-tab label="Stock">      <!-- Tab 4 --></mat-tab>
</mat-tab-group>
```

**Tab Operativo — KPIs:**
```html
<div class="grid-kpi">
  <app-kpi-card titulo="Órdenes activas"    [valor]="resumenOrdenes?.total"       color="orange"></app-kpi-card>
  <app-kpi-card titulo="Alta prioridad"     [valor]="ordenesAltaPrioridad.length" color="danger"  [alerta]="true"></app-kpi-card>
  <app-kpi-card titulo="Sin movimiento"     [valor]="ordenesSinMovimiento.length" color="warn"></app-kpi-card>
  <app-kpi-card titulo="Listas p/ entregar" [valor]="resumenOrdenes?.listas"      color="blue"></app-kpi-card>
</div>
```

**Embudo de reparación** (componente `app-embudo-reparacion`):
```html
<app-embudo-reparacion
  [datos]="embudo"
  [tiempoTotal]="tiempoTotalDias">
</app-embudo-reparacion>
```

**Tabla órdenes sin movimiento:**
```html
<table mat-table [dataSource]="ordenesSinMovimiento">
  <ng-container matColumnDef="id">...</ng-container>
  <ng-container matColumnDef="estado">...</ng-container>
  <ng-container matColumnDef="dias">
    <mat-cell *matCellDef="let o">
      <span [class]="o.dias_estancada >= 7 ? 'badge-danger' : 'badge-warn'">
        {{ o.dias_estancada }}d
      </span>
    </mat-cell>
  </ng-container>
</table>
```

---

### Tarea 4.5 — `NotificacionesPanelComponent`

**`notificaciones-panel.component.ts`:**

```typescript
import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-notificaciones-panel',
  templateUrl: './notificaciones-panel.component.html',
})
export class NotificacionesPanelComponent {
  @Input() alertas: any[] = [];
  @Input() sinLeer = 0;
  @Input() abierto = false;
  @Output() cerrar = new EventEmitter<void>();
  @Output() marcarLeida = new EventEmitter<string>();
  @Output() marcarTodas = new EventEmitter<void>();

  iconoPorModulo(modulo: string): string {
    const map: Record<string, string> = {
      ordenes: 'assignment',
      stock:   'inventory_2',
      caja:    'credit_card',
      sistema: 'info',
    };
    return map[modulo] || 'notifications';
  }
}
```

El template del panel renderiza la lista de alertas con:
- Icono de módulo (Angular Material)
- Título y descripción
- Timestamp relativo ("hace 2 horas")
- Punto naranja para las no leídas
- Click en cada alerta → `marcarLeida.emit(alerta.id)`
- Botón "Marcar todas como leídas"

**En el template del topbar del dashboard:**
```html
<button mat-icon-button (click)="togglePanel()">
  <mat-icon [matBadge]="resumenAlertas.sin_leer" matBadgeColor="warn"
            [matBadgeHidden]="!resumenAlertas.sin_leer">
    notifications
  </mat-icon>
</button>

<app-notificaciones-panel
  [alertas]="alertasActivas"
  [sinLeer]="resumenAlertas.sin_leer"
  [abierto]="panelAbierto"
  (cerrar)="panelAbierto = false"
  (marcarLeida)="marcarLeida($event)">
</app-notificaciones-panel>
```

---

### Tarea 4.6 — `KpiCardComponent` (reutilizable)

```typescript
@Component({
  selector: 'app-kpi-card',
  template: `
    <div class="kpi-card" [class]="color" [class.alerta]="alerta && valor > 0">
      <div class="kpi-header">
        <span class="kpi-label">{{ titulo }}</span>
        <div class="kpi-icon" [class]="color">
          <mat-icon>{{ icono }}</mat-icon>
        </div>
      </div>
      <div class="kpi-value">{{ valorFormateado }}</div>
      <div class="kpi-sub" *ngIf="subtitulo">{{ subtitulo }}</div>
    </div>
  `
})
export class KpiCardComponent {
  @Input() titulo = '';
  @Input() valor: number | string | null = null;
  @Input() subtitulo = '';
  @Input() color: 'orange' | 'blue' | 'green' | 'warn' | 'danger' = 'blue';
  @Input() alerta = false;
  @Input() formato: 'numero' | 'moneda' | 'porcentaje' = 'numero';
  @Input() icono = 'analytics';

  get valorFormateado(): string {
    if (this.valor === null) return '—';
    if (this.formato === 'moneda')     return `$${Number(this.valor).toLocaleString('es-AR')}`;
    if (this.formato === 'porcentaje') return `${this.valor}%`;
    return String(this.valor);
  }
}
```

---

## 5. Actualizar contexto del Asistente IA

El asistente usa la función `obtener_contexto_taller()`. Enriquecerla con los nuevos datos:

**En `analytics/app/services/analytics_service.py`**, modificar `obtener_contexto_taller`:

```python
def obtener_contexto_taller(db: Session) -> dict:
    resumen        = resumen_ordenes(db)
    criticos       = stock_critico(db)
    caja_hoy       = resumen_caja_diario(db)
    tecnicos       = rendimiento_tecnicos(db, mes_actual=True)
    alta_prio      = ordenes_alta_prioridad(db, dias_minimos=2)
    sin_mov        = ordenes_sin_movimiento(db, dias_umbral=5)
    conv           = conversion_presupuesto(db)
    rechazos_hoy   = rechazos_cobros(db, dias=1)
    recurrencia    = recurrencia_clientes(db, meses=1)   # Solo el mes actual

    top_tecnico        = tecnicos[0]["nombre"] if tecnicos else "N/A"
    nombres_criticos   = [r["nombre"] for r in criticos[:5]]
    recurrencia_mes    = recurrencia[0] if recurrencia else {}

    return {
        # Existentes
        "ordenes_pendientes":       resumen["pendientes"],
        "ordenes_en_proceso":       resumen["en_proceso"],
        "ordenes_listas":           resumen["listas"],
        "ordenes_entregadas":       resumen["entregadas"],
        "repuestos_criticos":       nombres_criticos,
        "ingresos_hoy":             caja_hoy["total_ingresos"],
        "top_tecnico":              top_tecnico,
        "tecnicos_rendimiento":     tecnicos[:3],
        # Nuevos
        "ordenes_alta_prioridad_paradas": len(alta_prio),
        "ordenes_sin_movimiento":   len(sin_mov),
        "conversion_presupuesto_pct": conv["tasa_conversion_pct"],
        "rechazos_hoy_monto":       rechazos_hoy["total_rechazado"],
        "clientes_recurrentes_pct": recurrencia_mes.get("porcentaje_recurrentes", 0),
    }
```

También actualizar el template de contexto en `groq_service.py` para incluir los campos nuevos:

```python
# Agregar en la función _construir_contexto_texto():
    return f"""
    ...
    Alertas operativas:
      - Órdenes ALTA prioridad paradas: {contexto.get('ordenes_alta_prioridad_paradas', 0)}
      - Órdenes sin movimiento (5+ días): {contexto.get('ordenes_sin_movimiento', 0)}

    Finanzas:
      - Ingresos de hoy: ${contexto.get('ingresos_hoy', 0):,.2f}
      - Tasa conversión presupuesto→cobro: {contexto.get('conversion_presupuesto_pct', 0)}%
      - Cobros rechazados hoy: ${contexto.get('rechazos_hoy_monto', 0):,.0f}

    Clientes:
      - % clientes recurrentes este mes: {contexto.get('clientes_recurrentes_pct', 0)}%
    ...
    """
```

---

## 6. Testing

### Nuevos tests a agregar

**`analytics/tests/test_nuevos_kpis.py`:**

```python
def test_ordenes_alta_prioridad_estructura(client):
    response = client.get("/analytics/ordenes/alta-prioridad")
    assert response.status_code == 200
    assert isinstance(response.json(), list)

def test_ordenes_sin_movimiento_umbral(client):
    response = client.get("/analytics/ordenes/sin-movimiento?dias_umbral=3")
    assert response.status_code == 200

def test_tiempo_por_estado_devuelve_lista(client):
    response = client.get("/analytics/ordenes/tiempo-por-estado")
    assert response.status_code == 200

def test_rechazos_estructura(client):
    response = client.get("/analytics/caja/rechazos?dias=7")
    assert response.status_code == 200
    data = response.json()
    assert "total_rechazado" in data
    assert "por_medio" in data

def test_conversion_presupuesto_estructura(client):
    response = client.get("/analytics/caja/conversion-presupuesto")
    assert response.status_code == 200
    data = response.json()
    assert "tasa_conversion_pct" in data
    assert 0 <= data["tasa_conversion_pct"] <= 100

def test_recurrencia_clientes_estructura(client):
    response = client.get("/analytics/clientes/recurrencia?meses=3")
    assert response.status_code == 200

def test_stock_por_categoria_estructura(client):
    response = client.get("/analytics/stock/por-categoria")
    assert response.status_code == 200

def test_alertas_activas_devuelven_estructura(client):
    response = client.get("/analytics/alertas/activas")
    assert response.status_code == 200
    for a in response.json():
        assert "id" in a
        assert "tipo" in a
        assert a["tipo"] in ["danger", "warn", "info", "success"]

def test_marcar_alerta_leida(client):
    response = client.post("/analytics/alertas/test-id/marcar-leida")
    assert response.status_code == 200
    assert response.json()["ok"] is True
```

---

## 7. Checklist final

### Backend — nuevos KPIs
- [ ] Función `ordenes_alta_prioridad()` implementada y testeada
- [ ] Función `ordenes_sin_movimiento()` implementada con parámetro `dias_umbral`
- [ ] Función `tiempo_promedio_por_estado()` implementada con nota de limitación
- [ ] Función `rechazos_cobros()` implementada y diferencia RECHAZADO de PENDIENTE
- [ ] Función `conversion_presupuesto()` solo cuenta cobros APROBADO
- [ ] Función `recurrencia_clientes()` distingue correctamente recurrente vs nuevo
- [ ] Función `stock_por_categoria()` agrupa NULL como "Sin categoría"
- [ ] Todos los nuevos routers registrados en `main.py`

### Backend — alertas
- [ ] `alertas_service.py` genera alertas para los 5 tipos definidos
- [ ] `GET /analytics/alertas/activas` ordena: danger → warn → info
- [ ] `GET /analytics/alertas/resumen` devuelve `sin_leer` correcto
- [ ] `POST /analytics/alertas/{id}/marcar-leida` actualiza el set en memoria
- [ ] Las alertas tienen IDs determinísticos (no cambian entre requests)

### Frontend Angular
- [ ] `AnalyticsService` tiene todos los métodos nuevos
- [ ] `DashboardComponent` carga todos los datos con `forkJoin` al iniciar
- [ ] Poll de alertas cada 2 minutos funciona y se cancela en `ngOnDestroy`
- [ ] Tab "Operativo" muestra 4 KPI cards, embudo y tabla sin movimiento
- [ ] Tab "Financiero" muestra conversión, rechazos y evolución mensual
- [ ] Tab "Clientes" muestra gráfico de recurrencia
- [ ] Tab "Stock" muestra categorías y tabla de stock crítico
- [ ] `AlertasBannerComponent` solo se muestra si hay alertas activas
- [ ] `NotificacionesPanelComponent` abre/cierra correctamente
- [ ] Badge de campana muestra el conteo `sin_leer` actualizado
- [ ] `KpiCardComponent` formatea moneda, número y porcentaje correctamente

### Asistente IA
- [ ] `obtener_contexto_taller()` incluye los 5 campos nuevos
- [ ] Template de contexto en `groq_service.py` los muestra en el prompt

### Colección Postman — agregar
- [ ] `GET /analytics/ordenes/alta-prioridad`
- [ ] `GET /analytics/ordenes/sin-movimiento`
- [ ] `GET /analytics/ordenes/tiempo-por-estado`
- [ ] `GET /analytics/caja/rechazos`
- [ ] `GET /analytics/caja/conversion-presupuesto`
- [ ] `GET /analytics/clientes/recurrencia`
- [ ] `GET /analytics/stock/por-categoria`
- [ ] `GET /analytics/alertas/activas`
- [ ] `GET /analytics/alertas/resumen`
- [ ] `POST /analytics/alertas/{id}/marcar-leida`

---

*TallerSoft — Trabajo Final Integrador — UTN FRC 2026*