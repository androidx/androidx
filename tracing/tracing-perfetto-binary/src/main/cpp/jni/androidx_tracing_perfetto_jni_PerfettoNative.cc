/*
 * Copyright (C) 2022 The Android Open Source Project
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
#include "../tracing_perfetto.h"

// Limit of 4096 should be safe as that's what android.os.Trace is using. See:
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/jni/
// android_os_Trace.cpp;l=42;drc=8dae06607c3ca449516ca2564d40a7174481c2ae
#define BUFFER_SIZE 4096 // Note: keep in sync with PerfettoSdkTraceTest

extern "C" {

static void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeRegisterWithPerfetto(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto::RegisterWithPerfetto();
}

static void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventBegin(
        JNIEnv *env, __unused jclass clazz, jint key, jstring traceInfo) {
    jsize lengthUtf = env->GetStringUTFLength(traceInfo);

    jsize lengthUtfWithNull = lengthUtf + 1;
    if (lengthUtfWithNull <= BUFFER_SIZE) {
        // fast path
        std::array<char, BUFFER_SIZE> traceInfoUtf;
        jsize length = env->GetStringLength(traceInfo);
        env->GetStringUTFRegion(traceInfo, 0, length, traceInfoUtf.data());
        traceInfoUtf[lengthUtf] = '\0'; // terminate the string
        tracing_perfetto::TraceEventBegin(key, traceInfoUtf.data());
    } else {
        // slow path
        const char *traceInfoUtf = env->GetStringUTFChars(traceInfo, NULL);
        tracing_perfetto::TraceEventBegin(key, traceInfoUtf);
        env->ReleaseStringUTFChars(traceInfo, traceInfoUtf);
    }
}

static void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventEnd() {
    tracing_perfetto::TraceEventEnd();
}

static jstring JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeVersion(
        JNIEnv *env, __unused jclass clazz) {
    return env->NewStringUTF(tracing_perfetto::Version());
}
} // extern "C"

// Explicitly registering native methods using CriticalNative / FastNative as per:
// https://source.android.com/devices/tech/dalvik/improvements#faster-native-methods.
// Note: this applies to Android 8 - 11. In Android 12+, this is recommended (to avoid slow lookups
// on first use), but not necessary.

static JNINativeMethod sMethods[] = {
        {"nativeRegisterWithPerfetto",
                "()V",
                reinterpret_cast<void *>(
                    Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeRegisterWithPerfetto)
        },
        {"nativeTraceEventBegin",
                "(ILjava/lang/String;)V",
                reinterpret_cast<void *>(
                    Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventBegin)
        },
        {"nativeTraceEventEnd",
                "()V",
                reinterpret_cast<void *>(
                    Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventEnd)
        },
        {"nativeVersion",
                "()Ljava/lang/String;",
                reinterpret_cast<void *>(
                    Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeVersion)
        },
};

jint JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    JNIEnv *env = NULL;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6)) {
        PERFETTO_LOG("JNI_OnLoad failure when trying to register native methods for tracing.");
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("androidx/tracing/perfetto/jni/PerfettoNative");
    if (clazz == NULL) {
        PERFETTO_LOG("Cannot find PerfettoNative class when trying to register native methods for "
                     "tracing.");
        return JNI_ERR;
    }

    int result = env->RegisterNatives(clazz, sMethods, 4);
    env->DeleteLocalRef(clazz);

    if (result != 0) {
        PERFETTO_LOG("Failure when trying to call RegisterNatives to register native methods for "
                     "tracing.");
        return JNI_ERR;
    }

    PERFETTO_LOG("Successfully registered native methods for tracing.");
    return JNI_VERSION_1_6;
}
