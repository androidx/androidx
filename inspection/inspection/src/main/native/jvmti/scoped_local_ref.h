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

#ifndef SCOPED_LOCAL_REF_H
#define SCOPED_LOCAL_REF_H

#include "jni.h"

namespace androidx_inspection {

/**
 * A copy of the same facility from the Android platform. See original source:
 * android/platform/libnativehelper/include/nativehelper/ScopedLocalRef.h
 */
template <typename T>
class ScopedLocalRef {
 public:
  ScopedLocalRef(JNIEnv* env, T ref) : env_(env), ref_(ref) {}

  ~ScopedLocalRef() { reset(NULL); }

  void reset(T ptr) {
    if (ptr != ref_) {
      if (ref_ != NULL) {
        env_->DeleteLocalRef(ref_);
      }
      ref_ = ptr;
    }
  }

  T release() {
    T ref = ref_;
    ref_ = NULL;
    return ref;
  }

  T get() const { return ref_; }

 private:
  JNIEnv* env_;
  T ref_;

  // Disallow copy and assignment.
  ScopedLocalRef(const ScopedLocalRef&) = delete;
  ScopedLocalRef& operator=(const ScopedLocalRef&) = delete;
};

}  // namespace androidx_inspection

#endif  // SCOPED_LOCAL_REF_H
