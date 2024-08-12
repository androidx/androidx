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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraExtensionMetadata
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
 * Provides caching and querying of [CameraMetadata] and [CameraExtensionMetadata] via Camera2.
 *
 * This class is thread safe and provides suspending functions for querying and accessing
 * [CameraMetadata] and [CameraExtensionMetadata].
 */
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

    @GuardedBy("cache") private val cache = ArrayMap<String, CameraMetadata>()

    @GuardedBy("extensionCache")
    private val extensionCache = ArrayMap<String, CameraExtensionMetadata>()

    @GuardedBy("extensionCharacteristicsCache")
    private val extensionCharacteristicsCache = ArrayMap<String, CameraExtensionCharacteristics>()

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

    override suspend fun getCameraExtensionMetadata(
        cameraId: CameraId,
        extension: Int
    ): CameraExtensionMetadata {
        synchronized(extensionCache) {
            val existing = extensionCache[cameraId.value]
            if (existing != null) {
                return existing
            }
        }

        // Suspend and query CameraExtensionMetadata on a background thread.
        return withContext(threads.backgroundDispatcher) {
            awaitCameraExtensionMetadata(cameraId, extension)
        }
    }

    override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata {
        return Debug.trace("$cameraId#awaitMetadata") {
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

    override fun awaitCameraExtensionMetadata(
        cameraId: CameraId,
        extension: Int
    ): CameraExtensionMetadata {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Debug.trace("$cameraId#awaitExtensionMetadata") {
                synchronized(extensionCache) {
                    val existing = extensionCache[cameraId.value]
                    if (existing != null) {
                        return@trace existing
                    } else if (!isMetadataRedacted()) {
                        val result = createCameraExtensionMetadata(cameraId, false, extension)
                        extensionCache[cameraId.value] = result
                        return@trace result
                    }
                }
                return@trace createCameraExtensionMetadata(cameraId, true, extension)
            }
        } else {
            throw Exception(
                "Extension sessions are only supported on Android S or higher. " +
                    "Device SDK is ${Build.VERSION.SDK_INT}"
            )
        }
    }

    override fun getSupportedCameraExtensions(cameraId: CameraId): Set<Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val extensionCharacteristics = getCameraExtensionCharacteristics(cameraId)
            return Api31Compat.getSupportedExtensions(extensionCharacteristics).toSet()
        }
        return emptySet()
    }

    private fun createCameraMetadata(cameraId: CameraId, redacted: Boolean): Camera2CameraMetadata {
        val start = Timestamps.now(timeSource)

        return Debug.trace("$cameraId#readCameraMetadata") {
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
                val cameraBlocklist =
                    if (shouldBlockSensorOrientationCache(characteristics)) {
                        (cameraMetadataConfig.cameraCacheBlocklist[cameraId] ?: emptySet()) +
                            CameraCharacteristics.SENSOR_ORIENTATION
                    } else {
                        cameraMetadataConfig.cameraCacheBlocklist[cameraId]
                    }
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createCameraExtensionMetadata(
        cameraId: CameraId,
        redacted: Boolean,
        extension: Int
    ): Camera2CameraExtensionMetadata {
        val start = Timestamps.now(timeSource)

        return Debug.trace("$cameraId#readCameraExtensionMetadata") {
            try {
                Log.debug { "Loading extension metadata for $cameraId" }

                val extensionCharacteristics = getCameraExtensionCharacteristics(cameraId)

                val extensionMetadata =
                    Camera2CameraExtensionMetadata(
                        cameraId,
                        redacted,
                        extension,
                        extensionCharacteristics,
                        emptyMap()
                    )

                Log.info {
                    val duration = Timestamps.now(timeSource) - start
                    val redactedString =
                        when (redacted) {
                            false -> ""
                            true -> " (redacted)"
                        }
                    "Loaded extension metadata for $cameraId in " +
                        "${duration.formatMs()}$redactedString"
                }

                return@trace extensionMetadata
            } catch (throwable: Throwable) {
                throw IllegalStateException(
                    "Failed to load extension metadata for $cameraId!",
                    throwable
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getCameraExtensionCharacteristics(
        cameraId: CameraId
    ): CameraExtensionCharacteristics {
        synchronized(extensionCharacteristicsCache) {
            val existing = extensionCharacteristicsCache[cameraId.value]
            if (existing != null) {
                return existing
            }
        }
        Log.debug { "Retrieving CameraExtensionCharacteristics for $cameraId" }
        val cameraManager =
            cameraPipeContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val extensionCharacteristics =
            Api31Compat.getCameraExtensionCharacteristics(cameraManager, cameraId.value)

        // This technically shouldn't be null per documentation, but we suspect it could be
        // under certain devices in certain situations.
        @Suppress("RedundantRequireNotNullCall")
        checkNotNull(extensionCharacteristics) {
            "Failed to get CameraExtensionCharacteristics for $cameraId!"
        }

        return extensionCharacteristics
    }

    private fun isMetadataRedacted(): Boolean = !permissions.hasCameraPermission

    private fun shouldBlockSensorOrientationCache(characteristics: CameraCharacteristics): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 &&
            characteristics[CameraCharacteristics.INFO_DEVICE_STATE_SENSOR_ORIENTATION_MAP] != null
    }
}
