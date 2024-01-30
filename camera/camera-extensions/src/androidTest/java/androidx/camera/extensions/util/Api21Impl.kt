/*
 * Copyright 2023 The Android Open Source Project
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

@file:RequiresApi(21)

package androidx.camera.extensions.util

import android.hardware.camera2.CameraDevice
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/**
 * Helper class to prevent class verification failures at API level 21.
 */
object Api21Impl {

    @DoNotInline
    @JvmStatic
    fun CameraDevice.toCameraDeviceWrapper() = CameraDeviceWrapper(this)

    class CameraDeviceWrapper(private val cameraDevice: CameraDevice) {

        @DoNotInline
        fun unwrap(): CameraDevice = cameraDevice

        @DoNotInline
        fun close() {
            cameraDevice.close()
        }
    }
}
