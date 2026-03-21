#include "AudioEngine.h"
#include <android/log.h>
#include <algorithm>

#define LOG_TAG "AudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioEngine::AudioEngine() : mNoiseGate(0.005f), mPitchDetector(48000.0f), mFramesAccumulated(0), mPitchCallback(nullptr), mWaveformCallback(nullptr) {
    mAudioBuffer.resize(TARGET_BUFFER_SIZE, 0.0f);
}

AudioEngine::~AudioEngine() {
    stop();
}

void AudioEngine::setPitchCallback(PitchCallback callback) {
    mPitchCallback = callback;
}

void AudioEngine::setWaveformCallback(WaveformCallback callback) {
    mWaveformCallback = callback;
}

void AudioEngine::setNoiseGateThreshold(float threshold) {
    mNoiseGate.setThreshold(threshold);
}

void AudioEngine::setYinTolerance(float tolerance) {
    mPitchDetector.setTolerance(tolerance);
}

bool AudioEngine::start() {
    oboe::AudioStreamBuilder builder;

    builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setInputPreset(oboe::InputPreset::Unprocessed)
            ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return false;
    }

    mPitchDetector.setSampleRate(static_cast<float>(mStream->getSampleRate()));
    mPitchDetector.setFrequencyRange(80.0f, 1200.0f);

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        mStream->close();
        return false;
    }

    return true;
}

void AudioEngine::stop() {
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}


oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
    auto *floatData = static_cast<float *>(audioData);

    if (mWaveformCallback != nullptr) {
        mWaveformCallback(floatData, numFrames);
    }

    if (mNoiseGate.isAboveThreshold(floatData, numFrames)) {

        int framesToCopy = std::min(numFrames, TARGET_BUFFER_SIZE - mFramesAccumulated);
        std::copy(floatData, floatData + framesToCopy, mAudioBuffer.begin() + mFramesAccumulated);
        mFramesAccumulated += framesToCopy;

        if (mFramesAccumulated >= TARGET_BUFFER_SIZE) {
            float pitchInHz = mPitchDetector.detectPitch(mAudioBuffer.data(), TARGET_BUFFER_SIZE);

            if (pitchInHz > 0.0f) {
                if (mPitchCallback != nullptr) {
                    mPitchCallback(pitchInHz);
                }
            } else {
                if (mPitchCallback != nullptr) {
                    mPitchCallback(-1.0f);
                }
            }
            mFramesAccumulated = 0;
        }

    } else {
        if (mPitchCallback != nullptr) {
            mPitchCallback(-1.0f);
        }
        mFramesAccumulated = 0;
    }

    return oboe::DataCallbackResult::Continue;
}