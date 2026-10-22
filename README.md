# KMA Library

Aplikacja webowa (mobile-first, PWA) do prowadzenia rejestru przeczytanych książek i domowej biblioteczki z ewidencją pożyczeń. Techniczny bliźniak [kma-poker-stats](https://github.com/krzycuh/kma-poker-stats): ten sam stack, te same konwencje i sposób wdrożenia.

## Funkcje

- **Przeczytane** – tytuł, autor, rok i opcjonalnie miesiąc przeczytania, ocena 1–5, notatka; lista z filtrami (rok, ocena, szukaj) i podsumowaniem roku.
- **Biblioteczka** – egzemplarze na półce, status (na półce / pożyczona / zgubiona / oddana), pożyczenie komuś (imię i nazwisko, data, termin), zwrot, historia pożyczeń, lista dłużników z zaznaczeniem przeterminowanych.
- **Katalog książek** – wspólny słownik tytułów (deduplikacja po ISBN lub po znormalizowanym tytule i autorze), autouzupełnianie przy dodawaniu, karta książki łącząca czytania i egzemplarze.
- **Konta** – rejestracja (wyłączalna flagą), logowanie JWT z refresh tokenem, profil, panel admina (blokada konta, reset hasła).
- **W planach** (fazy 6–8): ISBN + Open Library, skaner kodów kreskowych, biblioteczka rodzinna.

## Stack

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query + React Hook Form + Zod; Vitest, Playwright; PWA
- **Backend:** Kotlin + Spring Boot 3 + Spring Security (JWT) + Spring Data JPA + Flyway; JUnit 5 + MockK + ArchUnit; ktlint
- **Baza:** PostgreSQL 16
- **Infra:** Docker + Docker Compose + nginx + GitHub Actions (obrazy w GHCR), produkcja na Raspberry Pi

## Struktura

```
kma-library/
├── backend/     # Kotlin + Spring Boot (pakiet pl.kmazurek.library: domain / application / infrastructure / config)
├── frontend/    # React + TypeScript (src/pages, components, api, types, i18n)
├── docs/        # plan, projekt funkcjonalny, ADR, podsumowania faz
├── nginx/       # reverse proxy (prod)
├── scripts/     # backup / restore / admin
└── .github/workflows/
```

## Szybki start (lokalnie)

```bash
docker compose -f docker-compose.localhost.yml up -d   # PostgreSQL
cd backend && ./gradlew bootRun                         # http://localhost:8080 (Swagger: /swagger-ui.html)
cd frontend && npm install && npm run dev               # http://localhost:5173
```

Domyślny administrator: `admin@example.com` / `admin123` (zmień hasło po pierwszym logowaniu).

## Testy

```bash
cd backend && ./gradlew ktlintCheck test     # testy jednostkowe + ArchUnit
cd frontend && npm run lint && npm run test  # ESLint + Vitest
cd frontend && npm run test:e2e              # Playwright, wymaga działającego backendu + bazy
```

## Wdrożenie

Zobacz [prod-deploy.md](prod-deploy.md) (obrazy z GHCR, `docker-compose.prod.yml`, `.env.production.template`) oraz [ADR 002 – HTTPS](docs/adr/002-https.md).

## Dokumentacja

- [Plan implementacji](docs/IMPLEMENTATION_PLAN.md) – fazy 0–8 i podjęte decyzje
- [Projekt funkcjonalny i model domeny](docs/FUNCTIONALITY_DESIGN.md)
- [ADR 001 – stack i architektura](docs/adr/001-tech-stack-and-architecture.md), [ADR 002 – HTTPS](docs/adr/002-https.md)
- [Podsumowanie faz 0–5](docs/PHASE_0-5_SUMMARY.md)

## Status

Fazy 0–5 zrealizowane: działające MVP (auth, przeczytane, biblioteczka, pożyczenia, dashboard, PWA, CI, konfiguracja wdrożenia).
