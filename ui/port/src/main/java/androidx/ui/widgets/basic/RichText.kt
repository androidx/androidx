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

package androidx.ui.widgets.basic

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.FloatProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.foundation.diagnostics.IntProperty
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.painting.TextSpan
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.widgets.debugCheckHasDirectionality
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.LeafRenderObjectWidget

/**
 * A paragraph of rich text.
 *
 * The [RichText] widget displays text that uses multiple different styles. The text to display is
 * described using a tree of [TextSpan] objects, each of which has an associated style that is used
 * for that subtree. The text might break across multiple lines or might all be displayed on the
 * same line depending on the layout constraints.
 *
 * Text displayed in a [RichText] widget must be explicitly styled. When picking which style to use,
 * consider using [DefaultTextStyle.of] the current [BuildContext] to provide defaults. For more
 * details on how to style text in a [RichText] widget, see the documentation for [TextStyle].
 *
 * When all the text uses the same style, consider using the [Text] widget, which is less verbose
 * and integrates with [DefaultTextStyle] for default styling.
 *
 * * `text`: The text to display in this widget.
 *
 * * `textAlign`: How the text should be aligned horizontally.
 *
 * * `textDirection`: The directionality of the text.
 *   This decides how [textAlign] values like [TextAlign.START] and [TextAlign.END] are interpreted.
 *   This is also used to disambiguate how to render bidirectional text.
 *   For example, if the [text] is an English phrase followed by a Hebrew phrase, in a
 *   [TextDirection.LTR] context the English phrase will be on the left and the Hebrew phrase to its
 *   right, while in a [TextDirection.RTL] context, the English phrase will be on the right and the
 *   Hebrew phrase on its left.
 *   Defaults to the ambient [Directionality], if any. If there is no ambient [Directionality], then
 *   this must not be null.
 *
 * * `softWrap`: Whether the text should break at soft line breaks.
 *   If false, the glyphs in the text will be positioned as if there was unlimited horizontal space.
 *
 * * `overflow`: How visual overflow should be handled.
 *
 * * `textScaleFactor`: The number of font pixels for each logical pixel.
 *   For example, if the text scale factor is 1.5, text will be 50% larger than the specified font
 *   size.
 *
 * * `maxLines`: An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow].
 *   If this is 1, text will not wrap. Otherwise, text will be wrapped at the edge of the box.
 */
class RichText(
    key: Key? = null,
    val text: TextSpan,
    val textAlign: TextAlign = TextAlign.START,
    val textDirection: TextDirection? = null,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.CLIP,
    val textScaleFactor: Float = 1.0f,
    val maxLines: Int? = null
) : LeafRenderObjectWidget(key = key) {
    init {
        assert(maxLines == null || maxLines > 0)
    }

    override fun createRenderObject(context: BuildContext): RenderParagraph {
        assert(textDirection != null || debugCheckHasDirectionality(context))
        return RenderParagraph(
            text,
            textAlign = textAlign,
            textDirection = textDirection ?: Directionality.of(context),
            softWrap = softWrap,
            overflow = overflow,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        assert(textDirection != null || debugCheckHasDirectionality(context))
        (renderObject as RenderParagraph).let {
            it.text = text
            it.textAlign = textAlign
            it.textDirection = textDirection ?: Directionality.of(context)
            it.softWrap = softWrap
            it.overflow = overflow
            it.textScaleFactor = textScaleFactor
            it.maxLines = maxLines
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(
            EnumProperty<TextAlign>(
                "textAlign",
                textAlign,
                defaultValue = TextAlign.START
            )
        )
        properties.add(
            EnumProperty<TextDirection>(
                "textDirection",
                textDirection,
                defaultValue = null
            )
        )
        properties.add(
            FlagProperty(
                "softWrap",
                value = softWrap,
                ifTrue = "wrapping at box width",
                ifFalse = "no wrapping except at line break characters",
                showName = true
            )
        )
        properties.add(
            EnumProperty<TextOverflow>(
                "overflow",
                overflow,
                defaultValue = TextOverflow.CLIP
            )
        )
        properties.add(
            FloatProperty.create(
                "textScaleFactor",
                textScaleFactor,
                defaultValue = 1.0f
            )
        )
        properties.add(IntProperty("maxLines", maxLines, ifNull = "unlimited"))
        properties.add(StringProperty("text", text.toPlainText()))
    }
}
