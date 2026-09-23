# Start Redis (portable)
# Usage: powershell -ExecutionPolicy Bypass -File start-redis.ps1
$redisDir = 'D:\dev-env\Redis-7.4.2-Windows-x64-cygwin'
$conf = 'D:\dev-env\redis\redis.conf'

if (-not (Test-Path "$redisDir\redis-server.exe")) {
    # tporadowski fallback layout
    $alt = Get-ChildItem 'D:\dev-env' -Recurse -Filter 'redis-server.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($alt) { $redisDir = $alt.DirectoryName } else { Write-Error 'redis-server.exe not found'; exit 1 }
}

Write-Host "[START] redis-server on port 6379 (dir: $redisDir)"
Start-Process -FilePath "$redisDir\redis-server.exe" -ArgumentList "/cygdrive/d/dev-env/redis/redis.conf" -WindowStyle Hidden
Start-Sleep -Seconds 2
& "$redisDir\redis-cli.exe" -a redis123456 ping 2>$null
