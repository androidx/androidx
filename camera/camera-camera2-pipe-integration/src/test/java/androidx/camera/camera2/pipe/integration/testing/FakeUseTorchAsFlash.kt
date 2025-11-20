/*
 * Copyright 2025 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlash
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlashImpl
import androidx.camera.camera2.pipe.integration.internal.IntrinsicZoomCalculator
import androidx.camera.camera2.pipe.integration.internal.IntrinsicZoomCalculator.Companion.NO_OP_INTRINSIC_ZOOM_CALCULATOR
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import org.robolectric.shadows.StreamConfigurationMapBuilder

object FakeUseTorchAsFlash {
    fun createUseTorchAsFlash(
        lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK,
        forceEnable: Boolean = false,
        intrinsicZoomCalculator: IntrinsicZoomCalculator = NO_OP_INTRINSIC_ZOOM_CALCULATOR,
    ): UseTorchAsFlash {
        val metadata =
            FakeCameraMetadata(
                characteristics = mapOf(CameraCharacteristics.LENS_FACING to lensFacing),
                cameraId =
                    if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) CameraId("0")
                    else CameraId("1"),
            )

        val cameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = CameraBackendId(metadata.camera.value),
                concurrentCameraBackendIds =
                    setOf(
                        setOf(CameraBackendId("0"), CameraBackendId("1")),
                        setOf(CameraBackendId("0"), CameraBackendId("2")),
                    ),
                cameraMetadataMap =
                    mapOf(CameraBackendId(metadata.camera.value) to listOf(metadata)),
            )

        val cameraQuirks =
            CameraQuirks(
                metadata,
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        FakeCameraMetadata(),
                        StreamConfigurationMapBuilder.newBuilder().build(),
                    ),
                ),
            )

        return if (forceEnable) {
            UseTorchAsFlashImpl(cameraQuirks, cameraDevices, intrinsicZoomCalculator)
        } else {
            UseTorchAsFlash.Bindings.provideUseTorchAsFlash(
                cameraQuirks,
                cameraDevices,
                intrinsicZoomCalculator,
            )
        }
    }
}
