/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraBackends
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred

/** Provides utilities for querying cameras and accessing metadata about those cameras. */
@Singleton
internal class CameraDevicesImpl @Inject constructor(private val cameraBackends: CameraBackends) :
    CameraDevices {

    @Deprecated(
        "findAll() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("awaitCameraIds"),
        level = DeprecationLevel.WARNING
    )
    override fun findAll(): List<CameraId> = awaitCameraIds() ?: emptyList()

    @Deprecated(
        "ids() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("getCameraIds"),
        level = DeprecationLevel.WARNING
    )
    override suspend fun ids(): List<CameraId> = getCameraIds() ?: emptyList()

    @Deprecated(
        "getMetadata() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("getCameraMetadata"),
        level = DeprecationLevel.WARNING
    )
    override suspend fun getMetadata(camera: CameraId): CameraMetadata =
        checkNotNull(getCameraMetadata(camera))

    @Deprecated(
        "awaitMetadata() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("awaitCameraMetadata"),
        level = DeprecationLevel.WARNING
    )
    override fun awaitMetadata(camera: CameraId): CameraMetadata =
        checkNotNull(awaitCameraMetadata(camera))

    override suspend fun getCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        val cameraIds = cameraBackend.getCameraIds()
        if (cameraIds == null) {
            Log.warn { "Failed to load cameraIds from ${cameraBackend.id}" }
        }
        return cameraIds
    }

    override fun awaitCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        val cameraIds = cameraBackend.awaitCameraIds()
        if (cameraIds == null) {
            Log.warn { "Failed to load cameraIds from ${cameraBackend.id}" }
        }
        return cameraIds
    }

    override suspend fun getConcurrentCameraIds(
        cameraBackendId: CameraBackendId?
    ): Set<Set<CameraId>>? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        return cameraBackend.getConcurrentCameraIds()
    }

    override fun awaitConcurrentCameraIds(cameraBackendId: CameraBackendId?): Set<Set<CameraId>>? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        return cameraBackend.awaitConcurrentCameraIds()
    }

    override suspend fun getCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId?
    ): CameraMetadata? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        val metadata = cameraBackend.getCameraMetadata(cameraId)
        if (metadata == null) {
            Log.warn { "Failed to load metadata for $cameraId from ${cameraBackend.id}" }
        }
        return metadata
    }

    override fun awaitCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId?
    ): CameraMetadata? {
        val cameraBackend = getCameraBackend(cameraBackendId)
        val metadata = cameraBackend.awaitCameraMetadata(cameraId)
        if (metadata == null) {
            Log.warn { "Failed to load metadata for $cameraId from ${cameraBackend.id}" }
        }
        return metadata
    }

    override fun prewarm(cameraId: CameraId, cameraBackendId: CameraBackendId?) {
        val cameraBackend = getCameraBackend(cameraBackendId)
        cameraBackend.prewarm(cameraId)
    }

    override fun disconnect(cameraId: CameraId, cameraBackendId: CameraBackendId?) {
        val cameraBackend = getCameraBackend(cameraBackendId)
        cameraBackend.disconnect(cameraId)
    }

    override fun disconnectAsync(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId?
    ): Deferred<Unit> {
        val cameraBackend = getCameraBackend(cameraBackendId)
        return cameraBackend.disconnectAsync(cameraId)
    }

    override fun disconnectAll(cameraBackendId: CameraBackendId?) {
        val cameraBackend = getCameraBackend(cameraBackendId)
        cameraBackend.disconnectAll()
    }

    override fun disconnectAllAsync(cameraBackendId: CameraBackendId?): Deferred<Unit> {
        val cameraBackend = getCameraBackend(cameraBackendId)
        return cameraBackend.disconnectAllAsync()
    }

    private fun getCameraBackend(cameraBackendId: CameraBackendId?): CameraBackend =
        Debug.trace("getCameraBackend") {
            val actualBackendId = cameraBackendId ?: cameraBackends.default.id
            val backend = cameraBackends[actualBackendId]
            checkNotNull(backend) { "Failed to load CameraBackend $actualBackendId" }
        }
}
