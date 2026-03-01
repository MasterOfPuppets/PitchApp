package com.masterofpuppets.pitchapp.utils

import com.masterofpuppets.pitchapp.model.Transposition
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Data class representing the result of the pitch conversion.
 * -1 representa silence or undetected and unturned noise.
 */
data class PitchResult(
    val noteIndex: Int,
    val octave: Int,
    val targetFrequency: Double,
    val deviationInCents: Double
)

/**
 * Utility object for converting raw frequencies (Hz) into musical notes and cents.
 */
object PitchConverter {

    /**
     * Converts a frequency in Hz to a musical pitch result, taking into account
     * the reference A4 calibration and the instrument's transposition.
     *
     * @param frequencyHz The detected frequency.
     * @param referenceA4 The reference frequency for A4 (default is 440.0 Hz).
     * @param transposition The instrument's transposition (default is C - Concert Pitch).
     * @return PitchResult containing the closest note index, octave, target frequency, and cents deviation.
     */
    fun convertFrequencyToPitch(
        frequencyHz: Double,
        referenceA4: Double = 440.0,
        transposition: Transposition = Transposition.C
    ): PitchResult {
        if (frequencyHz <= 0.0) {
            return PitchResult(-1, 0, 0.0, 0.0)
        }

        // 1. Calculate the exact number of half-steps from the reference A4
        val halfStepsFromA4 = 12.0 * log2(frequencyHz / referenceA4)

        // 2. Find the closest MIDI note for the actual concert pitch heard
        val concertMidiNote = (69.0 + halfStepsFromA4).roundToInt()

        // 3. Calculate the exact mathematical frequency of that concert target note
        val targetFrequency = referenceA4 * 2.0.pow((concertMidiNote - 69) / 12.0)

        // 4. Calculate the deviation in cents (100 cents = 1 half-step)
        // The deviation is physical and remains the same regardless of what we call the note.
        val deviationInCents = 1200.0 * log2(frequencyHz / targetFrequency)

        // 5. Apply transposition for display purposes
        val displayMidiNote = concertMidiNote + transposition.semitoneOffset

        // 6. Determine note index and octave based on the transposed MIDI note
        // MIDI note 0 is C-1, note 12 is C0, etc.
        val noteIndex = if (displayMidiNote % 12 >= 0) displayMidiNote % 12 else (displayMidiNote % 12) + 12
        val octave = Math.floorDiv(displayMidiNote, 12) - 1

        return PitchResult(noteIndex, octave, targetFrequency, deviationInCents)
    }
}