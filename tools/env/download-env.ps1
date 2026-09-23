# Campus second-hand market - dev env download script (all under D:\dev-env)
# Usage: powershell -ExecutionPolicy Bypass -File download-env.ps1
$ErrorActionPreference = 'Continue'
$root = 'D:\dev-env'
$dl = "$root\downloads"
New-Item -ItemType Directory -Force -Path $dl | Out-Null
Set-Location $dl

function Get-File {
    param([string]$Url, [string]$Out, [string[]]$Fallbacks = @())
    if (Test-Path $Out) { Write-Host "[SKIP] $Out exists"; return $true }
    $urls = @($Url) + $Fallbacks
    foreach ($u in $urls) {
        Write-Host "[GET] $u"
        & curl.exe -L --connect-timeout 20 --max-time 3600 -o "$Out.tmp" $u 2>$null
        if ($LASTEXITCODE -eq 0 -and (Test-Path "$Out.tmp") -and ((Get-Item "$Out.tmp").Length -gt 1MB)) {
            Move-Item "$Out.tmp" $Out -Force
            Write-Host ("[OK]  {0} ({1} MB)" -f $Out, [math]::Round((Get-Item $Out).Length/1MB,1))
            return $true
        }
        Remove-Item "$Out.tmp" -Force -ErrorAction SilentlyContinue
        Write-Host "[FAIL] $u"
    }
    return $false
}

# 1) Maven 3.9.9
Get-File 'https://mirrors.aliyun.com/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' 'maven.zip' @(
    'https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip',
    'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'
) | Out-Null

# 2) MySQL 8.0.42 winx64
Get-File 'https://mirrors.aliyun.com/mysql/MySQL-8.0/mysql-8.0.42-winx64.zip' 'mysql.zip' @(
    'https://mirrors.tuna.tsinghua.edu.cn/mysql/downloads/MySQL-8.0/mysql-8.0.42-winx64.zip',
    'https://mirrors.aliyun.com/mysql/MySQL-8.0/mysql-8.0.43-winx64.zip'
) | Out-Null

# 3) Redis 7.4 Windows (cygwin build), fallback tporadowski 5.0.14
Get-File 'https://github.com/redis-windows/redis-windows/releases/download/7.4.2/Redis-7.4.2-Windows-x64-cygwin.zip' 'redis.zip' @(
    'https://ghproxy.net/https://github.com/redis-windows/redis-windows/releases/download/7.4.2/Redis-7.4.2-Windows-x64-cygwin.zip',
    'https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip'
) | Out-Null

# 4) Erlang OTP 26 (required by RabbitMQ)
Get-File 'https://github.com/erlang/otp/releases/download/OTP-26.2.5.11/otp_win64_26.2.5.11.exe' 'erlang.exe' @(
    'https://ghproxy.net/https://github.com/erlang/otp/releases/download/OTP-26.2.5.11/otp_win64_26.2.5.11.exe',
    'https://gh-proxy.com/https://github.com/erlang/otp/releases/download/OTP-26.2.5.11/otp_win64_26.2.5.11.exe'
) | Out-Null

# 5) RabbitMQ 3.13.7
Get-File 'https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.13.7/rabbitmq-server-windows-3.13.7.zip' 'rabbitmq.zip' @(
    'https://ghproxy.net/https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.13.7/rabbitmq-server-windows-3.13.7.zip'
) | Out-Null

Write-Host '=== ALL DOWNLOADS DONE ==='
Get-ChildItem $dl | Format-Table Name, @{L='MB';E={[math]::Round($_.Length/1MB,1)}} -AutoSize
