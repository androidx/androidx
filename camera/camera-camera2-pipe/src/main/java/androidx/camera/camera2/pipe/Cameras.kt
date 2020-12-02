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

package androidx.camera.camera2.pipe

/**
 * Methods for querying, iterating, and selecting the Cameras that are available on the device.
 */
interface Cameras {
    /**
     * Iterate and return a list of CameraId's on the device that are capable of being opened. Some
     * camera devices may be hidden or un-openable if they are included as part of a logical camera
     * group.
     */
    fun findAll(): List<CameraId>

    /**
     * Load CameraMetadata for a specific CameraId. Loading CameraMetadata can take a
     * non-zero amount of time to execute. If CameraMetadata is not already cached this function
     * will suspend until CameraMetadata can be loaded.
     */
    suspend fun getMetadata(camera: CameraId): CameraMetadata

    /**
     * Load CameraMetadata for a specific CameraId and block the calling thread until the result is
     * available.
     */
    fun awaitMetadata(camera: CameraId): CameraMetadata
}