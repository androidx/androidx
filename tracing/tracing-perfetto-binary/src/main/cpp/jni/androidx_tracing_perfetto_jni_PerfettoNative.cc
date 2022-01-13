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

extern "C" {

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeRegisterWithPerfetto(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto::RegisterWithPerfetto();
    PERFETTO_LOG("Perfetto: initialized");
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventBegin(
        JNIEnv *env, __unused jclass clazz, jint key, jstring traceInfo) {
    const char *traceInfoUtf = env->GetStringUTFChars(traceInfo, NULL);
    tracing_perfetto::TraceEventBegin(key, traceInfoUtf);
    PERFETTO_LOG("Perfetto: TraceEventBegin(%s key=%d)", traceInfoUtf, key);
    env->ReleaseStringUTFChars(traceInfo, traceInfoUtf);
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeTraceEventEnd(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto::TraceEventEnd();
    PERFETTO_LOG("Perfetto: TraceEventEnd()");
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_PerfettoNative_nativeFlushEvents(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto::Flush();
    PERFETTO_LOG("Perfetto: Flush()");
}
} // extern "C"
