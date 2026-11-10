// Textos de la interfaz. El español es el idioma de referencia:
// el inglés debe tener exactamente las mismas claves (TypeScript lo comprueba).

export const es = {
  appName: 'Mando TV',
  home: {
    welcome: 'Tu mando a distancia para Smart TV',
    phaseNotice: 'Versión de montaje (fase 0). Aún no controla ninguna tele.',
    deviceInfo: 'Sistema: {platform} · Versión de la app: {version}',
  },
};

export type Translations = typeof es;

export const en: Translations = {
  appName: 'Mando TV',
  home: {
    welcome: 'Your Smart TV remote control',
    phaseNotice: 'Setup build (phase 0). It does not control any TV yet.',
    deviceInfo: 'System: {platform} · App version: {version}',
  },
};
