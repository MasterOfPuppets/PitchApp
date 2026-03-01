#include <jni.h>
#include "AudioEngine.h"

// Global references for JNI communication
static AudioEngine *engine = nullptr;
static JavaVM *jvm = nullptr;
static jobject kotlinEngineObj = nullptr;
static jmethodID onPitchDetectedMethodId = nullptr;
static jmethodID onWaveformDataMethodId = nullptr;

// This function will be triggered by our AudioEngine when a pitch is found
void pitchCallback(float pitchInHz) {
    if (jvm == nullptr || kotlinEngineObj == nullptr || onPitchDetectedMethodId == nullptr) {
        return;
    }

    JNIEnv *env;
    int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    bool attached = false;

    if (getEnvStat == JNI_EDETACHED) {
        if (jvm->AttachCurrentThread(&env, nullptr) != 0) {
            return;
        }
        attached = true;
    }

    env->CallVoidMethod(kotlinEngineObj, onPitchDetectedMethodId, pitchInHz);
    if (attached) {
        jvm->DetachCurrentThread();
    }
}

// This function will be triggered continuously by our AudioEngine to send raw audio data
void waveformCallback(const float* waveformData, int numFrames) {
    if (jvm == nullptr || kotlinEngineObj == nullptr || onWaveformDataMethodId == nullptr) {
        return;
    }

    JNIEnv *env;
    int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    bool attached = false;

    if (getEnvStat == JNI_EDETACHED) {
        if (jvm->AttachCurrentThread(&env, nullptr) != 0) {
            return;
        }
        attached = true;
    }

    jfloatArray jWaveformArray = env->NewFloatArray(numFrames);
    if (jWaveformArray != nullptr) {
        env->SetFloatArrayRegion(jWaveformArray, 0, numFrames, waveformData);
        env->CallVoidMethod(kotlinEngineObj, onWaveformDataMethodId, jWaveformArray);
        env->DeleteLocalRef(jWaveformArray);
    }
    if (attached) {
        jvm->DetachCurrentThread();
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_masterofpuppets_pitchapp_audio_AudioEngine_startAudioEngine(JNIEnv *env, jobject thiz) {
    // Cache the JavaVM pointer
    env->GetJavaVM(&jvm);
    if (kotlinEngineObj == nullptr) {
        kotlinEngineObj = env->NewGlobalRef(thiz);
        jclass clazz = env->GetObjectClass(kotlinEngineObj);
        onPitchDetectedMethodId = env->GetMethodID(clazz, "onPitchDetectedFromNative", "(F)V");
        onWaveformDataMethodId = env->GetMethodID(clazz, "onWaveformDataFromNative", "([F)V");
    }

    if (engine == nullptr) {
        engine = new AudioEngine();
        engine->setPitchCallback(pitchCallback);
        engine->setWaveformCallback(waveformCallback);
    }

    return engine->start() ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_masterofpuppets_pitchapp_audio_AudioEngine_stopAudioEngine(JNIEnv *env, jobject thiz) {
    if (engine != nullptr) {
        engine->stop();
        delete engine;
        engine = nullptr;
    }

    if (kotlinEngineObj != nullptr) {
        env->DeleteGlobalRef(kotlinEngineObj);
        kotlinEngineObj = nullptr;
        onPitchDetectedMethodId = nullptr;
        onWaveformDataMethodId = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_masterofpuppets_pitchapp_audio_AudioEngine_setNoiseGateThreshold(JNIEnv *env, jobject thiz, jfloat threshold) {
    if (engine != nullptr) {
        engine->setNoiseGateThreshold(threshold);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_masterofpuppets_pitchapp_audio_AudioEngine_setYinTolerance(JNIEnv *env, jobject thiz, jfloat tolerance) {
    if (engine != nullptr) {
        engine->setYinTolerance(tolerance);
    }
}