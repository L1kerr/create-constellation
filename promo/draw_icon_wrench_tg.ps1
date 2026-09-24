Add-Type -AssemblyName System.Drawing

# 32x32 pixel grid -> 512x512 icon for Telegram (round crop safe: subject centered)
$G = 32; $SC = 16
$cell = New-Object 'System.Drawing.Color[,]' $G,$G

function PutPx($x,$y,$c) { if ($x -ge 0 -and $x -lt $G -and $y -ge 0 -and $y -lt $G) { $cell[$x,$y] = $c } }

# ---------- background: dark space with vignette ----------
for ($y=0; $y -lt $G; $y++) {
    for ($x=0; $x -lt $G; $x++) {
        $dx = ($x - 15.5)/18.0; $dy = ($y - 15.5)/18.0
        $v = [Math]::Sqrt($dx*$dx + $dy*$dy)
        $k = 1.0 - 0.45*[Math]::Min(1.0, $v)
        $c = [System.Drawing.Color]::FromArgb([int](20*$k)+4, [int](26*$k)+5, [int](56*$k)+10)
        PutPx $x $y $c
    }
}

# stars
$rnd = New-Object System.Random(99)
$stars = @(@(3,4),@(28,5),@(4,27),@(27,26),@(15,2),@(2,15),@(29,16),@(9,29),@(22,29),@(6,9),@(26,21),@(19,5))
foreach ($s in $stars) {
    $a = $rnd.Next(110,235)
    PutPx $s[0] $s[1] ([System.Drawing.Color]::FromArgb($a,205,218,255))
}

# ---------- golden glow behind the wrench ----------
$gcx = 15.5; $gcy = 15.5
for ($y=0; $y -lt $G; $y++) {
    for ($x=0; $x -lt $G; $x++) {
        $d = [Math]::Sqrt(($x-$gcx)*($x-$gcx) + ($y-$gcy)*($y-$gcy))
        if ($d -lt 11) {
            $a = (1.0 - $d/11.0) * 0.16
            $bg = $cell[$x,$y]
            $c = [System.Drawing.Color]::FromArgb(
                [int](235*$a + $bg.R*(1-$a)),
                [int](195*$a + $bg.G*(1-$a)),
                [int](100*$a + $bg.B*(1-$a)))
            PutPx $x $y $c
        }
    }
}

# ---------- wrench ----------
# handle: thick diagonal band from bottom-left (8,25) to head (19,12)
$h0x = 8.0; $h0y = 25.0; $h1x = 19.0; $h1y = 12.0
$hdx = $h1x-$h0x; $hdy = $h1y-$h0y; $hlen2 = $hdx*$hdx + $hdy*$hdy

# head: ring centered at (22,9), mouth facing top-right
$hcX = 22.0; $hcY = 9.0; $rin = 3.0; $rout = 6.3
$mouthDir = [Math]::Atan2(-1.0, 1.0)   # up-right, -45deg
$mouthHalf = 0.62                      # ~35deg half-angle

for ($y=0; $y -lt $G; $y++) {
    for ($x=0; $x -lt $G; $x++) {
        $px = $x + 0.5; $py = $y + 0.5
        $draw = $false; $col = $null

        # --- handle band (distance to segment)
        $t = (($px-$h0x)*$hdx + ($py-$h0y)*$hdy) / $hlen2
        if ($t -ge 0.0 -and $t -le 1.0) {
            $qx = $h0x + $hdx*$t; $qy = $h0y + $hdy*$t
            $dd = [Math]::Sqrt(($px-$qx)*($px-$qx) + ($py-$qy)*($py-$qy))
            if ($dd -le 2.05) {
                $draw = $true
                # shading: upper-left lit, lower-right dark
                if ($dd -lt 0.85) { $col = [System.Drawing.Color]::FromArgb(255,146,110,66) }      # bright core line
                elseif (($px-$qx)*(-0.7) + ($py-$qy)*(-0.7) -gt 0.55) { $col = [System.Drawing.Color]::FromArgb(255,101,64,44) } # lit edge
                else { $col = [System.Drawing.Color]::FromArgb(255,62,36,30) }                    # dark edge
                # brass grip bands near the middle
                if ($t -gt 0.42 -and $t -lt 0.58) {
                    if ($dd -lt 0.85) { $col = [System.Drawing.Color]::FromArgb(255,240,205,110) }
                    else { $col = [System.Drawing.Color]::FromArgb(255,185,145,55) }
                }
                # pommel hole at the very end
                $pe = [Math]::Sqrt(($px-8.8)*($px-8.8) + ($py-24.2)*($py-24.2))
                if ($pe -lt 0.85) { $draw = $false }
            }
        }

        # --- head ring
        $dhx = $px - $hcX; $dhy = $py - $hcY
        $dh = [Math]::Sqrt($dhx*$dhx + $dhy*$dhy)
        if ($dh -le $rout -and $dh -ge $rin - 0.4) {
            $ang = [Math]::Atan2($dhy, $dhx)
            $diff = [Math]::Abs((($ang - $mouthDir + [Math]::PI*3) % ([Math]::PI*2)) - [Math]::PI)
            $inMouth = ($diff -lt $mouthHalf) -and ($dh -gt $rin - 1.6)
            if (-not $inMouth) {
                $draw = $true
                # lighting on the ring
                $lit = ($dhx*(-0.7) + $dhy*(-0.7)) / [Math]::Max(0.001,$dh)
                if ($dh -ge $rout - 0.9) { $col = [System.Drawing.Color]::FromArgb(255,110,72,26) }        # outline
                elseif ($lit -gt 0.45) { $col = [System.Drawing.Color]::FromArgb(255,247,208,121) }        # lit face
                elseif ($lit -gt -0.1) { $col = [System.Drawing.Color]::FromArgb(255,224,160,64) }        # mid
                else { $col = [System.Drawing.Color]::FromArgb(255,150,100,36) }                          # shadow side
                # jaw tips: brass accent
                if ($diff -lt $mouthHalf + 0.22 -and $diff -gt $mouthHalf - 0.12 -and $dh -gt $rin) {
                    $col = [System.Drawing.Color]::FromArgb(255,225,185,85)
                }
            }
        }

        if ($draw -and $col -ne $null) { PutPx $x $y $col }
    }
}

# ---------- render ----------
$bmp = New-Object System.Drawing.Bitmap(($G*$SC), ($G*$SC))
$g2 = [System.Drawing.Graphics]::FromImage($bmp)
for ($y=0; $y -lt $G; $y++) {
    for ($x=0; $x -lt $G; $x++) {
        $b = New-Object System.Drawing.SolidBrush($cell[$x,$y])
        $g2.FillRectangle($b, $x*$SC, $y*$SC, $SC, $SC)
        $b.Dispose()
    }
}
$g2.Dispose()
$bmp.Save('D:\OpenCode\Mod\promo\icon_wrench_tg.png', [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output 'tg wrench icon done'
