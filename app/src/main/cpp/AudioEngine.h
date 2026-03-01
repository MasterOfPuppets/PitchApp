#ifndef PITCHAPP_AUDIOENGINE_H
#define PITCHAPP_AUDIOENGINE_H

#include <oboe/Oboe.h>
#include <vector>
#include <functional>
#include "NoiseGate.h"
#include "PitchDetector.h"

/**
 * Handles real-time audio capture using the Oboe library.
 */
class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    using PitchCallback = std::function<void(float)>;
    using WaveformCallback = std::function<void(const float*, int)>;

    AudioEngine();
    ~AudioEngine();

    bool start();
    void stop();
    void setPitchCallback(PitchCallback callback);
    void setWaveformCallback(WaveformCallback callback);
    void setNoiseGateThreshold(float threshold);
    void setYinTolerance(float tolerance);

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    NoiseGate mNoiseGate;
    PitchDetector mPitchDetector;

    PitchCallback mPitchCallback;
    WaveformCallback mWaveformCallback;

    std::vector<float> mAudioBuffer;
    int mFramesAccumulated;

    static constexpr int TARGET_BUFFER_SIZE = 4096;
};

#endif //PITCHAPP_AUDIOENGINE_H