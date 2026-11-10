Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead("instances/LivingWorld/minecraft/mods/mutantmonsters-v26.3.1-mc26.3.x+fabric.jar")
Write-Output "=== Items & Blocks in Mutant Monsters ==="
foreach ($entry in $zip.Entries) {
    if ($entry.FullName -like "*Item*.class" -or $entry.FullName -like "*Block*.class") {
        Write-Output $entry.FullName
    }
}
$zip.Dispose()
