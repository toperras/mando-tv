import Ionicons from '@expo/vector-icons/Ionicons';
import Constants from 'expo-constants';
import { LinearGradient } from 'expo-linear-gradient';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import {
  ActionTile,
  AppLogo,
  Card,
  HeroButton,
  IconButton,
  SectionTitle,
} from '@/components/ui';
import { Radius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { format, useTranslations } from '@/i18n';
import { type SavedTv } from '@/protocols/types';
import { loadSavedTvs } from '@/storage/tvs';

export default function HomeScreen() {
  const t = useTranslations();
  const theme = useTheme();
  const version = Constants.expoConfig?.version ?? '?';
  const [savedTv, setSavedTv] = useState<SavedTv | null>(null);

  // Se recarga cada vez que se vuelve a Inicio (por ejemplo, tras guardar u olvidar una tele).
  useFocusEffect(
    useCallback(() => {
      loadSavedTvs().then((tvs) => setSavedTv(tvs[0] ?? null));
    }, [])
  );

  const showComingSoon = (message: string) =>
    Alert.alert(t.common.comingSoonTitle, message, [{ text: t.common.ok }]);

  return (
    <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
      <ScrollView contentContainerStyle={styles.content}>
        {/* Barra superior */}
        <View style={styles.topBar}>
          <View style={styles.brand}>
            <AppLogo size={40} />
            <ThemedText type="subtitle" accessibilityRole="header">
              {t.appName}
            </ThemedText>
          </View>
          <IconButton
            icon="settings-outline"
            label={t.home.settings}
            onPress={() => router.push('/settings')}
          />
        </View>

        {/* Tarjeta principal con degradado */}
        <LinearGradient
          colors={[theme.gradientStart, theme.gradientEnd]}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.hero}>
          {/* Círculos decorativos: dan profundidad sin imágenes */}
          <View style={[styles.bubble, styles.bubbleLarge]} />
          <View style={[styles.bubble, styles.bubbleSmall]} />
          <Ionicons name="wifi" size={28} color="rgba(255,255,255,0.9)" />
          <ThemedText type="title" style={styles.heroTitle}>
            {t.home.heroTitle}
          </ThemedText>
          <ThemedText style={styles.heroSubtitle}>{t.home.heroSubtitle}</ThemedText>
          <HeroButton
            icon={savedTv ? 'game-controller' : 'search'}
            label={savedTv ? t.home.openRemote : t.home.findTv}
            onPress={() => router.push(savedTv ? '/remote' : '/add-tv')}
          />
        </LinearGradient>

        {/* Teles guardadas */}
        <SectionTitle>{t.home.myTvs}</SectionTitle>
        {savedTv ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${savedTv.name}. ${t.home.openRemote}`}
            onPress={() => router.push('/remote')}
            style={({ pressed }) => [{ transform: [{ scale: pressed ? 0.98 : 1 }] }]}>
            <Card style={styles.emptyCard}>
              <View style={[styles.emptyIcon, { backgroundColor: theme.accentSoft }]}>
                <Ionicons name="tv" size={28} color={theme.accent} />
              </View>
              <View style={styles.emptyText}>
                <ThemedText type="defaultBold">{savedTv.name}</ThemedText>
                <ThemedText type="small" themeColor="textSecondary">
                  {format(t.home.tvAddress, { ip: savedTv.ip })}
                </ThemedText>
              </View>
              <Ionicons name="chevron-forward" size={20} color={theme.textSecondary} />
            </Card>
          </Pressable>
        ) : (
          <Card style={styles.emptyCard}>
            <View style={[styles.emptyIcon, { backgroundColor: theme.surfaceRaised }]}>
              <Ionicons name="tv-outline" size={28} color={theme.textSecondary} />
            </View>
            <View style={styles.emptyText}>
              <ThemedText type="defaultBold">{t.home.noTvsTitle}</ThemedText>
              <ThemedText type="small" themeColor="textSecondary">
                {t.home.noTvsBody}
              </ThemedText>
            </View>
          </Card>
        )}

        {/* Otras acciones */}
        <SectionTitle>{t.home.more}</SectionTitle>
        <View style={styles.tiles}>
          <ActionTile
            icon="game-controller-outline"
            title={t.home.demoTitle}
            description={t.home.demoBody}
            onPress={() => router.push('/remote-demo')}
          />
          <ActionTile
            icon="help-buoy-outline"
            tone="warning"
            title={t.home.lostTitle}
            description={t.home.lostBody}
            badge={t.common.comingSoon}
            onPress={() => showComingSoon(t.home.lostSoon)}
          />
        </View>

        <ThemedText type="caption" themeColor="textSecondary" style={styles.version}>
          {format(t.home.version, { version })}
        </ThemedText>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    padding: Spacing.three + 4,
    gap: Spacing.three,
    paddingBottom: Spacing.six,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.one,
  },
  brand: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two + 4,
  },
  hero: {
    borderRadius: Radius.large,
    padding: Spacing.four,
    gap: Spacing.two + 4,
    overflow: 'hidden',
    marginBottom: Spacing.two,
  },
  bubble: {
    position: 'absolute',
    borderRadius: Radius.round,
    backgroundColor: 'rgba(255,255,255,0.10)',
  },
  bubbleLarge: {
    width: 220,
    height: 220,
    top: -80,
    right: -70,
  },
  bubbleSmall: {
    width: 120,
    height: 120,
    bottom: -50,
    right: 60,
  },
  heroTitle: {
    color: '#FFFFFF',
  },
  heroSubtitle: {
    color: 'rgba(255,255,255,0.88)',
    marginBottom: Spacing.two,
  },
  emptyCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    marginBottom: Spacing.two,
  },
  emptyIcon: {
    width: 56,
    height: 56,
    borderRadius: Radius.small,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    flex: 1,
    gap: 2,
  },
  tiles: {
    gap: Spacing.two + 4,
  },
  version: {
    textAlign: 'center',
    marginTop: Spacing.three,
  },
});
