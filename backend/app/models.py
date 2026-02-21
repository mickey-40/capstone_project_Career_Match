from sqlalchemy import Column, String, DateTime, Float, Text, ForeignKey
from sqlalchemy.orm import relationship
from uuid import uuid4
from datetime import datetime
from .db import Base


class User(Base):
  __tablename__ = "users"
  id = Column(String, primary_key=True, default=lambda: str(uuid4()))
  email = Column(String, unique=True, index=True, nullable=False)
  password_hash = Column(String, nullable=False)
  created_at = Column(DateTime, default=datetime.utcnow)


analyses = relationship("Analysis", back_populates="user")


class Analysis(Base):
  __tablename__ = "analyses"
  id = Column(String, primary_key=True, default=lambda: str(uuid4()))
  user_id = Column(String, ForeignKey("users.id"), nullable=False)
  resume_text = Column(Text, nullable=False)
  job_text = Column(Text, nullable=False)
  readiness_score = Column(Float, nullable=False)
  summary_json = Column(Text, nullable=True) # JSON string for simplicity
  created_at = Column(DateTime, default=datetime.utcnow)


user = relationship("User", back_populates="analyses")