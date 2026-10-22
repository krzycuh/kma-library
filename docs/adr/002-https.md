# ADR 002: HTTPS na Raspberry Pi

**Status:** Zaakceptowany
**Data:** 2026-10-22

## Kontekst

Aplikacja działa jako PWA na telefonach. Instalacja PWA, bezpieczne przechowywanie tokenów i dostęp do kamery
(skaner kodów ISBN w fazie 7) wymagają HTTPS. Obecny `nginx/nginx.prod.conf` słucha tylko na porcie 80.

## Decyzja

Terminacja TLS **przed** istniejącym nginx, bez zmian w aplikacji:

- **Wariant A (domyślny): Cloudflare Tunnel** (`cloudflared` jako dodatkowy kontener). Brak otwartych portów na routerze,
  darmowy certyfikat, działa z dowolnym łączem. Kontener `cloudflared` wskazuje na `http://nginx:80`.
- **Wariant B: Caddy** z automatycznym Let's Encrypt, gdy domena wskazuje bezpośrednio na publiczne IP Pi
  (wymaga przekierowania portów 80/443). Caddy proxy'uje do `nginx:80`.

W obu wariantach nginx pozostaje jedynym miejscem z routingiem `/api` → backend i `/` → frontend.
`CORS_ALLOWED_ORIGINS` ustawiamy na `https://<domena>`.

## Konsekwencje

- Plus: brak zmian w kodzie i w `nginx.prod.conf`; przełączenie wariantu to zmiana jednego serwisu w compose.
- Minus: Wariant A uzależnia dostępność od Cloudflare; wariant B wymaga publicznego IP i otwartych portów.
- Do zrobienia przy wdrożeniu: dodać wybrany serwis do `docker-compose.prod.yml` (nie jest częścią MVP, bo zależy od domeny).
