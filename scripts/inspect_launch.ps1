$content = Get-Content -Path logs\PrismLauncher-4.log
for ($i = 205; $i -lt [Math]::Min($content.Count, 225); $i++) {
    Write-Output "[$i]: $($content[$i])"
}
