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
    assert py["weight"] > 1.5
    assert fa["weight"] > 1.5

    # Missing Postgres is identified (weight boosted)
    pg = next(s for s in out["skills"] if s["name"] == "postgresql" and s["matchType"] == "MISSING")
    assert pg["weight"] > 1.5

def test_score_is_weighted_by_tech_matches():
    resume = "Python FastAPI"
    job = "Python FastAPI PostgreSQL"
    k = KeywordAnalyzer()
    out = k.run(resume, job)

    # 2 tech matches (1.5 + 1.5) over total weight (1.5 + 1.5 + 1.5) = 3.0/4.5 = 66.7%
    assert abs(out["readinessScore"] - 0.667) < 0.2

def test_normalizes_common_aliases_and_phrases():
    resume = "Built REST APIs on Amazon Web Services with Postgres and CI/CD."
    job = "Need REST API experience on AWS with PostgreSQL and continuous integration."
    k = KeywordAnalyzer()
    out = k.run(resume, job)

    names = {s["name"] for s in out["skills"] if s["matchType"] == "MATCHED"}
    assert "rest" in names
    assert "aws" in names
    assert "postgresql" in names
    assert "ci_cd" in names

def test_realistic_jd_language_scores_above_seventy_with_good_overlap():
    resume = (
        "Backend software engineer with 4 years building REST APIs using Python and FastAPI. "
        "Implemented JWT auth, optimized PostgreSQL and SQL queries, deployed Docker services on AWS, "
        "and maintained CI/CD pipelines with Git."
    )
    job = (
        "We are hiring a backend software engineer to build and maintain scalable services and REST APIs "
        "using Python and FastAPI. Required: PostgreSQL, SQL, Docker, AWS, JWT, Git, and CI/CD. "
        "Nice to have: Kubernetes and Redis."
    )
    k = KeywordAnalyzer()
    out = k.run(resume, job)

    assert out["readinessScore"] >= 0.7
