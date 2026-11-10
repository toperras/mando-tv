# Tabla de compatibilidad

Solo se anuncia compatibilidad con lo probado en hardware real.

| Aparato (modelo) | Sistema | Protocolo | Móvil | Funciones probadas | Resultado | Fecha |
|---|---|---|---|---|---|---|
| Google TV Streamer | Google TV | Android TV Remote v2 | Mi A3 (Android 11), app Kotlin | Búsqueda mDNS, emparejamiento con código, conexión, reconexión tras cerrar la app | ✅ Funciona | 2026-11-10 |
| Google TV Streamer | Google TV | Android TV Remote v2 | Mi A3 | Flechas, OK, atrás, inicio; reconexión automática tras cortar la wifi | ✅ Funciona (confirmado por el usuario en la tele) | 2026-11-10 |
| Google TV Streamer | Google TV | Android TV Remote v2 | Mi A3 | Volumen y silencio | ❌ No hacen nada. El Streamer informa de volumen vacío (sin máximo ni nivel) y no envía cambios ni cuando se usa su mando físico: el volumen lo cambia el mando directamente en la tele (probablemente IR). Posible solución sin probar: activar el control de volumen por HDMI-CEC en el Streamer y la tele | 2026-11-10 |
| DIGI R2A | Android TV | Android TV Remote v2 | Mi A3 | Búsqueda mDNS (aparece en la lista) | ✅ Visto; sin emparejar | 2026-11-10 |
| Xiaomi Mi TV 4A 32" (MiTV-MSSP2) | Android TV | Android TV Remote v2 | Mi A3 | Búsqueda mDNS | ✅ Visto. El móvil no llega a la tele (ARP falla): tele y móvil en wifis distintas («normal» / «Plus») | 2026-11-10 |
| Xiaomi Mi TV 4A 32" (MiTV-MSSP2) | Android TV | Xiaomi HTTP (puerto 6095) | PC | Puerto 6095 | ❌ Cerrado: este modelo no tiene ese servicio | 2026-11-10 |

## Notas por protocolo

- **Android TV Remote v2 (6466/6467):** emparejamiento con código hexadecimal de 6 caracteres
  y TLS con certificado de cliente. En Android, la clave del AndroidKeyStore necesita
  `ENCRYPTION_PADDING_NONE` para que Conscrypt pueda usarla en el handshake (si no, falla con
  «RSA routines: internal error»).
- **Xiaomi HTTP (6095):** no documentado oficialmente (pymitv). Solo en modelos PatchWall antiguos.
  Implementado únicamente en el prototipo Expo (`legacy-expo/`).
