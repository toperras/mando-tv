import Ionicons from '@expo/vector-icons/Ionicons';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef } from 'react';
import { AppState, Pressable, StyleSheet, View } from 'react-native';

import { useHaptics } from '@/components/haptics';
import { ThemedText } from '@/components/themed-text';
import { type IconName } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { type Translations } from '@/i18n/translations';
import { type RemoteKey } from '@/protocols/keys';

type KeyHandler = (key: RemoteKey) => void;

/*
 * Regla de oro del mando: cada gesto envía UNA orden.
 * - Toque normal → onPress (se dispara al soltar, una vez).
 * - Pulsación larga → solo los botones de volumen repiten; el resto ignora
 *   la pulsación larga, así nunca se suma un onPress extra.
 * - Dos toques rápidos → dos onPress, como espera el usuario.
 */

const LONG_PRESS_MS = 400;
const REPEAT_INTERVAL_MS = 180;

/**
 * Repite una tecla mientras se mantiene pulsado. Se detiene al soltar,
 * al desmontar la pantalla o cuando la app pasa a segundo plano.
 */
function useHoldRepeat(onRepeat: () => void) {
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);
  const callback = useRef(onRepeat);
  useEffect(() => {
    callback.current = onRepeat;
  });

  const stop = () => {
    if (timer.current) {
      clearInterval(timer.current);
      timer.current = null;
    }
  };

  const start = () => {
    stop();
    callback.current();
    timer.current = setInterval(() => callback.current(), REPEAT_INTERVAL_MS);
  };

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (state) => {
      if (state !== 'active') stop();
    });
    return () => {
      subscription.remove();
      stop();
    };
  }, []);

  return { start, stop };
}

/** Botón redondo del mando con icono y etiqueta accesible. */
export function RemoteButton({
  icon,
  label,
  remoteKey,
  onKey,
  tone = 'default',
  size = 64,
  disabled = false,
}: {
  icon: IconName;
  label: string;
  remoteKey: RemoteKey;
  onKey: KeyHandler;
  tone?: 'default' | 'danger';
  size?: number;
  disabled?: boolean;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  const color = tone === 'danger' ? theme.danger : theme.text;
  const background = tone === 'danger' ? theme.dangerSoft : theme.surfaceRaised;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled }}
      disabled={disabled}
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
          opacity: disabled ? 0.35 : 1,
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
  disabled = false,
}: {
  onKey: KeyHandler;
  labels: Record<'up' | 'down' | 'left' | 'right' | 'ok', string>;
  disabled?: boolean;
}) {
  const theme = useTheme();
  const haptics = useHaptics();

  const arrow = (key: 'up' | 'down' | 'left' | 'right', icon: IconName, position: object) => (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={labels[key]}
      accessibilityState={{ disabled }}
      disabled={disabled}
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
      style={[
        styles.dpad,
        {
          backgroundColor: theme.surfaceRaised,
          borderColor: theme.border,
          opacity: disabled ? 0.35 : 1,
        },
      ]}>
      {arrow('up', 'chevron-up', styles.arrowUp)}
      {arrow('down', 'chevron-down', styles.arrowDown)}
      {arrow('left', 'chevron-back', styles.arrowLeft)}
      {arrow('right', 'chevron-forward', styles.arrowRight)}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={labels.ok}
        accessibilityState={{ disabled }}
        disabled={disabled}
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

/** Mitad de un balancín (+ o −). Si repeatOnHold, repite al mantener pulsado. */
function RockerHalf({
  remoteKey,
  icon,
  label,
  onKey,
  repeatOnHold,
  disabled,
}: {
  remoteKey: RemoteKey;
  icon: IconName;
  label: string;
  onKey: KeyHandler;
  repeatOnHold: boolean;
  disabled: boolean;
}) {
  const theme = useTheme();
  const haptics = useHaptics();
  const repeater = useHoldRepeat(() => {
    haptics.tap();
    onKey(remoteKey);
  });

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled }}
      disabled={disabled}
      delayLongPress={LONG_PRESS_MS}
      onPress={() => {
        haptics.tap();
        onKey(remoteKey);
      }}
      // Sin repeatOnHold, la pulsación larga no hace nada (no envía extra).
      onLongPress={repeatOnHold ? repeater.start : () => {}}
      onPressOut={repeater.stop}
      style={({ pressed }) => [styles.rockerHalf, pressed && { backgroundColor: theme.accentSoft }]}>
      {({ pressed }) => (
        <Ionicons name={icon} size={26} color={pressed ? theme.accent : theme.text} />
      )}
    </Pressable>
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
  repeatOnHold = false,
  disabled = false,
}: {
  title: string;
  upKey: RemoteKey;
  downKey: RemoteKey;
  upLabel: string;
  downLabel: string;
  onKey: KeyHandler;
  repeatOnHold?: boolean;
  disabled?: boolean;
}) {
  const theme = useTheme();
  return (
    <View
      style={[
        styles.rocker,
        {
          backgroundColor: theme.surfaceRaised,
          borderColor: theme.border,
          opacity: disabled ? 0.35 : 1,
        },
      ]}>
      <RockerHalf
        remoteKey={upKey}
        icon="add"
        label={upLabel}
        onKey={onKey}
        repeatOnHold={repeatOnHold}
        disabled={disabled}
      />
      <ThemedText type="caption" themeColor="textSecondary" importantForAccessibility="no">
        {title}
      </ThemedText>
      <RockerHalf
        remoteKey={downKey}
        icon="remove"
        label={downLabel}
        onKey={onKey}
        repeatOnHold={repeatOnHold}
        disabled={disabled}
      />
    </View>
  );
}

/**
 * Mando completo. Los botones que la tele no admite (supportedKeys) o
 * todos, si enabled es false, aparecen atenuados y desactivados.
 */
export function RemotePanel({
  onKey,
  t,
  supportedKeys,
  enabled = true,
}: {
  onKey: KeyHandler;
  t: Translations;
  supportedKeys?: ReadonlySet<RemoteKey>;
  enabled?: boolean;
}) {
  const keys = t.remote.keys;
  const off = (key: RemoteKey) => !enabled || (supportedKeys ? !supportedKeys.has(key) : false);

  return (
    <View style={styles.panel}>
      <View style={styles.topRow}>
        <RemoteButton icon="menu" label={keys.menu} remoteKey="menu" onKey={onKey} disabled={off('menu')} />
        <RemoteButton
          icon="power"
          label={keys.power}
          remoteKey="power"
          onKey={onKey}
          tone="danger"
          disabled={off('power')}
        />
      </View>

      <DPad
        onKey={onKey}
        disabled={off('ok')}
        labels={{ up: keys.up, down: keys.down, left: keys.left, right: keys.right, ok: keys.ok }}
      />

      <View style={styles.navRow}>
        <RemoteButton icon="arrow-undo" label={keys.back} remoteKey="back" onKey={onKey} disabled={off('back')} />
        <RemoteButton icon="home" label={keys.home} remoteKey="home" onKey={onKey} disabled={off('home')} />
        <RemoteButton icon="volume-mute" label={keys.mute} remoteKey="mute" onKey={onKey} disabled={off('mute')} />
      </View>

      <View style={styles.rockers}>
        <Rocker
          title={t.remote.volume}
          upKey="volumeUp"
          downKey="volumeDown"
          upLabel={keys.volumeUp}
          downLabel={keys.volumeDown}
          onKey={onKey}
          repeatOnHold
          disabled={off('volumeUp')}
        />
        <Rocker
          title={t.remote.channel}
          upKey="channelUp"
          downKey="channelDown"
          upLabel={keys.channelUp}
          downLabel={keys.channelDown}
          onKey={onKey}
          disabled={off('channelUp')}
        />
      </View>
    </View>
  );
}

const DPAD_SIZE = 236;
const ARROW_SIZE = 72;
const OK_SIZE = 92;

const styles = StyleSheet.create({
  panel: {
    gap: Spacing.four,
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.two,
  },
  navRow: {
    flexDirection: 'row',
    justifyContent: 'space-evenly',
  },
  rockers: {
    flexDirection: 'row',
    justifyContent: 'space-evenly',
  },
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
