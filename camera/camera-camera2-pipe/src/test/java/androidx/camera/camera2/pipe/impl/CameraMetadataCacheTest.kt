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

package androidx.camera.camera2.pipe.impl

import android.Manifest
import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

@SmallTest
@RunWith(CameraPipeRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraMetadataCacheTest {
    companion object {
        const val CAMERA0_ID = "0"
        const val CAMERA0_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        const val CAMERA0_SENSOR_ORIENTATION = 90
        const val CAMERA0_LENS_FACING = CameraCharacteristics.LENS_FACING_BACK
        const val CAMERA0_FLASH_INFO_AVAILABLE = true

        const val CAMERA1_ID = "1"
        const val CAMERA1_SUPPORTED_HARDWARE_LEVEL =
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        const val CAMERA1_SENSOR_ORIENTATION = 0
        const val CAMERA1_LENS_FACING_INT = CameraCharacteristics.LENS_FACING_FRONT
        const val CAMERA1_FLASH_INFO_AVAILABLE = false
    }

    @Test
    fun metadataIsCachedAndShimmed() {
        configureShadowCameras()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = CameraMetadataCache(context)

        val metadata0 = cache.awaitMetadata(CameraId("0"))
        val metadata1 = cache.awaitMetadata(CameraId("1"))

        // Check to make sure that metadata is not null, and that various properties do not crash
        // on older OS versions when accessed.
        assertThat(metadata0).isNotNull()
        assertThat(metadata0.camera).isEqualTo(CameraId("0"))
        assertThat(metadata0.isRedacted).isFalse()
        assertThat(metadata0.keys).isNotNull()
        assertThat(metadata0.requestKeys).isNotNull()
        assertThat(metadata0.resultKeys).isNotNull()
        assertThat(metadata0.sessionKeys).isNotNull()
        assertThat(metadata0.physicalCameraIds).isNotNull()
        assertThat(metadata0.physicalRequestKeys).isNotNull()
        assertThat(metadata0[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(
            CAMERA0_SUPPORTED_HARDWARE_LEVEL
        )

        assertThat(metadata1).isNotNull()
        assertThat(metadata1.camera).isEqualTo(CameraId("1"))
        assertThat(metadata1.isRedacted).isFalse()
        assertThat(metadata1.keys).isNotNull()
        assertThat(metadata1.requestKeys).isNotNull()
        assertThat(metadata1.resultKeys).isNotNull()
        assertThat(metadata1.sessionKeys).isNotNull()
        assertThat(metadata1.physicalCameraIds).isNotNull()
        assertThat(metadata1.physicalRequestKeys).isNotNull()
        assertThat(metadata1[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(
            CAMERA1_SUPPORTED_HARDWARE_LEVEL
        )
    }

    private fun configureShadowCameras() {
        val app: Application =
            ApplicationProvider.getApplicationContext()
        val shadowApp: ShadowApplication = Shadows.shadowOf(app)
        shadowApp.grantPermissions(Manifest.permission.CAMERA)

        val shadowCameraManager = Shadow.extract<Any>(
            app.getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager

        val characteristics0 = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics0 = Shadow.extract<ShadowCameraCharacteristics>(characteristics0)
        shadowCharacteristics0.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            CAMERA0_SUPPORTED_HARDWARE_LEVEL
        )
        // Add a lens facing to the camera
        shadowCharacteristics0.set(
            CameraCharacteristics.LENS_FACING,
            CAMERA0_LENS_FACING
        )
        // Mock the sensor orientation
        shadowCharacteristics0.set(
            CameraCharacteristics.SENSOR_ORIENTATION,
            CAMERA0_SENSOR_ORIENTATION
        )
        // Mock the flash unit availability
        shadowCharacteristics0.set(
            CameraCharacteristics.FLASH_INFO_AVAILABLE,
            CAMERA0_FLASH_INFO_AVAILABLE
        )
        // Add the camera to the camera service
        shadowCameraManager.addCamera(CAMERA0_ID, characteristics0)

        // **** Camera 1 characteristics ****//
        val characteristics1 =
            ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics1 =
            Shadow.extract<ShadowCameraCharacteristics>(characteristics1)
        shadowCharacteristics1.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            CAMERA1_SUPPORTED_HARDWARE_LEVEL
        )
        // Add a lens facing to the camera
        shadowCharacteristics1.set(
            CameraCharacteristics.LENS_FACING,
            CAMERA1_LENS_FACING_INT
        )
        // Mock the sensor orientation
        shadowCharacteristics1.set(
            CameraCharacteristics.SENSOR_ORIENTATION,
            CAMERA1_SENSOR_ORIENTATION
        )
        // Mock the flash unit availability
        shadowCharacteristics1.set(
            CameraCharacteristics.FLASH_INFO_AVAILABLE,
            CAMERA1_FLASH_INFO_AVAILABLE
        )
        // Add the camera to the camera service
        shadowCameraManager.addCamera(CAMERA1_ID, characteristics1)
    }
}