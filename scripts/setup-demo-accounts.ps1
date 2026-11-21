param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$privateDir = Join-Path $repo 'target'
$credentialFile = Join-Path $privateDir 'demo-accounts.credentials.json'
$environmentFile = Join-Path $privateDir 'PastiSiap-Demo-Accounts.postman_environment.json'
$oldAdmin = $env:DEMO_ADMIN_PASSWORD
$oldStaff = $env:DEMO_STAFF_PASSWORD
function New-DemoPassword {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes); return 'Demo-' + [Convert]::ToBase64String($bytes) }
    finally { $rng.Dispose() }
}
function Assert-DockerExit([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed. Check target/demo-accounts-setup.log (no passwords are logged)." }
}
function Test-DemoLogin($credentials, [string]$kind, [string]$role, [string[]]$allowed, [string]$forbidden) {
    $email = $credentials."${kind}Email"
    $password = $credentials."${kind}Password"
    $csrf = Invoke-RestMethod "$($credentials.baseUrl)/api/v1/csrf" -SessionVariable browser -TimeoutSec 15
    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    $login = Invoke-RestMethod "$($credentials.baseUrl)/api/v1/auth/login" -Method Post -WebSession $browser -Headers @{ 'X-CSRF-TOKEN' = $csrf.token } -ContentType 'application/json' -Body $loginBody -TimeoutSec 15
    if ($login.user.email -ne $email -or $login.user.userType -ne 'INTERNAL' -or @($login.user.roles).Count -ne 1 -or $login.user.roles[0] -ne $role) {
        throw "Unexpected account identity or permissions for $kind."
    }
    foreach ($endpoint in $allowed) {
        $null = Invoke-RestMethod "$($credentials.baseUrl)/api/v1/$endpoint" -WebSession $browser -TimeoutSec 15
    }
    $denied = $false
    try { $null = Invoke-RestMethod "$($credentials.baseUrl)/api/v1/$forbidden" -WebSession $browser -TimeoutSec 15 }
    catch { if ($null -ne $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $denied = $true } else { throw } }
    if (-not $denied) { throw "Role boundary check failed for $kind." }
    return @{ email = $email; userType = 'INTERNAL'; role = $role; login = 'PASS'; allowed = $allowed; forbidden = $forbidden; forbiddenStatus = 403 }
}
Push-Location $repo
try {
    $null = New-Item -ItemType Directory -Path $privateDir -Force
    if (Test-Path -LiteralPath $credentialFile) {
        $credentials = Get-Content -LiteralPath $credentialFile -Raw | ConvertFrom-Json
    } else {
        $credentials = [pscustomobject]@{ baseUrl = 'http://localhost:8080'; adminEmail = 'admin.demo@pastisiap.test'; adminPassword = New-DemoPassword; staffEmail = 'staff.demo@pastisiap.test'; staffPassword = New-DemoPassword }
        $credentials | ConvertTo-Json | Set-Content -LiteralPath $credentialFile -Encoding UTF8
    }
    if ($credentials.adminEmail -ne 'admin.demo@pastisiap.test' -or $credentials.staffEmail -ne 'staff.demo@pastisiap.test' -or
        $credentials.adminPassword.Length -lt 12 -or $credentials.staffPassword.Length -lt 12) { throw 'Invalid local demo credential file.' }
    # Credentials are passed through process environment, never through shell arguments or public exports.
    $env:DEMO_ADMIN_PASSWORD = $credentials.adminPassword
    $env:DEMO_STAFF_PASSWORD = $credentials.staffPassword
    $log = Join-Path $privateDir 'demo-accounts-setup.log'
    if (-not $SkipBuild) {
        Write-Host 'Building backend image...'
        & docker compose build api *> $log
        Assert-DockerExit 'Backend build'
    }
    & docker compose up -d api worker *>> $log
    Assert-DockerExit 'Backend startup'
    Write-Host 'Creating or checking local demo accounts...'
    & docker compose --profile demo-tools run --rm --no-deps demo-accounts *>> $log
    Assert-DockerExit 'Demo account setup'
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        try { $health = Invoke-RestMethod "$($credentials.baseUrl)/actuator/health" -TimeoutSec 3; if ($health.status -eq 'UP') { $ready = $true; break } } catch { }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'Backend is not healthy. Check target/demo-accounts-setup.log and docker compose logs api.' }
    $results = @(
        Test-DemoLogin $credentials 'admin' 'PLATFORM_ADMIN' @('admin/roles', 'admin/dashboard', 'admin/users') 'staff/dashboard'
        Test-DemoLogin $credentials 'staff' 'QUESTION_STAFF' @('staff/dashboard', 'staff/taxonomy', 'staff/question-bank', 'staff/question-sets', 'staff/workbook-imports', 'staff/import-history', 'staff/pool-requirements', 'staff/audit') 'admin/users'
    )
    $values = @(@{ key = 'baseUrl'; value = $credentials.baseUrl; type = 'default'; enabled = $true })
    foreach ($key in @('adminEmail', 'adminPassword', 'staffEmail', 'staffPassword')) {
        $values += @{ key = $key; value = $credentials.$key; type = $(if ($key.EndsWith('Password')) { 'secret' } else { 'default' }); enabled = $true }
    }
    @{ name = 'PastiSiap Local Demo Accounts (PRIVATE)'; values = $values; _postman_variable_scope = 'environment' } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $environmentFile -Encoding UTF8
    @{ checkedAt = [DateTime]::UtcNow.ToString('o'); health = 'UP'; accounts = $results } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $privateDir 'demo-accounts-verification.json') -Encoding UTF8
    Write-Host 'PASS: both logins, exact roles and cross-role access checks.'
    Write-Host "Private credentials: $credentialFile"
    Write-Host "Private Postman environment: $environmentFile"
} finally {
    $env:DEMO_ADMIN_PASSWORD = $oldAdmin
    $env:DEMO_STAFF_PASSWORD = $oldStaff
    Pop-Location
}
