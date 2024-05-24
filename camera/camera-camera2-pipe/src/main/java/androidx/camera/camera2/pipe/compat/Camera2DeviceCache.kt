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
import kotlinx.coroutines.withContext

@Singleton
internal class Camera2DeviceCache
@Inject
constructor(
    private val cameraManager: Provider<CameraManager>,
    private val threads: Threads,
) {
    private val lock = Any()

    @GuardedBy("lock") private var openableCameras: List<CameraId>? = null

    @GuardedBy("lock") private var concurrentCameras: Set<Set<CameraId>>? = null

    suspend fun getCameraIds(): List<CameraId> {
        val cameras = synchronized(lock) { openableCameras }
        if (!cameras.isNullOrEmpty()) {
            return cameras
        }

        // Suspend and query the list of Cameras on the ioDispatcher
        return withContext(threads.backgroundDispatcher) {
            Debug.trace("readCameraIds") {
                val cameraIds = awaitCameraIds()

                if (!cameraIds.isNullOrEmpty()) {
                    synchronized(lock) { openableCameras = cameraIds }
                    return@trace cameraIds
                }

                // TODO(b/159052778): Find a way to make this poll for the camera list and to
                //  suspend if the value is not yet available. It will be important to detect and
                //  differentiate between devices that should have cameras, but don't, vs devices
                //  that do not physically have cameras. It may also be worthwhile to re-query if
                //  looking for external cameras, since they can be attached and/or detached.

                return@trace listOf<CameraId>()
            }
        }
    }

    fun awaitCameraIds(): List<CameraId>? {
        val cameras = synchronized(lock) { openableCameras }
        if (!cameras.isNullOrEmpty()) {
            return cameras
        }

        val cameraManager = cameraManager.get()
        val cameraIdArray =
            try {
                // WARNING: This method can, at times, return an empty list of cameras on devices
                // that will normally return a valid list of cameras (b/159052778)
                val ids = cameraManager.cameraIdList
                Log.info { "Loaded CameraIdList $ids" }
                ids
            } catch (e: CameraAccessException) {
                Log.warn(e) { "Failed to query CameraManager#getCameraIdList!" }
                return null
            }
        if (cameraIdArray.isEmpty()) {
            Log.warn { "Failed to query CameraManager#getCameraIdList: No values returned." }
            return emptyList()
        }
        return cameraIdArray.map { CameraId(it) }
    }

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
}
