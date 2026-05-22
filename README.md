# StockPilot AI / Gestion Stock

Application SaaS de gestion de stock multi-tenant pour TPE/PME.

## Stack

- Backend: Java 21, Spring Boot 3.4, Maven, Flyway, JWT, OAuth2 Google, H2 dev, PostgreSQL prod.
- Frontend: Angular, TypeScript, Signals/RxJS.
- Service IA: Python FastAPI.
- Infra locale: Docker Compose pour PostgreSQL/Redis, script PowerShell unique.

## Lancer en local

```powershell
cd "C:\Users\simoD\OneDrive\Bureau\Gestion Stock"
.\start-stockpilot.ps1
```

URLs:

- Frontend: http://127.0.0.1:4200
- Backend: http://127.0.0.1:8081
- IA: http://127.0.0.1:8000/health

Smoke test:

```powershell
.\start-stockpilot.ps1 -SkipInstall -SmokeTestSeconds 5
```

## Comptes demo dev

OWNER:

- Email: `owner@stockpilot.local`
- Mot de passe: `Owner@2026!`

Mot de passe commun ADMIN/USER demo:

- `Password123!`

Organisations:

- `admin@demo-stock.local` / `user@demo-stock.local`
- `admin@garage-atlas.local` / `mecano@garage-atlas.local`
- `admin@pharma-nova.local` / `preparateur@pharma-nova.local`
- `admin@boutique-lumiere.local` / `vendeur@boutique-lumiere.local`
- `admin@atelier-meca.local` / `atelier@atelier-meca.local`

## Tests

Backend:

```powershell
cd stock-backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd stock-frontend
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
npm run build:prod
```

Service IA:

```powershell
cd ai-service
.\.venv\Scripts\python.exe -m pytest
```

## Configuration

Copier `.env.example` vers `.env` pour une configuration locale. Ne jamais versionner `.env`, secrets OAuth, logs, bases locales, `.venv`, `node_modules`, `target` ou `dist`.

Pour PostgreSQL:

```powershell
docker compose up -d postgres redis
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/gestion_stock"
$env:DB_USERNAME="gestion_stock"
$env:DB_PASSWORD="gestion_stock"
$env:JWT_SECRET="replace_with_32_chars_minimum_secret"
.\start-stockpilot.ps1 -SkipInstall
```

## Notes production

- Utiliser `application-prod.yaml` avec variables d'environnement obligatoires.
- Remplacer tous les secrets dev.
- Desactiver H2 console.
- Restreindre CORS.
- Utiliser HTTPS derriere le reverse proxy.
- Garder les exports/logs hors repository.

## Paiement et abonnement

Le module abonnement est disponible pour les administrateurs dans le menu `Abonnement`.

En local, si Stripe n'est pas configure, l'ecran affiche clairement que le paiement en ligne est desactive et aucune souscription fictive n'est creee.

Variables a configurer pour activer Stripe:

```powershell
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret
STRIPE_PRICE_STARTER=price_...
STRIPE_PRICE_PRO=price_...
BILLING_SUCCESS_URL=https://app.example.com/billing?payment=success
BILLING_CANCEL_URL=https://app.example.com/billing?payment=cancel
```

Webhook Stripe attendu:

```text
POST /billing/stripe/webhook
```

## Lancement production Docker

Docker n'est pas necessaire pour le mode dev, mais le projet contient maintenant un compose production.

```powershell
copy .env.prod.example .env.prod
# Modifier .env.prod avec les vrais secrets, domaines, cles Google et Stripe.
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Services exposes:

- Frontend: `http://localhost`
- Backend interne: `backend:8080`
- IA interne: `ai-service:8000`
- PostgreSQL et Redis internes au compose

Checklist avant mise en ligne:

- renseigner un `JWT_SECRET` long et aleatoire;
- limiter `CORS_ALLOWED_ORIGINS` au vrai domaine;
- mettre les vrais identifiants Google OAuth;
- mettre les vrais identifiants Stripe + webhook;
- garder `APP_OWNER_ENABLED=false` en production sauf creation controlee;
- placer l'application derriere HTTPS;
- verifier les sauvegardes PostgreSQL;
- activer monitoring/logs hors repository.
