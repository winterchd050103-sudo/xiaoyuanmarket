# Collect deployment files into deploy-package\ and zip it.
# Upload deploy-package.zip to ECS, then follow docker\DEPLOY.md.
# Output contents:
#   backend\target\market-backend-1.0.0.jar   (pre-built jar)
#   sql\init.sql
#   docker\docker-compose.yml / .env.example / DEPLOY.md
#   frontend\  (source; built inside Docker on server)
$ErrorActionPreference = 'Stop'
$root = 'd:\PythonProject\xiaoyuanmarket'
$out = Join-Path $root 'deploy-package'

if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$out\backend\target" | Out-Null
New-Item -ItemType Directory -Force -Path "$out\sql" | Out-Null
New-Item -ItemType Directory -Force -Path "$out\docker" | Out-Null

# 1. backend jar (must be built first)
$jar = Join-Path $root 'backend\target\market-backend-1.0.0.jar'
if (-not (Test-Path $jar)) { Write-Error "jar not found: $jar (run mvn package first)"; exit 1 }
Copy-Item $jar "$out\backend\target\"

# 2. backend Dockerfile + seed product images (data volume is empty on first deploy)
Copy-Item (Join-Path $root 'backend\Dockerfile') "$out\backend\"
if (Test-Path "$root\backend\upload\seed") { Copy-Item "$root\backend\upload\seed" "$out\backend\seed-images" -Recurse }

# 3. sql
Copy-Item (Join-Path $root 'sql\init.sql') "$out\sql\"

# 4. docker dir (compose + env example + deploy doc)
Copy-Item (Join-Path $root 'docker\docker-compose.yml') "$out\docker\"
Copy-Item (Join-Path $root 'docker\.env.example') "$out\docker\"
Copy-Item (Join-Path $root 'docker\DEPLOY.md') "$out\docker\"

# 5. frontend source (Dockerfile + nginx.conf + src + configs, exclude node_modules/dist)
$feOut = "$out\frontend"
New-Item -ItemType Directory -Force -Path $feOut | Out-Null
foreach ($f in @('Dockerfile','nginx.conf','package.json','vite.config.js','index.html')) {
    $src = Join-Path $root "frontend\$f"
    if (Test-Path $src) { Copy-Item $src $feOut }
}
if (Test-Path "$root\frontend\src") { Copy-Item "$root\frontend\src" "$feOut\src" -Recurse }
if (Test-Path "$root\frontend\public") { Copy-Item "$root\frontend\public" "$feOut\public" -Recurse }

# 6. zip
$zip = Join-Path $root 'deploy-package.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path "$out\*" -DestinationPath $zip
$size = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Output "deploy-package.zip created: $size MB"
Write-Output "Next: scp deploy-package.zip root@<IP>:/root/  then see docker\DEPLOY.md"
