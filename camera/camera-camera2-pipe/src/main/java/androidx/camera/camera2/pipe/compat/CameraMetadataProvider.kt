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

import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * Interface that can be used to query for [CameraMetadata] using an existing [CameraId].
 */
interface CameraMetadataProvider {
    /**
     * Attempt to retrieve [CameraMetadata], suspending the caller if it is not yet available.
     */
    suspend fun getMetadata(cameraId: CameraId): CameraMetadata

    /**
     * Attempt to retrieve [CameraMetadata], blocking the calling thread if it is not yet available.
     */
    fun awaitMetadata(cameraId: CameraId): CameraMetadata
}