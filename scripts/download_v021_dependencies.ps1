$ErrorActionPreference = "Stop"

$modsDir = "instances/LivingWorld/minecraft/mods"
if (!(Test-Path $modsDir)) {
    New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
}

$dependencies = @(
    @{
        Name = "[fabric]ctov-26-3-4.2.2a.jar"
        Url  = "https://cdn.modrinth.com/data/fgmhI8kH/versions/Vcv7z4RB/%5Bfabric%5Dctov-26-3-4.2.2a.jar"
        Desc = "ChoiceTheorem's Overhauled Village (CTOV) 4.2.2a for Fabric 26.3"
    },
    @{
        Name = "lithostitched-2.0.6-fabric-26.3.jar"
        Url  = "https://cdn.modrinth.com/data/XaDC71GB/versions/pZgI6b2t/lithostitched-2.0.6-fabric-26.3.jar"
        Desc = "Lithostitched 2.0.6 worldgen library for Fabric 26.3"
    },
    @{
        Name = "BetterVanillaDragonFight-v1.0.1+26.3.0.jar"
        Url  = "https://cdn.modrinth.com/data/dkBVZc20/versions/sqQcWniX/BetterVanillaDragonFight-v1.0.1%2B26.3.0.jar"
        Desc = "Better Vanilla Dragon Fight v1.0.1 for 26.3.0"
    },
    @{
        Name = "fabric-language-kotlin-1.14.2+kotlin.2.4.21.jar"
        Url  = "https://cdn.modrinth.com/data/Ha28R6CL/versions/SKW1BTL3/fabric-language-kotlin-1.14.2%2Bkotlin.2.4.21.jar"
        Desc = "Fabric Language Kotlin 1.14.2 for Fabric 26.3"
    },
    @{
        Name = "MoogsEndStructures-universal-1.21-2.1.1.jar"
        Url  = "https://cdn.modrinth.com/data/r4PuRGfV/versions/S7bUhX4n/MoogsEndStructures-universal-1.21-2.1.1.jar"
        Desc = "Moog's End Structures 2.1.1"
    },
    @{
        Name = "MoogsNetherStructures-universal-1.21-3.1.1.jar"
        Url  = "https://cdn.modrinth.com/data/nGUXvjTa/versions/OLTqXnsN/MoogsNetherStructures-universal-1.21-3.1.1.jar"
        Desc = "Moog's Nether Structures 3.1.1"
    },
    @{
        Name = "MoogsStructureLib-fabric-26.3-3.4.2.jar"
        Url  = "https://cdn.modrinth.com/data/1oUDhxuy/versions/SkD4H9XM/MoogsStructureLib-fabric-26.3-3.4.2.jar"
        Desc = "Moog's Structure Lib 3.4.2 for Fabric 26.3"
    }
)

Write-Host "=================================================="
Write-Host " Downloading Living World v0.2.1 Dependencies     "
Write-Host " Target Directory: $modsDir                      "
Write-Host "=================================================="

foreach ($dep in $dependencies) {
    $targetPath = Join-Path $modsDir $dep.Name
    if (Test-Path $targetPath) {
        $size = (Get-Item $targetPath).Length
        Write-Host "[ALREADY EXISTS] $($dep.Name) ($size bytes)"
    } else {
        Write-Host "[DOWNLOADING] $($dep.Desc)..."
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13
            Invoke-WebRequest -Uri $dep.Url -OutFile $targetPath -UserAgent "LivingWorldBuildScript/0.2.1"
            $size = (Get-Item $targetPath).Length
            Write-Host "[DOWNLOADED] $($dep.Name) ($size bytes)"
        } catch {
            Write-Host "[ERROR] Failed to download $($dep.Name): $_"
        }
    }
}

Write-Host "=================================================="
Write-Host " Dependency verification complete.                "
Write-Host "=================================================="
