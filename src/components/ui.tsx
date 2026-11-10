import Ionicons from '@expo/vector-icons/Ionicons';
import { LinearGradient } from 'expo-linear-gradient';
import { type ComponentProps, type ReactNode } from 'react';
import { Pressable, StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { useHaptics } from '@/components/haptics';
import { ThemedText } from '@/components/themed-text';
import { MinTouchSize, Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type IconName = ComponentProps<typeof Ionicons>['name'];

/** Tarjeta con fondo de superficie y borde suave. */
export function Card({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  const theme = useTheme();
  return (
    <View
      style={[
        styles.card,
        { backgroundColor: theme.surface, borderColor: theme.border },
        style,
      ]}>
      {children}
    </View>
  );
}

/** Logo provisional: icono de tele sobre el degradado de la marca. */
export function AppLogo({ size = 40 }: { size?: number }) {
  const theme = useTheme();
  return (
    <LinearGradient
      colors={[theme.gradientStart, theme.gradientEnd]}
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={{
        width: size,
        height: size,
        borderRadius: size * 0.3,
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      <Ionicons name="tv" size={size * 0.55} color="#FFFFFF" />
    </LinearGradient>
  );
}

/** Botón circular solo con icono (por ejemplo, el engranaje de Ajustes). */
export function IconButton({
  icon,
  label,
  onPress,
}: {
  icon: IconName;
  label: string;
  onPress: () => void;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      hitSlop={8}
      onPress={() => {
        haptics.tap();
        onPress();
      }}
      style={({ pressed }) => [
        styles.iconButton,
        { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.7 : 1 },
      ]}>
      <Ionicons name={icon} size={22} color={theme.text} />
    </Pressable>
  );
}

/** Botón principal: fondo blanco pensado para ir encima del degradado. */
export function HeroButton({
  icon,
  label,
  onPress,
}: {
  icon: IconName;
  label: string;
  onPress: () => void;
}) {
  const haptics = useHaptics();
  return (
    <Pressable
      accessibilityRole="button"
      onPress={() => {
        haptics.tap();
        onPress();
      }}
      style={({ pressed }) => [styles.heroButton, { opacity: pressed ? 0.85 : 1 }]}>
      <Ionicons name={icon} size={20} color="#312E81" />
      <ThemedText style={styles.heroButtonText}>{label}</ThemedText>
    </Pressable>
  );
}

/** Fila pulsable con icono, título, descripción y flecha. */
export function ActionTile({
  icon,
  title,
  description,
  tone = 'accent',
  badge,
  onPress,
}: {
  icon: IconName;
  title: string;
  description: string;
  tone?: 'accent' | 'warning';
  badge?: string;
  onPress: () => void;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  const iconColor = tone === 'warning' ? theme.warning : theme.accent;
  const iconBackground = tone === 'warning' ? theme.warningSoft : theme.accentSoft;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${title}. ${description}`}
      onPress={() => {
        haptics.tap();
        onPress();
      }}
      style={({ pressed }) => [
        styles.tile,
        {
          backgroundColor: theme.surface,
          borderColor: theme.border,
          transform: [{ scale: pressed ? 0.98 : 1 }],
        },
      ]}>
      <View style={[styles.tileIcon, { backgroundColor: iconBackground }]}>
        <Ionicons name={icon} size={24} color={iconColor} />
      </View>
      <View style={styles.tileText}>
        <View style={styles.tileTitleRow}>
          <ThemedText type="defaultBold">{title}</ThemedText>
          {badge ? (
            <View style={[styles.badge, { backgroundColor: theme.surfaceRaised }]}>
              <ThemedText type="caption" themeColor="textSecondary">
                {badge}
              </ThemedText>
            </View>
          ) : null}
        </View>
        <ThemedText type="small" themeColor="textSecondary">
          {description}
        </ThemedText>
      </View>
      <Ionicons name="chevron-forward" size={20} color={theme.textSecondary} />
    </Pressable>
  );
}

/** Selector de varias opciones en una sola fila (por ejemplo, el tema). */
export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { value: T; label: string; icon: IconName }[];
  value: T;
  onChange: (value: T) => void;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  return (
    <View
      accessibilityRole="radiogroup"
      style={[styles.segmented, { backgroundColor: theme.surfaceRaised }]}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <Pressable
            key={option.value}
            accessibilityRole="radio"
            accessibilityState={{ selected }}
            accessibilityLabel={option.label}
            onPress={() => {
              haptics.tap();
              onChange(option.value);
            }}
            style={[
              styles.segment,
              selected && {
                backgroundColor: theme.surface,
                borderColor: theme.border,
                borderWidth: StyleSheet.hairlineWidth,
              },
            ]}>
            <Ionicons
              name={option.icon}
              size={20}
              color={selected ? theme.accent : theme.textSecondary}
            />
            <ThemedText type="small" themeColor={selected ? 'text' : 'textSecondary'}>
              {option.label}
            </ThemedText>
          </Pressable>
        );
      })}
    </View>
  );
}

/** Título de sección en mayúsculas pequeñas. */
export function SectionTitle({ children }: { children: string }) {
  return (
    <ThemedText type="caption" themeColor="textSecondary" style={styles.sectionTitle}>
      {children.toUpperCase()}
    </ThemedText>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: Radius.medium,
    borderWidth: StyleSheet.hairlineWidth,
    padding: Spacing.three,
  },
  iconButton: {
    width: 44,
    height: 44,
    borderRadius: Radius.round,
    borderWidth: StyleSheet.hairlineWidth,
    alignItems: 'center',
    justifyContent: 'center',
  },
  heroButton: {
    minHeight: MinTouchSize - 4,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
    backgroundColor: '#FFFFFF',
    borderRadius: Radius.round,
    paddingHorizontal: Spacing.four,
    alignSelf: 'flex-start',
  },
  heroButtonText: {
    color: '#312E81',
    fontWeight: '700',
  },
  tile: {
    minHeight: MinTouchSize + 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: Radius.medium,
    borderWidth: StyleSheet.hairlineWidth,
  },
  tileIcon: {
    width: 48,
    height: 48,
    borderRadius: Radius.small,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tileText: {
    flex: 1,
    gap: 2,
  },
  tileTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    flexWrap: 'wrap',
  },
  badge: {
    paddingHorizontal: Spacing.two,
    paddingVertical: 2,
    borderRadius: Radius.round,
  },
  segmented: {
    flexDirection: 'row',
    borderRadius: Radius.small + 4,
    padding: 4,
    gap: 4,
  },
  segment: {
    flex: 1,
    minHeight: MinTouchSize,
    borderRadius: Radius.small,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },
  sectionTitle: {
    letterSpacing: 1,
    marginLeft: Spacing.one,
  },
});
