# Mando TV (nombre provisional)

Mando a distancia para Smart TV por wifi y por infrarrojos. App nativa Android en Kotlin + Jetpack Compose.

## Estructura

```
app/            App Android: pantallas (Compose), ajustes, vibración, recursos
  src/main/kotlin/com/toperras/mandotv/
    MainActivity.kt        Navegación y tema
    data/                  Ajustes (DataStore)
    ui/theme/              Colores claro/oscuro y degradado de marca
    ui/components/         Botones del mando, tarjetas, barra superior, vibración
    ui/home|settings|remote/  Pantallas
  src/main/res/values(-es)/   Textos en inglés y español
core/           Modelo del mando y protocolos en Kotlin puro (sin android.*)
  RemoteKey.kt, TvController.kt   Teclas, estados de conexión, interfaz común
docs/           Estado del proyecto, compatibilidad y notas de protocolos
legacy-expo/    Prototipo anterior en Expo/React Native (solo referencia)
```

## Compilar e instalar en el móvil (PowerShell, desde `C:\apps\mando-tv`)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:installDebug
```

O abre la carpeta en Android Studio y pulsa ▶ Run con el móvil conectado por USB.

## Documentación
- [docs/estado.md](docs/estado.md): qué está hecho, qué falta y notas del protocolo Android TV.
- [docs/compatibilidad.md](docs/compatibilidad.md): resultados con teles reales.
