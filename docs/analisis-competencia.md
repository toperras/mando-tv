# Análisis de apps de mando (Google Play, 10/11/2026)

Datos tomados de las fichas públicas de Google Play (España). Resumen propio, sin copiar textos.

| App (desarrollador) | Nota · reseñas · descargas | Conexión | Extras | Modelo de negocio |
|---|---|---|---|---|
| Control remoto para Android TV (Simha Software) | 4,6 · 227 mil · 10 M+ | Wi-Fi, solo Android TV / Google TV (mismo protocolo que el nuestro) | Voz, teclado, touchpad, accesos a apps, volumen visible, recuerda la última tele | Anuncios (pocos, según reseñas) + compras |
| Control Remoto Universal de TV (ControllaTV) | 4,7 · 55 mil · 1 M+ | Wi-Fi multimarca + **IR automático si el móvil tiene emisor** | Cast, duplicar pantalla, cambiar fuente | Anuncios + compras |
| Control Remoto para Todos TVs (BoostVision) | 4,4 · 245 mil · 10 M+ | Wi-Fi multimarca + **modo IR** | Touchpad, texto, encendido, cast, duplicar pantalla | Anuncios + compras |
| Stick – Remote Control (Ai Apps) | 4,6 · 62 mil · 5 M+ | Wi-Fi multimarca (Roku, Fire TV, Samsung, LG, Android) | Touchpad/joystick, teclado, teclado numérico, cast | Compras (sin anuncios declarados) |
| Mando Universal para tv (One Music Player) | 4,2 · 171 mil · 50 M+ | Wi-Fi + **IR** | Voz, duplicar pantalla, gestos | Anuncios + compras |
| Control Universal Para TV – MyRem (Softoria) | 4,4 · 55 mil · 10 M+ | IR integrado y Wi-Fi | Interfaz imitando mando físico | Anuncios |
| Universal TV Remote (Mind Cube) | 4,1 · 8 mil · 500 mil+ | Wi-Fi multimarca | Voz (Roku); **«sin anuncios nunca»** como reclamo | Compras |
| Control para Fire TV Stick (Remote Control TV) | 4,7 · 8 mil · 500 mil+ | Wi-Fi, solo Fire TV | Teclado, voz, reconexión rápida | Compras |
| Control Remoto para Smart TV (iKame) | 4,3 · 19 mil · 1 M+ | Wi-Fi (TCL, Hisense, Philips…) | Gestos, texto, cast | Anuncios + compras |
| UniMote (SensusTech) | 3,9 · 746 mil · 50 M+ | Wi-Fi (smart) + **IR (no smart)** | Duplicar pantalla, cast | Anuncios + compras |

## Conclusiones
1. **Nuestro enfoque Wi-Fi es el correcto para Android TV:** la app mejor valorada del segmento usa el
   mismo protocolo (Android TV Remote v2) y funciona por Wi-Fi.
2. **Casi todas las universales combinan Wi-Fi + infrarrojos** y activan el IR automáticamente si el
   móvil tiene emisor. Es su red de seguridad cuando el Wi-Fi falla o la tele no es smart.
3. Extras que diferencian a las mejor valoradas: **teclado**, **touchpad**, **accesos a apps**,
   **voz**, **volumen visible** y **reconexión automática a la última tele**.
4. Reclamos que valoran los usuarios en las reseñas: **pocos o ningún anuncio** y que «funcione a la
   primera». Coincide con nuestros principios (sin anuncios, probar antes de pagar).
5. Lo que nadie resuelve bien (oportunidad): explicar POR QUÉ no conecta (router, wifi, reposo) en vez
   de un error genérico. Nuestra app ya separa lo observado de las causas.

## Propuesta para Mando TV
- Mantener Android TV por Wi-Fi (funciona con el Google TV Streamer).
- Añadir **modo infrarrojos automático** en móviles con emisor (el Mi A3 lo tiene) como respaldo,
  y para teles no smart. Pendiente: fuente de códigos IR con licencia compatible.
- Siguientes extras por orden de valor: teclado → touchpad → accesos a apps → voz.
