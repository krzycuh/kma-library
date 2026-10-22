# Fazy 0–5 – podsumowanie realizacji

Data: 2026-10-22. Zakres: bootstrap, auth, katalog + przeczytane, biblioteczka + pożyczenia, UX/PWA, testy i wdrożenie.

## Co powstało

### Backend (`backend/`, pakiet `pl.kmazurek.library`)
- Szkielet skopiowany z poker-stats: Gradle (JDK 21, ktlint, JaCoCo), Spring Boot 3.3, Security + JWT, Flyway, springdoc, bucket4j, request logging, audit log, `GlobalExceptionHandler`. Bez Redisa i ntfy (`NoOpNotificationService`).
- Konteksty i agregaty: `User`, `Book` (+ `Isbn`, `BookDeduplicationKey`), `ReadingEntry` (+ `FinishedPeriod`, `Rating`), `Library`/`LibraryMember`, `ShelfItem` (+ `Loan`, `BorrowerName`), `LibraryAccessPolicy`.
- Use case'y: auth (register tworzy domyślną biblioteczkę, login odrzuca zablokowane konta), user/admin (lista, blokada, reset hasła), catalog (`FindOrCreateBook` z deduplikacją po ISBN i tytuł+autor, `SearchBooks`, `UpdateBook`), reading (CRUD + `GetReadingSummary`), shelf (CRUD egzemplarzy, `LendShelfItem`, `ReturnLoan`, `ListActiveLoans`), `GetDashboard`.
- Persystencja: encje JPA osobno od domeny, mappery, `ShelfItem` z kaskadą pożyczeń; migracje `V1__initial_schema.sql` (pełny schemat, `pg_trgm`, częściowy indeks unikalny na aktywne pożyczenie) i `V2__insert_admin_user.sql`.
- REST: `/api/auth`, `/api/users/me`, `/api/admin/users`, `/api/books`, `/api/reading-entries`, `/api/libraries`, `/api/shelf-items`, `/api/dashboard`.
- Testy: 85 testów jednostkowych (domena, use case'y, ArchUnit) – `./gradlew test`.

### Frontend (`frontend/`)
- Szkielet z poker-stats (Vite 6, React 18, Tailwind, TanStack Query, RHF + Zod, PWA). Node 20.
- Teksty PL w jednym module `src/i18n/pl.ts`.
- Komponenty: `BookPicker` (autocomplete z katalogu), `RatingStars`, `YearMonthInput`, `LoanModal`, `BookCover`, `ShelfItemStatusBadge`, `Pagination`.
- Strony: Dashboard, Przeczytane (lista + filtry + formularz), Półka (lista, formularz, karta egzemplarza z pożyczeniem/zwrotem/historią), Pożyczone, Książka (katalog), Profil, Admin/Użytkownicy, Login/Rejestracja.
- Testy: Vitest (utils, hook auth), Playwright E2E `e2e/library-flow.spec.ts` (rejestracja → przeczytana → półka → pożyczenie → zwrot) uruchamiany na pełnym stacku.

### Infrastruktura
- `docker-compose.localhost.yml` (tylko Postgres), `docker-compose.prod.yml` (Postgres, backend, frontend, nginx) – bez Redisa i ntfy.
- GitHub Actions: `backend-ci.yml`, `frontend-ci.yml`, `deploy-production.yml` (obrazy multi-arch do GHCR).
- `prod-deploy.md`, `.env.production.template`, skrypty backupu/restore, ADR 002 (HTTPS).

## Odstępstwa od pierwotnego planu
- Data przeczytania: rok + opcjonalny miesiąc (decyzja użytkownika), bez precyzji dnia.
- Zwrot pożyczenia: `POST /shelf-items/{id}/return` (zamiast ścieżki z `loanId`) – egzemplarz ma najwyżej jedno aktywne pożyczenie.
- Admin: osobne endpointy PATCH na blokadę i reset hasła.

## Jak uruchomić lokalnie
```bash
docker compose -f docker-compose.localhost.yml up -d
cd backend && ./gradlew bootRun          # http://localhost:8080/swagger-ui.html
cd frontend && npm install && npm run dev # http://localhost:5173
# admin@example.com / admin123
```

## Następne kroki (fazy 6–8)
ISBN + Open Library (`BookMetadataProvider`), skaner kodów kreskowych (wymaga HTTPS – ADR 002), biblioteczka rodzinna (`LibraryMember` już w schemacie).
