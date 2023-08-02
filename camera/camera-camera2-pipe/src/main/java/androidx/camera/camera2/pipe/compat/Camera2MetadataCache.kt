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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.camera2.pipe.config.CameraPipeContext
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Provides caching and querying of [CameraMetadata] via Camera2.
 *
 * This class is thread safe and provides suspending functions for querying and accessing
 * [CameraMetadata].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class Camera2MetadataCache
@Inject
constructor(
    @CameraPipeContext private val cameraPipeContext: Context,
    private val threads: Threads,
    private val permissions: Permissions,
    private val cameraMetadataConfig: CameraPipe.CameraMetadataConfig,
    private val timeSource: TimeSource
) : Camera2MetadataProvider {
    @GuardedBy("cache")
    private val cache = ArrayMap<String, CameraMetadata>()

    override suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata {
        synchronized(cache) {
            val existing = cache[cameraId.value]
            if (existing != null) {
                return existing
            }
        }

        // Suspend and query CameraMetadata on a background thread.
        return withContext(threads.backgroundDispatcher) { awaitCameraMetadata(cameraId) }
    }

    override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata {
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
        val start = Timestamps.now(timeSource)

        return Debug.trace("Camera-${cameraId.value}#readCameraMetadata") {
            try {
                Log.debug { "Loading metadata for $cameraId" }
                val cameraManager =
                    cameraPipeContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val characteristics = cameraManager.getCameraCharacteristics(cameraId.value)

                // This technically shouldn't be null per documentation, but we suspect it could be
                // under certain devices in certain situations.
                @Suppress("RedundantRequireNotNullCall")
                checkNotNull(characteristics) {
                    "Failed to get CameraCharacteristics for $cameraId!"
                }

                // Merge the camera specific and global cache blocklists together.
                // this will prevent these values from being cached after first access.
                val cameraBlocklist = cameraMetadataConfig.cameraCacheBlocklist[cameraId]
                val cacheBlocklist =
                    if (cameraBlocklist == null) {
                        cameraMetadataConfig.cacheBlocklist
                    } else {
                        cameraMetadataConfig.cacheBlocklist + cameraBlocklist
                    }

                val cameraMetadata =
                    Camera2CameraMetadata(
                        cameraId,
                        redacted,
                        characteristics,
                        this,
                        emptyMap(),
                        cacheBlocklist
                    )

                Log.info {
                    val duration = Timestamps.now(timeSource) - start
                    val redactedString =
                        when (redacted) {
                            false -> ""
                            true -> " (redacted)"
                        }
                    "Loaded metadata for $cameraId in ${duration.formatMs()}$redactedString"
                }

                return@trace cameraMetadata
            } catch (throwable: Throwable) {
                if (CameraError.shouldHandleDoNotDisturbException(throwable)) {
                    throw DoNotDisturbException(
                        "Failed to load metadata: Do Not Disturb mode is on!"
                    )
                }
                throw IllegalStateException("Failed to load metadata for $cameraId!", throwable)
            }
        }
    }

    private fun isMetadataRedacted(): Boolean = !permissions.hasCameraPermission
}
