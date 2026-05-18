# TallerSoft 🔧

### ERP Inteligente para Talleres de Servicio Técnico

> Plataforma web moderna para la gestión integral de talleres técnicos con arquitectura de microservicios, analítica avanzada y asistente IA integrado.

---

## 🚀 Descripción

**TallerSoft** es un sistema ERP liviano orientado a talleres de reparación y servicio técnico (PCs, celulares, electrónica y electrodomésticos).
Centraliza los procesos operativos del negocio en una única plataforma moderna, escalable y segura.

El sistema permite:

* Gestión de clientes y equipos
* Órdenes de trabajo con flujo Kanban
* Control de stock y repuestos
* Caja y facturación
* Dashboard analítico en tiempo real
* Integración con MercadoPago
* Asistente conversacional con IA

---

## ✨ Características principales

✅ Arquitectura de microservicios
✅ Backend robusto con Spring Boot 3 + Java 21
✅ Frontend moderno con Angular 17
✅ Dashboard analítico con KPIs
✅ JWT + control de roles
✅ Integración con IA mediante Claude API
✅ Integración con MercadoPago
✅ Docker & Docker Compose
✅ API REST documentada
✅ Diseño escalable y mantenible

---

# 🏗️ Arquitectura

```text
Frontend (Angular)
        │
        ▼
API Gateway (Spring Cloud Gateway)
        │
 ┌──────┴────────┐
 ▼               ▼
Core Service   Analytics Service
(Spring Boot)   (FastAPI + Python)
        │
        ▼
 PostgreSQL
```

---

# 🛠️ Stack Tecnológico

| Capa          | Tecnología              |
| ------------- | ----------------------- |
| Frontend      | Angular 17              |
| UI            | Angular Material        |
| Backend       | Java 21 + Spring Boot 3 |
| Seguridad     | Spring Security + JWT   |
| API Gateway   | Spring Cloud Gateway    |
| Analítica     | Python + FastAPI        |
| Base de datos | PostgreSQL 16           |
| IA            | Anthropic Claude API    |
| Pagos         | MercadoPago API         |
| Contenedores  | Docker                  |
| Testing       | JUnit, Mockito, Jasmine |

---

# 📦 Estructura del Proyecto

```bash
tallersoft/
│
├── frontend/          # Angular App
├── backend/           # Core Service (Spring Boot)
├── gateway/           # API Gateway
├── analytics/         # Python Analytics Service
├── docker-compose.yml
└── README.md
```

---

# 🔐 Roles del Sistema

| Rol       | Permisos                          |
| --------- | --------------------------------- |
| ADMIN     | Gestión total del sistema         |
| TECNICO   | Gestión de órdenes y diagnósticos |
| RECEPCION | Clientes, cobros y presupuestos   |

---

# 📋 Módulos Principales

## 👥 Clientes y Equipos

* ABM de clientes
* Historial de reparaciones
* Gestión de múltiples equipos por cliente

## 🧾 Órdenes de Trabajo

* Flujo de estados
* Vista Kanban
* Asignación de técnicos
* Registro de diagnósticos

## 📦 Stock

* Gestión de repuestos
* Alertas de stock crítico
* Descuento automático de inventario

## 💳 Caja y Facturación

* Cobros
* Presupuestos PDF
* Integración con MercadoPago

## 📊 Dashboard Analítico

* KPIs del negocio
* Rendimiento de técnicos
* Evolución de ingresos
* Órdenes por período

## 🤖 Asistente IA

Chat inteligente conectado a los datos reales del sistema para responder consultas operativas y analíticas.

---

# 👨‍💻 Autor

**Fabrizio Ludueña**
*Técnicatura Universitaria en Programación — UTN FRC*

* GitHub: `@412237-Luduena`

---

# 📄 Licencia

Este proyecto fue desarrollado con fines académicos y educativos como Trabajo Final Integrador de la UTN FRC.

---

