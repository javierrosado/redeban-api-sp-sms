param()

Set-StrictMode -Version Latest

function Write-Log {
  param([string]$Level, [string]$Message)
  $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
  switch ($Level) {
    "INFO"    { $color = "Cyan" }
    "DEBUG"   { $color = "Gray" }
    "WARN"    { $color = "Yellow" }
    "ERROR"   { $color = "Red" }
    default   { $color = "White" }
  }
  Write-Host "[$ts][$Level] $Message" -ForegroundColor $color
}

function Load-Params {
  $path = Join-Path $PSScriptRoot "..\parametros.conf"
  if (!(Test-Path $path)) { throw "No existe parametros.conf en $path" }
  $map = @{}
  Get-Content $path | ForEach-Object {
    if ($_ -match "^\s*#") { return }
    if ($_ -match "^\s*$") { return }
    $kv = $_.Split("=",2)
    if ($kv.Length -eq 2) { $map[$kv[0].Trim()] = $kv[1].Trim() }
  }
  return $map
}

function Render-Manifests {
  param([hashtable]$ParamsMap)
  $out = "target/ocp"
  if (Test-Path $out) { Remove-Item $out -Recurse -Force }
  New-Item -ItemType Directory -Path $out | Out-Null
  Get-ChildItem deploy/base -File | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content.Replace("__NAMESPACE__", $ParamsMap.NAMESPACE)
    $content = $content.Replace("__VERSION__", $ParamsMap.VERSION)
    $content = $content.Replace("__REGISTRY__", $ParamsMap.REGISTRY)
    $content = $content.Replace("__ROUTE_HOST__", $ParamsMap.ROUTE_HOST)
    $dest = Join-Path $out $_.Name
    Set-Content -Path $dest -Value $content -Encoding UTF8
  }
  Write-Log INFO "Manifiestos renderizados en $out"
  return $out
}
