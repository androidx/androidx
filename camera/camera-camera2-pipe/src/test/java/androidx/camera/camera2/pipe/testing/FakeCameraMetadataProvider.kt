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

import android.hardware.camera2.CameraExtensionCharacteristics
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.compat.Camera2MetadataProvider

/** Utility class for providing fake metadata for tests. */
@RequiresApi(21)
class FakeCameraMetadataProvider(
    private val fakeMetadata: Map<CameraId, CameraMetadata> = emptyMap(),
    private val fakeExtensionMetadata: Map<CameraId, CameraExtensionMetadata> = emptyMap()
) : Camera2MetadataProvider {
    override suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata =
        awaitCameraMetadata(cameraId)

    override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata =
        checkNotNull(fakeMetadata[cameraId]) {
            "Failed to find metadata for $cameraId. Available fakeMetadata is $fakeMetadata"
        }

    override fun getCameraExtensionCharacteristics(
        cameraId: CameraId
    ): CameraExtensionCharacteristics {
        TODO("b/299356087 - Add support for fake extension metadata")
    }

    override suspend fun getCameraExtensionMetadata(
        cameraId: CameraId,
        extension: Int
    ): CameraExtensionMetadata = awaitCameraExtensionMetadata(cameraId, extension)

    override fun awaitCameraExtensionMetadata(
        cameraId: CameraId,
        extension: Int
    ): CameraExtensionMetadata =
        checkNotNull(fakeExtensionMetadata[cameraId]) {
            "Failed to find extension metadata for $cameraId. Available " +
                "fakeExtensionMetadata is $fakeExtensionMetadata"
        }
}
