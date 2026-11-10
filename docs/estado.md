# Estado del proyecto (10/11/2026)

## Hecho y verificado
| Parte | Estado | Verificado en |
|---|---|---|
| Fase 0: proyecto Expo SDK 58 + TypeScript + Expo Router | ✅ | iPhone 13 con Expo Go |
| Diseño (degradado índigo-cian), tema Sistema/Claro/Oscuro, vibración | ✅ | Navegador integrado (tamaño móvil); en iPhone sin confirmar |
| Mando de demostración con contador de pulsaciones | ✅ | Navegador integrado |
| Interfaz `TvController` (`src/protocols/types.ts`) | ✅ | TypeScript |
| Adaptador Xiaomi HTTP puerto 6095 + pantalla «Conectar tele» | ✅ código | Navegador (caso de error). **No sirve para la tele del usuario** |

## Descubrimiento clave
La tele de pruebas, **Xiaomi Mi TV 4A 32" (MiTV-MSSP2, IP 192.168.1.132)**, usa **Android TV**.
No tiene el puerto 6095. Tiene abiertos 6466/6467 (Android TV Remote v2) y 8008/8009 (Cast).
En la red también hay un **Google TV Streamer** (.129) y un **DIGI R2A** (.128), los dos Android TV.
→ Hace falta el protocolo Android TV Remote v2, que **no funciona en Expo Go** (TLS con certificado de cliente).

## A medias (sin compilar ni probar)
`modules/tv-tls-socket/`: módulo nativo local en Kotlin para Android.
- `ensureClientIdentity(alias)`: crea una clave RSA 2048 y un certificado autofirmado (CN=atvremote) en AndroidKeyStore y devuelve el módulo y el exponente en hexadecimal.
- `connect(id, host, port, alias, timeoutMs)`: abre TLS con el certificado de cliente y acepta el certificado de la tele solo en ese socket. Devuelve el módulo, el exponente y la huella SHA-256 de la tele.
- `write(id, base64)`, `close(id)`, eventos `onData` / `onClose`.
- Pendiente: el `index.ts` del lado JS, compilarlo, y la lógica del protocolo en TypeScript.

⚠️ Si se genera una build de Expo, este módulo se compilará automáticamente. Si falla, moverlo fuera de `modules/`.

## Decisión pendiente
¿Seguir con Expo (código compartido) o hacer apps nativas separadas (Kotlin para Android y,
más adelante, Swift para iOS)? Ver la conversación del 10/11/2026. El módulo Kotlin y estas notas
sirven en ambos casos.

## Protocolo Android TV Remote v2 (notas para implementarlo)
Referencias: tronikos/androidtvremote2 (Python), kud/androidtv-remote (Node), AOSP pairing protocol.

**Mensajes:** protobuf, cada uno precedido por su longitud en varint.

### Emparejamiento (puerto 6467, TLS con certificado de cliente)
1. → `PairingMessage{protocol_version=2, status=200, pairing_request(10){service_name(1)="atvremote", client_name(2)="Mando TV"}}`
2. ← `pairing_request_ack(11)`
3. → `pairing_option(20){input_encodings(1)=[{type(1)=3 HEX, symbol_length(2)=6}], preferred_role(3)=1 INPUT}`
4. ← `pairing_option`
5. → `pairing_configuration(30){encoding(1)={3,6}, client_role(2)=1}`
6. ← `pairing_configuration_ack(31)` → **la tele muestra un código de 6 caracteres hexadecimales**
7. secreto = SHA-256(módulo_cliente, exponente_cliente, módulo_tele, exponente_tele, bytes(código[2..6])).
   El exponente va como bytes `01 00 01` (hex "010001"); el módulo, sin el 00 inicial.
   **Comprobación local del PIN:** el primer byte del hash debe coincidir con bytes(código[0..2]). Si no coincide, el código es incorrecto y no se envía.
8. → `pairing_secret(40){secret(1)=hash}`
9. ← `pairing_secret_ack(41)` con status 200 = emparejado. Status ≠ 200 = rechazado (400 error, 401 mala configuración, 402 secreto incorrecto).

### Mando (puerto 6466, mismo certificado de cliente)
`RemoteMessage`: remote_configure=1, remote_set_active=2, remote_error=3, remote_ping_request=8,
remote_ping_response=9, remote_key_inject=10, remote_ime_key_inject=20, remote_start=40,
remote_set_volume_level=50, remote_app_link_launch_request=90.
1. ← `remote_configure` → responder `remote_configure{code1(1)=622, device_info(2){model(1), vendor(2), unknown1(3)=1, unknown2(4)="1", package_name(5), app_version(6)}}`
2. ← `remote_set_active` → responder `remote_set_active{active(1)=622}`
3. ← `remote_ping_request{val1(1)}` → responder **siempre** `remote_ping_response{val1(1)=val1}`, o la tele corta.
4. Tecla: `remote_key_inject{key_code(1), direction(2)=3 SHORT}` (1 START_LONG, 2 END_LONG).
5. Si el TLS falla en 6466 con una tele ya emparejada → la tele borró la autorización → volver a emparejar.

**Códigos de tecla (Android KeyEvent):** POWER 26, DPAD_UP 19, DPAD_DOWN 20, DPAD_LEFT 21,
DPAD_RIGHT 22, DPAD_CENTER 23, BACK 4, HOME 3, MENU 82, VOLUME_UP 24, VOLUME_DOWN 25,
VOLUME_MUTE 164, CHANNEL_UP 166, CHANNEL_DOWN 167.

**Descubrimiento:** mDNS `_androidtvremote2._tcp` (Android: NsdManager + MulticastLock).
Identificar la tele: `http://IP:8008/ssdp/device-desc.xml` (fabricante y modelo).
