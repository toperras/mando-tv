import Ionicons from '@expo/vector-icons/Ionicons';
import { router, Stack } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';

import { useHaptics } from '@/components/haptics';
import { ThemedText } from '@/components/themed-text';
import { Card, SectionTitle } from '@/components/ui';
import { MinTouchSize, Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { type Translations } from '@/i18n/translations';
import { type ConnectionErrorReason } from '@/protocols/types';
import { probeXiaomi, type XiaomiProbeResult } from '@/protocols/xiaomi-http';
import { saveTv } from '@/storage/tvs';

const IP_PATTERN = /^(25[0-5]|2[0-4]\d|1?\d?\d)(\.(25[0-5]|2[0-4]\d|1?\d?\d)){3}$/;

export default function AddTvScreen() {
  const t = useTranslations();
  const theme = useTheme();
  const haptics = useHaptics();
  const [ip, setIp] = useState('');
  const [testing, setTesting] = useState(false);
  const [result, setResult] = useState<XiaomiProbeResult | null>(null);
  const [testedIp, setTestedIp] = useState('');

  const trimmedIp = ip.trim();
  const ipValid = IP_PATTERN.test(trimmedIp);

  const test = async () => {
    if (!ipValid || testing) return;
    haptics.tap();
    setTesting(true);
    setResult(null);
    const probe = await probeXiaomi(trimmedIp);
    setTestedIp(trimmedIp);
    setResult(probe);
    setTesting(false);
    if (probe.ok) haptics.strong();
  };

  const save = async () => {
    if (!result?.ok) return;
    await saveTv({
      id: `xiaomi-http:${testedIp}`,
      name: result.deviceName ?? t.addTv.unnamedTv,
      ip: testedIp,
      protocol: 'xiaomi-http',
      addedAt: Date.now(),
    });
    router.replace('/remote');
  };

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Stack.Screen options={{ title: t.addTv.title }} />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Card style={styles.brandCard}>
          <View style={[styles.brandIcon, { backgroundColor: theme.accentSoft }]}>
            <Ionicons name="tv-outline" size={24} color={theme.accent} />
          </View>
          <View style={styles.flex}>
            <ThemedText type="defaultBold">{t.addTv.brandTitle}</ThemedText>
            <ThemedText type="small" themeColor="textSecondary">
              {t.addTv.brandBody}
            </ThemedText>
          </View>
        </Card>

        <SectionTitle>{t.addTv.ipLabel}</SectionTitle>
        <TextInput
          value={ip}
          onChangeText={(value) => {
            setIp(value);
            setResult(null);
          }}
          onSubmitEditing={test}
          placeholder={t.addTv.ipPlaceholder}
          placeholderTextColor={theme.textSecondary}
          keyboardType="decimal-pad"
          autoCorrect={false}
          autoCapitalize="none"
          returnKeyType="go"
          accessibilityLabel={t.addTv.ipLabel}
          accessibilityHint={t.addTv.ipHelp}
          style={[
            styles.input,
            { color: theme.text, backgroundColor: theme.surface, borderColor: theme.border },
          ]}
        />
        <ThemedText type="small" themeColor="textSecondary">
          {trimmedIp.length > 0 && !ipValid ? t.addTv.ipInvalid : t.addTv.ipHelp}
        </ThemedText>

        <PrimaryButton
          label={testing ? t.addTv.testing : t.addTv.test}
          icon="pulse"
          loading={testing}
          disabled={!ipValid || testing}
          onPress={test}
        />

        {result?.ok ? (
          <Card style={styles.resultCard}>
            <View style={styles.resultHeader}>
              <Ionicons name="checkmark-circle" size={28} color={theme.accent} />
              <ThemedText type="subtitle">{t.addTv.foundTitle}</ThemedText>
            </View>
            <ThemedText themeColor="textSecondary">
              {format(t.addTv.foundBody, { name: result.deviceName ?? t.addTv.unnamedTv })}
            </ThemedText>
            <PrimaryButton label={t.addTv.save} icon="game-controller" onPress={save} />
          </Card>
        ) : null}

        {result && !result.ok ? (
          <Card style={[styles.resultCard, { borderColor: theme.danger }]}>
            <View style={styles.resultHeader}>
              <Ionicons name="alert-circle" size={28} color={theme.danger} />
              <ThemedText type="defaultBold">{t.addTv.observed}</ThemedText>
            </View>
            <ThemedText>
              {format(failureTexts(t, result.reason).observed, { ip: testedIp })}
            </ThemedText>
            <ThemedText type="defaultBold" style={styles.causesTitle}>
              {t.addTv.causes}
            </ThemedText>
            <ThemedText type="small" themeColor="textSecondary">
              {failureTexts(t, result.reason).causes}
            </ThemedText>
          </Card>
        ) : null}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

/** Separa lo que la app ha observado de las posibles causas. */
function failureTexts(t: Translations, reason: ConnectionErrorReason) {
  switch (reason) {
    case 'timeout':
      return { observed: t.addTv.timeoutObserved, causes: t.addTv.timeoutCauses };
    case 'unexpectedResponse':
      return { observed: t.addTv.unexpectedObserved, causes: t.addTv.unexpectedCauses };
    case 'unreachable':
    case 'unknown':
      return { observed: t.addTv.unreachableObserved, causes: t.addTv.unreachableCauses };
  }
}

function PrimaryButton({
  label,
  icon,
  onPress,
  disabled = false,
  loading = false,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  onPress: () => void;
  disabled?: boolean;
  loading?: boolean;
}) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled, busy: loading }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.primary,
        { backgroundColor: theme.accent, opacity: disabled && !loading ? 0.4 : pressed ? 0.85 : 1 },
      ]}>
      {loading ? (
        <ActivityIndicator color={theme.textOnAccent} />
      ) : (
        <Ionicons name={icon} size={20} color={theme.textOnAccent} />
      )}
      <ThemedText type="defaultBold" style={{ color: theme.textOnAccent }}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  content: {
    padding: Spacing.three + 4,
    gap: Spacing.three,
    paddingBottom: Spacing.six,
  },
  brandCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
  },
  brandIcon: {
    width: 48,
    height: 48,
    borderRadius: Radius.small,
    alignItems: 'center',
    justifyContent: 'center',
  },
  input: {
    minHeight: MinTouchSize,
    borderWidth: 1,
    borderRadius: Radius.small,
    paddingHorizontal: Spacing.three,
    fontSize: 20,
    fontWeight: '600',
    letterSpacing: 1,
  },
  primary: {
    minHeight: MinTouchSize,
    borderRadius: Radius.round,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
    paddingHorizontal: Spacing.four,
  },
  resultCard: {
    gap: Spacing.two + 4,
  },
  resultHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  causesTitle: {
    marginTop: Spacing.one,
  },
});
