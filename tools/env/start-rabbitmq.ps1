# Start RabbitMQ (portable Erlang + portable RabbitMQ)
# Usage: powershell -ExecutionPolicy Bypass -File start-rabbitmq.ps1
$env:ERLANG_HOME = 'D:\dev-env\erlang'
$env:RABBITMQ_BASE = 'D:\dev-env\rabbitmq-data'
$env:HOMEDRIVE = 'D:'
$env:HOMEPATH = '\dev-env\rabbitmq-data\home'

$rmq = 'D:\dev-env\rabbitmq_server-3.13.7\sbin\rabbitmq-server.bat'
if (-not (Test-Path $rmq)) { Write-Error "rabbitmq-server.bat not found: $rmq"; exit 1 }

New-Item -ItemType Directory -Force -Path $env:RABBITMQ_BASE | Out-Null
New-Item -ItemType Directory -Force -Path "$($env:HOMEDRIVE)$($env:HOMEPATH)" | Out-Null

Write-Host '[START] rabbitmq-server (AMQP 5672 / Management 15672)'
Start-Process -FilePath $rmq -WindowStyle Hidden
Start-Sleep -Seconds 15
& 'D:\dev-env\rabbitmq_server-3.13.7\sbin\rabbitmqctl.bat' status 2>$null | Select-Object -First 5
