# Production StockPilot

Ce document regroupe les points minimums pour exploiter StockPilot en beta privee ou production controlee.

## Variables d'environnement

Copier `.env.prod.example` vers `.env.prod` sur le serveur, puis renseigner uniquement des valeurs reelles cote serveur.

Variables critiques:

- `DB_USERNAME`, `DB_PASSWORD`: compte PostgreSQL de production.
- `JWT_SECRET`: secret aleatoire d'au moins 32 caracteres, idealement 64+.
- `CORS_ALLOWED_ORIGINS`: domaine frontend exact, par exemple `https://app.stockpilot.example`.
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: credentials OAuth Google.
- `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_STARTER`, `STRIPE_PRICE_PRO`: valeurs Stripe du meme mode, test ou live.
- `AI_INTERNAL_API_KEY`: secret interne partage entre backend et service IA.
- `OPENAI_API_KEY`: uniquement si le copilote OpenAI est active.
- `APP_OWNER_ENABLED`: a activer seulement si un compte owner doit etre cree automatiquement.

Aucun secret reel ne doit etre commite dans Git.

## Demarrage production

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
```

Health checks:

- Frontend: `https://votre-domaine/`
- Backend via proxy: `https://votre-domaine/api/actuator/health`
- Backend interne Docker: `http://backend:8080/actuator/health`

Smoke test apres demarrage:

```powershell
.\scripts\smoke-prod.ps1 -BaseUrl "https://app.stockpilot.example"
```

Avec verification login:

```powershell
.\scripts\smoke-prod.ps1 -BaseUrl "https://app.stockpilot.example" -Email "admin@example.com" -Password "votre-mot-de-passe"
```

## HTTPS

Le conteneur frontend sert HTTP sur le port 80. En production, placer l'application derriere:

- un reverse proxy externe Nginx avec certificat TLS;
- Caddy ou Traefik avec Let's Encrypt;
- une plateforme cloud qui termine HTTPS.

Activer `Strict-Transport-Security` seulement quand HTTPS est valide sur le domaine final.

## CORS

En production, `CORS_ALLOWED_ORIGINS` doit contenir uniquement les domaines frontend autorises. Ne jamais utiliser `*`.

Exemple:

```env
CORS_ALLOWED_ORIGINS=https://app.stockpilot.example
```

## Stripe

Toutes les valeurs Stripe doivent venir du meme environnement:

- mode test: cle secrete Stripe test, prix `price_...` crees en mode test, webhook test;
- mode live: cle secrete Stripe live, prix live, webhook live.

Endpoint webhook backend:

```text
https://app.stockpilot.example/api/billing/stripe/webhook
```

Evenements a declarer dans Stripe:

- `checkout.session.completed`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_failed`

Test local avec tunnel:

```powershell
ngrok http 80
```

Puis declarer dans Stripe l'URL:

```text
https://votre-tunnel.ngrok-free.app/api/billing/stripe/webhook
```

Apres paiement, le statut passe de `CHECKOUT_PENDING` au statut recu par webhook. Sans webhook accessible publiquement, le paiement Stripe peut reussir mais l'application ne saura pas mettre a jour l'abonnement.

## Service IA

En production, le service IA n'expose aucun port public dans `docker-compose.prod.yml`. Il est appele uniquement par le backend via le reseau Docker.

`AI_INTERNAL_API_KEY` est obligatoire dans `docker-compose.prod.yml`. Les endpoints internes IA refusent les appels sans header interne valide. Le backend ajoute ce header automatiquement.

## Sauvegarde PostgreSQL

Sauvegarde:

```powershell
.\scripts\backup-postgres.ps1
```

Restauration:

```powershell
.\scripts\restore-postgres.ps1 -InputFile .\.backups\stockpilot_YYYYMMDD_HHMMSS.sql
```

Conserver les sauvegardes hors serveur applicatif et tester une restauration avant toute mise en production.

## Rollback

1. Sauvegarder la base avant de deployer.
2. Conserver l'image Docker precedente ou le tag Git precedent.
3. En cas de probleme:

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml down
git checkout <tag-precedent>
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

4. Restaurer la base uniquement si une migration ou une operation de donnees l'exige.

## Logs

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f frontend
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f ai-service
```

Les logs ne doivent pas contenir de mots de passe, JWT, cles Stripe, cles Google ou cles OpenAI.

## Checklist avant beta privee

- Backend tests OK.
- Frontend build prod OK.
- Docker compose prod config OK.
- Docker compose prod build OK.
- Smoke test prod OK.
- Migrations PostgreSQL OK.
- Webhook Stripe teste.
- OAuth Google teste.
- `CORS_ALLOWED_ORIGINS` strict.
- Sauvegarde PostgreSQL testee.
- IA non exposee publiquement.
- Compte OWNER protege et audite.
