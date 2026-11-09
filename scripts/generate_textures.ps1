Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$textureBase = "$projectRoot\src\main\resources\assets\livingworld\textures\entity"

function New-Texture {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height,
        [scriptblock]$Painter
    )
    $dir = Split-Path -Parent $Path
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    $bmp = [System.Drawing.Bitmap]::new($Width, $Height)
    & $Painter $bmp
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Created texture: $Path ($Width x $Height)" -ForegroundColor Green
}

# 1. Shark texture (64x64)
New-Texture -Path "$textureBase\shark\shark.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    
    # Body dorsal slate blue-gray
    $slateBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 75, 85, 99))
    $bellyBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 230, 235, 240))
    $darkBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 45, 55, 72))
    $redBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 180, 40, 40))
    $eyeBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 15, 20, 25))

    # Base dorsal fill
    $g.FillRectangle($slateBrush, 0, 0, 64, 40)
    # White belly
    $g.FillRectangle($bellyBrush, 0, 40, 64, 24)
    # Gills and fin accents
    $g.FillRectangle($darkBrush, 12, 10, 4, 18)
    $g.FillRectangle($darkBrush, 18, 10, 3, 16)
    $g.FillRectangle($darkBrush, 23, 10, 2, 14)
    # Eyes
    $bmp.SetPixel(6, 6, [System.Drawing.Color]::FromArgb(255, 20, 20, 20))
    $bmp.SetPixel(7, 6, [System.Drawing.Color]::FromArgb(255, 240, 240, 240))
    # Mouth & sharp teeth row
    $g.FillRectangle($redBrush, 2, 28, 16, 4)
    for ($x = 2; $x -lt 18; $x += 2) {
        $bmp.SetPixel($x, 29, [System.Drawing.Color]::FromArgb(255, 255, 255, 255))
    }
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

# 2. Gargoyle Swooper texture (64x64)
New-Texture -Path "$textureBase\swooper\gargoyle_swooper.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $stoneBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 60, 63, 65))
    $darkStoneBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 40, 42, 44))
    $wingBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 75, 70, 65))
    $goldBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 255, 215, 0))

    $g.FillRectangle($stoneBrush, 0, 0, 64, 64)
    $g.FillRectangle($darkStoneBrush, 8, 8, 48, 24)
    $g.FillRectangle($wingBrush, 0, 32, 64, 20)
    # Glowing golden gargoyle eyes
    $g.FillRectangle($goldBrush, 10, 12, 3, 2)
    $g.FillRectangle($goldBrush, 22, 12, 3, 2)
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

# 3. Zombie Cow texture (64x32)
New-Texture -Path "$textureBase\zombie\zombie_cow.png" -Width 64 -Height 32 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $hideBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 55, 40, 30))
    $rotBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 107, 54))
    $boneBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 220, 217, 205))
    $eyeBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 180, 220, 160))

    $g.FillRectangle($hideBrush, 0, 0, 64, 32)
    # Necrotic rot patches
    $g.FillRectangle($rotBrush, 4, 4, 20, 16)
    $g.FillRectangle($rotBrush, 36, 12, 18, 12)
    # Exposed rib bones
    $g.FillRectangle($boneBrush, 22, 18, 6, 8)
    $bmp.SetPixel(24, 20, [System.Drawing.Color]::FromArgb(255, 40, 40, 40))
    $bmp.SetPixel(24, 22, [System.Drawing.Color]::FromArgb(255, 40, 40, 40))
    $bmp.SetPixel(24, 24, [System.Drawing.Color]::FromArgb(255, 40, 40, 40))
    # Zombie milky eyes
    $g.FillRectangle($eyeBrush, 2, 4, 2, 2)
    $g.FillRectangle($eyeBrush, 8, 4, 2, 2)
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

# 4. Zombie Pig texture (64x32)
New-Texture -Path "$textureBase\zombie\zombie_pig.png" -Width 64 -Height 32 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $skinBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 180, 130, 125))
    $rotBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 91, 112, 67))
    $boneBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 224, 222, 195))

    $g.FillRectangle($skinBrush, 0, 0, 64, 32)
    $g.FillRectangle($rotBrush, 12, 0, 24, 20)
    $g.FillRectangle($boneBrush, 2, 2, 5, 6) # Exposed skull bone
    # Red necrotic eye
    $bmp.SetPixel(4, 4, [System.Drawing.Color]::FromArgb(255, 180, 30, 30))
    $bmp.SetPixel(12, 4, [System.Drawing.Color]::FromArgb(255, 140, 180, 120))
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

# 5. Zombie Sheep texture (64x32)
New-Texture -Path "$textureBase\zombie\zombie_sheep.png" -Width 64 -Height 32 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $woolBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 75, 90, 64))
    $rotBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 55, 65, 45))
    $boneBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 216, 212, 200))

    $g.FillRectangle($woolBrush, 0, 0, 64, 32)
    $g.FillRectangle($rotBrush, 16, 8, 30, 18)
    $g.FillRectangle($boneBrush, 32, 12, 8, 10)
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

# 6. End Void Stalker texture (64x32)
New-Texture -Path "$textureBase\end\end_void_stalker.png" -Width 64 -Height 32 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $voidBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 16, 8, 28))
    $darkChitin = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 26, 15, 46))
    $runeBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 168, 85, 247))
    $eyeBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 216, 180, 254))

    $g.FillRectangle($voidBrush, 0, 0, 64, 32)
    $g.FillRectangle($darkChitin, 8, 4, 32, 24)
    # Glowing void eyes and runes
    $g.FillRectangle($eyeBrush, 2, 6, 3, 2)
    $g.FillRectangle($eyeBrush, 8, 6, 3, 2)
    $g.FillRectangle($runeBrush, 14, 10, 2, 8)
    $g.FillRectangle($runeBrush, 20, 14, 2, 6)
    $g.Dispose()
}

# 7. Mutant Zombie Commander texture (64x64, 3 heads & mutated frame)
New-Texture -Path "$textureBase\zombie\mutant_commander.png" -Width 64 -Height 64 -Painter {
    param($bmp)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $zombieGreen = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 59, 94, 64))
    $darkRotted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 39, 66, 43))
    $mutatedPurple = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 74, 37, 69))
    $boneWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 215, 210, 195))
    $crimsonBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 214, 40, 40))

    # Base mutated torso & limbs
    $g.FillRectangle($zombieGreen, 0, 0, 64, 64)
    $g.FillRectangle($darkRotted, 16, 16, 24, 32)
    $g.FillRectangle($mutatedPurple, 20, 20, 16, 24)

    # Central Primary Head (x=0..32, y=0..16)
    $g.FillRectangle($crimsonBrush, 8, 4, 3, 2)
    $g.FillRectangle($crimsonBrush, 14, 4, 3, 2)
    $g.FillRectangle($boneWhite, 9, 10, 7, 2) # Exposed rotted teeth

    # Left Mutated Head (x=32..48, y=0..16)
    $g.FillRectangle($darkRotted, 32, 0, 16, 16)
    $bmp.SetPixel(36, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73)) # Corrupted yellow eye
    $bmp.SetPixel(42, 4, [System.Drawing.Color]::FromArgb(255, 252, 191, 73))

    # Right Mutated Head (x=48..64, y=0..16)
    $g.FillRectangle($darkRotted, 48, 0, 16, 16)
    $bmp.SetPixel(52, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15)) # Hollow black socket
    $bmp.SetPixel(58, 4, [System.Drawing.Color]::FromArgb(255, 15, 15, 15))

    # Spikes, bone spurs, rotted sinew
    for ($y = 20; $y -lt 50; $y += 6) {
        $bmp.SetPixel(18, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
        $bmp.SetPixel(38, $y, [System.Drawing.Color]::FromArgb(255, 230, 225, 210))
    }

    $g.Dispose()
}

Write-Host "All custom entity textures generated successfully!" -ForegroundColor Cyan
