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
import android.util.Pair
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.core.util.Predicate
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.Collections.unmodifiableList
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val TARGET_ASPECT_RATIO_NONE = -99
private val DEFAULT_SUPPORTED_SIZES =
    listOf(
        Size(4032, 3024), // 4:3
        Size(3840, 2160), // 16:9
        Size(1920, 1440), // 4:3
        Size(1920, 1080), // 16:9
        Size(1280, 960), // 4:3
        Size(1280, 720), // 16:9
        Size(960, 960), // 1:1
        Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
        Size(800, 450), // 16:9
        Size(640, 480), // 4:3
        Size(320, 240), // 4:3
        Size(320, 180), // 16:9
        Size(256, 144), // 16:9
    )
private val HIGH_RESOLUTION_SUPPORTED_SIZES = listOf(Size(8000, 6000), Size(8000, 4500))
private val CUSTOM_SUPPORTED_SIZES = listOf(Size(1920, 1080), Size(720, 480), Size(640, 480))
private val VALIDATOR_UP_TO_2000 =
    Predicate<Size> { size ->
        size.width <= 2000 && size.height <= 2000
    }
private val PORTRAIT_SUPPORTED_SIZES =
    listOf(
        Size(1440, 1920),
        Size(1080, 1920),
        Size(1080, 1440),
        Size(960, 1280),
        Size(720, 1280),
        Size(960, 540),
        Size(480, 640),
        Size(640, 480),
        Size(360, 480),
    )
private val LANDSCAPE_ACTIVE_ARRAY_SIZE = Size(4032, 3024)
private val PORTRAIT_ACTIVE_ARRAY_SIZE = Size(1440, 1920)

/** Unit tests for [SupportedOutputSizesSorter]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SupportedOutputSizesSorterTest {
    private val cameraInfoInternal =
        FakeCameraInfoInternal().apply {
            setSupportedResolutions(ImageFormat.JPEG, DEFAULT_SUPPORTED_SIZES)
            setSupportedHighResolutions(ImageFormat.JPEG, HIGH_RESOLUTION_SUPPORTED_SIZES)
        }
    private val supportedOutputSizesSorter =
        SupportedOutputSizesSorter(cameraInfoInternal, LANDSCAPE_ACTIVE_ARRAY_SIZE)

    @Test
    fun canSelectCustomOrderedResolutions() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val cameraInfoInternal =
            FakeCameraInfoInternal().apply {
                setSupportedResolutions(imageFormat, DEFAULT_SUPPORTED_SIZES)
            }
        val supportedOutputSizesSorter =
            SupportedOutputSizesSorter(cameraInfoInternal, LANDSCAPE_ACTIVE_ARRAY_SIZE)
        // Sets up the custom ordered resolutions
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.IMAGE_CAPTURE, imageFormat)
                .apply { setCustomOrderedResolutions(CUSTOM_SUPPORTED_SIZES) }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert
        assertThat(sortedResult).containsExactlyElementsIn(CUSTOM_SUPPORTED_SIZES).inOrder()
    }

    @Test
    fun canPrioritizeSupportedResolutions_withCameraEffect() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val fakeEffect =
            object :
                CameraEffect(
                    PREVIEW,
                    CameraXExecutors.mainThreadExecutor(),
                    FakeSurfaceProcessorInternal(CameraXExecutors.mainThreadExecutor()),
                    {},
                ) {
                // Only sizes with width <= 2000 and height <= 2000 are supported
                override fun getResolutionValidator(): Predicate<Size> = VALIDATOR_UP_TO_2000
            }

        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.PREVIEW, imageFormat)
                .apply {
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_EFFECT_RESOLUTION_VALIDATOR,
                        fakeEffect.resolutionValidator,
                    )
                }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert: legacy path without target aspect ratio prioritizes all sizes with
        // width <= 2000 && height <= 2000 ahead of larger unsupported sizes.
        val firstUnsupportedIndex = sortedResult.indexOfFirst {
            it.width > 2000 || it.height > 2000
        }
        val lastSupportedIndex = sortedResult.indexOfLast { it.width <= 2000 && it.height <= 2000 }

        assertThat(firstUnsupportedIndex).isGreaterThan(lastSupportedIndex)
        assertThat(sortedResult.first().width).isAtMost(2000)
    }

    @Test
    fun canPrioritizeSupportedResolutionsWithinEachAspectRatioGroup_withCameraEffect() {
        // Arrange: ResolutionSelector defaults to 4:3 preferred aspect ratio with AUTO fallback.
        // Validator only supports sizes <= 2000x2000 (so 4032x3024 in 4:3 and 3840x2160 in 16:9
        // are unsupported).
        val validator = VALIDATOR_UP_TO_2000
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.PREVIEW, ImageFormat.JPEG)
                .apply {
                    setResolutionSelector(ResolutionSelector.Builder().build())
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_EFFECT_RESOLUTION_VALIDATOR,
                        validator,
                    )
                }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert:
        // 1. All 4:3 sizes (both supported and unsupported) stay ahead of 16:9 sizes, preserving
        //    the preferred aspect ratio group order.
        // 2. Within the 4:3 group, supported sizes (1920x1440, 1280x960, 640x480, 320x240) precede
        //    the unsupported 4:3 size (4032x3024).
        // 3. Within the 16:9 group, supported sizes (1920x1080, ...) precede 3840x2160.
        assertThat(sortedResult)
            .containsExactly(
                // 4:3 group: supported first, then unsupported
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(320, 240),
                Size(4032, 3024),
                // 1:1 group (FOV closer to 4:3 than 16:9): supported
                Size(960, 960),
                // 16:9 group: supported first, then unsupported
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(320, 180),
                Size(256, 144),
                Size(3840, 2160),
            )
            .inOrder()
    }

    @Test
    fun canPrioritizeSupportedResolutionsWithinEachAspectRatioGroup_legacyApiWithCameraEffect() {
        // Arrange: Legacy API with targetAspectRatio = RATIO_4_3 (resolutionSelector == null).
        // Validator only supports sizes <= 2000x2000.
        val validator = VALIDATOR_UP_TO_2000
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.PREVIEW, ImageFormat.JPEG)
                .apply {
                    setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_EFFECT_RESOLUTION_VALIDATOR,
                        validator,
                    )
                }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert: 4:3 group stays ahead of 1:1 and 16:9 groups, and within each group supported
        // sizes precede unsupported sizes (>= 640x480 min size bound in legacy API).
        assertThat(sortedResult)
            .containsExactly(
                // 4:3 group: supported first, then unsupported
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(4032, 3024),
                // 1:1 group: supported
                Size(960, 960),
                // 16:9 group: supported first, then unsupported
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(3840, 2160),
            )
            .inOrder()
    }

    @Test
    fun resolutionFilter_notOverriddenByCameraEffectResolutionValidator() {
        // Arrange: Effect validator only supports sizes <= 2000x2000, but the user's
        // ResolutionFilter explicitly orders 4032x3024 (unsupported) ahead of 1920x1440.
        val validator = VALIDATOR_UP_TO_2000
        val explicitOrder = listOf(Size(4032, 3024), Size(1920, 1440), Size(1280, 720))
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionFilter { _, _ -> ArrayList(explicitOrder) }
                .build()
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.PREVIEW, ImageFormat.JPEG)
                .apply {
                    setResolutionSelector(resolutionSelector)
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_EFFECT_RESOLUTION_VALIDATOR,
                        validator,
                    )
                }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert: The explicit ordering returned by ResolutionFilter is preserved.
        assertThat(sortedResult).containsExactlyElementsIn(explicitOrder).inOrder()
    }

    @Test
    fun customOrderedResolutions_prioritizedByCameraEffectResolutionValidator() {
        // Arrange: custom ordered resolutions (e.g. from VideoCapture's QualitySelector) prefer
        // 3840x2160, which the effect doesn't support.
        val useCaseConfig =
            createCustomOrderedConfig(listOf(Size(3840, 2160), Size(1920, 1080), Size(1280, 720)))

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert: effect-supported sizes move ahead while keeping their relative order, and the
        // unsupported size is kept as a fallback.
        assertThat(sortedResult)
            .containsExactly(Size(1920, 1080), Size(1280, 720), Size(3840, 2160))
            .inOrder()
    }

    @Test
    fun customOrderedResolutions_onlyUnsupported_keepsOriginalOrder() {
        // Arrange: custom ordered resolutions contain only sizes the effect doesn't support.
        val customOrderedResolutions = listOf(Size(4032, 3024), Size(3840, 2160))
        val useCaseConfig = createCustomOrderedConfig(customOrderedResolutions)

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert
        assertThat(sortedResult).containsExactlyElementsIn(customOrderedResolutions).inOrder()
    }

    private fun createCustomOrderedConfig(customOrderedResolutions: List<Size>) =
        FakeUseCaseConfig.Builder(CaptureType.VIDEO_CAPTURE, ImageFormat.JPEG)
            .apply {
                setCustomOrderedResolutions(customOrderedResolutions)
                mutableConfig.insertOption(
                    UseCaseConfig.OPTION_EFFECT_RESOLUTION_VALIDATOR,
                    VALIDATOR_UP_TO_2000,
                )
            }
            .useCaseConfig

    @Test
    fun canSelectCustomSupportedResolutions() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val cameraInfoInternal =
            FakeCameraInfoInternal().apply {
                setSupportedResolutions(imageFormat, DEFAULT_SUPPORTED_SIZES)
            }
        val supportedOutputSizesSorter =
            SupportedOutputSizesSorter(cameraInfoInternal, LANDSCAPE_ACTIVE_ARRAY_SIZE)
        // Sets up the custom supported resolutions
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.IMAGE_CAPTURE, imageFormat)
                .apply {
                    setSupportedResolutions(
                        listOf(Pair.create(imageFormat, CUSTOM_SUPPORTED_SIZES.toTypedArray()))
                    )
                }
                .useCaseConfig

        // Act
        val sortedResult = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)

        // Assert
        assertThat(sortedResult).containsExactlyElementsIn(CUSTOM_SUPPORTED_SIZES).inOrder()
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio4x3_fallbackRuleNone() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_4_3,
            aspectRatioFallbackRule = AspectRatioStrategy.FALLBACK_RULE_NONE,
            expectedList =
                listOf(
                    // Only returns preferred AspectRatio matched items, sorted by area size.
                    Size(4032, 3024),
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio4x3OrDefault_fallbackRuleAuto() {
        // Using RATIO_DEFAULT should obtain the same result as RATIO_4_3
        listOf(AspectRatio.RATIO_4_3, AspectRatio.RATIO_DEFAULT).forEach {
            verifySupportedOutputSizesWithResolutionSelectorSettings(
                preferredAspectRatio = it,
                aspectRatioFallbackRule = AspectRatioStrategy.FALLBACK_RULE_AUTO,
                expectedList =
                    listOf(
                        // Matched preferred AspectRatio items, sorted by area size.
                        Size(4032, 3024), // 4:3
                        Size(1920, 1440),
                        Size(1280, 960),
                        Size(640, 480),
                        Size(320, 240),
                        // Mismatched preferred AspectRatio items, sorted by FOV and area size.
                        Size(960, 960), // 1:1
                        Size(3840, 2160), // 16:9
                        Size(1920, 1080),
                        Size(1280, 720),
                        Size(960, 544),
                        Size(800, 450),
                        Size(320, 180),
                        Size(256, 144),
                    ),
            )
        }
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_fallbackRuleNone() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            aspectRatioFallbackRule = AspectRatioStrategy.FALLBACK_RULE_NONE,
            expectedList =
                listOf(
                    // Only returns preferred AspectRatio matched items, sorted by area size.
                    Size(3840, 2160),
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_fallbackRuleAuto() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            aspectRatioFallbackRule = AspectRatioStrategy.FALLBACK_RULE_AUTO,
            expectedList =
                listOf(
                    // Matched preferred AspectRatio items, sorted by area size.
                    Size(3840, 2160), // 16:9
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                    // Mismatched preferred AspectRatio items, sorted by FOV and area size.
                    Size(4032, 3024), // 4:3
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    Size(960, 960), // 1:1
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withResolutionFallbackRuleNone() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            boundSize = Size(1280, 960),
            resolutionFallbackRule = ResolutionStrategy.FALLBACK_RULE_NONE,
            // Only returns preferred AspectRatio matched item.
            expectedList = listOf(Size(1280, 960)),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withBothAspectRatioResolutionFallbackRuleNone() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_4_3,
            aspectRatioFallbackRule = AspectRatioStrategy.FALLBACK_RULE_NONE,
            boundSize = Size(1920, 1080),
            resolutionFallbackRule = ResolutionStrategy.FALLBACK_RULE_NONE,
            // No size is returned since only 1920x1080 is allowed but it is not a 4:3 size
            expectedList = Collections.emptyList(),
        )
    }

    @Test
    fun getSupportedOutputSizes_withHighestAvailableResolutionStrategy() {
        // Bound size will be ignored when fallback rule is HIGHEST_AVAILABLE
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            resolutionStrategy = ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY,
            // The 4:3 default aspect ratio will make sizes of 4/3 have the highest priority
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(4032, 3024),
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(960, 960), // 1:1
                    Size(3840, 2160), // 16:9
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withClosestHigherThenLowerRule() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            boundSize = Size(1920, 1080),
            resolutionFallbackRule = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            // The 4:3 default aspect ratio will make sizes of 4/3 have the highest priority
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(1920, 1440), // 4:3 smallest larger size has highest priority
                    Size(4032, 3024), // the remaining 4:3 larger sizes
                    Size(1280, 960), // the remaining 4:3 smaller sizes
                    Size(640, 480),
                    Size(320, 240),
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(960, 960), // 1:1
                    Size(1920, 1080), // 16:9 smallest larger size
                    Size(3840, 2160), // the remaining 16:9 larger sizes
                    Size(1280, 720), // the remaining 16:9 smaller sizes
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withClosestHigherRule() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            boundSize = Size(1920, 1080),
            resolutionFallbackRule = FALLBACK_RULE_CLOSEST_HIGHER,
            // The 4:3 default aspect ratio will make sizes of 4/3 have the highest priority
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(1920, 1440), // 4:3 smallest larger size has highest priority
                    Size(4032, 3024), // the remaining 4:3 larger sizes
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(1920, 1080), // 16:9 smallest larger size
                    Size(3840, 2160), // the remaining 16:9 larger sizes
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withClosestLowerThenHigherRule() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            boundSize = Size(1920, 1080),
            resolutionFallbackRule = FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
            // The 4:3 default aspect ratio will make sizes of 4/3 have the highest priority
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(1280, 960), // 4:3 largest smaller size has highest priority
                    Size(640, 480), // the remaining 4:3 smaller sizes
                    Size(320, 240),
                    Size(1920, 1440), // the remaining 4:3 larger sizes
                    Size(4032, 3024),
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(960, 960), // 1:1
                    Size(1920, 1080), // 16:9 largest smaller size
                    Size(1280, 720), // the remaining 16:9 smaller sizes
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                    Size(3840, 2160), // the remaining 16:9 larger sizes
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_boundSize1920x1080_withClosestLowerRule() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            boundSize = Size(1920, 1080),
            resolutionFallbackRule = FALLBACK_RULE_CLOSEST_LOWER,
            // The 4:3 default aspect ratio will make sizes of 4/3 have the highest priority
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(1280, 960), // 4:3 largest smaller size has highest priority
                    Size(640, 480), // the remaining 4:3 smaller sizes
                    Size(320, 240),
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(960, 960), // 1:1
                    Size(1920, 1080), // 16:9 largest smaller size
                    Size(1280, 720), // the remaining 16:9 smaller sizes
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizesWithPortraitPixelArraySize_aspectRatio16x9() {
        val cameraInfoInternal =
            FakeCameraInfoInternal().apply {
                setSupportedResolutions(ImageFormat.JPEG, PORTRAIT_SUPPORTED_SIZES)
            }
        val supportedOutputSizesSorter =
            SupportedOutputSizesSorter(cameraInfoInternal, PORTRAIT_ACTIVE_ARRAY_SIZE)
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            outputSizesSorter = supportedOutputSizesSorter,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            expectedList =
                listOf(
                    // Matched preferred AspectRatio items, sorted by area size.
                    Size(1080, 1920),
                    Size(720, 1280),
                    // Mismatched preferred AspectRatio items, sorted by area size.
                    Size(1440, 1920),
                    Size(1080, 1440),
                    Size(960, 1280),
                    Size(480, 640),
                    Size(360, 480),
                    Size(640, 480),
                    Size(960, 540),
                ),
        )
    }

    @Test
    fun getSupportedOutputSizes_whenHighResolutionIsEnabled_aspectRatio16x9() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            allowedResolutionMode = PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE,
            expectedList =
                listOf(
                    // Matched preferred AspectRatio items, sorted by area size.
                    Size(8000, 4500), // 16:9 high resolution size
                    Size(3840, 2160),
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                    // Mismatched preferred AspectRatio items, sorted by area size.
                    Size(8000, 6000), // 4:3 high resolution size
                    Size(4032, 3024),
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    Size(960, 960), // 1:1
                ),
        )
    }

    @Test
    fun highResolutionCanNotBeSelected_whenHighResolutionForceDisabled() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            allowedResolutionMode = PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE,
            highResolutionForceDisabled = true,
            expectedList =
                listOf(
                    // Matched preferred AspectRatio items, sorted by area size.
                    Size(3840, 2160),
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                    // Mismatched preferred AspectRatio items, sorted by area size.
                    Size(4032, 3024), // 4:3
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    Size(960, 960), // 1:1
                ),
        )
    }

    @Test
    fun checkAllSupportedSizesCanBeSelected() {
        // For 4:3 and 16:9 sizes, sets each of supported size as bound size and also calculates
        // its ratio to set as preferred aspect ratio, so that the size can be put in the first
        // priority position. For sizes of other aspect ratio, creates a ResolutionFilter to select
        // it.
        DEFAULT_SUPPORTED_SIZES.forEach { supportedSize ->
            var resolutionFilter: ResolutionFilter? = null
            val preferredAspectRatio =
                if (
                    AspectRatioUtil.hasMatchingAspectRatio(
                        supportedSize,
                        AspectRatioUtil.ASPECT_RATIO_4_3,
                    )
                ) {
                    AspectRatio.RATIO_4_3
                } else if (
                    AspectRatioUtil.hasMatchingAspectRatio(
                        supportedSize,
                        AspectRatioUtil.ASPECT_RATIO_16_9,
                    )
                ) {
                    AspectRatio.RATIO_16_9
                } else {
                    // Sizes of special aspect ratios as 1:1 or 18:9 need to implement
                    // ResolutionFilter to select them.
                    resolutionFilter = ResolutionFilter { supportedSizes, _ ->
                        supportedSizes.remove(supportedSize)
                        supportedSizes.add(0, supportedSize)
                        supportedSizes
                    }
                    TARGET_ASPECT_RATIO_NONE
                }

            val useCaseConfig =
                createUseCaseConfig(
                    preferredAspectRatio = preferredAspectRatio,
                    boundSize = supportedSize,
                    resolutionFilter = resolutionFilter,
                )

            val resultList = supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)
            assertThat(resultList[0]).isEqualTo(supportedSize)
        }
    }

    @Test
    fun getSupportedOutputSizes_withResolutionFilter() {
        val filteredSizesList = arrayListOf(Size(1280, 720), Size(4032, 3024), Size(960, 960))
        val resolutionFilter = ResolutionFilter { _, _ -> filteredSizesList }
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            resolutionFilter = resolutionFilter,
            expectedList = filteredSizesList,
        )
    }

    @Test
    fun getSupportedOutputSizes_resolutionFilterReturnsAdditionalItem() {
        val resolutionFilter = ResolutionFilter { supportedSizes, _ ->
            supportedSizes.add(Size(1599, 899)) // Adds an additional size item
            supportedSizes
        }
        // Throws exception when additional items is added in the returned list
        assertThrows(IllegalArgumentException::class.java) {
            verifySupportedOutputSizesWithResolutionSelectorSettings(
                resolutionFilter = resolutionFilter
            )
        }
    }

    @Test
    fun getSortedSupportedOutputSizesReturns_whenCameraInfoProvidesUnmodifiableList() {
        // Arrange
        val imageFormat = ImageFormat.JPEG
        val cameraInfoInternal =
            FakeCameraInfoInternal().apply {
                setSupportedResolutions(imageFormat, unmodifiableList(DEFAULT_SUPPORTED_SIZES))
            }
        val supportedOutputSizesSorter =
            SupportedOutputSizesSorter(cameraInfoInternal, LANDSCAPE_ACTIVE_ARRAY_SIZE)
        // Sets up a no-op useCaseConfig
        val useCaseConfig =
            FakeUseCaseConfig.Builder(CaptureType.IMAGE_CAPTURE, imageFormat).useCaseConfig

        // Act & Assert
        supportedOutputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)
    }

    @Test
    fun canKeepFhdResolution_whenMaxResolutionHasShorterEdgeButLargerArea() {
        verifySupportedOutputSizesWithResolutionSelectorSettings(
            maxResolution = Size(2244, 1008),
            expectedList =
                listOf(
                    // Matched default preferred AspectRatio items, sorted by area size.
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    // Mismatched default preferred AspectRatio items, sorted by FOV and area size.
                    Size(960, 960), // 1:1
                    Size(1920, 1080), // 16:9, this can be kept even the max resolution
                    // setting has a shorter edge of 1008
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                ),
        )
    }

    private fun verifySupportedOutputSizesWithResolutionSelectorSettings(
        outputSizesSorter: SupportedOutputSizesSorter = supportedOutputSizesSorter,
        captureType: CaptureType = CaptureType.IMAGE_CAPTURE,
        preferredAspectRatio: Int = TARGET_ASPECT_RATIO_NONE,
        aspectRatioFallbackRule: Int = AspectRatioStrategy.FALLBACK_RULE_AUTO,
        resolutionStrategy: ResolutionStrategy? = null,
        boundSize: Size? = null,
        resolutionFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
        resolutionFilter: ResolutionFilter? = null,
        allowedResolutionMode: Int = ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION,
        highResolutionForceDisabled: Boolean = false,
        maxResolution: Size? = null,
        expectedList: List<Size> = Collections.emptyList(),
    ) {
        val useCaseConfig =
            createUseCaseConfig(
                captureType,
                preferredAspectRatio,
                aspectRatioFallbackRule,
                resolutionStrategy,
                boundSize,
                resolutionFallbackRule,
                resolutionFilter,
                allowedResolutionMode,
                highResolutionForceDisabled,
                maxResolution,
            )
        val resultList = outputSizesSorter.getSortedSupportedOutputSizes(useCaseConfig)
        assertThat(resultList).containsExactlyElementsIn(expectedList).inOrder()
    }

    private fun createUseCaseConfig(
        captureType: CaptureType = CaptureType.IMAGE_CAPTURE,
        preferredAspectRatio: Int = TARGET_ASPECT_RATIO_NONE,
        aspectRatioFallbackRule: Int = AspectRatioStrategy.FALLBACK_RULE_AUTO,
        resolutionStrategy: ResolutionStrategy? = null,
        boundSize: Size? = null,
        resolutionFallbackRule: Int = FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
        resolutionFilter: ResolutionFilter? = null,
        allowedResolutionMode: Int = ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION,
        highResolutionForceDisabled: Boolean = false,
        maxResolution: Size? = null,
    ): UseCaseConfig<*> {
        val useCaseConfigBuilder = FakeUseCaseConfig.Builder(captureType, ImageFormat.JPEG)
        val resolutionSelectorBuilder = ResolutionSelector.Builder()

        // Creates aspect ratio strategy and sets to resolution selector
        val aspectRatioStrategy =
            if (preferredAspectRatio != TARGET_ASPECT_RATIO_NONE) {
                AspectRatioStrategy(preferredAspectRatio, aspectRatioFallbackRule)
            } else {
                null
            }

        aspectRatioStrategy?.let { resolutionSelectorBuilder.setAspectRatioStrategy(it) }

        // Creates resolution strategy and sets to resolution selector
        if (resolutionStrategy != null) {
            resolutionSelectorBuilder.setResolutionStrategy(resolutionStrategy)
        } else {
            boundSize?.let {
                resolutionSelectorBuilder.setResolutionStrategy(
                    ResolutionStrategy(boundSize, resolutionFallbackRule)
                )
            }
        }

        // Sets the resolution filter to resolution selector
        resolutionFilter?.let { resolutionSelectorBuilder.setResolutionFilter(it) }
        // Sets the high resolution enabled flags to resolution selector
        resolutionSelectorBuilder.setAllowedResolutionMode(allowedResolutionMode)

        // Sets the custom resolution selector to use case config
        useCaseConfigBuilder.setResolutionSelector(resolutionSelectorBuilder.build())

        // Sets the high resolution force disabled setting
        useCaseConfigBuilder.setHighResolutionDisabled(highResolutionForceDisabled)

        // Sets the max resolution setting
        maxResolution?.let { useCaseConfigBuilder.setMaxResolution(it) }

        return useCaseConfigBuilder.useCaseConfig
    }
}
