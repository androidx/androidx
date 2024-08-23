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

package androidx.ink.strokes

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.colorspace.ColorSpaces
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class StrokeTest {

    @Test
    fun constructor_withBrushAndInputs() {
        val brushIn = buildTestBrush()
        val inputsIn = makeTestInputs()
        val stroke = Stroke(brushIn, inputsIn)

        assertThat(stroke.brush).isSameInstanceAs(brushIn)
        assertThat(stroke.inputs).isSameInstanceAs(inputsIn)
    }

    @Test
    fun constructor_withBrushInputsAndShape() {
        val brushIn = buildTestBrush()
        val inputsIn = makeTestInputs()
        val originalStroke = Stroke(brushIn, inputsIn)

        val newStroke = Stroke(brushIn, inputsIn, originalStroke.shape)

        // Kotlin properties are the same
        assertThat(newStroke.brush).isSameInstanceAs(brushIn)
        assertThat(newStroke.inputs).isSameInstanceAs(inputsIn)
        assertThat(newStroke.shape).isSameInstanceAs(originalStroke.shape)

        // C++ Stroke is different
        assertThat(newStroke.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun constructor_withMismatchedBrushAndShape_throwsException() {
        // Create a [ModeledShape] with render group.
        val inputs = makeTestInputs()
        val shape = Stroke(buildTestBrush(), inputs).shape
        assertThat(shape.renderGroupCount).isEqualTo(1)

        // Create a brush with two brush coats.
        val coat = BrushCoat(BrushTip(), BrushPaint())
        val brush = Brush(BrushFamily(ImmutableList.of(coat, coat)), size = 10f, epsilon = 0.1f)

        // We should get an error, because the number of render groups doesn't match the number of
        // brush
        // coats.
        assertThrows(IllegalArgumentException::class.java) { Stroke(brush, inputs, shape) }
    }

    @Test
    fun copy_withSameBrush_returnsSameInstance() {
        val originalStroke = buildTestStroke()

        val actual = originalStroke.copy(originalStroke.brush)

        // A pure copy returns `this`.
        assertThat(actual).isSameInstanceAs(originalStroke)
    }

    @Test
    fun copy_withChangedBrushColor_createsCopyWithSameInputsAndShape() {
        val originalBrush = buildTestBrush()
        val colorChangedBrush =
            Brush.createWithColorLong(
                family = originalBrush.family,
                colorLong = Color(0.1f, 0.2f, 0.3f, 0.4f, ColorSpaces.DisplayP3).value.toLong(),
                size = originalBrush.size,
                epsilon = originalBrush.epsilon,
            )
        val inputs = makeTestInputs()
        val originalStroke = Stroke(originalBrush, inputs)

        val actual = originalStroke.copy(brush = colorChangedBrush)

        // The new stroke has the changed brush.
        assertThat(actual.brush).isSameInstanceAs(colorChangedBrush)
        // The new stroke has the same inputs and shape as original brush.
        assertThat(actual.inputs).isSameInstanceAs(inputs)
        assertThat(actual.shape).isSameInstanceAs(originalStroke.shape)

        // The new C++ Stroke is different from the original stroke.
        assertThat(actual.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun copy_withChangedBrushTip_createsCopyWithSameInputs() {
        val originalBrush = buildTestBrush()
        val tipChangedBrush =
            Brush.createWithColorLong(
                family =
                    BrushFamily(
                        coats =
                            // The preferred Kotlin API method, [toImmutableList], is only available
                            // in google3,
                            // but this class and method are targeted for Jetpack.
                            @Suppress("PreferKotlinApi")
                            ImmutableList.copyOf(
                                originalBrush.family.coats.map { coat ->
                                    BrushCoat(
                                        tips =
                                            // The preferred Kotlin API method, [toImmutableList],
                                            // is only available in
                                            // google3, but this class and method are targeted for
                                            // Jetpack.
                                            @Suppress("PreferKotlinApi")
                                            ImmutableList.copyOf(
                                                coat.tips.map { tip -> tip.copy(scaleX = 0.12345f) }
                                            ),
                                        paint = coat.paint,
                                    )
                                }
                            ),
                        uri = originalBrush.family.uri,
                    ),
                colorLong = originalBrush.colorLong,
                size = originalBrush.size,
                epsilon = originalBrush.epsilon,
            )
        val inputs = makeTestInputs()
        val originalStroke = Stroke(originalBrush, inputs)

        val actual = originalStroke.copy(brush = tipChangedBrush)

        // The new stroke has the original inputs and the changed brush.
        assertThat(actual.inputs).isSameInstanceAs(inputs)
        assertThat(actual.brush).isSameInstanceAs(tipChangedBrush)

        // The new stroke has a different shape than the original stroke.
        assertThat(actual.shape).isNotSameInstanceAs(originalStroke.shape)

        // The new C++ Stroke is different from the original stroke.
        assertThat(actual.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun copy_withChangedBrushPaint_createsCopyWithSameInputsAndShape() {
        val originalBrush = buildTestBrush()
        val paintChangedBrush =
            originalBrush.copy(
                family =
                    originalBrush.family.copy(
                        coats =
                            // The preferred Kotlin API method, [toImmutableList], is only available
                            // in google3,
                            // but this class and method are targeted for Jetpack.
                            @Suppress("PreferKotlinApi")
                            ImmutableList.copyOf(
                                originalBrush.family.coats.map { coat ->
                                    coat.copy(
                                        paint =
                                            BrushPaint(
                                                ImmutableList.of(
                                                    BrushPaint.TextureLayer(
                                                        colorTextureUri =
                                                            "ink://ink/texture:test-one",
                                                        sizeX = 123.45F,
                                                        sizeY = 678.90F,
                                                        offsetX = 0.1F,
                                                        offsetY = 0.2F,
                                                        sizeUnit =
                                                            BrushPaint.TextureSizeUnit
                                                                .STROKE_COORDINATES,
                                                        mapping = BrushPaint.TextureMapping.TILING,
                                                    ),
                                                    BrushPaint.TextureLayer(
                                                        colorTextureUri =
                                                            "ink://ink/texture:test-two",
                                                        sizeX = 256F,
                                                        sizeY = 256F,
                                                        offsetX = 0.1F,
                                                        offsetY = 0.2F,
                                                        sizeUnit =
                                                            BrushPaint.TextureSizeUnit
                                                                .STROKE_COORDINATES,
                                                        mapping = BrushPaint.TextureMapping.TILING,
                                                    ),
                                                )
                                            )
                                    )
                                }
                            )
                    )
            )
        val inputs = makeTestInputs()
        val originalStroke = Stroke(originalBrush, inputs)

        val actual = originalStroke.copy(brush = paintChangedBrush)

        // The new stroke has the changed brush.
        assertThat(actual.brush).isSameInstanceAs(paintChangedBrush)
        // The new stroke has the same inputs and shape as original brush.
        assertThat(actual.inputs).isSameInstanceAs(inputs)
        assertThat(actual.shape).isSameInstanceAs(originalStroke.shape)

        // The new C++ Stroke is different from the original stroke.
        assertThat(actual.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun copy_withChangedBrushSize_createsCopyWithSameInputs() {
        val originalBrush = buildTestBrush()
        val sizeChangedBrush = originalBrush.copy(size = 99f)
        val inputs = makeTestInputs()
        val originalStroke = Stroke(originalBrush, inputs)

        val actual = originalStroke.copy(brush = sizeChangedBrush)

        // The new stroke has the original inputs and the changed brush.
        assertThat(actual.inputs).isSameInstanceAs(inputs)
        assertThat(actual.brush).isSameInstanceAs(sizeChangedBrush)

        // The new stroke has a different shape than the original stroke.
        assertThat(actual.shape).isNotSameInstanceAs(originalStroke.shape)

        // The new C++ Stroke is different from the original stroke.
        assertThat(actual.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun copy_withChangedBrushEpsilon_createsCopyWithSameInputs() {
        val originalBrush = buildTestBrush()
        val epsilonChangedBrush = originalBrush.copy(epsilon = 0.99f)
        val inputs = makeTestInputs()
        val originalStroke = Stroke(originalBrush, inputs)

        val actual = originalStroke.copy(brush = epsilonChangedBrush)

        // The new stroke has the original inputs and the changed brush.
        assertThat(actual.inputs).isSameInstanceAs(inputs)
        assertThat(actual.brush).isSameInstanceAs(epsilonChangedBrush)

        // The new stroke has a different shape than the original stroke.
        assertThat(actual.shape).isNotSameInstanceAs(originalStroke.shape)

        // The new C++ Stroke is different from the original stroke.
        assertThat(actual.nativeAddress).isNotEqualTo(originalStroke.nativeAddress)
    }

    @Test
    fun toString_returnsAString() {
        val string = buildTestStroke().toString()

        // Not elaborate checks - this test mainly exists to ensure that toString doesn't crash.
        assertThat(string).contains("Stroke")
        assertThat(string).contains("brush")
        assertThat(string).contains("inputs")
        assertThat(string).contains("shape")
    }

    /**
     * Creates a brush for testing with:
     *
     * Family Uri ="//ink/brush-family:pencil", distinctly different from the default native brush
     * family.
     *
     * Color has nontrivial values for all channels and the color space.
     *
     * Size = 7f, an arbitrary value for testing.
     *
     * Epsilon = 0.0012345f, an arbitrary value for testing.
     */
    private fun buildTestBrush() =
        Brush.createWithColorLong(
            BrushFamily(uri = "//ink/brush-family:pencil"),
            Color(0.6f, 0.7f, 0.8f, 0.9f, ColorSpaces.DisplayP3).value.toLong(),
            7f,
            0.0012345f,
        )

    /**
     * Creates a stroke with:
     *
     * Brush = buildTestBrush()
     *
     * Inputs = [{10,3}, {20, 5}]
     *
     * StrokeShape generated from the inputs and brush.
     */
    private fun buildTestStroke(): Stroke {
        val batch = buildStrokeInputBatchFromPoints(floatArrayOf(10f, 3f, 20f, 5f)).asImmutable()
        return Stroke(buildTestBrush(), batch)
    }

    /**
     * Make checkmark shaped test input batch with three input points, scaling the x,y,t values by a
     * [factor] to create varied input batches across a test case.
     */
    private fun makeTestInputs(factor: Int = 1): ImmutableStrokeInputBatch =
        buildStrokeInputBatchFromPoints(
                floatArrayOf(
                    factor * 1f,
                    factor * 1f,
                    factor * 2f,
                    factor * 3f,
                    factor * 5f,
                    factor * 2f
                )
            )
            .asImmutable()
}
