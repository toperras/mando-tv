import { getLocales } from 'expo-localization';

import { en, es, type Translations } from './translations';

export type Language = 'es' | 'en';

/**
 * Elige el idioma según el primer idioma preferido del móvil.
 * Si no es español, se usa inglés.
 */
export function detectLanguage(): Language {
  const code = getLocales()[0]?.languageCode ?? 'en';
  return code === 'es' ? 'es' : 'en';
}

const dictionaries: Record<Language, Translations> = { es, en };

/** Devuelve los textos del idioma actual del dispositivo. */
export function useTranslations(): Translations {
  // De momento el idioma no cambia en caliente; en una fase posterior
  // se podrá elegir manualmente en Ajustes.
  return dictionaries[detectLanguage()];
}

/** Sustituye marcadores como {version} por su valor. */
export function format(template: string, values: Record<string, string>): string {
  return template.replace(/\{(\w+)\}/g, (match, key: string) => values[key] ?? match);
}
