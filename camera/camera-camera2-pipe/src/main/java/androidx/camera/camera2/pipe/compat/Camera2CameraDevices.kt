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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides utilities for querying cameras and accessing metadata about those cameras.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class Camera2CameraDevices @Inject constructor(
    private val deviceCache: Camera2DeviceCache,
    private val metadataCache: Camera2MetadataCache
) : CameraDevices {
    override fun findAll(): List<CameraId> = runBlocking { deviceCache.getCameras() }
    override suspend fun ids(): List<CameraId> = deviceCache.getCameras()

    override suspend fun getMetadata(camera: CameraId): CameraMetadata =
        metadataCache.getMetadata(camera)

    override fun awaitMetadata(camera: CameraId): CameraMetadata =
        metadataCache.awaitMetadata(camera)
}