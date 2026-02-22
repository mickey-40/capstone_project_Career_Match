# CareerMatch AI

CareerMatch AI is a full-stack mobile application that helps job seekers compare resume content against a target job description and get actionable feedback before applying.

This capstone project demonstrates end-to-end product delivery across Android, API design, authentication, persistence, and testing.

## Demo

- Live backend/API: `https://d424-software-engineering-capstone-tdvq.onrender.com`
- API docs (Swagger): `https://d424-software-engineering-capstone-tdvq.onrender.com/docs`
- HTML report example format: `GET /reports/{analysis_id}`

If you are reviewing this for hiring, the fastest walkthrough is:
1. Open Swagger docs.
2. Login with the demo account.
3. Run `/analyze`.
4. Open the generated `/reports/{analysis_id}` page.

## Business Problem

Job seekers often tailor resumes manually and struggle to identify which required skills are missing for a role.

CareerMatch AI reduces this friction by:
- Extracting and comparing resume/job keywords.
- Estimating a readiness score.
- Returning prioritized suggestions for improvement.
- Preserving analysis history for iteration.

## Key Features

- JWT-based authentication flow for protected endpoints.
- Resume/job analysis endpoint with weighted keyword matching.
- Readiness scoring plus structured skill and suggestion output.
- Analysis history with pagination, search, sorting, and deletion.
- Shareable HTML and CSV report generation endpoints.
- Android app built with Jetpack Compose for login, analysis, and history workflows.
- Pull-to-refresh, deep-link/report sharing, and persistent user preferences (theme/demo mode/page settings).

## Tech Stack

- Frontend: Kotlin, Jetpack Compose, Retrofit, Room, DataStore
- Backend: Python, FastAPI, SQLAlchemy, Pydantic
- Data: SQLite for development, PostgreSQL-compatible configuration for deployment
- Auth: JWT (`python-jose`), password hashing (`passlib`)
- Testing: Pytest (backend), JUnit/Android tests (frontend)
- Deployment: Dockerized backend (Render endpoint currently configured in app client)

## Architecture

- `frontend/`: Android client (UI, ViewModel, repository, local preferences)
- `backend/`: FastAPI service (auth, analysis, persistence, reports)
- `firebase_site/`: static hosting scaffold

High-level flow:
1. User logs in from Android app.
2. App stores token and calls protected FastAPI endpoints.
3. `/analyze` computes skills/suggestions and stores analysis rows.
4. App fetches history from `/analyses`.
5. Reports are opened/shared via `/reports/{analysis_id}`.

## API Snapshot

- `GET /health` - service health check
- `POST /auth/login` - issue bearer token
- `POST /analyze` - run resume/job analysis
- `GET /analyses` - list analysis history (paged)
- `GET /analyses/{analysis_id}` - fetch one analysis
- `DELETE /analyses/{analysis_id}` - delete analysis
- `GET /reports/{analysis_id}` - HTML report
- `GET /reports/{analysis_id}/csv` - CSV export

## My Contribution / Scope

This project was built as an individual capstone and includes:
- Full API design and implementation (FastAPI + SQLAlchemy).
- Analysis engine integration with extensibility hooks (keyword strategy now, embedding/hybrid path prepared).
- Auth, persistence model, and report rendering.
- Android UI flows and state management for login, analysis, and history management.
- Automated tests for analyzer behavior and API flows.

## Run Locally

### Prerequisites

- Python 3.11+
- Android Studio (for mobile app)
- JDK 17

### Backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

export DATABASE_URL=sqlite:///./career_match_ai.db
export JWT_SECRET=change-me
export DEMO_EMAIL=demo@example.com
export DEMO_PASSWORD=Password123!

uvicorn app.main:app --reload
```

API will run at `http://127.0.0.1:8000` with docs at `/docs`.

### Frontend (Android)

```bash
cd frontend
./gradlew assembleDebug
```

Then open the `frontend/` project in Android Studio and run on an emulator/device.

Note: The current app `BASE_URL` points to the deployed backend. Update `frontend/app/src/main/java/com/example/careermatchai/data/remote/RetrofitProvider.kt` if you want to target local backend.

## Testing

### Backend tests

```bash
cd backend
pytest -q
```

### Frontend tests

```bash
cd frontend
./gradlew test
./gradlew connectedAndroidTest
```

Additional manual/acceptance coverage is documented in `TEST_PLAN.md`.

## Engineering Decisions

- FastAPI + Pydantic for typed contracts and quick API iteration.
- SQLAlchemy ORM for portable persistence and clean model boundaries.
- JWT bearer auth to model real protected-resource flows.
- Compose + ViewModel architecture for reactive Android UI state.
- DataStore preference persistence for user settings and UX continuity.

## Limitations and Next Steps

- Current analyzer is keyword-based; next step is semantic matching using embeddings.
- Demo credential store should be replaced with persistent user management.
- Pagination currently slices in memory after query; can be optimized with SQL-level pagination/count.
- CI pipeline and production monitoring can be expanded.

## Recruiter Notes

This repository is intended to showcase:
- Full-stack ownership from product idea to deployed API.
- Mobile + backend integration skills.
- Practical software engineering tradeoffs (security, data modeling, UX, testing).

If helpful during interview review, I can provide:
- A 3-5 minute guided demo walkthrough.
- Architecture deep-dive and tradeoff discussion.
- Planned roadmap for production hardening.
