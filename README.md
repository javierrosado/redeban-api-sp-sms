# redeban-api-sp-sms (Camel + Quarkus + OCP)

Modernización del **servicio SMS** legado (Fuse/Blueprint) a **microservicio** sobre **Quarkus + Camel** con arquitectura hexagonal (`com.redeban.sms`).Se toma como **estándar** el proyecto `redeban-api-sp-email` (estructura, scripts y manifiestos).

## Estructura
```
redeban-api-sp-sms/
  .vscode/
  deploy/base/           # Manifiestos OCP base (Deployment, Service, Route, ConfigMap, Secret, HPA)
  docs/plantuml/         # Diagramas: 01_legacy_sendSms, 02_legacy_sendSmsLatinia, 03_new_send_api
  postman/               # Colección y environment
  scripts/               # compile.ps1, build-push.ps1, deploy.ps1, undeploy.ps1
  src/main/java/com/redeban/sms/...   # Código fuente (hexagonal)
  src/main/resources/templates/       # Templates Velocity (reusados del legado)
  parametros.conf        # Parámetros para scripts
  Dockerfile
  pom.xml
```

## Endpoints
- BasePath: `/redeban/sms`
- OpenAPI / Swagger: `/q/swagger-ui`
- Health: `/q/health`
- REST: `POST /v1/sms/send`

### Headers opcionales para trazabilidad
`idTransaccion`, `timestamp`, `ipAplicacion`, `nombreAplicacion`.Se capturan en `LoggingFilter` y se propagan vía MDC a todos los logs. Cada método registra:
- `idTransaccion`
- Inputs y outputs (JSON)
- Tiempo de ejecución (ms)
- Errores con *stacktrace* y causa raíz

## Configuración
`deploy/base/configmap.yaml` expone:
- `LOG_LEVEL` (`INFO`, `DEBUG`, `WARN`, `ERROR`)
- `LOG_JSON` (`true|false`)
- `SMS_SERVICIO_HOST`, `SMS_SERVICIO_PATH` (backend WMB)
- `LATINIA_URL` (endpoint Latinia)

Y `deploy/base/secret.yaml`:
- `TRUSTSTORE_PATH`, `TRUSTSTORE_PASSWORD` (si se requiere SSL mutuamente confiable)

`src/main/resources/application.properties` mapea estas variables.

## Build local (PowerShell 7, Windows)
```powershell
# 1) Compilar (sin tests)
./scripts/compile.ps1

# 2) Build & Push (Dockerfile → imagen en registro OCP)
./scripts/build-push.ps1
```

## Despliegue en OpenShift
1. Edita `parametros.conf`:
   ```
   NAMESPACE=redeban-transversal
   VERSION=1.0.1
   REGISTRY=default-route-openshift-image-registry.apps-crc.testing
   ROUTE_HOST=router-redeban-desa.apps-desa.apps-crc.testing
   ```
2. Aplica:
   ```powershell
   ./scripts/deploy.ps1
   ```
   El script mostrará:
   - Swagger UI
   - Health
   - Endpoint REST funcional

## Validación de disponibilidad
- **Salud**:
  ```powershell
  $route = (oc -n <ns> get route redeban-api-sp-sms -o jsonpath="{.spec.host}")
  curl -k "https://$route/redeban/sms/q/health"
  ```
- **Prueba funcional**:
  ```powershell
  curl -k -X POST "https://$route/redeban/sms/v1/sms/send" ^
    -H "Content-Type: application/json" ^
    -H "idTransaccion: tx-001" -H "ipAplicacion: 10.0.0.1" -H "nombreAplicacion: tester" ^
    --data "{\"to\":\"51999999999\",\"message\":\"Hola desde Quarkus\"}"
  ```
- **Logs del pod**:
  ```powershell
  oc -n <ns> logs deploy/redeban-api-sp-sms -f
  ```

## Ajuste de nivel de log
Cambia `LOG_LEVEL` en el ConfigMap y ejecuta:
```powershell
oc -n <ns> apply -f target/ocp/configmap.yaml
oc -n <ns> rollout restart deploy/redeban-api-sp-sms
```

## Postman
Importa `postman/redeban-api-sp-sms.postman_collection.json` y el environment `postman/redeban-api-sp-sms.local.postman_environment.json`.
Actualiza `baseUrl` para apuntar a la `Route` generada.

## Notas
- Se reutilizan los templates Velocity del legado: `smsRequest.vm` y `peticionLatinia.vm`.
- `SmsRoute` implementa las rutas `direct:sendSms` y `direct:sendSmsLatinia` equivalentes a Blueprint.
- Para TLS, suministra `TRUSTSTORE_PATH` y `TRUSTSTORE_PASSWORD` si aplica.



### TLS del cliente HTTP
La ruta HTTPS utilizará el truststore por defecto de la JVM. Si necesitas un truststore propio, configura:
```powershell
# En OCP, agrega a deployment.yaml (env JAVA_TOOL_OPTIONS)
- name: JAVA_TOOL_OPTIONS
  value: "-Djavax.net.ssl.trustStore=/work/keystore/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"
```
Alternativamente, monta el truststore como Secret y referencia la ruta en `JAVA_TOOL_OPTIONS`.
