import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

// Ajustes de la app. Aquí NO se guardan tokens ni claves: esos irán a
// almacenamiento seguro (SecureStore / Keychain) en la fase 1.

export type ThemePreference = 'system' | 'light' | 'dark';

export type Settings = {
  themePreference: ThemePreference;
  hapticsEnabled: boolean;
};

const DEFAULT_SETTINGS: Settings = {
  themePreference: 'system',
  hapticsEnabled: true,
};

const STORAGE_KEY = 'settings.v1';

type SettingsContextValue = {
  settings: Settings;
  /** true cuando ya se han leído los ajustes guardados. */
  loaded: boolean;
  updateSettings: (changes: Partial<Settings>) => void;
};

const SettingsContext = createContext<SettingsContextValue | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((raw) => {
        if (raw) {
          // Mezclamos con los valores por defecto por si en una versión
          // futura se añaden ajustes nuevos.
          setSettings({ ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<Settings>) });
        }
      })
      .catch(() => {
        // Si el almacenamiento falla, la app sigue con los ajustes por defecto.
      })
      .finally(() => setLoaded(true));
  }, []);

  const updateSettings = (changes: Partial<Settings>) => {
    setSettings((previous) => {
      const next = { ...previous, ...changes };
      AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next)).catch(() => {});
      return next;
    });
  };

  return (
    <SettingsContext.Provider value={{ settings, loaded, updateSettings }}>
      {children}
    </SettingsContext.Provider>
  );
}

export function useSettings(): SettingsContextValue {
  const value = useContext(SettingsContext);
  if (!value) {
    throw new Error('useSettings debe usarse dentro de <SettingsProvider>');
  }
  return value;
}
