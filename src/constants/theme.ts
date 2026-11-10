/**
 * Colores y espaciados de la app, con variante clara y oscura.
 * Mantener contrastes altos: la app debe usarse con poca luz delante de la tele.
 */

export const Colors = {
  light: {
    text: '#11181C',
    textSecondary: '#5B6168',
    background: '#FFFFFF',
    backgroundElement: '#EEF0F3',
    accent: '#1E6FD9',
    onAccent: '#FFFFFF',
  },
  dark: {
    text: '#F2F4F7',
    textSecondary: '#A8AEB6',
    background: '#0B0D10',
    backgroundElement: '#1C1F24',
    accent: '#4C93F0',
    onAccent: '#0B0D10',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Spacing = {
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

/** Tamaño mínimo de cualquier botón táctil (botones grandes y accesibles). */
export const MinTouchSize = 56;
