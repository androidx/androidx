/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.BitmapTextMeasure
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.layout.rememberRemoteTextMeasurer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alignByBaseline
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Value-producing paint operations (`TextAttribute`, `TimeAttribute`, `BitmapTextMeasure`) in the
 * embedded player. Each test draws a bar whose width is the produced value, so a value that is
 * never computed (0) draws nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerAttributesTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun textAttributeLengthIsComputed() {
        val bitmap = rule.renderDocument {
            val text = writer.textCreateId("hello")
            val length = writer.textAttribute(text, TextAttribute.TEXT_LENGTH)
            bar(writer.floatExpression(length, 10f, MUL))
        }

        assertBarWidth(bitmap, 50)
    }

    @Test
    fun timeAttributeOfALongIsComputed() {
        val june =
            LocalDateTime.of(2020, 6, 15, 12, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val bitmap = rule.renderDocument {
            val time = writer.addLong(june)
            val month = writer.timeAttribute(time, TimeAttribute.TIME_MONTH_VALUE)
            bar(writer.floatExpression(month, 10f, MUL))
        }

        // TIME_MONTH_VALUE is zero based, so June is 5.
        assertBarWidth(bitmap, 50)
    }

    @Test
    fun bitmapTextMeasureWidthIsComputed() {
        val bitmap = rule.renderDocument {
            val font =
                writer.addBitmapFont(arrayOf(BitmapFontData.Glyph("a", 0, 0, 0, 0, 0, 10, 10)))
            val text = writer.textCreateId("aaaa")
            bar(writer.bitmapTextMeasure(text, font, BitmapTextMeasure.MEASURE_WIDTH, 0f))
        }

        assertBarWidth(bitmap, 40)
    }

    @Test
    fun textAttributeTopAlignsTwoLineLabelToValueCapTopAndBaseline() {
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(200.dp).testTag("player")) { it() }
            }
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            val valueStyle = RemoteTextStyle(fontSize = 48.rsp)
            val labelStyle = RemoteTextStyle(fontSize = 14.rsp)
            val valueTop = textMeasurer.measurePaintBounds("7".rs, valueStyle).top
            val labelTop = textMeasurer.measurePaintBounds("7".rs, labelStyle).top

            RemoteRow {
                RemoteText(
                    text = "72".rs,
                    style = valueStyle,
                    color = Color.Red.rc,
                    modifier = RemoteModifier.alignByBaseline(),
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.alignByBaseline()
                            .graphicsLayer(translationY = valueTop - labelTop)
                ) {
                    RemoteText(
                        text = "AVG".rs,
                        style = labelStyle,
                        color = Color.Blue.rc,
                    )
                    RemoteText(
                        text = "BPM".rs,
                        style = labelStyle,
                        color = Color.Green.rc,
                        modifier = RemoteModifier.padding(top = (labelTop - valueTop).toRemoteDp()),
                    )
                }
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        val valueBounds = findColorYBounds(bitmap) { r, g, b -> r > 150 && g < 80 && b < 80 }
        val topLabelBounds = findColorYBounds(bitmap) { r, g, b -> b > 150 && r < 80 && g < 80 }
        val bottomLabelBounds = findColorYBounds(bitmap) { r, g, b -> g > 150 && r < 80 && b < 80 }

        assertThat(valueBounds).isNotNull()
        assertThat(topLabelBounds).isNotNull()
        assertThat(bottomLabelBounds).isNotNull()

        // The top of the first label line ("AVG") aligns with the top of the value's digits ("72").
        assertThat(topLabelBounds!!.first.toFloat()).isWithin(2f).of(valueBounds!!.first.toFloat())
        // The baseline (bottom of cap-height glyphs "BPM") aligns with the baseline of "72".
        assertThat(bottomLabelBounds!!.second.toFloat())
            .isWithin(2f)
            .of(valueBounds.second.toFloat())
    }

    @Test
    fun textAttributeTopAndAscentAlignTwoLineLabelWithoutBaselineAlignment() {
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(200.dp).testTag("player")) { it() }
            }
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            val valueStyle = RemoteTextStyle(fontSize = 48.rsp)
            val labelStyle = RemoteTextStyle(fontSize = 14.rsp)
            val valueLayout = textMeasurer.measureSingleLine("7".rs, valueStyle)
            val labelLayout = textMeasurer.measureSingleLine("7".rs, labelStyle)
            val valueBounds = textMeasurer.measurePaintBounds("7".rs, valueStyle)
            val labelBounds = textMeasurer.measurePaintBounds("7".rs, labelStyle)
            val avgOffsetY =
                (valueLayout.baseline + valueBounds.top) - (labelLayout.baseline + labelBounds.top)
            val bpmOffsetY = valueLayout.baseline - labelLayout.baseline

            RemoteRow {
                RemoteText(
                    text = "72".rs,
                    style = valueStyle,
                    color = Color.Red.rc,
                )
                RemoteBox {
                    RemoteText(
                        text = "AVG".rs,
                        style = labelStyle,
                        color = Color.Blue.rc,
                        modifier = RemoteModifier.graphicsLayer(translationY = avgOffsetY),
                    )
                    RemoteText(
                        text = "BPM".rs,
                        style = labelStyle,
                        color = Color.Green.rc,
                        modifier = RemoteModifier.padding(top = bpmOffsetY.toRemoteDp()),
                    )
                }
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        val valueBounds = findColorYBounds(bitmap) { r, g, b -> r > 150 && g < 80 && b < 80 }
        val topLabelBounds = findColorYBounds(bitmap) { r, g, b -> b > 150 && r < 80 && g < 80 }
        val bottomLabelBounds = findColorYBounds(bitmap) { r, g, b -> g > 150 && r < 80 && b < 80 }

        assertThat(valueBounds).isNotNull()
        assertThat(topLabelBounds).isNotNull()
        assertThat(bottomLabelBounds).isNotNull()

        assertThat(topLabelBounds!!.first.toFloat()).isWithin(2f).of(valueBounds!!.first.toFloat())
        assertThat(bottomLabelBounds!!.second.toFloat())
            .isWithin(2f)
            .of(valueBounds.second.toFloat())
    }

    @Test
    fun textAttributeFontMetricsAndGlyphBoundsGuideLinesAreOrdered() {
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(300.dp, 200.dp).testTag("player")) { it() }
            }
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            val sampleText = "jKbfg".rs
            val style = RemoteTextStyle(fontSize = 64.rsp)
            val layout = textMeasurer.measureSingleLine(sampleText, style)
            val fontMetrics = textMeasurer.measureFontMetrics(style)
            val glyphBounds = textMeasurer.measurePaintBounds(sampleText, style)

            val ascentY = layout.baseline + fontMetrics.ascent
            val topY = layout.baseline + glyphBounds.top
            val baselineY = layout.baseline
            val bottomY = layout.baseline + glyphBounds.bottom
            val descentY = layout.baseline + fontMetrics.descent

            RemoteBox(modifier = RemoteModifier.padding(20.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.drawWithContent {
                            drawContent()
                            drawRect(
                                paint =
                                    RemotePaint {
                                        color = Color.Gray.rc
                                        this.style = RemotePaintingStyle.Stroke
                                        strokeWidth = 2f.rf
                                    },
                                topLeft = RemoteOffset(glyphBounds.left, topY),
                                size = RemoteSize(glyphBounds.width, bottomY - topY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Magenta.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, ascentY),
                                end = RemoteOffset(this.width, ascentY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Cyan.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, topY),
                                end = RemoteOffset(this.width, topY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Red.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, baselineY),
                                end = RemoteOffset(this.width, baselineY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Yellow.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, bottomY),
                                end = RemoteOffset(this.width, bottomY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Green.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, descentY),
                                end = RemoteOffset(this.width, descentY),
                            )
                        }
                ) {
                    RemoteText(
                        text = sampleText,
                        style = style,
                        color = Color.White.rc,
                    )
                }
            }
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        // Magenta = ascent (R > 150, B > 150, G < 80)
        val ascentBounds = findColorYBounds(bitmap) { r, g, b -> r > 150 && g < 80 && b > 150 }
        // Cyan = glyph top (G > 150, B > 150, R < 80)
        val topBounds = findColorYBounds(bitmap) { r, g, b -> r < 80 && g > 150 && b > 150 }
        // Red = baseline (R > 150, G < 80, B < 80)
        val baselineBounds = findColorYBounds(bitmap) { r, g, b -> r > 150 && g < 80 && b < 80 }
        // Yellow = glyph bottom (R > 150, G > 150, B < 80)
        val bottomBounds = findColorYBounds(bitmap) { r, g, b -> r > 150 && g > 150 && b < 80 }
        // Green = descent (G > 150, R < 80, B < 80)
        val descentBounds = findColorYBounds(bitmap) { r, g, b -> r < 80 && g > 150 && b < 80 }

        assertThat(ascentBounds).isNotNull()
        assertThat(topBounds).isNotNull()
        assertThat(baselineBounds).isNotNull()
        assertThat(bottomBounds).isNotNull()
        assertThat(descentBounds).isNotNull()

        // Verify strict top-to-bottom ordering: ascent < glyphTop < baseline < glyphBottom <
        // descent
        assertThat(ascentBounds!!.first).isLessThan(topBounds!!.first)
        assertThat(topBounds.first).isLessThan(baselineBounds!!.first)
        assertThat(baselineBounds.first).isLessThan(bottomBounds!!.first)
        assertThat(bottomBounds.first).isLessThan(descentBounds!!.first)
    }

    @Test
    fun textAttributeLayoutWidthIncludesSpacesBeyondGlyphBounds() {
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(300.dp, 100.dp).testTag("player")) { it() }
            }
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            val style = RemoteTextStyle(fontSize = 36.rsp)
            val layout = textMeasurer.measureSingleLine("   Hello   ".rs, style)
            val glyphBounds = textMeasurer.measurePaintBounds("   Hello   ".rs, style)

            RemoteBox(
                modifier =
                    RemoteModifier.drawWithContent {
                        drawContent()
                        drawLine(
                            paint =
                                RemotePaint {
                                    color = Color.Red.rc
                                    strokeWidth = 4f.rf
                                },
                            start = RemoteOffset(0f.rf, 20f.rf),
                            end = RemoteOffset(glyphBounds.width, 20f.rf),
                        )
                        drawLine(
                            paint =
                                RemotePaint {
                                    color = Color.Green.rc
                                    strokeWidth = 4f.rf
                                },
                            start = RemoteOffset(0f.rf, 50f.rf),
                            end = RemoteOffset(layout.size.width, 50f.rf),
                        )
                    }
            ) {}
        }
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()

        var maxRedX = -1
        var maxGreenX = -1
        for (x in 0 until bitmap.width) {
            val redPixel = bitmap.getPixel(x, 20)
            if (AndroidColor.red(redPixel) > 150 && AndroidColor.green(redPixel) < 80) {
                maxRedX = x
            }
            val greenPixel = bitmap.getPixel(x, 50)
            if (AndroidColor.green(greenPixel) > 150 && AndroidColor.red(greenPixel) < 80) {
                maxGreenX = x
            }
        }

        assertThat(maxRedX).isGreaterThan(0)
        assertThat(maxGreenX).isGreaterThan(maxRedX)
    }

    @Test
    fun remoteTextMeasurerMatchesComposeUiTextMeasurer() {
        val sampleText = "Hello 72 jKbfg"
        var uiLayout: TextLayoutResult? = null
        val uiGlyphBounds = AndroidRect()
        var uiFontMetrics: Paint.FontMetrics? = null

        val document =
            rule.setRemoteContent(
                playComposableWrapper = { content ->
                    val density = LocalDensity.current
                    val uiMeasurer = rememberTextMeasurer()
                    uiLayout =
                        uiMeasurer.measure(
                            text = sampleText,
                            style =
                                TextStyle(
                                    fontSize = 32.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                ),
                            softWrap = false,
                            maxLines = 1,
                        )
                    val paint =
                        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = with(density) { 32.sp.toPx() }
                            typeface = Typeface.create(Typeface.DEFAULT, 400, false)
                        }
                    paint.getTextBounds(sampleText, 0, sampleText.length, uiGlyphBounds)
                    uiFontMetrics = paint.fontMetrics
                    Box(modifier = Modifier.size(300.dp, 120.dp)) { content() }
                }
            ) {
                val measurer = rememberRemoteTextMeasurer()
                val style = RemoteTextStyle(fontSize = 32.rsp)
                val layout = measurer.measureSingleLine(sampleText.rs, style)
                val fontMetrics = measurer.measureFontMetrics(style)
                val glyphBounds = measurer.measurePaintBounds(sampleText.rs, style)

                RemoteBox(
                    modifier =
                        RemoteModifier.drawWithContent {
                            drawContent()
                            fun export(name: String, value: RemoteFloat) {
                                document.setNamedVariable(
                                    value.id,
                                    "USER:$name",
                                    NamedVariable.FLOAT_TYPE,
                                )
                            }

                            export("width", layout.size.width)
                            export("height", layout.size.height)
                            export("baseline", layout.baseline)
                            export("ascent", fontMetrics.ascent)
                            export("descent", fontMetrics.descent)
                            export("glyphLeft", glyphBounds.left)
                            export("glyphTop", glyphBounds.top)
                            export("glyphRight", glyphBounds.right)
                            export("glyphBottom", glyphBounds.bottom)
                        }
                ) {
                    RemoteText(text = sampleText.rs, style = RemoteTextStyle(fontSize = 32.rsp))
                }
            }
        rule.waitForIdle()

        val composeUi = checkNotNull(uiLayout)
        val fontMetrics = checkNotNull(uiFontMetrics)
        val state = RcPlayerState(document)
        val uiLineWidth = composeUi.getLineRight(0) - composeUi.getLineLeft(0)

        // Layout-box properties match Compose UI TextLayoutResult.
        assertThat(state.floatState("width").value).isWithin(1.5f).of(uiLineWidth)
        assertThat(state.floatState("width").value)
            .isWithin(1.5f)
            .of(composeUi.size.width.toFloat())
        assertThat(state.floatState("height").value)
            .isWithin(1.5f)
            .of(composeUi.size.height.toFloat())
        assertThat(state.floatState("baseline").value).isWithin(1.5f).of(composeUi.firstBaseline)

        // Font metrics match TextLayoutResult line box (includeFontPadding = false) & Paint.
        assertThat(state.floatState("ascent").value)
            .isWithin(1.5f)
            .of(composeUi.getLineTop(0) - composeUi.firstBaseline)
        assertThat(state.floatState("ascent").value).isWithin(1.5f).of(fontMetrics.ascent)
        assertThat(state.floatState("descent").value)
            .isWithin(1.5f)
            .of(composeUi.getLineBottom(0) - composeUi.firstBaseline)
        assertThat(state.floatState("descent").value).isWithin(1.5f).of(fontMetrics.descent)

        // Tight glyph ink bounds match Android Paint.getTextBounds.
        assertThat(state.floatState("glyphLeft").value)
            .isWithin(1.5f)
            .of(uiGlyphBounds.left.toFloat())
        assertThat(state.floatState("glyphTop").value)
            .isWithin(1.5f)
            .of(uiGlyphBounds.top.toFloat())
        assertThat(state.floatState("glyphRight").value)
            .isWithin(1.5f)
            .of(uiGlyphBounds.right.toFloat())
        assertThat(state.floatState("glyphBottom").value)
            .isWithin(1.5f)
            .of(uiGlyphBounds.bottom.toFloat())
    }

    @Test
    fun textMeasurePaintIsScopedPerComponentAndDoesNotAccumulateBundles() {
        val document = rule.setRemoteContent {
            val measurer = rememberRemoteTextMeasurer()
            RemoteRow {
                RemoteBox(
                    modifier =
                        RemoteModifier.drawWithContent {
                            drawContent()
                            val largeLayout =
                                measurer.measureSingleLine(
                                    "MMMM".rs,
                                    RemoteTextStyle(fontSize = 72.rsp),
                                )
                            document.setNamedVariable(
                                largeLayout.size.width.id,
                                "USER:largeWidth",
                                NamedVariable.FLOAT_TYPE,
                            )
                        }
                ) {}
                RemoteBox(
                    modifier =
                        RemoteModifier.drawWithContent {
                            drawContent()
                            // Measure without setting textSize in this scope: must use default
                            // paint rather than leaking 72.rsp from the sibling RemoteBox.
                            val textId = document.textCreateId("MMMM")
                            val defaultWidthFloat =
                                document.textAttribute(textId, TextAttribute.MEASURE_WIDTH)
                            document.setNamedVariable(
                                Utils.idFromNan(defaultWidthFloat),
                                "USER:defaultWidth",
                                NamedVariable.FLOAT_TYPE,
                            )
                        }
                ) {}
            }
        }
        rule.waitForIdle()

        val state = RcPlayerState(document)
        assertThat(state.floatState("largeWidth").value).isGreaterThan(150f)
        assertThat(state.floatState("defaultWidth").value).isLessThan(80f)
        assertThat(state.preprocessed.textMeasurePaintSpecs.size).isEqualTo(2)
        var sawCustomSize = false
        var sawDefaultSize = false
        state.preprocessed.textMeasurePaintSpecs.forEach { _, spec ->
            val paint = spec.toComposeLocalPaint(state.remoteContext)
            if (paint.isTextSizeSet) sawCustomSize = true else sawDefaultSize = true
        }
        assertThat(sawCustomSize).isTrue()
        assertThat(sawDefaultSize).isTrue()
    }

    @Test
    fun repeatedPaintAndTextAttributeOpsResolveLatestTextSizeWithoutBundleAccumulation() {
        val bitmap = rule.renderDocument {
            val text = writer.textCreateId("MMMM")
            var lastWidth = 0f
            for (step in 1..200) {
                painter
                    .setColor(AndroidColor.BLUE)
                    .setStrokeWidth((step % 7 + 1).toFloat())
                    .setSweepGradient(
                        10f,
                        10f,
                        intArrayOf(AndroidColor.RED, AndroidColor.GREEN),
                        null,
                    )
                    .setLinearGradient(
                        0f,
                        0f,
                        20f,
                        20f,
                        intArrayOf(AndroidColor.BLUE, AndroidColor.YELLOW),
                        null,
                        Shader.TileMode.CLAMP,
                    )
                    .setTextSize(if (step == 200) 20f else 60f)
                    .commit()
                lastWidth = writer.textAttribute(text, TextAttribute.MEASURE_WIDTH)
            }
            painter.setShader(0).commit()
            // Scale difference to bar width: if 60f leaked instead of 20f, width > 150px.
            bar(lastWidth)
        }

        val expectedBounds = AndroidRect()
        Paint(Paint.ANTI_ALIAS_FLAG)
            .apply { textSize = 20f }
            .getTextBounds("MMMM", 0, 4, expectedBounds)
        val expectedWidth = expectedBounds.right - expectedBounds.left
        assertBarWidth(bitmap, expectedWidth)
    }

    private fun findColorYBounds(
        bitmap: Bitmap,
        predicate: (r: Int, g: Int, b: Int) -> Boolean,
    ): Pair<Int, Int>? {
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (
                    predicate(
                        AndroidColor.red(pixel),
                        AndroidColor.green(pixel),
                        AndroidColor.blue(pixel),
                    )
                ) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return if (minY <= maxY) minY to maxY else null
    }

    /** Draws a red bar from x = 0 to [width], across the middle of the player. */
    private fun RemoteComposeContextAndroid.bar(width: Float) {
        painter.setColor(AndroidColor.RED).commit()
        writer.drawRect(0f, 40f, width, 60f)
    }

    private fun assertBarWidth(bitmap: Bitmap, width: Int) {
        assertRed(bitmap, width - 5, 50)
        assertNotRed(bitmap, width + 5, 50)
    }
}
