# ADR 001: Stack technologiczny i architektura

**Status:** Zaakceptowany
**Data:** 2026-10-22

## Kontekst

KMA Library to druga aplikacja domowa po [kma-poker-stats](https://github.com/krzycuh/kma-poker-stats). Ma działać na tym samym Raspberry Pi, być utrzymywana przez tę samą osobę i korzystać z tych samych narzędzi, żeby wiedza, skrypty i pipeline'y przenosiły się 1:1. Model danych musi być na tyle modularny, by biblioteczka mogła w przyszłości stać się wspólna (rodzinna), a lista przeczytanych pozostała osobista; katalog książek musi dać się później zasilać z zewnętrznych źródeł (ISBN, Open Library, skaner).

## Decyzja

### Stack identyczny z poker-stats

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query + React Hook Form + Zod + react-router; Vitest + Testing Library; PWA.
- **Backend:** Kotlin + Spring Boot 3.x (Web, Security, Validation, Data JPA, Actuator), JWT (access + refresh), Flyway, springdoc-openapi, bucket4j; JUnit 5 + MockK + Kotest + Testcontainers + ArchUnit; Gradle Kotlin DSL, JDK 21, ktlint.
- **Baza:** PostgreSQL 16.
- **Infra:** Docker multi-stage + `Dockerfile.prebuilt`, docker-compose (`localhost`, `prod`), nginx reverse proxy, GitHub Actions (CI + obrazy do GHCR), Raspberry Pi z danymi na SSD.

Uzasadnienie: zero kosztu nauki, możliwość kopiowania całych modułów (auth, layout, CI, deploy), spójne operacje (backup, aktualizacja) dla obu aplikacji.

### Odstępstwa od poker-stats

| Element | poker-stats | kma-library | Powód |
|---------|-------------|-------------|-------|
| Redis | tak (cache statystyk) | **nie w MVP**; `spring-boot-starter-cache` z `ConcurrentMapCacheManager` | brak kosztownych agregacji; mniej RAM na Pi; przełączenie na Redis to zmiana konfiguracji |
| ntfy | tak (powiadomienie o rejestracji) | port `NotificationService` + `NoOp`; ntfy dopiero przy powiadomieniach o pożyczeniach | YAGNI |
| Role | `CASUAL_PLAYER`/`ADMIN` | `USER`/`ADMIN` | nazewnictwo domeny |
| Pakiet | `pl.kmazurek` | `pl.kmazurek.library` | unikanie kolizji przy pracy w obu projektach |
| Java w CI | 17 (niespójne z toolchain 21) | 21 | poprawka błędu |
| Node | 18 | 20 LTS | wsparcie |

### Architektura: DDD + Clean Architecture, cztery bounded contexts

```
pl.kmazurek.library
├── domain/            # czysty Kotlin: model/{user,catalog,reading,shelf,shared}, repository/, service/
├── application/       # usecase/{auth,user,catalog,reading,shelf}, dto/, service/ (porty: BookMetadataProvider, NotificationService)
├── infrastructure/    # persistence/ (JPA entity + mapper + impl), api/rest/, security/, catalog/ (providery), notification/
└── config/
```

Kluczowe decyzje modelu:

1. **Catalog jest współdzielony i niezależny.** `Book` nie zna użytkowników. Reading i Shelf odwołują się do `BookId`. Integracje zewnętrzne (ISBN, Open Library, skaner) to wyłącznie port `BookMetadataProvider` i jego implementacje w `infrastructure/catalog`.
2. **Shelf jest własnością `Library`, nie użytkownika.** `ShelfItem.libraryId` zamiast `userId`; `LibraryMember` z rolą. W MVP jedna biblioteczka na użytkownika tworzona przy rejestracji. Biblioteczka rodzinna = dodanie członków, bez zmiany schematu pozycji i pożyczeń. Dostęp sprawdzany w jednym miejscu: `LibraryAccessPolicy`.
3. **Reading jest osobiste.** `ReadingEntry.readerId = UserId` wprost; żadnego współdzielenia bez świadomej decyzji w przyszłości.
4. **`ShelfItem` to egzemplarz, `Loan` żyje w jego agregacie.** Niezmiennik "jedno aktywne pożyczenie" jest zarówno w domenie, jak i w częściowym indeksie unikalnym w bazie.
5. **Pełny schemat w `V1`.** Wszystkie tabele MVP w jednej migracji; kolejne migracje tylko dla rozszerzeń (statusy czytania, członkowie).
6. **`Isbn` jako value object** z walidacją i normalizacją do ISBN-13 od fazy 2, mimo że pole jest opcjonalne; dzięki temu faza 6 nie zmienia modelu.

### Testowanie

Jak w poker-stats: TDD dla domeny i use case'ów, ArchUnit pilnuje warstw, Testcontainers dla REST, Vitest dla frontu, Playwright dla ścieżek krytycznych w fazie 5.

## Konsekwencje

- Plus: szybki start (faza 0–1 to głównie kopiowanie), jeden sposób utrzymania obu aplikacji, model gotowy na rodzinę i katalogi zewnętrzne bez przebudowy.
- Minus: Spring Boot na Pi obok poker-stats to drugi JVM (~400–600 MB RAM); trzeba pilnować `JAVA_OPTS` i rozważyć wspólnego Postgresa dla obu aplikacji (osobne bazy) w przyszłości.
- Minus: współdzielony katalog wymaga polityki edycji (twórca/admin) i narzędzia do scalania duplikatów (faza 6).
