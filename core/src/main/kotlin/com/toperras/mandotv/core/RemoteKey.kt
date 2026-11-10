package com.toperras.mandotv.core

/**
 * Teclas del mando básico, independientes de la marca.
 * Cada adaptador (Samsung, LG, Android TV, IR...) las traduce a su protocolo.
 */
enum class RemoteKey {
    POWER,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    OK,
    BACK,
    HOME,
    MENU,
    MUTE,
    VOLUME_UP,
    VOLUME_DOWN,
    CHANNEL_UP,
    CHANNEL_DOWN,
}
