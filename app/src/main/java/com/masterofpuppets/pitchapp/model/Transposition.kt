package com.masterofpuppets.pitchapp.model

/**
 * Represents the transposition of an instrument.
 * The offset is the number of semitones to add to the detected concert pitch
 * so that the display matches the instrument's written notes.
 */
enum class Transposition(val nameKey: String, val semitoneOffset: Int) {
    C("transposition_c", 0),              // e.g., Piano, Guitar, Flute
    B_FLAT("transposition_b_flat", 2),    // e.g., Trumpet, Clarinet, Tenor Sax
    E_FLAT("transposition_e_flat", -3),   // e.g., Alto Sax, Baritone Sax (can also be +9)
    F("transposition_f", 7)               // e.g., French Horn
}