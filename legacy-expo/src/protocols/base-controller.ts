import { type ConnectionListener, type ConnectionState } from './types';

/** Parte común a todos los adaptadores: guardar y avisar del estado. */
export abstract class ObservableConnection {
  private state: ConnectionState = { status: 'idle' };
  private listeners = new Set<ConnectionListener>();

  getState(): ConnectionState {
    return this.state;
  }

  subscribe(listener: ConnectionListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  protected setState(state: ConnectionState) {
    this.state = state;
    this.listeners.forEach((listener) => listener(state));
  }
}

/** fetch con tiempo máximo de espera. */
export async function fetchWithTimeout(url: string, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError';
}
