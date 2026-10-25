# Faza 8 – Biblioteczka rodzinna – podsumowanie

Data: 2026-10-25.

## Model
- Każdy użytkownik nadal ma własną biblioteczkę (tworzoną przy rejestracji, rola `OWNER`). Biblioteczka rodzinna to zwykła biblioteczka z dodatkowymi członkami (`MEMBER`) – schemat (`library_members`) był gotowy od fazy 1, migracje nie były potrzebne.
- Uprawnienia (`LibraryAccessPolicy` – jedyne miejsce decyzji):
  - każdy członek: przegląda półkę, dodaje, edytuje, pożycza, oznacza zwroty, widzi listę członków;
  - właściciel dodatkowo: dodaje i usuwa członków, zmienia nazwę biblioteczki;
  - członek może opuścić biblioteczkę; właściciela nie da się usunąć ani „wypisać” (biblioteczka zostałaby bez zarządzającego).
- Przeczytane pozostają prywatne – bez zmian.

## Backend
- REST:
  - `GET /api/libraries` – teraz z `myRole` i `memberCount`; własna biblioteczka pierwsza,
  - `PATCH /api/libraries/{id}` `{ name }` – zmiana nazwy (właściciel),
  - `GET /api/libraries/{id}/members` – członkowie (właściciel pierwszy),
  - `POST /api/libraries/{id}/members` `{ email }` – dodanie istniejącego, aktywnego konta (właściciel); 404 brak konta, 409 już członek,
  - `DELETE /api/libraries/{id}/members/{userId}` – usunięcie przez właściciela albo wyjście (własne `userId`); 403 dla właściciela i dla cudzych członków.
- Use case'y `ListLibraryMembers`, `InviteLibraryMember`, `RemoveLibraryMember`, `RenameLibrary`; `ListMyLibraries.summaries`.
- Dashboard liczy „na półce” i „pożyczone” ze wszystkich biblioteczek użytkownika (własnej i rodzinnych).
- Testy: `LibraryMemberUseCasesTest` (+7, razem 137).

## Frontend
- Bieżąca biblioteczka (`useCurrentLibrary`) – zapamiętana na urządzeniu, wspólna dla Półki, Pożyczonych, formularza „na półkę” i karty książki; gdy zapamiętanej już nie ma (wyjście z biblioteczki, inne konto) – własna. Logika wyboru w `utils/currentLibrary.ts` z testami.
- `LibrarySwitcher` – wybór biblioteczki na `/shelf`, `/loans`, `/shelf/new` i `/shelf/members`; ukryty, gdy użytkownik ma jedną.
- `/shelf/members` („👥 Domownicy” na Półce): lista członków z rolami; właściciel – zmiana nazwy, dodawanie po e-mailu (z komunikatami „brak konta” / „już członek”), usuwanie z potwierdzeniem; członek – „Opuść”.
- Vitest: 21 testów.

## Uwagi
- Dodanie domownika nie wysyła powiadomienia – domownik po prostu widzi nową biblioteczkę w przełączniku przy następnym wejściu na Półkę.
- Formularz dodawania zdradza, czy konto z danym e-mailem istnieje (tak jak rejestracja); w aplikacji rodzinnej to akceptowalne.
