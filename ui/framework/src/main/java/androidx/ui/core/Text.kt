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
package androidx.ui.core

import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionMode
import androidx.ui.core.selection.SelectionRegistrarAmbient
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.ParagraphStyle
import androidx.ui.core.selection.TextSelectionHandler
import androidx.ui.core.selection.TextSelectionProcessor
import androidx.ui.painting.TextPainter
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.painting.toAnnotatedString
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.services.text_editing.TextSelection

private val DefaultSoftWrap: Boolean = true
private val DefaultOverflow: TextOverflow = TextOverflow.Clip
private val DefaultMaxLines: Int? = null

/** The default selection color if none is specified. */
private val DefaultSelectionColor = Color(0x6633B5E5)

/**
 * The Text widget displays text that uses multiple different styles. The text to display is
 * described using a tree of [Span], each of which has an associated style that is used
 * for that subtree. The text might break across multiple lines or might all be displayed on the
 * same line depending on the layout constraints.
 */
@Composable
fun Text(
    /**
     * Style configuration that applies at character level such as color, font etc.
     */
    style: TextStyle? = null,
    /**
     * Style configuration that applies only to paragraphs such as text alignment, or text
     * direction.
     */
    paragraphStyle: ParagraphStyle? = null,
    /**
     *  Whether the text should break at soft line breaks.
     *  If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     *  space.
     *  If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
     */
    softWrap: Boolean = DefaultSoftWrap,
    /** How visual overflow should be handled. */
    overflow: TextOverflow = DefaultOverflow,
    /** The number of font pixels for each logical pixel. */
    textScaleFactor: Float = 1.0f,
    /**
     *  An optional maximum number of lines for the text to span, wrapping if necessary.
     *  If the text exceeds the given number of lines, it will be truncated according to [overflow]
     *  and [softWrap].
     *  The value may be null. If it is not null, then it must be greater than zero.
     */
    maxLines: Int? = DefaultMaxLines,
    /**
     *  The color used to draw selected region.
     */
    selectionColor: Color = DefaultSelectionColor,
    /**
     * Composable TextSpan attached after [text].
     */
    @Children child: @Composable TextSpanScope.() -> Unit
) {
    val rootTextSpan = +memo { TextSpan() }
    val ref = +compositionReference()
    compose(rootTextSpan, ref, child)
    +onDispose { disposeComposition(rootTextSpan, ref) }

    Text(
        text = rootTextSpan.toAnnotatedString(),
        style = style,
        paragraphStyle = paragraphStyle,
        softWrap = softWrap,
        overflow = overflow,
        textScaleFactor = textScaleFactor,
        maxLines = maxLines,
        selectionColor = selectionColor
    )
}

/**
 * Simplified version of [Text] component with minimal set of customizations.
 *
 * @param text The text to display.
 * @param style The text style for the text.
 */
@Composable
fun Text(
    text: String,
    style: TextStyle? = null,
    paragraphStyle: ParagraphStyle? = null,
    softWrap: Boolean = DefaultSoftWrap,
    overflow: TextOverflow = DefaultOverflow,
    textScaleFactor: Float = 1.0f,
    maxLines: Int? = DefaultMaxLines,
    selectionColor: Color = DefaultSelectionColor
) {
    Text(
        text = AnnotatedString(text),
        style = style,
        paragraphStyle = paragraphStyle,
        softWrap = softWrap,
        overflow = overflow,
        textScaleFactor = textScaleFactor,
        maxLines = maxLines,
        selectionColor = selectionColor
    )
}

/**
 * The Text widget displays text that uses multiple different styles. The text to display is
 * described using a [AnnotatedString].
 */
// TODO(migration/qqd): Add tests when text widget system is mature and testable.
@Composable
fun Text(
    /**
     * AnnotatedString encoding a styled text.
     */
    text: AnnotatedString,
    /** The default text style applied to all text in this widget. */
    style: TextStyle? = null,
    /**
     * Style configuration that applies only to paragraphs such as text alignment, or text
     * direction.
     */
    paragraphStyle: ParagraphStyle? = null,
    /**
     *  Whether the text should break at soft line breaks.
     *  If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     *  space.
     *  If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
     */
    softWrap: Boolean = DefaultSoftWrap,
    /** How visual overflow should be handled. */
    overflow: TextOverflow = DefaultOverflow,
    /** The number of font pixels for each logical pixel. */
    textScaleFactor: Float = 1.0f,
    /**
     *  An optional maximum number of lines for the text to span, wrapping if necessary.
     *  If the text exceeds the given number of lines, it will be truncated according to [overflow]
     *  and [softWrap].
     *  The value may be null. If it is not null, then it must be greater than zero.
     */
    maxLines: Int? = DefaultMaxLines,
    /**
     *  The color used to draw selected region.
     */
    selectionColor: Color = DefaultSelectionColor
) {
    val internalSelection = +state<TextSelection?> { null }
    val registrar = +ambient(SelectionRegistrarAmbient)
    val layoutCoordinates = +state<LayoutCoordinates?> { null }

    val themeStyle = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = themeStyle.merge(style)

    // TODO(Migration/siyamed): This is temporary and should be removed when resource
    //  system is resolved.
    val context = composer.composer.context
    mergedStyle.fontFamily?.context = context
    text.textStyles.forEach { it.style.fontFamily?.context = context }

    Semantics(label = text.text) {
        val textPainter = TextPainter(
            text = text,
            style = mergedStyle,
            paragraphStyle = paragraphStyle,
            softWrap = softWrap,
            overflow = overflow,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines
        )
        val children = @Composable {
            // Get the layout coordinates of the text widget. This is for hit test of cross-widget
            // selection.
            OnPositioned(onPositioned = { layoutCoordinates.value = it })
            Draw { canvas, _ ->
                internalSelection.value?.let {
                    textPainter.paintBackground(
                        it.start, it.end, selectionColor, canvas, Offset.zero)
                }
                textPainter.paint(canvas, Offset.zero)
            }
        }
        Layout(children = children, layoutBlock = { _, constraints ->
            textPainter.layout(constraints)
            layout(textPainter.width.px.round(), textPainter.height.px.round()) {}
        })

        +onCommit(textPainter) {
            val id = registrar.subscribe(
                object : TextSelectionHandler {
                    override fun getSelection(
                        selectionCoordinates: Pair<PxPosition, PxPosition>,
                        containerLayoutCoordinates: LayoutCoordinates,
                        mode: SelectionMode
                    ): Selection? {
                        val relativePosition = containerLayoutCoordinates.childToLocal(
                            layoutCoordinates.value!!, PxPosition.Origin
                        )
                        val startPx = selectionCoordinates.first - relativePosition
                        val endPx = selectionCoordinates.second - relativePosition

                        val textSelectionProcessor = TextSelectionProcessor(
                            selectionCoordinates = Pair(startPx, endPx),
                            mode = mode,
                            onSelectionChange = { internalSelection.value = it },
                            textPainter = textPainter
                        )

                        if (!textSelectionProcessor.isSelected) return null

                        // TODO(qqd): Determine a set of coordinates around a character that we need.
                        return Selection(
                            startOffset = textSelectionProcessor.startOffset,
                            endOffset = textSelectionProcessor.endOffset,
                            startLayoutCoordinates =
                            if (textSelectionProcessor.containsWholeSelectionStart) {
                                layoutCoordinates.value!!
                            } else null,
                            endLayoutCoordinates =
                            if (textSelectionProcessor.containsWholeSelectionEnd) {
                                layoutCoordinates.value!!
                            } else null
                        )
                    }
                }
            )
            onDispose {
                registrar.unsubscribe(id)
            }
        }
    }
}

internal val CurrentTextStyleAmbient = Ambient.of<TextStyle>("current text style") {
    TextStyle()
}

/**
 * This component is used to set the current value of the Text style ambient. The given style will
 * be merged with the current style values for any missing attributes. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Composable
fun CurrentTextStyleProvider(value: TextStyle, @Children children: @Composable() () -> Unit) {
    val style = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = style.merge(value)
    CurrentTextStyleAmbient.Provider(value = mergedStyle) {
        children()
    }
}

/**
 * This effect is used to read the current value of the Text style ambient. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
fun currentTextStyle() =
    effectOf<TextStyle> { +ambient(CurrentTextStyleAmbient) }