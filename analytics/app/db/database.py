import os
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

load_dotenv()

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "tallersoft")
ANALYTICS_USER = os.getenv("ANALYTICS_USER", "analytics_reader")
ANALYTICS_PASSWORD = os.getenv("ANALYTICS_PASSWORD", "")

DATABASE_URL = (
    f"postgresql://{ANALYTICS_USER}:{ANALYTICS_PASSWORD}"
    f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)

_engine = None
_SessionLocal = None


def _get_engine():
    global _engine, _SessionLocal
    if _engine is None:
        _engine = create_engine(
            DATABASE_URL,
            echo=False,
            pool_pre_ping=True,
            pool_recycle=3600,
            connect_args={"connect_timeout": 10},
        )

        @event.listens_for(_engine, "before_cursor_execute")
        def prevent_writes(conn, cursor, statement, parameters, context, executemany):
            stmt_upper = statement.strip().upper()
            if any(stmt_upper.startswith(w) for w in ["INSERT", "UPDATE", "DELETE", "DROP", "ALTER"]):
                raise PermissionError(f"El Analytics Service no puede ejecutar: {stmt_upper[:30]}...")

        _SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=_engine)
    return _engine, _SessionLocal


def get_db():
    _, session_factory = _get_engine()
    db = session_factory()
    try:
        yield db
    finally:
        db.close()
