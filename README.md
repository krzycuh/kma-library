# KMA Library

Aplikacja webowa (mobile-first, PWA) do prowadzenia rejestru przeczytanych książek i domowej biblioteczki z ewidencją pożyczeń. Techniczny bliźniak [kma-poker-stats](https://github.com/krzycuh/kma-poker-stats): ten sam stack, te same konwencje i sposób wdrożenia.

## Funkcje

- **Przeczytane** – tytuł, autor, data (dzień / miesiąc / rok), ocena 1–5, notatka; lista z filtrami i podsumowaniem roku.
- **Biblioteczka** – egzemplarze na półce, status (na półce / pożyczona / zgubiona / oddana), pożyczenie komuś (imię i nazwisko, data, termin), zwrot, historia pożyczeń, lista dłużników.
- **Katalog książek** – wspólny słownik tytułów (deduplikacja po ISBN lub tytule i autorze); w planach ISBN + Open Library i skaner kodów kreskowych.
- **W planach:** biblioteczka rodzinna (wspólna półka, osobiste listy przeczytanych).

## Stack

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query
- **Backend:** Kotlin + Spring Boot 3 + Spring Security (JWT) + Flyway
- **Baza:** PostgreSQL 16
- **Infra:** Docker + Docker Compose + nginx + GitHub Actions (GHCR), produkcja na Raspberry Pi

## Dokumentacja

- [Plan implementacji](docs/IMPLEMENTATION_PLAN.md) – fazy 0–8, zakres każdej, pytania otwarte
- [Projekt funkcjonalny i model domeny](docs/FUNCTIONALITY_DESIGN.md) – bounded contexts, agregaty, schemat SQL, API, ekrany
- [ADR 001 – stack i architektura](docs/adr/001-tech-stack-and-architecture.md)

## Status

**Faza planowania** – kod aplikacji powstanie w fazie 0 (bootstrap szkieletu z poker-stats). Zobacz plan implementacji.
