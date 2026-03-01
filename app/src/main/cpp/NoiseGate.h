#ifndef PITCHAPP_NOISEGATE_H
#define PITCHAPP_NOISEGATE_H

/**
 * A simple Noise Gate to filter out background noise.
 * Audio frames with an RMS volume below the threshold will be considered as noise.
 */
class NoiseGate {
public:
    NoiseGate(float threshold = 0.01f);
    ~NoiseGate();

    void setThreshold(float threshold);
    bool isAboveThreshold(const float* audioData, int numFrames) const;

private:
    float mThreshold;
};

#endif //PITCHAPP_NOISEGATE_H