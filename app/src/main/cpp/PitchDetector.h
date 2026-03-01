#ifndef PITCHAPP_PITCHDETECTOR_H
#define PITCHAPP_PITCHDETECTOR_H

#include <vector>

/**
 * Pitch detection algorithm based on Autocorrelation / YIN method.
 * Calculates the fundamental frequency (in Hz) of a given audio buffer.
 */
class PitchDetector {
public:
    PitchDetector(float sampleRate = 48000.0f);
    ~PitchDetector();

    /**
     * Sets the sample rate of the audio stream.
     */
    void setSampleRate(float sampleRate);

    /**
     * Sets the expected frequency range based on the sound source.
     * This optimizes the search and avoids false harmonics.
     */
    void setFrequencyRange(float minFreq, float maxFreq);

    /**
     * Sets the tolerance/threshold for the YIN algorithm.
     * Lower values = stricter detection (less false positives, but might miss decaying notes).
     * Higher values = more permissive (catches decaying notes, but might jitter).
     */
    void setTolerance(float tolerance);

    /**
     * Analyzes the audio buffer and returns the fundamental frequency in Hz.
     * Returns -1.0f if no clear pitch is detected.
     */
    float detectPitch(const float* audioData, int numFrames);

private:
    float mSampleRate;
    float mMinFrequency;
    float mMaxFrequency;
    float mTolerance;

    std::vector<float> mCorrelationBuffer;
};

#endif //PITCHAPP_PITCHDETECTOR_H