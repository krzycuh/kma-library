# KMA Library – projekt funkcjonalny i model domeny

Aplikacja webowa (mobile-first, PWA) do zarządzania:

1. **Przeczytanymi książkami** – co przeczytałem, kiedy, jak mi się podobało.
2. **Biblioteczką** – jakie książki mam na półce i komu je pożyczyłem.

Oba zbiory są niezależne (mogą, ale nie muszą się pokrywać). W pierwszej wersji każdy użytkownik ma własny zbiór przeczytanych książek i własną biblioteczkę. Model jest zaprojektowany tak, żeby w przyszłości biblioteczka mogła być **wspólna (rodzinna)**, a lista przeczytanych pozostała **osobista**.

---

## 1. Role użytkowników

| Rola | Uprawnienia |
|------|-------------|
| `USER` | Własny rejestr przeczytanych, własna biblioteczka, pożyczenia, edycja profilu |
| `ADMIN` | To co `USER` + zarządzanie użytkownikami (lista, blokowanie, reset hasła), edycja/scalanie wpisów katalogu |

Rejestracja: otwarta (jak w poker-stats, z rate-limitingiem), z możliwością wyłączenia flagą konfiguracyjną `app.registration.enabled`.

---

## 2. Bounded contexts

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  Identity & Access   │  │   Catalog            │  │   Reading            │  │   Shelf              │
│                      │  │   (katalog książek)  │  │   (przeczytane)      │  │   (biblioteczka)     │
│  - User              │  │  - Book              │  │  - ReadingEntry      │  │  - Library           │
│  - UserRole          │  │  - Isbn              │  │  - Rating            │  │  - LibraryMember     │
│  - JWT / refresh     │  │  - BookMetadata      │  │                      │  │  - ShelfItem (copy)  │
│                      │  │    Provider (port)   │  │                      │  │  - Loan              │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘  └──────────────────────┘
```

Zasady:

- **Catalog** jest wspólnym słownikiem książek (tytuł, autor, ISBN, okładka). Nie wie nic o tym, kto co przeczytał ani kto co ma. Dzięki temu integracja z katalogiem publicznym (ISBN, Open Library) dotyka tylko tego kontekstu.
- **Reading** i **Shelf** odwołują się do książki wyłącznie przez `BookId`. Nie mają między sobą zależności.
- **Shelf** nie odwołuje się do `UserId` bezpośrednio przy pozycjach na półce, tylko przez `LibraryId`. Biblioteczka ma właściciela i członków. W MVP każda biblioteczka ma dokładnie jednego członka (właściciela) i jest tworzona automatycznie przy rejestracji. Wariant rodzinny to wyłącznie dodanie członków, bez zmiany schematu pozycji i pożyczeń.
- **Reading** odwołuje się bezpośrednio do `UserId` (czytelnik), bo to zbiór z założenia osobisty.

---

## 3. Model domeny (Kotlin, warstwa `domain`)

### 3.1 Identity & Access (kopiowane z poker-stats)

- `User(id: UserId, email: Email, passwordHash: HashedPassword, name: String, role: UserRole, createdAt, updatedAt)`
- `UserRole { USER, ADMIN }`

### 3.2 Catalog

```kotlin
data class Book(
    val id: BookId,
    val title: BookTitle,            // VO: niepuste, max 500 znaków
    val authors: List<AuthorName>,   // VO: min 1 autor; w UI jedno pole "Autor" rozdzielane przecinkiem/";"
    val isbn: Isbn? = null,          // VO: ISBN-10/13, normalizowany do 13, walidacja sumy kontrolnej
    val publishedYear: Int? = null,
    val coverUrl: String? = null,
    val source: BookSource,          // MANUAL | OPEN_LIBRARY | GOOGLE_BOOKS ...
    val externalId: String? = null,  // id w katalogu zewnętrznym
    val createdByUserId: UserId,
    val createdAt, val updatedAt,
)
```

- Książka jest **współdzielona** między użytkownikami (jeden rekord "Lem – Solaris" dla wszystkich).
- Deduplikacja: przy dodawaniu use case `FindOrCreateBook` szuka po ISBN, a gdy brak ISBN – po znormalizowanym `title + authors` (lower-case, bez znaków diakrytycznych i interpunkcji). Jeśli znajdzie, zwraca istniejącą.
- Edycja wpisu katalogu: twórca lub `ADMIN`. Scalanie duplikatów: `ADMIN` (faza 6+).
- Port: `interface BookMetadataProvider { fun findByIsbn(isbn: Isbn): BookMetadata? ; fun search(query: String): List<BookMetadata> }` – w MVP implementacja `NoOpBookMetadataProvider`, w fazie 6 `OpenLibraryBookMetadataProvider`.

### 3.3 Reading (przeczytane)

```kotlin
data class ReadingEntry(
    val id: ReadingEntryId,
    val readerId: UserId,
    val bookId: BookId,
    val finishedOn: LocalDate?,            // null = "nie pamiętam kiedy"
    val datePrecision: DatePrecision,      // DAY | MONTH | YEAR – UI pozwala wpisać sam rok lub miesiąc
    val rating: Rating? = null,            // VO: 1..5
    val review: String? = null,            // krótka notatka
    val createdAt, val updatedAt,
)
```

- Jedna książka może mieć wiele wpisów u jednego czytelnika (ponowne czytanie).
- Niezmienniki: `finishedOn` nie może być z przyszłości; `rating` w zakresie 1..5.
- Status "w trakcie czytania" / "chcę przeczytać" **nie** jest w MVP, ale pole `status: ReadingStatus { FINISHED }` jest przygotowane jako enum, żeby dodać `IN_PROGRESS`, `WANT_TO_READ`, `ABANDONED` bez migracji modelu domenowego (migracja SQL: tylko nowe wartości).

### 3.4 Shelf (biblioteczka)

```kotlin
data class Library(                       // aggregate root
    val id: LibraryId,
    val name: String,                     // "Biblioteczka Krzyśka"
    val ownerId: UserId,
    val createdAt, val updatedAt,
)

data class LibraryMember(                 // MVP: zawsze 1 wiersz = właściciel
    val libraryId: LibraryId,
    val userId: UserId,
    val role: LibraryRole,                // OWNER | MEMBER
)

data class ShelfItem(                     // aggregate root – fizyczny egzemplarz na półce
    val id: ShelfItemId,
    val libraryId: LibraryId,
    val bookId: BookId,
    val acquiredOn: LocalDate? = null,
    val notes: String? = null,
    val status: ShelfItemStatus,          // ON_SHELF | LENT | LOST | GIVEN_AWAY
    val createdAt, val updatedAt,
)

data class Loan(                          // entity wewnątrz agregatu ShelfItem
    val id: LoanId,
    val shelfItemId: ShelfItemId,
    val borrower: BorrowerName,           // VO: firstName + lastName (niepuste)
    val lentOn: LocalDate,
    val dueOn: LocalDate? = null,
    val returnedOn: LocalDate? = null,
    val notes: String? = null,
)
```

Niezmienniki agregatu `ShelfItem`:

- Co najwyżej **jedno aktywne pożyczenie** (`returnedOn == null`) na egzemplarz.
- `lendTo(borrower, lentOn)` dozwolone tylko gdy `status == ON_SHELF`; ustawia `status = LENT`.
- `markReturned(returnedOn)` wymaga aktywnego pożyczenia, `returnedOn >= lentOn`; ustawia `status = ON_SHELF`.
- Historia pożyczeń jest zachowywana (lista `loans`), co daje "komu kiedyś pożyczałem tę książkę".
- Ta sama książka może występować jako kilka egzemplarzy (dwa `ShelfItem` z tym samym `bookId`).

Autoryzacja: operacja na `ShelfItem` wymaga, by aktualny użytkownik był członkiem `Library` (`LibraryAccessPolicy` w warstwie domain/service). W MVP sprowadza się to do `library.ownerId == currentUser`, ale punkt rozszerzenia jest jeden.

---

## 4. Historyjki użytkownika (MVP)

**Auth**
- Jako nowy użytkownik rejestruję się e-mailem i hasłem i od razu mam pustą biblioteczkę.
- Jako użytkownik loguję się i pozostaję zalogowany (refresh token) na telefonie.

**Przeczytane**
- Dodaję przeczytaną książkę podając tytuł i autora; opcjonalnie datę (rok / miesiąc / dzień), ocenę 1–5 i notatkę.
- Przeglądam listę przeczytanych posortowaną po dacie; filtruję po roku i ocenie; wyszukuję po tytule/autorze.
- Edytuję i usuwam wpis.
- Widzę proste podsumowanie: liczba książek w tym roku, średnia ocena, najlepiej ocenione.

**Biblioteczka**
- Dodaję książkę na półkę (tytuł + autor, opcjonalnie data nabycia, notatka).
- Przeglądam półkę; filtruję: wszystkie / na półce / pożyczone.
- Pożyczam egzemplarz: imię i nazwisko, data (domyślnie dziś), opcjonalnie termin zwrotu.
- Oznaczam zwrot. Widzę historię pożyczeń egzemplarza.
- Widzę listę "aktualnie pożyczone" z nazwiskiem i datą (lista dłużników).
- Z karty książki w biblioteczce jednym kliknięciem dodaję wpis "przeczytane" (i odwrotnie: z wpisu przeczytanego dodaję na półkę) – wspólny `BookId`.

**Admin**
- Przeglądam użytkowników, blokuję konto, resetuję hasło (jak `AdminUserController` w poker-stats).

---

## 5. Schemat bazy (Flyway `V1__initial_schema.sql`)

```sql
users (id UUID PK, email UNIQUE, password_hash, name, role CHECK IN ('USER','ADMIN'), is_active, created_at, updated_at)

books (
  id UUID PK, title VARCHAR(500) NOT NULL, authors TEXT NOT NULL,          -- autorzy jako "A; B"
  normalized_key VARCHAR(600) NOT NULL,                                     -- do deduplikacji (title+authors)
  isbn13 CHAR(13) NULL, published_year INT NULL, cover_url VARCHAR(500),
  source VARCHAR(30) NOT NULL DEFAULT 'MANUAL', external_id VARCHAR(100),
  created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at, updated_at
)
UNIQUE INDEX ux_books_isbn13 ON books(isbn13) WHERE isbn13 IS NOT NULL
INDEX idx_books_normalized_key ON books(normalized_key)
INDEX idx_books_title_trgm ON books USING gin (title gin_trgm_ops)       -- pg_trgm do wyszukiwania

reading_entries (
  id UUID PK, reader_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  book_id UUID NOT NULL REFERENCES books(id),
  status VARCHAR(20) NOT NULL DEFAULT 'FINISHED',
  finished_on DATE NULL, date_precision VARCHAR(10) NOT NULL DEFAULT 'DAY',
  rating SMALLINT NULL CHECK (rating BETWEEN 1 AND 5), review TEXT,
  created_at, updated_at
)
INDEX idx_reading_entries_reader_finished ON reading_entries(reader_id, finished_on DESC)

libraries (id UUID PK, name VARCHAR(255) NOT NULL, owner_id UUID NOT NULL REFERENCES users(id), created_at, updated_at)

library_members (library_id UUID REFERENCES libraries(id) ON DELETE CASCADE, user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                 role VARCHAR(20) NOT NULL, PRIMARY KEY (library_id, user_id))

shelf_items (
  id UUID PK, library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
  book_id UUID NOT NULL REFERENCES books(id),
  status VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF', acquired_on DATE, notes TEXT,
  created_at, updated_at
)
INDEX idx_shelf_items_library ON shelf_items(library_id, status)

loans (
  id UUID PK, shelf_item_id UUID NOT NULL REFERENCES shelf_items(id) ON DELETE CASCADE,
  borrower_first_name VARCHAR(100) NOT NULL, borrower_last_name VARCHAR(100) NOT NULL,
  lent_on DATE NOT NULL, due_on DATE, returned_on DATE, notes TEXT,
  created_at, updated_at
)
UNIQUE INDEX ux_loans_active ON loans(shelf_item_id) WHERE returned_on IS NULL   -- jedno aktywne pożyczenie
```

Plus funkcja/trigger `update_updated_at_column()` jak w poker-stats oraz `V2__insert_admin_user.sql`.

---

## 6. API REST (prefix `/api`)

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| POST | `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | jak w poker-stats |
| GET/PUT | `/users/me`, `/users/me/password` | profil |
| GET | `/books?search=` | wyszukiwanie w katalogu (autocomplete przy dodawaniu) |
| POST | `/books` | find-or-create (tytuł, autor, opcjonalnie ISBN) |
| GET/PUT | `/books/{id}` | szczegóły / edycja (twórca lub admin) |
| GET | `/books/lookup?isbn=` | faza 6: metadane z katalogu zewnętrznego |
| GET | `/reading-entries?year=&rating=&search=&page=` | moje przeczytane |
| POST | `/reading-entries` | `{ bookId }` lub `{ title, authors, isbn? }` + data/ocena |
| GET/PUT/DELETE | `/reading-entries/{id}` | |
| GET | `/reading-entries/summary?year=` | statystyki do dashboardu |
| GET | `/libraries` | moje biblioteczki (MVP: jedna) |
| GET | `/libraries/{id}/items?status=&search=&page=` | półka |
| POST | `/libraries/{id}/items` | dodaj egzemplarz |
| GET/PUT/DELETE | `/shelf-items/{id}` | |
| POST | `/shelf-items/{id}/loans` | pożycz |
| POST | `/shelf-items/{id}/loans/{loanId}/return` | zwrot |
| GET | `/libraries/{id}/loans?active=true` | lista pożyczonych |
| GET/PUT | `/admin/users`, `/admin/users/{id}` | admin |

Wszystkie listy paginowane przez `PagedResponse` (jak w poker-stats). Błędy przez `GlobalExceptionHandler` z mapowaniem wyjątków domenowych na 400/403/404/409.

---

## 7. Frontend – ekrany (mobile-first)

| Ścieżka | Ekran |
|---------|-------|
| `/login`, `/register` | kopiowane z poker-stats |
| `/` | Dashboard: "przeczytane w tym roku", "pożyczone (N)", ostatnio dodane, skróty "+ przeczytana", "+ na półkę" |
| `/read` | lista przeczytanych (karty: okładka/placeholder, tytuł, autor, data, gwiazdki); filtry rok/ocena; wyszukiwarka |
| `/read/new`, `/read/:id/edit` | formularz: pole tytuł + autor z autouzupełnianiem z `/books?search=`, data (przełącznik rok/miesiąc/dzień), ocena gwiazdkami, notatka |
| `/shelf` | półka; zakładki: Wszystkie / Na półce / Pożyczone |
| `/shelf/new` | dodaj egzemplarz |
| `/shelf/:id` | karta egzemplarza: status, przycisk "Pożycz" (modal: imię, nazwisko, data, termin), "Zwrócono", historia pożyczeń, "Oznacz jako przeczytaną" |
| `/loans` | lista aktualnie pożyczonych, sort po dacie, podświetlenie przeterminowanych |
| `/books/:id` | karta książki w katalogu: moje wpisy przeczytania + egzemplarze na półce |
| `/profile`, `/admin/users` | jak w poker-stats |

Komponenty współdzielone z poker-stats (kopiowane 1:1): `AppLayout`, `Navigation`, `ProtectedRoute`, `ErrorBoundary`, `LoadingSpinner`, `LoadingSkeleton`, `EmptyState`, `ErrorMessage`, `ConfirmationModal`, `PageHeader`, `useToast`, `AuthContext`, `api/client.ts`.

Nowe komponenty: `BookPicker` (autocomplete + "dodaj nową"), `RatingStars`, `FuzzyDateInput`, `LoanModal`, `BookCard`, `ShelfItemStatusBadge`.

---

## 8. Rozszerzenia planowane (poza MVP)

1. **ISBN + katalog publiczny** – pole ISBN w formularzach, `GET /books/lookup?isbn=`, `OpenLibraryBookMetadataProvider` (bez klucza API; endpoint `https://openlibrary.org/isbn/{isbn}.json` + okładki `covers.openlibrary.org`). Google Books jako drugi provider (fallback), konfigurowany przez `app.catalog.providers`.
2. **Skaner kodów kreskowych** – w PWA, przez kamerę: `BarcodeDetector` API tam gdzie dostępne (Chrome/Android), fallback `@zxing/browser`. EAN-13 zaczynający się od 978/979 = ISBN-13. Wymaga HTTPS (kamera) – patrz plan wdrożenia.
3. **Biblioteczka rodzinna** – zaproszenie członka (`POST /libraries/{id}/members`), przełącznik biblioteczki w UI, role OWNER/MEMBER. Przeczytane pozostają osobiste.
4. **Statusy czytania** – `IN_PROGRESS`, `WANT_TO_READ`, `ABANDONED`.
5. **Import/eksport CSV** (np. z Goodreads/Lubimyczytać).
6. **Powiadomienia ntfy** o przeterminowanych pożyczeniach (port `NotificationService` jak w poker-stats).
