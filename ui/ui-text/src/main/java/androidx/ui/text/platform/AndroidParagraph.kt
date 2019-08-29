/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.text.platform

import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.text.LayoutCompat.ALIGN_CENTER
import androidx.text.LayoutCompat.ALIGN_LEFT
import androidx.text.LayoutCompat.ALIGN_NORMAL
import androidx.text.LayoutCompat.ALIGN_OPPOSITE
import androidx.text.LayoutCompat.ALIGN_RIGHT
import androidx.text.LayoutCompat.DEFAULT_ALIGNMENT
import androidx.text.LayoutCompat.DEFAULT_JUSTIFICATION_MODE
import androidx.text.LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER
import androidx.text.LayoutCompat.DEFAULT_MAX_LINES
import androidx.text.LayoutCompat.JUSTIFICATION_MODE_INTER_WORD
import androidx.text.TextLayout
import androidx.text.selection.WordBoundary
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.toArgb
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Path
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Paragraph
import androidx.ui.text.ParagraphConstraints
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import java.util.Locale as JavaLocale

/**
 * Android specific implementation for [Paragraph]
 */
internal class AndroidParagraph constructor(
    val paragraphIntrinsics: AndroidParagraphIntrinsics,
    val maxLines: Int?,
    val ellipsis: Boolean?
) : Paragraph {

    constructor(
        text: String,
        style: TextStyle,
        paragraphStyle: ParagraphStyle,
        textStyles: List<AnnotatedString.Item<TextStyle>>,
        maxLines: Int?,
        ellipsis: Boolean?,
        typefaceAdapter: TypefaceAdapter,
        density: Density,
        layoutDirection: LayoutDirection
    ) : this(
        paragraphIntrinsics = AndroidParagraphIntrinsics(
            text = text,
            style = style,
            paragraphStyle = paragraphStyle,
            textStyles = textStyles,
            typefaceAdapter = typefaceAdapter,
            density = density,
            layoutDirection = layoutDirection
        ),
        maxLines = maxLines,
        ellipsis = ellipsis
    )

    /**
     * Initialized when [layout] function is called.
     */
    private var layout: TextLayout? = null

    override var width: Float = 0.0f
        get() = ensureLayout.let { field }

    override val height: Float
        get() = ensureLayout.let {
            // TODO(Migration/haoyuchang): Figure out a way to add bottomPadding properly
            val lineCount = it.lineCount
            if (maxLines != null &&
                maxLines >= 0 &&
                maxLines < lineCount
            ) {
                it.getLineBottom(maxLines - 1)
            } else {
                it.height.toFloat()
            }
        }

    override val maxIntrinsicWidth: Float
        get() = paragraphIntrinsics.maxIntrinsicWidth

    override val minIntrinsicWidth: Float
        get() = paragraphIntrinsics.minIntrinsicWidth

    override val firstBaseline: Float
        get() = ensureLayout.getLineBaseline(0)

    override val lastBaseline: Float
        get() = if (maxLines != null && maxLines >= 0 && maxLines < lineCount) {
            ensureLayout.getLineBaseline(maxLines - 1)
        } else {
            ensureLayout.getLineBaseline(lineCount - 1)
        }

    override val didExceedMaxLines: Boolean
        get() = ensureLayout.didExceedMaxLines

    @VisibleForTesting
    internal val textLocale: JavaLocale
        get() = paragraphIntrinsics.textPaint.textLocale

    override val lineCount: Int
        get() = ensureLayout.lineCount

    private val ensureLayout: TextLayout
        get() {
            return this.layout ?: throw java.lang.IllegalStateException(
                "layout() should be called first"
            )
        }

    @VisibleForTesting
    internal val charSequence: CharSequence
        get() = paragraphIntrinsics.charSequence

    @VisibleForTesting
    internal val textPaint: TextPaint
        get() = paragraphIntrinsics.textPaint

    override fun layout(constraints: ParagraphConstraints) {
        val paragraphStyle = paragraphIntrinsics.paragraphStyle

        val alignment = toLayoutAlign(paragraphStyle.textAlign)

        val maxLines = maxLines ?: DEFAULT_MAX_LINES
        val justificationMode = when (paragraphStyle.textAlign) {
            TextAlign.Justify -> JUSTIFICATION_MODE_INTER_WORD
            else -> DEFAULT_JUSTIFICATION_MODE
        }

        val lineSpacingMultiplier = paragraphStyle.lineHeight ?: DEFAULT_LINESPACING_MULTIPLIER

        val ellipsize = if (ellipsis == true) {
            TextUtils.TruncateAt.END
        } else {
            null
        }

        layout = TextLayout(
            charSequence = paragraphIntrinsics.charSequence,
            width = constraints.width,
            textPaint = textPaint,
            ellipsize = ellipsize,
            alignment = alignment,
            textDirectionHeuristic = paragraphIntrinsics.textDirectionHeuristic,
            lineSpacingMultiplier = lineSpacingMultiplier,
            maxLines = maxLines,
            justificationMode = justificationMode,
            layoutIntrinsics = paragraphIntrinsics.layoutIntrinsics
        )
        this.width = constraints.width
    }

    override fun getOffsetForPosition(position: PxPosition): Int {
        val line = ensureLayout.getLineForVertical(position.y.value.toInt())
        return ensureLayout.getOffsetForHorizontal(line, position.x.value)
    }

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect includes
     * the top, bottom, left and right of a character.
     */
    // TODO:(qqd) Implement RTL case.
    override fun getBoundingBox(offset: Int): Rect {
        val left = ensureLayout.getPrimaryHorizontal(offset)
        val right = ensureLayout.getPrimaryHorizontal(offset + 1)

        val line = ensureLayout.getLineForOffset(offset)
        val top = ensureLayout.getLineTop(line)
        val bottom = ensureLayout.getLineBottom(line)

        return Rect(top = top, bottom = bottom, left = left, right = right)
    }

    override fun getPathForRange(start: Int, end: Int): Path {
        if (start !in 0..end || end > charSequence.length) {
            throw AssertionError(
                "Start($start) or End($end) is out of Range(0..${charSequence.length})," +
                        " or start > end!"
            )
        }
        val path = android.graphics.Path()
        ensureLayout.getSelectionPath(start, end, path)
        return Path(path)
    }

    override fun getCursorRect(offset: Int): Rect {
        if (offset !in 0..charSequence.length) {
            throw AssertionError("offset($offset) is out of bounds (0,${charSequence.length}")
        }
        // TODO(nona): Support cursor drawable.
        val cursorWidth = 4.0f
        val layout = ensureLayout
        val horizontal = layout.getPrimaryHorizontal(offset)
        val line = layout.getLineForOffset(offset)

        return Rect(
            horizontal - 0.5f * cursorWidth,
            layout.getLineTop(line),
            horizontal + 0.5f * cursorWidth,
            layout.getLineBottom(line)
        )
    }

    private val wordBoundary: WordBoundary by lazy {
        WordBoundary(textLocale, ensureLayout.text)
    }

    override fun getWordBoundary(offset: Int): TextRange {
        return TextRange(wordBoundary.getWordStart(offset), wordBoundary.getWordEnd(offset))
    }

    override fun getLineLeft(lineIndex: Int): Float = ensureLayout.getLineLeft(lineIndex)

    override fun getLineRight(lineIndex: Int): Float = ensureLayout.getLineRight(lineIndex)

    override fun getLineBottom(lineIndex: Int): Float = ensureLayout.getLineBottom(lineIndex)

    override fun getLineHeight(lineIndex: Int): Float = ensureLayout.getLineHeight(lineIndex)

    override fun getLineWidth(lineIndex: Int): Float = ensureLayout.getLineWidth(lineIndex)

    override fun getLineForOffset(offset: Int): Int = ensureLayout.getLineForOffset(offset)

    override fun getPrimaryHorizontal(offset: Int): Float =
        ensureLayout.getPrimaryHorizontal(offset)

    override fun getSecondaryHorizontal(offset: Int): Float =
        ensureLayout.getSecondaryHorizontal(offset)

    override fun getParagraphDirection(offset: Int): TextDirection {
        val lineIndex = ensureLayout.getLineForOffset(offset)
        val direction = ensureLayout.getParagraphDirection(lineIndex)
        return if (direction == 1) TextDirection.Ltr else TextDirection.Rtl
    }

    override fun getBidiRunDirection(offset: Int): TextDirection {
        return if (ensureLayout.isRtlCharAt(offset)) TextDirection.Rtl else TextDirection.Ltr
    }

    /**
     * @return true if the given line is ellipsized, else false.
     */
    internal fun isEllipsisApplied(lineIndex: Int): Boolean =
        ensureLayout.isEllipsisApplied(lineIndex)

    override fun paint(canvas: Canvas) {
        val nativeCanvas = canvas.nativeCanvas
        if (didExceedMaxLines) {
            nativeCanvas.save()
            nativeCanvas.clipRect(0f, 0f, width, height)
        }
        ensureLayout.paint(nativeCanvas)
        if (didExceedMaxLines) {
            nativeCanvas.restore()
        }
    }
}

/**
 * Converts [TextAlign] into [TextLayout] alignment constants.
 */
private fun toLayoutAlign(align: TextAlign?): Int = when (align) {
    TextAlign.Left -> ALIGN_LEFT
    TextAlign.Right -> ALIGN_RIGHT
    TextAlign.Center -> ALIGN_CENTER
    TextAlign.Start -> ALIGN_NORMAL
    TextAlign.End -> ALIGN_OPPOSITE
    else -> DEFAULT_ALIGNMENT
}
