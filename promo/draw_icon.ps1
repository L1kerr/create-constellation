Add-Type -AssemblyName System.Drawing

$S = 512
$bmp = New-Object System.Drawing.Bitmap($S, $S)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'

# dark space background
$rect = New-Object System.Drawing.Rectangle(0, 0, $S, $S)
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, [System.Drawing.Color]::FromArgb(255,16,22,48), [System.Drawing.Color]::FromArgb(255,7,10,22), 90)
$g.FillRectangle($grad, $rect)

function Disc($cx, $cy, $r, $color) {
    $b = New-Object System.Drawing.SolidBrush($color)
    $g.FillEllipse($b, [single]($cx-$r), [single]($cy-$r), [single]($r*2), [single]($r*2))
    $b.Dispose()
}

function Ring($cx, $cy, $r, $color, $w) {
    $p = New-Object System.Drawing.Pen($color, [single]$w)
    $g.DrawEllipse($p, [single]($cx-$r), [single]($cy-$r), [single]($r*2), [single]($r*2))
    $p.Dispose()
}

function Link($x0, $y0, $x1, $y1, $color, $w) {
    $p = New-Object System.Drawing.Pen($color, [single]$w)
    $g.DrawLine($p, [single]$x0, [single]$y0, [single]$x1, [single]$y1)
    $p.Dispose()
}

# a few faint stars
$rnd = New-Object System.Random(9)
for ($i=0; $i -lt 60; $i++) {
    $x = $rnd.Next($S); $y = $rnd.Next($S); $a = $rnd.Next(50, 200); $sz = $rnd.Next(1, 3)
    $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($a, 210, 220, 255))
    $g.FillEllipse($b, $x, $y, $sz, $sz)
    $b.Dispose()
}

$cx = 256.0; $cy = 268.0

# orbit with 3 planets (constellation look)
Ring $cx $cy 190 ([System.Drawing.Color]::FromArgb(110,120,140,190)) 3

$planets = @(
    @{A=-2.35; CR=74;  CG=144; CB=217},
    @{A=0.0;   CR=217; CG=131; CB=74},
    @{A=2.35;  CR=106; CG=176; CB=74}
)
foreach ($p in $planets) {
    $px = $cx + 190*[Math]::Cos($p.A); $py = $cy + 190*[Math]::Sin($p.A)
    $pc = [System.Drawing.Color]::FromArgb(255, $p.CR, $p.CG, $p.CB)
    Disc $px $py 34 ([System.Drawing.Color]::FromArgb(40, $p.CR, $p.CG, $p.CB))
    Disc $px $py 24 ([System.Drawing.Color]::FromArgb(255, [int]($p.CR*0.45), [int]($p.CG*0.45), [int]($p.CB*0.45)))
    Disc ($px-4) ($py-4) 20 $pc
    Disc ($px-9) ($py-9) 7 ([System.Drawing.Color]::FromArgb(110,255,255,255))
}

# golden links from sun to planets (the "tree")
foreach ($p in $planets) {
    $px = $cx + 190*[Math]::Cos($p.A); $py = $cy + 190*[Math]::Sin($p.A)
    Link $cx $cy $px $py ([System.Drawing.Color]::FromArgb(220,231,195,106)) 4
}

# sun in the center with glow
Disc $cx $cy 92 ([System.Drawing.Color]::FromArgb(26,255,176,64))
Disc $cx $cy 72 ([System.Drawing.Color]::FromArgb(48,255,192,80))
Disc $cx $cy 54 ([System.Drawing.Color]::FromArgb(255,255,160,48))
Disc ($cx-14) ($cy-14) 22 ([System.Drawing.Color]::FromArgb(140,255,224,160))

# subtle golden rim around the whole icon (medal feel)
Ring $cx $cy 244 ([System.Drawing.Color]::FromArgb(90,231,195,106)) 3

$bmp.Save('D:\OpenCode\Mod\promo\icon.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output 'icon done'
