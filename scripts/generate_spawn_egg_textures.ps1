Add-Type -AssemblyName System.Drawing

$itemsDir = "src/main/resources/assets/livingworld/items"
$modelsDir = "src/main/resources/assets/livingworld/models/item"
$texturesDir = "src/main/resources/assets/livingworld/textures/item"

New-Item -ItemType Directory -Force -Path $itemsDir | Out-Null
New-Item -ItemType Directory -Force -Path $modelsDir | Out-Null
New-Item -ItemType Directory -Force -Path $texturesDir | Out-Null

$eggs = @(
    @{
        id = "shark_spawn_egg"
        baseR = 44; baseG = 62; baseB = 80
        spotR = 26; spotG = 188; spotB = 156
    },
    @{
        id = "gargoyle_swooper_spawn_egg"
        baseR = 74; baseG = 78; baseB = 83
        spotR = 211; spotG = 84; spotB = 0
    },
    @{
        id = "zombie_cow_spawn_egg"
        baseR = 58; baseG = 83; baseB = 43
        spotR = 93; spotG = 64; spotB = 55
    },
    @{
        id = "zombie_pig_spawn_egg"
        baseR = 160; baseG = 102; baseB = 118
        spotR = 46; spotG = 83; spotB = 57
    },
    @{
        id = "zombie_sheep_spawn_egg"
        baseR = 140; baseG = 158; baseB = 132
        spotR = 120; spotG = 40; spotB = 31
    },
    @{
        id = "end_void_stalker_spawn_egg"
        baseR = 26; baseG = 9; baseB = 51
        spotR = 0; spotG = 255; spotB = 200
    },
    @{
        id = "mutant_commander_spawn_egg"
        baseR = 30; baseG = 70; baseB = 32
        spotR = 231; spotG = 76; spotB = 60
    },
    @{
        id = "illager_swordsman_spawn_egg"
        baseR = 97; baseG = 106; baseB = 107
        spotR = 189; spotG = 195; spotB = 199
    },
    @{
        id = "illager_shieldbearer_spawn_egg"
        baseR = 97; baseG = 106; baseB = 107
        spotR = 81; spotG = 62; spotB = 46
    },
    @{
        id = "village_guard_spawn_egg"
        baseR = 147; baseG = 97; baseB = 62
        spotR = 207; spotG = 216; spotB = 220
    },
    @{
        id = "village_archer_spawn_egg"
        baseR = 147; baseG = 97; baseB = 62
        spotR = 39; spotG = 174; spotB = 96
    },
    @{
        id = "village_dog_spawn_egg"
        baseR = 215; baseG = 197; baseB = 175
        spotR = 191; spotG = 76; spotB = 58
    },
    @{
        id = "village_cavalry_spawn_egg"
        baseR = 147; baseG = 97; baseB = 62
        spotR = 241; spotG = 196; spotB = 15
    }
)

# Authentic 12x14 Vanilla Minecraft Spawn Egg Silhouette Mask (BUG-13 Fix)
# Exact proportions matching vanilla item spawn eggs:
# 2-pixel transparent padding on left/right, 1-pixel on top/bottom
# 0 = transparent, 1 = outline, 2 = base body, 3 = highlight, 4 = spots
$eggMask = @(
    "0000000000000000",
    "0000001111000000",
    "0000013322100000",
    "0000133442210000",
    "0001334444221000",
    "0001324424421000",
    "0013322222222100",
    "0013224424422100",
    "0013244444422100",
    "0012244444422100",
    "0012224424422100",
    "0012222222222100",
    "0001222442221000",
    "0000122442210000",
    "0000011221100000",
    "0000000000000000"
)

foreach ($egg in $eggs) {
    $id = $egg.id

    # 1. Item JSON (Minecraft 26.3 item definition)
    $itemJson = @{
        model = @{
            type = "minecraft:model"
            model = "livingworld:item/$id"
        }
    } | ConvertTo-Json -Depth 5
    Set-Content -Path "$itemsDir/$id.json" -Value $itemJson

    # 2. Model JSON (standard generated item model)
    $modelJson = @{
        parent = "minecraft:item/generated"
        textures = @{
            layer0 = "livingworld:item/$id"
        }
    } | ConvertTo-Json -Depth 5
    Set-Content -Path "$modelsDir/$id.json" -Value $modelJson

    # 3. 16x16 Pixel Art Texture with authentic 12x14 egg silhouette
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    
    $cOutline = [System.Drawing.Color]::FromArgb(255, [Math]::Max(0, [int]($egg.baseR * 0.45)), [Math]::Max(0, [int]($egg.baseG * 0.45)), [Math]::Max(0, [int]($egg.baseB * 0.45)))
    $cHighlight = [System.Drawing.Color]::FromArgb(255, [Math]::Min(255, [int]($egg.baseR * 1.30)), [Math]::Min(255, [int]($egg.baseG * 1.30)), [Math]::Min(255, [int]($egg.baseB * 1.30)))
    $cBase = [System.Drawing.Color]::FromArgb(255, [int]$egg.baseR, [int]$egg.baseG, [int]$egg.baseB)
    $cSpot = [System.Drawing.Color]::FromArgb(255, [int]$egg.spotR, [int]$egg.spotG, [int]$egg.spotB)

    for ($y = 0; $y -lt 16; $y++) {
        $row = $eggMask[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $char = $row[$x]
            switch ($char) {
                '0' { $bmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent) }
                '1' { $bmp.SetPixel($x, $y, $cOutline) }
                '2' { $bmp.SetPixel($x, $y, $cBase) }
                '3' { $bmp.SetPixel($x, $y, $cHighlight) }
                '4' { $bmp.SetPixel($x, $y, $cSpot) }
            }
        }
    }

    $outPng = "$texturesDir/$id.png"
    $bmp.Save($outPng, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Generated authentic proportional assets for $id -> $outPng"
}

Write-Host "All 13 spawn egg items, models, and textures generated with authentic vanilla proportions."
