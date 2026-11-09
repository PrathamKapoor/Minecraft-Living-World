# scripts/download_dependencies.ps1 — Ingest verified 26.3 Fabric dependencies
$destDir = "instances\LivingWorld\minecraft\mods"
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
}

$mods = @(
    @{ Name = "geckolib-fabric-26.3-5.5.7.jar"; Url = "https://cdn.modrinth.com/data/8BmcQJ2H/versions/kSxHvs99/geckolib-fabric-26.3-5.5.7.jar" },
    @{ Name = "mielons-the-sift-2.0.2-mc26.3-fabric.jar"; Url = "https://cdn.modrinth.com/data/j0IfTxBq/versions/mFSG4o4I/mielon%27s-the-sift-2.0.2-mc26.3-fabric.jar" },
    @{ Name = "xaeroworldmap-fabric-26.3-1.47.2.jar"; Url = "https://cdn.modrinth.com/data/NcUtCpym/versions/VM8lNSSM/xaeroworldmap-fabric-26.3-1.47.2.jar" },
    @{ Name = "xaerominimap-fabric-26.3-26.6.2.jar"; Url = "https://cdn.modrinth.com/data/1bokaNcj/versions/MfxfgXaO/xaerominimap-fabric-26.3-26.6.2.jar" },
    @{ Name = "regrowth-revamped-fabric-1.1.0+26.3.jar"; Url = "https://cdn.modrinth.com/data/mqlEubgZ/versions/oj6zcKap/regrowth-revamped-fabric-1.1.0%2B26.3.jar" },
    @{ Name = "Structory_26.3_v1.3.18+26.3.jar"; Url = "https://cdn.modrinth.com/data/aKCwCJlY/versions/GjOkOVW4/Structory_26.3_v1.3.18%2B26.3.jar" },
    @{ Name = "Explorify_v1.6.6.mod.jar"; Url = "https://cdn.modrinth.com/data/HSfsxuTo/versions/oyrAnNG8/Explorify%20v1.6.6.mod.jar" }
)

foreach ($m in $mods) {
    $outPath = Join-Path $destDir $m.Name
    if (-not (Test-Path $outPath)) {
        Write-Host "Downloading $($m.Name)..."
        try {
            Invoke-WebRequest -Uri $m.Url -OutFile $outPath -Headers @{"User-Agent"="LivingWorld/0.2"} -UseBasicParsing -TimeoutSec 120
            $len = (Get-Item $outPath).Length
            Write-Host "Successfully downloaded $($m.Name) ($len bytes)"
        } catch {
            Write-Host "Failed to download $($m.Name): $($_.Exception.Message)"
        }
    } else {
        $len = (Get-Item $outPath).Length
        Write-Host "Already present: $($m.Name) ($len bytes)"
    }
}
