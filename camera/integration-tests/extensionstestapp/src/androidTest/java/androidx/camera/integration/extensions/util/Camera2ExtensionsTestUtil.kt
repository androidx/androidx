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

package androidx.camera.integration.extensions.util

import android.content.Context
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import androidx.annotation.RequiresApi
import androidx.camera.testing.CameraUtil

@RequiresApi(31)
object Camera2ExtensionsTestUtil {

    /**
     * Gets a list of all camera id and extension mode combinations.
     */
    @JvmStatic
    fun getAllCameraIdExtensionModeCombinations(): List<Array<Any>> =
        arrayListOf<Array<Any>>().apply {
            CameraUtil.getBackwardCompatibleCameraIdListOrThrow().forEach { cameraId ->
                AVAILABLE_EXTENSION_MODES.forEach { mode ->
                    add(arrayOf(cameraId, mode))
                }
            }
        }

    @JvmStatic
    fun isCamera2ExtensionModeSupported(
        context: Context,
        cameraId: String,
        extensionMode: Int
    ): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        return extensionCharacteristics.supportedExtensions.contains(extensionMode)
    }

    /**
     * Camera2 extension modes
     */
    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    @JvmStatic
    val AVAILABLE_EXTENSION_MODES = arrayOf(
        CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
        CameraExtensionCharacteristics.EXTENSION_BEAUTY,
        CameraExtensionCharacteristics.EXTENSION_BOKEH,
        CameraExtensionCharacteristics.EXTENSION_HDR,
        CameraExtensionCharacteristics.EXTENSION_NIGHT,
    )
}