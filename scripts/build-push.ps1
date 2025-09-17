param()
. "$PSScriptRoot/common.ps1"

$P = Load-Params
$img = "$($P.REGISTRY)/$($P.NAMESPACE)/redeban-api-sp-sms:$($P.VERSION)"

try {
  $jar = Get-ChildItem target -Filter "*-runner.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $jar) {
    Write-Log WARN "No existe jar en target. Ejecutando compilacion..."
    & "$PSScriptRoot/compile.ps1"
    if ($LASTEXITCODE -ne 0) { throw "Compilacion fallida" }
  }

  Write-Log INFO "Autenticando contra registro de OpenShift: $($P.REGISTRY)"
  $token = (& oc whoami -t) 2>$null
  if (-not $token) { throw "No se pudo obtener token: ejecutar 'oc login' previamente" }
  docker login -u (oc whoami) -p $token $($P.REGISTRY)

  Write-Log INFO "Construyendo imagen con Dockerfile..."
  docker build -t $img .
  if ($LASTEXITCODE -ne 0) { throw "docker build fallo" }

  Write-Log INFO "Publicando imagen $img"
  docker push $img
  if ($LASTEXITCODE -ne 0) { throw "docker push fallo" }

  Write-Log INFO "Build & Push OK -> $img"
} catch {
  Write-Log ERROR $_
  exit 1
}
