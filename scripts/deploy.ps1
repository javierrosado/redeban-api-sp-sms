param()
. "$PSScriptRoot/common.ps1"

$P = Load-Params
$img = "$($P.REGISTRY)/$($P.NAMESPACE)/redeban-api-sp-sms:$($P.VERSION)"

try {
  Write-Log INFO "Renderizando manifiestos..."
  $dir = Render-Manifests -ParamsMap $P

  Write-Log INFO "Aplicando ConfigMap y Secret en namespace $($P.NAMESPACE)"
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "configmap.yaml")
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "secret.yaml")

  Write-Log INFO "Desplegando Deployment, Service, Route y HPA"
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "deployment.yaml")
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "service.yaml")
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "route.yaml")
  & oc -n $P.NAMESPACE apply -f (Join-Path $dir "hpa.yaml")

  Write-Log INFO "Esperando que el Deployment quede disponible..."
  & oc -n $P.NAMESPACE rollout status deploy/redeban-api-sp-sms

  $route = (& oc -n $P.NAMESPACE get route redeban-api-sp-sms -o jsonpath="{.spec.host}")
  $base  = "https://$route/redeban/sms"
  $swagger = "$base/q/swagger-ui"
  $health  = "$base/q/health"
  $send    = "$base/v1/sms/send"

  Write-Log INFO "Rutas expuestas:"
  Write-Host "  Swagger UI : $swagger" -ForegroundColor Green
  Write-Host "  Health     : $health"  -ForegroundColor Green
  Write-Host "  REST Send  : $send"    -ForegroundColor Green
} catch {
  Write-Log ERROR $_
  exit 1
}
