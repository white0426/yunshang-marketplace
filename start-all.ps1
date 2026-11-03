$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = 'C:\Program Files\Java\jdk-17'
$java = Join-Path $javaHome 'bin\java.exe'
$jar = Join-Path $root 'server\target\market-server-0.1.0.jar'
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
if ($null -ne $nodeCommand) {
    $node = $nodeCommand.Source
} else {
    throw '未找到 Node.js，请先安装 Node.js 并将其加入 PATH'
}
$vite = Join-Path $root 'node_modules\vite\bin\vite.js'

if (-not $env:DB_USERNAME) {
    $env:DB_USERNAME = 'yunshang_app'
}
if (-not $env:DB_PASSWORD) {
    throw '请先设置环境变量 DB_PASSWORD 后再启动项目'
}
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = [Convert]::ToBase64String(
        [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48)
    )
}

if (-not $env:VITE_PAYMENT_BASE) {
    $defaultRoute = Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue |
        Sort-Object RouteMetric |
        Select-Object -First 1
    $lanIp = $null
    if ($null -ne $defaultRoute) {
        $lanIp = Get-NetIPAddress -InterfaceIndex $defaultRoute.InterfaceIndex `
            -AddressFamily IPv4 -ErrorAction SilentlyContinue |
            Where-Object { $_.IPAddress -notlike '169.254.*' } |
            Select-Object -First 1 -ExpandProperty IPAddress
    }
    if ($lanIp) {
        $env:VITE_PAYMENT_BASE = "http://${lanIp}:4173"
    } else {
        $env:VITE_PAYMENT_BASE = 'http://127.0.0.1:4173'
    }
}

if (-not (Test-Path $jar)) {
    throw '后端尚未构建，请先在 server 目录执行 Maven package'
}
if (-not (Test-Path $vite)) {
    throw '前端依赖尚未安装，请先执行 pnpm install'
}

$backendHealth = $null
try {
    $backendHealth = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 2
} catch {
}

if ($null -eq $backendHealth) {
    Start-Process -FilePath $java -ArgumentList @('-jar', $jar) `
        -WorkingDirectory (Join-Path $root 'server') -WindowStyle Hidden
    Write-Host 'Spring Boot started: http://127.0.0.1:8080'
} else {
    Write-Host 'Spring Boot is already running.'
}

$frontend = $null
try {
    $frontend = Invoke-WebRequest -Uri 'http://127.0.0.1:4173' -UseBasicParsing -TimeoutSec 2
} catch {
}

if ($null -eq $frontend) {
    Start-Process -FilePath $node -ArgumentList @($vite, '--host', '0.0.0.0', '--port', '4173') `
        -WorkingDirectory $root -WindowStyle Hidden
    Write-Host 'Frontend started: http://127.0.0.1:4173'
} else {
    Write-Host 'Frontend is already running.'
}

Write-Host 'Login page: http://127.0.0.1:4173/login'
if ($env:VITE_PAYMENT_BASE -notlike '*127.0.0.1*') {
    Write-Host "LAN access: $($env:VITE_PAYMENT_BASE)/login"
}
