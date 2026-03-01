package com.masterofpuppets.pitchapp.data.local.entity

/**
 * Represents the category of the sound source being tuned.
 */
enum class SoundSourceCategory {
    PLUCKED_STRING, // e.g., Guitars, Basses
    BOWED_STRING,   // e.g., Violins, Cellos
    WIND,           // Wind instruments
    VOICE,          // Human voice
    OTHER           // Any other unclassified sound source
}