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
  # Future: add options like top_k or chunking


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


class KeywordOut(BaseModel):
  score: float
  skills: List[SkillItem] = []
  suggestions: List[Suggestion] = []


class SemanticMatch(BaseModel):
  resumeChunk: str
  jobChunk: str
  similarity: float


class SemanticMissing(BaseModel):
  concept: str
  evidence: str
  confidence: float


class SemanticOut(BaseModel):
  score: float
  topMatches: List[SemanticMatch] = []
  missingConcepts: List[SemanticMissing] = []
  error: Optional[str] = None


class SummaryOut(BaseModel):
  overallScore: float
  improvementNote: Optional[str] = None
  topGaps: List[str] = []


class AnalysisOut(BaseModel):
  id: str
  readinessScore: float
  createdAt: datetime
  skills: List[SkillItem] = []
  suggestions: List[Suggestion] = []
  # v2 additions (optional for backward compatibility)
  keyword: Optional[KeywordOut] = None
  semantic: Optional[SemanticOut] = None
  summary: Optional[SummaryOut] = None


class AnalysisRow(BaseModel):
  id: str
  readinessScore: float
  createdAt: datetime
  keywordScore: Optional[float] = None
  semanticScore: Optional[float] = None
  overallScore: Optional[float] = None


class PaginatedAnalyses(BaseModel):
  items: List[AnalysisRow]
  total: int
