#!/bin/bash
PW=$(grep '^MYSQL_PASSWORD=' /root/market/docker/.env | cut -d= -f2)
echo "=== all users ==="
docker exec market-mysql mysql -uroot -p"$PW" --default-character-set=utf8mb4 -e "SELECT id,username,nickname,create_time FROM xiaoyuan_market.user;" 2>/dev/null
echo "=== products ==="
docker exec market-mysql mysql -uroot -p"$PW" --default-character-set=utf8mb4 -e "SELECT id,title,status FROM xiaoyuan_market.product;" 2>/dev/null
echo "=== server charset vars ==="
docker exec market-mysql mysql -uroot -p"$PW" -e "SHOW VARIABLES LIKE 'character%';" 2>/dev/null
echo "=== backend jdbc url ==="
docker exec market-backend sh -c 'env | grep -i -E "datasource|jdbc" || true; grep -o "jdbc[^\"]*" /app/application-prod.yml 2>/dev/null || true'
