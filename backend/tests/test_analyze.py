from fastapi.testclient import TestClient
from app.main import app


client = TestClient(app)


def test_login_and_analyze():
  # Login
  r = client.post("/auth/login", json={"email": "demo@example.com", "password": "Password123!"})
  assert r.status_code == 200
  token = r.json()["access_token"]


  # Analyze
  headers = {"Authorization": f"Bearer {token}"}
  r2 = client.post("/analyze", json={
    "resumeText": "Python FastAPI Docker Kubernetes SQL",
    "jobText": "We need FastAPI, SQL, and Docker skills",
    "strategy": "keyword"
  }, headers=headers)
  assert r2.status_code == 200
  data = r2.json()
  assert 0.0 <= data["readinessScore"] <= 1.0
  assert "skills" in data