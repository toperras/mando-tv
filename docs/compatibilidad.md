# Tabla de compatibilidad

Solo se anuncia compatibilidad con lo probado en hardware real.

| Tele (modelo) | Sistema | Protocolo | Móvil | Funciones probadas | Resultado | Fecha |
|---|---|---|---|---|---|---|
| Xiaomi Mi TV 4A 32" LED HD (MiTV-MSSP2) | Android TV (Cast 3.72) | Xiaomi HTTP (puerto 6095) | PC (escaneo de puertos) | Puerto 6095 | ❌ Cerrado: este modelo no tiene el servicio | 2026-11-10 |
| Xiaomi Mi TV 4A 32" LED HD (MiTV-MSSP2) | Android TV | Android TV Remote v2 (6466/6467) | — | Puertos abiertos | Pendiente: requiere development build | — |

## Notas por protocolo

- **Xiaomi HTTP (6095):** no documentado oficialmente; basado en pymitv. Teclas: power, flechas,
  enter, home, back, menu, volumeup, volumedown. Sin silencio ni canales. No enciende la tele
  si está totalmente apagada. Funciona en Expo Go.
- **Android TV Remote v2 (6466/6467):** necesario para Xiaomi con Android TV / Google TV.
  Requiere TLS con certificado de cliente → development build (no Expo Go).
