#!/bin/bash
for f in $(docker exec market-frontend sh -c 'ls /usr/share/nginx/html/market/assets/*.css'); do
  n=$(docker exec market-frontend sh -c "grep -c '768px' $f")
  [ "$n" -gt 0 ] && echo "$(basename $f) -> $n"
done
echo "=== mobile.css specific rules present? ==="
docker exec market-frontend sh -c 'grep -o "el-dialog{width:94%" /usr/share/nginx/html/market/assets/index-*.css' | head -1
docker exec market-frontend sh -c 'grep -o "category-side{display:flex" /usr/share/nginx/html/market/assets/HomeView-*.css' | head -1
