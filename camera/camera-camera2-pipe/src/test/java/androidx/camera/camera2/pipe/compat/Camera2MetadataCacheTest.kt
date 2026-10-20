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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
internal class Camera2MetadataCacheTest {
    @Test
    fun metadataIsCachedAndShimmed() = runTest {
        val camera0 = RobolectricCameras.create {
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            )
            set(CameraCharacteristics.SENSOR_ORIENTATION, 90)
            set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK)
            set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
        }

        val camera1 = RobolectricCameras.create {
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            )
            set(CameraCharacteristics.SENSOR_ORIENTATION, 0)
            set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_FRONT)
            set(CameraCharacteristics.FLASH_INFO_AVAILABLE, false)
        }
        val cache =
            Camera2MetadataCache(
                RobolectricCameras.application,
                FakeThreads.fromTestScope(this),
                Permissions(RobolectricCameras.application),
                SystemTimeSource(),
            )

        val metadata0 = cache.awaitCameraMetadata(camera0)
        val metadata1 = cache.awaitCameraMetadata(camera1)

        // Check to make sure that metadata is not null, and that various properties do not crash
        // on older OS versions when accessed.
        assertThat(metadata0).isNotNull()
        assertThat(metadata0.camera).isEqualTo(camera0)
        assertThat(metadata0.cameraId).isEqualTo(camera0)
        assertThat(metadata0.isRedacted).isFalse()
        assertThat(metadata0.isRestricted).isFalse()
        assertThat(metadata0.keys).isNotNull()
        assertThat(metadata0.requestKeys).isNotNull()
        assertThat(metadata0.captureRequestKeys).isEqualTo(metadata0.requestKeys)
        assertThat(metadata0.resultKeys).isNotNull()
        assertThat(metadata0.captureResultKeys).isEqualTo(metadata0.resultKeys)
        assertThat(metadata0.sessionCharacteristicsKeys).isNotNull()
        assertThat(metadata0.sessionKeys).isEqualTo(metadata0.sessionCharacteristicsKeys)
        assertThat(metadata0.sessionCaptureRequestKeys).isNotNull()
        assertThat(metadata0.restrictedKeys).isNotNull()
        assertThat(metadata0.dynamicKeys).isNotNull()
        assertThat(metadata0.physicalCameraIds).isNotNull()
        assertThat(metadata0.physicalRequestKeys).isNotNull()
        assertThat(metadata0.physicalCaptureRequestKeys).isEqualTo(metadata0.physicalRequestKeys)
        assertThat(metadata0[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(2)

        assertThat(metadata1).isNotNull()
        assertThat(metadata1.camera).isEqualTo(camera1)
        assertThat(metadata1.cameraId).isEqualTo(camera1)
        assertThat(metadata1.isRedacted).isFalse()
        assertThat(metadata1.isRestricted).isFalse()
        assertThat(metadata1.keys).isNotNull()
        assertThat(metadata1.requestKeys).isNotNull()
        assertThat(metadata1.captureRequestKeys).isEqualTo(metadata1.requestKeys)
        assertThat(metadata1.resultKeys).isNotNull()
        assertThat(metadata1.captureResultKeys).isEqualTo(metadata1.resultKeys)
        assertThat(metadata1.sessionCharacteristicsKeys).isNotNull()
        assertThat(metadata1.sessionKeys).isEqualTo(metadata1.sessionCharacteristicsKeys)
        assertThat(metadata1.sessionCaptureRequestKeys).isNotNull()
        assertThat(metadata1.restrictedKeys).isNotNull()
        assertThat(metadata1.dynamicKeys).isNotNull()
        assertThat(metadata1.physicalCameraIds).isNotNull()
        assertThat(metadata1.physicalRequestKeys).isNotNull()
        assertThat(metadata1.physicalCaptureRequestKeys).isEqualTo(metadata1.physicalRequestKeys)
        assertThat(metadata1[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]).isEqualTo(3)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun camera2CameraMetadata_delegatesWrapperPropertiesAndUnwrapAs() {
        val mockCharacteristics = mock<CameraCharacteristics>()
        val mockProvider = mock<Camera2MetadataProvider>()
        whenever(mockCharacteristics.keysNeedingPermission)
            .thenReturn(listOf(CameraCharacteristics.LENS_FACING))

        val metadata =
            Camera2CameraMetadata(
                camera = CameraId("0"),
                isRedacted = true,
                characteristics = mockCharacteristics,
                metadataProvider = mockProvider,
                metadata = emptyMap(),
                cacheBlocklist = setOf(CameraCharacteristics.SENSOR_ORIENTATION),
            )

        assertThat(metadata.isRestricted).isTrue()
        assertThat(metadata.restrictedKeys).containsExactly(CameraCharacteristics.LENS_FACING)
        assertThat(metadata.dynamicKeys).containsExactly(CameraCharacteristics.SENSOR_ORIENTATION)
        assertThat(metadata.unwrapAs(CameraMetadata::class.java)).isSameInstanceAs(metadata)
        assertThat(metadata.unwrapAs(CameraCharacteristicsWrapper::class.java))
            .isSameInstanceAs(metadata)
        assertThat(metadata.unwrapAs(CameraCharacteristics::class.java))
            .isSameInstanceAs(mockCharacteristics)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun camera2CameraMetadata_restrictedKeysReturnsEmptySetOnFrameworkFailure() {
        val assertionFailingCharacteristics = mock<CameraCharacteristics>()
        whenever(assertionFailingCharacteristics.keysNeedingPermission)
            .thenThrow(AssertionError("Framework assertion failure"))
        val assertionMetadata =
            Camera2CameraMetadata(
                camera = CameraId("0"),
                isRedacted = false,
                characteristics = assertionFailingCharacteristics,
                metadataProvider = mock(),
                metadata = emptyMap(),
                cacheBlocklist = emptySet(),
            )
        assertThat(assertionMetadata.restrictedKeys).isEmpty()

        val runtimeFailingCharacteristics = mock<CameraCharacteristics>()
        whenever(runtimeFailingCharacteristics.keysNeedingPermission)
            .thenThrow(IllegalArgumentException("Unknown vendor tag"))
        val runtimeMetadata =
            Camera2CameraMetadata(
                camera = CameraId("1"),
                isRedacted = false,
                characteristics = runtimeFailingCharacteristics,
                metadataProvider = mock(),
                metadata = emptyMap(),
                cacheBlocklist = emptySet(),
            )
        assertThat(runtimeMetadata.restrictedKeys).isEmpty()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun cameraExtensionMetadataIsCachedSeparately() = runTest {
        val mockContext = mock<Context>()
        val mockCameraManager = mock<CameraManager>()
        val mockExtensionCharacteristics = mock<CameraExtensionCharacteristics>()

        whenever(mockContext.getSystemService(Context.CAMERA_SERVICE)).thenReturn(mockCameraManager)
        whenever(mockCameraManager.getCameraExtensionCharacteristics(any()))
            .thenReturn(mockExtensionCharacteristics)

        val cache =
            Camera2MetadataCache(
                mockContext,
                FakeThreads.fromTestScope(this),
                Permissions(mockContext),
                SystemTimeSource(),
            )

        val camera0 = CameraId("0")
        val metadataBokeh =
            cache.awaitCameraExtensionMetadata(
                camera0,
                CameraExtensionCharacteristics.EXTENSION_BOKEH,
            )
        val metadataHdr =
            cache.awaitCameraExtensionMetadata(
                camera0,
                CameraExtensionCharacteristics.EXTENSION_HDR,
            )

        assertThat(metadataBokeh).isNotNull()
        assertThat(metadataHdr).isNotNull()
        assertThat(metadataBokeh.cameraExtension)
            .isEqualTo(CameraExtensionCharacteristics.EXTENSION_BOKEH)
        assertThat(metadataHdr.cameraExtension)
            .isEqualTo(CameraExtensionCharacteristics.EXTENSION_HDR)
        assertThat(metadataBokeh).isNotEqualTo(metadataHdr)

        // Verify Caching: same instance returned for same extension
        val metadataBokeh2 =
            cache.awaitCameraExtensionMetadata(
                camera0,
                CameraExtensionCharacteristics.EXTENSION_BOKEH,
            )
        assertThat(metadataBokeh).isSameInstanceAs(metadataBokeh2)

        // Verify Characteristics Caching: mock was only called once for camera0.value
        verify(mockCameraManager, times(1)).getCameraExtensionCharacteristics(camera0.value)
    }
}
