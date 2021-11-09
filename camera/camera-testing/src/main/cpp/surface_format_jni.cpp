/*
 * Copyright 2020 The Android Open Source Project
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

#include <android/native_window_jni.h>

#include <cassert>

extern "C" {

JNIEXPORT jint JNICALL
Java_androidx_camera_testing_SurfaceFormatUtil_nativeGetSurfaceFormat(JNIEnv *env, jclass clazz,
                                                           jobject jsurface) {
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    assert(nativeWindow != nullptr);
    int32_t format = ANativeWindow_getFormat(nativeWindow);
    ANativeWindow_release(nativeWindow);
    return format;
}

}  // extern "C"
