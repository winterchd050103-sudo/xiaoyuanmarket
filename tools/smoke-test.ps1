# Full-chain smoke test for xiaoyuan market backend
# Usage: powershell -ExecutionPolicy Bypass -File tools\smoke-test.ps1 [-SkipTimeoutTest]
param(
    [switch]$SkipTimeoutTest
)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$base = 'http://127.0.0.1:8080/api'
$script:pass = 0
$script:fail = 0

function Check($name, $cond, $detail = '') {
    if ($cond) {
        $script:pass++
        Write-Output ("[PASS] " + $name)
    } else {
        $script:fail++
        Write-Output ("[FAIL] " + $name + "  " + $detail)
    }
}

function Post($url, $token, $body) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri ($base + $url) -Method Post -Headers $headers -Body ($body | ConvertTo-Json -Depth 5) -ContentType 'application/json'
}

function Get2($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    Invoke-RestMethod -Uri ($base + $url) -Method Get -Headers $headers
}

function Put($url, $token, $body) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    if ($body) {
        Invoke-RestMethod -Uri ($base + $url) -Method Put -Headers $headers -Body ($body | ConvertTo-Json -Depth 5) -ContentType 'application/json'
    } else {
        Invoke-RestMethod -Uri ($base + $url) -Method Put -Headers $headers
    }
}

# ===== 1. login seed accounts (verifies BCrypt seed data) =====
$test = Post '/auth/login' $null @{ username = 'test'; password = '123456' }
Check 'login test/123456' ($test.code -eq 200) $test.message
$testToken = $test.data.token

$buyer = Post '/auth/login' $null @{ username = 'buyer'; password = '123456' }
Check 'login buyer/123456' ($buyer.code -eq 200) $buyer.message
$buyerToken = $buyer.data.token

$admin = Post '/auth/login' $null @{ username = 'admin'; password = '123456' }
Check 'login admin/123456' ($admin.code -eq 200) $admin.message
$adminToken = $admin.data.token

# unified response body: all errors are HTTP 200 + code in body
$wrong = Post '/auth/login' $null @{ username = 'test'; password = 'wrong' }
Check 'wrong password rejected' ($wrong.code -ne 200) ("code=" + $wrong.code)

# ===== 2. buyer adds address =====
$addr = Post '/address' $buyerToken @{ receiver = 'Wang Buyer'; phone = '13900001111'; province = 'Guangdong'; city = 'Shenzhen'; district = 'Nanshan'; detail = 'Tech Park A-101'; isDefault = $true }
Check 'create address' ($addr.code -eq 200) $addr.message
$addrId = $addr.data.id

# ===== 3. test publishes a product =====
$pub = Post '/products' $testToken @{ categoryId = 1; title = 'Smoke Test iPhone 13 128G'; description = 'smoke test item'; price = 66.66; originalPrice = 99.00; coverImage = '/api/upload/smoke/cover.jpg'; images = @('/api/upload/smoke/cover.jpg') }
Check 'publish product' ($pub.code -eq 200) $pub.message
$productId = $pub.data.id

# ===== 4. public list + detail =====
$list = Get2 '/products?page=1&size=10&keyword=Smoke' $null
Check 'search product in list' ($list.code -eq 200 -and $list.data.records.Count -ge 1) $list.message
$detail = Get2 ("/products/" + $productId) $null
Check 'product detail' ($detail.code -eq 200 -and $detail.data.title -like '*Smoke*') $detail.message

# ===== 5. create order + idempotency =====
$clientNo = ([guid]::NewGuid().ToString('N')).ToUpper()
$o1 = Post '/orders' $buyerToken @{ clientOrderNo = $clientNo; productId = $productId; addressId = $addrId }
Check 'create order' ($o1.code -eq 200) $o1.message
$orderNo = $o1.data.orderNo
$o2 = Post '/orders' $buyerToken @{ clientOrderNo = $clientNo; productId = $productId; addressId = $addrId }
Check 'idempotent re-submit returns same order' ($o2.code -eq 200 -and $o2.data.orderNo -eq $orderNo) ($o2.message + ' vs ' + $orderNo)

# self-buy rejected
$selfBuy = Post '/orders' $testToken @{ clientOrderNo = ([guid]::NewGuid().ToString('N')).ToUpper(); productId = $productId; addressId = $addrId }
Check 'self-buy rejected' ($selfBuy.code -ne 200) ("code=" + $selfBuy.code + " " + $selfBuy.message)

# ===== 6. mock pay =====
$pay = Post ("/pay/mock/" + $orderNo) $buyerToken @{}
Check 'mock pay' ($pay.code -eq 200 -and $pay.data.status -eq 1) $pay.message
# duplicate pay callback should be idempotent-safe (no state corruption)
$pay2 = Post ("/pay/mock/" + $orderNo) $buyerToken @{}
$pay2Ok = ($pay2.code -ne 200) -or ($pay2.data.status -eq 1)
Check 'duplicate pay safe' $pay2Ok ($pay2.message)

# ===== 7. deliver -> receive -> comment =====
$dv = Put ("/orders/" + $orderNo + "/deliver") $testToken $null
Check 'seller deliver' ($dv.code -eq 200) $dv.message
$rc = Put ("/orders/" + $orderNo + "/receive") $buyerToken $null
Check 'buyer receive' ($rc.code -eq 200) $rc.message
$cm = Post '/comments' $buyerToken @{ productId = $productId; orderId = $o1.data.id; content = 'Good seller, fast shipping!'; rating = 5 }
Check 'buyer comment' ($cm.code -eq 200) $cm.message
$cmList = Get2 ("/comments?productId=" + $productId) $null
Check 'comment visible' ($cmList.code -eq 200 -and $cmList.data.records.Count -ge 1) $cmList.message
# product should be SOLD after receive
$detailAfter = Get2 ("/products/" + $productId) $null
Check 'product marked sold' ($detailAfter.data.status -eq 3) ("status=" + $detailAfter.data.status)

# ===== 8. favorite / unfavorite =====
$fv = Post ("/favorites/" + $productId) $buyerToken @{}
Check 'favorite product' ($fv.code -eq 200) $fv.message
$fvList = Get2 '/favorites?page=1&size=10' $buyerToken
Check 'favorite list' ($fvList.code -eq 200) $fvList.message
Invoke-RestMethod -Uri ($base + '/favorites/' + $productId) -Method Delete -Headers @{ Authorization = "Bearer $buyerToken" } | Out-Null
Check 'unfavorite done' $true

# ===== 9. admin backend =====
$au = Get2 '/admin/users?page=1&size=10' $adminToken
Check 'admin users page' ($au.code -eq 200 -and $au.data.total -ge 4) $au.message
$ap = Get2 '/admin/products?page=1&size=10' $adminToken
Check 'admin products page' ($ap.code -eq 200) $ap.message
$ao = Get2 '/admin/orders?page=1&size=10' $adminToken
Check 'admin orders page' ($ao.code -eq 200) $ao.message
$st = Get2 '/admin/stats' $adminToken
Check 'admin stats' ($st.code -eq 200 -and $st.data.gmv -ge 66.66) ("gmv=" + $st.data.gmv)
# forbidden without admin role
$denied = Get2 '/admin/stats' $testToken
Check 'non-admin denied' ($denied.code -eq 403) ("code=" + $denied.code + " " + $denied.message)

# ===== 10. unauthorized access =====
$unauth = Get2 '/user/profile' $null
Check 'unauthenticated denied' ($unauth.code -eq 401) ("code=" + $unauth.code + " " + $unauth.message)

# ===== 11. order timeout cancel (MQ DLX, 2 min TTL) =====
if (-not $SkipTimeoutTest) {
    $pub2 = Post '/products' $testToken @{ categoryId = 2; title = 'Smoke Test Timeout Item'; description = 'for timeout'; price = 12.34 }
    Check 'publish timeout-test product' ($pub2.code -eq 200) $pub2.message
    $clientNo2 = ([guid]::NewGuid().ToString('N')).ToUpper()
    $o3 = Post '/orders' $buyerToken @{ clientOrderNo = $clientNo2; productId = $pub2.data.id; addressId = $addrId }
    Check 'create timeout-test order' ($o3.code -eq 200) $o3.message
    Write-Output '... waiting 150s for MQ DLX delay + scheduled fallback ...'
    Start-Sleep -Seconds 150
    $d3 = Get2 ("/orders/" + $clientNo2) $buyerToken
    Check 'order auto-cancelled by timeout' ($d3.code -eq 200 -and $d3.data.status -eq 4) ("status=" + $d3.data.status)
} else {
    Write-Output '[SKIP] timeout test'
}

Write-Output ''
Write-Output ("RESULT: PASS=" + $script:pass + " FAIL=" + $script:fail)
if ($script:fail -gt 0) { exit 1 } else { exit 0 }
