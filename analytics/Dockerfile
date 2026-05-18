# Python 3.11 runtime with FastAPI
FROM python:3.11-alpine

WORKDIR /app

# Install system dependencies
RUN apk add --no-cache \
    gcc \
    musl-dev \
    postgresql-dev \
    curl

# Copy requirements
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ app/

# Create non-root user
RUN addgroup -g 1001 tallersoft && \
    adduser -D -u 1001 -G tallersoft tallersoft && \
    chown -R tallersoft:tallersoft /app

USER tallersoft

EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/health || exit 1

# Run FastAPI with uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082", "--log-level", "info"]
