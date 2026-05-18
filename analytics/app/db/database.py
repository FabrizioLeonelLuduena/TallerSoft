"""
SQLAlchemy Database Connection for Analytics Service

This module establishes a READ-ONLY connection to PostgreSQL using
the analytics_reader user. This ensures that the analytics service
can only perform SELECT operations and cannot modify the database.
"""

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Build database URL using environment variables
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "tallersoft")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")

# For analytics, use read-only user
ANALYTICS_USER = os.getenv("ANALYTICS_USER", "analytics_reader")
ANALYTICS_PASSWORD = os.getenv("ANALYTICS_PASSWORD", "analytics_reader_secure_password_change_in_production")

DATABASE_URL = (
    f"postgresql://{ANALYTICS_USER}:{ANALYTICS_PASSWORD}"
    f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

# Create engine with read-only optimizations
engine = create_engine(
    DATABASE_URL,
    echo=False,
    pool_pre_ping=True,
    pool_recycle=3600,
    connect_args={
        "connect_timeout": 10,
    }
)

# Session factory
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)


def get_db():
    """
    Dependency function for FastAPI to get a database session
    
    Usage:
        @router.get("/example")
        def example(db: Session = Depends(get_db)):
            # Use db here
            pass
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def test_connection():
    """Test database connection on startup"""
    try:
        with engine.connect() as conn:
            conn.execute("SELECT 1")
        return True
    except Exception as e:
        print(f"Database connection failed: {e}")
        return False
