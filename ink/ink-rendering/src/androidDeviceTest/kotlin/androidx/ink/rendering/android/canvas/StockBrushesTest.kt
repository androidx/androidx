/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlin.jvm.JvmStatic
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Emulator-based screenshot test of brushes defined by [StockBrushes] covering known edge cases and
 * expected conditions. For each new brush family released, add the BrushFamily to the `families`
 * list below with an appropriate `clientBrushFamilyId` for the test prefix.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(Parameterized::class)
@MediumTest
class StockBrushesTest(val brushName: String) {
    val family = familiesByName[brushName]!!

    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = StockBrushesTestHelper(context)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    companion object {
        private val families =
            listOf(
                StockBrushes.marker(StockBrushes.MarkerVersion.V1)
                    .copy(clientBrushFamilyId = "marker_1"),
                StockBrushes.pressurePen(StockBrushes.PressurePenVersion.V1)
                    .copy(clientBrushFamilyId = "pressure-pen_1"),
                StockBrushes.highlighter(version = StockBrushes.HighlighterVersion.V1)
                    .copy(clientBrushFamilyId = "highlighter_1"),
                StockBrushes.dashedLine(StockBrushes.DashedLineVersion.V1)
                    .copy(clientBrushFamilyId = "dashed-line_1"),
                StockBrushes.emojiHighlighter(
                        clientTextureId = "emoji_heart",
                        showMiniEmojiTrail = true,
                        version = StockBrushes.EmojiHighlighterVersion.V1,
                    )
                    // clientBrushFamilyId specified for test case exceptions.
                    .copy(clientBrushFamilyId = "heart_emoji_highlighter_1"),
                StockBrushes.emojiHighlighter(
                        clientTextureId = "emoji_heart",
                        showMiniEmojiTrail = false,
                        version = StockBrushes.EmojiHighlighterVersion.V1,
                    )
                    // clientBrushFamilyId specified for test case exceptions.
                    .copy(clientBrushFamilyId = "heart_emoji_highlighter_no_trail_1"),
            )
        private val familiesByName = families.associateBy { it.clientBrushFamilyId }
        private val alphas = listOf(255, 153, 76)
        private val colors = listOf(TestColors.BLUE, TestColors.RED, TestColors.GREEN)
        private val sizes = listOf(40f, 5f, 1f)

        // Brush families with known exceptions.
        /** BrushFamilies that have a size multiplier of zero. */
        private val sizeMultiplierZeroExceptions =
            listOf("heart_emoji_highlighter_1", "heart_emoji_highlighter_no_trail_1")
        /** BrushFamilies that do NOT have a prediction fade out behavior on every tip. */
        private val predictionFadeOutBehaviorExceptions =
            listOf("heart_emoji_highlighter_1", "heart_emoji_highlighter_no_trail_1")

        @Parameters(name = "{0}")
        @JvmStatic
        fun params(): List<Array<String>> =
            families.map { family -> arrayOf(family.clientBrushFamilyId) }
    }

    fun assertStrokesMatchGolden(strokes: List<List<List<Stroke>>>, name: String) =
        ImageDiffer.diffBitmapWithGolden(
            screenshotRule,
            this::class.simpleName,
            helper.drawToBitmap(strokes),
            name,
        )

    /**
     * Creates a copy of a stroke input batch and overrides the [pressure] of each input if pressure
     * is not null.
     */
    fun StrokeInputBatch.overrideInputChannel(
        pressure: Float? = null,
        tilt: Float? = null,
        orientation: Float? = null,
    ): ImmutableStrokeInputBatch {
        val builder = MutableStrokeInputBatch()
        for (i in 0 until size) {
            val input = get(i)
            builder.add(
                type = input.toolType,
                x = input.x,
                y = input.y,
                elapsedTimeMillis = input.elapsedTimeMillis,
                pressure = pressure ?: input.pressure,
                tiltRadians = tilt ?: input.tiltRadians,
                orientationRadians = orientation ?: input.orientationRadians,
            )
        }
        return builder.toImmutable()
    }

    @Test
    fun predictionFadeOutBehavior_occursOncePerBrushTip() {
        if (family.clientBrushFamilyId in predictionFadeOutBehaviorExceptions) return

        val prediction = StockBrushes.predictionFadeOutBehavior
        for (coat in family.coats) {
            assertEquals(1, coat.tip.behaviors.count { it == prediction })
        }
    }

    @Test
    fun targetSizeMultiplier_isNotZero() {
        if (family.clientBrushFamilyId in sizeMultiplierZeroExceptions) return

        for (coat in family.coats) {
            for (behavior in coat.tip.behaviors) {
                for (terminalNode in behavior.terminalNodes) {
                    if (
                        terminalNode is BrushBehavior.TargetNode &&
                            terminalNode.target == BrushBehavior.Target.SIZE_MULTIPLIER
                    ) {
                        assertNotEquals(0f, terminalNode.targetModifierRangeStart)
                        assertNotEquals(0f, terminalNode.targetModifierRangeEnd)
                    }
                }
            }
        }
    }

    @Test
    fun inputToolTypeTouch_isNotEnabledForSourcePressure() {
        for (coat in family.coats) {
            for (behavior in coat.tip.behaviors) {
                for (terminalNode in behavior.terminalNodes) {
                    // Every `SourceNode` with a source of `NORMALIZED_PRESSURE` must pass through a
                    // `ToolTypeFilterNode` that excludes `TOUCH` input before reaching the target.
                    val stack = mutableListOf<BrushBehavior.Node>(terminalNode)
                    while (!stack.isEmpty()) {
                        // stack.removeLast() isn't available until API 35 (V).
                        val node = stack.removeAt(stack.lastIndex)
                        // If we reach a `ToolTypeFilterNode` that excludes `TOUCH` input, we can
                        // ignore
                        // everything beyond it in the node graph.
                        if (
                            node is BrushBehavior.ToolTypeFilterNode &&
                                !node.enabledToolTypes.contains(InputToolType.TOUCH)
                        ) {
                            continue
                        }
                        // If we reach a `SourceNode` without passing through a `ToolTypeFilterNode`
                        // that
                        // excludes `TOUCH` input, it shouldn't be using the `NORMALIZED_PRESSURE`
                        // source.
                        if (node is BrushBehavior.SourceNode) {
                            assertNotEquals(node.source, BrushBehavior.Source.NORMALIZED_PRESSURE)
                        }
                        stack.addAll(node.inputs)
                    }
                }
            }
        }
    }

    @Test
    fun zeroAndLowPressureInput_appearFor() {
        assertStrokesMatchGolden(
            listOf(
                listOf(
                    listOf(
                        Stroke(
                            makeBrush(family = family, size = 10f),
                            helper.octogonStylusInputs.overrideInputChannel(pressure = 0f),
                        )
                    )
                ),
                listOf(
                    listOf(
                        Stroke(
                            makeBrush(family = family, size = 10f),
                            helper.octogonStylusInputs.overrideInputChannel(pressure = 0.1f),
                        )
                    )
                ),
            ),
            "${brushName}_withZeroAndLowPressureInput_appearFor",
        )
    }

    /**
     * Side by side comparison of stylus input with 1) all input channels a.k.a. baseline, 2) no
     * pressure, 3) no tilt, 4) no orientation
     */
    @Test
    fun missingPressureTiltOrOrientationStylusData_appearReasonable() {
        assertStrokesMatchGolden(
            listOf(
                listOf(
                    listOf(
                        Stroke(makeBrush(family = family, size = 10f), helper.octogonStylusInputs)
                    )
                ),
                listOf(
                    listOf(
                        Stroke(
                            makeBrush(family = family, size = 10f),
                            helper.octogonStylusInputs.overrideInputChannel(
                                pressure = StrokeInput.NO_PRESSURE
                            ),
                        )
                    )
                ),
                listOf(
                    listOf(
                        Stroke(
                            makeBrush(family = family, size = 10f),
                            helper.octogonStylusInputs.overrideInputChannel(
                                tilt = StrokeInput.NO_TILT
                            ),
                        )
                    )
                ),
                listOf(
                    listOf(
                        Stroke(
                            makeBrush(family = family, size = 10f),
                            helper.octogonStylusInputs.overrideInputChannel(
                                orientation = StrokeInput.NO_ORIENTATION
                            ),
                        )
                    )
                ),
            ),
            "${brushName}_withMissingPressureTiltOrOrientationStylusData_appearReasonable",
        )
    }

    @Test
    fun stylusAndTouch_appearSimilar() {
        assertStrokesMatchGolden(
            listOf(
                listOf(
                    listOf(
                        Stroke(makeBrush(family = family, size = 10f), helper.cursiveStylusInputs)
                    )
                ),
                listOf(
                    listOf(
                        Stroke(makeBrush(family = family, size = 10f), helper.cursiveTouchInputs)
                    )
                ),
            ),
            "${brushName}_withStylusAndTouch_appearSimilar",
        )
    }

    @Test
    fun variedAlphaAndSize_appear() {
        assertStrokesMatchGolden(
            alphas.map { alpha ->
                sizes.map { size ->
                    listOf(
                        Stroke(
                            makeBrush(family = family, alpha = alpha, size = size),
                            helper.octogonStylusInputs,
                        )
                    )
                }
            },
            "${brushName}_withVariedAlphaAndSize",
        )
    }

    @Test
    fun documentAppears() {
        assertStrokesMatchGolden(
            listOf(
                listOf(
                    helper.helloWorldDocument.map { batch ->
                        Stroke(makeBrush(family = family, size = 5f), batch)
                    }
                )
            ),
            "${brushName}_documentAppears",
        )
    }

    @Test
    fun emojiHighlighterHasCorrectMiniEmojiTrailBehavior() {
        if (
            !(family.clientBrushFamilyId in
                listOf("heart_emoji_highlighter_1", "heart_emoji_highlighter_no_trail_1"))
        ) {
            return
        }
        val stroke =
            InProgressStroke().apply {
                start(makeBrush(family = family, size = 10f))
                enqueueInputs(helper.octogonStylusInputs, ImmutableStrokeInputBatch.EMPTY)
                updateShape(
                    helper.octogonStylusInputs
                        .get(helper.octogonStylusInputs.size - 1)
                        .elapsedTimeMillis
                )
            }
        assertStrokesMatchGolden(
            listOf(listOf(listOf(stroke.toImmutable()))),
            "emojiHighlighterHasCorrectMiniEmojiTrailBehavior_" + family.clientBrushFamilyId,
        )
    }

    /**
     * Creates a brush with the given specifications.
     *
     * @param family required brush family.
     * @param color default blue.
     * @param alpha int value ranging [0,255], default 255.
     * @param size float value, default 2f.
     */
    private fun makeBrush(
        family: BrushFamily,
        @ColorInt color: Int = TestColors.BLUE,
        alpha: Int = 255,
        size: Float = 2f,
        epsilon: Float = 0.1f,
    ) =
        Brush.createWithColorIntArgb(
            family = family,
            colorIntArgb = ColorUtils.setAlphaComponent(color, alpha),
            size = size,
            epsilon = epsilon,
        )
}
