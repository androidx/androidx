/*
 * Copyright 2021 The Android Open Source Project
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
#include <string>
#include <android/native_activity.h>
#include <android/surface_control.h>
#include <android/api-level.h>
#include <android/native_window_jni.h>

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nCreate(JNIEnv *env, jobject thiz,
                                                            jlong surfaceControl,
                                                            jstring debug_name) {
    if (android_get_device_api_level() >= 29) {
        auto aSurfaceControl = reinterpret_cast<ASurfaceControl *>(surfaceControl);
        auto debugName = env->GetStringUTFChars(debug_name, nullptr);
        return reinterpret_cast<jlong>(ASurfaceControl_create(aSurfaceControl,
                                                              debugName));

    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nCreateFromWindow(JNIEnv *env, jobject thiz,
                                                                      jobject surface,
                                                                      jstring debug_name) {
    if (android_get_device_api_level() >= 29) {
        auto AWindow = ANativeWindow_fromSurface(env, surface);
        auto debugName = env->GetStringUTFChars(debug_name, nullptr);
        auto surfaceControl = reinterpret_cast<jlong>(ASurfaceControl_createFromWindow(AWindow,
                                                                                       debugName));

        ANativeWindow_release(AWindow);
        return surfaceControl;
    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_nRelease(JNIEnv *env, jobject thiz,
                                                             jlong surfaceControl) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceControl_release(reinterpret_cast<ASurfaceControl *>(surfaceControl));
    } else {
        return;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionCreate(
        JNIEnv *env, jobject thiz) {
    if (android_get_device_api_level() >= 29) {
        return reinterpret_cast<jlong>(ASurfaceTransaction_create());
    } else {
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_graphics_surface_SurfaceControlCompat_00024Transaction_nTransactionDelete(
        JNIEnv *env, jobject thiz,
        jlong surfaceTransaction) {
    if (android_get_device_api_level() >= 29) {
        ASurfaceTransaction_delete(reinterpret_cast<ASurfaceTransaction *>(surfaceTransaction));
    }
}