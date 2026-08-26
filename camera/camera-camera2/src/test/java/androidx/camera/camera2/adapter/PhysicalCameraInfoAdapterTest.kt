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

package androidx.camera.camera2.adapter

import androidx.camera.camera2.internal.IntrinsicZoomCalculator
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class PhysicalCameraInfoAdapterTest {

    private val defaultParentCameraInfo =
        FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK)

    private val defaultIntrinsicZoomCalculator =
        IntrinsicZoomCalculator.NO_OP_INTRINSIC_ZOOM_CALCULATOR

    private val defaultCameraProperties =
        FakeCameraProperties(FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate))

    @Test
    fun intrinsicZoomRatioIsEqualToCalculatorResult_whenCalculatorReturnsValidRatio() {
        // Arrange
        val expectedRatio = 2.5f
        val fakeCalculator =
            object : IntrinsicZoomCalculator {
                override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float =
                    expectedRatio
            }
        val adapter =
            PhysicalCameraInfoAdapter(
                defaultCameraProperties,
                fakeCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Act
        val ratio = adapter.intrinsicZoomRatio

        // Assert
        assertThat(ratio).isEqualTo(expectedRatio)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenCalculatorReturnsNull() {
        // Arrange
        val fakeCalculator =
            object : IntrinsicZoomCalculator {
                override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float? =
                    null
            }
        val adapter =
            PhysicalCameraInfoAdapter(
                defaultCameraProperties,
                fakeCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Act
        val ratio = adapter.intrinsicZoomRatio

        // Assert
        assertThat(ratio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun getCameraSelector_returnsSelectorWithPhysicalCameraId() {
        // Arrange
        val physicalCameraId = "2"
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate),
                cameraId = androidx.camera.camera2.pipe.CameraId(physicalCameraId),
            )
        val adapter =
            PhysicalCameraInfoAdapter(
                cameraProperties,
                defaultIntrinsicZoomCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Act
        val selector = adapter.cameraSelector

        // Assert
        assertThat(selector.physicalCameraId).isEqualTo(physicalCameraId)
        assertThat(selector.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)
    }

    @Test
    fun sessionStates_delegateToParentCameraInfo() {
        // Arrange
        val parentCameraInfo =
            FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_BACK).apply { setTorch(1) }
        val adapter =
            PhysicalCameraInfoAdapter(
                defaultCameraProperties,
                defaultIntrinsicZoomCalculator,
                parentCameraInfo = parentCameraInfo,
            )

        // Act & Assert
        assertThat(adapter.hasFlashUnit()).isEqualTo(parentCameraInfo.hasFlashUnit())
        assertThat(adapter.torchState.value).isEqualTo(parentCameraInfo.torchState.value)
        assertThat(adapter.zoomState.value?.zoomRatio)
            .isEqualTo(parentCameraInfo.zoomState.value?.zoomRatio)
        assertThat(adapter.cameraState.value).isEqualTo(parentCameraInfo.cameraState.value)
        assertThat(adapter.exposureState.exposureCompensationIndex)
            .isEqualTo(parentCameraInfo.exposureState.exposureCompensationIndex)
        assertThat(adapter.implementationType).isEqualTo(parentCameraInfo.implementationType)
    }

    @Test
    fun isLogicalMultiCameraSupported_returnsFalse() {
        // Arrange
        val adapter =
            PhysicalCameraInfoAdapter(
                defaultCameraProperties,
                defaultIntrinsicZoomCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Assert
        assertThat(adapter.isLogicalMultiCameraSupported).isFalse()
    }

    @Test
    fun getPhysicalCameraInfos_returnsEmptySet() {
        // Arrange
        val adapter =
            PhysicalCameraInfoAdapter(
                defaultCameraProperties,
                defaultIntrinsicZoomCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Assert
        assertThat(adapter.physicalCameraInfos).isEmpty()
    }

    @Test
    fun getCameraIdentifier_returnsIdentifierWithPhysicalCameraId() {
        // Arrange
        val physicalCameraId = "2"
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate),
                cameraId = androidx.camera.camera2.pipe.CameraId(physicalCameraId),
            )
        val adapter =
            PhysicalCameraInfoAdapter(
                cameraProperties,
                defaultIntrinsicZoomCalculator,
                parentCameraInfo = defaultParentCameraInfo,
            )

        // Act
        val identifier = adapter.cameraIdentifier

        // Assert
        assertThat(identifier).isNotNull()
        assertThat(identifier!!.cameraIds)
            .containsExactly(
                androidx.camera.core.CameraIdentifier.CompositeCameraId("0", physicalCameraId)
            )
    }
}
