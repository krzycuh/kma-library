# Faza 7 – Skaner kodów kreskowych – podsumowanie

Data: 2026-10-25.

## Co powstało (frontend)
- `components/BarcodeScanner.tsx` – pełnoekranowy podgląd z tylnego aparatu (`getUserMedia`, `facingMode: environment`, 1280×720, ciągły autofocus, gdy aparat go obsługuje) z ramką na kod. Odczyt co 150 ms; po trafieniu krótka wibracja (Android), aparat od razu się wyłącza. Kamera jest zatrzymywana także przy zamknięciu i odmontowaniu.
  - Latarka, gdy aparat ją udostępnia (zwykle Android).
  - „Zrób zdjęcie kodu” – `<input type="file" capture="environment">`: aparat systemowy, kod odczytywany ze zdjęcia. Działa też bez dostępu do kamery na żywo.
  - Komunikaty: brak HTTPS, brak zgody na aparat, brak aparatu, aparat zajęty, inny błąd (z „Spróbuj ponownie”); kod, który nie jest ISBN → podpowiedź, skanowanie trwa dalej.
- `lib/barcodeDetector.ts` – natywny `BarcodeDetector` (Chrome na Androidzie), a gdzie go brak (iOS Safari, Firefox, desktop) – ponyfill `barcode-detector` (ZXing w WebAssembly). Ponyfill i plik `.wasm` (~1,1 MB, ~460 kB gzip) ładują się dopiero przy pierwszym otwarciu skanera; `.wasm` serwujemy z `/assets` zamiast z CDN.
- `utils/barcode.ts` – `isbnFromBarcode`: EAN-13 z prefiksem 978/979 i poprawną sumą kontrolną = ISBN-13; odcina 2- lub 5-cyfrowy dodatek (cena); inne kody odrzuca. Testy w `utils/__tests__/barcode.test.ts`.
- Integracje:
  - `/isbn` – przycisk „📷 Skanuj”; `?scan=1` otwiera skaner od razu. Wynik → `?isbn=` → podgląd jak przy wpisaniu ręcznym.
  - Start – „📷 Skanuj ISBN” prowadzi do `/isbn?scan=1`.
  - `BookPicker` (formularze półki i przeczytanych) – przycisk 📷 przy polu ISBN; zeskanowany numer od razu wybiera/importuje książkę („Pobierz dane”).
- Zależności przypięte dokładnie: `barcode-detector@3.2.2`, `zxing-wasm@3.1.3`.

## nginx
- CSP: `script-src 'self' 'wasm-unsafe-eval'` – bez tego przeglądarka nie skompiluje dekodera WebAssembly (sprawdzone: z poprzednim CSP skaner się nie uruchamia). Nie zezwala na `eval()` JavaScriptu.
- `Permissions-Policy: camera=(self), microphone=(), geolocation=()`; `application/wasm` w `gzip_types`.

## Testy
- Vitest: `isbnFromBarcode` (+3 testy, razem 18).
- Playwright `e2e/isbn-flow.spec.ts` (ścieżka „ręczne ISBN”, z mockowanym API): błędny ISBN → poprawny → podgląd → „Dodaj na półkę” z wybraną książką; nieznany ISBN → link do ręcznego dodania.
- Ręcznie w Chromium z fałszywą kamerą (wygenerowany kod EAN-13 w nagraniu, CSP jak w produkcji): odczyt ISBN na żywo → podgląd; kod spożywczy → podpowiedź „To nie jest kod ISBN”; odmowa dostępu → komunikat + odczyt ze zdjęcia. To testuje ścieżkę ZXing/WASM (Chromium na Linuksie nie ma natywnego detektora).

## Do sprawdzenia ręcznie na telefonach
- iPhone (Safari i PWA z ekranu głównego): zgoda na aparat, ostrość z bliska, odczyt; iOS potrafi pytać o zgodę ponownie po zamknięciu aplikacji.
- Android (Chrome): natywny detektor, latarka, wibracja.
- Książki z dodatkowym kodem ceny obok ISBN i w słabym świetle.
