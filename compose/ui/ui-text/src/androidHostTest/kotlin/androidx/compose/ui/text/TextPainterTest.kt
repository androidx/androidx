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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(JUnit4::class)
class TextPainterTest {
    @Test
    fun textPainter_paint_resolvesColorBrushAlphaDefaultsAndClipping() {
        // 1. Default TextStyle: Color.Unspecified -> Color.Black, null shadow/decoration/drawStyle
        // -> None/Fill, no overflow
        val canvas1 = mock<Canvas>()
        val para1 = mock<Paragraph> { on { height } doReturn 20f }
        TextPainter.paint(
            canvas1,
            createTestTextLayoutResult(
                style = TextStyle(),
                paragraph = para1,
                size = IntSize(200, 100),
                overflow = TextOverflow.Clip,
            ),
        )
        verify(canvas1, never()).clipRect(Rect(Offset.Zero, Size(200f, 100f)))
        verify(canvas1, times(1)).save()
        verify(canvas1, times(1)).restore()
        verify(para1)
            .paint(
                canvas1,
                Color.Black,
                Shadow.None,
                TextDecoration.None,
                Fill,
                DrawScope.DefaultBlendMode,
            )

        // 2. Custom Color, Shadow, TextDecoration, Stroke with visual overflow and
        // TextOverflow.Clip -> clips & restores
        val canvas2 = mock<Canvas>()
        val para2 = mock<Paragraph> { on { height } doReturn 20f }
        val customShadow = Shadow(color = Color.Green, offset = Offset(1f, 2f), blurRadius = 3f)
        val customStroke = Stroke(width = 4f)
        TextPainter.paint(
            canvas2,
            createTestTextLayoutResult(
                style =
                    TextStyle(
                        color = Color.Red,
                        shadow = customShadow,
                        textDecoration = TextDecoration.Underline,
                        drawStyle = customStroke,
                    ),
                paragraph = para2,
                size =
                    IntSize(
                        50,
                        40,
                    ), // smaller than MultiParagraph width (200) -> hasVisualOverflow = true
                overflow = TextOverflow.Clip,
            ),
        )
        verify(canvas2).clipRect(0f, 0f, 50f, 40f)
        // 1 save/restore from clipping + 1 save/restore from MultiParagraph.paint
        verify(canvas2, times(2)).save()
        verify(canvas2, times(2)).restore()
        verify(para2)
            .paint(
                canvas2,
                Color.Red,
                customShadow,
                TextDecoration.Underline,
                customStroke,
                DrawScope.DefaultBlendMode,
            )

        // 3. SolidColor brush with alpha takes the color branch with modulated alpha, and
        // TextOverflow.Visible skips clipping
        val canvas3 = mock<Canvas>()
        val para3 = mock<Paragraph> { on { height } doReturn 20f }
        TextPainter.paint(
            canvas3,
            createTestTextLayoutResult(
                style = TextStyle(brush = SolidColor(Color.Blue), alpha = 0.5f),
                paragraph = para3,
                size = IntSize(50, 40), // hasVisualOverflow = true, but overflow = Visible
                overflow = TextOverflow.Visible,
            ),
        )
        verify(canvas3, never()).clipRect(Rect(Offset.Zero, Size(50f, 40f)))
        verify(canvas3, times(1)).save()
        verify(canvas3, times(1)).restore()
        verify(para3)
            .paint(
                canvas3,
                Color.Blue.copy(alpha = 0.5f),
                Shadow.None,
                TextDecoration.None,
                Fill,
                DrawScope.DefaultBlendMode,
            )

        // 4. ShaderBrush with explicit alpha takes the brush branch and forwards brush and alpha
        val canvas4 = mock<Canvas>()
        val para4 = mock<Paragraph> { on { height } doReturn 20f }
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        TextPainter.paint(
            canvas4,
            createTestTextLayoutResult(
                style = TextStyle(brush = shaderBrush, alpha = 0.35f),
                paragraph = para4,
                size = IntSize(200, 100),
                overflow = TextOverflow.Clip,
            ),
        )
        verify(para4)
            .paint(
                canvas4,
                shaderBrush,
                0.35f,
                Shadow.None,
                TextDecoration.None,
                Fill,
                DrawScope.DefaultBlendMode,
            )
    }

    @Test
    fun drawText_brush_resolvesSpanStyleFallbacksAndExplicitOverrides() {
        val styleBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val overrideBrush = Brush.linearGradient(listOf(Color.Green, Color.Yellow))
        val styleShadow = Shadow(color = Color.Magenta, offset = Offset(1f, 2f), blurRadius = 3f)
        val overrideShadow = Shadow(color = Color.Cyan, offset = Offset(4f, 5f), blurRadius = 6f)
        val styleStroke = Stroke(width = 2f)
        val overrideStroke = Stroke(width = 5f)
        val drawScope = CanvasDrawScope()

        // 1. Null/NaN overrides fall back to textLayoutResult.layoutInput.style.spanStyle
        val canvas1 = mock<Canvas>()
        val para1 = mock<Paragraph> { on { height } doReturn 20f }
        val result1 =
            createTestTextLayoutResult(
                style =
                    TextStyle(
                        brush = styleBrush,
                        alpha = 0.4f,
                        shadow = styleShadow,
                        textDecoration = TextDecoration.Underline,
                        drawStyle = styleStroke,
                    ),
                paragraph = para1,
                size = IntSize(200, 100),
                overflow = TextOverflow.Clip,
            )
        drawScope.draw(Density(1f), LayoutDirection.Ltr, canvas1, Size(200f, 100f)) {
            drawText(textLayoutResult = result1, brush = overrideBrush)
        }
        verify(para1)
            .paint(
                canvas1,
                overrideBrush,
                0.4f,
                styleShadow,
                TextDecoration.Underline,
                styleStroke,
                DrawScope.DefaultBlendMode,
            )

        // 2. Explicit overrides supersede spanStyle attributes
        val canvas2 = mock<Canvas>()
        val para2 = mock<Paragraph> { on { height } doReturn 20f }
        val result2 =
            createTestTextLayoutResult(
                style = result1.layoutInput.style,
                paragraph = para2,
                size = IntSize(200, 100),
                overflow = TextOverflow.Clip,
            )
        drawScope.draw(Density(1f), LayoutDirection.Ltr, canvas2, Size(200f, 100f)) {
            drawText(
                textLayoutResult = result2,
                brush = overrideBrush,
                alpha = 0.85f,
                shadow = overrideShadow,
                textDecoration = TextDecoration.LineThrough,
                drawStyle = overrideStroke,
            )
        }
        verify(para2)
            .paint(
                canvas2,
                overrideBrush,
                0.85f,
                overrideShadow,
                TextDecoration.LineThrough,
                overrideStroke,
                DrawScope.DefaultBlendMode,
            )
    }

    private fun createTestMultiParagraph(
        paragraph: Paragraph,
        overflow: TextOverflow = TextOverflow.Clip,
    ): MultiParagraph {
        val intrinsics =
            MultiParagraphIntrinsics(
                annotatedString = AnnotatedString(""),
                placeholders = emptyList(),
                infoList = emptyList(),
            )
        val multiParagraph =
            MultiParagraph(
                intrinsics = intrinsics,
                constraints = Constraints(maxWidth = 200, maxHeight = 100),
                maxLines = Int.MAX_VALUE,
                overflow = overflow,
            )
        @Suppress("UNCHECKED_CAST")
        (multiParagraph.paragraphInfoList as MutableList<ParagraphInfo>).add(
            ParagraphInfo(
                paragraph = paragraph,
                startIndex = 0,
                endIndex = 5,
                startLineIndex = 0,
                endLineIndex = 1,
                top = 0f,
                bottom = 20f,
            )
        )
        return multiParagraph
    }

    private fun createTestTextLayoutResult(
        style: TextStyle,
        paragraph: Paragraph,
        size: IntSize,
        overflow: TextOverflow,
    ): TextLayoutResult {
        val multiParagraph = createTestMultiParagraph(paragraph, overflow)
        val layoutInput =
            TextLayoutInput(
                text = AnnotatedString("hello"),
                style = style,
                placeholders = emptyList(),
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = overflow,
                density = Density(1f),
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = mock<FontFamily.Resolver>(),
                defaultLocaleList = LocaleList("en-US"),
                constraints = Constraints(maxWidth = 200, maxHeight = 100),
            )
        return TextLayoutResult(
            layoutInput = layoutInput,
            multiParagraph = multiParagraph,
            size = size,
        )
    }
}
