# KMA Library – plan implementacji

Plan pracy nad aplikacją do zarządzania przeczytanymi książkami i domową biblioteczką. Projekt jest technicznym "klonem" [kma-poker-stats](https://github.com/krzycuh/kma-poker-stats): ten sam stack, te same konwencje, ten sam sposób wdrożenia. Opis funkcjonalny i model domeny: [FUNCTIONALITY_DESIGN.md](FUNCTIONALITY_DESIGN.md). Decyzje architektoniczne: [adr/001-tech-stack-and-architecture.md](adr/001-tech-stack-and-architecture.md).

**Stack (odziedziczony z poker-stats):**
- Frontend: React 18 + TypeScript + Vite 6 + Tailwind 3 + TanStack Query 5 + React Hook Form + Zod + react-router 6 + react-hot-toast, Vitest + Testing Library, ESLint 9. PWA (manifest + ikony).
- Backend: Kotlin 1.9 + Spring Boot 3.3 (Web, Security, Validation, Data JPA, Actuator) + JWT (jjwt) + Flyway + PostgreSQL 16 + springdoc-openapi + bucket4j, ktlint, JUnit 5 + MockK + Kotest + Testcontainers + ArchUnit. Gradle Kotlin DSL, JDK 21.
- Infra: Docker (multi-stage + `Dockerfile.prebuilt`), docker-compose `localhost` i `prod`, nginx reverse proxy, GitHub Actions (CI backend/frontend + build i push obrazów do GHCR), produkcja na Raspberry Pi.

**Status:** fazy 0–8 zrealizowane (patrz `docs/PHASE_0-5_SUMMARY.md`, `docs/PHASE_6_SUMMARY.md`, `docs/PHASE_7_SUMMARY.md`, `docs/PHASE_8_SUMMARY.md`). Każda faza kończy się działającą, wdrażalną wersją.

---

## Zasady pracy (jak w poker-stats)

- DDD + Clean Architecture: `domain/` (czysty Kotlin, bez Springa), `application/` (use case'y, DTO), `infrastructure/` (JPA, REST, security), `config/`. Pilnowane testem ArchUnit.
- Domena jako niemutowalne `data class` z metodami zwracającymi kopię (`copy(...)`), ID jako `@JvmInline value class` na UUID, encje JPA osobno od domeny + mappery.
- Jeden use case = jedna klasa `@Service` z metodą `execute(command)`; wyjątki domenowe per moduł (`BookExceptions.kt`, `ShelfExceptions.kt`), mapowane w `GlobalExceptionHandler`.
- TDD dla domeny i use case'ów; testy integracyjne REST z Testcontainers; frontend: Vitest dla hooków/utili, Playwright później.
- Nazewnictwo: pakiet `pl.kmazurek.library`, grupa Gradle `pl.kmazurek`, obrazy `ghcr.io/krzycuh/kma-library/{backend,frontend}`, kontenery `kmalibrary-*`, baza `kmalibrary`.
- Mobile-first: wszystko ma działać jedną ręką na telefonie (dodanie książki w ≤ 3 polach).
- Każda faza: PR, zielone CI, krótki `docs/PHASE_N_SUMMARY.md`.

---

## Faza 0 – Bootstrap repozytorium (szkielet z poker-stats)

Cel: pusta aplikacja "hello world" z auth-less health checkiem, która buduje się w CI i uruchamia przez docker-compose.

1. Skopiować z poker-stats i przemianować (`pokerstats` → `kmalibrary`, `poker-stats` → `kma-library`, `PokerStats` → `KmaLibrary`, pakiet `pl.kmazurek` → `pl.kmazurek.library`):
   - `.gitignore` (root, `backend/`, `frontend/`), `README.md`, `local-run.md`, `prod-deploy.md`, `verify-setup.sh`
   - `backend/`: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, wrapper, `Dockerfile`, `Dockerfile.prebuilt`, `application.yml`, `logback-spring.xml`, `KmaLibraryApplication.kt`, `config/*` (bez `AsyncConfig` jeśli nie ma ntfy), `infrastructure/api/rest/config/*` (rate limiting, request logging), `GlobalExceptionHandler`, `HealthController`, `OpenApiConfig`, test ArchUnit, `application-test.yml`
   - `frontend/`: cały szkielet konfiguracyjny (`package.json`, `vite.config.ts`, `vitest.config.ts`, `tsconfig*`, `eslint.config.js`, `tailwind.config.js`, `postcss.config.js`, `index.html`, `nginx.conf`, `Dockerfile*`, `.env.development`, `.env.production`, `public/manifest.webmanifest` + nowe ikony), `src/api/client.ts`, `src/components/{AppLayout,Navigation,ProtectedRoute,ErrorBoundary,LoadingSpinner,LoadingSkeleton,EmptyState,ErrorMessage,ConfirmationModal,PageHeader}.tsx`, `src/hooks/useToast.ts`, `src/utils/{appInfo,format}.ts`, `src/test/setup.ts`
   - `docker-compose.localhost.yml`, `docker-compose.prod.yml` (bez osobnego nginx – nginx w obrazie frontendu), `.env.production.template`, `scripts/{backup-database,restore-database,create-admin-user}.sh`
   - `.github/workflows/{backend-ci,frontend-ci,deploy-production-optimized}.yml`
2. Poprawki względem poker-stats przy okazji kopiowania (drobne długi techniczne, które lepiej nie przenosić):
   - `backend-ci.yml` używa Javy 17, a build wymaga toolchain 21 – ujednolicić na 21.
   - `gradle.properties` zawiera ścieżkę do JDK specyficzną dla macOS (`org.gradle.java.installations.paths`) – usunąć, zostawić toolchain.
   - Root `.gitignore` ignoruje `gradle/` (negacja `!gradle-wrapper.jar` nie działa w ignorowanym katalogu) – zmienić na `.gradle/` i jawnie nie ignorować `backend/gradle/wrapper/`.
   - Redis i ntfy: **nie kopiować w MVP** (patrz ADR). `spring-boot-starter-cache` z prostym `ConcurrentMapCacheManager` zamiast Redisa; `NotificationService` jako port z implementacją `NoOp`, ntfy dopiero gdy pojawią się powiadomienia o pożyczeniach.
   - Frontend: Node 18 w CI i Dockerfile – podnieść do 20 LTS (Vite 6 wymaga ≥ 18, 20 jest bezpieczniejsze).
3. Testy architektoniczne (`DddLayerArchitectureTest`) – uruchomić od razu, z pustą listą repozytoriów.
4. CI zielone na pustym projekcie, `docker compose -f docker-compose.localhost.yml up` + `./gradlew bootRun` + `npm run dev` działają zgodnie z `local-run.md`.

**Deliverable:** repo z działającym `/actuator/health`, `/swagger-ui.html`, pustym SPA z layoutem.

---

## Faza 1 – Tożsamość i dostęp (kopiowane z poker-stats)

Cel: rejestracja, logowanie, refresh token, profil, admin użytkowników. Niemal wyłącznie kopiowanie i dostosowanie.

Backend:
- `domain/model/user/{User,UserId,Email,HashedPassword,UserRole}` (role `USER`/`ADMIN` zamiast `CASUAL_PLAYER`/`ADMIN`; dodatkowo `isActive` do blokowania kont).
- `domain/repository/UserRepository`, `infrastructure/persistence/{UserJpaEntity,JpaUserRepository,UserRepositoryImpl,UserMapper}`.
- `application/usecase/auth/{RegisterUser,LoginUser,RefreshAccessToken}`, `user/{GetCurrentUser,UpdateUserProfile,ChangeUserPassword}`, `application/dto/{AuthDtos,UserProfileDtos}`.
- `infrastructure/security/{JwtService,JwtAuthenticationFilter,PasswordEncoderService}`, `config/SecurityConfig`, `AuthController`, `UserController`, `AdminUserController`, `AuditLogger`.
- Flyway `V1__initial_schema.sql` (pełny schemat z FUNCTIONALITY_DESIGN §5 – od razu wszystkie tabele, żeby kolejne fazy nie dokładały migracji do pustej bazy) + `V2__insert_admin_user.sql`.
- Hook w `RegisterUser`: po utworzeniu użytkownika tworzona jest jego domyślna `Library` ("Biblioteczka {imię}") z członkiem `OWNER`. To jedyne miejsce, gdzie kontekst Identity wie o kontekście Shelf – zrobić przez use case `CreateDefaultLibrary` wywoływany z `RegisterUser`, nie przez zależność domenową.
- Testy: `UserTest`, `EmailTest`, `RegisterUserTest`, `LoginUserTest` (kopiowane), test integracyjny `AuthControllerIT` (Testcontainers).

Frontend:
- `types/auth.ts`, `api/auth.ts`, `contexts/AuthContext.tsx`, `hooks/useAuth.ts` (+ test), `pages/{Login,Register,Profile}.tsx`, `pages/admin/Users.tsx`, routing w `App.tsx`.

**Deliverable:** można założyć konto, zalogować się na telefonie, zmienić hasło; admin widzi użytkowników.

---

## Faza 2 – Katalog + przeczytane książki

Cel: pierwszy realny użytek – rejestr przeczytanych.

Backend:
- `domain/model/catalog/{Book,BookId,BookTitle,AuthorName,Isbn,BookSource}`; `Isbn` z walidacją ISBN-10/13 i normalizacją do 13 (TDD – to najbardziej "algorytmiczny" kawałek, dobry na start).
- `domain/service/BookDeduplicationKey` (normalizacja tytuł+autor: lowercase, bez diakrytyków, bez interpunkcji, zbite spacje).
- `domain/model/reading/{ReadingEntry,ReadingEntryId,Rating,FinishedPeriod,ReadingStatus}` + niezmienniki.
- `domain/repository/{BookRepository,ReadingEntryRepository}`; port `application/service/BookMetadataProvider` + `infrastructure/catalog/NoOpBookMetadataProvider`.
- Use case'y: `catalog/{FindOrCreateBook,SearchBooks,GetBook,UpdateBook}`, `reading/{AddReadingEntry,UpdateReadingEntry,DeleteReadingEntry,ListReadingEntries,GetReadingSummary}`. `AddReadingEntry` przyjmuje albo `bookId`, albo `title+authors(+isbn)` i wewnętrznie woła `FindOrCreateBook`.
- Kontrolery `BookController`, `ReadingEntryController`; DTO; `ReadingEntryRepository.findByReader(...)` z filtrami (rok, ocena, fraza) i paginacją (Spring Data `Specification` lub zapytania JPQL).
- Wyszukiwanie w katalogu: `ILIKE` na `title`/`authors` w MVP; rozszerzenie `pg_trgm` włączone już w `V1` (`CREATE EXTENSION IF NOT EXISTS pg_trgm`) i indeks GIN, żeby później przejść na podobieństwo bez migracji schematu.
- Testy: `IsbnTest`, `BookDeduplicationKeyTest`, `ReadingEntryTest`, `AddReadingEntryTest` (MockK), `ReadingEntryControllerIT`.

Frontend:
- `types/{book,reading}.ts`, `api/{books,readingEntries}.ts`.
- Komponenty: `BookPicker` (autocomplete po `/books?search=`, nowy tytuł = nowy wpis w katalogu), `RatingStars`, `YearMonthInput` (rok + opcjonalny miesiąc), `BookCover`.
- Strony: `/read` (lista + filtry), `/read/new`, `/read/:id/edit`, `/books/:id`.
- Dashboard w wersji minimalnej: licznik "w tym roku", ostatnie 5 wpisów, przycisk "+ przeczytana".

**Deliverable:** pełny cykl: dodaję przeczytaną książkę z telefonu w 20 sekund, widzę listę i oceny.

---

## Faza 3 – Biblioteczka i pożyczenia

Backend:
- `domain/model/shelf/{Library,LibraryId,LibraryMember,LibraryRole,ShelfItem,ShelfItemId,ShelfItemStatus,Loan,LoanId,BorrowerName}`; `ShelfItem` jako agregat z listą `loans` i metodami `lendTo`, `markReturned`, `markLost`, `giveAway` (TDD na niezmienniki: jedno aktywne pożyczenie, zwrot nie wcześniej niż pożyczenie).
- `domain/service/LibraryAccessPolicy` (`canAccess(userId, library)`), repozytoria `LibraryRepository`, `ShelfItemRepository` (ładuje egzemplarz razem z pożyczeniami), `LibraryMemberRepository`.
- Use case'y: `shelf/{ListMyLibraries,ListShelfItems,AddShelfItem,UpdateShelfItem,RemoveShelfItem,LendShelfItem,ReturnLoan,ListActiveLoans}`; każdy sprawdza `LibraryAccessPolicy`.
- Kontrolery `LibraryController`, `ShelfItemController`; DTO z zagnieżdżonym `activeLoan` i `loanHistory`.
- Testy: `ShelfItemTest`, `LendShelfItemTest`, `ReturnLoanTest`, `ShelfItemControllerIT` (w tym 403 dla cudzej biblioteczki).

Frontend:
- `types/shelf.ts`, `api/{libraries,shelfItems}.ts`.
- Strony: `/shelf` (zakładki Wszystkie / Na półce / Pożyczone), `/shelf/new`, `/shelf/:id` (karta egzemplarza z `LoanModal` i historią), `/loans` (dłużnicy, przeterminowane na czerwono).
- Powiązania: na `/shelf/:id` przycisk "Oznacz jako przeczytaną" (prefill `/read/new?bookId=`), na `/books/:id` sekcje "Moje czytania" i "Na półce".
- Dashboard: kafelek "Pożyczone: N" z linkiem do `/loans`.

**Deliverable:** pełne MVP funkcjonalne.

---

## Faza 4 – Dopracowanie UX i PWA

- Dashboard docelowy: statystyki roku (liczba, średnia ocena, top 3), ostatnio dodane, pożyczone, szybkie akcje.
- Stany puste (`EmptyState`) z CTA, skeletony ładowania, optymistyczne aktualizacje w TanStack Query (zwrot pożyczenia, ocena).
- Walidacja formularzy Zod + komunikaty PL; obsługa 409 (duplikat ISBN) z sugestią istniejącej książki.
- PWA: manifest, ikony, `apple-touch-icon`; sprawdzenie instalacji na iOS/Android.
- Dostępność: etykiety ARIA jak w poker-stats, focus ringi, obsługa klawiatury w `BookPicker`.
- i18n: na razie teksty PL na sztywno, ale w jednym module `src/i18n/pl.ts`, żeby dało się dodać EN.

---

## Faza 5 – Testy, wdrożenie, dokumentacja

- Playwright E2E: rejestracja → dodanie przeczytanej → dodanie na półkę → pożyczenie → zwrot.
- Pokrycie backendu: domena ~90%, use case'y ~80% (JaCoCo w `build.gradle.kts`, raport w CI jak w poker-stats).
- Release: tag `v0.1.0` → workflow `deploy-production-optimized.yml` buduje obrazy `linux/amd64,linux/arm64/v8` i pushuje do GHCR.
- Raspberry Pi: `docker-compose.prod.yml` + `.env.production` z `DATA_PATH` na SSD; `scripts/backup-database.sh` w cronie; `prod-deploy.md` z krokami aktualizacji.
- **HTTPS** już w tej fazie (potrzebne pod kamerę w fazie 7): reverse proxy z certyfikatem Let's Encrypt (np. Caddy przed nginx lub nginx + certbot) albo Cloudflare Tunnel. Decyzja w ADR 002.
- Docs: `ADMIN_GUIDE.md`, `TROUBLESHOOTING.md`, `USER_ONBOARDING.md` (wzorowane na poker-stats, krótsze).

---

## Faza 6 – ISBN i katalog publiczny

- Pole ISBN w `BookPicker` i formularzach; walidacja po stronie frontu (ta sama logika co `Isbn` VO, w `utils/isbn.ts` z testem).
- `OpenLibraryBookMetadataProvider` (Spring `RestClient`, timeout 3 s, cache w `CacheManager` po ISBN, mapowanie na `BookMetadata{title, authors, publishedYear, coverUrl}`); `GET /api/books/lookup?isbn=`. Źródła w `app.catalog.providers` (lista odpytywana po kolei, domyślnie `openlibrary,google`; `none` wyłącza).
- Flow w UI: wpisuję/skanuję ISBN → podgląd metadanych → "Dodaj na półkę" / "Dodaj jako przeczytaną" z prefillem.
- Okładki: przechowujemy tylko URL (jak avatary w poker-stats); brak uploadów w MVP.
- Admin: scalanie duplikatów książek (`POST /admin/books/{id}/merge-into/{targetId}` – przepina `reading_entries` i `shelf_items`).
- Testy: provider z WireMock/MockWebServer; kontrakt mapowania na kilku realnych odpowiedziach zapisanych w `src/test/resources`.

---

## Faza 7 – Skaner kodów kreskowych

- Komponent `BarcodeScanner`: `BarcodeDetector` API (Chrome/Android, format `ean_13`), fallback `@zxing/browser` (iOS Safari). Wynik EAN-13 z prefiksem 978/979 traktowany jako ISBN-13 → wywołanie lookup z fazy 6.
- Przycisk "Skanuj" obok pola ISBN; obsługa braku uprawnień do kamery i braku HTTPS (komunikat).
- Test manualny na iOS i Android; E2E tylko dla ścieżki "ręczne ISBN".

---

## Faza 8 – Biblioteczka rodzinna

- `POST /api/libraries/{id}/members` (zaproszenie po e-mailu istniejącego użytkownika), `DELETE .../members/{userId}`, rola `MEMBER` może dodawać/pożyczać, `OWNER` dodatkowo zarządza członkami.
- `LibraryAccessPolicy` rozszerzona o członków (jedyna zmiana w logice dostępu).
- UI: przełącznik biblioteczki w nagłówku `/shelf` (ukryty, gdy użytkownik ma jedną), ekran członków.
- Przeczytane pozostają osobiste; opcjonalnie podgląd "co przeczytali domownicy" jako osobna, świadoma decyzja (domyślnie prywatne).

---

## Decyzje podjęte przed fazą 2 (2026-10-22)

| # | Pytanie | Decyzja |
|---|---------|------------|
| 1 | Skala oceny | 1–5 gwiazdek (połówki nie) |
| 2 | Data przeczytania | opcjonalna; **rok + opcjonalny miesiąc, bez dnia** (`FinishedPeriod`) |
| 3 | Wiele autorów | jedno pole tekstowe, rozdzielane `;` lub `,`, przechowywane jako lista |
| 4 | Czy katalog książek wspólny dla wszystkich użytkowników | tak (deduplikacja po ISBN, potem po tytuł+autor); edycja: twórca lub admin |
| 5 | Redis/ntfy w MVP | nie; cache in-memory, powiadomienia dopiero przy przeterminowanych pożyczeniach |
| 6 | Rejestracja otwarta | tak, z flagą `app.registration.enabled` do wyłączenia |
| 7 | Język UI | polski; teksty w jednym module, bez biblioteki i18n w MVP |
| 8 | Pakiet Kotlin | `pl.kmazurek.library` (poker-stats ma `pl.kmazurek`; osobny podpakiet unika kolizji w IDE przy obu projektach) |
