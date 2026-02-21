# tests/test_analyzer.py
from app.analyzer.keyword import KeywordAnalyzer

def test_filters_filler_and_weights_tech():
    resume = "Python, FastAPI, Docker, SQL. 5 years experience."
    job = "Seeking a backend engineer with Python, FastAPI, and PostgreSQL"

    k = KeywordAnalyzer()
    out = k.run(resume, job)

    # No filler like "seeking" or "years"
    names = {s["name"] for s in out["skills"]}
    assert "seeking" not in names
    assert "years" not in names

    # Tech terms get boosted weight
    py = next(s for s in out["skills"] if s["name"] == "python" and s["matchType"] == "MATCHED")
    fa = next(s for s in out["skills"] if s["name"] == "fastapi" and s["matchType"] == "MATCHED")
    assert py["weight"] == 1.5
    assert fa["weight"] == 1.5

    # Missing Postgres is identified (weight boosted)
    pg = next(s for s in out["skills"] if s["name"] == "postgresql" and s["matchType"] == "MISSING")
    assert pg["weight"] == 1.5

def test_score_is_weighted_by_tech_matches():
    resume = "Python FastAPI"
    job = "Python FastAPI PostgreSQL"
    k = KeywordAnalyzer()
    out = k.run(resume, job)

    # 2 tech matches (1.5 + 1.5) over total weight (1.5 + 1.5 + 1.5) = 3.0/4.5 = 66.7%
    assert abs(out["readinessScore"] - 0.667) < 0.2
