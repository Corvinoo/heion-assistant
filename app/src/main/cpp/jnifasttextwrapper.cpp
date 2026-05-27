#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <memory>
#include <stdexcept>

#include "fastText/src/fasttext.h"

using fasttext::FastText;

namespace {
    void throwRuntimeException(JNIEnv* env, const std::string& message) {
        jclass exClass = env->FindClass("java/lang/RuntimeException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, message.c_str());
        }
    }

    FastText* asModel(jlong handle) {
        return reinterpret_cast<FastText*>(handle);
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_me_robin_heion_nlp_FastTextNative_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new FastText());
}

extern "C" JNIEXPORT void JNICALL
Java_me_robin_heion_nlp_FastTextNative_nativeDestroy(JNIEnv* env, jobject, jlong handle) {
    try {
        delete asModel(handle);
    } catch (const std::exception& e) {
        throwRuntimeException(env, e.what());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_me_robin_heion_nlp_FastTextNative_nativeLoadModel(
        JNIEnv* env, jobject, jlong handle, jstring path_) {
    if (handle == 0) {
        throwRuntimeException(env, "Model handle is null");
        return;
    }

    const char* pathCStr = env->GetStringUTFChars(path_, nullptr);
    if (pathCStr == nullptr) {
        throwRuntimeException(env, "Failed to read model path");
        return;
    }

    try {
        asModel(handle)->loadModel(std::string(pathCStr));
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(path_, pathCStr);
        throwRuntimeException(env, e.what());
        return;
    }

    env->ReleaseStringUTFChars(path_, pathCStr);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_me_robin_heion_nlp_FastTextNative_nativePredict(
        JNIEnv* env, jobject, jlong handle, jstring text_, jint k) {
    if (handle == 0) {
        throwRuntimeException(env, "Model handle is null");
        return nullptr;
    }

    const char* textCStr = env->GetStringUTFChars(text_, nullptr);
    if (textCStr == nullptr) {
        throwRuntimeException(env, "Failed to read input text");
        return nullptr;
    }

    std::vector<std::pair<float, std::string>> predictions;
    try {
        // Fixed: Direct initialization avoids the function declaration ambiguity
        std::istringstream iss(textCStr);

        asModel(handle)->predictLine(iss, predictions, static_cast<int32_t>(k), 0.0);
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(text_, textCStr);
        throwRuntimeException(env, e.what());
        return nullptr;
    }

    env->ReleaseStringUTFChars(text_, textCStr);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(
            static_cast<jsize>(predictions.size()),
            stringClass,
            nullptr);

    for (jsize i = 0; i < static_cast<jsize>(predictions.size()); ++i) {
        std::string line = predictions[i].second + "\t" + std::to_string(predictions[i].first);
        env->SetObjectArrayElement(
                result,
                i,
                env->NewStringUTF(line.c_str()));
    }

    return result;
}
