$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$java = "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
if (-not (Test-Path $java)) {
    $java = "$projectRoot\java\java-runtime-epsilon\bin\java.exe"
}

$metaMinecraft = Get-Content "$projectRoot\meta\net.minecraft\26.3.json" | ConvertFrom-Json
$metaLoader = Get-Content "$projectRoot\meta\net.fabricmc.fabric-loader\0.20.1.json" | ConvertFrom-Json
$metaLwjgl = Get-Content "$projectRoot\meta\org.lwjgl3\3.4.3.json" | ConvertFrom-Json

$allMetaLibs = @($metaMinecraft.libraries) + @($metaLoader.libraries) + @($metaLwjgl.libraries)

$jars = [System.Collections.Generic.List[string]]::new()
$jars.Add((Resolve-Path "$projectRoot\libraries\net\fabricmc\fabric-loader\0.20.1\fabric-loader-0.20.1.jar").Path)
$jars.Add((Resolve-Path "$projectRoot\libraries\net\fabricmc\sponge-mixin\0.17.5+mixin.0.8.7\sponge-mixin-0.17.5+mixin.0.8.7.jar").Path)
$jars.Add((Resolve-Path "$projectRoot\libraries\net\fabricmc\intermediary\26.3\intermediary-26.3.jar").Path)
$jars.Add((Resolve-Path "$projectRoot\libraries\com\mojang\minecraft\26.3\minecraft-26.3-client.jar").Path)

foreach ($lib in $allMetaLibs) {
    $parts = $lib.name -split ":"
    $group = $parts[0].Replace(".", "/")
    $art = $parts[1]
    $ver = $parts[2]
    $path = "$projectRoot\libraries\$group\$art\$ver\$art-$ver.jar"
    if (Test-Path $path) {
        $jars.Add((Resolve-Path $path).Path)
    }
}

# Add native jars
Get-ChildItem -Path "$projectRoot\libraries\org\lwjgl" -Recurse -Filter "*natives-windows*.jar" | ForEach-Object {
    if ($_.Name -like "*3.4.3*") {
        $jars.Add($_.FullName)
    }
}

$cp = ($jars -join ";")

$gameDir = "$projectRoot\instances\LivingWorld\minecraft"
$assetsDir = "$projectRoot\assets"

Write-Host "Launching Minecraft 26.3 into Survival World 'PlaytestSurvival' for full scenario run..."
$cmdArgs = @(
    "--enable-native-access=ALL-UNNAMED",
    "-Duser.language=en",
    "-Dminecraft.api.auth.host=https://nope.invalid",
    "-Dminecraft.api.account.host=https://nope.invalid",
    "-Dminecraft.api.session.host=https://nope.invalid",
    "-Dminecraft.api.services.host=https://nope.invalid",
    "-XX:StackShadowPages=32",
    "-Xms1024m",
    "-Xmx4096m",
    "-cp", $cp,
    "net.fabricmc.loader.impl.launch.knot.KnotClient",
    "--username", "Playtester",
    "--version", "26.3",
    "--gameDir", $gameDir,
    "--assetsDir", $assetsDir,
    "--assetIndex", "34",
    "--uuid", "0670b6300e4d43d4851b903341974a83",
    "--accessToken", "0",
    "--versionType", "release",
    "--quickPlaySingleplayer", "PlaytestSurvival"
)

# Start process
$proc = Start-Process -FilePath $java -ArgumentList $cmdArgs -PassThru -NoNewWindow -RedirectStandardOutput "logs/survival_stdout.log" -RedirectStandardError "logs/survival_stderr.log"
Write-Host "Started PID $($proc.Id). Monitoring for 60 seconds of Survival simulation..."

for ($s = 1; $s -le 12; $s++) {
    Start-Sleep -Seconds 5
    if ($proc.HasExited) {
        Write-Host "Process exited early with code: $($proc.ExitCode)"
        break
    } else {
        $ws = [Math]::Round($proc.WorkingSet64 / 1MB, 2)
        Write-Host "Second $($s * 5) - Running, Working Set: $($ws) MB, CPU: $($proc.TotalProcessorTime.TotalSeconds)s"
    }
}

Write-Host "=== SURVIVAL LOG TAIL ==="
Get-Content -Path "logs/survival_stdout.log" -Tail 80

if (-not $proc.HasExited) {
    Write-Host "Gracefully stopping playtest client PID $($proc.Id)..."
    Stop-Process -Id $proc.Id -Force
}
