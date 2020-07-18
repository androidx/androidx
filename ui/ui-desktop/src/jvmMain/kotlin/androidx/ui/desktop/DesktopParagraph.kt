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

import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.DesktopPath
import androidx.ui.graphics.Path
import androidx.ui.text.Paragraph
import androidx.ui.text.ParagraphConstraints
import androidx.ui.text.ParagraphIntrinsics
import androidx.ui.text.TextRange
import androidx.ui.text.style.ResolvedTextDirection
import org.jetbrains.skija.paragraph.RectHeightMode
import org.jetbrains.skija.paragraph.RectWidthMode

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
        get() = para.getLineMetrics().first().baseline.toFloat()

    override val lastBaseline: Float
        get() = para.getLineMetrics().last().baseline.toFloat()

    override val didExceedMaxLines: Boolean
        // TODO: support text ellipsize.
        get() = para.lineNumber < maxLines

    override val lineCount: Int
        get() = para.lineNumber.toInt()

    override val placeholderRects: List<Rect?> get() {
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
        println("Paragraph.getCursorRect $offset")
        return Rect(0.0f, 0.0f, 0.0f, 0.0f)
    }

    override fun getLineLeft(lineIndex: Int): Float {
        println("Paragraph.getLineLeft $lineIndex")
        return 0.0f
    }

    override fun getLineRight(lineIndex: Int): Float {
        println("Paragraph.getLineRight $lineIndex")
        return 0.0f
    }

    override fun getLineTop(lineIndex: Int): Float {
        println("Paragraph.getLineTop $lineIndex")
        return 0.0f
    }

    override fun getLineBottom(lineIndex: Int): Float {
        println("Paragraph.getLineBottom $lineIndex")
        return 0.0f
    }

    override fun getLineHeight(lineIndex: Int): Float {
        println("Paragraph.getLineHeight $lineIndex")
        return 0.0f
    }

    override fun getLineWidth(lineIndex: Int): Float {
        println("Paragraph.getLineWidth $lineIndex")
        return 0.0f
    }

    override fun getLineStart(lineIndex: Int): Int {
        println("Paragraph.getLineStart $lineIndex")
        return 0
    }

    override fun getLineEnd(lineIndex: Int): Int {
        println("Paragraph.getLineEnd $lineIndex")
        return 0
    }

    override fun getLineEllipsisOffset(lineIndex: Int): Int {
        println("Paragraph.getLineEllipsisOffset $lineIndex")
        return 0
    }

    override fun getLineEllipsisCount(lineIndex: Int): Int {
        println("Paragraph.getLineEllipsisCount $lineIndex")
        return 0
    }

    override fun getLineForOffset(offset: Int): Int {
        println("Paragraph.getLineForOffset $offset")
        return 0
    }

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        println("getHorizontalPosition $offset, $usePrimaryDirection")
        return 0.0f
    }

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        ResolvedTextDirection.Ltr

    override fun getOffsetForPosition(position: Offset): Int {
        return para.getGlyphPositionAtCoordinate(position.x, position.y).position
    }

    override fun getBoundingBox(offset: Int): Rect {
        println("getBoundingBox $offset")
        return Rect(0.0f, 0.0f, 0.0f, 0.0f)
    }

    override fun getWordBoundary(offset: Int): TextRange {
        println("getWordBoundary $offset")
        return TextRange(0, 0)
    }

    override fun paint(canvas: Canvas) {
        para.paint(canvas.nativeCanvas.skijaCanvas, 0.0f, 0.0f)
    }
}
