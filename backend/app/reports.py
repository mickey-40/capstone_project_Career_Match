# backend/app/reports.py
from fastapi import APIRouter, Depends, HTTPException, Response
from fastapi.responses import HTMLResponse
from sqlalchemy import select
from sqlalchemy.orm import Session
from datetime import datetime
from .db import get_session
from .models import Analysis
import json
from html import escape
from typing import Any, Dict, Iterable, Optional

router = APIRouter(prefix="/reports", tags=["reports"])


# ---------- Helpers: safe parsing & formatting ----------

from html import escape
from datetime import datetime, timezone
import json
from fastapi import HTTPException

def safe_str(v):
    try:
        if v is None:
            return ""
        return str(v)
    except Exception:
        return ""

def html_escape(v):
    return escape(safe_str(v))

def normalize_score(raw):
    """
    Accepts values like 0.83, 83, or >100; returns 0..100 float.
    """
    try:
        x = float(raw)
    except Exception:
        return 0.0
    if x <= 1.0:  # model sometimes returns 0..1
        x *= 100.0
    return max(0.0, min(x, 100.0))

def priority_label(v):
    """
    Convert various priority shapes (str, int, None) into 'High' | 'Medium' | 'Low'.
    Accepts: 'high', 'H', 3 -> High; 2 -> Medium; 1 -> Low; default Medium.
    """
    if v is None:
        return "Medium"
    # numeric?
    try:
        n = int(v)
        if n >= 3:
            return "High"
        if n <= 1:
            return "Low"
        return "Medium"
    except Exception:
        pass
    # text?
    s = safe_str(v).strip().lower()
    if s in {"3", "high", "h", "urgent"}:
        return "High"
    if s in {"1", "low", "l"}:
        return "Low"
    if s in {"2", "med", "medium", "m"}:
        return "Medium"
    return "Medium"

def formatted_dt(dt):
    try:
        # allow string or datetime
        if isinstance(dt, datetime):
            dt_utc = dt.astimezone(timezone.utc)
        else:
            # try ISO 8601
            dt_utc = datetime.fromisoformat(str(dt).replace("Z", "+00:00")).astimezone(timezone.utc)
        return dt_utc.strftime("%b %d, %Y • %I:%M %p UTC")
    except Exception:
        return safe_str(dt)

def li(content):
    return f"<li>{html_escape(content)}</li>"

def pill(text, color):
    return f"""
      <span style="
        display:inline-block; padding:.12rem .5rem; border-radius:999px;
        font-size:.75rem; font-weight:600; color:white; background:{color};
      ">{html_escape(text)}</span>
    """

def bar(pct):
    pct = max(0.0, min(100.0, pct))
    return f"""
      <div style="background:#eee;height:10px;border-radius:6px;overflow:hidden">
        <div style="background:linear-gradient(90deg,#2E7D32,#43A047);
                    width:{pct:.1f}%;height:100%"></div>
      </div>
    """

def split_strengths_suggestions(suggestions):
    """
    suggestions can be:
      - list[str]
      - list[dict{message, priority}]
      - dict with keys like strengths/suggestions
    This returns (strengths:list[str], improvements:list[(message, priority_label)])
    """
    strengths = []
    improvements = []

    # If the whole object is a dict with named buckets:
    if isinstance(suggestions, dict):
        # strengths
        raw_strengths = suggestions.get("strengths") or suggestions.get("pros") or []
        if isinstance(raw_strengths, list):
            for s in raw_strengths:
                strengths.append(safe_str(s))
        # improvements
        raw_impr = suggestions.get("suggestions") or suggestions.get("cons") or []
        if isinstance(raw_impr, list):
            for it in raw_impr:
                if isinstance(it, dict):
                    improvements.append((safe_str(it.get("message") or it.get("text") or it), priority_label(it.get("priority"))))
                else:
                    improvements.append((safe_str(it), "Medium"))
        return strengths, improvements

    # Else assume it's a list
    if isinstance(suggestions, list):
        for it in suggestions:
            if isinstance(it, dict):
                msg = safe_str(it.get("message") or it.get("text") or it)
                pr = priority_label(it.get("priority"))
                # Heuristic: if dict says "type=strength" or similar, bucket accordingly
                t = safe_str(it.get("type") or "").strip().lower()
                if t in {"strength", "pro", "positive"}:
                    strengths.append(msg)
                else:
                    improvements.append((msg, pr))
            else:
                improvements.append((safe_str(it), "Medium"))
    return strengths, improvements

def color_for_priority(plabel):
    if plabel == "High":
        return "#C62828"
    if plabel == "Low":
        return "#2E7D32"
    return "#F9A825"  # Medium

# ---------- CSV ----------

@router.get("/{analysis_id}/csv")
def report_csv(analysis_id: str, db: Session = Depends(get_session)):
    row = db.execute(select(Analysis).where(Analysis.id == analysis_id)).scalar_one_or_none()
    if not row:
        return Response(status_code=404)

    title = "AI Resume Analyzer Report"
    ts = datetime.utcnow().isoformat()

    pct = normalize_pct(row.readiness_score)
    csv = [
        f"title,{title}",
        f"generated_at,{ts}",
        "field,value",
        f"id,{row.id}",
        f"readiness_score_pct,{pct:.1f}"
    ]
    return Response("\n".join(csv), media_type="text/csv")


# ---------- HTML ----------

# ---------- Pretty HTML report ----------

@router.get("/{analysis_id}", response_class=HTMLResponse)
def report_html(analysis_id: str, db: Session = Depends(get_session)):
    # 1) Fetch row (exact match or prefix)
    row = db.get(Analysis, analysis_id)
    if not row:
        stmt = select(Analysis).where(Analysis.id.like(f"{analysis_id}%")).limit(1)
        row = db.execute(stmt).scalars().first()
        if not row:
            raise HTTPException(404, "Not Found")

    # 2) Unpack summary_json defensively
    skills = []
    suggestions_raw = []
    try:
        summary = json.loads(row.summary_json or "{}")
        # skills
        s_list = summary.get("skills") or []
        if isinstance(s_list, list):
            for s in s_list:
                if isinstance(s, dict):
                    name = safe_str(s.get("name") or s.get("skill") or "")
                    mtype = safe_str(s.get("matchType") or s.get("type") or "")
                    src   = safe_str(s.get("source") or "")
                    skills.append((name, mtype, src))
                else:
                    skills.append((safe_str(s), "", ""))
        # suggestions (can be many shapes)
        suggestions_raw = summary.get("suggestions") or summary.get("recommendations") or []
    except Exception:
        pass

    # 3) Process suggestions into strengths & improvements with priority labels
    strengths, improvements = split_strengths_suggestions(suggestions_raw)

    score = normalize_score(row.readiness_score)
    created = formatted_dt(row.created_at)

    # 4) Build HTML (single file, no external assets)
    skills_rows = "".join(
        f"<tr><td>{html_escape(n)}</td><td>{html_escape(mt)}</td><td>{html_escape(src)}</td></tr>"
        for (n, mt, src) in skills
    ) or '<tr><td colspan="3">No skills found</td></tr>'

    strengths_html = "".join(li(s) for s in strengths) or "<li>No strengths extracted</li>"
    improvements_html = "".join(
        f"<li>{html_escape(msg)}&nbsp;{pill(pr, color_for_priority(pr))}</li>"
        for (msg, pr) in improvements
    ) or "<li>No suggestions extracted</li>"

    hi_count = sum(1 for (_, pr) in improvements if pr == "High")
    md_count = sum(1 for (_, pr) in improvements if pr == "Medium")
    lo_count = sum(1 for (_, pr) in improvements if pr == "Low")

    html = f"""
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Analysis Report • {html_escape(row.id)}</title>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <style>
    :root {{
      --bg:#fafafa; --card:#fff; --text:#111; --muted:#666;
      --border:#e5e5e5; --accent:#2E7D32; --warn:#F9A825; --err:#C62828;
    }}
    body {{
      font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif;
      background: var(--bg); color: var(--text); margin:0; padding:24px;
    }}
    .container {{
      max-width: 900px; margin: 0 auto;
    }}
    .card {{
      background: var(--card); border:1px solid var(--border);
      border-radius: 12px; padding: 16px; margin-bottom: 16px;
      box-shadow: 0 1px 2px rgba(0,0,0,.03);
    }}
    h1,h2,h3 {{ margin: 0 0 8px 0; }}
    .muted {{ color: var(--muted); }}
    table {{ width:100%; border-collapse: collapse; }}
    th, td {{ padding: 8px 6px; border-bottom:1px solid var(--border); text-align:left; }}
    .grid {{ display:grid; grid-template-columns: 1fr 1fr; gap:12px; }}
    @media(max-width:720px) {{ .grid {{ grid-template-columns:1fr; }} }}
    .kpi {{ display:flex; align-items:center; gap:8px; }}
    .kpi .dot {{ width:10px; height:10px; border-radius:999px; display:inline-block; }}
  </style>
</head>
<body>
  <div class="container">

    <div class="card">
      <h1>Analysis Report</h1>
      <div class="muted">ID: {html_escape(row.id)}</div>
      <div class="muted">Created: {html_escape(created)}</div>
    </div>

    <div class="card">
      <h2>Overview</h2>
      <p style="margin:.25rem 0 .75rem 0;"><b>Readiness Score:</b> {score:.1f}%</p>
      {bar(score)}
      <div class="grid" style="margin-top:12px;">
        <div class="kpi"><span class="dot" style="background:var(--err)"></span> High priority: {hi_count}</div>
        <div class="kpi"><span class="dot" style="background:var(--warn)"></span> Medium priority: {md_count}</div>
        <div class="kpi"><span class="dot" style="background:var(--accent)"></span> Low priority: {lo_count}</div>
      </div>
    </div>

    <div class="card">
      <h2>Key Strengths</h2>
      <ul style="margin-top:.5rem">{strengths_html}</ul>
    </div>

    <div class="card">
      <h2>Improvement Opportunities</h2>
      <ul style="margin-top:.5rem">{improvements_html}</ul>
    </div>

    <div class="card">
      <h2>Skills Extracted</h2>
      <table>
        <thead>
          <tr><th>Skill</th><th>Match</th><th>Source</th></tr>
        </thead>
        <tbody>
          {skills_rows}
        </tbody>
      </table>
    </div>

    <div class="muted" style="text-align:center;margin-top:12px;">
      Generated by CareerMatch AI
    </div>

  </div>
</body>
</html>
    """
    return HTMLResponse(html)
