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

import androidx.ink.brush.BrushFamily.InputModel
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.behavior.BinaryOpNode
import androidx.ink.brush.behavior.BinaryOpNode.BinaryOp
import androidx.ink.brush.behavior.ConstantNode
import androidx.ink.brush.behavior.DampingNode
import androidx.ink.brush.behavior.EasingFunction
import androidx.ink.brush.behavior.IntegralNode
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.ResponseNode
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.brush.behavior.ToolTypeFilterNode
import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
        assertThat(InputModel.DEFAULT_INPUT_MODEL.hashCode())
            .isEqualTo(InputModel.DEFAULT_INPUT_MODEL.hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushFamily =
            BrushFamily(
                customTip,
                customPaint,
                inputModel =
                    InputModel.SlidingWindowModel(
                        windowDurationMillis = 250,
                        upsamplingFrequencyHz = 1,
                    ),
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
                    inputModel =
                        InputModel.SlidingWindowModel(
                            windowDurationMillis = 250,
                            upsamplingFrequencyHz = 1,
                        ),
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
    fun toString_returnsExpectedValues() {
        assertThat(
                BrushFamily(
                        inputModel =
                            InputModel.SlidingWindowModel(
                                windowDurationMillis = 1000,
                                upsamplingFrequencyHz = 1,
                            )
                    )
                    .toString()
            )
            .isEqualTo(
                "BrushFamily(developerComment=, coats=[BrushCoat(tip=BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slantDegrees=0.0, pinch=0.0, rotationDegrees=0.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, behaviors=[]), " +
                    "paintPreferences=[BrushPaint(textureLayers=[], colorFunctions=[], " +
                    "selfOverlap=SelfOverlap.ANY)])], " +
                    "inputModel=SlidingWindowModel(windowDurationMillis=1000, upsamplingFrequencyHz=1), " +
                    "clientBrushFamilyId=)"
            )
    }

    @Test
    fun calculateMinimumRequiredVersion_returnsExpectedValue() {
        assertThat(BrushFamily().calculateMinimumRequiredVersion())
            .isEqualTo(Version.V0_JETPACK1_0_0)
    }

    @Test
    fun hasFallbacks_returnsFalseByDefault() {
        assertThat(BrushFamily().hasFallbacks).isFalse()
    }

    @Test
    fun calculateMinimumRequiredVersion_withIntegralNode_returnsOne() {
        val behavior =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1f,
                    targetModifierRangeEnd = 2f,
                    input =
                        IntegralNode(
                            integrateOver = ProgressDomain.TIME_IN_SECONDS,
                            integralValueRangeStart = 0f,
                            integralValueRangeEnd = 1f,
                            integralOutOfRangeBehavior = OutOfRange.CLAMP,
                            input = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f),
                        ),
                )
            )
        val family = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        assertThat(family.calculateMinimumRequiredVersion())
            .isEqualTo(Version.V1_JETPACK1_1_0_ALPHA01)
    }

    @Test
    fun calculateMinimumRequiredVersion_withBinaryOpMin_returnsOne() {
        val behavior =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1f,
                    targetModifierRangeEnd = 2f,
                    input =
                        BinaryOpNode(
                            operation = BinaryOp.MIN,
                            firstInput = ConstantNode(1f),
                            secondInput = ConstantNode(2f),
                        ),
                )
            )
        val family = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        assertThat(family.calculateMinimumRequiredVersion())
            .isEqualTo(Version.V1_JETPACK1_1_0_ALPHA01)
    }

    @Test
    fun calculateMinimumRequiredVersion_withBinaryOpSum_returnsZero() {
        val behavior =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1f,
                    targetModifierRangeEnd = 2f,
                    input =
                        BinaryOpNode(
                            operation = BinaryOp.SUM,
                            firstInput = ConstantNode(1f),
                            secondInput = ConstantNode(2f),
                        ),
                )
            )
        val family = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        assertThat(family.calculateMinimumRequiredVersion()).isEqualTo(Version.V0_JETPACK1_0_0)
    }

    @Test
    fun calculateMinimumRequiredVersion_withTimeSinceStrokeEnd_returnsOne() {
        val behavior =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1f,
                    targetModifierRangeEnd = 2f,
                    input =
                        SourceNode(
                            source = Source.TIME_SINCE_STROKE_END_IN_SECONDS,
                            sourceValueRangeStart = 0f,
                            sourceValueRangeEnd = 1f,
                        ),
                )
            )
        val family = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        assertThat(family.calculateMinimumRequiredVersion())
            .isEqualTo(Version.V1_JETPACK1_1_0_ALPHA01)
    }

    @Test
    fun inputModelToString_returnsExpectedValues() {
        assertThat(InputModel.PASSTHROUGH_MODEL.toString()).isEqualTo("PassthroughModel")
        assertThat(
                InputModel.SlidingWindowModel(
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
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isEqualTo(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
        assertThat(InputModel.PASSTHROUGH_MODEL).isEqualTo(InputModel.PASSTHROUGH_MODEL)

        assertThat(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 48,
                    upsamplingFrequencyHz = 150,
                )
            )
        assertThat(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 151,
                )
            )
        assertThat(
                InputModel.SlidingWindowModel(
                    windowDurationMillis = 47,
                    upsamplingFrequencyHz = 150,
                )
            )
            .isNotEqualTo(InputModel.PASSTHROUGH_MODEL)
        assertThat(InputModel.PASSTHROUGH_MODEL)
            .isNotEqualTo(
                InputModel.SlidingWindowModel(
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
            BrushFamily.builder()
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
            TargetNode(
                target = Target.HEIGHT_MULTIPLIER,
                targetModifierRangeStart = 1.1f,
                targetModifierRangeEnd = 1.7f,
                input =
                    DampingNode(
                        dampingSource = ProgressDomain.TIME_IN_SECONDS,
                        dampingGap = 0.001f,
                        input =
                            ResponseNode(
                                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                input =
                                    ToolTypeFilterNode(
                                        enabledToolTypes = setOf(InputToolType.STYLUS),
                                        input =
                                            SourceNode(
                                                source = Source.TILT_IN_RADIANS,
                                                sourceValueRangeStart = 0.2f,
                                                sourceValueRangeEnd = .8f,
                                                sourceOutOfRangeBehavior = OutOfRange.MIRROR,
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
                TextureLayer(
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
                    sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    origin = TextureLayer.Origin.STROKE_SPACE_ORIGIN,
                    mapping = TextureLayer.Mapping.TILING,
                ),
                TextureLayer(
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
                    sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    origin = TextureLayer.Origin.STROKE_SPACE_ORIGIN,
                    mapping = TextureLayer.Mapping.TILING,
                ),
            )
        )

    /** Brush Family with every field different from default values. */
    private fun newCustomBrushFamily(): BrushFamily =
        BrushFamily(customTip, customPaint, clientBrushFamilyId = customBrushFamilyId)
}
