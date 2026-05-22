param(
    [string]$BaseUrl = "http://127.0.0.1",
    [string]$Email = "",
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"

function Invoke-SmokeRequest {
    param(
        [string]$Name,
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200)
    )

    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            TimeoutSec = 15
            UseBasicParsing = $true
        }
        if ($Headers.Count -gt 0) {
            $params.Headers = $Headers
        }
        if ($null -ne $Body) {
            $params.ContentType = "application/json"
            $params.Body = ($Body | ConvertTo-Json -Depth 8)
        }
        $response = Invoke-WebRequest @params
        $statusCode = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        } else {
            throw "[$Name] request failed before HTTP response: $($_.Exception.Message)"
        }
    }

    if ($ExpectedStatus -notcontains $statusCode) {
        throw "[$Name] expected HTTP $($ExpectedStatus -join '/') but got $statusCode"
    }

    Write-Host "[OK] $Name ($statusCode)"
}

$base = $BaseUrl.TrimEnd("/")

Invoke-SmokeRequest -Name "Frontend" -Uri "$base/"
Invoke-SmokeRequest -Name "Backend health" -Uri "$base/api/actuator/health"
Invoke-SmokeRequest -Name "Private API rejects anonymous" -Uri "$base/api/auth/me" -ExpectedStatus @(401, 403)

if ($Email -and $Password) {
    $loginBody = @{
        email = $Email
        password = $Password
    }
    $loginResponse = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body ($loginBody | ConvertTo-Json)
    if (-not $loginResponse.token) {
        throw "[Login] no token returned"
    }
    $headers = @{ Authorization = "Bearer $($loginResponse.token)" }
    Invoke-SmokeRequest -Name "Authenticated /me" -Uri "$base/api/auth/me" -Headers $headers
}

Write-Host "Smoke test termine."
