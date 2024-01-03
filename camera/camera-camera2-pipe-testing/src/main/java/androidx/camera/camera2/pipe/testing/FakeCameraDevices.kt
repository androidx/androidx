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

package androidx.camera.camera2.pipe.testing

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata

/**
 * This provides a fake implementation of [CameraDevices] for tests with a fixed list of Cameras.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class FakeCameraDevices(
    private val defaultCameraBackendId: CameraBackendId,
    private val concurrentCameraBackendIds: Set<Set<CameraBackendId>>,
    private val cameraMetadataMap: Map<CameraBackendId, List<CameraMetadata>>
) : CameraDevices {
    init {
        check(cameraMetadataMap.containsKey(defaultCameraBackendId)) {
            "FakeCameraDevices must include $defaultCameraBackendId"
        }
    }

    override suspend fun getCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? =
        awaitCameraIds(cameraBackendId)

    override fun awaitCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? {
        val backendId = cameraBackendId ?: defaultCameraBackendId
        return cameraMetadataMap[backendId]?.map { it.camera }
    }

    override suspend fun getConcurrentCameraIds(
        cameraBackendId: CameraBackendId?
    ): Set<Set<CameraId>> = awaitConcurrentCameraIds(cameraBackendId)

    override fun awaitConcurrentCameraIds(cameraBackendId: CameraBackendId?): Set<Set<CameraId>> {
        return concurrentCameraBackendIds.map { concurrentCameraIds ->
            concurrentCameraIds.map {
                    cameraId -> CameraId.fromCamera2Id(cameraId.value)
            }.toSet()
        }.toSet()
    }

    override suspend fun getCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId?
    ): CameraMetadata? = awaitCameraMetadata(cameraId, cameraBackendId)

    override fun awaitCameraMetadata(
        cameraId: CameraId,
        cameraBackendId: CameraBackendId?
    ): CameraMetadata? {
        val backendId = cameraBackendId ?: defaultCameraBackendId
        return cameraMetadataMap[backendId]?.firstOrNull { it.camera == cameraId }
    }

    @Deprecated(
        "findAll() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("awaitCameraIds"),
        level = DeprecationLevel.WARNING
    )
    override fun findAll(): List<CameraId> = checkNotNull(awaitCameraIds())

    @Deprecated(
        "ids() is not able to specify a specific CameraBackendId to query.",
        replaceWith = ReplaceWith("getCameraIds"),
        level = DeprecationLevel.WARNING
    )
    override suspend fun ids(): List<CameraId> = checkNotNull(getCameraIds())

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
}
