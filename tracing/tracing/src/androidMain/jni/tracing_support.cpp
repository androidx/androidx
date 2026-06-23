/*
 * Copyright 2026 The Android Open Source Project
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
#include <unistd.h>

inline pid_t bionic_get_tid() noexcept {
    return gettid();
}

jlong get_tid_critical() noexcept {
    return static_cast<jlong>(bionic_get_tid());
}

// Registration

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("androidx/tracing/support/Tid");
    if (clazz == nullptr) {
        return JNI_ERR;
    }

    static const JNINativeMethod sMethods[] = {
            {
                    "getTid",
                    "()J",
                    reinterpret_cast<void *>(get_tid_critical)
            }
    };

    // Register the method directly with ART
    int result = env->RegisterNatives(clazz, sMethods, 1);
    env->DeleteLocalRef(clazz);
    if (result != 0) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
