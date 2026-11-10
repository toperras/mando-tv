import * as Haptics from 'expo-haptics';

import { useSettings } from '@/storage/settings';

/** Vibración corta al pulsar, solo si el usuario la tiene activada en Ajustes. */
export function useHaptics() {
  const { settings } = useSettings();

  return {
    tap: () => {
      if (settings.hapticsEnabled) {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
      }
    },
    strong: () => {
      if (settings.hapticsEnabled) {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});
      }
    },
  };
}
