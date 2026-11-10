import { useEffect, useState } from 'react';

import { createController } from './index';
import { type ConnectionState, type SavedTv, type TvController } from './types';

/**
 * Crea el adaptador para la tele, se conecta al abrir la pantalla y se
 * desconecta al salir. Devuelve el adaptador y su estado actual.
 */
export function useTvController(tv: SavedTv | null) {
  const [controller, setController] = useState<TvController | null>(null);
  const [state, setState] = useState<ConnectionState>({ status: 'idle' });

  useEffect(() => {
    if (!tv) return;
    const instance = createController(tv);
    setController(instance);
    const unsubscribe = instance.subscribe(setState);
    instance.connect().catch(() => {});
    return () => {
      unsubscribe();
      instance.disconnect().catch(() => {});
    };
  }, [tv]);

  const reconnect = () => {
    controller?.connect().catch(() => {});
  };

  return { controller, state, reconnect };
}
