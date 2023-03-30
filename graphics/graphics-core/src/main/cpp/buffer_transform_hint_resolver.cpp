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
#include <android/log.h>
#include <jni.h>
#include <sys/system_properties.h>

#include "buffer_transform_hint_resolver.h"

jstring BufferTransformHintResolver_getDisplayOrientation(
        JNIEnv *env, jclass) {
    char name[PROP_VALUE_MAX];
    __system_property_get("ro.surface_flinger.primary_display_orientation", name);
    return (*env).NewStringUTF(name);
}

static const JNINativeMethod JNI_METHODS[] = {
        {
            "getDisplayOrientation",
            "()Ljava/lang/String;",
            (void *)BufferTransformHintResolver_getDisplayOrientation
        }
};

jint loadBufferTransformHintResolverMethods(JNIEnv* env) {
    jclass bufferTransformHintResolverClazz = env->FindClass(
            "androidx/graphics/lowlatency/BufferTransformHintResolver");
    if (bufferTransformHintResolverClazz == nullptr) {
        //ALOGE("Unable to resolve buffer transform hint resolver class");
        return JNI_ERR;
    }

    if (env->RegisterNatives(bufferTransformHintResolverClazz, JNI_METHODS,
                             sizeof(JNI_METHODS) / sizeof(JNINativeMethod)) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_OK;
}
