$pack1 = Get-Content -Path instances\26.3\mmc-pack.json
$pack2 = Get-Content -Path instances\LivingWorld\mmc-pack.json
Compare-Object $pack1 $pack2
