/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <android/log.h>

#define LOG(MSG) __android_log_write(ANDROID_LOG_DEBUG, "BENCHMARK", MSG)

extern "C" {

static void JNICALL consumeByte(__unused jbyte value) {}
static void JNICALL consumeShort(__unused jshort value) {}
static void JNICALL consumeInt(__unused jint value) {}
static void JNICALL consumeLong(__unused jlong value) {}
static void JNICALL consumeFloat(__unused jfloat value) {}
static void JNICALL consumeDouble(__unused jdouble value) {}
static void JNICALL consumeBoolean(__unused jboolean value) {}
static void JNICALL consumeChar(__unused jchar value) {}
static void JNICALL consumeObject(
        JNIEnv *env,
        __unused jclass clazz,
        __unused jobject value
) {}

}

static JNINativeMethod sMethods[] = {
        {"consume",
                "(B)V", // byte
                reinterpret_cast<void *>(consumeByte)
        },
        {"consume",
                "(S)V", // short
                reinterpret_cast<void *>(consumeShort)
        },
        {"consume",
                "(I)V", // int
                reinterpret_cast<void *>(consumeInt)
        },
        {"consume",
                "(J)V", // long
                reinterpret_cast<void *>(consumeLong)
        },
        {"consume",
                "(F)V", // float
                reinterpret_cast<void *>(consumeFloat)
        },
        {"consume",
                "(D)V", // double
                reinterpret_cast<void *>(consumeDouble)
        },
        {"consume",
                "(Z)V", // boolean
                reinterpret_cast<void *>(consumeBoolean)
        },
        {"consume",
                "(C)V", // char
                reinterpret_cast<void *>(consumeChar)
        },
        {"consume",
                "(Ljava/lang/Object;)V",
                reinterpret_cast<void *>(consumeObject)
        },
};

jint JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6)) {
        LOG("JNI_OnLoad failure when trying to register native methods for BlackHole.");
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("androidx/benchmark/BlackHole");
    if (clazz == nullptr) {
        LOG("Cannot find BlackHole class when trying to register native methods.");
        return JNI_ERR;
    }

    const int methodCount = sizeof(sMethods) / sizeof(sMethods[0]);
    int result = env->RegisterNatives(clazz, sMethods, methodCount);
    env->DeleteLocalRef(clazz);

    if (result != 0) {
        LOG("Failure when trying to call RegisterNatives to register native BlackHole methods.");
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}