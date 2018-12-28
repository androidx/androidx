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
import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextBox
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextPosition
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.foundation.diagnostics.IntProperty
import androidx.ui.painting.*
import androidx.ui.rendering.box.RenderBox
import androidx.ui.painting.basictypes.RenderComparison
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.debugRepaintTextRainbowEnabled
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.services.text_editing.TextRange
import androidx.ui.services.text_editing.TextSelection

/**
 * A render object that displays a paragraph of text
 *
 * Creates a paragraph render object.
 *
 * * `text`: The text to display.
 *   Must not be null.
 *
 * * `textAlign`: How the text should be aligned horizontally.
 *   Must not be null.
 *
 * * `textDirection`: The directionality of the text.
 *   This decides how the [TextAlign.start], [TextAlign.end], and [TextAlign.justify] values of
 *   [textAlign] are interpreted.
 *   Must not be null.
 *
 * * `softWrap`: Whether the text should break at soft line breaks.
 *   If false, the glyphs in the text will be positioned as if there was unlimited horizontal space.
 *   If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
 *   Must not be null.
 *
 * * `overflow`: How visual overflow should be handled.
 *   Must not be null.
 *
 * * `textScaleFactor`: The number of font pixels for each logical pixel.
 *   For example, if the text scale factor is 1.5, text will be 50% larger than the specified font
 *   size.
 *   Must not be null.
 *
 * * `maxLines`: An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap].
 *   The value may be null. If it is not null, then it must be greater than zero.
 */
private const val ELLIPSIS: String = "\u2026"

class RenderParagraph(
    text: TextSpan,
    textAlign: TextAlign = TextAlign.START,
    textDirection: TextDirection,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.CLIP,
    textScaleFactor: Double = 1.0,
    maxLines: Int? = null
) : RenderBox() {
    @VisibleForTesting
    // TODO(migration/qqd): Should be internal. Removed internal due to hacking reason for now.
    var textPainter: TextPainter

    private var overflowShader: Shader? = null
    private var hasVisualOverflow = false

    init {
        assert(maxLines == null || maxLines > 0)
        textPainter = TextPainter(
            text = text,
            textAlign = textAlign,
            textDirection = textDirection,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines,
            ellipsis = if (overflow == TextOverflow.ELLIPSIS) ELLIPSIS else null
        )
    }

    var text: TextSpan
        set(value) {
            when (textPainter.text!!.compareTo(value)) {
                RenderComparison.IDENTICAL, RenderComparison.METADATA -> return
                RenderComparison.PAINT -> {
                    textPainter.text = value
                    markNeedsPaint()
                }
                RenderComparison.LAYOUT -> {
                    textPainter.text = value
                    overflowShader = null
                    markNeedsLayout()
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
            markNeedsPaint()
        }
        get() {
            return textPainter.textAlign
        }

    var textDirection: TextDirection
        set(value) {
            if (textPainter.textDirection == value) return
            textPainter.textDirection = value
            markNeedsLayout()
        }
        get() {
            return textPainter.textDirection!!
        }

    var softWrap: Boolean = softWrap
        set(value) {
            if (field == value) return
            field = value
            markNeedsLayout()
        }

    var overflow: TextOverflow = overflow
        set(value) {
            if (field == value) return
            field = value
            textPainter.ellipsis = if (value === TextOverflow.ELLIPSIS) ELLIPSIS else null
            markNeedsLayout()
        }

    var textScaleFactor: Double
        set(value) {
            if (textPainter.textScaleFactor == value) return
            textPainter.textScaleFactor = value
            overflowShader = null
            markNeedsLayout()
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
            markNeedsLayout()
        }
        get() {
            return textPainter.maxLines
        }

    val width: Double
        get() = textPainter.width

    val height: Double
        get() = textPainter.height

    fun layoutText(minWidth: Double = 0.0, maxWidth: Double = Double.POSITIVE_INFINITY) {
        val widthMatters = softWrap || overflow == TextOverflow.ELLIPSIS
        textPainter.layout(
            minWidth = minWidth, maxWidth =
            if (widthMatters) maxWidth else Double.POSITIVE_INFINITY
        )
    }

    fun layoutTextWithConstraints(constraints: BoxConstraints) {
        layoutText(minWidth = constraints.minWidth, maxWidth = constraints.maxWidth)
    }

    public override fun computeMinIntrinsicWidth(height: Double): Double {
        layoutText()
        return textPainter.minIntrinsicWidth
    }

    public override fun computeMaxIntrinsicWidth(height: Double): Double {
        layoutText()
        return textPainter.maxIntrinsicWidth
    }

    @VisibleForTesting
    internal fun computeIntrinsicHeight(width: Double): Double {
        layoutText(minWidth = width, maxWidth = width)
        return textPainter.height
    }

    public override fun computeMinIntrinsicHeight(width: Double): Double {
        return computeIntrinsicHeight(width)
    }

    public override fun computeMaxIntrinsicHeight(width: Double): Double {
        return computeIntrinsicHeight(width)
    }

    public override fun computeDistanceToActualBaseline(baseline: TextBaseline): Double {
        assert(!debugNeedsLayout)
        // TODO(Migration/qqd): Need to figure out where this constraints come from and how to make
        // it non-null.
        assert(constraints != null)
        constraints?.let {
            assert(it.debugAssertIsValid())
            layoutTextWithConstraints(it)
        }
        return textPainter.computeDistanceToActualBaseline(baseline)
    }

    public override fun hitTestSelf(position: Offset): Boolean = true

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

    public override fun performLayout() {
        // TODO(Migration/qqd): Need to figure out where this constraints come from and how to make
        // it non-null.
        layoutTextWithConstraints(constraints!!)
        // We grab textPainter.size here because assigning to `size` will trigger
        // us to validate our intrinsic sizes, which will change textPainter's
        // layout because the intrinsic size calculations are destructive.
        // Other textPainter state like didExceedMaxLines will also be affected.
        // See also RenderEditable which has a similar issue.
        val textSize = textPainter.size
        val didOverflowHeight = textPainter.didExceedMaxLines
        constraints?.let { size = it.constrain(textSize) }

        val didOverflowWidth = size.width < textSize.width
        // TODO(abarth): We're only measuring the sizes of the line boxes here. If
        // the glyphs draw outside the line boxes, we might think that there isn't
        // visual overflow when there actually is visual overflow. This can become
        // a problem if we start having horizontal overflow and introduce a clip
        // that affects the actual (but undetected) vertical overflow.
        hasVisualOverflow = didOverflowWidth || didOverflowHeight
        if (hasVisualOverflow) {
            when (overflow) {
                TextOverflow.CLIP, TextOverflow.ELLIPSIS -> overflowShader = null
                TextOverflow.FADE -> {
                    val fadeSizePainter = TextPainter(
                        text = TextSpan(style = textPainter.text?.style, text = "\u2026"),
                        textDirection = textDirection,
                        textScaleFactor = textScaleFactor
                    )
                    fadeSizePainter.layout()
                    if (didOverflowWidth) {
                        val fadeEnd: Double
                        val fadeStart: Double
                        when (textDirection) {
                            TextDirection.RTL -> {
                                fadeEnd = 0.0
                                fadeStart = fadeSizePainter.width
                            }
                            TextDirection.LTR -> {
                                fadeEnd = size.width
                                fadeStart = fadeEnd - fadeSizePainter.width
                            }
                        }
                        overflowShader = Gradient.linear(
                            Offset(fadeStart, 0.0),
                            Offset(fadeEnd, 0.0),
                            listOf(Color(0xFFFFFFFF.toInt()), Color(0x00FFFFFF.toInt()))
                        )
                    } else {
                        val fadeEnd = size.height
                        val fadeStart = fadeEnd - fadeSizePainter.height / 2.0
                        overflowShader = Gradient.linear(
                            Offset(0.0, fadeStart),
                            Offset(0.0, fadeEnd),
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
        assert {
            if (debugRepaintTextRainbowEnabled) {
                TODO("(Migration/qqd): Needs HSVColor")
//                val paint = Paint()
//                paint.color = debugCurrentRepaintColor.toColor()
//                canvas.drawRect(offset.and(size), paint)
            }
            true
        }

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
                paint.blendMode = BlendMode.modulate
                paint.shader = overflowShader
                canvas.drawRect(Offset.zero.and(size), paint)
            }
            canvas.restore()
        }
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        paint(context.canvas, offset)
    }

    /**
     * Returns the offset at which to paint the caret.
     *
     * Valid only after [layout].
     */
    fun getOffsetForCaret(position: TextPosition, caretPrototype: Rect): Offset {
        assert(!debugNeedsLayout)
        layoutTextWithConstraints(constraints!!)
        return textPainter.getOffsetForCaret(position, caretPrototype)
    }

    /**
     * Returns a list of rects that bound the given selection.
     *
     * A given selection might have more than one rect if this text painter contains bidirectional
     * text because logically contiguous text might not be visually contiguous.
     *
     * Valid only after [layout].
     */
    fun getBoxesForSelection(selection: TextSelection): List<TextBox> {
        assert(!debugNeedsLayout)
        layoutTextWithConstraints(constraints!!)
        return textPainter.getBoxesForSelection(selection)
    }

    /**
     * Returns the position within the text for the given pixel offset.
     *
     * Valid only after [layout].
     */
    fun getPositionForOffset(offset: Offset): TextPosition {
        assert(!debugNeedsLayout)
        layoutTextWithConstraints(constraints!!)
        return textPainter.getPositionForOffset(offset)
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
        assert(!debugNeedsLayout)
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
            assert(!debugNeedsLayout)
            return textPainter.size
        }

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        config.label = text.toPlainText()
        config.textDirection = textDirection
    }

    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        return listOf(
            text.toDiagnosticsNode(
                name = "text",
                style = DiagnosticsTreeStyle.transition
            )
        )
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty<TextAlign>("textAlign", textAlign))
        properties.add(EnumProperty<TextDirection>("textDirection", textDirection))
        properties.add(
            FlagProperty(
                "softWrap",
                value = softWrap,
                ifTrue = "wrapping at box width",
                ifFalse = "no wrapping except at line break characters",
                showName = true
            )
        )
        properties.add(EnumProperty<TextOverflow>("overflow", overflow))
        properties.add(
            DoubleProperty.create(
                "textScaleFactor",
                textScaleFactor,
                defaultValue = 1.0
            )
        )
        properties.add(IntProperty("maxLines", maxLines, ifNull = "unlimited"))
    }
}
