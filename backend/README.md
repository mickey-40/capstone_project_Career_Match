# AI Resume Analyzer – Backend


## Run Locally (SQLite)
```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export DATABASE_URL=sqlite:///./dev.db
export JWT_SECRET=change-me
uvicorn app.main:app --reload

## Semantic Matching (v2)

Semantic analysis uses local sentence-transformers embeddings.
On first run, the model `all-MiniLM-L6-v2` is downloaded and cached.

Enable semantic or hybrid strategy via `strategy` in the `/analyze` request:

```json
{
  "resumeText": "...",
  "jobText": "...",
  "strategy": "embedding"
}
```

Valid strategies:
- `keyword` (default)
- `embedding`
- `hybrid`
