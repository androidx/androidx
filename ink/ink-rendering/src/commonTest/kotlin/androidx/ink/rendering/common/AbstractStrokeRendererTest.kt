/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.rendering.test

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.ImmutableAffineTransform
import androidx.ink.geometry.MutableAffineTransform
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Abstract parameterized rendering test base class.
 *
 * Concrete subclasses on each platform provide renderer-specific implementations of the following:
 * - Loading files (test inputs, textures, golden images)
 * - Rendering to a platform-specific image format
 * - Image comparison/assertion
 */
public abstract class AbstractStrokeRendererTest {

    public class TestCase(
        public val name: String,
        public val dryStroke: Stroke,
        public val wetStroke: InProgressStroke,
        public val transform: AffineTransform = AffineTransform.IDENTITY,
    ) {
        override fun toString(): String = name
    }

    /**
     * Loads the canonical stroke inputs used for testing - a real recording of someone writing
     * "hello" in cursive.
     *
     * Concrete implementations must provide standard platform-specific loading logic to satisfy the
     * contract. Each concrete implementation must load the same input data so that the resulting
     * rendered images can be compared for consistency.
     */
    protected abstract fun loadCursiveHelloInputs(): ImmutableStrokeInputBatch

    @OptIn(ExperimentalInkCustomBrushApi::class)
    protected val testCases: List<TestCase> by lazy {
        val cursiveHelloInputs = loadCursiveHelloInputs()
        listOf(
            // =========================================================================
            // 1. SIMPLE STROKE (SOLID, SINGLE COAT)
            // =========================================================================
            createTestCase(
                name = "simple_stroke_solid",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = 0xFF4FB5E8.toInt(),
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                inputs = cursiveHelloInputs,
            ),

            // =========================================================================
            // 2. SIMPLE STROKE WITH A SIMPLE TEXTURE
            // =========================================================================
            createTestCase(
                name = "simple_stroke_textured",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                paint =
                                    BrushPaint(
                                        listOf(
                                            BrushPaint.TilingTexture(
                                                clientTextureId = "checkerboard",
                                                sizeX = 10f,
                                                sizeY = 10f,
                                                sizeUnit =
                                                    BrushPaint.TextureLayer.SizeUnit
                                                        .STROKE_COORDINATES,
                                            )
                                        )
                                    )
                            ),
                        colorIntArgb = 0xFF0000FF.toInt(),
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                inputs = cursiveHelloInputs,
            ),
        )
    }

    private fun createTestCase(
        name: String,
        brush: Brush,
        inputs: StrokeInputBatch,
        transform: AffineTransform = AffineTransform.IDENTITY,
    ): TestCase {
        val wetStroke =
            InProgressStroke().apply {
                start(brush)
                enqueueInputs(inputs, ImmutableStrokeInputBatch.EMPTY)
                finishInput()
                updateShape(inputs.getDurationMillis())
            }
        val dryStroke = wetStroke.toImmutable()
        return TestCase(name, dryStroke, wetStroke, transform)
    }

    /**
     * Renders a dry [stroke] and compares the resulting image against [goldenName].
     *
     * The resulting image must have the given [imageWidth] and [imageHeight] dimensions, with a
     * completely transparent background (0x00000000).
     *
     * Lazy image comparison is encouraged for runtime efficiency and the human efficiency of
     * reviewing and approving golden image updates. Make sure to assert that the lazy asserts pass
     * in [assertLazyAssertsPass].
     */
    protected abstract fun renderAndCompareToGolden(
        stroke: Stroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
    )

    /**
     * Renders a wet [inProgressStroke] and compares the resulting image against [goldenName].
     *
     * The resulting image must have the given [imageWidth] and [imageHeight] dimensions, with a
     * completely transparent background (0x00000000).
     *
     * Lazy image comparison is encouraged for runtime efficiency and the human efficiency of
     * reviewing and approving golden image updates. Make sure to assert that the lazy asserts pass
     * in [assertLazyAssertsPass].
     */
    protected abstract fun renderAndCompareToGolden(
        inProgressStroke: InProgressStroke,
        transform: AffineTransform,
        imageWidth: Int,
        imageHeight: Int,
        goldenName: String,
    )

    /** Validates any lazy assertions made during [renderAndCompareToGolden]. */
    protected abstract fun assertLazyAssertsPass()

    @Test
    public fun dryRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            val params = computeRenderParams(testCase)
            renderAndCompareToGolden(
                stroke = testCase.dryStroke,
                transform = params.transform,
                imageWidth = params.width,
                imageHeight = params.height,
                goldenName = "${testCase.name}_dry",
            )
        }
    }

    @Test
    public fun wetRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            val params = computeRenderParams(testCase)
            renderAndCompareToGolden(
                inProgressStroke = testCase.wetStroke,
                transform = params.transform,
                imageWidth = params.width,
                imageHeight = params.height,
                goldenName = "${testCase.name}_wet",
            )
        }
    }

    @AfterTest
    fun tearDown() {
        assertLazyAssertsPass()
    }

    private class RenderParams(val transform: AffineTransform, val width: Int, val height: Int)

    private fun computeRenderParams(testCase: TestCase): RenderParams {
        val rawBox =
            checkNotNull(testCase.dryStroke.shape.computeBoundingBox()) {
                "Stroke bounding box must not be null"
            }
        val box = testCase.transform.applyTransform(rawBox).computeBoundingBox()
        val padding = 10f
        val width = kotlin.math.ceil(box.xMax - box.xMin + 2f * padding).toInt()
        val height = kotlin.math.ceil(box.yMax - box.yMin + 2f * padding).toInt()
        val translation =
            ImmutableAffineTransform(1f, 0f, padding - box.xMin, 0f, 1f, padding - box.yMin)
        val transform =
            MutableAffineTransform()
                .also { AffineTransform.multiply(translation, testCase.transform, it) }
                .toImmutable()
        return RenderParams(transform, width, height)
    }
}
