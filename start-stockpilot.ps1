param(
    [int]$BackendPort = 8081,
    [int]$FrontendPort = 4200,
    [int]$AiPort = 8000,
    [switch]$SkipInstall,
    [switch]$NoAi,
    [switch]$NoFrontend,
    [switch]$NoBackend,
    [int]$SmokeTestSeconds = 0
)

$ErrorActionPreference = "Stop"

$RootDir = $PSScriptRoot
$BackendDir = Join-Path $RootDir "stock-backend"
$FrontendDir = Join-Path $RootDir "stock-frontend"
$AiDir = Join-Path $RootDir "ai-service"
$LogDir = Join-Path $RootDir ".dev-logs"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Commande introuvable: $Name. Installe-la ou ajoute-la au PATH."
    }
}

function Test-Port {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(250)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-Port {
    param(
        [string]$Name,
        [int]$Port,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port -Port $Port) {
            Write-Host "$Name pret sur http://127.0.0.1:$Port" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    }
    Write-Warning "$Name ne repond pas encore sur le port $Port. Regarde les logs dans $LogDir."
}

function Start-ServiceJob {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [scriptblock]$Script,
        [object[]]$ArgumentList
    )

    $outLog = Join-Path $LogDir "$Name.out.log"
    $errLog = Join-Path $LogDir "$Name.err.log"
    Remove-Item -LiteralPath $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $job = Start-Job -Name $Name -ScriptBlock {
        param($WorkingDirectory, $OutLog, $ErrLog, $ScriptText, $ArgsForScript)
        Set-Location $WorkingDirectory
        $scriptBlock = [scriptblock]::Create($ScriptText)
        & $scriptBlock @ArgsForScript > $OutLog 2> $ErrLog
    } -ArgumentList $WorkingDirectory, $outLog, $errLog, $Script.ToString(), $ArgumentList

    [PSCustomObject]@{
        Name = $Name
        Job = $job
        OutLog = $outLog
        ErrLog = $errLog
    }
}

function Show-LogTail {
    param([array]$Services)
    foreach ($service in $Services) {
        if (Test-Path $service.OutLog) {
            Get-Content -Path $service.OutLog -Tail 3 -ErrorAction SilentlyContinue |
                ForEach-Object { Write-Host "[$($service.Name)] $_" -ForegroundColor DarkGray }
        }
        if (Test-Path $service.ErrLog) {
            Get-Content -Path $service.ErrLog -Tail 3 -ErrorAction SilentlyContinue |
                ForEach-Object { Write-Host "[$($service.Name) err] $_" -ForegroundColor DarkYellow }
        }
    }
}

Assert-Command "python"
Assert-Command "npm"

if (-not $NoBackend) {
    $mvnw = Join-Path $BackendDir "mvnw.cmd"
    if (-not (Test-Path $mvnw)) {
        throw "Maven wrapper introuvable: $mvnw"
    }
}

if (-not $SkipInstall) {
    if (-not $NoAi) {
        Write-Section "Preparation du service IA"
        $venvPython = Join-Path $AiDir ".venv\Scripts\python.exe"
        if (-not (Test-Path $venvPython)) {
            python -m venv (Join-Path $AiDir ".venv")
        }
        & $venvPython -m pip install -q -r (Join-Path $AiDir "requirements.txt")
    }

    if (-not $NoFrontend) {
        $nodeModules = Join-Path $FrontendDir "node_modules"
        if (-not (Test-Path $nodeModules)) {
            Write-Section "Installation des dependances frontend"
            Push-Location $FrontendDir
            npm install
            Pop-Location
        }
    }
}

$services = @()

try {
    Write-Section "Demarrage StockPilot AI"

    if (-not $NoAi) {
        if (Test-Port -Port $AiPort) {
            Write-Warning "Le port IA $AiPort est deja utilise. Le script continuera avec le service existant."
        } else {
            $venvPython = Join-Path $AiDir ".venv\Scripts\python.exe"
            if (-not (Test-Path $venvPython)) {
                $venvPython = "python"
            }
            $services += Start-ServiceJob -Name "ai-service" -WorkingDirectory $AiDir -Script {
                param($PythonExe, $Port)
                & $PythonExe -m uvicorn app.main:app --host 127.0.0.1 --port $Port
            } -ArgumentList @($venvPython, $AiPort)
        }
    }

    if (-not $NoBackend) {
        if (Test-Port -Port $BackendPort) {
            Write-Warning "Le port backend $BackendPort est deja utilise. Le script continuera avec le backend existant."
        } else {
            $services += Start-ServiceJob -Name "backend" -WorkingDirectory $BackendDir -Script {
                param($Port, $AiPort, $AiEnabled)
                $env:SERVER_PORT = "$Port"
                $env:AI_SERVICE_ENABLED = "$AiEnabled"
                $env:AI_SERVICE_URL = "http://127.0.0.1:$AiPort"
                if (-not $env:CACHE_TYPE) {
                    $env:CACHE_TYPE = "simple"
                }
                $env:JWT_SECRET = "dev_secret_key_just_for_local_tests_32chars_min"
                & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
            } -ArgumentList @($BackendPort, $AiPort, (-not $NoAi).ToString().ToLowerInvariant())
        }
    }

    if (-not $NoFrontend) {
        if (Test-Port -Port $FrontendPort) {
            Write-Warning "Le port frontend $FrontendPort est deja utilise. Le script continuera avec le frontend existant."
        } else {
            $services += Start-ServiceJob -Name "frontend" -WorkingDirectory $FrontendDir -Script {
                param($Port)
                & npx ng serve "--host=127.0.0.1" "--port=$Port"
            } -ArgumentList @($FrontendPort)
        }
    }

    if (-not $NoAi) { Wait-Port -Name "AI service" -Port $AiPort -TimeoutSeconds 60 }
    if (-not $NoBackend) { Wait-Port -Name "Backend" -Port $BackendPort -TimeoutSeconds 120 }
    if (-not $NoFrontend) { Wait-Port -Name "Frontend" -Port $FrontendPort -TimeoutSeconds 120 }

    Write-Host ""
    Write-Host "StockPilot AI est lance." -ForegroundColor Green
    Write-Host "Frontend : http://127.0.0.1:$FrontendPort"
    Write-Host "Backend  : http://127.0.0.1:$BackendPort"
    Write-Host "IA       : http://127.0.0.1:$AiPort/health"
    Write-Host "Logs     : $LogDir"
    Write-Host ""
    Write-Host "Appuie sur Ctrl+C pour tout arreter." -ForegroundColor Yellow

    if ($SmokeTestSeconds -gt 0) {
        Write-Host "Mode smoke test: arret automatique dans $SmokeTestSeconds secondes." -ForegroundColor Yellow
        Start-Sleep -Seconds $SmokeTestSeconds
        return
    }

    while ($true) {
        $failed = $services | Where-Object { $_.Job.State -in @("Failed", "Stopped", "Completed") }
        if ($failed) {
            Show-LogTail -Services $failed
            throw "Un service s'est arrete: $($failed.Name -join ', ')"
        }
        Start-Sleep -Seconds 5
    }
} finally {
    Write-Section "Arret des services"
    foreach ($service in $services) {
        Stop-Job -Job $service.Job -ErrorAction SilentlyContinue
        Remove-Job -Job $service.Job -Force -ErrorAction SilentlyContinue
    }
    Write-Host "Termine." -ForegroundColor Green
}
