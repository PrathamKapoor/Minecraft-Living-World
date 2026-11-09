# scripts/test_gameplay.ps1 — Run Gameplay Playtest Scenarios 1-7
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$javac = "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
$java = "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
if (-not (Test-Path $javac)) {
    $javac = "$projectRoot\java\java-runtime-epsilon\bin\javac.exe"
    $java = "$projectRoot\java\java-runtime-epsilon\bin\java.exe"
}

$buildDir = "$projectRoot\build"
$testClassesDir = "$buildDir\classes\java\test"
$mainClassesDir = "$buildDir\classes\java\main"

$metaMinecraft = Get-Content "$projectRoot\meta\net.minecraft\26.3.json" | ConvertFrom-Json
$metaLoader = Get-Content "$projectRoot\meta\net.fabricmc.fabric-loader\0.20.1.json" | ConvertFrom-Json
$metaLwjgl = Get-Content "$projectRoot\meta\org.lwjgl3\3.4.3.json" | ConvertFrom-Json

$allMetaLibs = @($metaMinecraft.libraries) + @($metaLoader.libraries) + @($metaLwjgl.libraries)

$jars = [System.Collections.Generic.List[string]]::new()
$jars.Add((Resolve-Path "$projectRoot\libraries\com\mojang\minecraft\26.3\minecraft-26.3-client.jar").Path)
$jars.Add((Resolve-Path "$projectRoot\libraries\net\fabricmc\fabric-loader\0.20.1\fabric-loader-0.20.1.jar").Path)
$jars.Add((Resolve-Path "$projectRoot\libraries\net\fabricmc\sponge-mixin\0.17.5+mixin.0.8.7\sponge-mixin-0.17.5+mixin.0.8.7.jar").Path)

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

Get-ChildItem -Path "$projectRoot\instances\26.3\minecraft\.fabric\processedMods\*.jar" | ForEach-Object {
    $jars.Add($_.FullName)
}

$jars.Add($mainClassesDir)
$cp = ($jars -join ";")

Write-Host "Compiling GameplayPlaytestRunner with javac..."
$testFiles = Get-ChildItem -Path "$projectRoot\src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac -cp $cp -d $testClassesDir -encoding UTF-8 $testFiles

$runCp = "$testClassesDir;$cp"
Write-Host "Executing GameplayPlaytestRunner..."
& $java "--enable-native-access=ALL-UNNAMED" -cp $runCp livingworld.GameplayPlaytestRunner
