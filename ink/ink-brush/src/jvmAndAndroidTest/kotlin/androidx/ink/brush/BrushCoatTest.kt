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
class BrushCoatTest {
    @Test
    fun constructor_withValidArguments_returnsABrushCoat() {
        assertThat(BrushCoat(customTip, customPaint)).isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushCoat() {
        assertThat(BrushCoat(BrushTip(), BrushPaint())).isNotNull()
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(newCustomBrushCoat().hashCode()).isEqualTo(newCustomBrushCoat().hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushCoat = BrushCoat(customTip, customPaint)
        val differentTip = BrushTip()
        val differentPaint = BrushPaint()

        // same values are equal.
        assertThat(brushCoat).isEqualTo(BrushCoat(customTip, customPaint))

        // different values are not equal.
        assertThat(brushCoat).isNotNull()
        assertThat(brushCoat).isNotEqualTo(Any())
        assertThat(brushCoat).isNotEqualTo(BrushCoat(differentTip, customPaint))
        assertThat(brushCoat).isNotEqualTo(BrushCoat(customTip, differentPaint))
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushCoat().toString())
            .isEqualTo(
                "BrushCoat(tip=BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slantDegrees=0.0, pinch=0.0, rotationDegrees=0.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, behaviors=[]), " +
                    "paintPreferences=[BrushPaint(textureLayers=[], colorFunctions=[], " +
                    "selfOverlap=SelfOverlap.ANY)])"
            )
    }

    @Test
    fun copy_whenSameContents_returnsSameInstance() {
        val customCoat = BrushCoat(customTip, customPaint)

        // A pure copy returns `this`.
        val copy = customCoat.copy()
        assertThat(copy).isSameInstanceAs(customCoat)
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val brushCoat = BrushCoat(customTip, customPaint)
        val differentTip = BrushTip()
        val differentPaint = BrushPaint()

        assertThat(brushCoat.copy(tip = differentTip))
            .isEqualTo(BrushCoat(differentTip, customPaint))
        assertThat(brushCoat.copy(paintPreferences = listOf(differentPaint)))
            .isEqualTo(BrushCoat(customTip, differentPaint))
    }

    @Test
    fun builder_createsExpectedBrushCoat() {
        val coat = BrushCoat.Builder().setTip(customTip).addPaintPreference(customPaint).build()
        assertThat(coat).isEqualTo(BrushCoat(customTip, customPaint))
    }

    /**
     * Creates an expected C++ BrushCoat with defaults and returns true if every property of the
     * Kotlin BrushCoat's JNI-created C++ counterpart is equivalent to the expected C++ BrushCoat.
     */
    @UsedByNative private external fun matchesDefaultCoat(brushCoatNativePointer: Long): Boolean

    /**
     * Creates an expected C++ BrushCoat with custom values and returns true if every property of
     * the Kotlin BrushCoat's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushCoat.
     */
    @UsedByNative
    private external fun matchesMultiBehaviorTipCoat(brushCoatNativePointer: Long): Boolean

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
            listOf<BrushBehavior>(customBehavior),
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
                    animationFrames = 6,
                    animationRows = 7,
                    animationColumns = 8,
                    animationDurationMillis = 9000,
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
                    animationFrames = 6,
                    animationRows = 7,
                    animationColumns = 8,
                    animationDurationMillis = 9000,
                    sizeUnit = BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    origin = BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    mapping = BrushPaint.TextureMapping.TILING,
                ),
            ),
            selfOverlap = SelfOverlap.ACCUMULATE,
        )

    /** Brush Coat with every field different from default values. */
    private fun newCustomBrushCoat(): BrushCoat = BrushCoat(customTip, customPaint)
}
