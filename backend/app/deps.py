from fastapi import Header, HTTPException, status
from jose import jwt, JWTError
import os


SECRET = os.getenv("JWT_SECRET", "dev-secret-change-me")
ALGO = "HS256"


async def get_current_user(authorization: str = Header(...)):
  try:
    scheme, token = authorization.split()
    if scheme.lower() != "bearer":
      raise ValueError
    payload = jwt.decode(token, SECRET, algorithms=["HS256"])
    return payload["sub"]
  except Exception:
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or missing token")