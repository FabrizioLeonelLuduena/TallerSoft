#!/usr/bin/env python3
"""
TallerSoft Analytics Service - WSGI Application

Entry point for production deployment.
"""
from app.main import app

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)
