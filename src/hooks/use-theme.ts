import { useColorScheme } from 'react-native';

import { Colors, type ColorScheme, type ThemeColors } from '@/constants/theme';
import { useSettings } from '@/storage/settings';

/** Esquema activo: el elegido en Ajustes o, en modo «Sistema», el del móvil. */
export function useColorSchemeSetting(): ColorScheme {
  const system = useColorScheme();
  const { settings } = useSettings();
  if (settings.themePreference === 'system') {
    return system === 'dark' ? 'dark' : 'light';
  }
  return settings.themePreference;
}

export function useTheme(): ThemeColors {
  return Colors[useColorSchemeSetting()];
}
