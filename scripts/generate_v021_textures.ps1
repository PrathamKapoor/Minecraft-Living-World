Add-Type -AssemblyName System.Drawing

# 1. SHARK TEXTURE (64x64)
$bmp = New-Object System.Drawing.Bitmap 64, 64
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Transparent)

# Palette
$cSlateDark = [System.Drawing.Color]::FromArgb(255, 59, 68, 80)
$cSlateMid = [System.Drawing.Color]::FromArgb(255, 76, 88, 102)
$cSlateLight = [System.Drawing.Color]::FromArgb(255, 98, 112, 128)
$cBellyWhite = [System.Drawing.Color]::FromArgb(255, 232, 236, 240)
$cBellyShadow = [System.Drawing.Color]::FromArgb(255, 205, 212, 220)
$cGillDark = [System.Drawing.Color]::FromArgb(255, 30, 36, 44)
$cEyeBlack = [System.Drawing.Color]::FromArgb(255, 15, 15, 20)
$cEyeHighlight = [System.Drawing.Color]::FromArgb(255, 220, 230, 245)
$cMouthDark = [System.Drawing.Color]::FromArgb(255, 90, 20, 20)
$cTeethWhite = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)

# Fill Body region (0..32, 0..24)
for ($x = 0; $x -lt 32; $x++) {
    for ($y = 0; $y -lt 24; $y++) {
        if ($y -lt 8) {
            $col = if ($x % 2 -eq 0) { $cSlateMid } else { $cSlateDark }
            $bmp.SetPixel($x, $y, $col)
        } elseif ($y -lt 16) {
            $col = if ($y -gt 12) { $cBellyShadow } else { $cSlateLight }
            $bmp.SetPixel($x, $y, $col)
        } else {
            $col = if ($x % 2 -eq 0) { $cBellyWhite } else { $cBellyShadow }
            $bmp.SetPixel($x, $y, $col)
        }
    }
}

# 5 Gill slits on body flanks
for ($i = 0; $i -lt 5; $i++) {
    $gx = 18 + ($i * 2)
    if ($gx -lt 32) {
        $bmp.SetPixel($gx, 9, $cGillDark)
        $bmp.SetPixel($gx, 10, $cGillDark)
        $bmp.SetPixel($gx, 11, $cGillDark)
    }
}

# Fill Head region (0..24, 24..37)
for ($x = 0; $x -lt 24; $x++) {
    for ($y = 24; $y -lt 37; $y++) {
        $col = if ($y -gt 32) { $cBellyWhite } else { $cSlateMid }
        $bmp.SetPixel($x, $y, $col)
    }
}
# Predatory Black Eyes on Head
$bmp.SetPixel(4, 28, $cEyeBlack)
$bmp.SetPixel(4, 27, $cEyeHighlight)
$bmp.SetPixel(18, 28, $cEyeBlack)
$bmp.SetPixel(18, 27, $cEyeHighlight)

# Snout wedge & sharp apex tip (0..24, 37..43)
for ($x = 0; $x -lt 24; $x++) {
    for ($y = 37; $y -lt 43; $y++) {
        $col = if ($y -gt 39) { $cBellyWhite } else { $cSlateDark }
        $bmp.SetPixel($x, $y, $col)
    }
}

# Articulated Jaw with Serrated Teeth (28..48, 24..32)
for ($x = 28; $x -lt 48; $x++) {
    for ($y = 24; $y -lt 32; $y++) {
        if ($y -eq 24 -or $y -eq 31) {
            $col = if ($x % 2 -eq 0) { $cTeethWhite } else { $cMouthDark }
            $bmp.SetPixel($x, $y, $col)
        } else {
            $bmp.SetPixel($x, $y, $cBellyWhite)
        }
    }
}

# Dorsal Fin (32..46, 0..8)
for ($x = 32; $x -lt 46; $x++) {
    for ($y = 0; $y -lt 8; $y++) {
        $col = if ($x -gt 40) { $cSlateDark } else { $cSlateMid }
        $bmp.SetPixel($x, $y, $col)
    }
}

# Pectoral Fins (40..56, 13..20)
for ($x = 40; $x -lt 56; $x++) {
    for ($y = 13; $y -lt 20; $y++) {
        $col = if ($y -gt 16) { $cBellyWhite } else { $cSlateLight }
        $bmp.SetPixel($x, $y, $col)
    }
}

# Tail peduncle (0..20, 43..56)
for ($x = 0; $x -lt 20; $x++) {
    for ($y = 43; $y -lt 56; $y++) {
        $col = if ($y -gt 49) { $cBellyWhite } else { $cSlateMid }
        $bmp.SetPixel($x, $y, $col)
    }
}

# Vertical Tail Fin Lobes (26..50, 32..42)
for ($x = 26; $x -lt 50; $x++) {
    for ($y = 32; $y -lt 42; $y++) {
        $col = if ($x -gt 38) { $cSlateDark } else { $cSlateLight }
        $bmp.SetPixel($x, $y, $col)
    }
}

$g.Dispose()
$bmp.Save('src\main\resources\assets\livingworld\textures\entity\shark\shark.png', [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Shark texture generated successfully."

# 2. MUTANT ZOMBIE COMMANDER TEXTURE (64x64)
$bmp2 = New-Object System.Drawing.Bitmap 64, 64
$g2 = [System.Drawing.Graphics]::FromImage($bmp2)
$g2.Clear([System.Drawing.Color]::Transparent)

$cSkinDark = [System.Drawing.Color]::FromArgb(255, 45, 68, 38)
$cSkinMid = [System.Drawing.Color]::FromArgb(255, 74, 107, 61)
$cSkinPale = [System.Drawing.Color]::FromArgb(255, 96, 135, 78)
$cEyeGlow = [System.Drawing.Color]::FromArgb(255, 230, 40, 40)
$cEyeIris = [System.Drawing.Color]::FromArgb(255, 255, 200, 30)
$cBoneWhite = [System.Drawing.Color]::FromArgb(255, 220, 215, 195)
$cBloodDark = [System.Drawing.Color]::FromArgb(255, 90, 15, 15)
$cArmorIron = [System.Drawing.Color]::FromArgb(255, 140, 145, 150)
$cArmorRust = [System.Drawing.Color]::FromArgb(255, 130, 75, 45)
$cTunicDark = [System.Drawing.Color]::FromArgb(255, 35, 40, 50)
$cTunicPurple = [System.Drawing.Color]::FromArgb(255, 55, 30, 65)

# Center Head (0..32, 0..16)
for ($x = 0; $x -lt 32; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        $col = if (($x + $y) % 3 -eq 0) { $cSkinPale } else { $cSkinMid }
        $bmp2.SetPixel($x, $y, $col)
    }
}
# Exposed Bone on Forehead
$bmp2.SetPixel(10, 8, $cBoneWhite)
$bmp2.SetPixel(11, 8, $cBoneWhite)
$bmp2.SetPixel(10, 9, $cBoneWhite)
$bmp2.SetPixel(11, 9, $cBloodDark)

# Piercing Glowing Red Eyes on Center Head
$bmp2.SetPixel(9, 11, $cEyeGlow)
$bmp2.SetPixel(10, 11, $cEyeIris)
$bmp2.SetPixel(13, 11, $cEyeIris)
$bmp2.SetPixel(14, 11, $cEyeGlow)

# Fangs / Snarl
$bmp2.SetPixel(10, 14, $cBoneWhite)
$bmp2.SetPixel(11, 14, $cBloodDark)
$bmp2.SetPixel(12, 14, $cBloodDark)
$bmp2.SetPixel(13, 14, $cBoneWhite)

# Left & Right Mutated Heads (32..64, 0..16)
for ($x = 32; $x -lt 64; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        $col = if ($x % 2 -eq 0) { $cSkinDark } else { $cSkinMid }
        $bmp2.SetPixel($x, $y, $col)
    }
}
# Left Head Eyes
$bmp2.SetPixel(42, 9, $cEyeGlow)
$bmp2.SetPixel(45, 9, $cEyeGlow)
# Right Head Eyes
$bmp2.SetPixel(53, 9, $cEyeGlow)
$bmp2.SetPixel(56, 9, $cEyeGlow)

# Mutated Torso (16..40, 16..32)
for ($x = 16; $x -lt 40; $x++) {
    for ($y = 16; $y -lt 32; $y++) {
        if ($y -lt 22) {
            $col = if ($x % 4 -eq 0) { $cArmorRust } else { $cArmorIron }
            $bmp2.SetPixel($x, $y, $col)
        } else {
            $col = if (($x + $y) % 2 -eq 0) { $cTunicDark } else { $cTunicPurple }
            $bmp2.SetPixel($x, $y, $col)
        }
    }
}
# Exposed Ribs
$bmp2.SetPixel(24, 25, $cBoneWhite)
$bmp2.SetPixel(27, 25, $cBoneWhite)
$bmp2.SetPixel(25, 27, $cBoneWhite)
$bmp2.SetPixel(26, 27, $cBoneWhite)

# Arms (40..56, 16..32)
for ($x = 40; $x -lt 56; $x++) {
    for ($y = 16; $y -lt 32; $y++) {
        $col = if ($y -gt 28) { $cSkinPale } elseif ($y -lt 20) { $cArmorIron } else { $cSkinMid }
        $bmp2.SetPixel($x, $y, $col)
    }
}

# Legs (0..16, 16..32)
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 16; $y -lt 32; $y++) {
        $col = if ($y -gt 26) { $cArmorIron } else { $cTunicDark }
        $bmp2.SetPixel($x, $y, $col)
    }
}

$g2.Dispose()
$bmp2.Save('src\main\resources\assets\livingworld\textures\entity\zombie\mutant_commander.png', [System.Drawing.Imaging.ImageFormat]::Png)
$bmp2.Dispose()
Write-Host "Mutant Commander texture generated successfully."
