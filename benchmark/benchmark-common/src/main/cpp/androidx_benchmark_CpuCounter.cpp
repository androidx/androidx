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
#include <asm/unistd.h>
#include <memory>
#include <android/log.h>
#include "Profiler.h"
#include <iostream>
#include <sys/syscall.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "UnusedParameter"

const int32_t CountersLongCount = sizeof(utils::Profiler::Counters) / sizeof(uint64_t);

static_assert(
        CountersLongCount == 19,
        "Expected Counters to have consistent length, "
        "may need to update Kotlin LongArray definition"
);

static int perf_event_open(perf_event_attr *hw_event, pid_t pid,
                           int cpu, int group_fd, unsigned long flags) {
    return (int) syscall(__NR_perf_event_open, hw_event, pid, cpu, group_fd, flags);
}

#pragma clang diagnostic pop
extern "C"
JNIEXPORT jstring JNICALL
Java_androidx_benchmark_CpuCounterJni_checkPerfEventSupport(
        JNIEnv *env,
        jobject thiz
) {

    // perf event group creation code copied from Profiler.cpp to allow us to
    // return an error string on failure instead of killing process
    perf_event_attr pe{};
    pe.type = PERF_TYPE_HARDWARE;
    pe.size = sizeof(perf_event_attr);
    pe.config = PERF_COUNT_HW_INSTRUCTIONS;
    pe.disabled = 1;
    pe.exclude_kernel = 1;
    pe.exclude_hv = 1;
    pe.read_format = PERF_FORMAT_GROUP |
                     PERF_FORMAT_ID |
                     PERF_FORMAT_TOTAL_TIME_ENABLED |
                     PERF_FORMAT_TOTAL_TIME_RUNNING;
    int fd = perf_event_open(&pe, 0, -1, -1, 0);
    // TODO: implement checkPerfEventSupport()
    if (fd == -1) {
        char output[256];
        sprintf(&output[0], "perf_event_open failed: [%d]%s", errno, strerror(errno));
        return (jstring) env->NewStringUTF(&output[0]);
    } else {
        close(fd);
        return (jstring) nullptr;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_benchmark_CpuCounterJni_newProfiler(
        JNIEnv *env,
        jobject thiz
) {
    auto *pProfiler = new utils::Profiler();
    return (long) pProfiler;
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_benchmark_CpuCounterJni_freeProfiler(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    delete pProfiler;
}
extern "C"
JNIEXPORT jint JNICALL
Java_androidx_benchmark_CpuCounterJni_resetEvents(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr,
        jint event_mask
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    return (jint) pProfiler->resetEvents(event_mask);
}
extern "C"
JNIEXPORT void JNICALL
Java_androidx_benchmark_CpuCounterJni_reset(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    pProfiler->reset();
}
extern "C"
JNIEXPORT void JNICALL
Java_androidx_benchmark_CpuCounterJni_start(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    pProfiler->start();
}
extern "C"
JNIEXPORT void JNICALL
Java_androidx_benchmark_CpuCounterJni_stop(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    pProfiler->stop();
}
extern "C"
JNIEXPORT void JNICALL
Java_androidx_benchmark_CpuCounterJni_read(
        JNIEnv *env,
        jobject thiz,
        jlong profiler_ptr,
        jlongArray out_data
) {
    auto *pProfiler = (utils::Profiler *) profiler_ptr;
    utils::Profiler::Counters counters = pProfiler->readCounters();
    jsize longCount = sizeof(utils::Profiler::Counters) / sizeof(uint64_t);
    env->SetLongArrayRegion(out_data, 0, longCount, reinterpret_cast<jlong *>(&counters));
}