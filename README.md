# Mando TV (nombre provisional)

Mando a distancia para Smart TV por wifi (iPhone y Android) y por infrarrojos (Android con emisor IR).
React Native + Expo SDK 58 + TypeScript estricto + Expo Router.

## Estructura

```
src/app/          Pantallas y navegación (Expo Router)
src/components/   Botones y componentes accesibles
src/constants/    Colores (modo claro/oscuro) y espaciados
src/hooks/        Hooks compartidos
src/discovery/    Descubrimiento de teles y conexión por IP
src/protocols/    Interfaz TvController y adaptadores por protocolo
src/storage/      Teles guardadas, ajustes y credenciales
src/billing/      Compra única Pro
src/i18n/         Traducciones español / inglés
modules/          Módulos nativos locales (Kotlin/Swift)
```

> Expo SDK 58 coloca las pantallas en `src/app/` en lugar de `app/`. Es equivalente y deja todo el código dentro de `src/`.

## Comandos (PowerShell, desde `C:\apps\mando-tv`)

| Qué | Comando |
|---|---|
| Comprobar TypeScript | `npm run typecheck` |
| Comprobar configuración Expo | `npm run doctor` |
| Servidor de desarrollo (development build) | `npx expo start` |
| Servidor de desarrollo (Expo Go) | `npx expo start --go` |
| Si el móvil no conecta | añadir `--tunnel` |
| Build de desarrollo Android | `npx eas-cli@latest build --profile development --platform android` |
| Build de desarrollo iOS | `npx eas-cli@latest build --profile development --platform ios` |

## Perfiles de EAS (`eas.json`)

- **development**: incluye expo-dev-client; se conecta a tu PC para recargar el código al instante.
- **preview**: app normal para probar en dispositivos registrados (APK en Android).
- **production**: para subir a App Store y Google Play.
