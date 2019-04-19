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

import android.content.Context
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.services.text_editing.TextSelection
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.effectOf
import com.google.r4a.unaryPlus

private val DefaultTextAlign: TextAlign = TextAlign.START
private val DefaultTextDirection: TextDirection = TextDirection.LTR
private val DefaultSoftWrap: Boolean = true
private val DefaultOverflow: TextOverflow = TextOverflow.CLIP
private val DefaultMaxLines: Int? = null

/** The default selection color if none is specified. */
private val DefaultSelectionColor = Color(0x6633B5E5)

/**
 * Text Widget Crane version.
 *
 * The Text widget displays text that uses multiple different styles. The text to display is
 * described using a tree of [TextSpan] objects, each of which has an associated style that is used
 * for that subtree. The text might break across multiple lines or might all be displayed on the
 * same line depending on the layout constraints.
 */
// TODO(migration/qqd): Add tests when text widget system is mature and testable.
@Composable
fun Text(
    /** The text to display. */
    text: TextSpan,
    /** How the text should be aligned horizontally. */
    textAlign: TextAlign = DefaultTextAlign,
    /** The directionality of the text. */
    textDirection: TextDirection = DefaultTextDirection,
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
    // TODO(qqd): Make variable selection private in future.
    /**
     *  The selection of the text.
     */
    selection: TextSelection? = null,
    /**
     *  The selection's start and end offset. In this pair, the first is start, and the second is
     *  end.
     */
    selectionPosition: Pair<Offset, Offset>? = null,
    /**
     *  The color used to draw selected region.
     */
    selectionColor: Color = DefaultSelectionColor
) {
    val context = composer.composer.context
    var internalSelection = selection

    fun attachContextToFont(
        text: TextSpan,
        context: Context
    ) {
        text.visitTextSpan() {
            it.style?.fontFamily?.let {
                it.context = context
            }
            true
        }
    }

        val style = +ambient(CurrentTextStyleAmbient)
        val mergedStyle = style.merge(text.style)
        // Make a wrapper to avoid modifying the style on the original element
        val styledText = TextSpan(style = mergedStyle, children = listOf(text))
        <Semantics
            label=text.toString()>
            val renderParagraph = RenderParagraph(
                text = styledText,
                textAlign = textAlign,
                textDirection = textDirection,
                softWrap = softWrap,
                overflow = overflow,
                textScaleFactor = textScaleFactor,
                maxLines = maxLines,
                selectionColor = selectionColor
            )
            // TODO(Migration/siyamed): This is temporary and should be removed when resource
            // system is resolved.
            attachContextToFont(styledText, context)

            val children = @Composable {
                <Draw> canvas, _ ->
                    internalSelection?.let { renderParagraph.paintSelection(canvas, it) }
                    renderParagraph.paint(canvas, Offset(0.0f, 0.0f))
                </Draw>
            }
            <Layout children> _, constraints ->
                renderParagraph.performLayout(constraints)
                // Convert the selection's start and end offset to a TextSelection object.
                selectionPosition?.let {
                    var selectionStart = renderParagraph.getPositionForOffset(it.first).offset
                    var selectionEnd = renderParagraph.getPositionForOffset(it.second).offset

                    if (selectionEnd == selectionStart) selectionEnd = selectionStart + 1
                    internalSelection = TextSelection(selectionStart, selectionEnd)
                }

                layout(renderParagraph.width.px.round(), renderParagraph.height.px.round()) {}
            </Layout>
        </Semantics>
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
    textAlign: TextAlign = DefaultTextAlign,
    textDirection: TextDirection = DefaultTextDirection,
    softWrap: Boolean = DefaultSoftWrap,
    overflow: TextOverflow = DefaultOverflow,
    maxLines: Int? = DefaultMaxLines
) {
    <Text
        text=TextSpan(text = text, style = style)
        textAlign
        textDirection
        softWrap
        overflow
        maxLines />
}

internal val CurrentTextStyleAmbient = Ambient<TextStyle>("current text style") {
    TextStyle()
}

/**
 * This component is used to set the current value of the Text style ambient. The given style will
 * be merged with the current style values for any missing attributes. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Composable
fun CurrentTextStyleProvider(value: TextStyle, @Children children: () -> Unit) {
    val style = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = style.merge(value)
    <CurrentTextStyleAmbient.Provider value=mergedStyle>
        <children />
    </CurrentTextStyleAmbient.Provider>
}

/**
 * This effect is used to read the current value of the Text style ambient. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
fun currentTextStyle() = effectOf<TextStyle> { +ambient(CurrentTextStyleAmbient) }