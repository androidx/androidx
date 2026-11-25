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

package androidx.camera.camera2.impl

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.compat.workaround.ExtraSupportedSurfaceCombinationsContainer
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraInfoAdapterCreator
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.camera2.testing.TestShadowWindowManager
import androidx.camera.common.CameraId
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowDisplayManager.removeDisplay
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS], shadows = [TestShadowWindowManager::class])
class MeteringRepeatingTest {
    companion object {
        val dummyZeroSizeStreamSpec = StreamSpec.builder(Size(0, 0)).build()

        val dummySizeListWithout640x480 =
            listOf(
                Size(4160, 3120),
                Size(1920, 1080),
                Size(1280, 720),
                Size(320, 240),
                Size(240, 144),
            )

        val dummySizeListWith640x480 =
            listOf(
                Size(4160, 3120),
                Size(1920, 1080),
                Size(1280, 720),
                Size(640, 480),
                Size(320, 240),
            )

        val dummySizeListWithoutSmaller =
            listOf(Size(4160, 3120), Size(1920, 1080), Size(1280, 720))

        val dummySizeListSmallerThan640x480 = listOf(Size(320, 480), Size(320, 240), Size(240, 144))

        val dummySizeListNotWithin320x240And640x480 =
            listOf(Size(4160, 3120), Size(1920, 1080), Size(1280, 720), Size(240, 144))

        val dummySizeListSmallerThan320x240 = listOf(Size(240, 144), Size(192, 144), Size(160, 120))

        fun getFakeMetadata(sizeList: List<Size>): FakeCameraMetadata {
            val shuffledList = sizeList.shuffled()

            val builder = StreamConfigurationMapBuilder.newBuilder()
            for (size in shuffledList) {
                builder.addOutputSize(size)
            }

            return FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    mapOf(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to builder.build()),
            )
        }
    }

    private lateinit var meteringRepeating: MeteringRepeating

    private val originalBrand = Build.BRAND
    private val originalModel = Build.MODEL

    private fun addDisplay(width: Int, height: Int) {
        ShadowDisplayManager.addDisplay(String.format("w%ddp-h%ddp", width, height))
    }

    private fun initDisplays(outputSizeList: List<Size>) {
        DisplayInfoManager.releaseInstance()
        for (size in outputSizeList) {
            addDisplay(size.width, size.height)
        }

        if (Build.VERSION.SDK_INT in 26..28) {
            /**
             * There seems to be a Robolectric bug where removeDisplay(0) call without adding other
             * displays and then calling [DisplayManager.getDisplays] first will make subsequent
             * [DisplayManager.getDisplays] calls fail.
             */
            ((ApplicationProvider.getApplicationContext() as Context).getSystemService(
                    Context.DISPLAY_SERVICE
                ) as DisplayManager?)
                ?.displays
        }

        removeDisplay(0)
    }

    private fun getMeteringRepeatingAndInitDisplay(outputSizeList: List<Size>): MeteringRepeating {
        initDisplays(outputSizeList)

        return MeteringRepeating.Builder(
                FakeCameraProperties(getFakeMetadata(outputSizeList)),
                DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()),
            )
            .build()
    }

    private fun createMeteringRepeatingWithMinFrameDurations(
        minFrameDurationsBySize: Map<Size, Long>,
        manualSensor: Boolean = true,
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
    ): Pair<MeteringRepeating, FakeCameraMetadata> {
        initDisplays(minFrameDurationsBySize.keys.toList())
        val map =
            FakeCameraInfoAdapterCreator.createMockStreamConfigurationMap(minFrameDurationsBySize)
        val capabilities = buildList {
            if (manualSensor) add(REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
            if (hardwareLevel == INFO_SUPPORTED_HARDWARE_LEVEL_3) {
                add(REQUEST_AVAILABLE_CAPABILITIES_RAW)
            }
        }
            .toIntArray()
        val metadata =
            FakeCameraMetadata(
                cameraId = CameraId("1"),
                characteristics =
                    mapOf(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to map,
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                            android.graphics.Rect(0, 0, 4000, 3000),
                    ),
            )
        val metering =
            MeteringRepeating.Builder(
                    FakeCameraProperties(metadata),
                    DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()),
                )
                .build()
        return metering to metadata
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", originalBrand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", originalModel)
        if (::meteringRepeating.isInitialized) {
            meteringRepeating.onUnbind()
        }
        DisplayInfoManager.releaseInstance()
    }

    @Test
    fun surfaceResolutionIsLargestLessThan640x480_when640x480NotPresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithout640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(320, 240), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIs640x480_when640x480PresentInOutputSizes() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWith640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(640, 480), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionFallsBackToMinimum_whenAllOutputSizesLargerThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWithoutSmaller)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(1280, 720), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIsLargestWithinPreviewSize_whenAllOutputSizesLessThan640x480() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListSmallerThan640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertEquals(Size(320, 480), meteringRepeating.attachedSurfaceResolution)
    }

    @Test
    fun surfaceResolutionIsLargestLessThan640x480_whenSizesOutside320x240And640x480() {
        meteringRepeating =
            getMeteringRepeatingAndInitDisplay(dummySizeListNotWithin320x240And640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(240, 144))
    }

    @Test
    fun surfaceResolutionIsLargerThan320x240_whenHuaweiMate9AndSizesOutside320x240And640x480() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Huawei")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "mha-l29")

        meteringRepeating =
            getMeteringRepeatingAndInitDisplay(dummySizeListNotWithin320x240And640x480)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun surfaceResolutionFallsBackToLargest_whenHuaweiMate9AndAllOutputSizesLessThan320x240() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Huawei")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "mha-l29")

        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListSmallerThan320x240)

        meteringRepeating.updateSuggestedStreamSpec(dummyZeroSizeStreamSpec, null)

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(240, 144))
    }

    @Test
    fun setupSessionWithSdr_setsSdrInStreamSpecAndOutputConfig() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWith640x480)

        meteringRepeating.setupSession(DynamicRange.SDR)

        assertThat(meteringRepeating.attachedStreamSpec?.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(meteringRepeating.sessionConfig.outputConfigs.single().dynamicRange)
            .isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun setupSessionWithDynamicRange_setsDynamicRangeInStreamSpecAndOutputConfig() {
        meteringRepeating = getMeteringRepeatingAndInitDisplay(dummySizeListWith640x480)

        meteringRepeating.setupSession(DynamicRange.HLG_10_BIT)

        assertThat(meteringRepeating.attachedStreamSpec?.dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
        assertThat(meteringRepeating.sessionConfig.outputConfigs.single().dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)

        // Verify error listener recreation preserves the dynamic range
        meteringRepeating.sessionConfig.errorListener?.onError(
            meteringRepeating.sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET,
        )
        assertThat(meteringRepeating.sessionConfig.outputConfigs.single().dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
    }

    @Test
    fun setupSession_selectsFpsCompatibleSize_whenVgaMaxFpsIsTooLow() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 33_333_332L,
                    Size(320, 240) to 33_333_332L,
                )
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
        // The frame rate only steers the surface size; it is not stamped onto the SessionConfig.
        assertThat(meteringRepeating.sessionConfig.expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)

        // Mixed [30, 60] also requires 60 FPS upper capability
        meteringRepeating.setupSession(DynamicRange.SDR, Range(30, 60))
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))

        // 30 FPS and unspecified preserve VGA 640x480
        meteringRepeating.setupSession(DynamicRange.SDR, Range(30, 30))
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(640, 480))
        assertThat(meteringRepeating.sessionConfig.expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
        meteringRepeating.setupSession(DynamicRange.SDR, StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(640, 480))
        assertThat(meteringRepeating.sessionConfig.expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun setupSession_acceptsRoundingBoundaryMinFrameDurationFor60Fps() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_667L, // 59.999998 FPS -> rounds to 60
                    Size(640, 480) to 33_333_332L,
                )
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun setupSession_keepsVga_whenVgaSupports60Fps() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 16_666_666L,
                    Size(320, 240) to 33_333_332L,
                )
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(640, 480))
    }

    @Test
    fun setupSession_treatsZeroMinFrameDurationAsUnlimited_whenManualSensorDisabled() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 0L,
                ),
                manualSensor = false,
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        // Without MANUAL_SENSOR, 0L means unlimited max FPS -> VGA stays eligible.
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(640, 480))
    }

    @Test
    fun setupSession_treatsZeroMinFrameDurationAsInvalid_whenManualSensorEnabled() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 0L,
                ),
                manualSensor = true,
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        // With MANUAL_SENSOR, 0L is invalid (0 FPS) -> falls forward to 1280x720.
        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun setupSession_fallsBackToVga_whenNoSizesMeetTargetFps() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1280, 720) to 33_333_332L,
                    Size(640, 480) to 33_333_332L,
                    Size(320, 240) to 33_333_332L,
                )
            )
        meteringRepeating = metering

        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(640, 480))
    }

    @Test
    fun setupSession_huaweiMate9QuirkAppliedBeforeFpsFilterFallback() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Huawei")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "mha-l29")
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(Size(1280, 720) to 33_333_332L, Size(240, 144) to 33_333_332L)
            )
        meteringRepeating = metering

        // Empty FPS fallback must NOT resurrect quirk-excluded 240x144.
        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        assertThat(meteringRepeating.attachedSurfaceResolution).isEqualTo(Size(1280, 720))
    }

    @Test
    fun setupSessionWithFps_errorResetPreservesFpsSelectedResolution() {
        val (metering, _) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 33_333_332L,
                )
            )
        meteringRepeating = metering
        meteringRepeating.setupSession(DynamicRange.SDR, Range(60, 60))

        meteringRepeating.sessionConfig.errorListener?.onError(
            meteringRepeating.sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET,
        )

        assertThat(meteringRepeating.sessionConfig.surfaces.single().prescribedSize)
            .isEqualTo(Size(1280, 720))
        assertThat(meteringRepeating.sessionConfig.expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun resolveStreamInfo_level3VgaOnlySlot_fallsBackToVgaInsteadOfNull() {
        val (metering, metadata) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 33_333_332L,
                ),
                hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_3,
            )
        meteringRepeating = metering
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                ApplicationProvider.getApplicationContext(),
                metadata,
                FakeEncoderProfilesProvider.Builder().build(),
                FeatureCombinationQuery.NO_OP_FEATURE_COMBINATION_QUERY,
                ExtraSupportedSurfaceCombinationsContainer(),
            )
        val factory = FakeUseCaseConfigFactory()
        val privConfig =
            FakeUseCaseConfig.Builder(
                    factory.getConfig(
                        CaptureType.PREVIEW,
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ),
                    CaptureType.PREVIEW,
                    ImageFormat.PRIVATE,
                )
                .useCaseConfig
        val jpegConfig =
            FakeUseCaseConfig.Builder(
                    factory.getConfig(
                        CaptureType.IMAGE_CAPTURE,
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ),
                    CaptureType.IMAGE_CAPTURE,
                    ImageFormat.JPEG,
                )
                .useCaseConfig
        val rawConfig =
            FakeUseCaseConfig.Builder(
                    factory.getConfig(
                        CaptureType.IMAGE_CAPTURE,
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ),
                    CaptureType.IMAGE_CAPTURE,
                    ImageFormat.RAW_SENSOR,
                )
                .useCaseConfig
        // LEVEL_3 supports (PRIV, PREVIEW) + (PRIV, VGA) + (JPEG, MAX) + (RAW, MAX),
        // NOT (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAX) + (RAW, MAX).
        val baseStreams =
            listOf(
                ResolvedStreamInfo(
                    privConfig,
                    StreamSpec.builder(Size(1280, 720))
                        .setExpectedFrameRateRange(Range(60, 60))
                        .build(),
                ),
                ResolvedStreamInfo(jpegConfig, StreamSpec.builder(Size(4000, 3000)).build()),
                ResolvedStreamInfo(rawConfig, StreamSpec.builder(Size(4000, 3000)).build()),
            )
        // Falls back to VGA 640x480 when the FPS-compatible 1280x720 overflows LEVEL_3, instead of
        // dropping the repeating stream.
        val result = meteringRepeating.resolveStreamInfo(baseStreams, supportedSurfaceCombination)
        assertThat(requireNotNull(result).streamSpec.resolution).isEqualTo(Size(640, 480))
    }

    @Test
    fun resolveStreamInfo_selectsHighestUpperFrameRate() {
        val (metering, metadata) =
            createMeteringRepeatingWithMinFrameDurations(
                mapOf(
                    Size(1920, 1080) to 16_666_666L,
                    Size(1280, 720) to 16_666_666L,
                    Size(640, 480) to 33_333_332L,
                )
            )
        meteringRepeating = metering
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                ApplicationProvider.getApplicationContext(),
                metadata,
                FakeEncoderProfilesProvider.Builder().build(),
                FeatureCombinationQuery.NO_OP_FEATURE_COMBINATION_QUERY,
                ExtraSupportedSurfaceCombinationsContainer(),
            )
        val factory = FakeUseCaseConfigFactory()
        val jpegConfig =
            FakeUseCaseConfig.Builder(
                    factory.getConfig(
                        CaptureType.IMAGE_CAPTURE,
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ),
                    CaptureType.IMAGE_CAPTURE,
                    ImageFormat.JPEG,
                )
                .useCaseConfig
        val videoConfig =
            FakeUseCaseConfig.Builder(
                    factory.getConfig(
                        CaptureType.VIDEO_CAPTURE,
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ),
                    CaptureType.VIDEO_CAPTURE,
                    ImageFormat.PRIVATE,
                )
                .useCaseConfig

        // E.g. a session-level frame rate (SessionConfig.frameRateRange) is propagated through
        // StreamSpec only, without a per-UseCase target frame rate.
        val result =
            meteringRepeating.resolveStreamInfo(
                listOf(
                    ResolvedStreamInfo(
                        jpegConfig,
                        StreamSpec.builder(Size(4000, 3000))
                            .setExpectedFrameRateRange(Range(30, 30))
                            .build(),
                    ),
                    ResolvedStreamInfo(
                        videoConfig,
                        StreamSpec.builder(Size(1920, 1080))
                            .setExpectedFrameRateRange(Range(60, 60))
                            .build(),
                    ),
                ),
                supportedSurfaceCombination,
            )

        assertThat(requireNotNull(result).streamSpec.resolution).isEqualTo(Size(1280, 720))
        assertThat(result.streamSpec.expectedFrameRateRange).isEqualTo(Range(60, 60))
    }
}
