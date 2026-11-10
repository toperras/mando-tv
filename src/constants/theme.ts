/**
 * Identidad visual de la app: base azul noche, superficies limpias y un
 * degradado índigo → cian como acento. Contrastes altos para usarla con
 * poca luz delante de la tele.
 */

export type ColorScheme = 'light' | 'dark';

export const Colors = {
  light: {
    text: '#0F172A',
    textSecondary: '#5B6478',
    textOnAccent: '#FFFFFF',
    background: '#F3F5FA',
    surface: '#FFFFFF',
    surfaceRaised: '#E8ECF5',
    border: '#DCE1EC',
    accent: '#4F46E5',
    accentSoft: '#E0E7FF',
    gradientStart: '#4F46E5',
    gradientEnd: '#0891B2',
    danger: '#DC2626',
    dangerSoft: '#FEE2E2',
    warning: '#B45309',
    warningSoft: '#FEF3C7',
  },
  dark: {
    text: '#F1F5F9',
    textSecondary: '#94A3B8',
    textOnAccent: '#FFFFFF',
    background: '#070B16',
    surface: '#111827',
    surfaceRaised: '#1C2536',
    border: '#243047',
    accent: '#818CF8',
    accentSoft: '#1E1B4B',
    gradientStart: '#6366F1',
    gradientEnd: '#06B6D4',
    danger: '#F87171',
    dangerSoft: '#3B1219',
    warning: '#FBBF24',
    warningSoft: '#3A2A0A',
  },
} as const;

export type ThemeColors = { [K in keyof typeof Colors.light]: string };
export type ThemeColor = keyof ThemeColors;

export const Spacing = {
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const Radius = {
  small: 12,
  medium: 20,
  large: 28,
  round: 999,
} as const;

/** Tamaño mínimo de cualquier botón táctil (botones grandes y accesibles). */
export const MinTouchSize = 56;
