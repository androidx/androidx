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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.layout.RemoteTextMeasurer
import androidx.compose.remote.creation.compose.layout.rememberRemoteTextMeasurer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alignByBaseline
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteColor
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
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val GlyphBoundsOrange = Color(0xFFFF9800)

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerTextScreenshotTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteText_measureTop_alignsTwoLineLabelToValueCapTopAndBaseline() {
        val width = 540
        val height = 260

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            val valueStyle = RemoteTextStyle(fontSize = 48.rsp)
            val labelStyle = RemoteTextStyle(fontSize = 14.rsp)
            val tagStyle = RemoteTextStyle(fontSize = 9.rsp)
            val valueLayout = textMeasurer.measureSingleLine("7".rs, valueStyle)
            val valueFontMetrics = textMeasurer.measureFontMetrics(valueStyle)
            val valueBounds = textMeasurer.measurePaintBounds("7".rs, valueStyle)
            val labelBounds = textMeasurer.measurePaintBounds("7".rs, labelStyle)
            val tagLayout = textMeasurer.measureSingleLine("Ag".rs, tagStyle)
            val tagBounds = textMeasurer.measurePaintBounds("Ag".rs, tagStyle)
            val tagCenterY = tagLayout.baseline + (tagBounds.top + tagBounds.bottom) * 0.5f.rf

            val valueTop = valueBounds.top
            val labelTop = labelBounds.top
            val ascentY = valueLayout.baseline + valueFontMetrics.ascent
            val capTopY = valueLayout.baseline + valueTop
            val baselineY = valueLayout.baseline
            val lineEndX = 250f.rf
            val tagX = 258f.rf

            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize().padding(horizontal = 8.rdp, vertical = 12.rdp)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth().drawWithContent {
                            drawContent()
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Magenta.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, ascentY),
                                end = RemoteOffset(lineEndX, ascentY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Cyan.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, capTopY),
                                end = RemoteOffset(lineEndX, capTopY),
                            )
                            drawLine(
                                paint =
                                    RemotePaint {
                                        color = Color.Yellow.rc
                                        strokeWidth = 2f.rf
                                    },
                                start = RemoteOffset(0f.rf, baselineY),
                                end = RemoteOffset(lineEndX, baselineY),
                            )
                        }
                ) {
                    RemoteRow {
                        RemoteText(
                            text = "72".rs,
                            style = valueStyle,
                            color = Color.White.rc,
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
                                color = Color.Cyan.rc,
                            )
                            RemoteText(
                                text = "BPM".rs,
                                style = labelStyle,
                                color = Color.Yellow.rc,
                                modifier =
                                    RemoteModifier.padding(
                                        top = (labelTop - valueTop).toRemoteDp()
                                    ),
                            )
                        }
                    }

                    LineTag(
                        label = "ascent",
                        color = Color.Magenta.rc,
                        style = tagStyle,
                        x = tagX,
                        y = ascentY - tagCenterY,
                    )
                    LineTag(
                        label = "glyphTop",
                        color = Color.Cyan.rc,
                        style = tagStyle,
                        x = tagX,
                        y = capTopY - tagCenterY,
                    )
                    LineTag(
                        label = "firstBaseline",
                        color = Color.Yellow.rc,
                        style = tagStyle,
                        x = tagX,
                        y = baselineY - tagCenterY,
                    )
                }
            }
        }
    }

    @Test
    fun remoteText_fontMetricsAndGlyphBoundsGuideLines() {
        val width = 800
        val height = 250

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            RemoteFontMetricsAndBoundsSample(
                textMeasurer = textMeasurer,
                sampleText = "jKbfg 72",
                style = RemoteTextStyle(fontSize = 44.rsp),
            )
        }
    }

    @Test
    fun remoteTextMeasurer_comparedToComposeUiTextMeasurer() {
        val width = 800
        val height = 500
        val sampleText = "jKbfg 72"

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = { remoteContent ->
                val density = LocalDensity.current
                val uiMeasurer = rememberTextMeasurer()
                val uiStyle =
                    TextStyle(
                        fontSize = 44.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    )
                val tagUiStyle =
                    TextStyle(
                        fontSize = 8.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    )
                val captionStyle =
                    TextStyle(
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    )
                val uiLayout =
                    uiMeasurer.measure(
                        text = sampleText,
                        style = uiStyle,
                        softWrap = false,
                        maxLines = 1,
                    )
                val glyphRect =
                    remember(sampleText, density) {
                        val paint =
                            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = with(density) { 44.sp.toPx() }
                                typeface = Typeface.create(Typeface.DEFAULT, 400, false)
                            }
                        AndroidRect().also {
                            paint.getTextBounds(sampleText, 0, sampleText.length, it)
                        }
                    }

                val uiTagRef = uiMeasurer.measure("Ag", style = tagUiStyle)
                val uiTagCenterY = uiTagRef.size.height / 2f
                val paintRightLabelWidth =
                    uiMeasurer.measure("Paint.right", style = tagUiStyle).size.width

                val uiSize = Size(uiLayout.size.width.toFloat(), uiLayout.size.height.toFloat())
                val lineTop = uiLayout.getLineTop(0)
                val baseline = uiLayout.firstBaseline
                val lineBottom = uiLayout.getLineBottom(0)
                val paintTopY = baseline + glyphRect.top
                val paintBottomY = baseline + glyphRect.bottom
                val lineEndX = uiSize.width + 16f
                val tagX = (uiSize.width + 24f).roundToInt()
                val boundsLabelY = (lineBottom + 6f).roundToInt()

                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(Color.Black)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Compose UI TextMeasurer + Paint.getTextBounds",
                            style = captionStyle,
                        )
                        Box(
                            modifier =
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp).drawWithContent {
                                    drawContent()
                                    drawRect(
                                        color = Color.LightGray,
                                        topLeft = Offset.Zero,
                                        size = uiSize,
                                        style = Stroke(width = 2f),
                                    )
                                    drawRect(
                                        color = GlyphBoundsOrange,
                                        topLeft = Offset(glyphRect.left.toFloat(), paintTopY),
                                        size =
                                            Size(
                                                glyphRect.width().toFloat(),
                                                glyphRect.height().toFloat(),
                                            ),
                                        style = Stroke(width = 2f),
                                    )
                                    drawLine(
                                        color = Color.Magenta,
                                        start = Offset(0f, lineTop),
                                        end = Offset(lineEndX, lineTop),
                                        strokeWidth = 2f,
                                    )
                                    drawLine(
                                        color = Color.Cyan,
                                        start = Offset(0f, paintTopY),
                                        end = Offset(lineEndX, paintTopY),
                                        strokeWidth = 2f,
                                    )
                                    drawLine(
                                        color = Color.Red,
                                        start = Offset(0f, baseline),
                                        end = Offset(lineEndX, baseline),
                                        strokeWidth = 2f,
                                    )
                                    drawLine(
                                        color = Color.Yellow,
                                        start = Offset(0f, paintBottomY),
                                        end = Offset(lineEndX, paintBottomY),
                                        strokeWidth = 2f,
                                    )
                                    drawLine(
                                        color = Color.Green,
                                        start = Offset(0f, lineBottom),
                                        end = Offset(lineEndX, lineBottom),
                                        strokeWidth = 2f,
                                    )
                                }
                        ) {
                            Text(text = sampleText, color = Color.White, style = uiStyle)
                            Text(
                                text = "lineTop",
                                color = Color.Magenta,
                                style = tagUiStyle,
                                modifier =
                                    Modifier.offset {
                                        IntOffset(tagX, (lineTop - uiTagCenterY).roundToInt())
                                    },
                            )
                            Text(
                                text = "Paint.top",
                                color = Color.Cyan,
                                style = tagUiStyle,
                                modifier =
                                    Modifier.offset {
                                        IntOffset(tagX, (paintTopY - uiTagCenterY).roundToInt())
                                    },
                            )
                            Text(
                                text = "firstBaseline",
                                color = Color.Red,
                                style = tagUiStyle,
                                modifier =
                                    Modifier.offset {
                                        IntOffset(tagX, (baseline - uiTagCenterY).roundToInt())
                                    },
                            )
                            Row(
                                modifier =
                                    Modifier.offset {
                                        IntOffset(
                                            tagX,
                                            ((paintBottomY + lineBottom) * 0.5f - uiTagCenterY)
                                                .roundToInt(),
                                        )
                                    },
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Paint.bottom",
                                    color = Color.Yellow,
                                    style = tagUiStyle,
                                )
                                Text(text = "lineBottom", color = Color.Green, style = tagUiStyle)
                                Text(text = "size", color = Color.LightGray, style = tagUiStyle)
                            }
                            Text(
                                text = "Paint.left",
                                color = GlyphBoundsOrange,
                                style = tagUiStyle,
                                modifier =
                                    Modifier.offset { IntOffset(glyphRect.left, boundsLabelY) },
                            )
                            Text(
                                text = "Paint.right",
                                color = GlyphBoundsOrange,
                                style = tagUiStyle,
                                modifier =
                                    Modifier.offset {
                                        IntOffset(
                                            glyphRect.right - paintRightLabelWidth,
                                            boundsLabelY,
                                        )
                                    },
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = "RemoteTextMeasurer (RcPlayer)", style = captionStyle)
                        Box(modifier = Modifier.fillMaxSize()) { remoteContent() }
                    }
                }
            },
        ) {
            val textMeasurer = rememberRemoteTextMeasurer()
            RemoteFontMetricsAndBoundsSample(
                textMeasurer = textMeasurer,
                sampleText = sampleText,
                style = RemoteTextStyle(fontSize = 44.rsp),
                applyOuterPadding = false,
            )
        }
    }
}

@RemoteComposable
@Composable
private fun RemoteFontMetricsAndBoundsSample(
    textMeasurer: RemoteTextMeasurer,
    sampleText: String,
    style: RemoteTextStyle,
    applyOuterPadding: Boolean = true,
) {
    val tagStyle = RemoteTextStyle(fontSize = 8.rsp)
    val layout = textMeasurer.measureSingleLine(sampleText.rs, style)
    val fontMetrics = textMeasurer.measureFontMetrics(style)
    val glyphBounds = textMeasurer.measurePaintBounds(sampleText.rs, style)
    val tagLayout = textMeasurer.measureSingleLine("Ag".rs, tagStyle)
    val tagBounds = textMeasurer.measurePaintBounds("Ag".rs, tagStyle)
    val tagCenterY = tagLayout.baseline + (tagBounds.top + tagBounds.bottom) * 0.5f.rf

    val ascentY = layout.baseline + fontMetrics.ascent
    val topY = layout.baseline + glyphBounds.top
    val baselineY = layout.baseline
    val bottomY = layout.baseline + glyphBounds.bottom
    val descentY = layout.baseline + fontMetrics.descent
    val lineEndX = layout.size.width + 16f.rf
    val tagX = layout.size.width + 24f.rf
    val boundsLabelY = descentY + 6f.rf

    val outerModifier =
        if (applyOuterPadding) {
            RemoteModifier.fillMaxSize().padding(horizontal = 8.rdp, vertical = 12.rdp)
        } else {
            RemoteModifier.fillMaxSize().padding(horizontal = 4.rdp)
        }

    RemoteBox(modifier = outerModifier) {
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth().drawWithContent {
                    drawContent()
                    drawRect(
                        paint =
                            RemotePaint {
                                color = Color.LightGray.rc
                                this.style = RemotePaintingStyle.Stroke
                                strokeWidth = 2f.rf
                            },
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = layout.size,
                    )
                    drawRect(
                        paint =
                            RemotePaint {
                                color = GlyphBoundsOrange.rc
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
                        end = RemoteOffset(lineEndX, ascentY),
                    )
                    drawLine(
                        paint =
                            RemotePaint {
                                color = Color.Cyan.rc
                                strokeWidth = 2f.rf
                            },
                        start = RemoteOffset(0f.rf, topY),
                        end = RemoteOffset(lineEndX, topY),
                    )
                    drawLine(
                        paint =
                            RemotePaint {
                                color = Color.Red.rc
                                strokeWidth = 2f.rf
                            },
                        start = RemoteOffset(0f.rf, baselineY),
                        end = RemoteOffset(lineEndX, baselineY),
                    )
                    drawLine(
                        paint =
                            RemotePaint {
                                color = Color.Yellow.rc
                                strokeWidth = 2f.rf
                            },
                        start = RemoteOffset(0f.rf, bottomY),
                        end = RemoteOffset(lineEndX, bottomY),
                    )
                    drawLine(
                        paint =
                            RemotePaint {
                                color = Color.Green.rc
                                strokeWidth = 2f.rf
                            },
                        start = RemoteOffset(0f.rf, descentY),
                        end = RemoteOffset(lineEndX, descentY),
                    )
                }
        ) {
            RemoteText(
                text = sampleText.rs,
                style = style,
                color = Color.White.rc,
            )

            LineTag(
                label = "ascent",
                color = Color.Magenta.rc,
                style = tagStyle,
                x = tagX,
                y = ascentY - tagCenterY,
            )
            LineTag(
                label = "glyphTop",
                color = Color.Cyan.rc,
                style = tagStyle,
                x = tagX,
                y = topY - tagCenterY,
            )
            LineTag(
                label = "firstBaseline",
                color = Color.Red.rc,
                style = tagStyle,
                x = tagX,
                y = baselineY - tagCenterY,
            )
            // glyphBottom and descent overlap (~2px apart on "jKbfg 72"), so place them in a row.
            RemoteRow(
                modifier =
                    RemoteModifier.graphicsLayer(
                        translationX = tagX,
                        translationY = (bottomY + descentY) * 0.5f.rf - tagCenterY,
                    ),
                horizontalArrangement = RemoteArrangement.spacedBy(6.rdp),
            ) {
                RemoteText(text = "glyphBottom".rs, style = tagStyle, color = Color.Yellow.rc)
                RemoteText(text = "descent".rs, style = tagStyle, color = Color.Green.rc)
                RemoteText(text = "size".rs, style = tagStyle, color = Color.LightGray.rc)
            }
            LineTag(
                label = "glyphLeft",
                color = GlyphBoundsOrange.rc,
                style = tagStyle,
                x = glyphBounds.left,
                y = boundsLabelY,
            )
            val rightLabelWidth =
                textMeasurer.measureSingleLine("glyphRight".rs, tagStyle).size.width
            LineTag(
                label = "glyphRight",
                color = GlyphBoundsOrange.rc,
                style = tagStyle,
                x = glyphBounds.right - rightLabelWidth,
                y = boundsLabelY,
            )
        }
    }
}

@RemoteComposable
@Composable
private fun LineTag(
    label: String,
    color: RemoteColor,
    style: RemoteTextStyle,
    x: RemoteFloat,
    y: RemoteFloat,
) {
    RemoteText(
        text = label.rs,
        style = style,
        color = color,
        modifier = RemoteModifier.graphicsLayer(translationX = x, translationY = y),
    )
}
