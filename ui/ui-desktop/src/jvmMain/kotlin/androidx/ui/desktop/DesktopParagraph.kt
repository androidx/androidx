/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DesktopPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toAndroidX
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphConstraints
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import org.jetbrains.skija.paragraph.LineMetrics
import org.jetbrains.skija.paragraph.RectHeightMode
import org.jetbrains.skija.paragraph.RectWidthMode
import kotlin.math.floor

internal class DesktopParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    val constraints: ParagraphConstraints
) : Paragraph {

    val paragraphIntrinsics = intrinsics as DesktopParagraphIntrinsics

    val para = paragraphIntrinsics.para

    init {
        para.layout(constraints.width)
    }

    override val width: Float
        get() = para.getMaxWidth()

    override val height: Float
        get() = para.getHeight()

    override val minIntrinsicWidth: Float
        get() = paragraphIntrinsics.minIntrinsicWidth

    override val maxIntrinsicWidth: Float
        get() = paragraphIntrinsics.maxIntrinsicWidth

    override val firstBaseline: Float
        get() = para.getLineMetrics().firstOrNull()?.run { baseline.toFloat() } ?: 0f

    override val lastBaseline: Float
        get() = para.getLineMetrics().lastOrNull()?.run { baseline.toFloat() } ?: 0f

    override val didExceedMaxLines: Boolean
        // TODO: support text ellipsize.
        get() = para.lineNumber < maxLines

    override val lineCount: Int
        get() = para.lineNumber.toInt()

    override val placeholderRects: List<Rect?>
        get() {
            println("Paragraph.placeholderRects")
            return listOf()
        }

    override fun getPathForRange(start: Int, end: Int): Path {
        val boxes = para.getRectsForRange(
            start,
            end,
            RectHeightMode.MAX,
            RectWidthMode.MAX
        )
        val path = DesktopPath()
        for (b in boxes) {
            path.internalPath.addRect(b.rect)
        }
        return path
    }

    override fun getCursorRect(offset: Int): Rect {
        val cursorWidth = 4.0f
        val horizontal = getHorizontalPosition(offset, true)
        val line = getLineForOffset(offset)

        return Rect(
            horizontal - 0.5f * cursorWidth,
            getLineTop(line),
            horizontal + 0.5f * cursorWidth,
            getLineBottom(line)
        )
    }

    override fun getLineLeft(lineIndex: Int): Float {
        println("Paragraph.getLineLeft $lineIndex")
        return 0.0f
    }

    override fun getLineRight(lineIndex: Int): Float {
        println("Paragraph.getLineRight $lineIndex")
        return 0.0f
    }

    override fun getLineTop(lineIndex: Int) =
        para.lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline - line.ascent).toFloat())
        } ?: 0f

    override fun getLineBottom(lineIndex: Int) =
        para.lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline + line.descent).toFloat())
        } ?: 0f

    private fun lineMetricsForOffset(offset: Int): LineMetrics? {
        val metrics = para.lineMetrics
        for (line in metrics) {
            if (offset <= line.endIndex) {
                return line
            }
        }
        return null
    }

    override fun getLineHeight(lineIndex: Int) = para.lineMetrics[lineIndex].height.toFloat()

    override fun getLineWidth(lineIndex: Int) = para.lineMetrics[lineIndex].width.toFloat()

    override fun getLineStart(lineIndex: Int) = para.lineMetrics[lineIndex].startIndex.toInt()

    override fun getLineEnd(lineIndex: Int) = para.lineMetrics[lineIndex].endIndex.toInt()

    override fun getLineEllipsisOffset(lineIndex: Int): Int {
        println("Paragraph.getLineEllipsisOffset $lineIndex")
        return 0
    }

    override fun getLineEllipsisCount(lineIndex: Int): Int {
        println("Paragraph.getLineEllipsisCount $lineIndex")
        return 0
    }

    override fun getLineForOffset(offset: Int) =
        lineMetricsForOffset(offset)?.run { lineNumber.toInt() }
            ?: 0

    override fun getLineForVerticalPosition(vertical: Float): Int {
        println("Paragraph.getLineForVerticalPosition $vertical")
        return 0
    }

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        val metrics = lineMetricsForOffset(offset)

        return when {
            metrics == null -> 0f
            metrics.startIndex.toInt() == offset || metrics.startIndex == metrics.endIndex -> 0f
            metrics.endIndex.toInt() == offset -> {
                para.getRectsForRange(offset - 1, offset, RectHeightMode.MAX, RectWidthMode.MAX)
                    .first()
                    .rect.right
            }
            else -> {
                para.getRectsForRange(
                    offset, offset + 1, RectHeightMode.MAX, RectWidthMode.MAX
                ).first().rect.left
            }
        }
    }

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getOffsetForPosition(position: Offset): Int {
        return para.getGlyphPositionAtCoordinate(position.x, position.y).position
    }

    override fun getBoundingBox(offset: Int) =
        para.getRectsForRange(
            offset, offset + 1, RectHeightMode.MAX, RectWidthMode
                .MAX
        ).first().rect.toAndroidX()

    override fun getWordBoundary(offset: Int): TextRange {
        println("Paragraph.getWordBoundary $offset")
        return TextRange(0, 0)
    }

    override fun paint(canvas: Canvas) {
        para.paint(canvas.nativeCanvas.skijaCanvas, 0.0f, 0.0f)
    }
}