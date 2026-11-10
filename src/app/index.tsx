import Constants from 'expo-constants';
import { Platform, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { format, useTranslations } from '@/i18n';

export default function HomeScreen() {
  const t = useTranslations();
  const version = Constants.expoConfig?.version ?? '?';

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.hero}>
          <ThemedText type="title" accessibilityRole="header">
            {t.appName}
          </ThemedText>
          <ThemedText type="subtitle" style={styles.center}>
            {t.home.welcome}
          </ThemedText>
        </View>

        <ThemedView type="backgroundElement" style={styles.card}>
          <ThemedText style={styles.center}>{t.home.phaseNotice}</ThemedText>
          <ThemedText type="small" themeColor="textSecondary" style={styles.center}>
            {format(t.home.deviceInfo, { platform: Platform.OS, version })}
          </ThemedText>
        </ThemedView>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    padding: Spacing.four,
    gap: Spacing.four,
  },
  hero: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
  },
  center: {
    textAlign: 'center',
  },
  card: {
    padding: Spacing.four,
    borderRadius: Spacing.four,
    gap: Spacing.two,
  },
});
