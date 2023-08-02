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

#ifndef ART_TOOLING_IMPL_H_
#define ART_TOOLING_IMPL_H_

#include <atomic>
#include <string>
#include <jni.h>
#include <jvmti.h>

namespace androidx_inspection {

    class ArtToolingImpl {
    public:
        static ArtToolingImpl* create(JNIEnv* env);

        // transforms the given method and inserts ArtToolingImpl.onEntry call
        // as an entry hook
        void AddEntryTransform(JNIEnv* jni, const jclass& origin_class,
                               const std::string& method_name,
                               const std::string& signature) {
            AddTransform(jni, origin_class, method_name, signature, true);
        }

        // transforms the given method and inserts ArtToolingImpl.onExit call as
        // exit hook
        void AddExitTransform(JNIEnv* jni, const jclass& origin_class,
                              const std::string& method_name,
                              const std::string& signature) {
            AddTransform(jni, origin_class, method_name, signature, false);
        }
        // finds instances of the given class in the heap
        jobjectArray FindInstances(JNIEnv* jni, jclass jclass);

    private:
        explicit ArtToolingImpl(jvmtiEnv* jvmti);
        // java object ArtToolingImpl that keeps reference to this object is
        // singleton, so no need to clean up
        ~ArtToolingImpl() = delete;
        void Initialize();

        jvmtiEnv* jvmti_;
        std::atomic<long> next_tag_;

        void AddTransform(JNIEnv* jni, const jclass& origin_class,
                          const std::string& method_name,
                          const std::string& signature, bool is_entry);
        bool tagClassInstancesO(JNIEnv* jni, jclass clazz, jlong tag);
        bool tagClassInstancesQ(jclass clazz, jlong tag);

        static void OnClassFileLoaded(jvmtiEnv* jvmti_env, JNIEnv* jni_env,
                                      jclass class_being_redefined, jobject loader,
                                      const char* name, jobject protection_domain,
                                      jint class_data_len,
                                      const unsigned char* class_data,
                                      jint* new_class_data_len,
                                      unsigned char** new_class_data);

    };

}  // namespace androidx_inspection

#endif  // ART_TOOLING_IMPL_H_
