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

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.testing.FakeCameraBackend.Companion.FAKE_CAMERA_BACKEND_ID
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FakeCameraDevicesTest {
    private val EXTERNAL_BACKEND_ID =
        CameraBackendId("androidx.camera.camera2.pipe.testing.FakeCameraDevicesTest")
    private val metadata1 = FakeCameraMetadata(
        mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT)
    )
    private val metadata2 = FakeCameraMetadata(
        mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
    )
    private val metadata3 = FakeCameraMetadata(
        mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_EXTERNAL)
    )
    private val cameraMetadataMap = mapOf(
        FAKE_CAMERA_BACKEND_ID to listOf(metadata1, metadata2),
        EXTERNAL_BACKEND_ID to listOf(metadata3)
    )

    @Test
    fun getCameraIdsReturnsDefaultCameraIdList() = runTest {
        val cameraDevices = FakeCameraDevices(
            defaultCameraBackendId = FAKE_CAMERA_BACKEND_ID,
            concurrentCameraBackendIds = setOf(
                setOf(CameraBackendId("0"), CameraBackendId("1")),
                setOf(CameraBackendId("0"), CameraBackendId("2"))
            ),
            cameraMetadataMap = cameraMetadataMap
        )
        val devices = cameraDevices.getCameraIds()
        assertThat(devices).containsExactlyElementsIn(
            listOf(
                metadata1.camera,
                metadata2.camera
            )
        ).inOrder()

        assertThat(cameraDevices.getCameraMetadata(metadata1.camera)).isSameInstanceAs(metadata1)
        assertThat(cameraDevices.getCameraMetadata(metadata2.camera)).isSameInstanceAs(metadata2)
    }

    @Test
    fun getCameraIdsWithBackendReturnsCustomCameraIdList() = runTest {
        val cameraDevices = FakeCameraDevices(
            defaultCameraBackendId = FAKE_CAMERA_BACKEND_ID,
            concurrentCameraBackendIds = setOf(
                setOf(CameraBackendId("0"), CameraBackendId("1")),
                setOf(CameraBackendId("0"), CameraBackendId("2"))
            ),
            cameraMetadataMap = cameraMetadataMap
        )
        val devices = cameraDevices.getCameraIds(EXTERNAL_BACKEND_ID)
        assertThat(devices).containsExactlyElementsIn(
            listOf(
                metadata3.camera,
            )
        ).inOrder()

        assertThat(cameraDevices.getCameraMetadata(metadata3.camera)).isNull()
        assertThat(
            cameraDevices.getCameraMetadata(
                metadata3.camera,
                EXTERNAL_BACKEND_ID
            )
        ).isSameInstanceAs(metadata3)
    }
}
