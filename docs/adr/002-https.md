# ADR 002: HTTPS na Raspberry Pi

**Status:** Zaakceptowany
**Data:** 2026-10-22

## Kontekst

Aplikacja działa jako PWA na telefonach. Instalacja PWA, bezpieczne przechowywanie tokenów i dostęp do kamery
(skaner kodów ISBN w fazie 7) wymagają HTTPS. Aplikacja jest publikowana przez nginx wbudowany w kontener frontendu
(serwuje SPA i proxy'uje `/api` do backendu), na porcie `FRONTEND_PORT` (domyślnie 8020), bez TLS.

## Decyzja

Terminacja TLS **przed** kontenerem frontendu, bez zmian w aplikacji:

- **Wariant A (domyślny): Cloudflare Tunnel** (`cloudflared` jako dodatkowy kontener). Brak otwartych portów na routerze,
  darmowy certyfikat, działa z dowolnym łączem. Kontener `cloudflared` wskazuje na `http://frontend:80`.
- **Wariant B: Caddy** z automatycznym Let's Encrypt, gdy domena wskazuje bezpośrednio na publiczne IP Pi
  (wymaga przekierowania portów 80/443). Caddy proxy'uje do `frontend:80`.

W obu wariantach nginx w kontenerze frontendu pozostaje jedynym miejscem z routingiem `/api` → backend i `/` → SPA.
`CORS_ALLOWED_ORIGINS` ustawiamy na `https://<domena>`.

## Konsekwencje

- Plus: brak zmian w kodzie i w `frontend/nginx.conf`; przełączenie wariantu to dodanie jednego serwisu w compose.
- Minus: Wariant A uzależnia dostępność od Cloudflare; wariant B wymaga publicznego IP i otwartych portów.
- Do zrobienia przy wdrożeniu: dodać wybrany serwis do `docker-compose.prod.yml` (nie jest częścią MVP, bo zależy od domeny).
