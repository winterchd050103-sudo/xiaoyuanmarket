#!/bin/bash
set -e
cd /root/market
PW=$(grep '^MYSQL_PASSWORD=' docker/.env | cut -d= -f2)
RP=$(grep '^REDIS_PASSWORD=' docker/.env | cut -d= -f2)

echo "=== re-import init.sql with utf8mb4 ==="
docker exec -i market-mysql mysql -uroot -p"$PW" --default-character-set=utf8mb4 < sql/init.sql 2>&1 | grep -v 'password on the command line' || true

echo "=== verify data ==="
docker exec market-mysql mysql -uroot -p"$PW" --default-character-set=utf8mb4 -e "SELECT id,name FROM xiaoyuan_market.category LIMIT 3; SELECT username,nickname FROM xiaoyuan_market.user LIMIT 3;" 2>/dev/null

echo "=== flush redis cache ==="
docker exec market-redis redis-cli -a "$RP" FLUSHALL 2>/dev/null

echo "=== restart backend ==="
docker restart market-backend
