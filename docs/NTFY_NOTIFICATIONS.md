# Powiadomienia z GitHub Actions przez ntfy

Workflowy wysyłają powiadomienie na Twój serwer ntfy:

| Workflow | Kiedy | Treść |
|---|---|---|
| `deploy-production.yml` | po każdym buildzie obrazów (sukces i błąd) | „obrazy gotowe” / „build nie powiódł się”, wersja i commit |
| `backend-ci.yml`, `frontend-ci.yml` | tylko gdy coś padnie | które CI, PR albo gałąź + commit |

Kliknięcie powiadomienia otwiera przebieg w GitHub Actions. Logika jest w `.github/actions/ntfy/action.yml`. Jeśli ntfy nie odpowie, workflow i tak kończy się poprawnie (w logu zostaje ostrzeżenie). Bez sekretów krok jest po cichu pomijany.

## Czy sekrety w publicznym repo są bezpieczne?

Tak. Sekrety GitHub Actions **nie są częścią repozytorium** i nie widać ich publicznie:
- są szyfrowane; po zapisaniu nie da się ich odczytać nawet w ustawieniach repo, można je tylko nadpisać lub usunąć;
- w logach przebiegów GitHub zamienia ich wartości na `***`;
- workflowy uruchomione przez **pull request z forka nie dostają sekretów**, więc obca osoba nie wyciągnie ich, wystawiając PR (krok ntfy jest wtedy pomijany);
- w tym repo nie ma workflowów `pull_request_target` (to jedyny typowy sposób, w jaki PR z forka dostaje sekrety).

Pozostałe ryzyka: każdy z prawem zapisu do repo może napisać workflow, który użyje sekretu, a własny kod workflowu mógłby go wypisać w zmienionej postaci, której maskowanie nie rozpozna. Dlatego token dla GitHuba dostaje **wyłącznie prawo zapisu do jednego tematu**. Gdyby wyciekł, ktoś może najwyżej wysłać Ci powiadomienie; nie przeczyta niczego i nie dotknie innych tematów.

## 1. Przygotuj ntfy na Pi

Zakładam, że ntfy działa w kontenerze `ntfy` i ma włączoną autoryzację (`auth-file` w `server.yml`). Bez niej każdy, kto zna adres i temat, może do niego pisać i go czytać. Zalecane: `auth-default-access: deny-all`.

```bash
# Osobny użytkownik tylko dla GitHub Actions (zapyta o hasło – i tak będzie używany token)
docker exec -it ntfy ntfy user add github-actions

# Tylko zapis, tylko do jednego tematu
docker exec -it ntfy ntfy access github-actions kma-builds write-only

# Token do sekretu NTFY_TOKEN (zaczyna się od tk_)
docker exec -it ntfy ntfy token add github-actions

# Twoje konto musi móc czytać ten temat, żeby subskrybować go w aplikacji
docker exec -it ntfy ntfy access <twoj-uzytkownik> kma-builds read-only
```

Nazwę tematu (`kma-builds`) możesz zmienić; wtedy użyj tej samej w sekrecie `NTFY_TOPIC`.

### Cloudflare
Runner GitHuba łączy się z Twoim ntfy z internetu, przez Cloudflare Tunnel. Jeśli adres ntfy jest za **Cloudflare Access** (logowanie przed wejściem), runner zostanie zatrzymany. Dodaj wtedy dla tej domeny regułę *Bypass* dla ścieżki publikowania albo użyj service tokena. Podobnie *Bot Fight Mode* i reguły WAF potrafią blokować `curl`. Objaw: w logu kroku „Send ntfy notification” ostrzeżenie `HTTP 403` albo `HTTP 503`.

Sprawdzenie z dowolnego komputera:
```bash
curl -H "Authorization: Bearer tk_..." -d "test z curla" https://ntfy.twoja-domena.pl/kma-builds
```

## 2. Dodaj sekrety w GitHubie

GitHub → repozytorium **kma-library** → **Settings** → **Secrets and variables** → **Actions** → zakładka **Secrets** → **New repository secret**. Dodaj trzy sekrety:

| Nazwa | Wartość |
|---|---|
| `NTFY_URL` | adres serwera **bez** tematu, np. `https://ntfy.twoja-domena.pl` |
| `NTFY_TOPIC` | `kma-builds` |
| `NTFY_TOKEN` | token z `ntfy token add` (`tk_...`) |

Użyj zakładki *Secrets*, nie *Variables*: zmienne są widoczne w logach.

## 3. Subskrybuj temat w telefonie

Aplikacja ntfy → **+** → temat `kma-builds` → *Use another server* → `https://ntfy.twoja-domena.pl` → zaloguj się swoim kontem (z prawem odczytu).

## 4. Test

GitHub → **Actions** → **Deploy to Production** → **Run workflow** (gałąź `main`). Po kilku minutach powinno przyjść „KMA Library: obrazy gotowe”. Jeśli nie, otwórz job **Notify** → krok **Send ntfy notification**: jest tam „Notification sent”, „ntfy is not configured” (brak sekretów) albo ostrzeżenie z kodem HTTP.
