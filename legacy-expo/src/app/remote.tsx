import Ionicons from '@expo/vector-icons/Ionicons';
import { router, Stack } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';

import { RemotePanel } from '@/components/remote';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { type RemoteKey } from '@/protocols/keys';
import { type SavedTv } from '@/protocols/types';
import { useTvController } from '@/protocols/use-tv-controller';
import { loadSavedTvs, removeTv } from '@/storage/tvs';

export default function RemoteScreen() {
  const t = useTranslations();
  const theme = useTheme();
  const [tv, setTv] = useState<SavedTv | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);

  useEffect(() => {
    loadSavedTvs().then((tvs) => {
      setTv(tvs[0] ?? null);
      setLoaded(true);
    });
  }, []);

  const { controller, state, reconnect } = useTvController(tv);
  const connected = state.status === 'connected';

  const onKey = (key: RemoteKey) => {
    if (!controller || !connected) return;
    setSendError(null);
    controller.sendKey(key).catch(() => {
      setSendError(format(t.remote.sendFailed, { key: t.remote.keys[key] }));
    });
  };

  const forget = () => {
    if (!tv) return;
    Alert.alert(t.remote.forget, tv.name, [
      { text: t.common.cancel, style: 'cancel' },
      {
        text: t.remote.forget,
        style: 'destructive',
        onPress: async () => {
          await removeTv(tv.id);
          router.back();
        },
      },
    ]);
  };

  const statusColor =
    state.status === 'connected'
      ? theme.accent
      : state.status === 'error'
        ? theme.danger
        : theme.textSecondary;

  const missingKeys =
    controller && (!controller.capabilities.keys.has('mute') || !controller.capabilities.keys.has('channelUp'));

  return (
    <>
      <Stack.Screen
        options={{
          title: tv?.name ?? t.remote.title,
          headerRight: () =>
            tv ? (
              <Pressable accessibilityRole="button" accessibilityLabel={t.remote.forget} onPress={forget} hitSlop={10}>
                <Ionicons name="trash-outline" size={22} color={theme.textSecondary} />
              </Pressable>
            ) : null,
        }}
      />
      <ScrollView contentContainerStyle={styles.content}>
        {loaded && !tv ? (
          <ThemedText style={styles.center}>{t.remote.noTv}</ThemedText>
        ) : (
          <>
            {/* Estado de conexión: VoiceOver lo anuncia cuando cambia */}
            <View
              accessibilityLiveRegion="polite"
              style={[styles.status, { backgroundColor: theme.surface, borderColor: theme.border }]}>
              <View style={[styles.dot, { backgroundColor: statusColor }]} />
              <View style={styles.statusText}>
                <ThemedText type="defaultBold">{t.remote.status[state.status]}</ThemedText>
                {tv ? (
                  <ThemedText type="small" themeColor="textSecondary">
                    {format(t.home.tvAddress, { ip: tv.ip })}
                  </ThemedText>
                ) : null}
              </View>
              {state.status === 'error' ? (
                <Pressable
                  accessibilityRole="button"
                  onPress={reconnect}
                  style={[styles.retry, { backgroundColor: theme.accentSoft }]}>
                  <ThemedText type="small" style={{ color: theme.accent, fontWeight: '700' }}>
                    {t.remote.retry}
                  </ThemedText>
                </Pressable>
              ) : null}
            </View>

            {sendError ? (
              <ThemedText type="small" style={[styles.center, { color: theme.danger }]}>
                {sendError}
              </ThemedText>
            ) : null}

            <RemotePanel
              onKey={onKey}
              t={t}
              supportedKeys={controller?.capabilities.keys}
              enabled={connected}
            />

            {missingKeys ? (
              <ThemedText type="small" themeColor="textSecondary" style={styles.center}>
                {t.remote.unsupportedNote}
              </ThemedText>
            ) : null}
          </>
        )}
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
  center: {
    textAlign: 'center',
  },
  status: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    padding: Spacing.three,
    borderRadius: Radius.medium,
    borderWidth: StyleSheet.hairlineWidth,
  },
  dot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  statusText: {
    flex: 1,
  },
  retry: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    borderRadius: Radius.round,
  },
});
