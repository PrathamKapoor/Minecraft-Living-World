# scripts/test.ps1 — Living World Automated Acceptance Test Runner
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Running Living World Automated Tests   " -ForegroundColor Cyan
Write-Host " Target: Minecraft 26.3 | Java 25       " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# JDK paths
$javac = "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
$java = "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
if (-not (Test-Path $javac)) {
    $javac = "$projectRoot\java\java-runtime-epsilon\bin\javac.exe"
    $java = "$projectRoot\java\java-runtime-epsilon\bin\java.exe"
}

# Directories
$buildDir = "$projectRoot\build"
$testClassesDir = "$buildDir\classes\java\test"
$mainClassesDir = "$buildDir\classes\java\main"
if (Test-Path $testClassesDir) { Remove-Item -Recurse -Force $testClassesDir }
New-Item -ItemType Directory -Path $testClassesDir -Force | Out-Null

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

$jars.Add($mainClassesDir)

$cp = ($jars -join ";")

# Compile test sources
$testFiles = Get-ChildItem -Path "$projectRoot\src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$argsFile = "$buildDir\test_javac_args.txt"
$argLines = @(
    "-cp", $cp,
    "-d", $testClassesDir,
    "-encoding", "UTF-8",
    "-source", "25",
    "-target", "25"
) + $testFiles

[System.IO.File]::WriteAllLines($argsFile, $argLines)

Write-Host "Compiling test sources with javac..." -ForegroundColor Yellow
$proc = Start-Process -FilePath $javac -ArgumentList "@$argsFile" -NoNewWindow -PassThru -Wait
if ($proc.ExitCode -ne 0) {
    throw "Test compilation failed with exit code $($proc.ExitCode)!"
}

# Run tests
$runCp = "$testClassesDir;$cp"
$runArgsFile = "$buildDir\test_run_args.txt"
$runArgLines = @(
    "-cp", $runCp,
    "livingworld.LivingWorldTests"
)
[System.IO.File]::WriteAllLines($runArgsFile, $runArgLines)

Write-Host "Executing test suite..." -ForegroundColor Yellow
$runProc = Start-Process -FilePath $java -ArgumentList "@$runArgsFile" -NoNewWindow -PassThru -Wait
if ($runProc.ExitCode -ne 0) {
    throw "Tests failed with exit code $($runProc.ExitCode)!"
}

Write-Host "========================================" -ForegroundColor Green
Write-Host " ALL ACCEPTANCE TESTS PASSED!           " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
