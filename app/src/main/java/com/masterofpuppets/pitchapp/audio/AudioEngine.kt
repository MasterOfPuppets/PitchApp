package com.masterofpuppets.pitchapp.audio

/**
 * Kotlin wrapper for the C++ Oboe Audio Engine.
 */
class AudioEngine {

    interface PitchListener {
        fun onPitchDetected(pitchInHz: Float)
        fun onWaveformData(waveformData: FloatArray)
    }

    var listener: PitchListener? = null

    init {
        System.loadLibrary("pitchapp")
    }

    external fun startAudioEngine(): Boolean

    external fun stopAudioEngine()

    external fun setNoiseGateThreshold(threshold: Float)

    external fun setYinTolerance(tolerance: Float)

    @Suppress("unused")
    private fun onPitchDetectedFromNative(pitchInHz: Float) {
        listener?.onPitchDetected(pitchInHz)
    }

    /**
     * This method is called continuously from C++ via JNI to send raw audio data.
     */
    @Suppress("unused")
    private fun onWaveformDataFromNative(waveformData: FloatArray) {
        listener?.onWaveformData(waveformData)
    }
}