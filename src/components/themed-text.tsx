import { StyleSheet, Text, type TextProps } from 'react-native';

import { type ThemeColor } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type ThemedTextProps = TextProps & {
  type?: 'default' | 'title' | 'subtitle' | 'small';
  themeColor?: ThemeColor;
};

/** Texto que usa el color del tema y respeta el tamaño de letra del sistema. */
export function ThemedText({ style, type = 'default', themeColor, ...rest }: ThemedTextProps) {
  const theme = useTheme();
  return <Text style={[{ color: theme[themeColor ?? 'text'] }, styles[type], style]} {...rest} />;
}

const styles = StyleSheet.create({
  default: {
    fontSize: 17,
    lineHeight: 24,
    fontWeight: '500',
  },
  small: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '500',
  },
  title: {
    fontSize: 40,
    lineHeight: 48,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 22,
    lineHeight: 30,
    fontWeight: '600',
  },
});
