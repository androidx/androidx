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
import androidx.ui.painting.TextSpan
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.r4a.Component
import com.google.r4a.composer

/**
 * Text Widget Crane version.
 *
 * The Text widget displays text that uses multiple different styles. The text to display is
 * described using a tree of [TextSpan] objects, each of which has an associated style that is used
 * for that subtree. The text might break across multiple lines or might all be displayed on the
 * same line depending on the layout constraints.
 */
// TODO(migration/qqd): Add tests when text widget system is mature and testable.
class Text() : Component() {
    /** The text to display. */
    lateinit var text: TextSpan
    /** How the text should be aligned horizontally. */
    var textAlign: TextAlign = TextAlign.START
    /** The directionality of the text. */
    var textDirection: TextDirection = TextDirection.LTR
    /**
     *  Whether the text should break at soft line breaks.
     *  If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     *  space.
     *  If [softWrap] is false, [overflow] and [textAlign] may have unexpected effects.
     */
    var softWrap: Boolean = true
    /** How visual overflow should be handled. */
    var overflow: TextOverflow = TextOverflow.CLIP
    /** The number of font pixels for each logical pixel. */
    var textScaleFactor: Double = 1.0
    /**
     *  An optional maximum number of lines for the text to span, wrapping if necessary.
     *  If the text exceeds the given number of lines, it will be truncated according to [overflow]
     *  and [softWrap].
     *  The value may be null. If it is not null, then it must be greater than zero.
     */
    var maxLines: Int? = null

    override fun compose() {
        assert(text != null)
        <MeasureBox> constraints, measureOperations ->
            val renderParagraph = RenderParagraph(
                    text = text,
                    textAlign = textAlign,
                    textDirection = textDirection,
                    softWrap = softWrap,
                    overflow = overflow,
                    textScaleFactor = textScaleFactor,
                    maxLines = maxLines
            )
            val context = composer.composer.context

            // TODO(Migration/siyamed): This is temporary and should be removed when resource
            // system is resolved.
            attachContextToFont(text, context)

            val boxConstraints = BoxConstraints(
                    constraints.minWidth.toPx(context).toDouble(),
                    constraints.maxWidth.toPx(context).toDouble(),
                    constraints.minHeight.toPx(context).toDouble(),
                    constraints.maxHeight.toPx(context).toDouble())
            renderParagraph.layoutTextWithConstraints(boxConstraints)
            measureOperations.collect {
                <Draw> canvas, parent ->
                    renderParagraph.paint(canvas, Offset(0.0, 0.0))
                </Draw>
            }
            measureOperations.layout(
                    renderParagraph.width.toDp(context),
                    renderParagraph.height.toDp(context)) {}
        </MeasureBox>
    }

    private fun attachContextToFont(
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
}
