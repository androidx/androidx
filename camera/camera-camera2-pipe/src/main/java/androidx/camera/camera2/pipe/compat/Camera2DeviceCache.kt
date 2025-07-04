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

import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

@Singleton
internal class Camera2DeviceCache
@Inject
constructor(
    private val cameraManager: Provider<CameraManager>,
    private val threads: Threads,
    packageManager: PackageManager,
) {
    private val scope =
        CoroutineScope(threads.lightweightDispatcher + CoroutineName("Camera2DeviceCache"))
    private val lock = Any()

    @GuardedBy("lock") private var openableCameras: List<CameraId>? = null

    @GuardedBy("lock") private var concurrentCameras: Set<Set<CameraId>>? = null

    private val minimumCameraCount = estimateMinInternalCameraCount(packageManager)

    init {
        Log.debug { "Camera2DeviceCache: Expected minimum camera count = $minimumCameraCount" }
    }

    val cameraIds: Flow<List<CameraId>> =
        createCameraIdListFlow()
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    suspend fun getCameraIds(): List<CameraId> {
        val cachedCameras = synchronized(lock) { openableCameras }
        if (cachedCameras != null) {
            return cachedCameras
        }

        // Suspend and query the list of Cameras on the ioDispatcher
        return withContext(threads.backgroundDispatcher) {
            Debug.trace("readCameraIds") {
                val cameraIds = readCameraIds()

                if (cameraIds != null) {
                    return@trace cameraIds
                }
                return@trace emptyList()
            }
        }
    }

    fun awaitCameraIds(): List<CameraId>? {
        val cachedCameras = synchronized(lock) { openableCameras }
        if (cachedCameras != null) {
            return cachedCameras
        }
        return readCameraIds()
    }

    private fun createCameraIdListFlow() =
        callbackFlow<List<CameraId>> {
            // Send the initial camera ID list first.
            val cachedCameras = synchronized(lock) { openableCameras }
            if (cachedCameras != null) {
                sendCameraIdList(cachedCameras)
            } else {
                // If the list is invalid, we still need to make sure there is an initial value.
                // Consider the case where all cameras are broken, meaning we don't ever get an
                // onCameraAvailable call for any cameras. In this case, we should still make sure
                // we at least emit an empty list, the true value.
                val cameraIds = readCameraIds()
                if (cameraIds != null) {
                    sendCameraIdList(cameraIds)
                }
            }

            val callback =
                object : CameraManager.AvailabilityCallback() {
                    override fun onCameraAvailable(cameraId: String) {
                        onCameraAvailabilityChanged(cameraId, isAvailable = true)
                    }

                    override fun onCameraUnavailable(cameraId: String) {
                        onCameraAvailabilityChanged(cameraId, isAvailable = false)
                    }
                }
            val cameraManager = cameraManager.get()
            cameraManager.registerAvailabilityCallback(callback, threads.camera2Handler)

            awaitClose { cameraManager.unregisterAvailabilityCallback(callback) }
        }

    private fun ProducerScope<List<CameraId>>.onCameraAvailabilityChanged(
        cameraId: String,
        isAvailable: Boolean,
    ) {
        val cachedCameraIds = synchronized(lock) { openableCameras }
        val cameraIdsRead =
            when (isAvailable) {
                true ->
                    if (cachedCameraIds == null || cachedCameraIds.all { it.value != cameraId }) {
                        Log.info { "New camera $cameraId detected" }
                        readCameraIds()
                    } else null
                false ->
                    if (cachedCameraIds == null || cachedCameraIds.any { it.value == cameraId }) {

                        Log.info { "Unavailable camera $cameraId detected" }
                        readCameraIds()
                    } else null
            }

        val updatedCameraIds = getUpdatedCameraIds(cachedCameraIds, cameraIdsRead)
        if (updatedCameraIds != null) {
            sendCameraIdList(updatedCameraIds)
        }
    }

    private fun getUpdatedCameraIds(
        cachedCameraIds: List<CameraId>?,
        cameraIdsRead: List<CameraId>?,
    ): List<CameraId>? {
        if (cameraIdsRead != null) {
            if (isValidCameraIds(cameraIdsRead)) {
                // If the read camera ID list is valid, it takes precedence. Return it.
                return cameraIdsRead
            } else if (cachedCameraIds == null) {
                // If the cached camera ID list is null (invalid), the read list, whether
                // valid or not, is still better than nothing. Return it.
                return cameraIdsRead
            } // else the cached camera ID list is valid, and we should use it.
        }
        return cachedCameraIds
    }

    private fun ProducerScope<List<CameraId>>.sendCameraIdList(cameraIds: List<CameraId>) {
        Log.debug { "Emitting camera ID list: $cameraIds" }
        trySendBlocking(cameraIds).onFailure {
            Log.error { "Failed to send camera ID list: $cameraIds!" }
        }
    }

    private fun readCameraIds(): List<CameraId>? {
        val cameraManager = cameraManager.get()
        val cameraIdArray =
            try {
                // WARNING: This method can, at times, return an empty list of cameras on devices
                // that will normally return a valid list of cameras (b/159052778)
                val ids = cameraManager.cameraIdList
                ids
            } catch (e: CameraAccessException) {
                Log.warn(e) { "Failed to query CameraManager#getCameraIdList!" }
                return null
            }
        val cameraIds = cameraIdArray.map { CameraId(it) }
        if (isValidCameraIds(cameraIds)) {
            // Only update the cached camera IDs if the list is valid.
            synchronized(lock) { openableCameras = cameraIds }
            Log.info { "Loaded CameraIdList $cameraIds" }
        } else {
            Log.warn { "Failed to query camera ID list: Invalid list returned: $cameraIds." }
        }
        return cameraIds
    }

    // Estimates the minimum internal camera count. When a device supports FEATURE_CAMERA, it
    // should have at least a back camera. When a device supports FEATURE_CAMERA_FRONT, it is
    // likewise expected to have a front camera. This is an estimate - it is possible for a device
    // to have multiple front or back cameras.
    private fun estimateMinInternalCameraCount(packageManager: PackageManager): Int {
        var minCameras = 0
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) minCameras++
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) minCameras++
        return minCameras
    }

    // Important: Even if the camera ID list is not considered valid, the list should be still
    // be returned. Builtin cameras may disappear due to transient errors, and faulty cameras may
    // also disappear permanently.
    private fun isValidCameraIds(cameraIds: List<CameraId>) = cameraIds.size >= minimumCameraCount

    suspend fun getConcurrentCameraIds(): Set<Set<CameraId>> {
        val cameras = synchronized(lock) { concurrentCameras }
        if (!cameras.isNullOrEmpty()) {
            return cameras
        }

        // Suspend and query the list of concurrent Cameras on the ioDispatcher
        return withContext(threads.backgroundDispatcher) {
            Debug.trace("readConcurrentCameraIds") {
                val cameraIds = awaitConcurrentCameraIds()

                if (!cameraIds.isNullOrEmpty()) {
                    synchronized(lock) { concurrentCameras = cameraIds }
                    return@trace cameraIds
                }

                return@trace emptySet()
            }
        }
    }

    fun awaitConcurrentCameraIds(): Set<Set<CameraId>>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return emptySet()
        }
        val cameras = synchronized(lock) { concurrentCameras }
        if (!cameras.isNullOrEmpty()) {
            return cameras
        }

        val cameraManager = cameraManager.get()
        val cameraIdsSet =
            try {
                val idSetSet = Api30Compat.getConcurrentCameraIds(cameraManager)
                Log.debug { "Loaded ConcurrentCameraIdsSet $idSetSet" }
                idSetSet
            } catch (e: CameraAccessException) {
                Log.warn(e) { "Failed to query CameraManager#getConcurrentStreamingCameraIds" }
                return null
            }
        return cameraIdsSet
            .map { it.map { cameraIdString -> CameraId(cameraIdString) }.toSet() }
            .toSet()
    }

    fun shutdown() {
        scope.cancel()
    }
}
