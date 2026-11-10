Add-Type -AssemblyName System.Drawing

$guardDir = "src/main/resources/assets/livingworld/textures/entity/guard"
if (!(Test-Path $guardDir)) {
    New-Item -ItemType Directory -Force -Path $guardDir | Out-Null
}

function Create-Guard-Texture {
    param (
        [string]$Path,
        [bool]$IsArcher
    )

    $bmp = New-Object System.Drawing.Bitmap 64, 64
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    # Core Palettes
    $cSkin = [System.Drawing.Color]::FromArgb(255, 189, 139, 114)       # Villager tan
    $cSkinShadow = [System.Drawing.Color]::FromArgb(255, 161, 104, 78) # Darker skin shade
    $cNose = [System.Drawing.Color]::FromArgb(255, 175, 115, 88)       # 3D Nose tone
    $cBrow = [System.Drawing.Color]::FromArgb(255, 76, 53, 36)          # Villager dark unibrow
    $cEyeGreen = [System.Drawing.Color]::FromArgb(255, 34, 139, 34)    # Iconic emerald eyes
    $cEyeWhite = [System.Drawing.Color]::FromArgb(255, 240, 240, 240)  # Sclera
    $cLeather = [System.Drawing.Color]::FromArgb(255, 110, 75, 45)      # Sturdy leather
    $cLeatherDark = [System.Drawing.Color]::FromArgb(255, 78, 52, 30)
    $cSteel = [System.Drawing.Color]::FromArgb(255, 170, 175, 185)      # Chainmail steel
    $cSteelDark = [System.Drawing.Color]::FromArgb(255, 120, 125, 135)
    $cGold = [System.Drawing.Color]::FromArgb(255, 218, 165, 32)        # Belt buckle gold

    # Guard Blue vs Archer Green
    if ($IsArcher) {
        $cTunic = [System.Drawing.Color]::FromArgb(255, 59, 83, 51)       # Forest olive green
        $cTunicDark = [System.Drawing.Color]::FromArgb(255, 42, 60, 36)
        $cTunicTrim = [System.Drawing.Color]::FromArgb(255, 140, 110, 60)
    } else {
        $cTunic = [System.Drawing.Color]::FromArgb(255, 42, 74, 122)      # Royal city guard cobalt blue
        $cTunicDark = [System.Drawing.Color]::FromArgb(255, 28, 50, 85)
        $cTunicTrim = [System.Drawing.Color]::FromArgb(255, 180, 185, 195)
    }

    # 1. HEAD (0..32, 0..16)
    for ($x = 0; $x -lt 32; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $shade = if (($x + $y) % 2 -eq 0) { $cSkin } else { $cSkinShadow }
            $bmp.SetPixel($x, $y, $shade)
        }
    }
    # Face details (Front: x=8..15, y=8..15)
    # Iconic Unibrow across row 9
    for ($x = 9; $x -le 14; $x++) {
        $bmp.SetPixel($x, 9, $cBrow)
    }
    # Emerald Villager Eyes
    $bmp.SetPixel(9, 10, $cEyeWhite)
    $bmp.SetPixel(10, 10, $cEyeGreen)
    $bmp.SetPixel(13, 10, $cEyeGreen)
    $bmp.SetPixel(14, 10, $cEyeWhite)
    # Cheek tones
    $bmp.SetPixel(9, 12, $cSkinShadow)
    $bmp.SetPixel(14, 12, $cSkinShadow)

    # 2. 3D NOSE (24..28, 0..5)
    for ($x = 24; $x -lt 28; $x++) {
        for ($y = 0; $y -lt 5; $y++) {
            $bmp.SetPixel($x, $y, $cNose)
        }
    }

    # 3. HAT / COIF LAYER (32..64, 0..16)
    for ($x = 32; $x -lt 64; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            if ($IsArcher) {
                # Archer leather headband / hood trim
                if ($y -ge 7 -and $y -le 9) {
                    $bmp.SetPixel($x, $y, $cLeather)
                }
            } else {
                # Guard steel chainmail coif
                if ($y -le 12 -and ($x + $y) % 2 -eq 0) {
                    $bmp.SetPixel($x, $y, $cSteelDark)
                } elseif ($y -le 12) {
                    $bmp.SetPixel($x, $y, $cSteel)
                }
            }
        }
    }

    # 4. BODY / TORSO (16..40, 16..32)
    for ($x = 16; $x -lt 40; $x++) {
        for ($y = 16; $y -lt 32; $y++) {
            $shade = if (($x + $y) % 2 -eq 0) { $cTunic } else { $cTunicDark }
            $bmp.SetPixel($x, $y, $shade)
        }
    }
    # Front Torso details (x=20..27, y=20..31)
    # Leather Belt with Buckle at y=28..29
    for ($x = 20; $x -le 27; $x++) {
        $bmp.SetPixel($x, 28, $cLeatherDark)
        $bmp.SetPixel($x, 29, $cLeather)
    }
    $bmp.SetPixel(23, 28, $cGold)
    $bmp.SetPixel(24, 28, $cGold)
    $bmp.SetPixel(23, 29, $cGold)
    $bmp.SetPixel(24, 29, $cGold)

    if ($IsArcher) {
        # Diagonal Quiver Strap across chest
        $bmp.SetPixel(21, 20, $cLeather)
        $bmp.SetPixel(22, 21, $cLeather)
        $bmp.SetPixel(23, 22, $cLeather)
        $bmp.SetPixel(24, 23, $cLeather)
        $bmp.SetPixel(25, 24, $cLeather)
        $bmp.SetPixel(26, 25, $cLeather)
        $bmp.SetPixel(27, 26, $cLeather)
    } else {
        # Steel Gorget / Neckplate at y=20..21
        for ($x = 22; $x -le 25; $x++) {
            $bmp.SetPixel($x, 20, $cSteel)
            $bmp.SetPixel($x, 21, $cSteelDark)
        }
    }

    # 5. RIGHT ARM (40..56, 16..32)
    for ($x = 40; $x -lt 56; $x++) {
        for ($y = 16; $y -lt 32; $y++) {
            if ($y -ge 24) {
                # Vambrace / Bracer
                $bracerColor = if ($IsArcher) { $cLeather } else { $cSteel }
                $bmp.SetPixel($x, $y, $bracerColor)
            } else {
                $bmp.SetPixel($x, $y, $cTunic)
            }
        }
    }

    # 6. LEFT ARM (32..48, 48..64)
    for ($x = 32; $x -lt 48; $x++) {
        for ($y = 48; $y -lt 64; $y++) {
            if ($y -ge 56) {
                # Vambrace / Bracer
                $bracerColor = if ($IsArcher) { $cLeather } else { $cSteel }
                $bmp.SetPixel($x, $y, $bracerColor)
            } else {
                $bmp.SetPixel($x, $y, $cTunic)
            }
        }
    }

    # 7. RIGHT LEG (0..16, 16..32)
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 16; $y -lt 32; $y++) {
            if ($y -ge 26) {
                # Boots
                $bmp.SetPixel($x, $y, $cLeatherDark)
            } else {
                $bmp.SetPixel($x, $y, $cLeather)
            }
        }
    }

    # 8. LEFT LEG (16..32, 48..64)
    for ($x = 16; $x -lt 32; $x++) {
        for ($y = 48; $y -lt 64; $y++) {
            if ($y -ge 58) {
                # Boots
                $bmp.SetPixel($x, $y, $cLeatherDark)
            } else {
                $bmp.SetPixel($x, $y, $cLeather)
            }
        }
    }

    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $g.Dispose()
    Write-Host "Generated texture: $Path"
}

Create-Guard-Texture "$guardDir/village_guard.png" $false
Create-Guard-Texture "$guardDir/village_archer.png" $true
