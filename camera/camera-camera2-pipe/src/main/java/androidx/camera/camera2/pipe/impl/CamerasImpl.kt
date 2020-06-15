/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CameraManager
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Cameras
import androidx.camera.camera2.pipe.impl.Debug.trace
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Provides utilities for querying cameras and accessing metadata about those cameras.
 */
@Singleton
class CamerasImpl @Inject constructor(
    private val cameraManager: Provider<CameraManager>,
    private val metadata: CameraMetadataCache
) : Cameras {

    private val cameras = lazy(LazyThreadSafetyMode.PUBLICATION) {
        // NOTE: Publication safety mode may cause this method to be invoked more than once if there
        //  a race between multiple threads. Only one return value will ultimately be cached.

        trace("cameraIdList") {
            val cameraManager = cameraManager.get()

            // TODO(b/159052778): Find a way to make this poll for the camera list and to suspend
            //  if the value is not yet available. It will be important to detect and differentiate
            //  between devices that should have cameras, but don't, vs devices that do not
            //  physically have cameras. It may also be worthwhile to re-query if looking for
            //  external cameras.

            // WARNING: This method can, at times, return an empty list of cameras on devices that
            //  will normally return a valid list of cameras (b/159052778)
            val cameraIdList = cameraManager.cameraIdList
            cameraIdList.map { CameraId(it) }
        }
    }

    override fun findAll(): List<CameraId> = cameras.value

    override suspend fun getMetadata(camera: CameraId): CameraMetadata {
        return metadata.get(camera)
    }

    override fun awaitMetadata(camera: CameraId): CameraMetadata {
        return metadata.awaitMetadata(camera)
    }
}