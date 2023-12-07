/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.os.Build
import android.util.Size
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.internal.SupportedOutputSizesSorter
import androidx.camera.core.streamsharing.ResolutionsMerger.filterOutParentSizeThatIsTooSmall
import androidx.camera.core.streamsharing.ResolutionsMerger.filterResolutionsByAspectRatio
import androidx.camera.core.streamsharing.ResolutionsMerger.getParentSizesThatAreTooLarge
import androidx.camera.core.streamsharing.ResolutionsMerger.hasUpscaling
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ResolutionsMerger].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ResolutionsMergerTest {

    @Test(expected = IllegalArgumentException::class)
    fun getMergedResolutions_whenNoSupportedChildSize_throwsException() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = emptyList<Size>() // no supported size
        val sorter = FakeSupportedOutputSizesSorter(
            mapOf(
                config1 to candidateChildSizes1,
                config2 to candidateChildSizes2
            )
        )
        val merger = ResolutionsMerger(sensorSize, childConfigs, sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert.
        val parentConfig = MutableOptionsBundle.create()
        merger.getMergedResolutions(parentConfig)
    }

    @Test
    fun getMergedResolutions_whenChildRequiresSensorAndNonSensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720, SIZE_640_480) // 4:3
        val sorter = FakeSupportedOutputSizesSorter(
            mapOf(
                config1 to candidateChildSizes1,
                config2 to candidateChildSizes2
            )
        )
        val merger = ResolutionsMerger(sensorSize, childConfigs, sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert, should returns a list that concatenates 4:3 resolutions before 16:9
        // resolutions and removes resolutions that are too large (no need for multiple resolutions
        // that can be cropped to all child sizes) and too small (causing upscaling).
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).containsExactly(
            SIZE_1920_1440, SIZE_1280_960, SIZE_1920_1080, SIZE_1280_720
        ).inOrder()
    }

    @Test
    fun getMergedResolutions_whenChildRequiresOnlySensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_2560_1920, SIZE_1920_1440) // 4:3
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720) // 4:3
        val sorter = FakeSupportedOutputSizesSorter(
            mapOf(
                config1 to candidateChildSizes1,
                config2 to candidateChildSizes2
            )
        )
        val merger = ResolutionsMerger(sensorSize, childConfigs, sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert, should returns a list of 4:3 resolutions and removes resolutions that are
        // too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).containsExactly(
            SIZE_2560_1920, SIZE_1920_1440
        ).inOrder()
    }

    @Test
    fun getMergedResolutions_whenChildRequiresOnlyNonSensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_2560_1440, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = listOf(SIZE_1920_1080, SIZE_960_540) // 16:9
        val sorter = FakeSupportedOutputSizesSorter(
            mapOf(
                config1 to candidateChildSizes1,
                config2 to candidateChildSizes2
            )
        )
        val merger = ResolutionsMerger(sensorSize, childConfigs, sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert, should returns a list of 16:9 resolutions and removes resolutions that are
        // too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).containsExactly(
            SIZE_2560_1440, SIZE_1920_1080, SIZE_1280_720
        ).inOrder()
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPreferredChildSize_withConfigNotPassedToConstructor_throwsException() {
        // Arrange.
        val config = createUseCaseConfig()
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to SIZES_16_9))
        val merger = ResolutionsMerger(SENSOR_SIZE, setOf(config), sorter, CAMERA_SUPPORTED_SIZES)

        // Act.
        val useCaseConfigNotPassedToConstructor = createUseCaseConfig()
        merger.getPreferredChildSize(SIZE_1920_1440, useCaseConfigNotPassedToConstructor)
    }

    @Test
    fun getPreferredChildSize_whenParentSizeIsSensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes = listOf(SIZE_2560_1440, SIZE_1920_1080, SIZE_960_540) // 16:9
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, setOf(config), sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert, should returns the first child size that do not need upscale.
        assertThat(merger.getPreferredChildSize(SIZE_1920_1440, config)).isEqualTo(SIZE_1920_1080)
        assertThat(merger.getPreferredChildSize(SIZE_1280_960, config)).isEqualTo(SIZE_960_540)

        // Act & Assert, should returns parent size when no matching.
        assertThat(merger.getPreferredChildSize(SIZE_640_480, config)).isEqualTo(SIZE_640_480)
    }

    @Test
    fun getPreferredChildSize_whenParentSizeIsNotSensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes = listOf(
            // 4:3
            SIZE_2560_1920,
            SIZE_1920_1440,
            SIZE_1280_960,
            SIZE_960_720,
            // 16:9
            SIZE_1920_1080,
            SIZE_960_540
        )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, setOf(config), sorter, CAMERA_SUPPORTED_SIZES)

        // Act & Assert, should returns the first child size that do not need upscale and cause
        // double-cropping.
        assertThat(merger.getPreferredChildSize(SIZE_2560_1440, config)).isEqualTo(SIZE_1920_1080)
        assertThat(merger.getPreferredChildSize(SIZE_1280_720, config)).isEqualTo(SIZE_960_540)

        // Act & Assert, should returns parent size when no matching.
        assertThat(merger.getPreferredChildSize(SIZE_192_108, config)).isEqualTo(SIZE_192_108)
    }

    @Test
    fun filterResolutionsByAspectRatio_canFilter_4_3() {
        val sizes = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        assertThat(filterResolutionsByAspectRatio(ASPECT_RATIO_4_3, sizes)).containsExactly(
            *SIZES_4_3.toTypedArray()
        ).inOrder()
    }

    @Test
    fun filterResolutionsByAspectRatio_canFilter_16_9() {
        val sizes = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        assertThat(filterResolutionsByAspectRatio(ASPECT_RATIO_16_9, sizes)).containsExactly(
            *SIZES_16_9.toTypedArray()
        ).inOrder()
    }

    @Test
    fun filterOutParentSizeThatIsTooSmall_canFilterOutSmallSizes() {
        val parentSizes = listOf(
            SIZE_3264_2448,
            SIZE_2560_1920,
            SIZE_1920_1440,
            SIZE_1280_960,
            SIZE_960_720,
            SIZE_640_480,
            SIZE_320_240
        )
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, parentSizes)).containsExactly(
            SIZE_3264_2448,
            SIZE_2560_1920,
            SIZE_1920_1440,
            SIZE_1280_960,
            SIZE_960_720,
        ).inOrder()
    }

    @Test
    fun getParentSizesThatAreTooLarge_canReturnLargeSizes() {
        val parentSizes = listOf(
            SIZE_3264_2448,
            SIZE_2560_1920,
            SIZE_1920_1440,
            SIZE_1280_960,
            SIZE_960_720,
            SIZE_640_480,
            SIZE_320_240
        )
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(getParentSizesThatAreTooLarge(childSizes, parentSizes)).containsExactly(
            SIZE_3264_2448,
            SIZE_2560_1920
        ).inOrder()
    }

    @Test
    fun hasUpscaling_return_false_whenTwoSizesAreEqualed() {
        assertThat(hasUpscaling(SIZE_1280_960, SIZE_1280_960)).isFalse()
        assertThat(hasUpscaling(SIZE_1920_1080, SIZE_1920_1080)).isFalse()
    }

    @Test
    fun hasUpscaling_return_false_whenChildSizeIsSmaller() {
        assertThat(hasUpscaling(SIZE_1280_960, SIZE_1920_1440)).isFalse()
        assertThat(hasUpscaling(SIZE_1280_720, SIZE_1920_1080)).isFalse()
    }

    @Test
    fun hasUpscaling_return_true_whenChildSizeIsLarger() {
        assertThat(hasUpscaling(SIZE_1920_1440, SIZE_1280_960)).isTrue()
        assertThat(hasUpscaling(SIZE_1920_1080, SIZE_1280_720)).isTrue()
    }

    @Test
    fun hasUpscaling_return_true_whenChildSizeIsLargerOnWidth() {
        assertThat(hasUpscaling(SIZE_1440_720, SIZE_1280_960)).isTrue()
        assertThat(hasUpscaling(SIZE_1440_720, SIZE_1280_720)).isTrue()
    }

    @Test
    fun hasUpscaling_return_true_whenChildSizeIsLargerOnHeight() {
        assertThat(hasUpscaling(SIZE_800_800, SIZE_1280_720)).isTrue()
        assertThat(hasUpscaling(SIZE_720_720, SIZE_960_540)).isTrue()
    }

    private fun createUseCaseConfig(): UseCaseConfig<*> {
        return FakeUseCaseConfig.Builder().useCaseConfig
    }

    /**
     * A fake implementation of [SupportedOutputSizesSorter] for testing.
     */
    private class FakeSupportedOutputSizesSorter(
        private val supportedOutputSizes: Map<UseCaseConfig<*>, List<Size>>
    ) : SupportedOutputSizesSorter(FakeCameraInfoInternal(), null) {
        override fun getSortedSupportedOutputSizes(useCaseConfig: UseCaseConfig<*>): List<Size> {
            return supportedOutputSizes[useCaseConfig]!!
        }
    }

    companion object {
        // 4:3 resolutions.
        private val SIZE_3264_2448 = Size(3264, 2448)
        private val SIZE_2560_1920 = Size(2560, 1920)
        private val SIZE_1920_1440 = Size(1920, 1440)
        private val SIZE_1280_960 = Size(1280, 960)
        private val SIZE_960_720 = Size(960, 720)
        private val SIZE_640_480 = Size(640, 480)
        private val SIZE_320_240 = Size(320, 240)
        private val SIZES_4_3 = listOf(
            SIZE_3264_2448,
            SIZE_2560_1920,
            SIZE_1920_1440,
            SIZE_1280_960,
            SIZE_960_720,
            SIZE_640_480,
            SIZE_320_240
        )
        // 16:9 resolutions.
        private val SIZE_3840_2160 = Size(3840, 2160)
        private val SIZE_2560_1440 = Size(2560, 1440)
        private val SIZE_1920_1080 = Size(1920, 1080)
        private val SIZE_1280_720 = Size(1280, 720)
        private val SIZE_960_540 = Size(960, 540)
        private val SIZE_192_108 = Size(192, 108)
        private val SIZES_16_9 = listOf(
            SIZE_3840_2160,
            SIZE_2560_1440,
            SIZE_1920_1080,
            SIZE_1280_720,
            SIZE_960_540,
            SIZE_192_108
        )
        // Other aspect-ratio resolutions.
        private val SIZE_1440_720 = Size(1440, 720)
        private val SIZE_800_800 = Size(800, 800)
        private val SIZE_720_720 = Size(720, 720)
        private val SIZE_500_400 = Size(500, 400)
        private val SIZE_176_144 = Size(176, 144)
        private val SIZES_OTHER_ASPECT_RATIO = listOf(
            SIZE_1440_720,
            SIZE_800_800,
            SIZE_720_720,
            SIZE_500_400,
            SIZE_176_144
        )
        private val CAMERA_SUPPORTED_SIZES = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        private val SENSOR_SIZE = SIZE_3264_2448 // 4:3
    }
}
