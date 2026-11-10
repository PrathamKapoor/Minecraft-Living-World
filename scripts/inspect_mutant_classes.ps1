Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("instances/LivingWorld/minecraft/mods/mutantmonsters-v26.3.1-mc26.3.x+fabric.jar")
Write-Output "=== Entities & Registries in Mutant Monsters ==="
foreach ($entry in $zip.Entries) {
    if ($entry.FullName -like "*Entity*.class" -or $entry.FullName -like "*Registry*.class" -or $entry.FullName -like "*ModEntities*.class") {
        Write-Output $entry.FullName
    }
}
$zip.Dispose()
