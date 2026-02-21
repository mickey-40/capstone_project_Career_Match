# AI Resume Analyzer – Backend


## Run Locally (SQLite)
```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export DATABASE_URL=sqlite:///./dev.db
export JWT_SECRET=change-me
uvicorn app.main:app --reload