/*
 * Copyright (C) 2017 The Android Open Source Project
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

#ifndef JVMTI_HELPER_H
#define JVMTI_HELPER_H

#include "jni.h"
#include "jvmti.h"

#include <string>

namespace androidx_inspection {
static const std::string kEmpty = std::string();

/**
 * Returns a jvmtiEnv pointer. Note that it is the responsibility of the caller
 * to ensure that the thread is attached.
 * See JavaVM->AttachCurrentThread(...), and also GetThreadLocalJNI(JavaVM* vm)
 */
jvmtiEnv* CreateJvmtiEnv(JavaVM* vm);

/**
 * Checks against the err_num.
 * Returns true if there is an error, false otherwise.
 */
bool CheckJvmtiError(jvmtiEnv* jvmti, jvmtiError err_num,
                     const std::string& message = kEmpty);

/**
 * Sets all available capabilities on the given JVMTI environment.
 */
void SetAllCapabilities(jvmtiEnv* jvmti);

/**
 * Helper to enable/disable an event via the SetEventNotificationMode API.
 */
void SetEventNotification(jvmtiEnv* jvmti, jvmtiEventMode mode,
                          jvmtiEvent event_type);

/**
 * Helper to deallocate memory allocated with jvmti.
 */
void* Allocate(jvmtiEnv* jvmti, jlong size);

/**
 * Helper to deallocate memory allocated with jvmti.
 */
void Deallocate(jvmtiEnv* jvmti, void* ptr);

/**
 * Returns a JNIEnv pointer that is attached to the caller thread.
 */
JNIEnv* GetThreadLocalJNI(JavaVM* vm);

}  // namespace androidx_inspection

#endif  // JVMTI_HELPER_H
