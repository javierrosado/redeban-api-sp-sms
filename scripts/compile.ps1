param()
. "$PSScriptRoot/common.ps1"

try {
  Write-Log INFO "Compilando (sin tests)..."
  & mvn -q -DskipTests clean package
  if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion Maven" }
  $jar = Get-ChildItem target -Filter "*-runner.jar" | Select-Object -First 1
  if (-not $jar) { throw "No se encuentra el jar *-runner.jar en target" }
  Write-Log INFO "Compilado OK: $($jar.Name)"
} catch {
  Write-Log ERROR $_
  exit 1
}
