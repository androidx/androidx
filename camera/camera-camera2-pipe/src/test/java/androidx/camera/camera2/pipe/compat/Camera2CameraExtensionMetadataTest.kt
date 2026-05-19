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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
internal class Camera2CameraExtensionMetadataTest {

    private val cameraId = CameraId("0")
    private val extension = 1 // e.g. EXTENSION_BOKEH

    @Test
    fun testKeysAndGet_preApi35() {
        val characteristics: CameraExtensionCharacteristics = mock()
        val metadata =
            Camera2CameraExtensionMetadata(
                camera = cameraId,
                isRedacted = false,
                cameraExtension = extension,
                extensionCharacteristics = characteristics,
                metadata = emptyMap(),
            )

        assertThat(metadata.keys).isEmpty()
        assertThat(metadata[CameraCharacteristics.LENS_FACING]).isNull()
        assertThat(metadata.getOrDefault(CameraCharacteristics.LENS_FACING, 1)).isEqualTo(1)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testKeysAndGet_api35() {
        val characteristics: CameraExtensionCharacteristics = mock()

        val mockKey = CameraCharacteristics.LENS_FACING
        val mockKeys = setOf<CameraCharacteristics.Key<*>>(mockKey)

        whenever(characteristics.getKeys(extension)).thenReturn(mockKeys)
        whenever(characteristics.get(extension, mockKey))
            .thenReturn(CameraCharacteristics.LENS_FACING_BACK)

        val metadata =
            Camera2CameraExtensionMetadata(
                camera = cameraId,
                isRedacted = false,
                cameraExtension = extension,
                extensionCharacteristics = characteristics,
                metadata = emptyMap(),
            )

        assertThat(metadata.keys).containsExactly(mockKey)
        assertThat(metadata[mockKey]).isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
        assertThat(metadata.getOrDefault(mockKey, CameraCharacteristics.LENS_FACING_FRONT))
            .isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun testKeysAndGet_api35_returnsNull() {
        val characteristics: CameraExtensionCharacteristics = mock()

        val mockKey = CameraCharacteristics.LENS_FACING

        whenever(characteristics.get(extension, mockKey)).thenReturn(null)

        val metadata =
            Camera2CameraExtensionMetadata(
                camera = cameraId,
                isRedacted = false,
                cameraExtension = extension,
                extensionCharacteristics = characteristics,
                metadata = emptyMap(),
            )

        assertThat(metadata[mockKey]).isNull()
        assertThat(metadata.getOrDefault(mockKey, CameraCharacteristics.LENS_FACING_FRONT))
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
    }
}
