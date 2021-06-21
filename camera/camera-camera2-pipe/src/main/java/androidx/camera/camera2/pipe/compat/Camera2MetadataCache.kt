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

package androidx.camera.camera2.pipe.compat

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.Threads
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides caching and querying of [CameraMetadata].
 *
 * This class is designed to be thread safe and provides suspend functions for querying and
 * accessing CameraMetadata.
 */
@Singleton
internal class Camera2MetadataCache @Inject constructor(
    private val context: Context,
    private val threads: Threads,
    private val permissions: Permissions
) : CameraMetadataProvider {
    @GuardedBy("cache")
    private val cache = ArrayMap<String, CameraMetadata>()

    override suspend fun getMetadata(cameraId: CameraId): CameraMetadata {
        synchronized(cache) {
            val existing = cache[cameraId.value]
            if (existing != null) {
                return existing
            }
        }

        // Suspend and query CameraMetadata on a background thread.
        return withContext(threads.ioDispatcher) {
            awaitMetadata(cameraId)
        }
    }

    override fun awaitMetadata(cameraId: CameraId): CameraMetadata {
        return Debug.trace("Camera-${cameraId.value}#awaitMetadata") {
            synchronized(cache) {
                val existing = cache[cameraId.value]
                if (existing != null) {
                    return@trace existing
                } else if (!isMetadataRedacted()) {
                    val result = createCameraMetadata(cameraId, false)
                    cache[cameraId.value] = result
                    return@trace result
                }
            }
            return@trace createCameraMetadata(cameraId, true)
        }
    }

    private fun createCameraMetadata(cameraId: CameraId, redacted: Boolean): Camera2CameraMetadata {
        val start = Timestamps.now()

        return Debug.trace("Camera-${cameraId.value}#readCameraMetadata") {
            try {
                val cameraManager =
                    context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val characteristics =
                    cameraManager.getCameraCharacteristics(cameraId.value)
                val cameraMetadata =
                    Camera2CameraMetadata(cameraId, redacted, characteristics, this, emptyMap())

                Log.info {
                    val duration = Timestamps.now() - start
                    val redactedString = when (redacted) {
                        false -> ""
                        true -> " (redacted)"
                    }
                    "Loaded metadata for $cameraId in ${duration.formatMs()}$redactedString"
                }

                return@trace cameraMetadata
            } catch (e: Throwable) {
                throw IllegalStateException("Failed to load metadata for $cameraId!", e)
            }
        }
    }

    private fun isMetadataRedacted(): Boolean = !permissions.hasCameraPermission
}
