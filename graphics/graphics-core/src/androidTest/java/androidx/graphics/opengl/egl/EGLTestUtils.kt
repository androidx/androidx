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

package androidx.graphics.opengl.egl

import androidx.opengl.EGLExt

/**
 * Helper method to determine if the Android device supports creation of native file descriptors
 * from EGLSync objects
 */
fun deviceSupportsNativeAndroidFence(): Boolean {
    val eglManager = EGLManager().apply { initialize() }
    val supportsAndroidFence = eglManager.supportsNativeAndroidFence()
    eglManager.release()
    return supportsAndroidFence
}

/**
 * Queries the corresponding EGL fence extensions from an initialized [EGLManager] instance
 */
fun EGLManager.supportsNativeAndroidFence(): Boolean =
    isExtensionSupported(EGLExt.EGL_KHR_FENCE_SYNC) &&
        isExtensionSupported(EGLExt.EGL_ANDROID_NATIVE_FENCE_SYNC)
