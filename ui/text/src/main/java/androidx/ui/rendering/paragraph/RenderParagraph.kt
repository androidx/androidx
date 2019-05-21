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

package androidx.ui.rendering.paragraph

import androidx.annotation.VisibleForTesting
import androidx.ui.core.Constraints
import androidx.ui.core.IntPxSize
import androidx.ui.core.constrain
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextPosition
import androidx.ui.painting.BlendMode
import androidx.ui.painting.Canvas
import androidx.ui.graphics.Color
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.painting.Shader
import androidx.ui.painting.TextPainter
import androidx.ui.painting.TextSpan
import androidx.ui.painting.basictypes.RenderComparison
import androidx.ui.services.text_editing.TextRange
/*import androidx.ui.semantics.SemanticsConfiguration*/
import androidx.ui.services.text_editing.TextSelection

/** The default selection color if none is specified. */
private val DEFAULT_SELECTION_COLOR = Color(0x6633B5E5)
/**
 * A render object that displays a paragraph of text
 *
 * Creates a paragraph render object.
 *
 * @param text The text to display.
 *
 * @param textAlign How the text should be aligned horizontally.
 *
 * @param textDirection The directionality of the text.
 *   This decides how the [TextAlign.start], [TextAlign.end], and [TextAlign.justify] values of
 *   [textAlign] are interpreted.
 *
 * @param softWrap Whether the text should break at soft line breaks.
 *   If false, the glyphs in the text will be positioned as if there was unlimited horizontal space.
 *   If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
 *
 * @param overflow How visual overflow should be handled.
 *
 * @param textScaleFactor The number of font pixels for each logical pixel.
 *   For example, if the text scale factor is 1.5, text will be 50% larger than the specified font
 *   size.
 *
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap].
 *   The value may be null. If it is not null, then it must be greater than zero.
 *
 * @param selectionColor The highlight color when the text is selected.
 */

class RenderParagraph(
    text: TextSpan,
    textAlign: TextAlign = TextAlign.START,
    textDirection: TextDirection,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    textScaleFactor: Float = 1.0f,
    maxLines: Int? = null,
    selectionColor: Color = DEFAULT_SELECTION_COLOR
) {
    @VisibleForTesting
    // TODO(migration/qqd): Should be internal. Removed internal due to hacking reason for now.
    var textPainter: TextPainter

    private var overflowShader: Shader? = null
    private var hasVisualOverflow = false
    private var constraints: Constraints? = null
    internal var selectionPaint: Paint

    init {
        assert(maxLines == null || maxLines > 0)
        textPainter = TextPainter(
            text = text,
            textAlign = textAlign,
            textDirection = textDirection,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines,
            ellipsis = overflow == TextOverflow.Ellipsis
        )
        selectionPaint = Paint()
        selectionPaint.color = selectionColor
    }

    var text: TextSpan
        set(value) {
            when (textPainter.text!!.compareTo(value)) {
                RenderComparison.IDENTICAL, RenderComparison.METADATA -> return
                RenderComparison.PAINT -> {
                    textPainter.text = value
                    // markNeedsPaint()
                }
                RenderComparison.LAYOUT -> {
                    textPainter.text = value
                    overflowShader = null
                    // markNeedsLayout()
                }
            }
        }
        get() {
            return textPainter.text!!
        }

    var textAlign: TextAlign
        set(value) {
            if (textPainter.textAlign == value) return
            textPainter.textAlign = value
            // markNeedsPaint()
        }
        get() {
            return textPainter.textAlign
        }

    var textDirection: TextDirection
        set(value) {
            if (textPainter.textDirection == value) return
            textPainter.textDirection = value
            // markNeedsLayout()
        }
        get() {
            return textPainter.textDirection!!
        }

    var softWrap: Boolean = softWrap
        set(value) {
            if (field == value) return
            field = value
            // markNeedsLayout()
        }

    var overflow: TextOverflow = overflow
        set(value) {
            if (field == value) return
            field = value
            textPainter.ellipsis = value === TextOverflow.Ellipsis
            // markNeedsLayout()
        }

    var textScaleFactor: Float
        set(value) {
            if (textPainter.textScaleFactor == value) return
            textPainter.textScaleFactor = value
            overflowShader = null
            // markNeedsLayout()
        }
        get() {
            return textPainter.textScaleFactor
        }

    var maxLines: Int?
        set(value) {
            assert(value == null || value > 0)
            if (textPainter.maxLines == value) return
            textPainter.maxLines = value
            overflowShader = null
            // markNeedsLayout()
        }
        get() {
            return textPainter.maxLines
        }

    var size: Size = Size(0.0f, 0.0f)

    val width: Float
        get() = textPainter.width

    val height: Float
        get() = textPainter.height

    // TODO(b/130800659): IR compiler bug, should be internal
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    fun layoutText(minWidth: Float = 0.0f, maxWidth: Float = Float.POSITIVE_INFINITY) {
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        textPainter.layout(
            minWidth = minWidth, maxWidth =
            if (widthMatters) maxWidth else Float.POSITIVE_INFINITY
        )
    }

    internal fun layoutTextWithConstraints(constraints: Constraints) {
        layoutText(
            minWidth = constraints.minWidth.value.toFloat(),
            maxWidth = constraints.maxWidth.value.toFloat()
        )
    }

    fun computeMinIntrinsicWidth(): Float {
        layoutText()
        return textPainter.minIntrinsicWidth
    }

    fun computeMaxIntrinsicWidth(): Float {
        layoutText()
        return textPainter.maxIntrinsicWidth
    }

    @VisibleForTesting
    internal fun computeIntrinsicHeight(width: Float): Float {
        layoutText(minWidth = width, maxWidth = width)
        return textPainter.height
    }

    // Height computation functions were unused, therefore commenting out.
    // we will go over them when we decide on the final state of render paragraph class.
//    fun computeMinIntrinsicHeight(width: Float): Float {
//        return computeIntrinsicHeight(width)
//    }
//
//    fun computeMaxIntrinsicHeight(width: Float): Float {
//        return computeIntrinsicHeight(width)
//    }

//    public override fun hitTestSelf(position: Offset): Boolean = true

    // TODO(Migration/qqd): handleEvent is not implemented in RenderBox, because RenderObject does
    // not implement HitTestTarget
//    override fun handleEvent(event: PointerEvent, entry: BoxHitTestEntry) {
//        assert(debugHandleEvent(event, entry))
//        if (event is! PointerDownEvent) return
//        layoutTextWithConstraints(constraints)
//        val offset = entry.localPosition
//        val position = textPainter.getPositionForOffset(offset)
//        val span = textPainter.text.getSpanForPosition(position)
//        span?.recognizer?.addPointer(event)
//    }

    /**
     * Whether this paragraph currently has a [Shader] for its overflow effect.
     *
     * Used to test this object. Not for use in production.
     */
    @VisibleForTesting
    internal val debugHasOverflowShader: Boolean
        get() = overflowShader != null

    fun performLayout(constraints: Constraints) {
        this.constraints = constraints

        layoutTextWithConstraints(constraints)
        // We grab textPainter.size here because assigning to `size` will trigger
        // us to validate our intrinsic sizes, which will change textPainter's
        // layout because the intrinsic size calculations are destructive.
        // Other textPainter state like didExceedMaxLines will also be affected.
        // See also RenderEditable which has a similar issue.
        val didOverflowHeight = textPainter.didExceedMaxLines
        size = constraints.constrain(
            IntPxSize(textPainter.size.width.px.round(), textPainter.size.height.px.round())
        ).let {
            Size(it.width.value.toFloat(), it.height.value.toFloat())
        }

        val didOverflowWidth = size.width < textSize.width
        // TODO(abarth): We're only measuring the sizes of the line boxes here. If
        // the glyphs draw outside the line boxes, we might think that there isn't
        // visual overflow when there actually is visual overflow. This can become
        // a problem if we start having horizontal overflow and introduce a clip
        // that affects the actual (but undetected) vertical overflow.
        hasVisualOverflow = didOverflowWidth || didOverflowHeight
        if (hasVisualOverflow) {
            when (overflow) {
                TextOverflow.Clip, TextOverflow.Ellipsis -> overflowShader = null
                TextOverflow.Fade -> {
                    val fadeSizePainter = TextPainter(
                        text = TextSpan(style = textPainter.text?.style, text = "\u2026"),
                        textDirection = textDirection,
                        textScaleFactor = textScaleFactor
                    )
                    fadeSizePainter.layout()
                    if (didOverflowWidth) {
                        var fadeEnd: Float
                        var fadeStart: Float
                        when (textDirection) {
                            TextDirection.RTL -> {
                                fadeEnd = 0.0f
                                fadeStart = fadeSizePainter.width
                            }
                            TextDirection.LTR -> {
                                fadeEnd = size.width
                                fadeStart = fadeEnd - fadeSizePainter.width
                            }
                        }
                        overflowShader = Gradient.linear(
                            Offset(fadeStart, 0.0f),
                            Offset(fadeEnd, 0.0f),
                            listOf(Color(0xFFFFFFFF.toInt()), Color(0x00FFFFFF))
                        )
                    } else {
                        val fadeEnd = size.height
                        val fadeStart = fadeEnd - fadeSizePainter.height
                        overflowShader = Gradient.linear(
                            Offset(0.0f, fadeStart),
                            Offset(0.0f, fadeEnd),
                            listOf(Color(0xFFFFFFFF.toInt()), Color(0x00FFFFFF))
                        )
                    }
                }
            }
        } else {
            overflowShader = null
        }
    }

    fun paint(canvas: Canvas, offset: Offset) {
        // Ideally we could compute the min/max intrinsic width/height with a
        // non-destructive operation. However, currently, computing these values
        // will destroy state inside the painter. If that happens, we need to
        // get back the correct state by calling layout again.
        //
        // TODO(abarth): Make computing the min/max intrinsic width/height
        // a non-destructive operation.
        //
        // If you remove this call, make sure that changing the textAlign still
        // works properly.
        // TODO(Migration/qqd): Need to figure out where this constraints come from and how to make
        // it non-null. For now Crane Text version does not need to layout text again. Comment it.
//        layoutTextWithConstraints(constraints!!)

        if (hasVisualOverflow) {
            val bounds = offset.and(size)
            if (overflowShader != null) {
                // This layer limits what the shader below blends with to be just the text
                // (as opposed to the text and its background).
                canvas.saveLayer(bounds, Paint())
            } else {
                canvas.save()
            }
            canvas.clipRect(bounds)
        }
        textPainter.paint(canvas, offset)
        if (hasVisualOverflow) {
            if (overflowShader != null) {
                canvas.translate(offset.dx, offset.dy)
                val paint = Paint()
                paint.blendMode = BlendMode.multiply
                paint.shader = overflowShader
                canvas.drawRect(Offset.zero.and(size), paint)
            }
            canvas.restore()
        }
    }

    /** Paint selected region. */
    fun paintSelection(canvas: Canvas, selection: TextSelection) {
        val path = getPathForSelection(selection)
        canvas.drawPath(path, selectionPaint)
    }

    /** Returns path that enclose the given text selection range. */
    internal fun getPathForSelection(selection: TextSelection): Path {
        return textPainter.getPathForSelection(selection)
    }

    /**
     * Returns the position within the text for the given pixel offset.
     *
     * Valid only after [layout].
     */
    fun getPositionForOffset(offset: Offset): TextPosition {
        layoutTextWithConstraints(constraints!!)
        return textPainter.getPositionForOffset(offset)
    }

    /**
     * Returns the Caret as a vertical bar for given text position, at which to paint the caret.
     *
     * Valid only after [layout].
     */
    fun getCaretForTextPosition(position: TextPosition): Pair<Offset, Offset> {
        layoutTextWithConstraints(constraints!!)
        return textPainter.getCaretForTextPosition(position)
    }

    /**
     * Returns the text range of the word at the given offset. Characters not part of a word, such
     * as spaces, symbols, and punctuation, have word breaks on both sides. In such cases, this
     * method will return a text range that contains the given text position.
     *
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * <http://www.unicode.org/reports/tr29/#Word_Boundaries>.
     *
     * Valid only after [layout].
     */
    fun getWordBoundary(position: TextPosition): TextRange {
        layoutTextWithConstraints(constraints!!)
        return textPainter.getWordBoundary(position)
    }

    /**
     * Returns the size of the text as laid out.
     *
     * This can differ from [size] if the text overflowed or if the [constraints] provided by the
     * parent [RenderObject] forced the layout to be bigger than necessary for the given [text].
     *
     * This returns the [TextPainter.size] of the underlying [TextPainter].
     *
     * Valid only after [layout].
     */
    val textSize: Size
        get() {
            return textPainter.size
        }

    /*override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        config.label = text.toPlainText()
        config.textDirection = textDirection
    }

    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        return listOf() /*listOf(
            text.toDiagnosticsNode(
                name = "text",
                style = DiagnosticsTreeStyle.transition
            )
        )*/
    }*/
}
