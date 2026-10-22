# 🔧 Wdrożenie produkcyjne (Raspberry Pi)

Obrazy budowane są przez GitHub Actions (`.github/workflows/deploy-production.yml`) po opublikowaniu release'a
(tag `vX.Y.Z`) lub ręcznie (`workflow_dispatch`). Obrazy trafiają do GHCR:

- `ghcr.io/krzycuh/kma-library/backend:<tag>`
- `ghcr.io/krzycuh/kma-library/frontend:<tag>`

## Pierwsze uruchomienie na Pi

1. Skopiuj do katalogu na Pi: `docker-compose.prod.yml`, `nginx/`, `scripts/`, `.env.production.template`.
2. `cp .env.production.template .env.production` i uzupełnij hasła (`openssl rand -base64 32`) oraz `DATA_PATH` (SSD).
3. Utwórz katalogi danych: `mkdir -p $DATA_PATH/postgres $DATA_PATH/backups logs/backend logs/nginx`.
4. Zaloguj się do GHCR: `echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin`.
5. `docker compose -f docker-compose.prod.yml --env-file .env.production up -d`.
6. Zaloguj się jako `admin@example.com` / `admin123` i **od razu zmień hasło** (Profil → Zmiana hasła).
7. HTTPS: patrz [docs/adr/002-https.md](docs/adr/002-https.md) (wymagane pod skaner kodów w fazie 7).

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

## Backup

`scripts/backup-database.sh` (cron, np. codziennie o 3:00) oraz `scripts/restore-database.sh <plik>`.
