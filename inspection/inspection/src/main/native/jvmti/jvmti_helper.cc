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
#include "jvmti_helper.h"

#include <iomanip>
#include <sstream>
#include <android/api-level.h>
#include <sys/system_properties.h>

#include "scoped_local_ref.h"
#include "stdlib.h"
#include "logger.h"
#include "properties.h"

namespace androidx_inspection {

const char* const kBuildType = "ro.build.type";
const char* const kUserBuild = "user";

jvmtiEnv* CreateJvmtiEnv(JavaVM* vm) {
  jvmtiEnv* jvmti_env;
  jint jvmti_flag = JVMTI_VERSION_1_2;
  if (GetProperty(kBuildType, "") != kUserBuild &&
      android_get_device_api_level() >= __ANDROID_API_P__) {
    // On non-user-build devices (such as userdebug build), we use flag
    // |kArtTiVersion| as defined in //art/openjdkjvmti/art_jvmti.h to support
    // non-debuggable apps. The flag was introduced in Android P (API 28).
    jvmti_flag = JVMTI_VERSION_1_2 | 0x40000000;
  }
  jint result = vm->GetEnv((void**)&jvmti_env, jvmti_flag);
  if (result != JNI_OK) {
    LOGE("%s", "Error creating jvmti environment.");
    return nullptr;
  }

  return jvmti_env;
}

bool CheckJvmtiError(jvmtiEnv* jvmti, jvmtiError err_num,
                     const std::string& message) {
  if (err_num == JVMTI_ERROR_NONE) {
    return false;
  }

  char* error = nullptr;
  jvmti->GetErrorName(err_num, &error);
  LOGE("JVMTI error: %d(%s) %s", err_num, error == nullptr ? "Unknown" : error, message.c_str());
  Deallocate(jvmti, error);
  return true;
}

void SetAllCapabilities(jvmtiEnv* jvmti) {
  jvmtiCapabilities caps;
  jvmtiError error;
  error = jvmti->GetPotentialCapabilities(&caps);
  CheckJvmtiError(jvmti, error);
  error = jvmti->AddCapabilities(&caps);
  CheckJvmtiError(jvmti, error);
}

void SetEventNotification(jvmtiEnv* jvmti, jvmtiEventMode mode,
                          jvmtiEvent event_type) {
  jvmtiError err = jvmti->SetEventNotificationMode(mode, event_type, nullptr);
  CheckJvmtiError(jvmti, err);
}

JNIEnv* GetThreadLocalJNI(JavaVM* vm) {
  JNIEnv* jni;
  jint result =
      vm->GetEnv((void**)&jni, JNI_VERSION_1_6);  // ndk is only up to 1.6.
  if (result == JNI_EDETACHED) {
    LOGV("%s", "JNIEnv not attached");
#ifdef __ANDROID__
    if (vm->AttachCurrentThread(&jni, nullptr) != 0) {
#else
    // TODO get rid of this. Currently bazel built with the jdk's jni headers
    // which has a slightly different signature. Once bazel has switched to
    // platform-dependent headers we will remove this.
    if (vm->AttachCurrentThread((void**)&jni, nullptr) != 0) {
#endif
      LOGV("%s", "Failed to attach JNIEnv");
      return nullptr;
    }
  }

  return jni;
}

void* Allocate(jvmtiEnv* jvmti, jlong size) {
  unsigned char* alloc = nullptr;
  jvmtiError err = jvmti->Allocate(size, &alloc);
  CheckJvmtiError(jvmti, err);
  return (void*)alloc;
}

void Deallocate(jvmtiEnv* jvmti, void* ptr) {
  if (ptr == nullptr) {
    return;
  }

  jvmtiError err = jvmti->Deallocate((unsigned char*)ptr);
  CheckJvmtiError(jvmti, err);
}

}  // namespace androidx_inspection
