#!/bin/bash
set -e
apt-get install -y unzip >/dev/null 2>&1 || (apt-get update -qq && apt-get install -y unzip >/dev/null)
cd /root
rm -rf market
tar -xf deploy-package.tar
mv deploy-package market
cd market/docker
M1=$(openssl rand -hex 16)
M2=$(openssl rand -hex 16)
M3=$(openssl rand -hex 16)
M4="pay_$(openssl rand -hex 16)"
M5=$(openssl rand -hex 32)
cat > .env <<EOF
MYSQL_PASSWORD=$M1
REDIS_PASSWORD=$M2
RABBITMQ_USER=market_app
RABBITMQ_PASSWORD=$M3
ORDER_TIMEOUT_MINUTES=15
PAY_SALT=$M4
JWT_SECRET=$M5
EOF
chmod 600 .env
echo "=== .env created ==="
grep -c '=' .env
docker compose config --quiet && echo 'COMPOSE-VALID'
