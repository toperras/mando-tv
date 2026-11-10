import AsyncStorage from '@react-native-async-storage/async-storage';

import { type SavedTv } from '@/protocols/types';

// Teles guardadas. En la versión gratuita se guarda una; la lista ya está
// preparada para «varias teles» de la versión Pro.
const STORAGE_KEY = 'tvs.v1';

export async function loadSavedTvs(): Promise<SavedTv[]> {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as SavedTv[]) : [];
  } catch {
    return [];
  }
}

/** Guarda la tele. Por ahora sustituye a la anterior (una tele gratis). */
export async function saveTv(tv: SavedTv): Promise<void> {
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify([tv]));
}

export async function removeTv(id: string): Promise<void> {
  const tvs = await loadSavedTvs();
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(tvs.filter((tv) => tv.id !== id)));
}
