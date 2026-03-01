#include "PitchDetector.h"
#include <cmath>
#include <algorithm>

PitchDetector::PitchDetector(float sampleRate)
        : mSampleRate(sampleRate), mMinFrequency(30.0f), mMaxFrequency(2000.0f), mTolerance(0.15f) {
}

PitchDetector::~PitchDetector() {
}

void PitchDetector::setSampleRate(float sampleRate) {
    mSampleRate = sampleRate;
}

void PitchDetector::setFrequencyRange(float minFreq, float maxFreq) {
    mMinFrequency = minFreq;
    mMaxFrequency = maxFreq;
}

void PitchDetector::setTolerance(float tolerance) {
    mTolerance = tolerance;
}

float PitchDetector::detectPitch(const float* audioData, int numFrames) {
    if (numFrames == 0 || audioData == nullptr) {
        return -1.0f;
    }

    int halfFrames = numFrames / 2;
    mCorrelationBuffer.assign(halfFrames, 0.0f);

    int minLag = static_cast<int>(mSampleRate / mMaxFrequency);
    int maxLag = static_cast<int>(mSampleRate / mMinFrequency);

    if (maxLag >= halfFrames) {
        maxLag = halfFrames - 1;
    }
    if (minLag < 1) {
        minLag = 1;
    }

    // Step 1: Difference function
    for (int tau = 1; tau <= maxLag; tau++) {
        float sum = 0.0f;
        for (int j = 0; j < halfFrames; j++) {
            float diff = audioData[j] - audioData[j + tau];
            sum += diff * diff;
        }
        mCorrelationBuffer[tau] = sum;
    }

    // Step 2: Cumulative mean normalized difference function
    float runningSum = 0.0f;
    mCorrelationBuffer[0] = 1.0f;
    for (int tau = 1; tau <= maxLag; tau++) {
        runningSum += mCorrelationBuffer[tau];
        mCorrelationBuffer[tau] *= tau / runningSum;
    }

    // Step 3: Absolute threshold
    int bestTau = -1;
    for (int tau = minLag; tau <= maxLag; tau++) {
        if (mCorrelationBuffer[tau] < mTolerance) {
            // Find the local minimum
            while (tau + 1 <= maxLag && mCorrelationBuffer[tau + 1] < mCorrelationBuffer[tau]) {
                tau++;
            }
            bestTau = tau;
            break;
        }
    }

    // If no pitch found below threshold, find the absolute minimum
    if (bestTau == -1) {
        float minVal = 1.0f;
        for (int tau = minLag; tau <= maxLag; tau++) {
            if (mCorrelationBuffer[tau] < minVal) {
                minVal = mCorrelationBuffer[tau];
                bestTau = tau;
            }
        }
        // If the minimum is still too high, it's likely unpitched noise
        if (minVal > 0.5f) {
            return -1.0f;
        }
    }

    // Step 4: Parabolic interpolation for sub-sample accuracy
    float betterTau = static_cast<float>(bestTau);
    if (bestTau > 0 && bestTau < maxLag) {
        float s0 = mCorrelationBuffer[bestTau - 1];
        float s1 = mCorrelationBuffer[bestTau];
        float s2 = mCorrelationBuffer[bestTau + 1];

        // Parabolic interpolation formula
        float denominator = s0 + s2 - 2.0f * s1;
        if (denominator != 0.0f) {
            betterTau += (s0 - s2) / (2.0f * denominator);
        }
    }

    return mSampleRate / betterTau;
}