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

#include <cstring>
#include <jni.h>
#include "../shared_counter.h"

static_assert(sizeof(jlong) >= sizeof(volatile std::atomic<uint32_t>*),
              "jlong not large enough for pointer");

jint ThrowIoException(JNIEnv* env, const char* message) {
  jclass ioExceptionClass = env->FindClass("java/io/IOException");
  if (ioExceptionClass == nullptr) {
    // We couldn't find the IOException class to throw. We can special case -1
    // kotlin side in case this happens.
    return -1;
  }

  return env->ThrowNew(ioExceptionClass, message);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_androidx_datastore_multiprocess_NativeSharedCounter_nativeTruncateFile(
        JNIEnv *env, jclass clazz, jint fd) {
    if (int errNum = datastore::TruncateFile(fd)) {
        return ThrowIoException(env, strerror(errNum));
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_androidx_datastore_multiprocess_NativeSharedCounter_nativeCreateSharedCounter(
        JNIEnv *env, jclass clazz, jint fd, jboolean enable_mlock) {
    void* address = nullptr;
    if (int errNum = datastore::CreateSharedCounter(fd, &address, enable_mlock)) {
        return ThrowIoException(env, strerror(errNum));
    }
    return reinterpret_cast<jlong>(address);
}

JNIEXPORT jint JNICALL
Java_androidx_datastore_multiprocess_NativeSharedCounter_nativeGetCounterValue(
        JNIEnv *env, jclass clazz, jlong address) {
    return static_cast<jint>(
        datastore::GetCounterValue(reinterpret_cast<std::atomic<uint32_t>*>(address)));
}

JNIEXPORT jint JNICALL
Java_androidx_datastore_multiprocess_NativeSharedCounter_nativeIncrementAndGetCounterValue(
        JNIEnv *env, jclass clazz, jlong address) {
    return static_cast<jint>(
        datastore::IncrementAndGetCounterValue(reinterpret_cast<std::atomic<uint32_t>*>(address)));
}

}