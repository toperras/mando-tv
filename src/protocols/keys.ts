// Teclas del mando básico, independientes de la marca de la tele.
// Cada adaptador (Xiaomi, Samsung, LG...) traducirá estas teclas a su protocolo.

export type RemoteKey =
  | 'power'
  | 'up'
  | 'down'
  | 'left'
  | 'right'
  | 'ok'
  | 'back'
  | 'home'
  | 'menu'
  | 'mute'
  | 'volumeUp'
  | 'volumeDown'
  | 'channelUp'
  | 'channelDown';
