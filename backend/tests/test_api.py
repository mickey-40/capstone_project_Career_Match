# tests/test_api.py
import os
from fastapi.testclient import TestClient
from app.main import app

# Ensure demo secret is present for token issuing
os.environ.setdefault("JWT_SECRET", "test-secret")

client = TestClient(app)

def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json().get("status") == "ok"

def test_login_and_analyze_flow():
    # Login
    r = client.post("/auth/login", json={
        "email": "demo@example.com",
        "password": os.getenv("DEMO_PASSWORD", "Password123!")
    })
    assert r.status_code == 200
    token = r.json()["access_token"]
    assert token

    # Analyze (camelCase request)
    r2 = client.post(
        "/analyze",
        headers={"Authorization": f"Bearer {token}"},
        json={
            "resumeText": "Python, FastAPI, Docker, SQL",
            "jobText": "Backend engineer using Python, FastAPI, PostgreSQL"
        },
    )
    assert r2.status_code == 200
    body = r2.json()
    assert "id" in body and "readinessScore" in body and "skills" in body
    # Expect postgres to be missing if not in resume
    names = {s["name"] for s in body["skills"]}
    assert "postgresql" in names
