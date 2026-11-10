import Ionicons from '@expo/vector-icons/Ionicons';
import { Stack } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { DPad, RemoteButton, Rocker } from '@/components/remote';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { type RemoteKey } from '@/protocols/keys';

/**
 * Vista previa del mando sin tele conectada. Muestra la última tecla y un
 * contador: así se puede comprobar ya que un toque cuenta una sola vez.
 */
export default function RemoteDemoScreen() {
  const t = useTranslations();
  const theme = useTheme();
  const [lastKey, setLastKey] = useState<RemoteKey | null>(null);
  const [count, setCount] = useState(0);

  const onKey = (key: RemoteKey) => {
    setLastKey(key);
    setCount((value) => value + 1);
  };

  const keys = t.remote.keys;

  return (
    <>
      <Stack.Screen options={{ title: t.remote.title }} />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={[styles.banner, { backgroundColor: theme.warningSoft }]}>
          <Ionicons name="flask-outline" size={18} color={theme.warning} />
          <ThemedText type="small" style={[styles.bannerText, { color: theme.warning }]}>
            {t.remote.demoBanner}
          </ThemedText>
        </View>

        {/* Pantallita con la última tecla: VoiceOver la anuncia al cambiar */}
        <View
          accessibilityLiveRegion="polite"
          style={[styles.display, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <ThemedText type="defaultBold">
            {lastKey ? format(t.remote.lastKey, { key: keys[lastKey] }) : t.remote.noKeyYet}
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            {format(t.remote.pressCount, { count: String(count) })}
          </ThemedText>
        </View>

        <View style={styles.topRow}>
          <RemoteButton icon="menu" label={keys.menu} remoteKey="menu" onKey={onKey} />
          <RemoteButton
            icon="power"
            label={keys.power}
            remoteKey="power"
            onKey={onKey}
            tone="danger"
          />
        </View>

        <DPad
          onKey={onKey}
          labels={{ up: keys.up, down: keys.down, left: keys.left, right: keys.right, ok: keys.ok }}
        />

        <View style={styles.navRow}>
          <RemoteButton icon="arrow-undo" label={keys.back} remoteKey="back" onKey={onKey} />
          <RemoteButton icon="home" label={keys.home} remoteKey="home" onKey={onKey} />
          <RemoteButton icon="volume-mute" label={keys.mute} remoteKey="mute" onKey={onKey} />
        </View>

        <View style={styles.rockers}>
          <Rocker
            title={t.remote.volume}
            upKey="volumeUp"
            downKey="volumeDown"
            upLabel={keys.volumeUp}
            downLabel={keys.volumeDown}
            onKey={onKey}
          />
          <Rocker
            title={t.remote.channel}
            upKey="channelUp"
            downKey="channelDown"
            upLabel={keys.channelUp}
            downLabel={keys.channelDown}
            onKey={onKey}
          />
        </View>
      </ScrollView>
    </>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: Spacing.three + 4,
    gap: Spacing.four,
    paddingBottom: Spacing.six,
  },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    padding: Spacing.three,
    borderRadius: Radius.small,
  },
  bannerText: {
    flex: 1,
  },
  display: {
    alignItems: 'center',
    padding: Spacing.three,
    borderRadius: Radius.medium,
    borderWidth: StyleSheet.hairlineWidth,
    gap: 2,
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
});
