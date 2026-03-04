/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushFamilyTest {
    @Test
    fun constructor_withValidArguments_returnsABrushFamily() {
        assertThat(BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId))
            .isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushFamily() {
        assertThat(BrushFamily(BrushTip(), BrushPaint(), clientBrushFamilyId = "")).isNotNull()
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(newCustomBrushFamily().hashCode()).isEqualTo(newCustomBrushFamily().hashCode())
    }

    @Test
    fun inputModelHashCode_isSameForIdenticalModels() {
        assertThat(BrushFamily.SPRING_MODEL.hashCode())
            .isEqualTo(BrushFamily.SPRING_MODEL.hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushFamily =
            BrushFamily(
                customTip,
                customPaint,
                inputModel = BrushFamily.SPRING_MODEL,
                clientBrushFamilyId = customBrushFamilyId,
            )
        val differentCoat = BrushCoat(BrushTip(), BrushPaint())
        val differentId = "different"

        // same values are equal.
        assertThat(brushFamily)
            .isEqualTo(
                BrushFamily(
                    tip = customTip,
                    paint = customPaint,
                    inputModel = BrushFamily.SPRING_MODEL,
                    clientBrushFamilyId = customBrushFamilyId,
                )
            )

        // different values are not equal.
        assertThat(brushFamily).isNotNull()
        assertThat(brushFamily).isNotEqualTo(Any())
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(coat = differentCoat))
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(clientBrushFamilyId = differentId))
    }

    @Test
    fun inputModelEquals_comparesModels() {
        assertThat(BrushFamily.SPRING_MODEL).isEqualTo(BrushFamily.SPRING_MODEL)
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushFamily(inputModel = BrushFamily.SPRING_MODEL).toString())
            .isEqualTo(
                "BrushFamily(developerComment=, coats=[BrushCoat(tip=BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slantDegrees=0.0, pinch=0.0, rotationDegrees=0.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, behaviors=[]), " +
                    "paintPreferences=[BrushPaint(textureLayers=[], colorFunctions=[], " +
                    "selfOverlap=SelfOverlap.ANY)])], inputModel=SpringModel, clientBrushFamilyId=)"
            )
    }

    @Test
    fun inputModelToString_returnsExpectedValues() {
        assertThat(BrushFamily.SPRING_MODEL.toString()).isEqualTo("SpringModel")
        assertThat(BrushFamily.EXPERIMENTAL_NAIVE_MODEL.toString())
            .isEqualTo("ExperimentalNaiveModel")
        assertThat(
                BrushFamily.SlidingWindowModel(
                        windowDurationMillis = 47,
                        upsamplingFrequencyHz = 150,
                    )
                    .toString()
            )
            .isEqualTo("SlidingWindowModel(windowDurationMillis=47, upsamplingFrequencyHz=150)")
    }

    @Test
    fun inputModelEquals() {
        assertThat(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isEqualTo(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
        assertThat(BrushFamily.SPRING_MODEL).isEqualTo(BrushFamily.SPRING_MODEL)

        assertThat(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 48,
                    upsamplingFrequencyHz = 150,
                )
            )
        assertThat(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 151,
                )
            )
        assertThat(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(BrushFamily.SPRING_MODEL)
        assertThat(BrushFamily.SPRING_MODEL)
            .isNotEqualTo(
                BrushFamily.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
    }

    @Test
    fun copy_whenSameContents_returnsSameInstance() {
        val customFamily =
            BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId)

        // A pure copy returns `this`.
        val copy = customFamily.copy()
        assertThat(copy).isSameInstanceAs(customFamily)
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val brushFamily =
            BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId)
        val differentCoats = listOf(BrushCoat(BrushTip(), BrushPaint()))
        val differentId = "different"

        assertThat(brushFamily.copy(coats = differentCoats))
            .isEqualTo(BrushFamily(differentCoats, clientBrushFamilyId = customBrushFamilyId))
        assertThat(brushFamily.copy(clientBrushFamilyId = differentId))
            .isEqualTo(BrushFamily(customTip, customPaint, clientBrushFamilyId = differentId))
    }

    @Test
    fun builder_createsExpectedBrushFamily() {
        val family =
            BrushFamily.Builder()
                .setCoats(listOf(BrushCoat(customTip, customPaint)))
                .setClientBrushFamilyId(customBrushFamilyId)
                .build()
        assertThat(family)
            .isEqualTo(
                BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId)
            )
    }

    /**
     * Creates an expected C++ BrushFamily with defaults and returns true if every property of the
     * Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    @UsedByNative private external fun matchesDefaultFamily(brushFamilyNativePointer: Long): Boolean

    /**
     * Creates an expected C++ BrushFamily with custom values and returns true if every property of
     * the Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    @UsedByNative
    private external fun matchesMultiBehaviorTipFamily(brushFamilyNativePointer: Long): Boolean

    private val customBrushFamilyId = "inkpen"

    /** Brush behavior with every field different from default values. */
    private val customBehavior =
        BrushBehavior(
            BrushBehavior.TargetNode(
                target = BrushBehavior.Target.HEIGHT_MULTIPLIER,
                targetModifierRangeStart = 1.1f,
                targetModifierRangeEnd = 1.7f,
                input =
                    BrushBehavior.DampingNode(
                        dampingSource = BrushBehavior.ProgressDomain.TIME_IN_SECONDS,
                        dampingGap = 0.001f,
                        input =
                            BrushBehavior.ResponseNode(
                                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                input =
                                    BrushBehavior.ToolTypeFilterNode(
                                        enabledToolTypes = setOf(InputToolType.STYLUS),
                                        input =
                                            BrushBehavior.SourceNode(
                                                source = BrushBehavior.Source.TILT_IN_RADIANS,
                                                sourceValueRangeStart = 0.2f,
                                                sourceValueRangeEnd = .8f,
                                                sourceOutOfRangeBehavior =
                                                    BrushBehavior.OutOfRange.MIRROR,
                                            ),
                                    ),
                            ),
                    ),
            )
        )

    /** Brush tip with every field different from default values and non-empty behaviors. */
    private val customTip =
        BrushTip(
            scaleX = 0.1f,
            scaleY = 0.2f,
            cornerRounding = 0.3f,
            slantDegrees = 0.4f,
            pinch = 0.5f,
            rotationDegrees = 0.6f,
            particleGapDistanceScale = 0.8f,
            particleGapDurationMillis = 9L,
            behaviors = listOf(customBehavior),
        )

    /**
     * Brush Paint with every field different from default values, including non-empty texture
     * layers.
     */
    private val customPaint =
        BrushPaint(
            listOf(
                BrushPaint.TextureLayer(
                    clientTextureId = "test-one",
                    sizeX = 123.45F,
                    sizeY = 678.90F,
                    offsetX = 0.123f,
                    offsetY = 0.678f,
                    rotationDegrees = 0.1f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    sizeUnit = BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    origin = BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    mapping = BrushPaint.TextureMapping.TILING,
                ),
                BrushPaint.TextureLayer(
                    clientTextureId = "test-two",
                    sizeX = 256F,
                    sizeY = 256F,
                    offsetX = 0.456f,
                    offsetY = 0.567f,
                    rotationDegrees = 0.2f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    sizeUnit = BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    origin = BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    mapping = BrushPaint.TextureMapping.TILING,
                ),
            )
        )

    /** Brush Family with every field different from default values. */
    private fun newCustomBrushFamily(): BrushFamily =
        BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId)
}
