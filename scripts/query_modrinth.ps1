$headers = @{ 'User-Agent' = 'LivingWorld-Agent/0.2.3 (https://github.com)' }
foreach ($projSlug in @('puzzles-lib', 'forge-config-api-port')) {
    Write-Output "=== Project: $projSlug ==="
    $proj = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$projSlug/version" -Headers $headers
    foreach ($v in $proj) {
        if ($v.game_versions -contains '26.3' -and $v.loaders -contains 'fabric') {
            Write-Output "Version: $($v.version_number) | Loaders: $($v.loaders -join ',') | Games: $($v.game_versions -join ',')"
            foreach ($f in $v.files) {
                Write-Output "   File: $($f.filename) ($($f.size) bytes)"
                Write-Output "   Url: $($f.url)"
                Write-Output "   Hashes: SHA1=$($f.hashes.sha1), SHA512=$($f.hashes.sha512)"
            }
        }
    }
}
