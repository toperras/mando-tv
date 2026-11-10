import Ionicons from '@expo/vector-icons/Ionicons';
import { LinearGradient } from 'expo-linear-gradient';
import { Pressable, StyleSheet, View } from 'react-native';

import { useHaptics } from '@/components/haptics';
import { ThemedText } from '@/components/themed-text';
import { type IconName } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { type RemoteKey } from '@/protocols/keys';

type KeyHandler = (key: RemoteKey) => void;

/** Botón redondo del mando con icono y etiqueta accesible. */
export function RemoteButton({
  icon,
  label,
  remoteKey,
  onKey,
  tone = 'default',
  size = 64,
}: {
  icon: IconName;
  label: string;
  remoteKey: RemoteKey;
  onKey: KeyHandler;
  tone?: 'default' | 'danger';
  size?: number;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  const color = tone === 'danger' ? theme.danger : theme.text;
  const background = tone === 'danger' ? theme.dangerSoft : theme.surfaceRaised;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={() => {
        haptics.tap();
        onKey(remoteKey);
      }}
      style={({ pressed }) => [
        styles.roundButton,
        {
          width: size,
          height: size,
          backgroundColor: background,
          borderColor: theme.border,
          transform: [{ scale: pressed ? 0.92 : 1 }],
        },
      ]}>
      <Ionicons name={icon} size={size * 0.4} color={color} />
    </Pressable>
  );
}

/** Cruceta de navegación con botón OK central. */
export function DPad({
  onKey,
  labels,
}: {
  onKey: KeyHandler;
  labels: Record<'up' | 'down' | 'left' | 'right' | 'ok', string>;
}) {
  const theme = useTheme();
  const haptics = useHaptics();

  const arrow = (key: 'up' | 'down' | 'left' | 'right', icon: IconName, position: object) => (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={labels[key]}
      onPress={() => {
        haptics.tap();
        onKey(key);
      }}
      style={({ pressed }) => [
        styles.arrow,
        position,
        pressed && { backgroundColor: theme.accentSoft },
      ]}>
      {({ pressed }) => (
        <Ionicons name={icon} size={30} color={pressed ? theme.accent : theme.textSecondary} />
      )}
    </Pressable>
  );

  return (
    <View
      style={[styles.dpad, { backgroundColor: theme.surfaceRaised, borderColor: theme.border }]}>
      {arrow('up', 'chevron-up', styles.arrowUp)}
      {arrow('down', 'chevron-down', styles.arrowDown)}
      {arrow('left', 'chevron-back', styles.arrowLeft)}
      {arrow('right', 'chevron-forward', styles.arrowRight)}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={labels.ok}
        onPress={() => {
          haptics.strong();
          onKey('ok');
        }}
        style={({ pressed }) => [styles.okWrapper, { transform: [{ scale: pressed ? 0.94 : 1 }] }]}>
        <LinearGradient
          colors={[theme.gradientStart, theme.gradientEnd]}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.ok}>
          <ThemedText type="defaultBold" style={{ color: theme.textOnAccent }}>
            OK
          </ThemedText>
        </LinearGradient>
      </Pressable>
    </View>
  );
}

/** Balancín vertical (+ / −) para volumen o canales. */
export function Rocker({
  title,
  upKey,
  downKey,
  upLabel,
  downLabel,
  onKey,
}: {
  title: string;
  upKey: RemoteKey;
  downKey: RemoteKey;
  upLabel: string;
  downLabel: string;
  onKey: KeyHandler;
}) {
  const theme = useTheme();
  const haptics = useHaptics();

  const half = (key: RemoteKey, icon: IconName, label: string) => (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={() => {
        haptics.tap();
        onKey(key);
      }}
      style={({ pressed }) => [styles.rockerHalf, pressed && { backgroundColor: theme.accentSoft }]}>
      {({ pressed }) => (
        <Ionicons name={icon} size={26} color={pressed ? theme.accent : theme.text} />
      )}
    </Pressable>
  );

  return (
    <View
      style={[styles.rocker, { backgroundColor: theme.surfaceRaised, borderColor: theme.border }]}>
      {half(upKey, 'add', upLabel)}
      <ThemedText type="caption" themeColor="textSecondary" importantForAccessibility="no">
        {title}
      </ThemedText>
      {half(downKey, 'remove', downLabel)}
    </View>
  );
}

const DPAD_SIZE = 236;
const ARROW_SIZE = 72;
const OK_SIZE = 92;

const styles = StyleSheet.create({
  roundButton: {
    borderRadius: Radius.round,
    borderWidth: StyleSheet.hairlineWidth,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dpad: {
    width: DPAD_SIZE,
    height: DPAD_SIZE,
    borderRadius: DPAD_SIZE / 2,
    borderWidth: StyleSheet.hairlineWidth,
    alignSelf: 'center',
  },
  arrow: {
    position: 'absolute',
    width: ARROW_SIZE,
    height: ARROW_SIZE,
    borderRadius: ARROW_SIZE / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  arrowUp: { top: 4, left: (DPAD_SIZE - ARROW_SIZE) / 2 },
  arrowDown: { bottom: 4, left: (DPAD_SIZE - ARROW_SIZE) / 2 },
  arrowLeft: { left: 4, top: (DPAD_SIZE - ARROW_SIZE) / 2 },
  arrowRight: { right: 4, top: (DPAD_SIZE - ARROW_SIZE) / 2 },
  okWrapper: {
    position: 'absolute',
    top: (DPAD_SIZE - OK_SIZE) / 2,
    left: (DPAD_SIZE - OK_SIZE) / 2,
  },
  ok: {
    width: OK_SIZE,
    height: OK_SIZE,
    borderRadius: OK_SIZE / 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rocker: {
    width: 76,
    borderRadius: Radius.large + 10,
    borderWidth: StyleSheet.hairlineWidth,
    alignItems: 'center',
    paddingVertical: Spacing.one,
    gap: Spacing.one,
  },
  rockerHalf: {
    width: 68,
    height: 68,
    borderRadius: 34,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
