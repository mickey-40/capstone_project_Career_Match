from datetime import datetime, timedelta
from jose import jwt
from passlib.hash import pbkdf2_sha256 as hasher
from fastapi import HTTPException, status
import os


SECRET = os.getenv("JWT_SECRET", "dev-secret-change-me")
ALGO = "HS256"
TTL_MIN = int(os.getenv("JWT_TTL_MIN", "120"))


# Demo user store (replace with real persistence in Task 3)
DEMO_USER = {
  "email": os.getenv("DEMO_EMAIL", "demo@example.com"),
  "password_hash": hasher.hash(os.getenv("DEMO_PASSWORD", "Password123!"))
}


def verify_user(email: str, password: str):
  if email.lower() != DEMO_USER["email"].lower():
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
  if not hasher.verify(password, DEMO_USER["password_hash"]): 
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
  return {"email": email}


def issue_token(sub: str):
  now = datetime.utcnow()
  payload = {"sub": sub, "iat": now, "exp": now + timedelta(minutes=TTL_MIN)}
  return jwt.encode(payload, SECRET, algorithm=ALGO)