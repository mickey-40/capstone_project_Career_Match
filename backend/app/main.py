from fastapi import FastAPI, Depends, HTTPException, Path, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import select, func
from datetime import datetime
import json

# ⬇️ This line brings Base and engine into scope
from .db import Base, engine, get_session

from .models import User, Analysis
from .schemas import LoginRequest, Token, AnalyzeRequest, AnalysisOut, PaginatedAnalyses, AnalysisRow
from .auth import verify_user, issue_token
from .deps import get_current_user
from .analyzer.keyword import KeywordAnalyzer
from .analyzer.semantic import SemanticAnalyzer
from . import reports


app = FastAPI(title="AI Resume Analyzer API", version="1.0.0")


app.add_middleware(
  CORSMiddleware,
  allow_origins=["*"],
  allow_credentials=True,
  allow_methods=["*"],
  allow_headers=["*"],
)


# Create tables on startup (simple dev approach)
Base.metadata.create_all(bind=engine)

@app.get("/health", tags=["health"])
def health():
    return {"status": "ok"}


@app.post("/auth/login", response_model=Token)
def login(body: LoginRequest):
  user = verify_user(body.email, body.password)
  return {"access_token": issue_token(user["email"])}


@app.post("/analyze", response_model=AnalysisOut)
def analyze(req: AnalyzeRequest, db: Session = Depends(get_session), sub: str = Depends(get_current_user)):
  keyword_analyzer = KeywordAnalyzer()
  keyword_result = keyword_analyzer.run(req.resumeText, req.jobText)

  semantic_result = None
  semantic_error = None
  if req.strategy in ("embedding", "hybrid"):
    try:
      semantic_analyzer = SemanticAnalyzer()
      semantic_result = semantic_analyzer.run(req.resumeText, req.jobText)
    except Exception as e:
      semantic_error = str(e)

  keyword_block = {
    "score": keyword_result["readinessScore"],
    "skills": keyword_result["skills"],
    "suggestions": keyword_result["suggestions"],
  }

  if semantic_result is None and semantic_error:
    semantic_block = {"score": 0.0, "topMatches": [], "missingConcepts": [], "error": semantic_error}
  else:
    semantic_block = semantic_result
  if semantic_result is None:
    overall = keyword_result["readinessScore"]
    note = "Semantic matching not enabled (strategy=keyword)."
  else:
    overall = round((keyword_result["readinessScore"] + semantic_result.get("score", 0.0)) / 2, 3)
    note = None

  summary_block = {
    "overallScore": overall,
    "improvementNote": note,
    "topGaps": [s["name"] for s in keyword_result["skills"] if s["matchType"] == "MISSING"][:5],
  }
  row = Analysis(
    user_id=sub,
    resume_text=req.resumeText,
    job_text=req.jobText,
    readiness_score=keyword_result["readinessScore"],
    summary_json=json.dumps({
      "keyword": keyword_block,
      "semantic": semantic_block,
      "summary": summary_block,
      "skills": keyword_result["skills"],
      "suggestions": keyword_result["suggestions"],
    })
  )
  db.add(row)
  db.commit()
  db.refresh(row)


  return {
    "id": row.id,
    "readinessScore": row.readiness_score,
    "createdAt": row.created_at,
    "skills": keyword_result["skills"],
    "suggestions": keyword_result["suggestions"],
    "keyword": keyword_block,
    "semantic": semantic_block,
    "summary": summary_block,
  }


@app.get("/analyses", response_model=PaginatedAnalyses)
def list_analyses(page: int = 1, size: int = 20, q: str | None = None, db: Session = Depends(get_session), sub: str = Depends(get_current_user)):
  offset = (page - 1) * size
  base = select(Analysis).where(Analysis.user_id==sub).order_by(Analysis.created_at.desc())
  if q:
    like = f"%{q.lower()}%"
    base = base.where(func.lower(Analysis.resume_text).like(like) | func.lower(Analysis.job_text).like(like))
  total = db.execute(base).scalars().all()
  rows = total[offset: offset+size]
  items = []
  for r in rows:
    summary = json.loads(r.summary_json or "{}")
    items.append({
      "id": r.id,
      "readinessScore": r.readiness_score,
      "createdAt": r.created_at,
      "keywordScore": summary.get("keyword", {}).get("score") if summary.get("keyword") else None,
      "semanticScore": summary.get("semantic", {}).get("score") if summary.get("semantic") else None,
      "overallScore": summary.get("summary", {}).get("overallScore") if summary.get("summary") else None,
    })
  return {"items": items, "total": len(total)}

@app.get("/analyses/{analysis_id}", response_model=AnalysisOut)
def get_analysis(
    analysis_id: str,
    db: Session = Depends(get_session),
    sub: str = Depends(get_current_user),
):
    row = db.get(Analysis, analysis_id)
    if not row or row.user_id != sub:
        raise HTTPException(status_code=404, detail="Analysis not found")

    summary = json.loads(row.summary_json or "{}")
    return {
        "id": row.id,
        "readinessScore": row.readiness_score,
        "createdAt": row.created_at,
        "skills": summary.get("skills", []),
        "suggestions": summary.get("suggestions", []),
        "keyword": summary.get("keyword"),
        "semantic": summary.get("semantic"),
        "summary": summary.get("summary"),
    }


@app.delete("/analyses/{analysis_id}", status_code=204)
def delete_analysis(analysis_id: str, db: Session = Depends(get_session), sub: str = Depends(get_current_user)):
    row = db.get(Analysis, analysis_id)
    if not row or row.user_id != sub:
        raise HTTPException(status_code=404, detail="Not found")
    db.delete(row)
    db.commit()
    return  # 204 No Content


# Mount reports router
app.include_router(reports.router)
