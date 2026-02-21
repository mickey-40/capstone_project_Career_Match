from pydantic import BaseModel, EmailStr, Field
from typing import List, Optional
from datetime import datetime


class Token(BaseModel):
  access_token: str
  token_type: str = "bearer"


class LoginRequest(BaseModel):
  email: EmailStr
  password: str


class AnalyzeRequest(BaseModel):
  resumeText: str = Field(min_length=10)
  jobText: str = Field(min_length=10)
  strategy: str = Field(default="keyword", pattern="^(keyword|embedding|hybrid)$")


class SkillItem(BaseModel):
  name: str
  category: Optional[str] = None
  matchType: str # MATCHED | MISSING | EXTRA
  source: str # RESUME | JOB | BOTH
  weight: float = 1.0


class Suggestion(BaseModel):
  type: str # ADD | REMOVE | REPHRASE
  message: str
  priority: int = 2


class AnalysisOut(BaseModel):
  id: str
  readinessScore: float
  createdAt: datetime
  skills: List[SkillItem] = []
  suggestions: List[Suggestion] = []


class AnalysisRow(BaseModel):
  id: str
  readinessScore: float
  createdAt: datetime


class PaginatedAnalyses(BaseModel):
  items: List[AnalysisRow]
  total: int