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

package androidx.camera.core.internal

import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

private const val TARGET_ASPECT_RATIO_NONE = -99
@RequiresApi(21)
private val DEFAULT_SUPPORTED_SIZES = listOf(
    Size(4032, 3024), // 4:3
    Size(3840, 2160), // 16:9
    Size(1920, 1440), // 4:3
    Size(1920, 1080), // 16:9
    Size(1280, 960), // 4:3
    Size(1280, 720), // 16:9
    Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
    Size(800, 450), // 16:9
    Size(640, 480), // 4:3
    Size(320, 240), // 4:3
    Size(320, 180), // 16:9
    Size(256, 144) // 16:9
)

/**
 * Unit tests for [SupportedOutputSizesSorterLegacy].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedOutputSizesSorterLegacyTest {
    private val cameraInfoInternal = FakeCameraInfoInternal()
    private val supportedOutputSizesSorterLegacy =
        SupportedOutputSizesSorterLegacy(cameraInfoInternal, AspectRatioUtil.ASPECT_RATIO_4_3)

    @Test
    fun checkFilterOutSmallSizesByDefaultSize640x480() {
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            createUseCaseConfig()
        )
        // Sizes smaller than 640x480 are filtered out by default
        assertThat(resultList).containsNoneIn(
            arrayOf(
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
    }

    @Test
    fun canPrioritize4x3SizesByTargetAspectRatio() {
        val useCaseConfig = createUseCaseConfig(targetAspectRatio = AspectRatio.RATIO_4_3)
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // 4:3 items are prioritized in the result list
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 4:3 items
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                // 16:9 items
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
            )
        ).inOrder()
    }

    @Test
    fun canPrioritize16x9SizesByTargetAspectRatio() {
        val useCaseConfig = createUseCaseConfig(targetAspectRatio = AspectRatio.RATIO_16_9)
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // 16:9 items are prioritized in the result list
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 16:9 items
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                // 4:3 items
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
            )
        ).inOrder()
    }

    @Test
    fun canPrioritize4x3SizesByTargetResolution() {
        val useCaseConfig = createUseCaseConfig(targetResolution = Size(1280, 960))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // 4:3 items are prioritized in the result list
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 4:3 items
                Size(1280, 960),
                Size(1920, 1440),
                Size(4032, 3024),
                Size(640, 480),
                // 16:9 items
                Size(1920, 1080),
                Size(3840, 2160),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
            )
        ).inOrder()
    }

    @Test
    fun canPrioritize16x9SizesByTargetResolution() {
        val useCaseConfig = createUseCaseConfig(targetResolution = Size(1920, 1080))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // 16:9 items are prioritized in the result list
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 16:9 items
                Size(1920, 1080),
                Size(3840, 2160),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                // 4:3 items
                Size(1920, 1440),
                Size(4032, 3024),
                Size(1280, 960),
                Size(640, 480),
            )
        ).inOrder()
    }

    @Test
    fun canSelectSmallSizesByTargetResolutionSmallerThan640x480() {
        val useCaseConfig = createUseCaseConfig(targetResolution = Size(320, 240))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // 16:9 items are prioritized in the result list
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 4:3 items
                Size(320, 240),
                Size(640, 480),
                Size(1280, 960),
                Size(1920, 1440),
                Size(4032, 3024),
                // 16:9 items
                Size(800, 450),
                Size(960, 544),
                Size(1280, 720),
                Size(1920, 1080),
                Size(3840, 2160),
            )
        ).inOrder()
    }

    @Test
    fun canPrioritizeSizesByResolutionOfUncommonAspectRatio() {
        val useCaseConfig = createUseCaseConfig(targetResolution = Size(1280, 640))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // Sets target resolution as 1280x640, all supported resolutions will be put into aspect
        // ratio not matched list. Then, 1280x720 will be the nearest matched one.
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 16:9 items
                Size(1280, 720),
                Size(1920, 1080),
                Size(3840, 2160),
                Size(960, 544),
                Size(800, 450),
                // 4:3 items
                Size(1280, 960),
                Size(1920, 1440),
                Size(4032, 3024),
                Size(640, 480),
            )
        ).inOrder()
    }

    @Test
    fun checkFilterOutLargeSizesByMaxResolution() {
        val useCaseConfig = createUseCaseConfig(maxResolution = Size(1920, 1080))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                Size(1920, 1080),
                Size(1280, 960),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(640, 480),
            )
        ).inOrder()
    }

    @Test
    fun canKeepSmallSizesBySmallMaxResolution() {
        val useCaseConfig = createUseCaseConfig(maxResolution = Size(320, 240))
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                Size(320, 240),
                Size(320, 180),
                Size(256, 144),
            )
        ).inOrder()
    }

    @Test
    fun canSelectCorrect4x3SizeByDefaultResolution() {
        val useCaseConfig = createUseCaseConfig(
            targetAspectRatio = AspectRatio.RATIO_4_3,
            defaultResolution = Size(640, 480)
        )
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // The default resolution 640x480 will be used as target resolution to filter out too-large
        // items in each aspect ratio group
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 4:3 items
                Size(640, 480),
                Size(1280, 960),
                Size(1920, 1440),
                Size(4032, 3024),
                // 16:9 items
                Size(960, 544),
                Size(1280, 720),
                Size(1920, 1080),
                Size(3840, 2160),
                Size(800, 450),
            )
        ).inOrder()
    }

    @Test
    fun canSelectCorrect16x9SizeByDefaultResolution() {
        val useCaseConfig = createUseCaseConfig(
            targetAspectRatio = AspectRatio.RATIO_16_9,
            defaultResolution = Size(640, 480)
        )
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // The default resolution 640x480 will be used as target resolution to filter out too-large
        // items in each aspect ratio group
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 16:9 items
                Size(960, 544),
                Size(1280, 720),
                Size(1920, 1080),
                Size(3840, 2160),
                Size(800, 450),
                // 4:3 items
                Size(640, 480),
                Size(1280, 960),
                Size(1920, 1440),
                Size(4032, 3024),
            )
        ).inOrder()
    }

    @Test
    fun targetResolutionCanOverrideDefaultResolution() {
        val useCaseConfig = createUseCaseConfig(
            targetResolution = Size(1920, 1080),
            defaultResolution = Size(640, 480)
        )
        val resultList = supportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
            DEFAULT_SUPPORTED_SIZES,
            useCaseConfig
        )
        // The default resolution 640x480 will be used as target resolution to filter out too-large
        // items in each aspect ratio group
        assertThat(resultList).containsExactlyElementsIn(
            arrayOf(
                // 16:9 items
                Size(1920, 1080),
                Size(3840, 2160),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                // 4:3 items
                Size(1920, 1440),
                Size(4032, 3024),
                Size(1280, 960),
                Size(640, 480),
            )
        ).inOrder()
    }

    @Test
    fun sortByCompareSizesByArea_canSortSizesCorrectly() {
        val sizes = arrayOfNulls<Size>(DEFAULT_SUPPORTED_SIZES.size)
        // Generates a unsorted array from mSupportedSizes.
        val centerIndex = DEFAULT_SUPPORTED_SIZES.size / 2
        // Puts 2nd half sizes in the front
        for (i in centerIndex until DEFAULT_SUPPORTED_SIZES.size) {
            sizes[i - centerIndex] = DEFAULT_SUPPORTED_SIZES[i]
        }
        // Puts 1st half sizes inversely in the tail
        for (j in centerIndex - 1 downTo 0) {
            sizes[DEFAULT_SUPPORTED_SIZES.size - j - 1] = DEFAULT_SUPPORTED_SIZES[j]
        }
        // The testing sizes array will be equal to mSupportedSizes after sorting.
        Arrays.sort(sizes, CompareSizesByArea(true))
        assertThat(sizes.toList()).containsExactlyElementsIn(DEFAULT_SUPPORTED_SIZES).inOrder()
    }

    private fun createUseCaseConfig(
        captureType: CaptureType = CaptureType.IMAGE_CAPTURE,
        targetRotation: Int = Surface.ROTATION_0,
        targetAspectRatio: Int = TARGET_ASPECT_RATIO_NONE,
        targetResolution: Size? = null,
        maxResolution: Size? = null,
        defaultResolution: Size? = null,
    ): UseCaseConfig<*> {
        val builder = FakeUseCaseConfig.Builder(captureType, ImageFormat.JPEG)
        builder.setTargetRotation(targetRotation)
        if (targetAspectRatio != TARGET_ASPECT_RATIO_NONE) {
            builder.setTargetAspectRatio(targetAspectRatio)
        }
        targetResolution?.let { builder.setTargetResolution(it) }
        maxResolution?.let { builder.setMaxResolution(it) }
        defaultResolution?.let { builder.setDefaultResolution(it) }
        return builder.useCaseConfig
    }
}
