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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FakeCameraDevicesTest {
    @Test
    fun cameraMetadataIsNotEqual() {
        val metadata1 = FakeCameraMetadata(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT)
        )
        val metadata2 = FakeCameraMetadata(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
        )

        val cameraDevices = FakeCameraDevices(listOf(metadata1, metadata2))
        val devices = runBlocking { cameraDevices.ids() }
        assertThat(devices).containsExactlyElementsIn(
            listOf(
                metadata1.camera,
                metadata2.camera
            )
        ).inOrder()

        assertThat(cameraDevices.awaitMetadata(metadata1.camera)).isSameInstanceAs(metadata1)
        assertThat(cameraDevices.awaitMetadata(metadata2.camera)).isSameInstanceAs(metadata2)
    }
}