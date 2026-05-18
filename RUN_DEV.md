# Lancer StockPilot AI en local

Depuis un seul terminal PowerShell :

```powershell
cd "C:\Users\simoD\OneDrive\Bureau\Gestion Stock"
.\start-stockpilot.ps1
```

Le script demarre :

- Frontend Angular : http://127.0.0.1:4200
- Backend Spring Boot : http://127.0.0.1:8081
- Service IA FastAPI : http://127.0.0.1:8000/health

Pour arreter tous les services lances par le script :

```powershell
Ctrl+C
```

Options utiles :

```powershell
.\start-stockpilot.ps1 -SkipInstall
.\start-stockpilot.ps1 -NoAi
.\start-stockpilot.ps1 -NoFrontend
.\start-stockpilot.ps1 -NoBackend
.\start-stockpilot.ps1 -BackendPort 8081 -FrontendPort 4200 -AiPort 8000
```

Cache Redis pour tester une configuration plus proche production :

```powershell
docker compose up -d redis
$env:CACHE_TYPE="redis"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
.\start-stockpilot.ps1 -SkipInstall
```

Smoke test du script :

```powershell
.\start-stockpilot.ps1 -SkipInstall -SmokeTestSeconds 5
```
