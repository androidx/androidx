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

import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.util.Size
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.core.internal.SupportedOutputSizesSorter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.streamsharing.ResolutionsMerger.filterOutParentSizeThatIsTooSmall
import androidx.camera.core.streamsharing.ResolutionsMerger.filterResolutionsByAspectRatio
import androidx.camera.core.streamsharing.ResolutionsMerger.getCropRectOfReferenceAspectRatio
import androidx.camera.core.streamsharing.ResolutionsMerger.getParentSizesThatAreTooLarge
import androidx.camera.core.streamsharing.ResolutionsMerger.hasUpscaling
import androidx.camera.core.streamsharing.ResolutionsMerger.reverseRect
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [ResolutionsMerger]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ResolutionsMergerTest {

    @Test
    fun getMergedResolutions_whenNoSupportedChildSize_returnEmptyList() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = emptyList<Size>() // no supported size
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert.
        val parentConfig = MutableOptionsBundle.create()
        merger.getMergedResolutions(parentConfig)
        assertThat(merger.getMergedResolutions(parentConfig)).isEmpty()
    }

    @Test
    fun getMergedResolutions_whenChildRequiresSensorAndNonSensorAspectRatio_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080, SIZE_1280_720, SIZE_640_480) // 16:9, 4:3
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720, SIZE_1280_720) // 4:3, 16:9
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list that concatenates sensor (4:3) resolutions before
        // 16:9 resolutions and removes resolutions that are too large (no need for multiple
        // resolutions that can be cropped to all child sizes) and too small (causing upscaling).
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_1920_1080,
                SIZE_1280_720
            )
            .inOrder()
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
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list of 4:3 resolutions and removes resolutions that are
        // too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(SIZE_2560_1920, SIZE_1920_1440)
            .inOrder()
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
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list of 16:9 resolutions and removes resolutions that are
        // too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(SIZE_2560_1440, SIZE_1920_1080, SIZE_1280_720)
            .inOrder()
    }

    @Test
    fun getMergedResolutions_whenDifferentChildRequiresDifferentAspectRatio_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720, SIZE_640_480) // 4:3
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list of sensor (4:3) resolutions and removes resolutions
        // that are too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(SIZE_1920_1440, SIZE_1280_960)
            .inOrder()
    }

    @Test
    fun getMergedResolutions_whenAllChildrenRequires720To480Resolution_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_2560_1920, SIZE_1920_1440, SIZE_720_480) // 4:3, 3:2
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720, SIZE_720_480) // 4:3, 3:2
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list that concatenates 4:3 resolutions before 3:2
        // resolutions and removes resolutions that are too large (no need for multiple resolutions
        // that can be cropped to all child sizes) and too small (causing upscaling).
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_720_480
            )
            .inOrder()
    }

    @Test
    fun getMergedResolutions_whenOnlyOneChildRequires720To480Resolution_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_2560_1920, SIZE_1920_1440, SIZE_720_480) // 4:3, 3:2
        val candidateChildSizes2 = listOf(SIZE_1280_960, SIZE_960_720, SIZE_192_108) // 4:3, 16:9
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list that concatenates 4:3 resolutions before 3:2
        // resolutions and removes resolutions that are too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_720_480
            )
            .inOrder()
    }

    @Test
    fun getMergedResolutions_whenNeither16To9Nor4To3AreSupported_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3840_2000 // near 2:1
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_2560_1440, SIZE_1280_720) // 16:9
        val candidateChildSizes2 = listOf(SIZE_1920_1080, SIZE_960_540) // 16:9
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val fakeCameraInfo = createCameraInfo(supportedResolutions = SIZES_OTHER_ASPECT_RATIO)
        val merger = ResolutionsMerger(sensorSize, fakeCameraInfo, childConfigs, sorter)

        // Act & Assert, should returns a list of near 2:1 resolutions and removes resolutions that
        // are too large and too small.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig))
            .containsExactly(SIZE_3840_2000, SIZE_3840_1920, SIZE_2560_1280, SIZE_1440_720)
            .inOrder()
    }

    @Test
    fun getMergedResolutions_whenChildHasButDisabledHighResolutions_canReturnCorrectly() {
        // Arrange.
        val sensorSize = Size(4000, 3000) // 4:3
        val selector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .build()
        val config =
            FakeUseCaseConfig.Builder()
                .setHighResolutionDisabled(true)
                .setResolutionSelector(selector)
                .useCaseConfig
        val childConfigs = setOf(config)
        val candidateChildSizes = listOf(Size(4000, 3000))
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger =
            ResolutionsMerger(sensorSize, CAMERA_INFO_WITH_HIGH_RESOLUTIONS, childConfigs, sorter)

        // Act & Assert, should returns an empty list.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).isEmpty()
    }

    @Test
    fun getMergedResolutions_whenChildHasButNotAllowHighResolutions_canReturnCorrectly() {
        // Arrange.
        val sensorSize = Size(4000, 3000) // 4:3
        val config = createUseCaseConfig()
        val childConfigs = setOf(config)
        val candidateChildSizes = listOf(Size(4000, 3000))
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger =
            ResolutionsMerger(sensorSize, CAMERA_INFO_WITH_HIGH_RESOLUTIONS, childConfigs, sorter)

        // Act & Assert, should returns an empty list.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).isEmpty()
    }

    @Test
    fun getMergedResolutions_whenChildHasAndAllowHighResolutions_canReturnCorrectly() {
        // Arrange.
        val sensorSize = Size(4000, 3000) // 4:3
        val selector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .build()
        val config = FakeUseCaseConfig.Builder().setResolutionSelector(selector).useCaseConfig
        val childConfigs = setOf(config)
        val candidateChildSizes = listOf(Size(4000, 3000))
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger =
            ResolutionsMerger(sensorSize, CAMERA_INFO_WITH_HIGH_RESOLUTIONS, childConfigs, sorter)

        // Act & Assert, should returns the only selected high resolution.
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).containsExactly(Size(4000, 3000))
    }

    @Test
    fun getMergedResolutions_whenChildHasResolutionNeverBeSelected_canReturnCorrectly() {
        // Arrange.
        val sensorSize = SIZE_3264_2448 // 4:3
        val config1 = createUseCaseConfig()
        val config2 = createUseCaseConfig()
        val childConfigs = setOf(config1, config2)
        val candidateChildSizes1 = listOf(SIZE_1920_1080) // 16:9
        val candidateChildSizes2 = listOf(SIZE_1920_1080, SIZE_960_540, SIZE_3840_2160) // 16:9
        val sorter =
            FakeSupportedOutputSizesSorter(
                mapOf(config1 to candidateChildSizes1, config2 to candidateChildSizes2)
            )
        val merger = ResolutionsMerger(sensorSize, CAMERA_INFO, childConfigs, sorter)

        // Act & Assert, should returns a list of 16:9 resolutions and not consider child resolution
        // that will never be selected (3840x2160).
        val parentConfig = MutableOptionsBundle.create()
        assertThat(merger.getMergedResolutions(parentConfig)).containsExactly(SIZE_1920_1080)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPreferredChildSizePair_whenConfigNotPassedToConstructor_throwsException() {
        // Arrange.
        val config = createUseCaseConfig()
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to SIZES_16_9))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act.
        val useCaseConfigNotPassed = createUseCaseConfig()
        merger.getPreferredChildSizePair(useCaseConfigNotPassed, SIZE_1920_1440.toRect(), 0, false)
    }

    @Test
    fun getPreferredChildSizePair_whenViewportIsNotSet_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes =
            listOf(
                // 4:3
                SIZE_2560_1920,
                SIZE_1280_960,
                SIZE_640_480,
                // 16:9
                SIZE_1920_1080,
                SIZE_960_540
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns the first child size that do not need upscale and cause
        // double-cropping.
        merger
            .getPreferredChildSizePair(config, SIZE_2560_1440.toRect(), 0, false)
            .containsExactly(SIZE_2560_1440.toRect(), SIZE_1920_1080)
        merger
            .getPreferredChildSizePair(config, SIZE_1280_720.toRect(), 0, false)
            .containsExactly(SIZE_1280_720.toRect(), SIZE_960_540)

        // Act & Assert, should returns parent size when no matching.
        merger
            .getPreferredChildSizePair(config, SIZE_192_108.toRect(), 0, false)
            .containsExactly(SIZE_192_108.toRect(), SIZE_192_108)
    }

    @Test
    fun getPreferredChildSizePair_whenViewportIsSet_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes =
            listOf(
                // 16:9
                SIZE_1920_1080,
                SIZE_960_540
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns 1:1 crop rect and size, that are generated from the first
        // child size that do not need upscale.
        val rect1440To1440 = SIZE_2560_1920.crop(Size(1440, 1440))
        merger
            .getPreferredChildSizePair(config, rect1440To1440, 0, true)
            .containsExactly(rect1440To1440, Size(1080, 1080))
        val rect720To720 = SIZE_1280_720.crop(Size(720, 720))
        merger
            .getPreferredChildSizePair(config, rect720To720, 0, true)
            .containsExactly(rect720To720, Size(540, 540))

        // Act & Assert, should returns crop rect and size, that are generated from parent size
        // when no matching.
        val size108To108 = Size(108, 108)
        val rect108To108 = SIZE_192_108.crop(size108To108)
        merger
            .getPreferredChildSizePair(config, rect108To108, 0, true)
            .containsExactly(rect108To108, size108To108)
    }

    @Test
    fun getPreferredChildSizePair_whenViewportIsSetAndRotationIs90_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes =
            listOf(
                // 16:9
                SIZE_1920_1080,
                SIZE_960_540
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns 1:2 crop rect and size, that are generated from the first
        // child size that do not need upscale.
        val rect1280To2560 = SIZE_2560_1440.crop(Size(2560, 1280)).reverse()
        merger
            .getPreferredChildSizePair(config, rect1280To2560, 90, true)
            .containsExactly(rect1280To2560, Size(960, 1920))
        val rect640To1280 = SIZE_1280_720.crop(Size(1280, 640)).reverse()
        merger
            .getPreferredChildSizePair(config, rect640To1280, 90, true)
            .containsExactly(rect640To1280, Size(480, 960))

        // Act & Assert, should returns crop rect and size, that are generated from parent size
        // when no matching.
        val rect96To192 = SIZE_192_108.crop(Size(192, 96)).reverse()
        merger
            .getPreferredChildSizePair(config, rect96To192, 90, true)
            .containsExactly(rect96To192, rectToSize(rect96To192))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPreferredChildSize_whenConfigNotPassedToConstructor_throwsException() {
        // Arrange.
        val config = createUseCaseConfig()
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to SIZES_16_9))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

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
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

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
        val candidateChildSizes =
            listOf(
                // 4:3
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                // 16:9
                SIZE_1920_1080,
                SIZE_960_540,
                // 3:2
                SIZE_720_480
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns the first child size that do not need upscale and cause
        // double-cropping.
        assertThat(merger.getPreferredChildSize(SIZE_2560_1440, config)).isEqualTo(SIZE_1920_1080)
        assertThat(merger.getPreferredChildSize(SIZE_1280_720, config)).isEqualTo(SIZE_960_540)
        assertThat(merger.getPreferredChildSize(SIZE_720_480, config)).isEqualTo(SIZE_720_480)

        // Act & Assert, should returns parent size when no matching.
        assertThat(merger.getPreferredChildSize(SIZE_192_108, config)).isEqualTo(SIZE_192_108)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPreferredChildSizeForViewPort_whenConfigNotPassedToConstructor_throwsException() {
        // Arrange.
        val config = createUseCaseConfig()
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to SIZES_16_9))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act.
        val useCaseConfigNotPassedToConstructor = createUseCaseConfig()
        merger.getPreferredChildSizeForViewport(SIZE_1920_1440, useCaseConfigNotPassedToConstructor)
    }

    @Test
    fun getPreferredChildSize_whenViewportHasSameAspectRatio_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes =
            listOf(
                // 4:3
                SIZE_1920_1440,
                SIZE_960_720,
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns the first child size that can be cropped to parent
        // aspect-ratio and do not cause upscaling.
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_2560_1920, config))
            .isEqualTo(SIZE_1920_1440)
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_1280_960, config))
            .isEqualTo(SIZE_960_720)

        // Act & Assert, should returns parent size when no matching.
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_640_480, config))
            .isEqualTo(SIZE_640_480)
    }

    @Test
    fun getPreferredChildSize_whenViewportHasDifferentAspectRatio_canReturnCorrectly() {
        // Arrange.
        val config = createUseCaseConfig()
        val candidateChildSizes =
            listOf(
                // 16:9
                SIZE_1920_1080,
                SIZE_1280_720
            )
        val sorter = FakeSupportedOutputSizesSorter(mapOf(config to candidateChildSizes))
        val merger = ResolutionsMerger(SENSOR_SIZE, CAMERA_INFO, setOf(config), sorter)

        // Act & Assert, should returns the first child size that can be cropped to parent
        // aspect-ratio and do not cause upscaling.
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_1920_1440, config))
            .isEqualTo(Size(1440, 1080))
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_1280_960, config))
            .isEqualTo(SIZE_960_720)

        // Act & Assert, should returns parent size when no matching.
        assertThat(merger.getPreferredChildSizeForViewport(SIZE_640_480, config))
            .isEqualTo(SIZE_640_480)
    }

    @Test
    fun getCropRect_whenSameAspectRatio_noCropping() {
        val cropRect = getCropRectOfReferenceAspectRatio(SIZE_2560_1920, SIZE_1280_960)
        assertThat(cropRect.width()).isEqualTo(2560)
        assertThat(cropRect.height()).isEqualTo(1920)
        assertThat(cropRect.centerX()).isEqualTo(2560 / 2)
        assertThat(cropRect.centerY()).isEqualTo(1920 / 2)
    }

    @Test
    fun getCropRect_whenParentIs_4_3_canCropParentToChildAspectRatio() {
        val cropRect = getCropRectOfReferenceAspectRatio(SIZE_2560_1920, SIZE_1920_1080)
        assertThat(cropRect.width()).isEqualTo(2560)
        assertThat(cropRect.hasMatchingAspectRatio(SIZE_1920_1080)).isTrue()
        assertThat(cropRect.centerX()).isEqualTo(2560 / 2)
        assertThat(cropRect.centerY()).isEqualTo(1920 / 2)
    }

    @Test
    fun getCropRect_whenParentIs_16_9_canCropParentToChildAspectRatio() {
        val cropRect = getCropRectOfReferenceAspectRatio(SIZE_2560_1440, SIZE_1280_960)
        assertThat(cropRect.height()).isEqualTo(1440)
        assertThat(cropRect.hasMatchingAspectRatio(SIZE_1280_960)).isTrue()
        assertThat(cropRect.centerX()).isEqualTo(2560 / 2)
        assertThat(cropRect.centerY()).isEqualTo(1440 / 2)
    }

    @Test
    fun filterResolutionsByAspectRatio_canFilter_4_3() {
        val sizes = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        assertThat(filterResolutionsByAspectRatio(ASPECT_RATIO_4_3, sizes))
            .containsExactly(*SIZES_4_3.toTypedArray())
            .inOrder()
    }

    @Test
    fun filterResolutionsByAspectRatio_canFilter_16_9() {
        val sizes = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        assertThat(filterResolutionsByAspectRatio(ASPECT_RATIO_16_9, sizes))
            .containsExactly(*SIZES_16_9.toTypedArray())
            .inOrder()
    }

    @Test
    fun filterOutParentSizeThatIsTooSmall_canFilterOutTooSmallSizes() {
        val parentSizes =
            listOf(
                SIZE_3264_2448,
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_640_480,
                SIZE_320_240
            )
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, parentSizes))
            .containsExactly(
                SIZE_3264_2448,
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
            )
            .inOrder()
    }

    @Test
    fun filterOutParentSizeThatIsTooSmall_whenOnlyOneParentSize_canFilterOutTooSmallSize() {
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, listOf(SIZE_960_720)))
            .containsExactly(SIZE_960_720)
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, listOf(SIZE_640_480))).isEmpty()
    }

    @Test
    fun filterOutParentSizeThatIsTooSmall_whenNoParentSize_returnEmptyList() {
        val parentSizes = emptyList<Size>()
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, parentSizes)).isEmpty()
    }

    @Test
    fun filterOutParentSizeThatIsTooSmall_whenNoChildSize_returnEmptyList() {
        val parentSizes =
            listOf(
                SIZE_3264_2448,
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_640_480,
                SIZE_320_240
            )
        val childSizes = emptySet<Size>()
        assertThat(filterOutParentSizeThatIsTooSmall(childSizes, parentSizes)).isEmpty()
    }

    @Test
    fun getParentSizesThatAreTooLarge_canReturnTooLargeSizes() {
        val parentSizes =
            listOf(
                SIZE_3264_2448,
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_640_480,
                SIZE_320_240
            )
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(getParentSizesThatAreTooLarge(childSizes, parentSizes))
            .containsExactly(SIZE_3264_2448, SIZE_2560_1920)
            .inOrder()
    }

    @Test
    fun getParentSizesThatAreTooLarge_whenOnlyOneParentSize_alwaysReturnEmptyList() {
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_2560_1920))).isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_1920_1440))).isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_1280_960))).isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_640_480))).isEmpty()
    }

    @Test
    fun getParentSizesThatAreTooLarge_whenTwoParentSizes_canReturnTooLargeSize() {
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(
                getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_2560_1920, SIZE_1920_1440))
            )
            .containsExactly(SIZE_2560_1920)
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_1920_1440, SIZE_1280_960)))
            .isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_1280_960, SIZE_960_720)))
            .isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_960_720, SIZE_640_480)))
            .isEmpty()
        assertThat(getParentSizesThatAreTooLarge(childSizes, listOf(SIZE_640_480, SIZE_320_240)))
            .isEmpty()
    }

    @Test
    fun getParentSizesThatAreTooLarge_whenNoParentSize_returnEmptyList() {
        val parentSizes = emptyList<Size>()
        val childSizes = setOf(SIZE_1920_1080, SIZE_1280_720, SIZE_960_540)
        assertThat(getParentSizesThatAreTooLarge(childSizes, parentSizes)).isEmpty()
    }

    @Test
    fun getParentSizesThatAreTooLarge_whenNoChildSize_returnEmptyList() {
        val parentSizes =
            listOf(
                SIZE_3264_2448,
                SIZE_2560_1920,
                SIZE_1920_1440,
                SIZE_1280_960,
                SIZE_960_720,
                SIZE_640_480,
                SIZE_320_240
            )
        val childSizes = emptySet<Size>()
        assertThat(getParentSizesThatAreTooLarge(childSizes, parentSizes)).isEmpty()
    }

    @Test
    fun getParentSizesThatAreTooLarge_containsDuplicateParentSize() {
        val parentSizes =
            listOf(
                SIZE_1920_1440,
                SIZE_1920_1440, // duplicate
                SIZE_1280_960,
            )
        val childSizes = setOf(SIZE_1920_1080)
        assertThat(getParentSizesThatAreTooLarge(childSizes, parentSizes)).isEmpty()
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

    private fun android.util.Pair<Rect, Size>.containsExactly(rect: Rect, size: Size) {
        assertThat(first).isEqualTo(rect)
        assertThat(second).isEqualTo(size)
    }

    private fun Rect.hasMatchingAspectRatio(resolution: Size): Boolean {
        return AspectRatioUtil.hasMatchingAspectRatio(resolution, Rational(width(), height()))
    }

    private fun Rect.reverse(): Rect {
        return reverseRect(this)
    }

    private fun Size.crop(referenceSize: Size): Rect {
        return getCropRectOfReferenceAspectRatio(this, referenceSize)
    }

    private fun Size.toRect(): Rect {
        return sizeToRect(this)
    }

    /** A fake implementation of [SupportedOutputSizesSorter] for testing. */
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
        private val SIZES_4_3 =
            listOf(
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
        private val SIZES_16_9 =
            listOf(
                SIZE_3840_2160,
                SIZE_2560_1440,
                SIZE_1920_1080,
                SIZE_1280_720,
                SIZE_960_540,
                SIZE_192_108
            )

        // Other aspect-ratio resolutions.
        private val SIZE_3840_2000 = Size(3840, 2000)
        private val SIZE_3840_1920 = Size(3840, 1920)
        private val SIZE_2560_1280 = Size(2560, 1280)
        private val SIZE_1440_720 = Size(1440, 720)
        private val SIZE_800_800 = Size(800, 800)
        private val SIZE_720_720 = Size(720, 720)
        private val SIZE_720_480 = Size(720, 480)
        private val SIZE_500_400 = Size(500, 400)
        private val SIZE_176_144 = Size(176, 144)
        private val SIZES_OTHER_ASPECT_RATIO =
            listOf(
                SIZE_3840_2000,
                SIZE_3840_1920,
                SIZE_2560_1280,
                SIZE_1440_720,
                SIZE_800_800,
                SIZE_720_720,
                SIZE_720_480,
                SIZE_500_400,
                SIZE_176_144
            )
        private val CAMERA_SUPPORTED_SIZES = SIZES_4_3 + SIZES_16_9 + SIZES_OTHER_ASPECT_RATIO
        private val CAMERA_INFO = createCameraInfo(supportedResolutions = CAMERA_SUPPORTED_SIZES)
        private val CAMERA_INFO_WITH_HIGH_RESOLUTIONS =
            createCameraInfo(
                supportedResolutions =
                    listOf(
                        Size(2560, 1440),
                        Size(1920, 1080),
                        Size(1440, 1080),
                        Size(1280, 960),
                        Size(1088, 1088),
                    ),
                supportedHighResolutions =
                    listOf(
                        Size(4000, 3000),
                        Size(4000, 2250),
                        Size(2992, 2992),
                        Size(4000, 1800),
                        Size(2560, 1920),
                    )
            )
        private val SENSOR_SIZE = SIZE_3264_2448 // 4:3

        private fun createCameraInfo(
            supportedResolutions: List<Size>? = null,
            supportedHighResolutions: List<Size>? = null
        ): FakeCameraInfoInternal {
            val cameraInfo = FakeCameraInfoInternal()
            supportedResolutions?.let {
                cameraInfo.setSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, it)
            }
            supportedHighResolutions?.let {
                cameraInfo.setSupportedHighResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, it)
            }

            return cameraInfo
        }
    }
}
