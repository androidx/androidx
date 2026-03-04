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
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.config.CameraPipeContext
import androidx.camera.camera2.pipe.config.CameraPipeJob
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
    @CameraPipeContext private val context: Context,
    packageManager: PackageManager,
    private val cameraErrorListener: CameraErrorListener,
    private val cameraDeviceSetupCompatFactoryProvider: Provider<CameraDeviceSetupCompatFactory>,
    cameraPipeLifetime: CameraPipeLifetime,
    @CameraPipeJob cameraPipeJob: Job,
) {
    private val scope =
        CoroutineScope(
            SupervisorJob(cameraPipeJob) +
                threads.lightweightDispatcher +
                CoroutineName("Camera2DeviceCache")
        )
    private val lock = Any()

    @GuardedBy("lock") private var openableCameras: List<CameraId>? = null

    @GuardedBy("lock") private var concurrentCameras: Set<Set<CameraId>>? = null

    @GuardedBy("lock")
    private val cameraDeviceSetupCache =
        mutableMapOf<CameraId, Deferred<CameraDeviceSetupCompat?>>()

    @GuardedBy("lock")
    private val camera2DeviceSetupWrapperCache =
        mutableMapOf<CameraId, Deferred<Camera2DeviceSetupWrapper?>>()

    private val minimumCameraCount = estimateMinInternalCameraCount(packageManager)

    init {
        Log.debug { "Camera2DeviceCache: Expected minimum camera count = $minimumCameraCount" }

        cameraPipeLifetime.addShutdownAction(CameraPipeLifetime.ShutdownType.SCOPE) {
            scope.cancel()
        }
    }

    val cameraIds: Flow<List<CameraId>> =
        createCameraIdListFlow()
            .distinctUntilChanged()
            .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)
    private val cameraDeviceSetupCompatFactory by lazy {
        cameraDeviceSetupCompatFactoryProvider.get()
    }

    /**
     * Retrieves a cached or new [CameraDeviceSetupCompat], returning `null` on failure.
     *
     * Failed initialization attempts are removed from the cache to allow for retries, which may
     * succeed if the failure was due to a transient state (e.g. CameraAccessException).
     */
    suspend fun getOrInitializeDeviceSetupCompat(cameraId: CameraId): CameraDeviceSetupCompat? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return null
        val deferred =
            synchronized(lock) {
                cameraDeviceSetupCache.getOrPut(cameraId) {
                    scope.async(threads.backgroundDispatcher) {
                        Log.debug { "Initializing CameraDeviceSetupCompat for $cameraId" }
                        catchAndReportCameraExceptions(cameraId, cameraErrorListener) {
                            cameraDeviceSetupCompatFactory.getCameraDeviceSetupCompat(
                                cameraId.value
                            )
                        }
                    }
                }
            }

        val deferredResult = deferred.await()

        if (deferredResult == null) {
            Log.debug { "Removing null CameraDeviceSetupCompat from cache for $cameraId" }
            synchronized(lock) { cameraDeviceSetupCache.remove(cameraId, deferred) }
        }
        return deferredResult
    }

    /**
     * Retrieves a cached or new [Camera2DeviceSetupWrapper], returning `null` on failure.
     *
     * Failed initialization attempts are removed from the cache to allow for retries, which may
     * succeed if the failure was due to a transient state (e.g. CameraAccessException).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun getOrInitializeDeviceSetupWrapper(cameraId: CameraId): Camera2DeviceSetupWrapper? {
        val deferred =
            synchronized(lock) {
                camera2DeviceSetupWrapperCache.getOrPut(cameraId) {
                    scope.async(threads.backgroundDispatcher) {
                        val isSupported =
                            catchAndReportCameraExceptions(cameraId, cameraErrorListener) {
                                cameraManager.get().isCameraDeviceSetupSupported(cameraId.value)
                            }

                        if (isSupported != true) {
                            return@async null
                        }
                        Log.debug { "Initializing CameraDeviceSetup for $cameraId" }
                        catchAndReportCameraExceptions(cameraId, cameraErrorListener) {
                                cameraManager.get().getCameraDeviceSetup(cameraId.value)
                            }
                            ?.let { cameraDeviceSetup ->
                                Camera2DeviceSetup(cameraDeviceSetup, cameraId, cameraErrorListener)
                            }
                    }
                }
            }
        val deferredResult = deferred.await()

        if (deferredResult == null) {
            Log.debug { "Removing null camera2DeviceSetupWrapper from cache for $cameraId" }
            synchronized(lock) { camera2DeviceSetupWrapperCache.remove(cameraId, deferred) }
        }
        return deferredResult
    }

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
            } catch (e: ArrayIndexOutOfBoundsException) {
                // getCameraIdList() can throw ArrayIndexOutOfBoundsException: b/443332525
                Log.warn(e) {
                    "Failed to query CameraManager#getCameraIdList!" +
                        "Unexpected ArrayIndexOutOfBoundsException thrown by framework."
                }
                return null
            } catch (e: NullPointerException) {
                // getCameraIdList() can return null on problematic problems, which then ran afoul
                // with kotlin intrinsics: b/450641047
                Log.warn(e) {
                    "Failed to query CameraManager#getCameraIdList!" +
                        "Null was returned by framework."
                }
                return null
            }
        val cameraIds = cameraIdArray.mapNotNull { CameraId(it) }
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
