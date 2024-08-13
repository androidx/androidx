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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@JvmDefaultWithCompatibility
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/** Methods for querying, iterating, and selecting the Cameras that are available on the device. */
public interface CameraDevices {
    /**
     * Read the list of currently openable [CameraId]s from the provided CameraBackend, suspending
     * if needed. By default this will load the list of openable [CameraId]s from the default
     * backend.
     */
    public suspend fun getCameraIds(cameraBackendId: CameraBackendId? = null): List<CameraId>?

    /**
     * Read the list of currently openable [CameraId]s from the provided CameraBackend, blocking the
     * thread if needed. By default this will load the list of openable [CameraId]s from the default
     * backend.
     */
    public fun awaitCameraIds(cameraBackendId: CameraBackendId? = null): List<CameraId>?

    /**
     * Read the set of [CameraId] sets that can be operated concurrently from the provided
     * CameraBackend, suspending if needed. By default this will load the set of [CameraId] sets
     * from the default backend.
     */
    public suspend fun getConcurrentCameraIds(
        cameraBackendId: CameraBackendId? = null
    ): Set<Set<CameraId>>?

    /**
     * Read the set of [CameraId] sets that can be operated concurrently from the provided
     * CameraBackend, blocking the thread if needed. By default this will load the set of [CameraId]
     * sets from the default backend.
     */
    public fun awaitConcurrentCameraIds(
        cameraBackendId: CameraBackendId? = null
    ): Set<Set<CameraId>>?

    /**
     * Read metadata for a specific camera id, suspending if needed. By default, this method will
     * query metadata from the default backend if one is not specified.
     */
    public suspend fun getCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId? = null
    ): CameraMetadata?

    /**
     * Read metadata for a specific camera id, blocking if needed. By default, this method will
     * query metadata from the default backend if one is not specified.
     */
    public fun awaitCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId? = null
    ): CameraMetadata?

    /**
     * Opens the camera device indicated by the cameraId, so that any subsequent open calls will
     * potentially have a better latency.
     */
    public fun prewarm(cameraId: CameraId, cameraBackendId: CameraBackendId? = null)

    /** Non blocking operation that disconnects the underlying active Camera. */
    public fun disconnect(cameraId: CameraId, cameraBackendId: CameraBackendId? = null)

    /**
     * Disconnects the underlying active Camera. Once fully closed, the returned [Deferred] should
     * be completed. It is synchronous with the other operations within this class.
     */
    public fun disconnectAsync(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId? = null
    ): Deferred<Unit>

    /** Non blocking operation that disconnects all active Cameras. */
    public fun disconnectAll(cameraBackendId: CameraBackendId? = null)

    /**
     * Non blocking operation that disconnects all active Cameras. Once all connections are fully
     * closed, the returned [Deferred] should be completed. It is synchronous with the other
     * operations within this class.
     */
    public fun disconnectAllAsync(cameraBackendId: CameraBackendId? = null): Deferred<Unit>

    /**
     * Iterate and return a list of CameraId's on the device that are capable of being opened. Some
     * camera devices may be hidden or un-openable if they are included as part of a logical camera
     * group.
     */
    @Deprecated(
        message = "findAll() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("awaitCameraIds"),
        level = DeprecationLevel.WARNING
    )
    public fun findAll(): List<CameraId>

    /**
     * Load the list of CameraIds from the Camera2 CameraManager, suspending if the list of
     * CameraIds has not yet been loaded.
     */
    @Deprecated(
        message = "ids() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("getCameraIds"),
        level = DeprecationLevel.WARNING
    )
    public suspend fun ids(): List<CameraId>

    /**
     * Load CameraMetadata for a specific CameraId. Loading CameraMetadata can take a non-zero
     * amount of time to execute. If CameraMetadata is not already cached this function will suspend
     * until CameraMetadata can be loaded.
     */
    @Deprecated(
        message = "getMetadata() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("getCameraMetadata"),
        level = DeprecationLevel.WARNING
    )
    public suspend fun getMetadata(camera: CameraId): CameraMetadata

    /**
     * Load CameraMetadata for a specific CameraId and block the calling thread until the result is
     * available.
     */
    @Deprecated(
        message = "awaitMetadata() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("awaitCameraMetadata"),
        level = DeprecationLevel.WARNING
    )
    public fun awaitMetadata(camera: CameraId): CameraMetadata
}

/** CameraId represents a typed identifier for a camera represented as a non-blank String. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class CameraId(public val value: String) {
    init {
        require(value.isNotBlank()) { "CameraId cannot be null or blank!" }
    }

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

    override fun toString(): String = "CameraId-$value"
}

/**
 * Produce a [Flow]<[CameraMetadata]>, optionally expanding the list to include the physical
 * metadata of cameras that are otherwise hidden. Metadata for hidden cameras are always returned
 * last.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun CameraDevices.find(
    cameraBackendId: CameraBackendId? = null,
    includePhysicalCameraMetadata: Boolean = false
): Flow<CameraMetadata> = flow {
    val cameraIds = this@find.getCameraIds() ?: return@flow

    val visited = mutableSetOf<CameraId>()
    val emitted = mutableSetOf<CameraMetadata>()
    for (cameraId in cameraIds) {
        if (visited.add(cameraId)) {
            val metadata = this@find.getCameraMetadata(cameraId, cameraBackendId)
            if (metadata != null) {
                emitted.add(metadata)
                emit(metadata)
            }
        }
    }

    if (includePhysicalCameraMetadata) {
        for (metadata in emitted) {
            for (physicalId in metadata.physicalCameraIds) {
                if (!visited.contains(physicalId)) {
                    val physicalMetadata = this@find.getCameraMetadata(physicalId, cameraBackendId)
                    if (
                        physicalMetadata != null &&
                            physicalMetadata.camera == physicalId &&
                            visited.add(physicalId)
                    ) {
                        emit(physicalMetadata)
                    }
                }
            }
        }
    }
}
