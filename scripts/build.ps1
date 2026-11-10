# scripts/build.ps1 — Living World Deterministic Java 25 Build & Deploy Script
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Minecraft: Living World Mod   " -ForegroundColor Cyan
Write-Host " Target: Minecraft 26.3 | Fabric 0.20.1 " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Find JDK 25
$javac = "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
$jar = "C:\Program Files\Java\jdk-25.0.2\bin\jar.exe"
if (-not (Test-Path $javac)) {
    $javac = "$projectRoot\java\java-runtime-epsilon\bin\javac.exe"
    $jar = "$projectRoot\java\java-runtime-epsilon\bin\jar.exe"
}
if (-not (Test-Path $javac)) {
    throw "JDK 25 javac.exe not found!"
}
Write-Host "Using Java compiler: $javac" -ForegroundColor Green

# Prepare build directories
$buildDir = "$projectRoot\build"
$classesDir = "$buildDir\classes\java\main"
$libsDir = "$buildDir\libs"
if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir }
New-Item -ItemType Directory -Path $classesDir, $libsDir -Force | Out-Null

# Resolve exact 26.3 libraries from meta
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

# Add Fabric API processed modules
Get-ChildItem -Path "$projectRoot\instances\26.3\minecraft\.fabric\processedMods\*.jar" | ForEach-Object {
    $jars.Add($_.FullName)
}

$cp = ($jars -join ";")

# Collect source files
$javaFiles = Get-ChildItem -Path "$projectRoot\src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if ($javaFiles.Count -eq 0) {
    throw "No Java source files found in src/main/java!"
}
Write-Host "Found $($javaFiles.Count) Java source files to compile." -ForegroundColor Yellow

# Write javac arguments file
$argsFile = "$buildDir\javac_args.txt"
$argLines = @(
    "-cp", $cp,
    "-d", $classesDir,
    "-encoding", "UTF-8",
    "-source", "25",
    "-target", "25"
) + $javaFiles

[System.IO.File]::WriteAllLines($argsFile, $argLines)

Write-Host "Compiling Java sources with javac..." -ForegroundColor Yellow
$proc = Start-Process -FilePath $javac -ArgumentList "@$argsFile" -NoNewWindow -PassThru -Wait
if ($proc.ExitCode -ne 0) {
    throw "Compilation failed with exit code $($proc.ExitCode)!"
}
Write-Host "Compilation successful!" -ForegroundColor Green

# Copy resources
if (Test-Path "$projectRoot\scripts\generate_spawn_egg_textures.ps1") {
    & "$projectRoot\scripts\generate_spawn_egg_textures.ps1"
}
if (Test-Path "$projectRoot\scripts\generate_guard_textures.ps1") {
    & "$projectRoot\scripts\generate_guard_textures.ps1"
}
if (Test-Path "$projectRoot\src\main\resources") {
    Copy-Item -Path "$projectRoot\src\main\resources\*" -Destination $classesDir -Recurse -Force
    Write-Host "Copied resources into classes directory." -ForegroundColor Green
}

# Package JAR
$modJar = "$libsDir\livingworld-0.2.5.jar"
if (Test-Path $modJar) { Remove-Item -Force $modJar }

Set-Location $classesDir
& $jar cf $modJar *
Set-Location $projectRoot

if (-not (Test-Path $modJar)) {
    throw "Failed to create jar at $modJar!"
}
$jarSize = (Get-Item $modJar).Length
Write-Host "Successfully packaged $modJar ($jarSize bytes)" -ForegroundColor Green

# Deploy to Prism Launcher instance
$instanceMods = "$projectRoot\instances\LivingWorld\minecraft\mods"
if (Test-Path $instanceMods) {
    # Remove older jars from instance mods to prevent duplicate ID collision
    if (Test-Path "$instanceMods\livingworld-0.1.0.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.1.0.jar"
    }
    if (Test-Path "$instanceMods\livingworld-0.2.0.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.2.0.jar"
    }
    if (Test-Path "$instanceMods\livingworld-0.2.1.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.2.1.jar"
    }
    if (Test-Path "$instanceMods\livingworld-0.2.2.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.2.2.jar"
    }
    if (Test-Path "$instanceMods\livingworld-0.2.3.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.2.3.jar"
    }
    if (Test-Path "$instanceMods\livingworld-0.2.4.jar") {
        Remove-Item -Force "$instanceMods\livingworld-0.2.4.jar"
    }
    Copy-Item -Path $modJar -Destination "$instanceMods\livingworld-0.2.5.jar" -Force
    Write-Host "Deployed to Prism Launcher instance: $instanceMods\livingworld-0.2.5.jar" -ForegroundColor Green
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " BUILD & DEPLOY COMPLETE               " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
