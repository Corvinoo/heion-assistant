#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include <unistd.h>

extern "C" {
#include <espeak-ng/speak_lib.h>
}

#define TAG "PhonemizerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static pthread_mutex_t g_mutex;
static bool g_espeak_initialized = false;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&g_mutex, &attr);
    pthread_mutexattr_destroy(&attr);

    LOGI("PhonemizerJNI loaded and mutex initialized");
    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL
Java_me_robin_heion_tts_phonemization_NativePhonemizer_espeakInitialize(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring dataPath
) {
    pthread_mutex_lock(&g_mutex);

    LOGI("espeakInitialize ENTER");

    if (g_espeak_initialized) {
        LOGI("espeak-ng-data already initialized");
        pthread_mutex_unlock(&g_mutex);
        return 22050;
    }

    if (dataPath == nullptr) {
        LOGE("dataPath is null");
        pthread_mutex_unlock(&g_mutex);
        return -1;
    }

    const char* path = env->GetStringUTFChars(dataPath, nullptr);

    if (path == nullptr) {
        LOGE("GetStringUTFChars failed");
        pthread_mutex_unlock(&g_mutex);
        return -1;
    }

    LOGI("Initializing espeak-ng-data with path: %s", path);

    std::string phondataPath =
            std::string(path) + "/espeak-ng-data/phondata";

    int exists = access(phondataPath.c_str(), F_OK);

    LOGI("Checking phondata: %s exists=%d",
         phondataPath.c_str(),
         exists);

    if (exists != 0) {
        LOGE("phondata missing");

        env->ReleaseStringUTFChars(dataPath, path);

        pthread_mutex_unlock(&g_mutex);
        return -1;
    }

    int sampleRate = espeak_Initialize(
            AUDIO_OUTPUT_SYNCHRONOUS,
            0,
            path,
            espeakINITIALIZE_DONT_EXIT |
            espeakINITIALIZE_PHONEME_IPA
    );

    env->ReleaseStringUTFChars(dataPath, path);

    if (sampleRate <= 0) {
        LOGE("espeak_Initialize failed: %d", sampleRate);

        pthread_mutex_unlock(&g_mutex);
        return -1;
    }

    g_espeak_initialized = true;

    LOGI("espeak-ng-data initialized successfully: %d", sampleRate);

    pthread_mutex_unlock(&g_mutex);

    return sampleRate;
}

JNIEXPORT jstring JNICALL
Java_me_robin_heion_tts_phonemization_NativePhonemizer_espeakTextToPhonemes(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring text,
        jstring voice
) {
    pthread_mutex_lock(&g_mutex);

    if (!g_espeak_initialized) {
        LOGE("espeak not initialized");
        pthread_mutex_unlock(&g_mutex);
        return env->NewStringUTF("");
    }

    if (text == nullptr || voice == nullptr) {
        LOGE("text or voice is null");
        pthread_mutex_unlock(&g_mutex);
        return env->NewStringUTF("");
    }

    const char* voiceName =
            env->GetStringUTFChars(voice, nullptr);

    if (voiceName == nullptr) {
        LOGE("Failed to get voice string");
        pthread_mutex_unlock(&g_mutex);
        return env->NewStringUTF("");
    }

    espeak_ERROR voiceResult =
            espeak_SetVoiceByName(voiceName);

    env->ReleaseStringUTFChars(voice, voiceName);

    if (voiceResult != EE_OK) {
        LOGE("espeak_SetVoiceByName failed: %d", voiceResult);
        pthread_mutex_unlock(&g_mutex);
        return env->NewStringUTF("");
    }

    const char* input =
            env->GetStringUTFChars(text, nullptr);

    if (input == nullptr) {
        LOGE("Failed to get input text");
        pthread_mutex_unlock(&g_mutex);
        return env->NewStringUTF("");
    }

    const void* p = input;
    std::string out;

    while (p != nullptr && *(const char*)p != '\0') {
        const char* phonemes =
                espeak_TextToPhonemes(
                        &p,
                        espeakCHARS_UTF8,
                        espeakPHONEMES_IPA
                );

        if (phonemes == nullptr) {
            break;
        }

        if (*phonemes != '\0') {
            out += phonemes;
        }
    }

    env->ReleaseStringUTFChars(text, input);
    pthread_mutex_unlock(&g_mutex);

    return env->NewStringUTF(out.c_str());
}

JNIEXPORT jint JNICALL
Java_me_robin_heion_tts_phonemization_NativePhonemizer_openJTalkInitialize(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring dictPath,
        jstring voicePath
) {
    pthread_mutex_lock(&g_mutex);
    LOGI("openJTalkInitialize: STUB (Not implemented)");

    // Log paths if provided to verify they are being passed correctly
    if (dictPath != nullptr) {
        const char* dPath = env->GetStringUTFChars(dictPath, nullptr);
        if (dPath) {
            LOGI("  Dict path: %s", dPath);
            env->ReleaseStringUTFChars(dictPath, dPath);
        }
    }

    if (voicePath != nullptr) {
        const char* vPath = env->GetStringUTFChars(voicePath, nullptr);
        if (vPath) {
            LOGI("  Voice path: %s", vPath);
            env->ReleaseStringUTFChars(voicePath, vPath);
        }
    }

    pthread_mutex_unlock(&g_mutex);
    return 0; // Return success for the stub
}

JNIEXPORT jstring JNICALL
Java_me_robin_heion_tts_phonemization_NativePhonemizer_openJTalkTextToPhonemes(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring text
) {
    pthread_mutex_lock(&g_mutex);
    LOGI("openJTalkTextToPhonemes: STUB (Not implemented)");

    if (text != nullptr) {
        const char* input = env->GetStringUTFChars(text, nullptr);
        if (input) {
            LOGI("  Input: %s", input);
            env->ReleaseStringUTFChars(text, input);
        }
    }

    pthread_mutex_unlock(&g_mutex);
    return env->NewStringUTF(""); // Return empty string for the stub
}

JNIEXPORT void JNICALL
Java_me_robin_heion_tts_phonemization_NativePhonemizer_release(
        JNIEnv* /*env*/,
        jobject /*thiz*/
) {
    pthread_mutex_lock(&g_mutex);

    if (g_espeak_initialized) {
        espeak_Terminate();
        g_espeak_initialized = false;
        LOGI("espeak-ng-data terminated");
    }

    pthread_mutex_unlock(&g_mutex);
}

}
