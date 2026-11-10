// Ejecuta EAS CLI leyendo el token de Expo desde .env.local (que Git ignora).
// Uso: npm run eas -- build --profile development --platform android
const { spawnSync } = require('node:child_process');
const { existsSync } = require('node:fs');
const path = require('node:path');

const envFile = path.join(__dirname, '..', '.env.local');

if (existsSync(envFile)) {
  // Carga las variables de .env.local en el entorno de este proceso.
  process.loadEnvFile(envFile);
}

if (!process.env.EXPO_TOKEN || process.env.EXPO_TOKEN === 'pega-aqui-tu-token') {
  console.error(
    'Falta EXPO_TOKEN. Copia .env.local.example como .env.local y pega tu token de expo.dev.'
  );
  process.exit(1);
}

const result = spawnSync('npx', ['eas-cli@latest', ...process.argv.slice(2)], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
  env: process.env,
});

process.exit(result.status ?? 1);
