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

package androidx.ink.rendering.android.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withMatrix
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.StampingTexture
import androidx.ink.brush.BrushPaint.TextureLayer
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.BrushTip
import androidx.ink.brush.InputToolType
import androidx.ink.brush.SelfOverlap
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.geometry.Angle
import androidx.ink.geometry.Box
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.rendering.test.R
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.math.PI
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [CanvasStrokeRenderer] for Stroke and InProgressStroke. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(TestParameterInjector::class)
@MediumTest
class CanvasStrokeRendererTest {

    val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @SuppressWarnings("ImmutableEnumChecker")
    enum class TestCase(val stroke: InProgressStroke, val transform: Matrix = Matrix()) {
        /*
         ******************************************************************
         * Simple Strokes
         ******************************************************************
         */
        // TODO: b/274461578 - Add a case for winding textures
        // TODO: b/330528190 - Add a case for atlased textures
        SIMPLE_STROKES_SOLID(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag)
        ),
        SIMPLE_STROKES_TRANSLUCENT(
            finishedInProgressStroke(
                brush(color = TestColors.COBALT_BLUE.withAlpha(0.4)),
                ::inputsZigzag,
            )
        ),
        SIMPLE_STROKES_TILED(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    textureSize = 10f,
                ),
                ::inputsZigzag,
            )
        ),
        SIMPLE_STROKES_MULTICOAT(
            finishedInProgressStroke(
                brush(
                    BrushFamily(
                        listOf(
                            BrushCoat(
                                paint =
                                    tilingTextureBrushPaint(
                                        textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                                        textureSize = 10f,
                                    )
                            ),
                            BrushCoat(tip = BrushTip(scaleX = 0.5f, scaleY = 0.5f)),
                        )
                    ),
                    TestColors.RED,
                ),
                ::inputsTwist,
            )
        ),
        SIMPLE_STROKES_OPACITY_AND_HSL_SHIFT(
            finishedInProgressStroke(
                brush(
                    BrushFamily(
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
                                                    sourceOutOfRangeBehavior = OutOfRange.MIRROR,
                                                ),
                                        )
                                    ),
                                    BrushBehavior(
                                        TargetNode(
                                            target = Target.HUE_OFFSET_IN_RADIANS,
                                            targetModifierRangeStart = 0f,
                                            targetModifierRangeEnd = Angle.FULL_TURN_RADIANS,
                                            input =
                                                SourceNode(
                                                    source =
                                                        Source
                                                            .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                    sourceValueRangeStart = 0f,
                                                    sourceValueRangeEnd = 3f,
                                                    sourceOutOfRangeBehavior = OutOfRange.REPEAT,
                                                ),
                                        )
                                    ),
                                )
                        )
                    ),
                    TestColors.AVOCADO_GREEN,
                ),
                ::inputsTwist,
            )
        ),
        /*
         ******************************************************************
         * Particle Strokes
         ******************************************************************
         */
        PARTICLE_STROKES_SOLID(
            finishedInProgressStroke(
                brush(BrushFamily(BrushTip(particleGapDistanceScale = 2f)), TestColors.RED),
                ::inputsZigzag,
            )
        ),
        PARTICLE_STROKES_TRANSLUCENT(
            finishedInProgressStroke(
                brush(
                    BrushFamily(BrushTip(particleGapDistanceScale = 0.75f)),
                    TestColors.COBALT_BLUE.withAlpha(0.4),
                ),
                ::inputsTwist,
            )
        ),
        PARTICLE_STROKES_TILED(
            finishedInProgressStroke(
                tilingTextureBrush(
                    particleGapDistanceScale = 2f,
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    textureSize = 10f,
                ),
                ::inputsZigzag,
            )
        ),
        PARTICLE_STROKES_STAMPING(
            finishedInProgressStroke(
                stampingTextureBrush(particleGapDistanceScale = 2f),
                ::inputsZigzag,
            )
        ),
        /*
         ******************************************************************
         * Texture Origins
         ******************************************************************
         */
        TEXTURE_ORIGINS_STROKE_SPACE_ORIGIN(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureId = TEXTURE_ID_CIRCLE,
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
                    textureOffsetX = 0.5f,
                    textureOffsetY = 0.5f,
                    brushSize = 25f,
                ),
                ::inputsZagzig,
            )
        ),
        TEXTURE_ORIGINS_FIRST_STROKE_INPUT(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureId = TEXTURE_ID_CIRCLE,
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureOffsetX = 0.5f,
                    textureOffsetY = 0.5f,
                    brushSize = 25f,
                ),
                ::inputsZagzig,
            )
        ),
        TEXTURE_ORIGINS_LAST_STROKE_INPUT(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureId = TEXTURE_ID_CIRCLE,
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.LAST_STROKE_INPUT,
                    textureOffsetX = 0.5f,
                    textureOffsetY = 0.5f,
                    brushSize = 25f,
                ),
                ::inputsZagzig,
            )
        ),
        /*
         ******************************************************************
         * Texture Size Units
         ******************************************************************
         */
        TEXTURE_SIZE_UNITS_BRUSH_SIZE_15(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    brushSize = 15f,
                ),
                ::inputsZigzag,
            )
        ),
        TEXTURE_SIZE_UNITS_BRUSH_SIZE_30(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    brushSize = 30f,
                ),
                ::inputsZigzag,
            )
        ),
        TEXTURE_SIZE_UNITS_BRUSH_SIZE_30_TEXTURE_SIZE_HALF(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 0.5f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    brushSize = 30f,
                ),
                ::inputsZigzag,
            )
        ),
        TEXTURE_SIZE_UNITS_TEXTURE_SIZE_5_STROKE_COORDS(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 5f,
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                ),
                ::inputsZigzag,
            )
        ),
        TEXTURE_SIZE_UNITS_TEXTURE_SIZE_10_STROKE_COORDS(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 10f,
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                ),
                ::inputsZigzag,
            )
        ),
        /*
         ******************************************************************
         * Tiling Texture Wrap
         ******************************************************************
         */
        TILING_TEXTURE_WRAP_X_REPEAT_Y_REPEAT(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.REPEAT,
                    textureWrapY = TextureLayer.Wrap.REPEAT,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_MIRROR_Y_MIRROR(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.MIRROR,
                    textureWrapY = TextureLayer.Wrap.MIRROR,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_CLAMP_Y_CLAMP(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.CLAMP,
                    textureWrapY = TextureLayer.Wrap.CLAMP,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_REPEAT_Y_MIRROR(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.REPEAT,
                    textureWrapY = TextureLayer.Wrap.MIRROR,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_MIRROR_Y_REPEAT(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.MIRROR,
                    textureWrapY = TextureLayer.Wrap.REPEAT,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_REPEAT_Y_CLAMP(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.REPEAT,
                    textureWrapY = TextureLayer.Wrap.CLAMP,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_CLAMP_Y_REPEAT(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.CLAMP,
                    textureWrapY = TextureLayer.Wrap.REPEAT,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_MIRROR_Y_CLAMP(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.MIRROR,
                    textureWrapY = TextureLayer.Wrap.CLAMP,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        TILING_TEXTURE_WRAP_X_CLAMP_Y_MIRROR(
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 1f,
                    textureSizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    textureOrigin = TilingTexture.Origin.FIRST_STROKE_INPUT,
                    textureWrapX = TextureLayer.Wrap.CLAMP,
                    textureWrapY = TextureLayer.Wrap.MIRROR,
                    brushSize = 25f,
                ),
                ::inputsZigzag,
            )
        ),
        /*
         ******************************************************************
         * Blend Modes with Brush Color
         ******************************************************************
         */
        BLEND_MODES_WITH_BRUSH_COLOR_MODULATE_WHITE(
            colorBlendedStroke(TextureLayer.BlendMode.MODULATE, TestColors.WHITE)
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_MODULATE_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.MODULATE, TestColors.RED.withAlpha(0.5))
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_DST_IN_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.DST_IN, TestColors.RED.withAlpha(0.5))
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_DST_OUT_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.DST_OUT, TestColors.RED.withAlpha(0.5))
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_SRC_ATOP_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.SRC_ATOP, TestColors.RED.withAlpha(0.5))
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_SRC_IN_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.SRC_IN, TestColors.RED.withAlpha(0.5))
        ),
        BLEND_MODES_WITH_BRUSH_COLOR_SRC_RED_WITH_ALPHA_POINT_5(
            colorBlendedStroke(TextureLayer.BlendMode.SRC, TestColors.RED.withAlpha(0.5))
        ),
        /*
         ******************************************************************
         * Blend Modes with Two Textures
         ******************************************************************
         */
        BLEND_MODES_WITH_TWO_TEXTURES_SRC(textureBlendedStroke(TextureLayer.BlendMode.SRC)),
        BLEND_MODES_WITH_TWO_TEXTURES_DST(textureBlendedStroke(TextureLayer.BlendMode.DST)),
        BLEND_MODES_WITH_TWO_TEXTURES_SRC_OVER(
            textureBlendedStroke(TextureLayer.BlendMode.SRC_OVER)
        ),
        BLEND_MODES_WITH_TWO_TEXTURES_DST_OVER(
            textureBlendedStroke(TextureLayer.BlendMode.DST_OVER)
        ),
        BLEND_MODES_WITH_TWO_TEXTURES_SRC_OUT(textureBlendedStroke(TextureLayer.BlendMode.SRC_OUT)),
        BLEND_MODES_WITH_TWO_TEXTURES_DST_ATOP(
            textureBlendedStroke(TextureLayer.BlendMode.DST_ATOP)
        ),
        BLEND_MODES_WITH_TWO_TEXTURES_XOR(textureBlendedStroke(TextureLayer.BlendMode.XOR)),
        /*
         ******************************************************************
         * Texture Offset
         ******************************************************************
         */
        TEXTURE_OFFSET_X_0_Y_0(textureTransformStroke(offsetX = 0.0f, offsetY = 0.0f)),
        TEXTURE_OFFSET_X_25_Y_0(textureTransformStroke(offsetX = 0.25f, offsetY = 0.0f)),
        TEXTURE_OFFSET_X_5_Y_0(textureTransformStroke(offsetX = 0.5f, offsetY = 0.0f)),
        TEXTURE_OFFSET_X_75_Y_0(textureTransformStroke(offsetX = 0.75f, offsetY = 0.0f)),
        TEXTURE_OFFSET_X_25_Y_25(textureTransformStroke(offsetX = 0.25f, offsetY = 0.25f)),
        /*
         ******************************************************************
         * Texture Rotation
         ******************************************************************
         */
        TEXTURE_ROTATION_X_0_Y_0_R_45(
            textureRotationStroke(offsetX = 0.0f, offsetY = 0.0f, rotation = 45f)
        ),
        TEXTURE_ROTATION_X_5_Y_0_R_45(
            textureRotationStroke(offsetX = 0.5f, offsetY = 0.0f, rotation = 45f)
        ),
        TEXTURE_ROTATION_X_0_Y_0_R_180(
            textureRotationStroke(offsetX = 0.0f, offsetY = 0.0f, rotation = 180f)
        ),
        TEXTURE_ROTATION_X_0_Y_5_R_180(
            textureRotationStroke(offsetX = 0.0f, offsetY = 0.5f, rotation = 180f)
        ),
        /*
         ******************************************************************
         * Paint Preferences
         ******************************************************************
         */
        PAINT_PREFERENCES_NONE_ANY(
            textureSelfOverlapStroke(textureLayer = null, selfOverlap = SelfOverlap.ANY)
        ),
        PAINT_PREFERENCES_NONE_ACCUMULATE(
            textureSelfOverlapStroke(textureLayer = null, selfOverlap = SelfOverlap.ACCUMULATE)
        ),
        PAINT_PREFERENCES_NONE_DISCARD(
            textureSelfOverlapStroke(textureLayer = null, selfOverlap = SelfOverlap.DISCARD)
        ),
        PAINT_PREFERENCES_TILING_ANY(tilingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.ANY)),
        PAINT_PREFERENCES_TILING_ACCUMULATE(
            tilingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.ACCUMULATE)
        ),
        PAINT_PREFERENCES_TILING_DISCARD(
            tilingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.DISCARD)
        ),
        PAINT_PREFERENCES_STAMPING_ANY(
            stampingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.ANY)
        ),
        PAINT_PREFERENCES_STAMPING_ACCUMULATE(
            stampingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.ACCUMULATE)
        ),
        PAINT_PREFERENCES_STAMPING_DISCARD(
            stampingTextureSelfOverlapStroke(selfOverlap = SelfOverlap.DISCARD)
        ),
        /*
         ******************************************************************
         * Transformations
         ******************************************************************
         */
        TRANSFORM_IDENTITY(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag)
            // Use default transform, which is identity matrix
        ),
        TRANSFORM_UNIFORM_SCALE(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag),
            Matrix().apply { setScale(2f, 2f) },
        ),
        TRANSFORM_NON_UNIFORM_SCALE(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag),
            Matrix().apply { setScale(0.5f, 3.5f) },
        ),
        TRANSFORM_ROTATION(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag),
            strokeBounds(
                    finishedInProgressStroke(
                        brush(color = TestColors.AVOCADO_GREEN),
                        ::inputsZigzag,
                    )
                )
                .let { Matrix().apply { setRotate(45f, it.width, it.height) } },
        ),
        TRANSFORM_TRANSLATION(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag),
            Matrix().apply { setTranslate(10f, 20f) },
        ),
        TRANSFORM_SKEW(
            finishedInProgressStroke(brush(color = TestColors.AVOCADO_GREEN), ::inputsZigzag),
            Matrix().apply { setSkew(0.5f, 0f) },
        ),
    }

    @Test
    fun canDraw(@TestParameter testCase: TestCase) {
        assertWetAndDryMatchGoldensAndEachOther(
            testCase.stroke.brush!!,
            testCase.stroke,
            testCase.name,
            testCase.transform,
        )
    }

    /*
     ******************************************************************
     * Helpers
     ******************************************************************
     */

    private fun assertMatchesGolden(bitmap: Bitmap, name: String) =
        ImageDiffer.diffBitmapWithGolden(screenshotRule, this::class.simpleName, bitmap, name)

    private fun assertWetAndDryMatchGoldensAndEachOther(
        brush: Brush,
        stroke: InProgressStroke,
        name: String,
        transform: Matrix,
    ) {

        // Apply the transform to the stroke bounds
        val strokeBounds = strokeBounds(stroke)
        val adjustedBounds =
            floatArrayOf(
                // top-left
                0f,
                0f,
                // top-right
                strokeBounds.width + brush.size,
                0f,
                // bottom-right
                strokeBounds.width + brush.size,
                strokeBounds.height + brush.size,
                // bottom-left
                0f,
                strokeBounds.height + brush.size,
            )
        transform.mapPoints(adjustedBounds)
        var xMin = 0f
        var xMax = 0f
        var yMin = 0f
        var yMax = 0f
        for (i in 0 until adjustedBounds.size step 2) {
            xMax = maxOf(xMax, adjustedBounds[i])
            xMin = minOf(xMin, adjustedBounds[i])
            yMax = maxOf(yMax, adjustedBounds[i + 1])
            yMin = minOf(yMin, adjustedBounds[i + 1])
        }
        val width = (xMax - xMin).toInt()
        val height = (yMax - yMin).toInt()

        // Generate bitmaps and diff them against goldens
        val wet =
            ImageDiffer.createBitmap(width, height) { canvas ->
                drawStroke(canvas, stroke, transform)
            }
        assertMatchesGolden(wet, name + "_wet")

        val dryStroke = stroke.toImmutable()
        val dry =
            ImageDiffer.createBitmap(width, height) { canvas ->
                drawStroke(canvas, dryStroke, transform)
            }
        assertMatchesGolden(dry, name + "_dry")
    }

    private val textureStore = TextureBitmapStore { id ->
        when (id) {
            TEXTURE_ID_AIRPLANE_EMOJI -> R.drawable.airplane_emoji
            TEXTURE_ID_CHECKERBOARD -> R.drawable.checkerboard_black_and_transparent
            TEXTURE_ID_CIRCLE -> R.drawable.circle
            TEXTURE_ID_POOP_EMOJI -> R.drawable.poop_emoji
            else -> null
        }?.let { BitmapFactory.decodeResource(context.resources, it) }
    }

    private val defaultRenderer = CanvasStrokeRenderer.create(textureStore)

    private fun drawStroke(canvas: Canvas, stroke: InProgressStroke, transform: Matrix) {
        canvas.withMatrix(transform) { defaultRenderer.draw(canvas, stroke, transform) }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke, transform: Matrix) {
        canvas.withMatrix(transform) { defaultRenderer.draw(canvas, stroke, transform) }
    }

    private companion object {
        val NO_PREDICTION = ImmutableStrokeInputBatch.EMPTY

        fun inputsZigzag(offset: Float) =
            MutableStrokeInputBatch()
                .add(
                    InputToolType.UNKNOWN,
                    x = 0F + offset,
                    y = 0F + offset,
                    elapsedTimeMillis = 100,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 13F + offset,
                    y = 13F + offset,
                    elapsedTimeMillis = 117,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 27F + offset,
                    elapsedTimeMillis = 133,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 40F + offset,
                    y = 40F + offset,
                    elapsedTimeMillis = 150,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 50F + offset,
                    elapsedTimeMillis = 167,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 13F + offset,
                    y = 60F + offset,
                    elapsedTimeMillis = 183,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 0F + offset,
                    y = 70F + offset,
                    elapsedTimeMillis = 200,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 10F + offset,
                    y = 80F + offset,
                    elapsedTimeMillis = 217,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 20F + offset,
                    y = 90F + offset,
                    elapsedTimeMillis = 233,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 30F + offset,
                    y = 100F + offset,
                    elapsedTimeMillis = 250,
                )
                .toImmutable()

        fun inputsZagzig(offset: Float) =
            MutableStrokeInputBatch()
                .add(
                    InputToolType.UNKNOWN,
                    x = 30F + offset,
                    y = 0F + offset,
                    elapsedTimeMillis = 100,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 20F + offset,
                    y = 13F + offset,
                    elapsedTimeMillis = 117,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 10F + offset,
                    y = 27F + offset,
                    elapsedTimeMillis = 133,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 0F + offset,
                    y = 40F + offset,
                    elapsedTimeMillis = 150,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 13F + offset,
                    y = 50F + offset,
                    elapsedTimeMillis = 167,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 60F + offset,
                    elapsedTimeMillis = 183,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 40F + offset,
                    y = 70F + offset,
                    elapsedTimeMillis = 200,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 28F + offset,
                    y = 77F + offset,
                    elapsedTimeMillis = 217,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 17F + offset,
                    y = 83F + offset,
                    elapsedTimeMillis = 233,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 5F + offset,
                    y = 90F + offset,
                    elapsedTimeMillis = 250,
                )
                .toImmutable()

        fun inputsTwist(offset: Float) =
            MutableStrokeInputBatch()
                .add(
                    InputToolType.UNKNOWN,
                    x = 0F + offset,
                    y = 0F + offset,
                    elapsedTimeMillis = 100,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 33F + offset,
                    elapsedTimeMillis = 117,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 53F + offset,
                    y = 67F + offset,
                    elapsedTimeMillis = 133,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 80F + offset,
                    y = 100F + offset,
                    elapsedTimeMillis = 150,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 53F + offset,
                    y = 100F + offset,
                    elapsedTimeMillis = 167,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 100F + offset,
                    elapsedTimeMillis = 183,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 0F + offset,
                    y = 100F + offset,
                    elapsedTimeMillis = 200,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 27F + offset,
                    y = 67F + offset,
                    elapsedTimeMillis = 217,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 53F + offset,
                    y = 33F + offset,
                    elapsedTimeMillis = 233,
                )
                .add(
                    InputToolType.UNKNOWN,
                    x = 80F + offset,
                    y = 0F + offset,
                    elapsedTimeMillis = 250,
                )
                .toImmutable()

        fun brush(
            family: BrushFamily = StockBrushes.marker(),
            @ColorInt color: Int = TestColors.BLACK,
            size: Float = 15F,
            epsilon: Float = 0.1F,
        ) = Brush.createWithColorIntArgb(family, color, size, epsilon)

        fun tilingTextureBrush(
            particleGapDistanceScale: Float = 0f,
            textureId: String = TEXTURE_ID_CHECKERBOARD,
            textureSizeUnit: TextureLayer.SizeUnit,
            textureSize: Float,
            textureOrigin: TilingTexture.Origin = TilingTexture.Origin.STROKE_SPACE_ORIGIN,
            textureOffsetX: Float = 0f,
            textureOffsetY: Float = 0f,
            textureRotationDegrees: Float = 0f,
            textureWrapX: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
            textureWrapY: TextureLayer.Wrap = TextureLayer.Wrap.REPEAT,
            @ColorInt brushColor: Int = TestColors.BLACK,
            brushSize: Float = 15f,
        ): Brush {
            val tip = BrushTip(particleGapDistanceScale = particleGapDistanceScale)
            val paint =
                tilingTextureBrushPaint(
                    textureId = textureId,
                    textureWrapX = textureWrapX,
                    textureWrapY = textureWrapY,
                    textureSizeUnit = textureSizeUnit,
                    textureSize = textureSize,
                    textureOrigin = textureOrigin,
                    textureOffsetX = textureOffsetX,
                    textureOffsetY = textureOffsetY,
                    textureRotationDegrees = textureRotationDegrees,
                )
            return brush(BrushFamily(tip = tip, paint = paint), brushColor, brushSize)
        }

        fun stampingTextureBrush(
            particleGapDistanceScale: Float = 0f,
            textureId: String = TEXTURE_ID_CHECKERBOARD,
            @ColorInt brushColor: Int = TestColors.BLACK,
            brushSize: Float = 15f,
        ): Brush {
            val tip = BrushTip(particleGapDistanceScale = particleGapDistanceScale)
            val paint = stampingTextureBrushPaint(textureId = textureId)
            return brush(BrushFamily(tip = tip, paint = paint), brushColor, brushSize)
        }

        fun tilingTextureBrushPaint(
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

        fun stampingTextureBrushPaint(textureId: String = TEXTURE_ID_CHECKERBOARD): BrushPaint {
            return BrushPaint(listOf(StampingTexture(clientTextureId = textureId)))
        }

        fun textureTransformStroke(offsetX: Float, offsetY: Float): InProgressStroke =
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureSize = 30f,
                    textureOffsetX = offsetX,
                    textureOffsetY = offsetY,
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    brushSize = 30f,
                ),
                inputsZigzag(30f),
            )

        fun textureRotationStroke(
            offsetX: Float,
            offsetY: Float,
            rotation: Float,
        ): InProgressStroke =
            finishedInProgressStroke(
                tilingTextureBrush(
                    textureId = TEXTURE_ID_AIRPLANE_EMOJI,
                    textureSize = 30f,
                    textureSizeUnit = TextureLayer.SizeUnit.STROKE_COORDINATES,
                    textureOffsetX = offsetX,
                    textureOffsetY = offsetY,
                    textureRotationDegrees = rotation,
                    brushSize = 30f,
                ),
                inputsZigzag(30f),
            )

        fun finishedInProgressStroke(brush: Brush, inputs: (Float) -> ImmutableStrokeInputBatch) =
            finishedInProgressStroke(brush, inputs(brush.size))

        fun finishedInProgressStroke(brush: Brush, inputs: ImmutableStrokeInputBatch) =
            InProgressStroke().apply {
                start(brush)
                enqueueInputs(inputs, NO_PREDICTION)
                finishInput()
                updateShape(inputs.getDurationMillis())
            }

        fun colorBlendedStroke(
            blendMode: TextureLayer.BlendMode,
            @ColorInt color: Int,
        ): InProgressStroke {
            val textureLayer =
                TilingTexture(
                    TEXTURE_ID_POOP_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    blendMode = blendMode,
                )
            val paint = BrushPaint(listOf(textureLayer))
            val brush = brush(BrushFamily(paint = paint), color, size = 30f)
            return finishedInProgressStroke(brush, inputsTwist(brush.size))
        }

        fun textureBlendedStroke(blendMode: TextureLayer.BlendMode): InProgressStroke {
            val textureLayer1 =
                TilingTexture(
                    TEXTURE_ID_AIRPLANE_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    blendMode = blendMode,
                )
            val textureLayer2 =
                TilingTexture(
                    TEXTURE_ID_POOP_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                )
            val paint = BrushPaint(listOf(textureLayer1, textureLayer2))
            val brush = brush(BrushFamily(paint = paint), color = TestColors.WHITE, size = 40f)
            return finishedInProgressStroke(brush, inputsZigzag(brush.size))
        }

        fun tilingTextureSelfOverlapStroke(selfOverlap: SelfOverlap): InProgressStroke =
            textureSelfOverlapStroke(
                textureLayer =
                    TilingTexture(
                        TEXTURE_ID_POOP_EMOJI,
                        sizeX = 1f,
                        sizeY = 1f,
                        sizeUnit = TextureLayer.SizeUnit.BRUSH_SIZE,
                    ),
                selfOverlap = selfOverlap,
            )

        fun stampingTextureSelfOverlapStroke(selfOverlap: SelfOverlap): InProgressStroke =
            textureSelfOverlapStroke(
                textureLayer = StampingTexture(TEXTURE_ID_POOP_EMOJI),
                selfOverlap = selfOverlap,
            )

        fun textureSelfOverlapStroke(
            textureLayer: TextureLayer?,
            selfOverlap: SelfOverlap,
        ): InProgressStroke {
            val textureLayers = buildList {
                if (textureLayer != null) {
                    add(textureLayer)
                }
            }
            val paint = BrushPaint(textureLayers = textureLayers, selfOverlap = selfOverlap)
            val tip =
                BrushTip(
                    cornerRounding = 0.2f,
                    particleGapDistanceScale = 1.5f,
                    behaviors =
                        listOf(
                            BrushBehavior(
                                terminalNodes =
                                    listOf(
                                        TargetNode(
                                            target = Target.ROTATION_OFFSET_IN_RADIANS,
                                            targetModifierRangeStart = -PI.toFloat(),
                                            targetModifierRangeEnd = PI.toFloat(),
                                            input =
                                                SourceNode(
                                                    source = Source.DIRECTION_ABOUT_ZERO_IN_RADIANS,
                                                    sourceValueRangeStart = -PI.toFloat(),
                                                    sourceValueRangeEnd = PI.toFloat(),
                                                    sourceOutOfRangeBehavior = OutOfRange.REPEAT,
                                                ),
                                        )
                                    )
                            )
                        ),
                )
            val brush = brush(family = BrushFamily(tip, paint), color = 0x7733fc66)
            return finishedInProgressStroke(brush, inputsTwist(brush.size))
        }

        @ColorInt
        fun Int.withAlpha(alpha: Double): Int {
            return ColorUtils.setAlphaComponent(this, (alpha * 255).toInt())
        }

        private fun strokeBounds(stroke: InProgressStroke): Box {
            return BoxAccumulator().also { stroke.populateMeshBounds(0, it) }.box!!
        }

        const val TEXTURE_ID_AIRPLANE_EMOJI = "airplane-emoji"
        const val TEXTURE_ID_CHECKERBOARD = "checkerboard-overlay-pen"
        const val TEXTURE_ID_CIRCLE = "circle"
        const val TEXTURE_ID_POOP_EMOJI = "poop-emoji"
    }
}
