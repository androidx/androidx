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

import androidx.ui.Type
import androidx.ui.engine.text.TextAlign
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.foundation.diagnostics.IntProperty
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import androidx.ui.widgets.basic.Builder
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.InheritedWidget
import androidx.ui.widgets.framework.Widget

/**
 * The text style to apply to descendant [Text] widgets without explicit style.
 *
 * Creates a default text style for the given subtree.
 *
 * Consider using [DefaultTextStyle.merge] to inherit styling information from the current default
 * text style for a given [BuildContext].
 */
class DefaultTextStyle(
    key: Key? = null,
    /**
     * The text style to apply.
     *
     *  Must not be null.
     */
    val style: TextStyle,
    /** How the text should be aligned horizontally. */
    val textAlign: TextAlign? = null,
    /**
     * Whether the text should break at soft line breaks.
     *
     * If false, the glyphs in the text will be positioned as if there was unlimited horizontal
     * space.
     *
     * Must not be null.
     */
    val softWrap: Boolean = true,
    /**
     * How visual overflow should be handled.
     *
     *  Must not be null.
     */
    val overflow: TextOverflow = TextOverflow.CLIP,
    /**
     * An optional maximum number of lines for the text to span, wrapping if necessary.
     *
     * If the text exceeds the given number of lines, it will be truncated according to [overflow].
     *
     * If this is 1, text will not wrap. Otherwise, text will be wrapped at the edge of the box.
     *
     * If this is non-null, it will override even explicit null values of [Text.maxLines].
     *
     * May be null(and indeed defaults to null), but if it is not null, it must be greater than
     * zero.
     */
    val maxLines: Int? = null,
    child: Widget?
) : InheritedWidget(key = key, child = child) {
    init {
        assert(maxLines == null || maxLines > 0)
    }

    /**
     * Secondary constructor that provides fallback values.
     *
     * Returned from [of] when the given [BuildContext] doesn't have an enclosing default text style.
     *
     * This constructor creates a [DefaultTextStyle] that lacks a [child], which means the
     * constructed value cannot be incorporated into the tree.
     */
    constructor() : this(
        key = null,
        style = TextStyle(),
        textAlign = null,
        softWrap = true,
        maxLines = null,
        overflow = TextOverflow.CLIP,
        child = null
    )

    // TODO(Migration/qqd): Write tests for merge and of after figuring out how _ElementLifecycle
    // works.
    // Currently tests for [DefaultTextStyle.of()] causes error from
    // [Element._debugCheckStateIsActiveForAncestorLookup()],
    // because {_debugLifecycleState != _ElementLifecycle.active}.
    companion object {
        /**
         * Creates a default text style that overrides the text styles in scope at this point in the
         * widget tree.
         *
         * The given [style] is merged with the [style] from the default text style for the
         * [BuildContext] where the widget is inserted, and any of the other arguments that are not
         * null replace the corresponding properties on that same default text style.
         *
         * This constructor cannot be used to override the [maxLines] property of the ancestor with
         * the value null, since null here is used to mean "defer to ancestor". To replace a
         * non-null [maxLines] from an ancestor with the null value (to remove the restriction on
         * number of lines), manually obtain the ambient [DefaultTextStyle] using
         * [DefaultTextStyle.of], then create a new [DefaultTextStyle] using the
         * [new DefaultTextStyle] constructor directly.
         *
         * See the source below for an example of how to do this (since that's essentially what this
         * constructor does).
         */
        fun merge(
            key: Key? = null,
            style: TextStyle? = null,
            textAlign: TextAlign? = null,
            softWrap: Boolean? = null,
            overflow: TextOverflow? = null,
            maxLines: Int? = null,
            child: Widget
        ): Widget {
            assert(child != null)
            return Builder(
                builder = { context ->
                    val parent: DefaultTextStyle = of(context)
                    DefaultTextStyle(
                        key = key,
                        style = parent.style.merge(style),
                        textAlign = textAlign ?: parent.textAlign,
                        softWrap = softWrap ?: parent.softWrap,
                        overflow = overflow ?: parent.overflow,
                        maxLines = maxLines ?: parent.maxLines,
                        child = child
                    ) // DefaultTextStyle
                }
            ) // Builder
        }

        /**
         * The closest instance of this class that encloses the given context.
         *
         * If no such instance exists, returns an instance created by [DefaultTextStyle] secondary
         * constructor, which contains fallback values.
         */
        fun of(context: BuildContext): DefaultTextStyle {
            return context.inheritFromWidgetOfExactType(Type(DefaultTextStyle::class.java))
                    as DefaultTextStyle
                ?: DefaultTextStyle()
        }
    }

    override fun updateShouldNotify(oldWidget: InheritedWidget): Boolean {
        return style != (oldWidget as DefaultTextStyle).style ||
                textAlign != oldWidget.textAlign ||
                softWrap != oldWidget.softWrap ||
                overflow != oldWidget.overflow ||
                maxLines != oldWidget.maxLines
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        style.debugFillProperties(properties)
        properties.add(EnumProperty<TextAlign>("textAlign", textAlign, defaultValue = null))
        properties.add(
            FlagProperty(
                "softWrap",
                value = softWrap,
                ifTrue = "wrapping at box width",
                ifFalse = "no wrapping except at line break characters",
                showName = true
            )
        )
        properties.add(EnumProperty<TextOverflow>("overflow", overflow, defaultValue = null))
        properties.add(IntProperty("maxLines", maxLines, defaultValue = null))
    }
}
