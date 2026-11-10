Add-Type -AssemblyName System.IO.Compression.FileSystem
$jars = @('mutantmonsters-v26.3.1-mc26.3.x+fabric.jar', 'puzzleslib-v26.3.17-mc26.3.x+fabric.jar', 'ForgeConfigAPIPort-v26.3.1-mc26.3.x-Fabric.jar')
foreach ($j in $jars) {
    Write-Output "================ $j ================"
    $zip = [System.IO.Compression.ZipFile]::OpenRead("instances/LivingWorld/minecraft/mods/$j")
    $entry = $zip.GetEntry("fabric.mod.json")
    if ($entry) {
        $reader = New-Object System.IO.StreamReader($entry.Open())
        Write-Output $reader.ReadToEnd()
        $reader.Close()
    }
    $zip.Dispose()
}
