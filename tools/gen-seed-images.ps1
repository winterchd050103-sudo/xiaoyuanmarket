# Generate simple product illustration images for seed products (local, no network)
Add-Type -AssemblyName System.Drawing

$dir = 'd:\PythonProject\xiaoyuanmarket\backend\upload\seed'
New-Item -ItemType Directory -Force -Path $dir | Out-Null

function New-ProductImage {
    param([string]$File, [string]$Title, [string]$BgHex, [string]$AccentHex, [scriptblock]$Draw)

    $w = 800; $h = 600
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAlias'

    $bg = [System.Drawing.ColorTranslator]::FromHtml($BgHex)
    $g.Clear($bg)

    $accent = [System.Drawing.ColorTranslator]::FromHtml($AccentHex)
    $brush = New-Object System.Drawing.SolidBrush($accent)
    $pen = New-Object System.Drawing.Pen($accent, 8)

    & $Draw $g $brush $pen $w $h

    # title text
    $font = New-Object System.Drawing.Font('Microsoft YaHei', 26, [System.Drawing.FontStyle]::Bold)
    $tBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(60,60,60))
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = 'Center'
    $g.DrawString($Title, $font, $tBrush, ($w/2), ($h - 80), $fmt)

    $g.Dispose()
    $bmp.Save("$dir\$File", [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $bmp.Dispose()
    Write-Output "$File OK"
}

# 1. iPhone 12 (blue phone)
New-ProductImage -File '1.jpg' -Title 'iPhone 12 128G 蓝色 95新' -BgHex '#EEF3FA' -AccentHex '#3B6FB6' -Draw {
    param($g, $brush, $pen, $w, $h)
    $g.FillRectangle($brush, 320, 110, 160, 320)                       # body
    $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25,35,55))
    $g.FillRectangle($dark, 335, 140, 130, 240)                        # screen
    $cam = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220,230,245))
    $g.FillRectangle($cam, 335, 118, 46, 46)                           # camera
}

# 2. Laptop
New-ProductImage -File '2.jpg' -Title '联想小新Pro16 2022款' -BgHex '#F2F0EC' -AccentHex '#8C9099' -Draw {
    param($g, $brush, $pen, $w, $h)
    $g.FillRectangle($brush, 240, 130, 320, 200)                       # lid/screen
    $scr = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(40,50,70))
    $g.FillRectangle($scr, 255, 145, 290, 170)
    $g.FillRectangle($brush, 210, 330, 380, 26)                        # base
}

# 3. Math textbooks (two stacked books)
New-ProductImage -File '3.jpg' -Title '高等数学 同济第七版 上下册' -BgHex '#F7F3EC' -AccentHex '#2E5E8C' -Draw {
    param($g, $brush, $pen, $w, $h)
    $g.FillRectangle($brush, 250, 300, 300, 46)                        # book 1
    $b2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(96,74,110))
    $g.FillRectangle($b2, 270, 248, 300, 46)                           # book 2
    $page = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(245,245,240))
    $g.FillRectangle($page, 548, 254, 22, 92)
}

# 4. Foldable desk
New-ProductImage -File '4.jpg' -Title '宿舍折叠书桌 小户型必备' -BgHex '#EFF5F1' -AccentHex '#E8E4DC' -Draw {
    param($g, $brush, $pen, $w, $h)
    $top = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(250,248,244))
    $g.FillRectangle($top, 200, 220, 400, 34)                          # desktop
    $leg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(210,200,185))
    $g.FillRectangle($leg, 230, 254, 20, 150)                          # legs
    $g.FillRectangle($leg, 550, 254, 20, 150)
}

# 5. Badminton rackets (two crossed)
New-ProductImage -File '5.jpg' -Title '尤尼克斯羽毛球拍 双拍' -BgHex '#F4F1F7' -AccentHex '#2F6FAE' -Draw {
    param($g, $brush, $pen, $w, $h)
    $p2 = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190,70,80), 8)
    $g.DrawEllipse($pen, 250, 110, 180, 220)                           # racket 1 head
    $g.DrawEllipse($p2, 370, 110, 180, 220)                            # racket 2 head
    $g.DrawLine($pen, 340, 330, 300, 460)                              # handle 1
    $g.DrawLine($p2, 460, 330, 500, 460)                               # handle 2
    $sh = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillEllipse($sh, 380, 240, 60, 60)                              # shuttlecock
}
Write-Output 'DONE'
