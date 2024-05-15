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
#include <cerrno>
#include <dlfcn.h>

extern "C" {

JNIEXPORT jint JNICALL
Java_androidx_camera_testing_impl_SurfaceUtil_nativeGetSurfaceFormat(JNIEnv *env, jclass clazz,
                                                           jobject jsurface) {
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    assert(nativeWindow != nullptr);
    int32_t format = ANativeWindow_getFormat(nativeWindow);
    ANativeWindow_release(nativeWindow);
    return format;
}

JNIEXPORT jint JNICALL
Java_androidx_camera_testing_impl_SurfaceUtil_nativeSetBuffersTransform(
        JNIEnv *env,
        jclass clazz,
        jobject jsurface,
        jint transform) {
    // Load libnativewindow for newer APIs
    void* libnativewindow = dlopen("libnativewindow.so", RTLD_NOW);
    if (!libnativewindow) {
        // Unable to load libnativewindow
        return -ENOENT;
    }

    // Load ANativeWindow_setBuffersTransform which was added in API 26
    int32_t (*ANativeWindow_setBuffersTransform)(ANativeWindow*, int32_t);

    ANativeWindow_setBuffersTransform = (int32_t(*)(ANativeWindow*, int32_t))
            dlsym(libnativewindow, "ANativeWindow_setBuffersTransform");

    if (!ANativeWindow_setBuffersTransform) {
        dlclose(libnativewindow);
        // Unable to load ANativeWindow_setBuffersTransform
        return -ENOSYS;
    }

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    auto transformValue = (ANativeWindowTransform)transform;
    assert(nativeWindow != nullptr);
    int32_t result = ANativeWindow_setBuffersTransform(nativeWindow, transformValue);
    ANativeWindow_release(nativeWindow);
    return result;
}

}  // extern "C"
