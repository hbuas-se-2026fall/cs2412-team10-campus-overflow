# Lab 4 verification helper (T-031 + build evidence)
# Usage:  powershell -ExecutionPolicy Bypass -File docs\lab4\lab4-verify.ps1
# Requires: Docker Desktop running, JDK 21, Maven 3.9+
# Scope: Docker check -> compose up -> wait healthy -> Maven build -> test count -> report.
# It does NOT start the Spring Boot app. Run section 4/5 of the Lab4 guide afterwards.

$ErrorActionPreference = "Stop"
$repo    = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$backend = Join-Path $repo "campus-overflow-student\backend"
$report  = Join-Path $PSScriptRoot "lab4-verify-report.md"
$buildLog = Join-Path $PSScriptRoot "lab4-build-local.log"
$script:lines = New-Object System.Collections.Generic.List[string]

function Note([string]$s) {
    Write-Host $s
    $script:lines.Add($s)
}

Note "# Lab 4 verification report"
Note ""
Note ("Generated: " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))
Note ("Repo: " + $repo)
Note ""

# --- 1. Docker daemon ---
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not running. Start it, wait until the tray icon is ready, then re-run."
}
Note "[OK] Docker daemon reachable"

# --- 2. Compose up (repo root docker-compose.yml: MySQL 8 + Redis 7) ---
Push-Location $repo
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }
} finally {
    Pop-Location
}
Note "[OK] docker compose up -d"

# --- 3. Wait until both containers are healthy ---
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    $m = (docker inspect -f "{{.State.Health.Status}}" co-mysql 2>$null)
    $r = (docker inspect -f "{{.State.Health.Status}}" co-redis 2>$null)
    if ($m -eq "healthy" -and $r -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    throw "MySQL or Redis did not become healthy within 120s. Run 'docker compose ps' and check logs."
}
Note "[OK] co-mysql and co-redis are healthy"
Note ""
Note "## Containers"
Note '```'
Note ((docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Where-Object { $_ -match "co-mysql|co-redis" }) -join "`n")
Note '```'
Note ""

# --- 4. Standard Lab 4 build ---
Push-Location $backend
try {
    mvn clean install -DskipITs 2>&1 | Tee-Object -FilePath $buildLog | Select-Object -Last 5 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed. See $buildLog" }
} finally {
    Pop-Location
}
Note "[OK] mvn clean install -DskipITs : BUILD SUCCESS"
Note ("[..] build log: " + $buildLog)

# --- 5. Count unit tests from surefire reports ---
$xmls = Get-ChildItem -Path $backend -Recurse -Filter "TEST-*.xml" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -like "*surefire-reports*" }
$tests = 0; $failures = 0; $errors = 0; $skipped = 0
foreach ($x in $xmls) {
    try {
        $n = [xml](Get-Content $x.FullName -Raw)
        $tests    += [int]$n.testsuite.tests
        $failures += [int]$n.testsuite.failures
        $errors   += [int]$n.testsuite.errors
        $skipped  += [int]$n.testsuite.skipped
    } catch { }
}
Note ("[OK] unit tests: " + $xmls.Count + " classes / " + $tests + " cases / failures=" + $failures + " errors=" + $errors + " skipped=" + $skipped)
$jar = Join-Path $backend "co-bootstrap\target\campus-overflow.jar"
Note ("[OK] jar exists: " + (Test-Path $jar))
Note ""

# --- 6. Manual steps still required ---
Note "## Next (manual, see Lab4 guide section 4 and 5)"
Note '```'
Note "cd campus-overflow-student\backend"
Note "java -jar co-bootstrap\target\campus-overflow.jar"
Note "curl http://localhost:8080/actuator/health"
Note 'docker exec -it co-mysql mysql -uco_user -pco_pass campusoverflow -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank; SHOW TABLES;"'
Note '```'
Note ""
Note "Then in Postman: GET /api/v1/auth/csrf -> POST /api/v1/auth/login (2023001 / Campus2026)"
Note "-> POST /api/v1/questions (expect 201) -> GET /api/v1/questions, and save screenshots + run report."
Note ""

$script:lines | Out-File -FilePath $report -Encoding utf8
Write-Host ""
Write-Host ("Report written to: " + $report)
