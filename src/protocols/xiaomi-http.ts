import { fetchWithTimeout, isAbortError, ObservableConnection } from './base-controller';
import { type RemoteKey } from './keys';
import { type ConnectionErrorReason, type TvCapabilities, type TvController } from './types';

/*
 * Adaptador para teles Xiaomi con el servicio HTTP local del puerto 6095
 * (PatchWall / Mi TV 4A, 4S, 3S...). No está documentado oficialmente por
 * Xiaomi: se basa en lo observado por proyectos de la comunidad (pymitv).
 * Funciona en Expo Go porque solo usa peticiones HTTP normales.
 *
 * Limitaciones conocidas:
 * - No hay teclas de silencio ni de canal.
 * - No puede encender la tele si está completamente apagada.
 * - isalive responde igual aunque la tele esté en reposo.
 */

const PORT = 6095;
const PROBE_TIMEOUT_MS = 2500;
const KEY_TIMEOUT_MS = 2000;

const KEY_CODES: Partial<Record<RemoteKey, string>> = {
  power: 'power',
  up: 'up',
  down: 'down',
  left: 'left',
  right: 'right',
  ok: 'enter',
  back: 'back',
  home: 'home',
  menu: 'menu',
  volumeUp: 'volumeup',
  volumeDown: 'volumedown',
};

export type XiaomiProbeResult =
  | { ok: true; deviceName: string | null }
  | { ok: false; reason: ConnectionErrorReason };

/** Pregunta a una IP si hay una tele Xiaomi compatible. */
export async function probeXiaomi(ip: string, timeoutMs = PROBE_TIMEOUT_MS): Promise<XiaomiProbeResult> {
  try {
    const response = await fetchWithTimeout(
      `http://${ip}:${PORT}/request?action=isalive`,
      timeoutMs
    );
    if (!response.ok) {
      return { ok: false, reason: 'unexpectedResponse' };
    }
    const body: unknown = await response.json().catch(() => null);
    if (!isIsAliveBody(body)) {
      return { ok: false, reason: 'unexpectedResponse' };
    }
    return { ok: true, deviceName: body.data?.devicename ?? null };
  } catch (error) {
    return { ok: false, reason: isAbortError(error) ? 'timeout' : 'unreachable' };
  }
}

type IsAliveBody = { status: number; data?: { devicename?: string } };

function isIsAliveBody(value: unknown): value is IsAliveBody {
  return (
    typeof value === 'object' &&
    value !== null &&
    'status' in value &&
    (value as { status: unknown }).status === 0
  );
}

export class XiaomiHttpController extends ObservableConnection implements TvController {
  readonly protocol = 'xiaomi-http' as const;
  readonly capabilities: TvCapabilities = {
    keys: new Set(Object.keys(KEY_CODES) as RemoteKey[]),
    text: false,
    powerOnFromOff: false,
  };

  constructor(private readonly ip: string) {
    super();
  }

  async connect(): Promise<void> {
    this.setState({ status: 'connecting' });
    const result = await probeXiaomi(this.ip);
    this.setState(result.ok ? { status: 'connected' } : { status: 'error', reason: result.reason });
  }

  async disconnect(): Promise<void> {
    // HTTP no mantiene conexión abierta: basta con olvidar el estado.
    this.setState({ status: 'idle' });
  }

  async sendKey(key: RemoteKey): Promise<void> {
    const code = KEY_CODES[key];
    if (!code) {
      throw new Error(`Tecla no admitida por Xiaomi HTTP: ${key}`);
    }
    // Una sola petición, sin reintentos: si reintentáramos tras un timeout,
    // la tele podría recibir la orden dos veces.
    const response = await fetchWithTimeout(
      `http://${this.ip}:${PORT}/controller?action=keyevent&keycode=${code}`,
      KEY_TIMEOUT_MS
    );
    if (!response.ok) {
      throw new Error(`La tele respondió ${response.status}`);
    }
  }

  async sendText(): Promise<void> {
    throw new Error('Xiaomi HTTP no permite escribir texto');
  }
}
