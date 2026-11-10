import { type SavedTv, type TvController } from './types';
import { XiaomiHttpController } from './xiaomi-http';

/** Crea el adaptador adecuado para una tele guardada. */
export function createController(tv: SavedTv): TvController {
  switch (tv.protocol) {
    case 'xiaomi-http':
      return new XiaomiHttpController(tv.ip);
  }
}
