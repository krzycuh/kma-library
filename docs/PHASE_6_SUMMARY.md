# Faza 6 – ISBN i katalog publiczny – podsumowanie

Data: 2026-10-24.

## Co powstało

### Backend
- Port `application/service/BookMetadataProvider` (`lookupByIsbn(Isbn): BookMetadata?`) + `BookMetadataUnavailableException` (timeout, 5xx, limit zapytań → HTTP 503).
- Adaptery w `infrastructure/catalog/`:
  - `OpenLibraryBookMetadataProvider` – `GET /api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data` (jedno zapytanie zwraca tytuł, autorów, datę wydania i okładkę),
  - `GoogleBooksBookMetadataProvider` – `GET /books/v1/volumes?q=isbn:{isbn}`, opcjonalny klucz API,
  - `NoOpBookMetadataProvider` – dla `none`,
  - `FallbackBookMetadataProvider` – odpytuje katalogi po kolei: następny, gdy poprzedni nie zna ISBN albo nie odpowiada; „nie znaleziono” tylko gdy wszystkie odpowiedziały „nie znam”, a gdy nic nie znaleziono i któryś był niedostępny – 503 (bez zapisu w cache, żeby kolejna próba znów pytała),
  - `CachingBookMetadataProvider` – dekorator na `CacheManager` (cache `book-metadata`, klucz = ISBN-13; zapamiętuje też „nie znaleziono”, nie zapamiętuje awarii).
- `config/CatalogConfig` składa łańcuch z listy `app.catalog.providers` (np. `openlibrary,google` – domyślnie; `google,openlibrary`; `openlibrary`; `none` – w testach); Spring `RestClient` z timeoutem połączenia i odczytu `app.catalog.timeout` (3 s na każde źródło). Nieznana nazwa = błąd startu.
- Use case'y:
  - `LookupBookByIsbn` – najpierw lokalny katalog, potem zewnętrzny; niczego nie zapisuje,
  - `ImportBookByIsbn` – zwraca istniejącą książkę albo tworzy wpis z metadanych (`source`, `externalId`, `coverUrl`); deduplikacja jak w `FindOrCreateBook` (ISBN, potem tytuł+autor – wtedy istniejący wpis jest uzupełniany),
  - `MergeBooks` (admin) – przepina `reading_entries` i `shelf_items` na książkę docelową, usuwa duplikat, a docelowa przejmuje brakujące dane (ISBN, rok, okładka – `Book.absorb`).
- `FindOrCreateBook` dostał wariant `findOrCreate(BookDraft)` – autorzy z katalogu zewnętrznego nie są ponownie dzieleni po przecinku („Tolkien, J. R. R.”).
- REST:
  - `GET /api/books/lookup?isbn=` → `{ isbn, book | null, metadata | null }`; 400 zły ISBN, 404 nikt nie zna, 503 katalog niedostępny,
  - `POST /api/books/import` `{ isbn }` → `BookDto`,
  - `POST /api/admin/books/{id}/merge-into/{targetId}` → `{ target, movedReadingEntries, movedShelfItems }`.
- Okładki: tylko URL (linki `http://` z API zamieniane na `https://`), bez uploadów. CSP nginx już dopuszcza `img-src https:`.
- Testy (+35, razem 120): `BookTest` (fillGapsFrom/absorb), `IsbnLookupTest`, `MergeBooksTest`, `CachingBookMetadataProviderTest`, `PublishedYearParserTest`, `CatalogConfigTest`, `FallbackBookMetadataProviderTest`, adaptery na MockWebServer (`OpenLibrary…Test`, `GoogleBooks…Test`: mapowanie, brak wyniku, 5xx/429, timeout) z odpowiedziami w `src/test/resources/catalog/`, reguła ArchUnit dla adapterów.

### Frontend
- `utils/isbn.ts`: `toIsbn13` (normalizacja ISBN-10 → 13 jak `Isbn.parse`) + testy; `utils/isbnLookup.ts` – komunikaty dla 404/503/400.
- Nowa strona `/isbn` („Dodaj po ISBN”, link na Starcie): wpisuję ISBN → podgląd (okładka, tytuł, autorzy, rok, źródło) → „Dodaj na półkę” / „Dodaj jako przeczytaną” → formularz z wybraną książką (`?bookId=`). Obsługuje `?isbn=` w adresie – pod skaner z fazy 7.
- `BookPicker`: przycisk „Pobierz dane” przy polu ISBN (też Enter) – wybiera książkę z katalogu lub importuje ją z zewnętrznego.
- Karta książki (admin): sekcja „Scal duplikat” – wyszukanie książki docelowej, potwierdzenie, przejście na kartę książki, która została.

### Konfiguracja
- `APP_CATALOG_PROVIDERS` (domyślnie `openlibrary,google`) i `APP_CATALOG_GOOGLE_API_KEY` w `docker-compose.prod.yml` i `.env.production.template`.
- Open Library nie wymaga klucza ani konfiguracji (aplikacja wysyła własny `User-Agent`, o co prosi Open Library); serwer musi mieć wyjście do `openlibrary.org`.
- Klucz Google Books (darmowy, bez podpinania płatności): Google Cloud Console → nowy projekt → „APIs & Services → Library” → włączyć **Books API** → „Credentials → Create credentials → API key” → w „API restrictions” ograniczyć klucz do Books API. Bez klucza Google też działa, ale ze wspólnym limitem na adres IP (łatwo o 429).

## Uwagi
- Fixture'y w `src/test/resources/catalog/` odwzorowują udokumentowany format odpowiedzi Open Library (`jscmd=data`) i Google Books; nie zostały pobrane na żywo (środowisko, w którym powstawała faza, nie miało dostępu do tych serwisów). Warto podmienić je na prawdziwe odpowiedzi przy pierwszej okazji.
- Scalanie jest nieodwracalne i dotyczy wpisów wszystkich użytkowników (katalog jest wspólny) – dlatego tylko admin i z potwierdzeniem.
- Zapytanie SQL scalania (`UPDATE … SET book_id`) nie ma testu integracyjnego na bazie (w projekcie nie ma jeszcze testów z Testcontainers) – sprawdzone tylko testami jednostkowymi use case'u.
