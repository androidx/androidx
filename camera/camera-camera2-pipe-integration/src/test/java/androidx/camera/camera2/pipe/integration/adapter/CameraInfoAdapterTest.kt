/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF
import android.hardware.camera2.CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON
import android.hardware.camera2.CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
import android.os.Build
import android.util.Range
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter.Companion.unwrapAs
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_10B_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.testing.FakeZoomCompat
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraInfo
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.RestrictedCameraInfo
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.testutils.MainDispatcherRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraInfoAdapterTest {
    private val zoomControl = ZoomControl(FakeZoomCompat())
    private val cameraInfoAdapter = createCameraInfoAdapter(zoomControl = zoomControl)

    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    private val defaultCameraId = "0"
    private val defaultCameraCharacteristics =
        mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS to floatArrayOf(1.0f),
            CameraCharacteristics.SENSOR_ORIENTATION to 0,
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to Size(10, 10),
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to Rect(0, 0, 10, 10),
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to SizeF(10f, 10f),
        )
    private val defaultCameraProperties =
        FakeCameraProperties(FakeCameraMetadata(characteristics = defaultCameraCharacteristics))

    private val telephotoCameraId = "2"

    // Only SENSOR_INFO_PHYSICAL_SIZE has been made less wider and everything else kept the same
    private val telephotoCameraProperties =
        FakeCameraProperties(
            FakeCameraMetadata(
                characteristics =
                    defaultCameraCharacteristics.toMutableMap().apply {
                        put(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, SizeF(1f, 10f))
                    }
            )
        )

    private val ultraWideCameraId = "2"

    // Only SENSOR_INFO_PHYSICAL_SIZE has been made wider and everything else kept the same
    private val ultraWideCameraProperties =
        FakeCameraProperties(
            FakeCameraMetadata(
                characteristics =
                    defaultCameraCharacteristics.toMutableMap().apply {
                        put(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, SizeF(100f, 10f))
                    }
            )
        )

    @Test
    fun getSupportedResolutions() {
        // Act.
        val resolutions: List<Size> =
            cameraInfoAdapter.getSupportedResolutions(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            )

        // Assert.
        assertThat(resolutions).containsExactly(Size(1920, 1080), Size(1280, 720), Size(640, 480))
    }

    @Test
    fun getSupportedFpsRanges() {
        // Act.
        val ranges: Set<Range<Int>> = cameraInfoAdapter.supportedFrameRateRanges

        // Assert.
        assertThat(ranges)
            .containsExactly(Range(12, 30), Range(24, 24), Range(30, 30), Range(60, 60))
    }

    @Test
    fun canReturnIsFocusMeteringSupported() {
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val action = FocusMeteringAction.Builder(factory.createPoint(0.5f, 0.5f)).build()

        assertWithMessage("isFocusMeteringSupported() method did not return successfully")
            .that(cameraInfoAdapter.isFocusMeteringSupported(action))
            .isAnyOf(true, false)
    }

    @Test
    fun canReturnDefaultZoomState() {
        // make new ZoomControl to test first-time initialization scenario
        val zoomControl = ZoomControl(FakeZoomCompat())
        val cameraInfoAdapter = createCameraInfoAdapter(zoomControl = zoomControl)

        assertWithMessage("zoomState did not return default zoom ratio successfully")
            .that(cameraInfoAdapter.zoomState.value)
            .isEqualTo(zoomControl.defaultZoomState)
    }

    @Test
    fun canObserveZoomStateUpdate(): Unit = runBlocking {
        var currentZoomState: ZoomState = ZoomValue(-1.0f, -1.0f, -1.0f)
        cameraInfoAdapter.zoomState.observeForever { currentZoomState = it }

        // if useCaseCamera is null, zoom setting operation will be cancelled
        zoomControl.requestControl = FakeUseCaseCameraRequestControl()

        val expectedZoomState = ZoomValue(3.0f, 1.0f, 10.0f)
        zoomControl.applyZoomState(expectedZoomState)[3, TimeUnit.SECONDS]

        assertWithMessage("zoomState did not return the correct zoom state successfully")
            .that(currentZoomState)
            .isEqualTo(expectedZoomState)
    }

    @Test
    fun canObserveZoomStateReset(): Unit = runBlocking {
        var currentZoomState: ZoomState = ZoomValue(-1.0f, -1.0f, -1.0f)
        cameraInfoAdapter.zoomState.observeForever { currentZoomState = it }

        // if useCaseCamera is null, zoom setting operation will be cancelled
        zoomControl.requestControl = FakeUseCaseCameraRequestControl()

        zoomControl.reset()

        // minZoom and maxZoom will be set as 0 due to FakeZoomCompat using those values
        assertWithMessage("zoomState did not return default zoom state successfully")
            .that(currentZoomState)
            .isEqualTo(zoomControl.defaultZoomState)
    }

    @Test
    fun cameraInfo_getImplementationType_legacy() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                                )
                        )
                    )
            )
        assertThat(cameraInfo.implementationType)
            .isEqualTo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)
    }

    @Test
    fun cameraInfo_getImplementationType_noneLegacy() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                                )
                        )
                    )
            )
        assertThat(cameraInfo.implementationType).isEqualTo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
    }

    @Test
    fun cameraInfo_isPreviewStabilizationSupported() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                                        intArrayOf(
                                            CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                                            CONTROL_VIDEO_STABILIZATION_MODE_ON,
                                            CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                                        )
                                )
                        )
                    )
            )

        assertThat(cameraInfo.isPreviewStabilizationSupported).isTrue()
    }

    @Test
    fun cameraInfo_isPreviewStabilizationNotSupported() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                                        intArrayOf(
                                            CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                                            CONTROL_VIDEO_STABILIZATION_MODE_ON
                                        )
                                )
                        )
                    )
            )

        assertThat(cameraInfo.isPreviewStabilizationSupported).isFalse()
    }

    @Test
    fun cameraInfo_isVideoStabilizationSupported() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                                        intArrayOf(
                                            CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                                            CONTROL_VIDEO_STABILIZATION_MODE_ON
                                        )
                                )
                        )
                    )
            )

        assertThat(cameraInfo.isVideoStabilizationSupported).isTrue()
    }

    @Test
    fun cameraInfo_isVideoStabilizationNotSupported() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES to
                                        intArrayOf(CONTROL_VIDEO_STABILIZATION_MODE_OFF)
                                )
                        )
                    )
            )

        assertThat(cameraInfo.isVideoStabilizationSupported).isFalse()
    }

    // Analog to Camera2CameraInfoImplTest#apiVersionMet_canReturnSupportedHdrDynamicRanges()
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun cameraInfo_hdrDynamicRangeSupported() {
        val cameraInfo: CameraInfo =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to
                                        HLG10_UNCONSTRAINED
                                )
                        )
                    )
            )

        assertThat(
                cameraInfo.querySupportedDynamicRanges(
                    setOf(
                        HLG_10_BIT,
                        HDR10_10_BIT,
                        HDR10_PLUS_10_BIT,
                        DOLBY_VISION_10_BIT,
                        DOLBY_VISION_8_BIT
                    )
                )
            )
            .containsExactly(HLG_10_BIT)

        assertThat(
                cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.HDR_UNSPECIFIED_10_BIT))
            )
            .containsExactly(HLG_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun cameraInfo_tenBitHdrDynamicRangeSupported_whenAlsoQuerying8Bit() {
        val cameraInfo: CameraInfo =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to
                                        DOLBY_VISION_10B_UNCONSTRAINED
                                )
                        )
                    )
            )

        assertThat(
                cameraInfo.querySupportedDynamicRanges(
                    setOf(DOLBY_VISION_10_BIT, DOLBY_VISION_8_BIT)
                )
            )
            .containsExactly(DOLBY_VISION_10_BIT)
    }

    // Analog to Camera2CameraInfoImplTest#apiVersionMet_canReturnSupportedDynamicRanges()
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun cameraInfo_returnsAllSupportedDynamicRanges_whenQueryingWithUnspecified() {
        val cameraInfo: CameraInfo =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to
                                        HLG10_UNCONSTRAINED
                                )
                        )
                    )
            )

        assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)))
            .containsExactly(DynamicRange.SDR, HLG_10_BIT)
    }

    // Analog to
    // Camera2CameraInfoImplTest#apiVersionMet_canReturnSupportedDynamicRanges_fromFullySpecified()
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun cameraInfo_hdrAndSdrDynamicRangesSupported_whenQueryingWithFullySpecified() {
        val cameraInfo: CameraInfo =
            createCameraInfoAdapter(
                cameraProperties =
                    FakeCameraProperties(
                        FakeCameraMetadata(
                            characteristics =
                                mapOf(
                                    CameraCharacteristics
                                        .REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to
                                        HLG10_UNCONSTRAINED
                                )
                        )
                    )
            )

        assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.SDR, HLG_10_BIT)))
            .containsExactly(DynamicRange.SDR, HLG_10_BIT)
    }

    // Analog to Camera2CameraInfoImplTest#apiVersionNotMet_canReturnSupportedDynamicRanges()
    @Test
    fun cameraInfo_queryUnspecifiedDynamicRangeSupported() {
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)))
            .containsExactly(DynamicRange.SDR)
    }

    // Analog to Camera2CameraInfoImplTest#apiVersionNotMet_queryHdrDynamicRangeNotSupported()
    @Test
    fun cameraInfo_queryForHdrWhenUnsupported_returnsEmptySet() {
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThat(
                cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.HDR_UNSPECIFIED_10_BIT))
            )
            .isEmpty()
    }

    // Analog to Camera2CameraInfoImplTest#querySdrDynamicRange_alwaysSupported()
    @Test
    fun cameraInfo_querySdrSupported() {
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.SDR)))
            .containsExactly(DynamicRange.SDR)
    }

    // Analog to Camera2CameraInfoImplTest#queryDynamicRangeWithEmptySet_throwsException()
    @Test
    fun cameraInfo_queryWithEmptySet_throwsException() {
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThrows(IllegalArgumentException::class.java) {
            cameraInfo.querySupportedDynamicRanges(emptySet())
        }
    }

    @Test
    fun cameraInfo_queryLogicalMultiCameraSupported() {
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThat(cameraInfo.isLogicalMultiCameraSupported).isTrue()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Test
    fun cameraInfo_getPhysicalCameraInfos() {
        val physicalCameraIds = setOf(CameraId.fromCamera2Id("5"), CameraId.fromCamera2Id("6"))
        val cameraInfo: CameraInfo = createCameraInfoAdapter()

        assertThat(cameraInfo.physicalCameraInfos).isNotNull()
        assertThat(cameraInfo.physicalCameraInfos.size).isEqualTo(2)
        for (info in cameraInfo.physicalCameraInfos) {
            assertThat(physicalCameraIds)
                .contains(CameraId(Camera2CameraInfo.from(info).getCameraId()))
        }
    }

    @Test
    fun intrinsicZoomRatioIsLessThan1_whenSensorHorizontalLengthWiderThanDefault() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(ultraWideCameraId),
                cameraProperties = ultraWideCameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(defaultCameraProperties.metadata),
                                CameraBackendId(ultraWideCameraId) to
                                    listOf(ultraWideCameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isLessThan(1)
    }

    @Test
    fun intrinsicZoomRatioIsGreaterThan1_whenSensorHorizontalLengthSmallerThanDefault() {
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(telephotoCameraId),
                cameraProperties = telephotoCameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(defaultCameraProperties.metadata),
                                CameraBackendId(telephotoCameraId) to
                                    listOf(telephotoCameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isGreaterThan(1)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoLensFacingInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.LENS_FACING)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoLensFocalLengthInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoSensorOrientationInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.SENSOR_ORIENTATION)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoSensorPixelArraySizeInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoSensorActiveArraySizeInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun intrinsicZoomRatioIsUnknown_whenNoSensorPhysicalSizeInfo() {
        val cameraProperties =
            FakeCameraProperties(
                FakeCameraMetadata(
                    characteristics =
                        defaultCameraCharacteristics.toMutableMap().apply {
                            remove(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        }
                )
            )
        val cameraInfo: CameraInfoInternal =
            createCameraInfoAdapter(
                cameraId = CameraId(defaultCameraId),
                cameraProperties = cameraProperties,
                cameraDevices =
                    FakeCameraDevices(
                        defaultCameraBackendId = CameraBackendId(defaultCameraId),
                        concurrentCameraBackendIds = emptySet(),
                        cameraMetadataMap =
                            mapOf(
                                CameraBackendId(defaultCameraId) to
                                    listOf(cameraProperties.metadata),
                            )
                    )
            )

        assertThat(cameraInfo.intrinsicZoomRatio).isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun canUnwrapRestrictedCameraInfoAsCameraMetadata() {
        val fakeCameraConfig = FakeCameraConfig()
        val restrictedCameraInfo = RestrictedCameraInfo(cameraInfoAdapter, fakeCameraConfig)

        val cameraMetadata = restrictedCameraInfo.unwrapAs(CameraMetadata::class)
        assertThat(cameraMetadata).isNotNull()
    }
}
