param(
    [Parameter(Mandatory = $true)]
    [string]$InputFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $InputFile)) {
    throw "Fichier de sauvegarde introuvable: $InputFile"
}

Get-Content -Raw $InputFile |
    docker compose --env-file .env.prod -f docker-compose.prod.yml exec -T postgres sh -c 'psql -U "$POSTGRES_USER" "$POSTGRES_DB"'

Write-Host "Restauration PostgreSQL terminee depuis: $InputFile"
