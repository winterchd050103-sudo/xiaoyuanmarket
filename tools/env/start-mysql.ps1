# Start MySQL (portable, no service install)
# Usage: powershell -ExecutionPolicy Bypass -File start-mysql.ps1
$mysqlHome = 'D:\dev-env\mysql-8.0.46-winx64'
$datadir = 'D:\dev-env\mysql8\data'

if (-not (Test-Path "$mysqlHome\bin\mysqld.exe")) {
    Write-Error "MySQL not found at $mysqlHome"
    exit 1
}

# first-time initialization
if (-not (Test-Path $datadir)) {
    Write-Host '[INIT] initializing datadir (root no password)...'
    & "$mysqlHome\bin\mysqld.exe" --initialize-insecure --basedir="$mysqlHome" --datadir="$datadir" --lower-case-table-names=1 --console
    if ($LASTEXITCODE -ne 0) { Write-Error 'mysqld initialize failed'; exit 1 }
}

Write-Host '[START] mysqld on port 3306, log: D:\dev-env\mysql8\mysqld.log'
Start-Process -FilePath "$mysqlHome\bin\mysqld.exe" -ArgumentList "--defaults-file=D:\dev-env\mysql8\my.ini", "--console" -WindowStyle Hidden
Start-Sleep -Seconds 5
& "$mysqlHome\bin\mysql.exe" -h127.0.0.1 -uroot -e "SELECT VERSION();"
