Add-Type -AssemblyName System.IO.Compression.FileSystem

$cp = @(
    "instances/LivingWorld/minecraft/mods/mutantmonsters-v26.3.1-mc26.3.x+fabric.jar",
    "instances/LivingWorld/minecraft/mods/puzzleslib-v26.3.17-mc26.3.x+fabric.jar",
    "libraries/com/mojang/minecraft/26.3/minecraft-26.3-client.jar"
)

# Use javap to inspect class hierarchy
$javap = "C:\Program Files\Java\jdk-25.0.2\bin\javap.exe"
$classes = @(
    "fuzs.mutantmonsters.common.world.entity.mutant.MutantZombie",
    "fuzs.mutantmonsters.common.world.entity.mutant.MutantSkeleton",
    "fuzs.mutantmonsters.common.world.entity.mutant.MutantCreeper",
    "fuzs.mutantmonsters.common.world.entity.mutant.MutantEnderman",
    "fuzs.mutantmonsters.common.world.entity.mutant.MutantSnowGolem"
)

foreach ($cls in $classes) {
    Write-Output "=== $cls ==="
    & $javap -cp ($cp -join ";") $cls | Select-String "public class"
}
