#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: no se encontró .env en $SCRIPT_DIR"
  exit 1
fi

# Carga las variables del .env (ignora comentarios y líneas vacías)
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Cuando el backend corre local (fuera de Docker), la DB está en localhost
export DB_HOST=localhost

echo "Variables cargadas:"
echo "  MP_USER_ID=$MP_USER_ID"
echo "  MP_POS_EXTERNAL_ID=$MP_POS_EXTERNAL_ID"
echo "  DB_HOST=$DB_HOST"
echo ""
echo "Iniciando backend..."

cd "$SCRIPT_DIR/backend"
mvn spring-boot:run
