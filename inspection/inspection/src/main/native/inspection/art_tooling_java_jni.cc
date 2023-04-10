/*
 * Copyright 2022 The Android Open Source Project
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
#include "art_tooling_impl.h"
#include "jni_wrappers.h"
#include "logger.h"

namespace androidx_inspection {

    jobjectArray FindInstances(JNIEnv *env, jlong nativePtr, jclass jclass) {
        ArtToolingImpl *inspector =
                reinterpret_cast<ArtToolingImpl *>(nativePtr);
        return inspector->FindInstances(env, jclass);
    }

    void AddEntryTransformation(JNIEnv *env, jlong nativePtr, jclass origin_class,
                                jstring method_name) {
        ArtToolingImpl *inspector =
                reinterpret_cast<ArtToolingImpl *>(nativePtr);
        JStringWrapper method_str(env, method_name);
        std::size_t found = method_str.get().find("(");
        if (found == std::string::npos) {
            LOGE("Method should be in the format $method_name($signature)$return_type, but was %s",
                 method_str.get().c_str());
            return;
        }

        inspector->AddEntryTransform(env, origin_class,
                                     method_str.get().substr(0, found),
                                     method_str.get().substr(found));
    }

    void AddExitTransformation(JNIEnv *env, jlong nativePtr, jclass origin_class,
                               jstring method_name) {
        ArtToolingImpl *inspector =
                reinterpret_cast<ArtToolingImpl *>(nativePtr);
        JStringWrapper method_str(env, method_name);
        std::size_t found = method_str.get().find("(");
        if (found == std::string::npos) {
            LOGE("Method should be in the format $method_name($signature)$return_type, but was %s",
                 method_str.get().c_str());
            return;
        }

        inspector->AddExitTransform(env, origin_class,
                                    method_str.get().substr(0, found),
                                    method_str.get().substr(found));
    }

}  // namespace androidx_inspection

extern "C" {

JNIEXPORT jlong JNICALL
Java_androidx_inspection_ArtToolingImpl_createNativeArtTooling(
        JNIEnv *env, jclass jclazz) {
    auto tooling = androidx_inspection::ArtToolingImpl::create(env);
    return reinterpret_cast<jlong>(tooling);
}

JNIEXPORT void JNICALL
Java_androidx_inspection_ArtToolingImpl_nativeRegisterEntryHook(
        JNIEnv *env, jclass jclazz, jlong servicePtr, jclass originClass,
        jstring originMethod) {
    androidx_inspection::AddEntryTransformation(env, servicePtr, originClass, originMethod);
}

JNIEXPORT void JNICALL
Java_androidx_inspection_ArtToolingImpl_nativeRegisterExitHook(
        JNIEnv *env, jclass jclazz, jlong servicePtr, jclass originClass,
        jstring originMethod) {
    androidx_inspection::AddExitTransformation(env, servicePtr, originClass, originMethod);
}

JNIEXPORT jobjectArray JNICALL
Java_androidx_inspection_ArtToolingImpl_nativeFindInstances(
        JNIEnv *env, jclass callerClass, jlong servicePtr, jclass jclass) {
    return androidx_inspection::FindInstances(env, servicePtr, jclass);
}
}
