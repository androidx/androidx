/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_HDR
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_NIGHT
import androidx.camera.camera2.pipe.testing.FakeCameraExtensionMetadata
import androidx.camera.camera2.pipe.testing.FakeCameraIds
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class CameraMetadataTest {

    @Test
    fun isExtensionSupported_defaultImplementation_returnsTrueForSupportedExtension() {
        val cameraMetadata =
            FakeCameraMetadata(
                extensionMetadata =
                    mapOf(
                        EXTENSION_NIGHT to
                            FakeCameraExtensionMetadata(FakeCameraIds.default, EXTENSION_NIGHT)
                    )
            )

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_NIGHT)).isTrue()
    }

    @Test
    fun isExtensionSupported_defaultImplementation_returnsFalseForUnsupportedExtension() {
        val cameraMetadata =
            FakeCameraMetadata(
                extensionMetadata =
                    mapOf(
                        EXTENSION_NIGHT to
                            FakeCameraExtensionMetadata(FakeCameraIds.default, EXTENSION_NIGHT)
                    )
            )

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_HDR)).isFalse()
    }

    @Test
    fun isExtensionSupported_defaultImplementation_returnsFalseWhenNoExtensionsSupported() {
        val cameraMetadata = FakeCameraMetadata()

        assertThat(cameraMetadata.isExtensionSupported(EXTENSION_NIGHT)).isFalse()
    }
}
