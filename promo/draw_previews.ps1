Add-Type -AssemblyName System.Drawing

function New-StarSky($bmp) {
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $rect = New-Object System.Drawing.Rectangle(0,0,$bmp.Width,$bmp.Height)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, [System.Drawing.Color]::FromArgb(255,11,16,38), [System.Drawing.Color]::FromArgb(255,5,7,15), 90)
    $g.FillRectangle($grad, $rect)
    $rnd = New-Object System.Random(42)
    for ($i=0; $i -lt 400; $i++) {
        $x = $rnd.Next($bmp.Width); $y = $rnd.Next($bmp.Height)
        $a = $rnd.Next(60,255); $s = $rnd.Next(1,3)
        $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($a,220,230,255))
        $g.FillEllipse($b, $x, $y, $s, $s)
        $b.Dispose()
    }
    return $g
}

function Draw-Disc($g, $cx, $cy, $r, $color) {
    $b = New-Object System.Drawing.SolidBrush($color)
    $g.FillEllipse($b, [single]($cx-$r), [single]($cy-$r), [single]($r*2), [single]($r*2))
    $b.Dispose()
}

function Draw-Orbit($g, $cx, $cy, $r, $color, $width) {
    $p = New-Object System.Drawing.Pen($color, [single]$width)
    $g.DrawEllipse($p, [single]($cx-$r), [single]($cy-$r), [single]($r*2), [single]($r*2))
    $p.Dispose()
}

function Draw-Node($g, $cx, $cy, $size, $rim, $iconColor) {
    $half = [int]($size/2)
    $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,20,26,46))
    $g.FillRectangle($b, [int]($cx-$half), [int]($cy-$half), $size, $size)
    $b.Dispose()
    $p = New-Object System.Drawing.Pen($rim, 3)
    $g.DrawRectangle($p, [int]($cx-$half), [int]($cy-$half), $size, $size)
    $p.Dispose()
    $b2 = New-Object System.Drawing.SolidBrush($iconColor)
    $g.FillRectangle($b2, [int]($cx-$half+8), [int]($cy-$half+8), $size-16, $size-16)
    $b2.Dispose()
}

function Draw-Link($g, $x0, $y0, $x1, $y1, $color, $width) {
    $p = New-Object System.Drawing.Pen($color, [single]$width)
    $g.DrawLine($p, [single]$x0, [single]$y0, [single]$x1, [single]$y1)
    $p.Dispose()
}

function Draw-Title($g, $title, $subtitle) {
    $sf = New-Object System.Drawing.StringFormat; $sf.Alignment='Center'
    $f1 = New-Object System.Drawing.Font('Segoe UI', 58, [System.Drawing.FontStyle]::Bold)
    $b1 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,255,231,195))
    $g.DrawString($title, $f1, $b1, 960, 36, $sf)
    $f2 = New-Object System.Drawing.Font('Segoe UI', 24)
    $b2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230,160,170,200))
    $g.DrawString($subtitle, $f2, $b2, 960, 122, $sf)
    $f1.Dispose(); $b1.Dispose(); $f2.Dispose(); $b2.Dispose(); $sf.Dispose()
}

# ============ PREVIEW 1: solar system overview ============
$bmp = New-Object System.Drawing.Bitmap(1920,1080)
$g = New-StarSky $bmp

$sunX = 960; $sunY = 600
Draw-Disc $g $sunX $sunY 150 ([System.Drawing.Color]::FromArgb(20,255,176,64))
Draw-Disc $g $sunX $sunY 110 ([System.Drawing.Color]::FromArgb(34,255,192,80))
Draw-Disc $g $sunX $sunY 80 ([System.Drawing.Color]::FromArgb(255,255,160,48))
Draw-Disc $g ($sunX-20) ($sunY-20) 36 ([System.Drawing.Color]::FromArgb(120,255,224,160))

$ringRadii = @(190, 290)
$ringCounts = @(6, 12)
for ($r=0; $r -lt 2; $r++) {
    $rad = $ringRadii[$r]; $cnt = $ringCounts[$r]
    Draw-Orbit $g $sunX $sunY $rad ([System.Drawing.Color]::FromArgb(50,70,90,140)) 2
    for ($j=0; $j -lt $cnt; $j++) {
        $ang = 0.35*($r+1) + 2*[Math]::PI*$j/$cnt
        $nx = $sunX + $rad*[Math]::Cos($ang); $ny = $sunY + $rad*[Math]::Sin($ang)
        if ($j -lt ($cnt-2)) { $rim = [System.Drawing.Color]::FromArgb(255,87,178,87); $ic=[System.Drawing.Color]::FromArgb(255,120,200,120) }
        else { $rim = [System.Drawing.Color]::FromArgb(255,231,195,106); $ic=[System.Drawing.Color]::FromArgb(255,200,170,90) }
        Draw-Node $g $nx $ny 34 $rim $ic
    }
}

$planetDefs = @(
    @{R=520; A=-1.2; CR=74;  CG=144; CB=217; N='FLUIDS'},
    @{R=660; A=0.9;  CR=217; CG=131; CB=74;  N='LOGISTICS'},
    @{R=800; A=2.6;  CR=106; CG=176; CB=74;  N='CONTRAPTIONS'}
)
foreach ($pd in $planetDefs) {
    $pc = [System.Drawing.Color]::FromArgb(255,$pd.CR,$pd.CG,$pd.CB)
    Draw-Orbit $g $sunX $sunY $pd.R ([System.Drawing.Color]::FromArgb(160,$pd.CR,$pd.CG,$pd.CB)) 3
    $px = $sunX + $pd.R*[Math]::Cos($pd.A); $py = $sunY + $pd.R*[Math]::Sin($pd.A)
    Draw-Disc $g $px $py 46 ([System.Drawing.Color]::FromArgb(24,$pd.CR,$pd.CG,$pd.CB))
    Draw-Disc $g $px $py 34 ([System.Drawing.Color]::FromArgb(255,[int]($pd.CR*0.45),[int]($pd.CG*0.45),[int]($pd.CB*0.45)))
    Draw-Disc $g ($px-6) ($py-6) 28 $pc
    Draw-Disc $g ($px-12) ($py-12) 10 ([System.Drawing.Color]::FromArgb(100,255,255,255))
    for ($j=0; $j -lt 4; $j++) {
        $ang = 2*[Math]::PI*$j/4 + 0.4
        $nx = $px + 78*[Math]::Cos($ang); $ny = $py + 78*[Math]::Sin($ang)
        Draw-Link $g $px $py $nx $ny ([System.Drawing.Color]::FromArgb(120,180,190,220)) 2
        Draw-Node $g $nx $ny 26 ([System.Drawing.Color]::FromArgb(255,90,95,102)) ([System.Drawing.Color]::FromArgb(255,140,145,160))
    }
    $sf = New-Object System.Drawing.StringFormat; $sf.Alignment='Center'
    $f = New-Object System.Drawing.Font('Segoe UI', 14, [System.Drawing.FontStyle]::Bold)
    $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220,232,236,253))
    $g.DrawString($pd.N, $f, $b, [single]$px, [single]($py+56), $sf)
    $f.Dispose(); $b.Dispose(); $sf.Dispose()
}

Draw-Title $g 'CREATE: CONSTELLATION' 'A skill tree that gates Create recipes - craft, earn EXP, unlock'

$bmp.Save('D:\OpenCode\Mod\promo\preview1_system.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output 'preview1 done'

# ============ PREVIEW 2: economy flow ============
$bmp2 = New-Object System.Drawing.Bitmap(1920,1080)
$g2 = New-StarSky $bmp2

$sf2 = New-Object System.Drawing.StringFormat; $sf2.Alignment='Center'

# flow: craft -> EXP -> points -> unlock
$steps = @(
    @{X=280;  T1='1. CRAFT';      T2='Use Create machines';    C=[System.Drawing.Color]::FromArgb(255,74,144,217)},
    @{X=720;  T1='2. EARN EXP';   T2='Gated items give EXP';   C=[System.Drawing.Color]::FromArgb(255,106,176,74)},
    @{X=1160; T1='3. GET POINTS'; T2='100 EXP = 1 point';      C=[System.Drawing.Color]::FromArgb(255,217,193,74)},
    @{X=1600; T1='4. UNLOCK';     T2='Recipes work again';     C=[System.Drawing.Color]::FromArgb(255,231,123,106)}
)
$cy2 = 560
foreach ($st in $steps) {
    Draw-Disc $g2 $st.X $cy2 120 ([System.Drawing.Color]::FromArgb(26,$($st.C.R),$($st.C.G),$($st.C.B)))
    Draw-Disc $g2 $st.X $cy2 96 ([System.Drawing.Color]::FromArgb(255,20,26,46))
    Draw-Orbit $g2 $st.X $cy2 96 $st.C 4
    $f = New-Object System.Drawing.Font('Segoe UI', 26, [System.Drawing.FontStyle]::Bold)
    $b = New-Object System.Drawing.SolidBrush($st.C)
    $g2.DrawString($st.T1, $f, $b, [single]$st.X, [single]($cy2-16), $sf2)
    $f2b = New-Object System.Drawing.Font('Segoe UI', 15)
    $b2b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220,180,190,215))
    $g2.DrawString($st.T2, $f2b, $b2b, [single]$st.X, [single]($cy2+22), $sf2)
    $f.Dispose(); $b.Dispose(); $f2b.Dispose(); $b2b.Dispose()
}
# arrows between steps
$pa = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(200,231,195,106), 5)
$pa.EndCap = 'Arrow'
for ($i=0; $i -lt 3; $i++) {
    $x0 = $steps[$i].X + 130; $x1 = $steps[$i+1].X - 130
    $g2.DrawLine($pa, [single]$x0, [single]$cy2, [single]$x1, [single]$cy2)
}
$pa.Dispose()

# bottom info chips
$chips = @('Mixer / Press / Saw / Crusher gated', 'Vanilla crafting gated too', 'Locked items never destroyed', 'Datapack configurable')
$chipW = 420; $chipX = 960 - ($chips.Count*$chipW + ($chips.Count-1)*24)/2
for ($i=0; $i -lt $chips.Count; $i++) {
    $x = $chipX + $i*($chipW+24)
    $b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200,7,11,24))
    $g2.FillRectangle($b, [single]$x, 800, $chipW, 52)
    $b.Dispose()
    $p = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(120,70,90,140), 1)
    $g2.DrawRectangle($p, [single]$x, 800, $chipW, 52)
    $p.Dispose()
    $fc = New-Object System.Drawing.Font('Segoe UI', 16)
    $bc = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235,200,210,230))
    $g2.DrawString($chips[$i], $fc, $bc, [single]($x + $chipW/2), 814, $sf2)
    $fc.Dispose(); $bc.Dispose()
}

Draw-Title $g2 'HOW PROGRESSION WORKS' 'Craft with Create to level up the tree'

$bmp2.Save('D:\OpenCode\Mod\promo\preview2_flow.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g2.Dispose(); $bmp2.Dispose()
Write-Output 'preview2 done'

# ============ PREVIEW 3: focus view on a planet ============
$bmp3 = New-Object System.Drawing.Bitmap(1920,1080)
$g3 = New-StarSky $bmp3

# big planet on the left with its local tree
$plX = 620; $plY = 560
$plC = [System.Drawing.Color]::FromArgb(255,74,144,217)
Draw-Orbit $g3 1700 560 900 ([System.Drawing.Color]::FromArgb(140,74,144,217)) 4   # fragment of the orbit around off-screen sun
Draw-Disc $g3 $plX $plY 110 ([System.Drawing.Color]::FromArgb(26,74,144,217))
Draw-Disc $g3 $plX $plY 88 ([System.Drawing.Color]::FromArgb(255,33,65,98))
Draw-Disc $g3 ($plX-14) ($plY-14) 72 $plC
Draw-Disc $g3 ($plX-34) ($plY-34) 22 ([System.Drawing.Color]::FromArgb(100,255,255,255))
Draw-Orbit $g3 $plX $plY 190 ([System.Drawing.Color]::FromArgb(70,70,90,140)) 2
Draw-Orbit $g3 $plX $plY 280 ([System.Drawing.Color]::FromArgb(70,70,90,140)) 2

# planet name + focus ring
Draw-Orbit $g3 $plX $plY 122 ([System.Drawing.Color]::FromArgb(200,255,210,78)) 3
$sf3 = New-Object System.Drawing.StringFormat; $sf3.Alignment='Center'
$fn = New-Object System.Drawing.Font('Segoe UI', 24, [System.Drawing.FontStyle]::Bold)
$bn = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,233,169,160))
$g3.DrawString('FLUIDS', $fn, $bn, [single]$plX, [single]($plY+130), $sf3)
$fn.Dispose(); $bn.Dispose()

# nodes on two local rings, links between them
for ($j=0; $j -lt 5; $j++) {
    $ang = 0.5 + 2*[Math]::PI*$j/5
    $nx = $plX + 190*[Math]::Cos($ang); $ny = $plY + 190*[Math]::Sin($ang)
    Draw-Link $g3 $plX $plY $nx $ny ([System.Drawing.Color]::FromArgb(150,231,195,106)) 2
    if ($j -lt 3) { $rim=[System.Drawing.Color]::FromArgb(255,87,178,87); $ic=[System.Drawing.Color]::FromArgb(255,110,190,110) }
    else { $rim=[System.Drawing.Color]::FromArgb(255,90,95,102); $ic=[System.Drawing.Color]::FromArgb(255,130,135,150) }
    Draw-Node $g3 $nx $ny 36 $rim $ic
}
$prevRing = @()
for ($j=0; $j -lt 5; $j++) {
    $ang = 0.5 + 2*[Math]::PI*$j/5
    $prevRing += ,@($plX + 190*[Math]::Cos($ang), $plY + 190*[Math]::Sin($ang))
}
for ($j=0; $j -lt 8; $j++) {
    $ang = 0.35*2 + 2*[Math]::PI*$j/8
    $nx = $plX + 280*[Math]::Cos($ang); $ny = $plY + 280*[Math]::Sin($ang)
    # link to two nearest inner nodes
    $sorted = $prevRing | Sort-Object { [Math]::Abs($_[0]-$nx) + [Math]::Abs($_[1]-$ny) }
    Draw-Link $g3 $sorted[0][0] $sorted[0][1] $nx $ny ([System.Drawing.Color]::FromArgb(110,64,74,102)) 2
    Draw-Link $g3 $sorted[1][0] $sorted[1][1] $nx $ny ([System.Drawing.Color]::FromArgb(110,64,74,102)) 2
    Draw-Node $g3 $nx $ny 32 ([System.Drawing.Color]::FromArgb(255,138,74,74)) ([System.Drawing.Color]::FromArgb(255,150,110,110))
}

# HUD bars like in-game
$bh = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(190,7,11,24))
$g3.FillRectangle($bh, 0, 0, 1920, 64)
$g3.FillRectangle($bh, 0, 1030, 1920, 50)
$bh.Dispose()
$fh = New-Object System.Drawing.Font('Segoe UI', 22, [System.Drawing.FontStyle]::Bold)
$bhh = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,255,224,112))
$g3.DrawString('Skill Points: 3', $fh, $bhh, 24, 18)
$fhh = New-Object System.Drawing.Font('Segoe UI', 15)
$bhf = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200,138,144,168))
$g3.DrawString('Click node: unlock  -  Click planet: follow  -  Drag/Wheel: free cam  -  C: back to Sun', $fhh, $bhf, 340, 1042)
# exp bar
$g3.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,0,0,0))), 1300, 22, 402, 18)
$g3.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,20,26,46))), 1301, 23, 400, 16)
$g3.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,127,231,163))), 1301, 23, 260, 16)
$fh.Dispose(); $bhh.Dispose(); $fhh.Dispose(); $bhf.Dispose()

Draw-Title $g3 'FOCUS & FOLLOW' 'Click a planet - the camera flies to it and follows its orbit'

$bmp3.Save('D:\OpenCode\Mod\promo\preview3_focus.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g3.Dispose(); $bmp3.Dispose()
Write-Output 'preview3 done'
