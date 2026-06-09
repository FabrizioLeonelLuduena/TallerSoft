# Prompt: Auditoría de Código — TallerSoft

Pegá esto en Claude Code **antes de pedirle que haga cualquier cambio**.

---

## PROMPT

```
Antes de tocar cualquier archivo, quiero que hagas una auditoría completa del código del proyecto TallerSoft. Revisá todos los archivos de los tres servicios (backend Java, frontend Angular y analytics Python) y generame un reporte estructurado con lo siguiente:

---

### 1. VALORES HARDCODEADOS

Buscá en el código fuente (no en archivos .env ni application.yml):

**Backend (Java):**
- Strings con IPs, puertos o URLs fijas (ej: "localhost:8081", "http://...")
- Credenciales, tokens o secrets en el código fuente
- Valores de configuración que deberían venir de @Value o variables de entorno
- Números mágicos sin constante nombrada (ej: 24 como horas de expiración del JWT)
- Emails o usuarios de prueba hardcodeados fuera del seed de BD

**Frontend (TypeScript/Angular):**
- apiUrl hardcodeada en servicios (debería venir de environment.ts)
- Cualquier string "localhost" o URL directa dentro de services o components
- Tokens o keys de API visibles
- Roles hardcodeados como strings sueltos (ej: "ADMIN") fuera de un enum o constante

**Analytics (Python):**
- DATABASE_URL, API keys o secrets fuera del .env / config.py
- URLs hardcodeadas en los routers o servicios
- El SYSTEM_PROMPT del asistente (verificar si está en constante o hardcodeado inline)
- Límites o parámetros de negocio hardcodeados (ej: días sin movimiento, top N)

---

### 2. TODO / FIXME / HACK / WORKAROUND

Buscá en todos los archivos (incluyendo comentarios):
- `// TODO`
- `// FIXME`
- `// HACK`
- `// WORKAROUND`
- `# TODO` (Python)
- `# FIXME` (Python)
- Cualquier comentario que diga "temporal", "provisorio", "por ahora", "arreglar", "mejorar"

Para cada uno, indicá:
- Archivo y número de línea
- El texto exacto del comentario
- Contexto (qué función o clase lo rodea)
- Tu evaluación de si es crítico, medio o bajo impacto

---

### 3. CÓDIGO MUERTO O INCOMPLETO

- Métodos con cuerpo vacío o que solo tiran `throw new UnsupportedOperationException()`
- Funciones que devuelven `null` o `None` siempre (posible stub)
- Imports sin usar
- Variables declaradas pero nunca utilizadas
- Endpoints declarados pero sin implementación real (ej: solo `return null`)

---

### 4. RESUMEN EJECUTIVO

Al final del reporte, dame:
- Total de hardcodes encontrados por servicio
- Total de TODOs/FIXMEs por servicio
- Los 3 problemas más críticos que debería resolver antes de cualquier deploy
- Una línea diciendo si el código está "listo para continuar trabajando" o "requiere limpieza previa"

---

**Formato de salida esperado:** una tabla o lista clara por sección, con archivo + línea + descripción + severidad (🔴 crítico / 🟡 medio / 🟢 bajo).

No hagas ningún cambio todavía. Solo el reporte.
```

---
