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

import androidx.annotation.ColorInt
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Angle
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
@OptIn(ExperimentalInkCustomBrushApi::class)
abstract class AbstractStrokeRendererTest {

    class TestCase(
        val name: String,
        val dryStroke: Stroke,
        val wetStroke: InProgressStroke,
        val transform: AffineTransform = AffineTransform.IDENTITY,
        /**
         * Minimum Android API level on which Android `CanvasStrokeRenderer` supports rendering this
         * test case (e.g., 34 for `Canvas.drawMesh`-only features such as `StampingTexture` without
         * a tiling fallback or `SelfOverlap.ACCUMULATE`). Ignored on non-Android platforms.
         */
        val minAndroidSdk: Int = 21,
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

    private val cursiveHelloInputs: ImmutableStrokeInputBatch by lazy { loadCursiveHelloInputs() }

    protected val simpleStrokeTexturedTestCase: TestCase by lazy {
        createTestCase(
            name = "simple_stroke_textured",
            brush =
                Brush.createWithColorIntArgb(
                    family =
                        BrushFamily(
                            paint =
                                BrushPaint(
                                    listOf(
                                        TilingTexture(
                                            clientTextureId = TEXTURE_ID_CHECKERBOARD,
                                            sizeX = 10f,
                                            sizeY = 10f,
                                            sizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                        )
                                    )
                                )
                        ),
                    colorIntArgb = 0xFF0000FF.toInt(),
                    size = 15f,
                    epsilon = 0.1f,
                ),
            inputs = cursiveHelloInputs,
        )
    }

    protected val testCases: List<TestCase> by lazy {
        listOf(
            // =========================================================================
            // 1. SIMPLE STROKES
            // =========================================================================
            createTestCase(
                name = "simple_stroke_solid",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_LIGHT_BLUE,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            simpleStrokeTexturedTestCase,
            createTestCase(
                name = "simple_stroke_translucent",
                brush =
                    Brush.createWithColorIntArgb(
                        family = BrushFamily(paint = BrushPaint()),
                        colorIntArgb = COLOR_COBALT_BLUE_TRANSLUCENT,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "simple_stroke_multicoat",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                listOf(
                                    BrushCoat(
                                        paint =
                                            tilingTextureBrushPaint(
                                                textureSizeUnit =
                                                    TextureLayer.SizeUnit.STROKE_COORDINATES,
                                                textureSize = 10f,
                                            )
                                    ),
                                    BrushCoat(tip = BrushTip(scaleX = 0.5f, scaleY = 0.5f)),
                                )
                            ),
                        colorIntArgb = COLOR_RED,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
            ),
            createTestCase(
                name = "simple_stroke_opacity_and_hcl_shift",
                brush =
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                tip =
                                    BrushTip(
                                        behaviors =
                                            listOf(
                                                BrushBehavior(
                                                    TargetNode(
                                                        target = Target.OPACITY_MULTIPLIER,
                                                        targetModifierRangeStart = 1f,
                                                        targetModifierRangeEnd = 0.25f,
                                                        input =
                                                            SourceNode(
                                                                source =
                                                                    Source
                                                                        .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                                sourceValueRangeStart = 0f,
                                                                sourceValueRangeEnd = 2f,
                                                                sourceOutOfRangeBehavior =
                                                                    OutOfRange.MIRROR,
                                                            ),
                                                    )
                                                ),
                                                BrushBehavior(
                                                    TargetNode(
                                                        target = Target.HUE_OFFSET_IN_RADIANS,
                                                        targetModifierRangeStart = 0f,
                                                        targetModifierRangeEnd =
                                                            Angle.FULL_TURN_RADIANS,
                                                        input =
                                                            SourceNode(
                                                                source =
                                                                    Source
                                                                        .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                                sourceValueRangeStart = 0f,
                                                                sourceValueRangeEnd = 3f,
                                                                sourceOutOfRangeBehavior =
                                                                    OutOfRange.REPEAT,
                                                            ),
                                                    )
                                                ),
                                            )
                                    )
                            ),
                        colorIntArgb = COLOR_AVOCADO_GREEN,
                        size = 15f,
                        epsilon = 0.1f,
                    ),
                // Per-vertex opacity and HCL shift brush behaviors require CanvasMeshRenderer
                // (Canvas.drawMesh, Android API 34+) on Android.
                minAndroidSdk = ANDROID_MESH_RENDERER_MIN_SDK,
            ),
        )
    }

    private fun createTestCase(
        name: String,
        brush: Brush,
        inputs: StrokeInputBatch = cursiveHelloInputs,
        transform: AffineTransform = AffineTransform.IDENTITY,
        minAndroidSdk: Int = 21,
    ): TestCase {
        val wetStroke =
            InProgressStroke().apply {
                start(brush)
                enqueueInputs(inputs, ImmutableStrokeInputBatch.EMPTY)
                finishInput()
                updateShape(inputs.getDurationMillis())
            }
        val dryStroke = wetStroke.toImmutable()
        return TestCase(
            name = name,
            dryStroke = dryStroke,
            wetStroke = wetStroke,
            transform = transform,
            minAndroidSdk = minAndroidSdk,
        )
    }

    private fun tilingTextureBrushPaint(
        textureId: String = TEXTURE_ID_CHECKERBOARD,
        textureSizeUnit: TextureLayer.SizeUnit,
        textureSize: Float,
        textureOrigin: TilingTexture.Origin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
        textureOffsetX: Float = 0f,
        textureOffsetY: Float = 0f,
        textureRotationDegrees: Float = 0f,
        textureWrapX: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
        textureWrapY: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
    ): BrushPaint {
        val textureLayer =
            TilingTexture(
                clientTextureId = textureId,
                sizeX = textureSize,
                sizeY = textureSize,
                offsetX = textureOffsetX,
                offsetY = textureOffsetY,
                rotationDegrees = textureRotationDegrees,
                sizeUnit = textureSizeUnit,
                origin = textureOrigin,
                wrapX = textureWrapX,
                wrapY = textureWrapY,
            )
        return BrushPaint(listOf(textureLayer))
    }

    /** Returns whether the current platform/SDK environment supports rendering [testCase]. */
    protected open fun isSupported(testCase: TestCase): Boolean = true

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
    fun dryRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            if (!isSupported(testCase)) continue
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
    fun wetRendering_matchesExpectedGolden() {
        for (testCase in testCases) {
            if (!isSupported(testCase)) continue
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

    protected class RenderParams(val transform: AffineTransform, val width: Int, val height: Int)

    protected fun computeRenderParams(testCase: TestCase): RenderParams {
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

    protected companion object {
        const val TEXTURE_ID_CHECKERBOARD = "checkerboard"
        const val TEXTURE_ID_CIRCLE = "circle"
        const val TEXTURE_ID_AIRPLANE_EMOJI = "airplane_emoji"
        const val TEXTURE_ID_POOP_EMOJI = "poop_emoji"

        private const val ANDROID_MESH_RENDERER_MIN_SDK = 34
        @ColorInt private const val COLOR_RED = 0xFFF7251E.toInt()
        @ColorInt private const val COLOR_AVOCADO_GREEN = 0xFF558B2F.toInt()
        @ColorInt private const val COLOR_LIGHT_BLUE = 0xFF4FB5E8.toInt()
        @ColorInt private const val COLOR_COBALT_BLUE_TRANSLUCENT = 0x6601579B
    }
}
