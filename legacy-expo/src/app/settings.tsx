import Ionicons from '@expo/vector-icons/Ionicons';
import Constants from 'expo-constants';
import { Stack } from 'expo-router';
import { ScrollView, StyleSheet, Switch, View } from 'react-native';

import { useHaptics } from '@/components/haptics';
import { ThemedText } from '@/components/themed-text';
import { Card, SectionTitle, SegmentedControl, type IconName } from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { useSettings, type ThemePreference } from '@/storage/settings';

export default function SettingsScreen() {
  const t = useTranslations();
  const theme = useTheme();
  const haptics = useHaptics();
  const { settings, updateSettings } = useSettings();
  const version = Constants.expoConfig?.version ?? '?';

  const themeOptions: { value: ThemePreference; label: string; icon: IconName }[] = [
    { value: 'system', label: t.settings.themeSystem, icon: 'phone-portrait-outline' },
    { value: 'light', label: t.settings.themeLight, icon: 'sunny-outline' },
    { value: 'dark', label: t.settings.themeDark, icon: 'moon-outline' },
  ];

  return (
    <>
      <Stack.Screen options={{ title: t.settings.title }} />
      <ScrollView contentContainerStyle={styles.content}>
        <SectionTitle>{t.settings.appearance}</SectionTitle>
        <SegmentedControl
          options={themeOptions}
          value={settings.themePreference}
          onChange={(value) => updateSettings({ themePreference: value })}
        />

        <SectionTitle>{t.settings.feedback}</SectionTitle>
        <Card style={styles.row}>
          <RowIcon icon="phone-portrait-outline" />
          <View style={styles.rowText}>
            <ThemedText type="defaultBold">{t.settings.haptics}</ThemedText>
            <ThemedText type="small" themeColor="textSecondary">
              {t.settings.hapticsBody}
            </ThemedText>
          </View>
          <Switch
            accessibilityLabel={t.settings.haptics}
            value={settings.hapticsEnabled}
            trackColor={{ true: theme.accent, false: theme.surfaceRaised }}
            onValueChange={(value) => {
              updateSettings({ hapticsEnabled: value });
              // Al activarla, vibra una vez para que el usuario note el efecto.
              if (value) haptics.strong();
            }}
          />
        </Card>

        <SectionTitle>{t.settings.language}</SectionTitle>
        <Card style={styles.row}>
          <RowIcon icon="language-outline" />
          <ThemedText type="small" themeColor="textSecondary" style={styles.rowText}>
            {t.settings.languageBody}
          </ThemedText>
        </Card>

        <SectionTitle>{t.settings.about}</SectionTitle>
        <Card style={styles.row}>
          <RowIcon icon="information-circle-outline" />
          <ThemedText style={styles.rowText}>
            {t.appName} · {format(t.home.version, { version })}
          </ThemedText>
        </Card>
      </ScrollView>
    </>
  );
}

function RowIcon({ icon }: { icon: IconName }) {
  const theme = useTheme();
  return (
    <View style={[styles.rowIcon, { backgroundColor: theme.accentSoft }]}>
      <Ionicons name={icon} size={20} color={theme.accent} />
    </View>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: Spacing.three + 4,
    gap: Spacing.three,
    paddingBottom: Spacing.six,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    marginBottom: Spacing.two,
  },
  rowIcon: {
    width: 40,
    height: 40,
    borderRadius: Radius.small,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rowText: {
    flex: 1,
    gap: 2,
  },
});
