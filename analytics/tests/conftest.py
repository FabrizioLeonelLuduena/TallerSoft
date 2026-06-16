import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from app.main import app
from app.db.database import get_db

# SQLite in-memory con StaticPool para que todas las sesiones compartan la misma conexión
SQLALCHEMY_DATABASE_URL = "sqlite://"

engine_test = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(bind=engine_test, autocommit=False, autoflush=False)


def _create_schema():
    with engine_test.begin() as conn:
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS clientes (
                id     INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS equipos (
                id         INTEGER PRIMARY KEY,
                cliente_id INTEGER,
                tipo       TEXT NOT NULL DEFAULT '',
                marca      TEXT,
                modelo     TEXT
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS ordenes_trabajo (
                id          INTEGER PRIMARY KEY,
                estado      TEXT NOT NULL,
                prioridad   TEXT NOT NULL DEFAULT 'NORMAL',
                cliente_id  INTEGER,
                equipo_id   INTEGER,
                tecnico_id  INTEGER,
                presupuesto REAL,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id     INTEGER PRIMARY KEY,
                nombre TEXT NOT NULL,
                rol    TEXT
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS repuestos (
                id           INTEGER PRIMARY KEY,
                nombre       TEXT NOT NULL,
                categoria    TEXT,
                stock_actual INTEGER NOT NULL DEFAULT 0,
                stock_minimo INTEGER NOT NULL DEFAULT 0,
                stock_bajo   INTEGER NOT NULL DEFAULT 0,
                precio       REAL    NOT NULL DEFAULT 0
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS orden_repuestos (
                id          INTEGER PRIMARY KEY,
                orden_id    INTEGER,
                repuesto_id INTEGER,
                cantidad    INTEGER NOT NULL DEFAULT 1
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS cobros (
                id           INTEGER PRIMARY KEY,
                monto        REAL NOT NULL,
                medio_pago   TEXT NOT NULL,
                estado_pago  TEXT NOT NULL,
                created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS alertas_leidas (
                id          INTEGER PRIMARY KEY,
                usuario_id  INTEGER NOT NULL,
                alerta_key  TEXT    NOT NULL,
                leida_en    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (usuario_id, alerta_key)
            )
        """))


_create_schema()


def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def db_session():
    db = TestingSessionLocal()
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()
