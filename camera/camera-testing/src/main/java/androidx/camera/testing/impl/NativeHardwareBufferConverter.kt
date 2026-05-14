/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.hardware.HardwareBuffer
import android.util.Log

/** Utility to convert HardwareBuffer to RGBA byte array using native graphics APIs. */
public object NativeHardwareBufferConverter {
    init {
        try {
            System.loadLibrary("testing_surface_jni")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("NativeHWBufferJNI", "Failed to load testing_surface_jni", e)
        }
    }

    /**
     * Converts a HardwareBuffer to RGBA bytes.
     *
     * @param buffer The HardwareBuffer to convert.
     * @param rgbaArray The output byte array. Must be at least width * height * 4 in length.
     * @return true if successful, false otherwise.
     */
    public fun convertToRgba(buffer: HardwareBuffer, rgbaArray: ByteArray): Boolean {
        return nativeConvertToRgba(buffer, rgbaArray)
    }

    @JvmStatic
    private external fun nativeConvertToRgba(buffer: HardwareBuffer, rgbaArray: ByteArray): Boolean
}
