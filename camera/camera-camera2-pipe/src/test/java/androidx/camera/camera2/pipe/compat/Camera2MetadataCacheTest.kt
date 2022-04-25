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

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.camera2.pipe.testing.FakeThreads
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Camera2MetadataCacheTest {
    @Test
    fun metadataIsCachedAndShimmed() {
        val camera0 = RobolectricCameras.create(
            mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to CameraCharacteristics
                    .INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.SENSOR_ORIENTATION to 90,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.FLASH_INFO_AVAILABLE to true
            )
        )

        val camera1 = RobolectricCameras.create(
            mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to CameraCharacteristics
                    .INFO_SUPPORTED_HARDWARE_LEVEL_3,
                CameraCharacteristics.SENSOR_ORIENTATION to 0,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT,
                CameraCharacteristics.FLASH_INFO_AVAILABLE to false
            )
        )

        val cache = Camera2MetadataCache(
            RobolectricCameras.application,
            FakeThreads.forTests,
            Permissions(RobolectricCameras.application),
            CameraPipe.CameraMetadataConfig()
        )

        val metadata0 = cache.awaitMetadata(camera0)
        val metadata1 = cache.awaitMetadata(camera1)

        // Check to make sure that metadata is not null, and that various properties do not crash
        // on older OS versions when accessed.
        assertThat(metadata0).isNotNull()
        assertThat(metadata0.camera).isEqualTo(camera0)
        assertThat(metadata0.isRedacted).isFalse()
        assertThat(metadata0.keys).isNotNull()
        assertThat(metadata0.requestKeys).isNotNull()
        assertThat(metadata0.resultKeys).isNotNull()
        assertThat(metadata0.sessionKeys).isNotNull()
        assertThat(metadata0.physicalCameraIds).isNotNull()
        assertThat(metadata0.physicalRequestKeys).isNotNull()
        assertThat(metadata0[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(2)

        assertThat(metadata1).isNotNull()
        assertThat(metadata1.camera).isEqualTo(camera1)
        assertThat(metadata1.isRedacted).isFalse()
        assertThat(metadata1.keys).isNotNull()
        assertThat(metadata1.requestKeys).isNotNull()
        assertThat(metadata1.resultKeys).isNotNull()
        assertThat(metadata1.sessionKeys).isNotNull()
        assertThat(metadata1.physicalCameraIds).isNotNull()
        assertThat(metadata1.physicalRequestKeys).isNotNull()
        assertThat(metadata1[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(3)
    }
}