# Test Plan — CareerMatch AI (Android)

## Scope
Validate history list interactions, persistence (DataStore), and basic flows.

## Environments
- Android Studio Giraffe+
- Pixel 6a emulator (API 34)

## Test Cases
### Unit (ViewModel)
1. setSort persists enum and reloads page 1 — **PASS**
2. setPageSize persists value and reloads page 1 — **PASS**
3. reloadFirstPage triggers listAnalyses(page=1) — **PASS**
4. deleteAnalysis reloads current page (happy path) — **MANUAL/NA in unit**

### UI (Compose)
1. Pull-to-refresh shows spinner and reloads page 1 — **PASS (manual)**
2. Copy-link icon copies URL and shows Snackbar — **PASS (manual)**
3. Delete icon opens confirm dialog — **PASS (automated)**
4. “Open Report” button opens external browser intent — **PASS (manual)**

## Evidence
- JUnit output attached (CI logs or local screenshots).
- AndroidTest report attached.

## Known Issues
- Network errors surfaced via Snackbar; retry button planned but not critical.

## Conclusion
App meets functional requirements for Task 3; docs + demo included for Task 4.
