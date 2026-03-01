#include "NoiseGate.h"
#include <cmath>

NoiseGate::NoiseGate(float threshold) : mThreshold(threshold) {
}

NoiseGate::~NoiseGate() {
}

void NoiseGate::setThreshold(float threshold) {
    if (threshold < 0.0f) {
        mThreshold = 0.0f;
    } else {
        mThreshold = threshold;
    }
}

bool NoiseGate::isAboveThreshold(const float* audioData, int numFrames) const {
    if (numFrames <= 0 || audioData == nullptr) {
        return false;
    }

    float sumSquares = 0.0f;
    for (int i = 0; i < numFrames; ++i) {
        sumSquares += audioData[i] * audioData[i];
    }

    float rms = std::sqrt(sumSquares / numFrames);

    return rms >= mThreshold;
}