param()
. "$PSScriptRoot/common.ps1"
$P = Load-Params

try {
  $dir = "target/ocp"
  if (!(Test-Path $dir)) {
    Write-Log WARN "No existe $dir, renderizando desde deploy/base..."
    $null = Render-Manifests -ParamsMap $P
  }
  Write-Log WARN "Eliminando recursos en $($P.NAMESPACE)"
  & oc -n $P.NAMESPACE delete -f "$dir" --ignore-not-found=true
  Write-Log INFO "Undeploy completado."
} catch {
  Write-Log ERROR $_
  exit 1
}
