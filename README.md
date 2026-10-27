# ClinicApp — Microservicio de recetas

`ms-recetas` es un microservicio REST de ejemplo desarrollado con **Quarkus 3.15.1** y **Java 17**. Lee la configuración de farmacia y la clave de firma desde **HashiCorp Vault**, oculta la API key al devolver la configuración y genera una firma SHA-256 truncada para cada receta.

## Tecnologías y estructura

- `pom.xml`: dependencias Maven gestionadas por el BOM de Quarkus, incluyendo REST/Jackson, CDI y la extensión `io.quarkiverse.vault:quarkus-vault`.
- `src/main/resources/application.properties`: puerto HTTP y conexión a Vault. El motor KV se configura como versión 2, montado en `secret`.
- `src/main/java/com/nttdata/recetas/RecetaResource.java`: endpoints REST y lectura de las propiedades `farmacia.api-key`, `farmacia.url` y `firma.clave`.
- `src/main/java/com/nttdata/recetas/FirmaService.java`: cálculo de SHA-256 sobre `codigoReceta:clave`; la firma devuelta contiene los primeros 16 caracteres hexadecimales.
- `src/main/java/com/nttdata/recetas/model/`: records para las peticiones y respuestas.
- `vault/`: scripts de PowerShell para iniciar Vault y crear los secretos.
- `peticiones.http` y `ms-recetas.postman_collection.json`: ejemplos de llamadas a la API.
- `src/main/java/com/nttdata/recetas/validacion/Validador.java`: validador del ejercicio, ejecutable desde IntelliJ.

## Requisitos

- JDK 17.
- Maven 3.9 o compatible, disponible como comando `mvn` (o Maven configurado en IntelliJ).
- HashiCorp Vault para Windows. Descarga el ejecutable desde [Vault — Install](https://developer.hashicorp.com/vault/install).
- Postman o un cliente HTTP para probar los endpoints.

Comprueba las herramientas antes de iniciar:

```powershell
java -version
mvn -version
```

Abre en IntelliJ la carpeta que contiene este `pom.xml` y espera a que Maven importe las dependencias.

## Ejecutar localmente

El ejemplo utiliza Vault en **modo desarrollo**, que almacena datos en memoria. Usa tres ventanas de PowerShell y conserva abierta la primera mientras pruebas el servicio.

1. **Inicia Vault** (ajusta la ruta si `vault.exe` está en otra ubicación):

   ```powershell
   .\vault\01-levantar-vault.ps1 -VaultExe "C:\vault\vault.exe"
   ```

   El servidor queda disponible en `http://localhost:8200` y el script configura el token de desarrollo `root`.

2. **Crea los secretos** desde una segunda ventana:

   ```powershell
   .\vault\02-crear-secreto.ps1 -VaultExe "C:\vault\vault.exe"
   ```

   El script crea estas propiedades en el secreto KV v2 `secret/clinica/ms-recetas`:

   | Propiedad | Valor de ejemplo |
   |---|---|
   | `farmacia.api-key` | `rx-key-2026-abcd` |
   | `farmacia.url` | `https://api.farmaciared.com` |
   | `firma.clave` | `s3cr3t-firma-2026` |

   Estos valores son únicamente para el taller local. Al reiniciar Vault en modo desarrollo, vuelve a crear el secreto.

3. **Arranca Quarkus** desde la carpeta del proyecto:

   ```powershell
   mvn quarkus:dev
   ```

   Quarkus utiliza Vault en `http://localhost:8200`; la configuración permite `VAULT_TOKEN` como variable de entorno y usa `root` como valor predeterminado para este entorno de desarrollo. Para usar otro token en la sesión actual de PowerShell:

   ```powershell
   $env:VAULT_TOKEN = "tu-token-local"
   mvn quarkus:dev
   ```

   Para detener Quarkus o Vault, usa `Ctrl+C` en sus respectivas ventanas.

## API disponible

Base URL: `http://localhost:8080/api/recetas`

| Método | Ruta | Comportamiento |
|---|---|---|
| `GET` | `/config` | Devuelve la URL de farmacia, la API key enmascarada (por ejemplo, `****abcd`) y si la clave de firma está configurada. |
| `POST` | `/firmar` | Recibe `codigoReceta` y `paciente`; genera la firma. Devuelve `400 Bad Request` si el código falta o está en blanco. |

Ejemplo de cuerpo para `POST /api/recetas/firmar`:

```json
{
  "codigoReceta": "RX-001",
  "paciente": "Ana Torres"
}
```

La respuesta correcta incluye `codigoReceta`, `paciente` y `firma`. Las peticiones de ejemplo están en `peticiones.http` y en la colección de Postman.

## Compilar y validar

En una terminal con Maven configurado, desde la raíz del proyecto:

```powershell
mvn clean package
```

Para validar el taller desde IntelliJ, ejecuta `Validador.java`. El validador consulta Vault, por lo que Vault debe estar iniciado y el secreto creado; no requiere que Quarkus esté ejecutándose.

## Nota de seguridad

La configuración `root` y Vault en modo desarrollo son exclusivamente para pruebas locales: Vault dev no es persistente y no debe exponerse a la red. No uses credenciales de producción ni guardes secretos reales en el código, en `application.properties` o en el repositorio. En otros entornos, configura Vault con autenticación y almacenamiento apropiados, y proporciona el token de forma segura mediante el entorno de ejecución.
