# Architecture

This document summarizes the system architecture, key components, and request/data flow for CareerMatch AI.

## System Overview

```
+---------------------+        HTTPS         +----------------------------+
|  Android App        | <------------------> |  FastAPI Backend           |
|  (Jetpack Compose)  |                      |  (Auth, Analyze, Reports)  |
+----------+----------+                      +--------------+-------------+
           |                                               |
           | Room + DataStore                              | SQLAlchemy ORM
           |                                               |
           v                                               v
+---------------------+                      +----------------------------+
|  Local Storage      |                      |  Database                  |
|  (Room, DataStore)  |                      |  SQLite (dev) / Postgres   |
+---------------------+                      +----------------------------+
```

## Core Components

- **Android App (`frontend/`)**
  - Jetpack Compose UI
  - ViewModel + Repository pattern
  - Retrofit for API calls
  - Room for local history cache
  - DataStore for user preferences

- **FastAPI Backend (`backend/`)**
  - JWT authentication
  - `/analyze` endpoint with keyword scoring
  - `/analyses` history endpoints
  - HTML + CSV report rendering

- **Database**
  - SQLite for development
  - Postgres-compatible configuration for deployment

## API/Data Flow

### Authentication
1. User logs in with email + password.
2. Backend verifies credentials and returns JWT.
3. App stores token and uses it for protected endpoints.

### Analysis
1. User submits resume text + job description.
2. Backend extracts and weights keywords.
3. Backend computes readiness score + suggestions.
4. Results stored in database and returned to client.

### History + Reports
1. Client fetches analysis history with pagination.
2. Client opens `/reports/{analysis_id}` in a browser or shares link.
3. CSV export available via `/reports/{analysis_id}/csv`.

## Deployment Notes

- Backend is containerized and deployed on Render.
- Android app uses the deployed `BASE_URL` by default but can be pointed to local backend via `RetrofitProvider.kt`.

## Future Enhancements

- Replace keyword analysis with semantic embeddings.
- Move demo credentials to persistent user management.
- Optimize pagination with SQL-level queries.
