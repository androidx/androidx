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

@file:Suppress("NOTHING_TO_INLINE")
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Methods for querying, iterating, and selecting the Cameras that are available on the device.
 */
public interface CameraDevices {

    /**
     * Iterate and return a list of CameraId's on the device that are capable of being opened. Some
     * camera devices may be hidden or un-openable if they are included as part of a logical camera
     * group.
     */
    @Deprecated(
        message = "findAll may block the calling thread and is deprecated.",
        replaceWith = ReplaceWith("ids"),
        level = DeprecationLevel.WARNING
    )
    public fun findAll(): List<CameraId>

    /**
     * Load the list of CameraIds from the Camera2 CameraManager, suspending if the list of
     * CameraIds has not yet been loaded.
     */
    public suspend fun ids(): List<CameraId>

    /**
     * Load CameraMetadata for a specific CameraId. Loading CameraMetadata can take a
     * non-zero amount of time to execute. If CameraMetadata is not already cached this function
     * will suspend until CameraMetadata can be loaded.
     */
    public suspend fun getMetadata(camera: CameraId): CameraMetadata

    /**
     * Load CameraMetadata for a specific CameraId and block the calling thread until the result is
     * available.
     */
    public fun awaitMetadata(camera: CameraId): CameraMetadata
}

@JvmInline
public value class CameraId(public val value: String) {
    public companion object {
        public inline fun fromCamera2Id(value: String): CameraId = CameraId(value)
        public inline fun fromCamera1Id(value: Int): CameraId = CameraId("$value")
    }

    /**
     * Attempt to parse an camera1 id from a camera2 id.
     *
     * @return The parsed Camera1 id, or null if the value cannot be parsed as a Camera1 id.
     */
    public inline fun toCamera1Id(): Int? = value.toIntOrNull()
    public override fun toString(): String = "Camera $value"
}

/**
 * Produce a [Flow]<[CameraMetadata]>, optionally expanding the list to include the physical
 * metadata of cameras that are otherwise hidden. Metadata for hidden cameras are always returned
 * last.
 */
public fun CameraDevices.find(includeHidden: Boolean = false): Flow<CameraMetadata> =
    flow {
        val cameras = this@find.ids()
        val visited = mutableSetOf<CameraId>()

        for (id in cameras) {
            if (visited.add(id)) {
                val metadata = this@find.getMetadata(id)
                emit(metadata)
            }
        }

        if (includeHidden) {
            for (id in cameras) {
                val metadata = this@find.getMetadata(id)
                for (physicalId in metadata.physicalCameraIds) {
                    if (visited.add(physicalId)) {
                        val physicalMetadata = this@find.getMetadata(id)
                        emit(physicalMetadata)
                    }
                }
            }
        }
    }
