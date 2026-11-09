$content = Get-Content -Path logs\PrismLauncher-4.log
for ($i = 0; $i -lt $content.Count; $i++) {
    if ($content[$i] -match '-cp') {
        Write-Output "Found at line $i : $($content[$i].Substring(0, 80))..."
        $script:targetLine = $content[$i]
    }
}
$parts = $targetLine.Split('"')
Write-Output "Parts count: $($parts.Count)"
for ($j = 0; $j -lt $parts.Count; $j++) {
    if ($parts[$j] -match '-cp') {
        Write-Output "Part $j contains -cp, length: $($parts[$j].Length)"
        # Write to file
        Set-Content -Path "scripts/raw_command.txt" -Value $parts[$j]
    }
}
