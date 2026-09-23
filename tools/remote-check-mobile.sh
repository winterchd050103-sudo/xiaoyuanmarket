#!/bin/bash
echo "=== mobile.css on server ==="
ls -la /root/market/frontend/src/styles/
echo "=== main.js imports ==="
grep -n "import" /root/market/frontend/src/main.js
echo "=== css in container has 768px? ==="
for f in $(docker exec market-frontend sh -c 'ls /usr/share/nginx/html/market/assets/*.css'); do
  n=$(docker exec market-frontend sh -c "grep -c 'max-width:768px' $f")
  echo "$f -> $n"
done
