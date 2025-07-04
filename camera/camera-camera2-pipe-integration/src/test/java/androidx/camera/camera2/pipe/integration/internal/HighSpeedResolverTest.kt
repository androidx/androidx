/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class HighSpeedResolverTest {

    private companion object {
        private const val CAMERA_ID = "0"

        private const val FPS_30 = 30
        private const val FPS_120 = 120
        private const val FPS_240 = 240
        private const val FPS_480 = 480

        private val RANGE_30_120 = Range.create(FPS_30, FPS_120)
        private val RANGE_120_120 = Range.create(FPS_120, FPS_120)
        private val RANGE_30_240 = Range.create(FPS_30, FPS_240)
        private val RANGE_240_240 = Range.create(FPS_240, FPS_240)
        private val RANGE_30_480 = Range.create(FPS_30, FPS_480)
        private val RANGE_480_480 = Range.create(FPS_480, FPS_480)

        private const val FORMAT_PRIVATE =
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE

        private val COMMON_HIGH_SPEED_SUPPORTED_SIZE_FPS_MAP =
            mapOf(
                RESOLUTION_1080P to
                    listOf(RANGE_30_120, RANGE_120_120, RANGE_30_240, RANGE_240_240),
                RESOLUTION_720P to
                    listOf(
                        RANGE_30_120,
                        RANGE_120_120,
                        RANGE_30_240,
                        RANGE_240_240,
                        RANGE_30_480,
                        RANGE_480_480,
                    ),
            )
    }

    private val defaultHighSpeedResolver = createHighSpeedResolver()
    private val emptyHighSpeedResolver =
        createHighSpeedResolver(
            characteristics = createCharacteristicsMap(supportedHighSpeedSizeAndFpsMap = emptyMap())
        )

    @Test
    fun isHighSpeedOn_configsHaveSameSessionTypeHighSpeed_returnsTrue() {
        val attachedSurfaceInfos =
            listOf(createAttachedSurfaceInfo(sessionType = SESSION_TYPE_HIGH_SPEED))
        val useCaseConfigs =
            listOf<UseCaseConfig<*>>(
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_HIGH_SPEED),
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_HIGH_SPEED),
            )

        val result = HighSpeedResolver.isHighSpeedOn(attachedSurfaceInfos, useCaseConfigs)

        assertThat(result).isTrue()
    }

    @Test
    fun isHighSpeedOn_configsDoNotHaveSessionTypeHighSpeed_returnsFalse() {
        val attachedSurfaceInfos =
            listOf(createAttachedSurfaceInfo(sessionType = SESSION_TYPE_REGULAR))
        val useCaseConfigs =
            listOf<UseCaseConfig<*>>(
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_REGULAR),
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_REGULAR),
            )

        val result = HighSpeedResolver.isHighSpeedOn(attachedSurfaceInfos, useCaseConfigs)

        assertThat(result).isFalse()
    }

    @Test
    fun isHighSpeedOn_configsHaveDifferentSessionTypes_throwsException() {
        // Differ in AttachedSurfaceInfo list
        val attachedSurfaceInfos =
            listOf(
                createAttachedSurfaceInfo(sessionType = SESSION_TYPE_HIGH_SPEED),
                createAttachedSurfaceInfo(sessionType = SESSION_TYPE_REGULAR),
            )
        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedResolver.isHighSpeedOn(attachedSurfaceInfos, emptyList())
        }

        // Differ in UseCaseConfig list
        val useCaseConfigs =
            listOf(
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_HIGH_SPEED),
                createFakeUseCaseConfig(sessionType = SESSION_TYPE_REGULAR),
            )
        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedResolver.isHighSpeedOn(emptyList(), useCaseConfigs)
        }

        // Differ from AttachedSurfaceInfo list and UseCaseConfig list
        val attachedSurfaceInfos2 =
            listOf(createAttachedSurfaceInfo(sessionType = SESSION_TYPE_HIGH_SPEED))
        val useCaseConfigs2 = listOf(createFakeUseCaseConfig(sessionType = SESSION_TYPE_REGULAR))
        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedResolver.isHighSpeedOn(attachedSurfaceInfos2, useCaseConfigs2)
        }
    }

    @Test
    fun filterCommonSupportedSizes_returnsCorrectMap() {
        val useCaseSupportedSizeMap =
            listOf(
                    listOf(RESOLUTION_480P, RESOLUTION_720P, RESOLUTION_1080P),
                    listOf(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_2160P),
                    listOf(RESOLUTION_480P, RESOLUTION_720P, RESOLUTION_1080P, RESOLUTION_VGA),
                )
                .toUseCaseSupportedSizeMap()

        val result = defaultHighSpeedResolver.filterCommonSupportedSizes(useCaseSupportedSizeMap)

        // Assert: return common sizes and preserve the original order.
        assertThat(result.values)
            .containsExactly(
                listOf(RESOLUTION_720P, RESOLUTION_1080P),
                listOf(RESOLUTION_1080P, RESOLUTION_720P),
                listOf(RESOLUTION_720P, RESOLUTION_1080P),
            )
            .inOrder()
    }

    @Test
    fun getMaxSize_noSupportedSizes_returnsNull() {
        val result = emptyHighSpeedResolver.maxSize

        assertThat(result).isNull()
    }

    @Test
    fun getMaxSize_supportedSizesExist_returnsLargestSize() {
        val result = defaultHighSpeedResolver.maxSize

        assertThat(result).isEqualTo(RESOLUTION_1080P)
    }

    @Test
    fun getMaxFrameRate_noSupportedFpsRanges_returnsZero() {
        val result = emptyHighSpeedResolver.getMaxFrameRate(RESOLUTION_1080P)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun getMaxFrameRate_supportedFpsRangesExist_returnMaxFps() {
        val result = defaultHighSpeedResolver.getMaxFrameRate(RESOLUTION_1080P)

        assertThat(result).isEqualTo(FPS_240)
    }

    @Test
    fun getSizeArrangements_emptyInput_returnEmptyList() {
        val sizeArrangements = defaultHighSpeedResolver.getSizeArrangements(emptyList())

        assertThat(sizeArrangements).isEmpty()
    }

    @Test
    fun getSizeArrangements_hasCommonSizes_returnCorrectArrangements() {
        val common1080p = RESOLUTION_1080P
        val common720p = RESOLUTION_720P
        val supportedOutputSizesList =
            listOf(
                listOf(RESOLUTION_480P, common720p, common1080p),
                listOf(common1080p, common720p, RESOLUTION_2160P),
                listOf(RESOLUTION_480P, common720p, common1080p, RESOLUTION_VGA),
            )
        val sizeArrangements =
            defaultHighSpeedResolver.getSizeArrangements(supportedOutputSizesList)

        assertThat(sizeArrangements)
            .containsExactly(
                listOf(common720p, common720p, common720p),
                listOf(common1080p, common1080p, common1080p),
            )
            .inOrder()
    }

    @Test
    fun getSizeArrangements_noCommonSizes_returnEmptyList() {
        val supportedOutputSizesList =
            listOf(
                listOf(RESOLUTION_480P, RESOLUTION_720P),
                listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                listOf(RESOLUTION_1080P, RESOLUTION_720P),
            )

        val result = defaultHighSpeedResolver.getSizeArrangements(supportedOutputSizesList)

        assertThat(result).isEmpty()
    }

    @Test
    fun getFrameRateRangesFor_invalidInput_returnsNull() {
        // More than 2 surfaces.
        assertThat(
                defaultHighSpeedResolver.getFrameRateRangesFor(
                    listOf(RESOLUTION_720P, RESOLUTION_720P, RESOLUTION_720P)
                )
            )
            .isNull()

        // Different sizes.
        assertThat(
                defaultHighSpeedResolver.getFrameRateRangesFor(
                    listOf(RESOLUTION_720P, RESOLUTION_1080P)
                )
            )
            .isNull()

        // Empty list.
        assertThat(defaultHighSpeedResolver.getFrameRateRangesFor(emptyList())).isNull()
    }

    @Test
    fun getFrameRateRangesFor_noSupportedSizes_returnsNull() {
        assertThat(emptyHighSpeedResolver.getFrameRateRangesFor(listOf(RESOLUTION_720P))).isNull()
    }

    @Test
    fun getFrameRateRangesFor_oneSurface_returnsAllSupportedRanges() {
        val result = defaultHighSpeedResolver.getFrameRateRangesFor(listOf(RESOLUTION_720P))

        assertThat(result!!.toList())
            .containsExactly(
                RANGE_30_120,
                RANGE_120_120,
                RANGE_30_240,
                RANGE_240_240,
                RANGE_30_480,
                RANGE_480_480,
            )
    }

    @Test
    fun getFrameRateRangesFor_twoSurfaces_returnsFixedFpsRanges() {
        val result =
            defaultHighSpeedResolver.getFrameRateRangesFor(listOf(RESOLUTION_720P, RESOLUTION_720P))

        assertThat(result!!.toList())
            .containsExactly(RANGE_120_120, RANGE_240_240, RANGE_480_480)
            .inOrder()
    }

    private fun createHighSpeedResolver(
        cameraId: CameraId = CameraId(CAMERA_ID),
        characteristics: Map<CameraCharacteristics.Key<*>, Any?> = createCharacteristicsMap(),
    ): HighSpeedResolver {
        return HighSpeedResolver(
            cameraMetadata =
                FakeCameraMetadata(cameraId = cameraId, characteristics = characteristics)
        )
    }

    private fun createCharacteristicsMap(
        hardwareLevel: Int = INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        supportedHighSpeedSizeAndFpsMap: Map<Size, List<Range<Int>>>? =
            COMMON_HIGH_SPEED_SUPPORTED_SIZE_FPS_MAP,
    ): Map<CameraCharacteristics.Key<*>, Any?> {
        val mockMap =
            Mockito.mock(StreamConfigurationMap::class.java).also { map ->
                if (supportedHighSpeedSizeAndFpsMap != null) {
                    // Mock highSpeedVideoSizes
                    Mockito.`when`(map.highSpeedVideoSizes)
                        .thenReturn(supportedHighSpeedSizeAndFpsMap.keys.toTypedArray())

                    // Mock highSpeedVideoFpsRanges
                    val allFpsRanges = supportedHighSpeedSizeAndFpsMap.values.flatten().distinct()
                    Mockito.`when`(map.highSpeedVideoFpsRanges)
                        .thenReturn(allFpsRanges.toTypedArray())

                    // Mock getHighSpeedVideoSizesFor
                    allFpsRanges.forEach { fpsRange ->
                        val sizesForRange =
                            supportedHighSpeedSizeAndFpsMap.entries
                                .filter { (_, fpsRanges) -> fpsRanges.contains(fpsRange) }
                                .map { it.key }
                                .sortedWith(CompareSizesByArea(false)) // Descending order
                                .toTypedArray()
                        Mockito.`when`(map.getHighSpeedVideoSizesFor(fpsRange))
                            .thenReturn(sizesForRange)
                    }

                    // Mock getHighSpeedVideoFpsRangesFor
                    supportedHighSpeedSizeAndFpsMap.forEach { (size, fpsRanges) ->
                        Mockito.`when`(map.getHighSpeedVideoFpsRangesFor(size))
                            .thenReturn(fpsRanges.toTypedArray())
                    }
                }
            }

        return mutableMapOf<CameraCharacteristics.Key<*>, Any?>(
            INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
            SCALER_STREAM_CONFIGURATION_MAP to mockMap,
        )
    }

    private fun createFakeUseCaseConfig(
        sessionType: Int = SESSION_TYPE_HIGH_SPEED,
        targetFrameRate: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
    ): FakeUseCaseConfig =
        FakeUseCaseConfig.Builder()
            .setSessionType(sessionType)
            .setTargetFrameRate(targetFrameRate)
            .useCaseConfig

    private fun createAttachedSurfaceInfo(
        surfaceConfig: SurfaceConfig = SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW),
        imageFormat: Int = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
        size: Size = RESOLUTION_480P,
        dynamicRange: DynamicRange = SDR,
        captureTypes: List<CaptureType> = listOf(CaptureType.PREVIEW),
        implementationOptions: androidx.camera.core.impl.Config? = null,
        sessionType: Int = SESSION_TYPE_HIGH_SPEED,
        targetFrameRate: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
        isStrictFrameRateRequired: Boolean = false,
    ): AttachedSurfaceInfo =
        AttachedSurfaceInfo.create(
            surfaceConfig,
            imageFormat,
            size,
            dynamicRange,
            captureTypes,
            implementationOptions,
            sessionType,
            targetFrameRate,
            isStrictFrameRateRequired,
        )

    private fun List<List<Size>>.toUseCaseSupportedSizeMap(): Map<UseCaseConfig<*>, List<Size>> {
        return associate { sizes ->
            FakeUseCaseConfig.Builder(CaptureType.PREVIEW, FORMAT_PRIVATE).build().currentConfig to
                sizes
        }
    }
}
