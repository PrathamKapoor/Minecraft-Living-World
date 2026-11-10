$modsDir = "instances/LivingWorld/minecraft/mods"

# URLs and hashes
$downloads = @(
    @{
        name = "mutantmonsters-v26.3.1-mc26.3.x+fabric.jar"
        url = "https://cdn.modrinth.com/data/derP0ten/versions/XNliGY5t/mutantmonsters-v26.3.1-mc26.3.x%2Bfabric.jar"
        sha512 = "76c4ddd2a1b659270d0013ca758b0d64000a3b9fed44230b2a56e1b636cda83ec8661a76f39e1a7cf6e43af9ba04c358f9cfea02533c4f512e9cbd80d50e33c9"
    },
    @{
        name = "puzzleslib-v26.3.17-mc26.3.x+fabric.jar"
        url = "https://cdn.modrinth.com/data/QAGBst4M/versions/b1UA02v8/puzzleslib-v26.3.17-mc26.3.x%2Bfabric.jar"
        sha512 = "6366724a2eb5320b02feacb0983fdd66bf8615c17d7a44426264434222acbc7462ab0c0b81756ae3a11aa82e32a584fae774fccd0f4abfd0db5535747ff74c40"
    },
    @{
        name = "ForgeConfigAPIPort-v26.3.1-mc26.3.x-Fabric.jar"
        url = "https://cdn.modrinth.com/data/ohNO6lps/versions/JpKvrr9J/ForgeConfigAPIPort-v26.3.1-mc26.3.x-Fabric.jar"
        sha512 = "b9339c21e14beec2eae0ca215317284bcaacef72e1d1dee8b3dfbe2de0eefaa758a172446df85419b6534557c479abe972d7d21f4ec194f2d436f9ed4329cb9f"
    }
)

foreach ($d in $downloads) {
    $dest = Join-Path $modsDir $d.name
    Write-Output "Downloading $($d.name)..."
    Invoke-WebRequest -Uri $d.url -OutFile $dest -UserAgent "LivingWorld-Agent/0.2.3"
    $hash = Get-FileHash -Path $dest -Algorithm SHA512
    if ($hash.Hash.ToLower() -eq $d.sha512.ToLower()) {
        Write-Output "  [OK] Hash verified: $($d.name)"
    } else {
        Write-Error "  [FAIL] Hash mismatch for $($d.name)! Expected $($d.sha512) got $($hash.Hash)"
    }
}
