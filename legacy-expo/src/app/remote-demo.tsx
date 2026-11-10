import Ionicons from '@expo/vector-icons/Ionicons';
import { Stack } from 'expo-router';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { RemotePanel } from '@/components/remote';
import { ThemedText } from '@/components/themed-text';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { type RemoteKey } from '@/protocols/keys';

/**
 * Vista previa del mando sin tele conectada. Muestra la última tecla y un
 * contador: así se puede comprobar que un toque cuenta una sola vez.
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

        <View
          accessibilityLiveRegion="polite"
          style={[styles.display, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <ThemedText type="defaultBold">
            {lastKey
              ? format(t.remote.lastKey, { key: t.remote.keys[lastKey] })
              : t.remote.noKeyYet}
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            {format(t.remote.pressCount, { count: String(count) })}
          </ThemedText>
        </View>

        <RemotePanel onKey={onKey} t={t} />
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
});
