# 🔧 Wdrożenie produkcyjne (Raspberry Pi)

Obrazy budowane są przez GitHub Actions (`.github/workflows/deploy-production.yml`) po opublikowaniu release'a
(tag `vX.Y.Z`) lub ręcznie (`workflow_dispatch`). Obrazy trafiają do GHCR:

- `ghcr.io/krzycuh/kma-library/backend:<tag>`
- `ghcr.io/krzycuh/kma-library/frontend:<tag>`

## Pierwsze uruchomienie na Pi

1. Skopiuj do **osobnego katalogu** na Pi (np. `/opt/kma-library`): `docker-compose.prod.yml`, `scripts/`, `.env.production.template`.
   Nie trzymaj tu plików compose innych aplikacji: współdzieliłyby `.env` i względne katalogi (`logs/`, `data/`).
   Projekt ma stałą nazwę `name: kma-library`, więc nie łączy się z innymi projektami compose.
2. `cp .env.production.template .env.production` i uzupełnij hasła (`openssl rand -base64 32`) oraz `DATA_PATH` (SSD).
3. Utwórz katalogi danych: `mkdir -p $DATA_PATH/postgres $BACKUP_PATH logs/backend` (wartości z `.env.production`).
4. Zaloguj się do GHCR: `echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin`.
5. `docker compose -f docker-compose.prod.yml --env-file .env.production up -d`.
   Aplikacja jest dostępna na `http://<pi>:8020` (port zmienisz przez `FRONTEND_PORT`). Backend nie publikuje portu:
   `/api` przechodzi przez nginx w kontenerze frontendu do `backend:8080` w sieci compose.
6. Zaloguj się jako `admin@example.com` / `admin123` i **od razu zmień hasło** (Profil → Zmiana hasła).
7. HTTPS: patrz [docs/adr/002-https.md](docs/adr/002-https.md) (wymagane pod skaner kodów w fazie 7). Terminator TLS kieruje ruch na port 8020.

## Aktualizacja do nowej wersji

```bash
# w .env.production ustaw VERSION=vX.Y.Z (lub main)
docker compose -f docker-compose.prod.yml --env-file .env.production pull
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
docker compose -f docker-compose.prod.yml logs -f backend   # Flyway zastosuje migracje przy starcie
```

## Ręczne zbudowanie obrazów (bez CI)

```bash
docker build -t ghcr.io/$GITHUB_USER/kma-library/backend:main ./backend
docker build -t ghcr.io/$GITHUB_USER/kma-library/frontend:main ./frontend
docker push ghcr.io/$GITHUB_USER/kma-library/backend:main
docker push ghcr.io/$GITHUB_USER/kma-library/frontend:main
```

## Baza danych

Postgres nie publikuje portu na hoście; backend łączy się z nim przez sieć compose (`postgres:5432`).
Konsola SQL: `docker exec -it kmalibrary-postgres-prod psql -U kmalibrary`.

## Backup

`scripts/backup-database.sh` robi `pg_dump` (skompresowany gzipem) do `$BACKUP_PATH/postgres` i usuwa kopie
starsze niż `BACKUP_RETENTION_DAYS`. Skrypty same przechodzą do katalogu projektu i czytają `.env.production`,
więc działają z crona:

```bash
# crontab -e: codziennie o 3:00
0 3 * * * /opt/kma-library/scripts/backup-database.sh >> /opt/kma-library/logs/backup.log 2>&1
```

Odtwarzanie: `scripts/restore-database.sh` (interaktywnie wybiera kopię, przed odtworzeniem robi kopię bieżącej bazy
`pre_restore_*.sql.gz` i na czas odtwarzania zatrzymuje backend).

Kopie zrobione przed poprawką skryptu (podwójnie skompresowane) odtworzysz tak:
`gunzip -c plik.sql.gz | gunzip | docker exec -i kmalibrary-postgres-prod psql -U kmalibrary -d postgres`.

## Przejście ze starej konfiguracji (bez `name:`)

Kontenery uruchomione przed dodaniem `name: kma-library` należą do projektu nazwanego od katalogu, więc nowe
`docker compose ... down` ich nie widzi. Zatrzymaj je raz po nazwie: `docker rm -f kmalibrary-frontend-prod
kmalibrary-backend-prod kmalibrary-postgres-prod`, potem `up -d` jak wyżej. Dane bazy zostają w `$DATA_PATH/postgres`.
