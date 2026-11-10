# Entorno y stack para esta app

## Mi entorno
- Desarrollo en Windows 11, sin Mac. No uses Xcode, CocoaPods ni comandos que requieran macOS.
- Pruebo en un iPhone 13 físico con la app Expo Go (misma red WiFi que el PC).
- Uso PowerShell como terminal.

## Stack obligatorio
- React Native con Expo (SDK más reciente estable) y TypeScript.
- Navegación con Expo Router.
- Usa solo librerías compatibles con Expo Go mientras sea posible. Si una función necesita
  código nativo que Expo Go no soporta, avísame antes y propón usar un "development build"
  con EAS en vez de hacer "expo prebuild" o "expo run:ios".
- Instala dependencias con "npx expo install" (no npm install a secas) para que las
  versiones sean compatibles con el SDK.

## Flujo de trabajo
1. Crear el proyecto con: npx create-expo-app@latest <nombre> --template
2. Probar en local con: npx expo start  (si el iPhone no conecta, usar --tunnel)
3. Compilar para iOS en la nube con EAS Build: eas build --platform ios
4. Publicar en App Store / TestFlight con: eas submit --platform ios
- Configura eas.json con perfiles development, preview y production.
- Configura app.json con nombre, bundleIdentifier (com.<mi-nombre>.<app>), icono y splash.

## Cómo quiero que trabajes
- Antes de programar, resúmeme en pocas líneas qué pantallas y funciones vas a hacer.
- Construye la app en pasos pequeños que pueda probar en el iPhone tras cada uno.
- Dime exactamente qué comando ejecutar y qué debería ver en el móvil.
- Explica en español y de forma sencilla; no soy experto.
- Si algo requiere cuenta de pago (Apple Developer 99 $/año, plan EAS de pago), avísame antes.

# Migración a android

- La aplicación podrá migrarse a android en un futuro, prioriza esto a la hora de programar.

## La app que quiero

Quiero que me ayudes a construir desde cero una app de mando a distancia para Smart TV con React Native, Expo y TypeScript, preparada para publicarse en App Store y Google Play. Controlará televisores por wifi desde iPhone y Android, y también por infrarrojos desde móviles Android que tengan un emisor compatible.

Soy principiante y trabajo en Windows. Háblame en español. Guíame desde la creación del proyecto hasta las pruebas en dispositivos reales y la publicación. Explícame qué puedes hacer tú en el proyecto y qué pasos tengo que hacer yo en el ordenador, el móvil, la tele o las consolas de las tiendas.
1. Qué problema quiero resolver
Analicé unas 2.600 reseñas negativas de 65 apps de mando para TV en España. Las quejas más frecuentes eran que la app no detectaba la tele, que pedía pagar antes de comprobar la conexión y que mostraba anuncios al pulsar botones. También se repetían tres problemas concretos que quiero resolver bien.
- En algunas teles, una pulsación cambia dos canales.
- Cuando la tele exige aceptar el emparejamiento con el mando físico, el usuario que lo ha perdido se queda bloqueado.
- Muchos usuarios esperan poder encender la tele o escribir en el buscador de una app y solo descubren las limitaciones después de pagar.
El primer hito es que pueda instalar la app en un móvil real, encontrar mi tele, conectarme y comprobar que una pulsación física en la pantalla produce exactamente una acción en la tele. Quiero validar ese recorrido antes de añadir más marcas o monetización.
2. Principios del producto
- Mando básico gratuito. Volumen, silencio, canales, flechas, OK, atrás, inicio y encendido o apagado cuando la tele los admita. Una tele guardada y su reconexión también son gratis.
- Sin anuncios en la primera versión y sin intersticiales en ninguna versión.
- Pro mediante pago único, con un precio orientativo de 3–5 €. Los extras previstos son teclado, varias teles guardadas, accesos directos a apps y widgets o controles rápidos donde sean viables. Nada de suscripciones.
- El usuario puede probar la conexión y el mando antes de ver una oferta. No se pide tarjeta al abrir la app ni una valoración antes de usarla. Antes de ofrecer Pro, indica qué extras funcionan con la tele conectada.
- Incluye un modo «He perdido el mando», accesible incluso antes de emparejar. Ofrece instrucciones por marca, alternativas verificadas y la opción de infrarrojos en Android cuando esté disponible.
- Diseña la interfaz en español e inglés, con modo claro y oscuro, botones grandes, vibración configurable y etiquetas para VoiceOver y TalkBack.
- Usa nombre, icono e identidad propios. Menciona las marcas únicamente para describir compatibilidad comprobada.
3. Stack y arquitectura
Usa React Native + Expo + TypeScript en modo estricto. Elige una versión estable del SDK de Expo y dependencias compatibles verificadas en la documentación vigente. Usa development builds con expo-dev-client para el desarrollo con módulos nativos y EAS Build para generar builds de Android e iOS. Desde Windows, explícame cómo compilar iOS en la nube, firmar la app e instalarla en un iPhone físico. Avísame en el paso exacto en que necesite una cuenta de desarrollador, registrar un dispositivo o configurar credenciales. Documentación de EAS Build

Comparte las pantallas, el estado y la lógica de los mandos en TypeScript. Para SSDP, mDNS/Bonjour, UDP, TLS con certificado de cliente, infrarrojos o extensiones del sistema, comprueba primero qué API funciona en cada plataforma. Si hace falta código nativo, usa un módulo local de Expo en Kotlin o Swift y un config plugin cuando corresponda. Los cambios nativos deben poder reproducirse al regenerar el proyecto.

Organiza el código de forma sencilla.
app/                      Pantallas y navegación con Expo Router
src/components/           Botones y componentes accesibles del mando
src/discovery/            Descubrimiento de teles y conexión por IP
src/protocols/            Interfaz TvController y adaptadores por protocolo
src/storage/              Teles guardadas, ajustes y credenciales
src/billing/              Compra única, restauración y acceso Pro
src/i18n/                 Traducciones en español e inglés
modules/                  Módulos nativos locales cuando sean necesarios
Define una interfaz TvController con connect, disconnect, sendKey, sendText, estado de conexión observable y capacidades del dispositivo. La interfaz debe distinguir entre buscando, conectando, esperando aceptación en la tele, esperando PIN, conectado y error. Guarda ajustes y metadatos por separado de tokens y claves privadas. Usa almacenamiento protegido adecuado a cada plataforma para las credenciales.

Cada gesto debe enviar una sola orden. Evita duplicados entre onPress, pulsación larga y reintentos de red, conservando dos toques rápidos intencionados. Solo el volumen se repite al mantener pulsado y debe detenerse al soltar el botón o al pasar la app a segundo plano.
4. Compatibilidad, descubrimiento y emparejamiento
Antes de implementar cada adaptador, verifica el protocolo, sus condiciones de uso para una app distribuida en tiendas y las limitaciones por modelo. Distingue marca de sistema operativo. Por ejemplo, identifica el sistema de una Sony o Xiaomi antes de elegir el adaptador. Mantén una tabla breve con modelo, plataforma móvil, funciones probadas y resultado.
Familia Implementación que debes investigar y validar
Samsung Tizen Control por WebSocket, aceptación inicial en la tele, almacenamiento del token y reconexión. Comprueba qué modelos admiten cada función.
LG webOS  Registro SSAP, confirmación en pantalla y conservación de la client-key. Comprueba el uso del socket de puntero para navegación.
Android TV / Google TV  Descubrimiento mDNS, protocolo Remote v2, emparejamiento mediante código mostrado en la tele, TLS y comandos protobuf. Verifica que la identidad del cliente sobrevive al cierre de la app.
Roku  Revisa expresamente las condiciones vigentes de ECP para apps de terceros antes de escribir este adaptador o anunciar compatibilidad. Si el uso previsto está restringido, documenta la decisión y continúa con una familia viable. Documentación oficial de Roku ECP
Infrarrojos Solo Android con emisor compatible. Detecta el hardware mediante ConsumerIrManager, declara la característica como opcional y verifica las licencias de los códigos IR.
Fire TV Déjalo para una fase posterior. Evalúa el emparejamiento y la viabilidad para un usuario doméstico antes de comprometer soporte.


Para el descubrimiento, evalúa SSDP y mDNS/Bonjour, y ofrece siempre añadir por IP. En iOS, configura la descripción del permiso de red local y los servicios Bonjour que realmente uses. Comprueba si el tráfico multicast o broadcast necesita una autorización adicional de Apple. En Android, revisa los permisos correspondientes al SDK objetivo y si el descubrimiento necesita MulticastLock. Autorización multicast de Apple

Si algo falla, distingue lo que la app ha observado de las posibles causas. Por ejemplo, si la tele responde al introducir su IP pero no aparece durante la búsqueda, permite usarla y explica que puede haber un problema con el descubrimiento. Si la tele usa HTTP local o un certificado autofirmado, limita cualquier excepción al caso necesario y evita desactivar globalmente la seguridad de red.
5. Plan de trabajo
Haz una fase cada vez. Al terminar, dame una prueba corta con un resultado observable y espera a que te confirme qué ha ocurrido en mi dispositivo.
0. Montaje desde Windows. Crea el proyecto, la estructura mínima, .gitignore, Git y los perfiles de EAS para desarrollo, pruebas y producción. Crea una pantalla inicial sencilla en español e inglés. Comprueba TypeScript y la configuración de Expo. Guíame para instalar una development build en Android por USB y otra en iPhone desde EAS. El criterio de éxito es abrir la app en cada dispositivo disponible. Si falta un iPhone, una cuenta o una credencial, deja identificada la comprobación pendiente.
1. Primera tele de extremo a extremo. Pregúntame qué marca y modelo tengo para probar. Prioriza Samsung o Roku si son viables para publicación y tengo hardware disponible. Implementa descubrimiento, IP manual, emparejamiento, mando básico y reconexión tras cerrar la app. Prueba una pulsación simple, dos toques rápidos y una pulsación larga de volumen. Completa la segunda familia viable después de validar la primera.
2. LG webOS. Añade emparejamiento, pantalla «Acepta el aviso en tu tele», navegación y recuperación cuando la tele haya eliminado la autorización.
3. Android TV / Google TV. Añade emparejamiento por código, control del mando y reconexión. Comprueba el caso de PIN incorrecto.
4. Modo «He perdido el mando» e infrarrojos. Ofrece ayuda por marca en ambas plataformas. En Android compatible, permite elegir la marca y probar un código con una pregunta concreta, como «¿Ha bajado el volumen?».
5. Funciones adicionales. Añade teclado y accesos directos cuando el televisor los admita. Verifica el teclado dentro de las apps concretas antes de anunciar compatibilidad con sus buscadores. Implementa Wake-on-LAN cuando se conozca la MAC y la tele permita encendido por red. El encendido compatible sigue siendo gratuito. Evalúa por separado widgets de iOS y Android, controles rápidos y botones físicos de volumen. Prioriza lo que pueda probarse en hardware real.
6. Compra única Pro. Implementa un producto no consumible en App Store y una compra única no consumible en Google Play. Elige una integración mantenida y compatible con Expo. Incluye restauración de compras y prueba compra correcta, cancelación, reinstalación y fallos de la tienda con las herramientas de prueba de ambas plataformas. Un fallo de facturación debe dejar disponible el mando básico.
7. Publicación en ambas tiendas. Prepara identificadores definitivos, firma, icono propio, capturas, descripciones en español e inglés, política de privacidad pública y correo de soporte. Guíame por TestFlight, las pruebas de Google Play, App Store Connect y Play Console. Prepara las declaraciones de privacidad y seguridad de datos según el comportamiento real de la app y sus SDK. Comprueba los requisitos vigentes de SDK, cuentas y pruebas cerradas que correspondan a mi caso. Incluye instrucciones de revisión para una app que necesita una tele en la misma red. Distingue subir una build, enviarla a revisión y publicarla.
6. Cómo quiero que trabajes conmigo
- Antes de escribir código, resume en pocas líneas qué vas a implementar, qué archivos tocarás y cómo sabremos que funciona.
- Escribe código claro, con nombres de tipos y funciones en inglés y comentarios en español donde ayuden a entender una decisión.
- Después de cada cambio, ejecuta las comprobaciones que correspondan. Genera una nueva build nativa cuando cambien módulos, permisos o configuración nativa. Indica exactamente qué has ejecutado y en qué plataforma. Presenta por separado el resultado de compilación y el de la prueba con una tele real.
- Dame los comandos para PowerShell en Windows, indicando desde qué carpeta ejecutarlos. Cuando yo tenga que actuar en el móvil o la tele, dame una lista corta con el resultado esperado.
- Si una decisión depende de mí, como el nombre comercial, el precio o el diseño, dame opciones y una recomendación. Usa un nombre provisional para el montaje.
- Haz un commit al terminar cada fase validada. Si falla una prueba, ayúdame a diagnosticarla antes de avanzar.
Empieza ahora únicamente por la fase 0. El primer objetivo es que pueda abrir la aplicación en mis dispositivos desde mi entorno Windows.
