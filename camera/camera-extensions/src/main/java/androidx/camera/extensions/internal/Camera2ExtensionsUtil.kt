/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions.internal

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CameraXConfig.ImplType
import androidx.camera.extensions.ExtensionMode

/** The class to provide Camera2 Extensions related util methods. */
public object Camera2ExtensionsUtil {
    private const val TAG = "Camera2ExtensionsUtil"

    /** Returns the Camera2 extension mode corresponding to the input CameraX extension mode. */
    @RequiresApi(31)
    public fun convertCameraXModeToCamera2Mode(cameraXMode: Int): Int =
        when (cameraXMode) {
            ExtensionMode.BOKEH -> CameraExtensionCharacteristics.EXTENSION_BOKEH
            ExtensionMode.HDR -> CameraExtensionCharacteristics.EXTENSION_HDR
            ExtensionMode.NIGHT -> CameraExtensionCharacteristics.EXTENSION_NIGHT
            ExtensionMode.FACE_RETOUCH -> CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH
            ExtensionMode.AUTO -> CameraExtensionCharacteristics.EXTENSION_AUTOMATIC
            else ->
                throw IllegalArgumentException("Unavailable CameraX extension mode ($cameraXMode)")
        }

    /** Returns the CameraX extension mode corresponding to the input Camera2 extension mode. */
    @RequiresApi(31)
    public fun convertCamera2ModeToCameraXMode(camera2Mode: Int): Int =
        when (camera2Mode) {
            CameraExtensionCharacteristics.EXTENSION_BOKEH -> ExtensionMode.BOKEH
            CameraExtensionCharacteristics.EXTENSION_HDR -> ExtensionMode.HDR
            CameraExtensionCharacteristics.EXTENSION_NIGHT -> ExtensionMode.NIGHT
            CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH -> ExtensionMode.FACE_RETOUCH
            CameraExtensionCharacteristics.EXTENSION_AUTOMATIC -> ExtensionMode.AUTO
            else ->
                throw IllegalArgumentException("Unavailable Camera2 extension mode ($camera2Mode)")
        }

    /** Creates the camera id to CameraExtensionCharacteristics map. */
    @JvmStatic
    public fun createCameraId2CameraExtensionCharacteristicsMap(
        applicationContext: Context
    ): Map<String, CameraExtensionCharacteristics> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return emptyMap()
        }

        return mutableMapOf<String, CameraExtensionCharacteristics>().apply {
            try {
                val cameraManager = applicationContext.getSystemService(CameraManager::class.java)
                for (cameraId in cameraManager.cameraIdList) {
                    put(cameraId, cameraManager.getCameraExtensionCharacteristics(cameraId))
                }
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to retrieve CameraExtensionCharacteristics info. ", e)
            }
        }
    }

    /** Returns true if Camera2 Extensions API should be be used to turn on the extension mode. */
    @JvmStatic
    public fun shouldUseCamera2Extensions(@ImplType configImplType: Int): Boolean =
        configImplType == CameraXConfig.CAMERAX_CONFIG_IMPL_TYPE_PIPE
}
