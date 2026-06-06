#!/bin/bash
set -e

echo "=== Tests Backend (JUnit) ==="
cd "$(dirname "$0")/../backend"
mvn test --no-transfer-progress
echo ""

echo "=== Tests Analytics (pytest) ==="
cd "../analytics"
python -m pytest tests/ -v --tb=short
echo ""

echo "=== Tests Frontend (Karma - una ejecución) ==="
cd "../frontend"
ng test --watch=false --browsers=ChromeHeadless
