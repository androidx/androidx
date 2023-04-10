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
#ifndef JNI_WRAPPERS_H_
#define JNI_WRAPPERS_H_

#include <jni.h>
#include <string>

// This file implements a collection of classes that make it easy to wrap
// JNI types, exposing C++ versions of their values and releasing JNI resources
// automatically.
namespace androidx_inspection {

// Wrap a jstring, exposing it as a std::string.
// If the jstring comes from a null object, it will be exposed as "".
    class JStringWrapper {
    public:
        JStringWrapper(JNIEnv *env, const jstring &jstr) {
            if (jstr == nullptr) {
                str_ = "";
            } else {
                const char *c_str = env->GetStringUTFChars(jstr, NULL);
                if (c_str == nullptr) {
                    str_ = "";
                } else {
                    str_ = c_str;
                }
                env->ReleaseStringUTFChars(jstr, c_str);
            }
        }

        const std::string &get() const { return str_; }

    private:
        std::string str_;
    };

}  // namespace androidx_inspection

#endif  // JNI_WRAPPERS_H_
