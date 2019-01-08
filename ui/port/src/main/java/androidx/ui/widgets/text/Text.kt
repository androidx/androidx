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

package androidx.ui.widgets.text

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsTreeStyle
import androidx.ui.foundation.diagnostics.FloatProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.foundation.diagnostics.IntProperty
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.widgets.basic.RichText
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.Widget

/**
 * A run of text with a single style.
 *
 * The [Text] widget displays a string of text with single style. The string might break across
 * multiple lines or might all be displayed on the same line depending on the layout constraints.
 *
 * The [style] argument is optional. When omitted, the text will use the style from the closest
 * enclosing [DefaultTextStyle]. If the given style's [TextStyle.inherit] property is true, the
 * given style will be merged with the closest enclosing [DefaultTextStyle]. This merging behavior
 * is useful, for example, to make the text bold while using the default font family and size.
 *
 * Using the second secondary constructor, the [Text] widget can also be created with a
 * [TextSpan] to display text that use multiple styles (e.g., a paragraph with some bold words).
 */
class Text(
    key: Key? = null,
    /**
     * The text style to apply.
     *
     * If non-null, the style to use for this text.
     *
     * If the style's "inherit" property is true, the style will be merged with the closest
     * enclosing [DefaultTextStyle]. Otherwise, the style will replace the closest enclosing
     * [DefaultTextStyle].
     */
    val style: TextStyle? = null,
    /** How the text should be aligned horizontally. */
    val textAlign: TextAlign? = null,
    /**
     * The directionality of the text.
     *
     * This decides how [textAlign] values like [TextAlign.START] and [TextAlign.END] are
     * interpreted.
     *
     * This is also used to disambiguate how to render bidirectional text.
     *
     * Defaults to the ambient [Directionality], if any.
     */
    val textDirection: TextDirection? = null,
    /**
     * Whether the text should break at soft line breaks.
     *
     * If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     * space.
     */
    val softWrap: Boolean? = null,
    /** How visual overflow should be handled. */
    val overflow: TextOverflow? = null,
    /** The number of font pixels for each logical pixel. */
    val textScaleFactor: Float? = null,
    /**
     * An optional maximum number of lines for the text to span, wrapping if necessary.If the text
     * exceeds the given number of lines, it will be truncated according to [overflow].
     *
     * If this is 1, text will not wrap. Otherwise, text will be wrapped at the edge of the box.
     *
     * If this is null, but there is an ambient [DefaultTextStyle] that specifies an explicit number
     * for its [DefaultTextStyle.maxLines], then the [DefaultTextStyle] value will take precedence.
     *
     * You can use a [RichText] widget directly to entirely override the [DefaultTextStyle].
     */
    val maxLines: Int? = null
) : StatelessWidget(key) {

    /**
     * The text to display.
     *
     * This will be null if a [textSpan] is provided instead.
     */
    var data: String? = null
    /**
     * The text to display as a [TextSpan].
     *
     * This will be null if [data] is provided instead.
     */
    var textSpan: TextSpan? = null

    /**
     * Creates a text widget.
     *
     * If the [style] argument is null, the text will use the style from the closest enclosing
     * [DefaultTextStyle].
     */
    constructor(
        data: String,
        key: Key? = null,
        style: TextStyle? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        softWrap: Boolean? = null,
        overflow: TextOverflow? = null,
        textScaleFactor: Float? = null,
        maxLines: Int? = null
    ) : this(key, style, textAlign, textDirection, softWrap, overflow, textScaleFactor, maxLines) {
        this.data = data
        textSpan = null
    }

    /** Creates a text widget with a [TextSpan]. */
    constructor(
        textSpan: TextSpan,
        key: Key? = null,
        style: TextStyle? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        softWrap: Boolean? = null,
        overflow: TextOverflow? = null,
        textScaleFactor: Float? = null,
        maxLines: Int? = null
    ) : this(key, style, textAlign, textDirection, softWrap, overflow, textScaleFactor, maxLines) {
        this.textSpan = textSpan
        data = null
    }

    // TODO(Migration/qqd): Write tests for build after figuring out how _ElementLifecycle works.
    // Currently tests for [DefaultTextStyle.of()] causes error from
    // [Element._debugCheckStateIsActiveForAncestorLookup()],
    // because {_debugLifecycleState != _ElementLifecycle.active}.
    override fun build(context: BuildContext): Widget {
        val defaultTextStyle: DefaultTextStyle = DefaultTextStyle.of(context)
        var effectiveTextStyle: TextStyle? = style
        // Default value of style.inherit is true.
        if (style == null || (style.inherit ?: true)) {
            effectiveTextStyle = defaultTextStyle.style.merge(style)
        }
        return RichText(
            textAlign = textAlign ?: defaultTextStyle.textAlign ?: TextAlign.START,
            // RichText uses Directionality.of to obtain a default if this is null.
            textDirection = textDirection,
            softWrap = softWrap ?: defaultTextStyle.softWrap,
            overflow = overflow ?: defaultTextStyle.overflow,
            // TODO(Migration/qqd): Implement textScaleFactor's fallback value
            // MediaQuery.textScaleFactorOf(context) after MediaQuery is implemented.
            textScaleFactor = textScaleFactor ?: 1.0f,
            maxLines = maxLines ?: defaultTextStyle.maxLines,
            text = TextSpan(
                style = effectiveTextStyle,
                text = data,
                children = textSpan?.let { listOf(it) }
            ) // TextSpan
        ) // RichText
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(StringProperty("data", data, showName = false))
        textSpan?.let {
            properties.add(
                it.toDiagnosticsNode(
                    name = "textSpan",
                    style = DiagnosticsTreeStyle.transition
                )
            )
        }
        style?.debugFillProperties(properties)
        properties.add(EnumProperty<TextAlign>("textAlign", textAlign, defaultValue = null))
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
                value = softWrap ?: true, // softWrap's default value is true.
                ifTrue = "wrapping at box width",
                ifFalse = "no wrapping except at line break characters",
                showName = true
            )
        )
        properties.add(EnumProperty<TextOverflow>("overflow", overflow, defaultValue = null))
        properties.add(
            FloatProperty.create(
                "textScaleFactor",
                textScaleFactor,
                defaultValue = null
            )
        )
        properties.add(IntProperty("maxLines", maxLines, defaultValue = null))
    }
}
