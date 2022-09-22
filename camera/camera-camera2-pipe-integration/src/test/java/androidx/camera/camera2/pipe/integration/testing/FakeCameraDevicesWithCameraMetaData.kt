/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import kotlinx.coroutines.runBlocking

class FakeCameraDevicesWithCameraMetaData(
    private val cameraMetadataMap: Map<CameraId, CameraMetadata>,
    private val defaultCameraMetadata: CameraMetadata
) : CameraDevices {
    @Deprecated(
        message = "findAll may block the calling thread and is deprecated.",
        replaceWith = ReplaceWith("ids"),
        level = DeprecationLevel.WARNING
    )
    override fun findAll(): List<CameraId> = runBlocking { ids() }
    override suspend fun ids(): List<CameraId> = cameraMetadataMap.keys.toList()
    override suspend fun getMetadata(camera: CameraId): CameraMetadata = awaitMetadata(camera)
    override fun awaitMetadata(camera: CameraId) =
        cameraMetadataMap[camera] ?: defaultCameraMetadata
}