#!/bin/bash

# ================================================================
# Database Setup Script - TallerSoft
# Initializes PostgreSQL database schema and creates read-only user
# ================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tallersoft}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-}"

echo -e "${YELLOW}Starting TallerSoft database setup...${NC}"

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql is not installed${NC}"
    exit 1
fi

# Export password for non-interactive authentication
export PGPASSWORD="$DB_PASSWORD"

# Test database connection
echo -e "${YELLOW}Testing database connection...${NC}"
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Database connection successful${NC}"
else
    echo -e "${RED}Error: Could not connect to database${NC}"
    exit 1
fi

# Create database if it doesn't exist
echo -e "${YELLOW}Creating database (if not exists)...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" 2>&1 | grep -v "already exists" || true
echo -e "${GREEN}✓ Database ready${NC}"

# Run schema initialization
echo -e "${YELLOW}Running schema initialization...${NC}"
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/init-db.sql"; then
    echo -e "${GREEN}✓ Schema initialized successfully${NC}"
else
    echo -e "${RED}Error: Schema initialization failed${NC}"
    exit 1
fi

# Create analytics user
echo -e "${YELLOW}Creating analytics_reader user...${NC}"
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$(dirname "$0")/create-analytics-user.sql"; then
    echo -e "${GREEN}✓ Analytics user created successfully${NC}"
else
    echo -e "${RED}Error: Analytics user creation failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Database setup completed successfully!${NC}"
echo -e "${YELLOW}Database: $DB_NAME on $DB_HOST:$DB_PORT${NC}"
