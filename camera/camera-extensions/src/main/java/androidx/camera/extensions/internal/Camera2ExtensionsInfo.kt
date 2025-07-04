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

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger

/**
 * Provides Camera2Extensions related info.
 *
 * This class will cache [CameraExtensionCharacteristics] and some other support information for
 * performance improvement.
 */
public class Camera2ExtensionsInfo(private val cameraManager: CameraManager) {
    private val lock = Any()
    @GuardedBy("lock")
    private val cachedCharacteristics: MutableMap<String, CameraExtensionCharacteristics> =
        mutableMapOf()
    @GuardedBy("lock")
    private val cachedSupportedOutputSizes: MutableMap<String, List<Size>> = mutableMapOf()
    @GuardedBy("lock")
    private val cachedSupportedExtensions: MutableMap<String, List<Int>> = mutableMapOf()

    /** Retrieves [CameraExtensionCharacteristics] for the specified camera. */
    @RequiresApi(31)
    public fun getExtensionCharacteristics(cameraId: String): CameraExtensionCharacteristics =
        synchronized(lock) {
            cachedCharacteristics[cameraId]
                ?: cameraManager.getCameraExtensionCharacteristics(cameraId).also {
                    cachedCharacteristics[cameraId] = it
                }
        }

    /**
     * Returns true if the specified camera supports the specific extension mode. Otherwise, returns
     * false.
     */
    public fun isExtensionAvailable(cameraId: String, mode: Int): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            false
        } else {
            getSupportedExtensions(cameraId).contains(mode)
        }

    @RequiresApi(31)
    private fun getSupportedExtensions(cameraId: String): List<Int> =
        synchronized(lock) {
            cachedSupportedExtensions[cameraId]
                ?: getExtensionCharacteristics(cameraId).supportedExtensions.also {
                    cachedSupportedExtensions[cameraId] = it
                }
        }

    /** Retrieves supported output sizes for the specified camera, extension mode and format. */
    public fun getSupportedOutputSizes(cameraId: String, mode: Int, format: Int): List<Size> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return emptyList()
        }

        val key = getCachedOutputSizesKey(cameraId, mode, format)

        synchronized(lock) { cachedSupportedOutputSizes[key] }
            ?.let {
                return it
            }

        return try {
            if (format == ImageFormat.PRIVATE) {
                    getExtensionCharacteristics(cameraId)
                        .getExtensionSupportedSizes(mode, SurfaceTexture::class.java)
                } else {
                    getExtensionCharacteristics(cameraId).getExtensionSupportedSizes(mode, format)
                }
                .also { synchronized(lock) { cachedSupportedOutputSizes[key] = it } }
        } catch (e: IllegalArgumentException) {
            Logger.e(
                TAG,
                "Failed to retrieve supported output sizes for camera $cameraId, mode $mode, format $format",
                e,
            )
            emptyList()
        }
    }

    private fun getCachedOutputSizesKey(cameraId: String, mode: Int, format: Int): String =
        "$cameraId-$mode-$format"

    private companion object {
        private const val TAG = "Camera2ExtensionsInfo"
    }
}
