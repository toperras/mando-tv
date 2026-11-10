import { type RemoteKey } from './keys';

/** Protocolos que la app sabe hablar. Se irán añadiendo más. */
export type ProtocolId = 'xiaomi-http';

/** Motivos de error que la interfaz sabe explicar al usuario. */
export type ConnectionErrorReason =
  | 'unreachable' // nadie responde en esa IP/puerto
  | 'unexpectedResponse' // algo responde, pero no parece una tele compatible
  | 'timeout' // la tele no respondió a tiempo
  | 'unknown';

/**
 * Estado de la conexión. Incluye estados que Xiaomi HTTP no usa
 * (aceptación en la tele, PIN) porque Samsung, LG o Android TV sí los necesitan.
 */
export type ConnectionState =
  | { status: 'idle' }
  | { status: 'searching' }
  | { status: 'connecting' }
  | { status: 'awaitingTvAcceptance' }
  | { status: 'awaitingPin' }
  | { status: 'connected' }
  | { status: 'error'; reason: ConnectionErrorReason };

/** Lo que la tele conectada permite hacer. La interfaz se adapta a esto. */
export type TvCapabilities = {
  keys: ReadonlySet<RemoteKey>;
  /** Escribir texto en buscadores. */
  text: boolean;
  /** Encender la tele cuando está totalmente apagada. */
  powerOnFromOff: boolean;
};

export type ConnectionListener = (state: ConnectionState) => void;

export interface TvController {
  readonly protocol: ProtocolId;
  readonly capabilities: TvCapabilities;
  getState(): ConnectionState;
  /** Se suscribe a cambios de estado; devuelve la función para darse de baja. */
  subscribe(listener: ConnectionListener): () => void;
  connect(): Promise<void>;
  disconnect(): Promise<void>;
  /** Envía UNA orden. Nunca reintenta por su cuenta, para no duplicar acciones. */
  sendKey(key: RemoteKey): Promise<void>;
  sendText(text: string): Promise<void>;
  submitPin?(pin: string): Promise<void>;
}

/** Tele guardada en el móvil (sin secretos: los tokens irán a SecureStore). */
export type SavedTv = {
  id: string;
  name: string;
  ip: string;
  protocol: ProtocolId;
  addedAt: number;
};
