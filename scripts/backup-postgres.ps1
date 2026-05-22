param(
    [string]$OutputDir = ".backups"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputFile = Join-Path $OutputDir "stockpilot_$timestamp.sql"

docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' |
    Set-Content -Encoding utf8 $outputFile

Write-Host "Backup PostgreSQL cree: $outputFile"
