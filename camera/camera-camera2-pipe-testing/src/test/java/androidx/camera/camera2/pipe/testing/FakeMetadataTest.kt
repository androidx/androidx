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

package androidx.camera.camera2.pipe.testing

import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.MetadataTransform
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.common.CameraCharacteristicsWrapper
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.common.Metadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
class MetadataTest {
    @Test
    fun testMetadataCanRetrieveValues() {
        val metadata = FakeMetadata(mapOf(FakeMetadata.TEST_KEY to 42))

        assertThat(metadata[FakeMetadata.TEST_KEY]).isNotNull()
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata.getOrDefault(FakeMetadata.TEST_KEY, 84)).isEqualTo(42)
        assertThat(metadata.getOrDefault(FakeMetadata.TEST_KEY_ABSENT, 84)).isEqualTo(84)
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class CameraMetadataTest {
    @Test
    fun cameraMetadataIsNotEqual() {
        val metadata1 =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
                metadataOverrides = mapOf(FakeMetadata.TEST_KEY to 42),
            )
        val metadata2 =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
                metadataOverrides = mapOf(FakeMetadata.TEST_KEY to 42),
            )

        assertThat(metadata1).isNotEqualTo(metadata2)
    }

    @Test
    fun canRetrieveCameraCharacteristicsOrCameraMetadataViaInterface() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
                metadataOverrides = mapOf(FakeMetadata.TEST_KEY to 42),
            )

        assertThat(metadata[FakeMetadata.TEST_KEY]).isNotNull()
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata[CameraCharacteristics.LENS_FACING]).isNotNull()
        assertThat(metadata[CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE]).isNull()
    }

    @Test
    fun verifyHighEndDeviceTemplate_simulatesAdvancedHardwareWithFlash() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("0"),
            )

        assertThat(metadata[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
        assertThat(metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]).isTrue()
        assertThat(metadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL])
            .isEqualTo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    }

    @Test
    fun verifyHighEndDeviceTemplate_providesDefaultStreamConfigurationMap() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("0"),
            )

        val streamMap = metadata[CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP]
        assertThat(streamMap).isNotNull()
        assertThat(streamMap!!.getOutputFormats())
            .containsAtLeast(
                StreamFormat.YUV_420_888,
                StreamFormat.JPEG,
                StreamFormat.PRIVATE,
                StreamFormat.RAW_SENSOR,
                StreamFormat.RAW10,
                StreamFormat.RAW12,
                StreamFormat.YCBCR_P010,
                StreamFormat.JPEG_R,
            )
        assertThat(streamMap.isOutputSupportedFor(StreamFormat.YUV_420_888)).isTrue()
        assertThat(streamMap.isOutputSupportedFor(StreamFormat.RAW_SENSOR)).isTrue()
    }

    @Test
    fun verifyHighEndDeviceTemplate_providesFrontStreamConfigurationMap() {
        val frontMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("1"),
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            )

        val frontStreamMap = frontMetadata[CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP]
        assertThat(frontStreamMap).isNotNull()
        assertThat(frontStreamMap!!.getOutputFormats())
            .containsExactly(
                StreamFormat(PixelFormat.RGB_888),
                StreamFormat.RAW_SENSOR,
                StreamFormat.JPEG,
                StreamFormat.PRIVATE,
                StreamFormat.YUV_420_888,
                StreamFormat.RAW10,
                StreamFormat.YCBCR_P010,
                StreamFormat.JPEG_R,
            )
    }

    @Test
    fun verifyHighEndDeviceTemplate_providesAdvancedControlRanges() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("0"),
            )

        assertThat(metadata[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]).isNotNull()
        assertThat(metadata[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]).isNotNull()
        if (Build.VERSION.SDK_INT >= 30) {
            assertThat(metadata[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]).isNotNull()
        }
        assertThat(metadata[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM])
            .isGreaterThan(1.0f)
    }

    @Test
    fun verifyHighEndDeviceTemplate_providesPhysicalCameraMetadata() {
        val backMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("0"),
                lensFacing = CameraCharacteristics.LENS_FACING_BACK,
            )
        val frontMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("1"),
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            )

        assertThat(backMetadata.physicalCameraIds).isNotEmpty()
        assertThat(frontMetadata.physicalCameraIds).isNotEmpty()
        assertThat(backMetadata.physicalCameraIds).containsNoneIn(frontMetadata.physicalCameraIds)
        assertThat(backMetadata.physicalRequestKeys).isNotEmpty()
        assertThat(frontMetadata.physicalRequestKeys).isNotEmpty()

        assertPhysicalCamerasHaveMetadata(backMetadata, CameraCharacteristics.LENS_FACING_BACK)
        assertPhysicalCamerasHaveMetadata(frontMetadata, CameraCharacteristics.LENS_FACING_FRONT)
    }

    @Test
    fun verifyHighEndDeviceTemplate_resolvesLensFacingAndNullCharacteristicsOverrides() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("1"),
                characteristicsOverrides =
                    mapOf(
                        CameraCharacteristics.LENS_FACING to
                            CameraCharacteristics.LENS_FACING_FRONT,
                        CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE to null,
                    ),
            )

        assertThat(metadata[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(metadata[CameraCharacteristics.SENSOR_ORIENTATION]).isEqualTo(270)
        assertThat(metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]).isFalse()
        assertThat(metadata.physicalCameraIds).isNotEmpty()
        assertThat(metadata[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]).isNull()
        assertThat(metadata.keys)
            .doesNotContain(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
    }

    @Test
    fun verifyHighEndDeviceTemplate_omitsDefaultPhysicalMetadataWithoutLogicalMultiCameraCapability() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                cameraId = CameraId("0"),
                characteristicsOverrides =
                    mapOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to null),
            )

        assertThat(metadata.physicalCameraIds).isEmpty()
        assertThat(metadata.physicalRequestKeys).isEmpty()
    }

    @Test
    fun verifyEmulatorDeviceTemplate_simulatesLimitedHardwareNoFlash() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = EmulatorDeviceTemplate,
                cameraId = CameraId("0"),
            )

        assertThat(metadata[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
        assertThat(metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]).isFalse()
        assertThat(metadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL])
            .isEqualTo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
    }

    @Test
    fun verifyFromTemplateCorrectlyAppliesOverrides() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            )

        assertThat(metadata[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]).isFalse()
    }

    @Test
    fun verifyFromTemplateFrontFacing_defaultsSensorOrientationTo270() {
        val metadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            )

        assertThat(metadata[CameraCharacteristics.LENS_FACING])
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
        assertThat(metadata[CameraCharacteristics.SENSOR_ORIENTATION]).isEqualTo(270)
    }

    @Test
    fun cameraCharacteristicsWrapperProperties_delegateCorrectly() {
        val defaultWrapper: CameraCharacteristicsWrapper = FakeCameraMetadata()
        assertThat(defaultWrapper.cameraId).isEqualTo(FakeCameraIds.default)
        assertThat(defaultWrapper.keys).isEmpty()
        assertThat(defaultWrapper.captureRequestKeys).isEmpty()
        assertThat(defaultWrapper.captureResultKeys).isEmpty()
        assertThat(defaultWrapper.sessionKeys).isEmpty()
        assertThat(defaultWrapper.sessionCaptureRequestKeys).isEmpty()
        assertThat(defaultWrapper.physicalCameraIds).isEmpty()
        assertThat(defaultWrapper.physicalCaptureRequestKeys).isEmpty()
        assertThat(defaultWrapper.isRestricted).isFalse()
        assertThat(defaultWrapper.restrictedKeys).isEmpty()
        assertThat(defaultWrapper.dynamicKeys).isEmpty()
        assertThat(defaultWrapper.unwrapAs(CameraMetadata::class.java))
            .isSameInstanceAs(defaultWrapper)
        assertThat(defaultWrapper.unwrapAs(CameraCharacteristics::class.java)).isNull()

        val physicalMetadata = FakeCameraMetadata(cameraId = CameraId("2"))
        val metadata =
            FakeCameraMetadata(
                cameraId = CameraId("0"),
                characteristics = mapOf(CameraCharacteristics.SENSOR_ORIENTATION to 90),
                requestKeys = setOf(CaptureRequest.CONTROL_AE_MODE),
                resultKeys = setOf(CaptureResult.CONTROL_AE_STATE),
                sessionCharacteristicsKeys = setOf(CameraCharacteristics.SENSOR_ORIENTATION),
                sessionCaptureRequestKeys = setOf(CaptureRequest.CONTROL_AF_MODE),
                physicalMetadata = mapOf(CameraId("2") to physicalMetadata),
                physicalRequestKeys = setOf(CaptureRequest.FLASH_MODE),
                isRedacted = true,
                restrictedKeys = setOf(CameraCharacteristics.LENS_FACING),
                dynamicKeys = setOf(CameraCharacteristics.SENSOR_ORIENTATION),
            )

        assertThat(metadata.cameraId).isEqualTo(CameraId("0"))
        assertThat(metadata.captureRequestKeys).containsExactly(CaptureRequest.CONTROL_AE_MODE)
        assertThat(metadata.captureResultKeys).containsExactly(CaptureResult.CONTROL_AE_STATE)
        assertThat(metadata.sessionKeys).containsExactly(CameraCharacteristics.SENSOR_ORIENTATION)
        assertThat(metadata.sessionCharacteristicsKeys)
            .containsExactly(CameraCharacteristics.SENSOR_ORIENTATION)
        assertThat(metadata.sessionCaptureRequestKeys)
            .containsExactly(CaptureRequest.CONTROL_AF_MODE)
        assertThat(metadata.physicalCameraIds).containsExactly(CameraId("2"))
        assertThat(metadata.physicalCaptureRequestKeys).containsExactly(CaptureRequest.FLASH_MODE)
        assertThat(metadata.isRestricted).isTrue()
        assertThat(metadata.restrictedKeys).containsExactly(CameraCharacteristics.LENS_FACING)
        assertThat(metadata.dynamicKeys).containsExactly(CameraCharacteristics.SENSOR_ORIENTATION)
        assertThat(metadata.unwrapAs(CameraMetadata::class.java)).isSameInstanceAs(metadata)
        assertThat(metadata.unwrapAs(CameraCharacteristicsWrapper::class.java))
            .isSameInstanceAs(metadata)
        assertThat(metadata.unwrapAs(CameraCharacteristics::class.java)).isNull()
    }

    private fun assertPhysicalCamerasHaveMetadata(
        metadata: CameraMetadata,
        expectedLensFacing: Int,
    ) {
        for (physicalId in metadata.physicalCameraIds) {
            val physicalCamera = metadata.awaitPhysicalMetadata(physicalId)
            assertThat(physicalCamera[CameraCharacteristics.LENS_FACING])
                .isEqualTo(expectedLensFacing)
            assertThat(physicalCamera[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]).isNotNull()
            assertThat(physicalCamera[CameraCharacteristics.LENS_INTRINSIC_CALIBRATION]).isNotNull()
            if (Build.VERSION.SDK_INT >= 30) {
                assertThat(physicalCamera[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE])
                    .isNotNull()
            }
            val streamMap = physicalCamera[CameraMetadata.CAMERA_STREAM_CONFIGURATION_MAP]
            assertThat(streamMap).isNotNull()
            val outputFormats = streamMap!!.getOutputFormats()
            assertThat(outputFormats).isNotEmpty()
            for (format in outputFormats) {
                val sizes = streamMap.getOutputSizes(format)
                assertThat(sizes).isNotEmpty()
                for (size in sizes) {
                    assertThat(streamMap.getOutputMinFrameDuration(format, size)).isAtLeast(0L)
                    assertThat(streamMap.getOutputStallDuration(format, size)).isAtLeast(0L)
                }
            }
        }
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class RequestMetadataTest {

    @Test
    fun canRetrieveCaptureRequestOrCameraMetadataViaInterface() {
        val requestMetadata =
            FakeRequestMetadata(
                requestParameters = mapOf(CaptureRequest.JPEG_QUALITY to 95),
                request =
                    Request(
                        streams = listOf(),
                        parameters = mapOf(CaptureRequest.JPEG_QUALITY to 20),
                        extras = mapOf(FakeMetadata.TEST_KEY to 42),
                    ),
            )

        assertThat(requestMetadata[CaptureRequest.JPEG_QUALITY]).isEqualTo(95)
        assertThat(requestMetadata[CaptureRequest.COLOR_CORRECTION_MODE]).isNull()
        assertThat(requestMetadata[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(requestMetadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(requestMetadata.request[CaptureRequest.JPEG_QUALITY]).isEqualTo(20)
        assertThat(requestMetadata.request[CaptureRequest.COLOR_CORRECTION_MODE]).isNull()
        assertThat(requestMetadata.request[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(requestMetadata.request[FakeMetadata.TEST_KEY_ABSENT]).isNull()
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameMetadataTest {
    @Test
    fun canRetrieveCaptureResultOrCameraMetadataViaInterface() {
        val metadata =
            FakeFrameMetadata(
                resultMetadata = mapOf(CaptureResult.JPEG_QUALITY to 95),
                extraResultMetadata = mapOf(FakeMetadata.TEST_KEY to 42),
            )

        assertThat(metadata[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata[CaptureResult.JPEG_QUALITY]).isEqualTo(95)
        assertThat(metadata[CaptureResult.COLOR_CORRECTION_MODE]).isNull()

        assertThat(metadata.getOrDefault(CaptureResult.JPEG_QUALITY, 10)).isEqualTo(95)
        assertThat(metadata.getOrDefault(CaptureResult.COLOR_CORRECTION_MODE, -1)).isEqualTo(-1)

        assertThat(metadata.keys).containsExactly(CaptureResult.JPEG_QUALITY)
        assertThat(metadata.metadataKeys).containsExactly(FakeMetadata.TEST_KEY)
    }

    @Test
    fun fakeFrameMetadata_includesExtraMetadataInKeysAndMetadataKeys() {
        val extraMetaKey = Metadata.Key<String>("extra.meta.key")
        val metadata =
            FakeFrameMetadata(
                resultMetadata = mapOf(CaptureResult.JPEG_QUALITY to 95),
                extraResultMetadata = mapOf(FakeMetadata.TEST_KEY to 42),
                extraMetadata =
                    mapOf(
                        CaptureResult.LENS_STATE to CaptureResult.LENS_STATE_STATIONARY,
                        extraMetaKey to "extra_value",
                    ),
            )

        assertThat(metadata[CaptureResult.JPEG_QUALITY]).isEqualTo(95)
        assertThat(metadata[CaptureResult.LENS_STATE])
            .isEqualTo(CaptureResult.LENS_STATE_STATIONARY)
        assertThat(metadata[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(metadata[extraMetaKey]).isEqualTo("extra_value")

        assertThat(metadata.keys)
            .containsExactly(CaptureResult.JPEG_QUALITY, CaptureResult.LENS_STATE)
        assertThat(metadata.metadataKeys).containsExactly(FakeMetadata.TEST_KEY, extraMetaKey)
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class MetadataTransformTest {
    private val metadata =
        FakeCameraMetadata.fromTemplate(
            template = HighEndDeviceTemplate,
            lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
            metadataOverrides = mapOf(FakeMetadata.TEST_KEY to 42),
        )

    private val requestMetadata =
        FakeRequestMetadata(
            requestParameters = mapOf(CaptureRequest.JPEG_QUALITY to 95),
            request =
                Request(
                    streams = listOf(),
                    parameters = mapOf(CaptureRequest.JPEG_QUALITY to 20),
                    extras = mapOf(FakeMetadata.TEST_KEY to 42),
                ),
        )

    private val fakeFrameInfo = FakeFrameInfo(requestMetadata = requestMetadata)

    @Test
    fun defaultMetadataTransformIsNoOp() {
        val transform = MetadataTransform()
        val overrides =
            transform.transformFn.computeOverridesFor(fakeFrameInfo, CameraId("Fake"), listOf())

        assertThat(overrides).isEmpty()
    }

    @Test
    fun canCreateAndInvokeMetadataTransform() {
        val transform =
            MetadataTransform(
                transformFn =
                    object : MetadataTransform.TransformFn {
                        override fun computeOverridesFor(
                            result: FrameInfo,
                            camera: CameraId,
                            related: List<FrameInfo?>,
                        ): Map<*, Any?> {
                            return mapOf(FakeMetadata.TEST_KEY to result.frameNumber.value)
                        }
                    }
            )
        val overrides =
            transform.transformFn.computeOverridesFor(
                FakeFrameInfo(metadata = FakeFrameMetadata(frameNumber = CameraFrameNumber(128))),
                CameraId("Fake"),
                listOf(),
            )

        assertThat(overrides).hasSize(1)
        assertThat(overrides[FakeMetadata.TEST_KEY]).isEqualTo(128)
    }

    @Test
    fun canUseCameraMetadataForTransforms() {
        val transform =
            MetadataTransform(
                transformFn =
                    object : MetadataTransform.TransformFn {
                        override fun computeOverridesFor(
                            result: FrameInfo,
                            camera: CameraId,
                            related: List<FrameInfo?>,
                        ): Map<*, Any?> {
                            return mapOf(FakeMetadata.TEST_KEY to metadata[FakeMetadata.TEST_KEY])
                        }
                    }
            )
        val overrides =
            transform.transformFn.computeOverridesFor(fakeFrameInfo, CameraId("Fake"), listOf())

        assertThat(overrides).hasSize(1)
        assertThat(overrides[FakeMetadata.TEST_KEY]).isEqualTo(42)
    }
}
