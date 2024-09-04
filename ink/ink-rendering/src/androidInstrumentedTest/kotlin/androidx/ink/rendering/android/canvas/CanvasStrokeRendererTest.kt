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

import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.BlendMode
import androidx.ink.brush.BrushPaint.TextureOrigin
import androidx.ink.brush.BrushPaint.TextureSizeUnit
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.Angle
import androidx.ink.rendering.test.R
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [CanvasStrokeRenderer] for Stroke and InProgressStroke. */
@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CanvasStrokeRendererTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(CanvasStrokeRendererTestActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun drawsSimpleStrokes() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair(
                        "Solid",
                        finishedInProgressStroke(
                            brush(color = TestColors.AVOCADO_GREEN),
                            INPUTS_ZIGZAG
                        ),
                    ),
                    Pair(
                        "Translucent",
                        finishedInProgressStroke(
                            brush(
                                BrushFamily(BrushTip(opacityMultiplier = 1.0F)),
                                TestColors.COBALT_BLUE.withAlpha(0.4),
                            ),
                            INPUTS_TWIST,
                        ),
                    ),
                    Pair(
                        "Tiled",
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
                                textureSize = 10f
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                    Pair(
                        "Multicoat",
                        finishedInProgressStroke(
                            brush(
                                BrushFamily(
                                    listOf(
                                        BrushCoat(
                                            paint =
                                                tiledBrushPaint(
                                                    textureSizeUnit =
                                                        TextureSizeUnit.STROKE_COORDINATES,
                                                    textureSize = 10f,
                                                )
                                        ),
                                        BrushCoat(tip = BrushTip(scaleX = 0.5f, scaleY = 0.5f)),
                                    )
                                ),
                                TestColors.RED,
                            ),
                            INPUTS_TWIST,
                        ),
                    ),
                    // TODO: b/330528190 - Add row for atlased textures
                    Pair(
                        """
              Opacity &
              HSL Shift
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            brush(
                                BrushFamily(
                                    BrushTip(
                                        behaviors =
                                            listOf(
                                                BrushBehavior(
                                                    source =
                                                        BrushBehavior.Source
                                                            .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                    target =
                                                        BrushBehavior.Target.OPACITY_MULTIPLIER,
                                                    sourceValueRangeLowerBound = 0f,
                                                    sourceValueRangeUpperBound = 2f,
                                                    targetModifierRangeLowerBound = 1f,
                                                    targetModifierRangeUpperBound = 0.25f,
                                                    sourceOutOfRangeBehavior =
                                                        BrushBehavior.OutOfRange.MIRROR,
                                                ),
                                                BrushBehavior(
                                                    source =
                                                        BrushBehavior.Source
                                                            .DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                                                    target =
                                                        BrushBehavior.Target.HUE_OFFSET_IN_RADIANS,
                                                    sourceValueRangeLowerBound = 0f,
                                                    sourceValueRangeUpperBound = 3f,
                                                    targetModifierRangeLowerBound = 0f,
                                                    targetModifierRangeUpperBound =
                                                        Angle.FULL_TURN_RADIANS,
                                                    sourceOutOfRangeBehavior =
                                                        BrushBehavior.OutOfRange.REPEAT,
                                                ),
                                            )
                                    )
                                ),
                                TestColors.AVOCADO_GREEN,
                            ),
                            INPUTS_TWIST,
                        ),
                    ),
                    // TODO: b/274461578 - Add row for winding textures
                )
            )
        }
        assertScreenshot("SimpleStrokes")
    }

    @Test
    fun supportsTextureOrigins() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair(
                        "STROKE_SPACE_ORIGIN",
                        finishedInProgressStroke(
                            tiledBrush(
                                textureUri = CanvasStrokeRendererTestActivity.TEXTURE_URI_CIRCLE,
                                textureSize = 1f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                textureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
                                textureOffsetX = 0.5f,
                                textureOffsetY = 0.5f,
                                brushSize = 25f,
                            ),
                            INPUTS_ZAGZIG,
                        ),
                    ),
                    Pair(
                        "FIRST_STROKE_INPUT",
                        finishedInProgressStroke(
                            tiledBrush(
                                textureUri = CanvasStrokeRendererTestActivity.TEXTURE_URI_CIRCLE,
                                textureSize = 1f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                textureOrigin = TextureOrigin.FIRST_STROKE_INPUT,
                                textureOffsetX = 0.5f,
                                textureOffsetY = 0.5f,
                                brushSize = 25f,
                            ),
                            INPUTS_ZAGZIG,
                        ),
                    ),
                    Pair(
                        "LAST_STROKE_INPUT",
                        finishedInProgressStroke(
                            tiledBrush(
                                textureUri = CanvasStrokeRendererTestActivity.TEXTURE_URI_CIRCLE,
                                textureSize = 1f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                textureOrigin = TextureOrigin.LAST_STROKE_INPUT,
                                textureOffsetX = 0.5f,
                                textureOffsetY = 0.5f,
                                brushSize = 25f,
                            ),
                            INPUTS_ZAGZIG,
                        ),
                    ),
                )
            )
        }
        assertScreenshot("TextureOrigins")
    }

    @Test
    fun supportsTextureSizeUnits() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair(
                        """
              textureSize=
              BRUSH_SIZE*1
              brushSize=15
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSize = 1f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                brushSize = 15f,
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                    Pair(
                        """
              textureSize=
              BRUSH_SIZE*1
              brushSize=30
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSize = 1f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                brushSize = 30f,
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                    Pair(
                        """
              textureSize=
              BRUSH_SIZE/2
              brushSize=30
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSize = 0.5f,
                                textureSizeUnit = TextureSizeUnit.BRUSH_SIZE,
                                brushSize = 30f,
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                    // TODO: b/336835642 - add row for STROKE_SIZE
                    Pair(
                        """
              textureSize=
              STROKE_COORDS*5
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSize = 5f,
                                textureSizeUnit = TextureSizeUnit.STROKE_COORDINATES
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                    Pair(
                        """
              textureSize=
              STROKE_COORDS*10
            """
                            .trimIndent(),
                        finishedInProgressStroke(
                            tiledBrush(
                                textureSize = 10f,
                                textureSizeUnit = TextureSizeUnit.STROKE_COORDINATES
                            ),
                            INPUTS_ZIGZAG,
                        ),
                    ),
                )
            )
        }
        assertScreenshot("TextureSizeUnits")
    }

    @Test
    fun supportsBlendModesWithBrushColor() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair(
                        """
              MODULATE
              WHITE
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.MODULATE, TestColors.WHITE),
                    ),
                    Pair(
                        """
              MODULATE
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.MODULATE, TestColors.RED.withAlpha(0.5)),
                    ),
                    Pair(
                        """
              DST_IN
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.DST_IN, TestColors.RED.withAlpha(0.5)),
                    ),
                    Pair(
                        """
              DST_OUT
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.DST_OUT, TestColors.RED.withAlpha(0.5)),
                    ),
                    Pair(
                        """
              SRC_ATOP
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.SRC_ATOP, TestColors.RED.withAlpha(0.5)),
                    ),
                    Pair(
                        """
              SRC_IN
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.SRC_IN, TestColors.RED.withAlpha(0.5)),
                    ),
                    Pair(
                        """
              SRC
              RED.withAlpha(0.5)
            """
                            .trimIndent(),
                        colorBlendedStroke(BlendMode.SRC, TestColors.RED.withAlpha(0.5)),
                    ),
                )
            )
        }
        assertScreenshot("BlendWithBrushColor")
    }

    @Test
    fun supportsBlendModesWithTwoTextures() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair("SRC", textureBlendedStroke(BlendMode.SRC)),
                    Pair("DST", textureBlendedStroke(BlendMode.DST)),
                    Pair("SRC_OVER", textureBlendedStroke(BlendMode.SRC_OVER)),
                    Pair("DST_OVER", textureBlendedStroke(BlendMode.DST_OVER)),
                    Pair("SRC_OUT", textureBlendedStroke(BlendMode.SRC_OUT)),
                    Pair("DST_ATOP", textureBlendedStroke(BlendMode.DST_ATOP)),
                    Pair("XOR", textureBlendedStroke(BlendMode.XOR)),
                )
            )
        }
        assertScreenshot("BlendTwoTextures")
    }

    @Test
    fun supportsTextureOffset() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.addStrokeRows(
                listOf(
                    Pair(
                        """
              offsetX=0.0
              offsetY=0.0
            """
                            .trimIndent(),
                        textureTransformStroke(offsetX = 0.0f, offsetY = 0.0f),
                    ),
                    Pair(
                        """
              offsetX=0.25
              offsetY=0.0
            """
                            .trimIndent(),
                        textureTransformStroke(offsetX = 0.25f, offsetY = 0.0f),
                    ),
                    Pair(
                        """
              offsetX=0.5
              offsetY=0.0
            """
                            .trimIndent(),
                        textureTransformStroke(offsetX = 0.5f, offsetY = 0.0f),
                    ),
                    Pair(
                        """
              offsetX=0.75
              offsetY=0.0
            """
                            .trimIndent(),
                        textureTransformStroke(offsetX = 0.75f, offsetY = 0.0f),
                    ),
                    Pair(
                        """
              offsetX=0.25
              offsetY=0.25
            """
                            .trimIndent(),
                        textureTransformStroke(offsetX = 0.25f, offsetY = 0.25f),
                    ),
                )
            )
        }
        assertScreenshot("TextureOffset")
    }

    private fun assertScreenshot(filename: String) {
        onView(withId(R.id.stroke_grid))
            .perform(
                captureToBitmap() {
                    it.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
                }
            )
    }

    private companion object {
        val NO_PREDICTION = ImmutableStrokeInputBatch.EMPTY

        val INPUTS_ZIGZAG =
            MutableStrokeInputBatch()
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 0F, elapsedTimeMillis = 100)
                .addOrThrow(InputToolType.UNKNOWN, x = 40F, y = 40F, elapsedTimeMillis = 150)
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 70F, elapsedTimeMillis = 200)
                .addOrThrow(InputToolType.UNKNOWN, x = 30F, y = 100F, elapsedTimeMillis = 250)
                .asImmutable()

        val INPUTS_ZAGZIG =
            MutableStrokeInputBatch()
                .addOrThrow(InputToolType.UNKNOWN, x = 30F, y = 0F, elapsedTimeMillis = 100)
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 40F, elapsedTimeMillis = 150)
                .addOrThrow(InputToolType.UNKNOWN, x = 40F, y = 70F, elapsedTimeMillis = 200)
                .addOrThrow(InputToolType.UNKNOWN, x = 5F, y = 90F, elapsedTimeMillis = 250)
                .asImmutable()

        val INPUTS_TWIST =
            MutableStrokeInputBatch()
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 0F, elapsedTimeMillis = 100)
                .addOrThrow(InputToolType.UNKNOWN, x = 80F, y = 100F, elapsedTimeMillis = 150)
                .addOrThrow(InputToolType.UNKNOWN, x = 0F, y = 100F, elapsedTimeMillis = 200)
                .addOrThrow(InputToolType.UNKNOWN, x = 80F, y = 0F, elapsedTimeMillis = 250)
                .asImmutable()

        fun brush(
            family: BrushFamily = StockBrushes.markerLatest,
            @ColorInt color: Int = TestColors.BLACK,
            size: Float = 15F,
            epsilon: Float = 0.1F,
        ) = Brush.createWithColorIntArgb(family, color, size, epsilon)

        fun tiledBrush(
            textureUri: String = CanvasStrokeRendererTestActivity.TEXTURE_URI_CHECKERBOARD,
            textureSizeUnit: TextureSizeUnit,
            textureSize: Float,
            textureOrigin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
            textureOffsetX: Float = 0f,
            textureOffsetY: Float = 0f,
            @ColorInt brushColor: Int = TestColors.BLACK,
            brushSize: Float = 15f,
        ): Brush {
            val paint =
                tiledBrushPaint(
                    textureUri = textureUri,
                    textureSizeUnit = textureSizeUnit,
                    textureSize = textureSize,
                    textureOrigin = textureOrigin,
                    textureOffsetX = textureOffsetX,
                    textureOffsetY = textureOffsetY,
                )
            return brush(BrushFamily(paint = paint), brushColor, brushSize)
        }

        fun tiledBrushPaint(
            textureUri: String = CanvasStrokeRendererTestActivity.TEXTURE_URI_CHECKERBOARD,
            textureSizeUnit: TextureSizeUnit,
            textureSize: Float,
            textureOrigin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
            textureOffsetX: Float = 0f,
            textureOffsetY: Float = 0f,
        ): BrushPaint {
            val textureLayer =
                BrushPaint.TextureLayer(
                    colorTextureUri = textureUri,
                    sizeX = textureSize,
                    sizeY = textureSize,
                    offsetX = textureOffsetX,
                    offsetY = textureOffsetY,
                    sizeUnit = textureSizeUnit,
                    origin = textureOrigin,
                )
            return BrushPaint(listOf(textureLayer))
        }

        fun textureTransformStroke(offsetX: Float, offsetY: Float): InProgressStroke =
            finishedInProgressStroke(
                tiledBrush(
                    textureSize = 30f,
                    textureOffsetX = offsetX,
                    textureOffsetY = offsetY,
                    textureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
                    brushSize = 30f,
                ),
                INPUTS_ZIGZAG,
            )

        fun finishedInProgressStroke(brush: Brush, inputs: ImmutableStrokeInputBatch) =
            InProgressStroke().apply {
                start(brush)
                enqueueInputs(inputs, NO_PREDICTION).getOrThrow()
                finishInput()
                updateShape(inputs.getDurationMillis()).getOrThrow()
            }

        fun colorBlendedStroke(blendMode: BlendMode, @ColorInt color: Int): InProgressStroke {
            val textureLayer =
                BrushPaint.TextureLayer(
                    CanvasStrokeRendererTestActivity.TEXTURE_URI_POOP_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureSizeUnit.BRUSH_SIZE,
                    blendMode = blendMode,
                )
            val paint = BrushPaint(listOf(textureLayer))
            val brush = brush(BrushFamily(paint = paint), color, size = 30f)
            return finishedInProgressStroke(brush, INPUTS_TWIST)
        }

        fun textureBlendedStroke(blendMode: BlendMode): InProgressStroke {
            val textureLayer1 =
                BrushPaint.TextureLayer(
                    CanvasStrokeRendererTestActivity.TEXTURE_URI_AIRPLANE_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureSizeUnit.BRUSH_SIZE,
                    blendMode = blendMode,
                )
            val textureLayer2 =
                BrushPaint.TextureLayer(
                    CanvasStrokeRendererTestActivity.TEXTURE_URI_POOP_EMOJI,
                    sizeX = 1f,
                    sizeY = 1f,
                    sizeUnit = TextureSizeUnit.BRUSH_SIZE,
                )
            val paint = BrushPaint(listOf(textureLayer1, textureLayer2))
            val brush = brush(BrushFamily(paint = paint), color = TestColors.WHITE, size = 40f)
            return finishedInProgressStroke(brush, INPUTS_ZIGZAG)
        }

        @ColorInt
        fun Int.withAlpha(alpha: Double): Int {
            return ColorUtils.setAlphaComponent(this, (alpha * 255).toInt())
        }
    }
}
