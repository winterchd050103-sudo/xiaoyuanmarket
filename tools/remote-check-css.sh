#!/bin/bash
echo "=== css files in container ==="
docker exec market-frontend sh -c 'ls /usr/share/nginx/html/market/assets/'
echo "=== media query count in each css ==="
for f in $(docker exec market-frontend sh -c 'ls /usr/share/nginx/html/market/assets/*.css'); do
  docker exec market-frontend sh -c "grep -c '768px' $f"
done
