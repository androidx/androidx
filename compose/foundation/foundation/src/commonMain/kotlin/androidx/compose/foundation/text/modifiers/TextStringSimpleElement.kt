/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextStyleScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow

/**
 * Modifier element for String based text
 *
 * This is faster than [TextAnnotatedStringElement]
 */
internal class TextStringSimpleElement
private constructor(
    private val text: String,
    private val style: TextStyle?,
    private val styleProvider: (TextStyleScope.() -> Unit)?,
    private val fontFamilyResolver: FontFamily.Resolver,
    private val defaultLocaleList: LocaleList,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
    private val color: ColorProducer? = null,
) : ModifierNodeElement<TextStringSimpleNode>() {

    constructor(
        text: String,
        style: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
        color: ColorProducer? = null,
    ) : this(
        text = text,
        style = style,
        styleProvider = null,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color,
    )

    constructor(
        text: String,
        styleProvider: (TextStyleScope.() -> Unit)?,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
    ) : this(
        text = text,
        style = null,
        styleProvider = styleProvider,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = null,
    )

    override fun create(): TextStringSimpleNode =
        if (style != null) {
            TextStringSimpleNode(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                overrideColor = color,
            )
        } else {
            TextStringSimpleNode(
                text = text,
                styleProvider = styleProvider,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
            )
        }

    override fun update(node: TextStringSimpleNode) {
        if (style != null) {
            node.doInvalidations(
                drawChanged = node.updateDraw(color, style),
                textChanged = node.updateText(text = text),
                layoutChanged =
                    node.updateLayoutRelatedArgs(
                        style = style,
                        minLines = minLines,
                        maxLines = maxLines,
                        softWrap = softWrap,
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                        overflow = overflow,
                    ),
                styleChanged = false,
            )
        } else {
            val styleChanged = node.updateStyle(styleProvider)
            val drawChanged = node.updateDraw(color)
            val textChanged = node.updateText(text = text)
            val layoutChanged =
                node.updateLayoutRelatedArgs(
                    minLines = minLines,
                    maxLines = maxLines,
                    softWrap = softWrap,
                    fontFamilyResolver = fontFamilyResolver,
                    defaultLocaleList = defaultLocaleList,
                    overflow = overflow,
                )
            node.doInvalidations(
                drawChanged = drawChanged,
                textChanged = textChanged,
                layoutChanged = layoutChanged,
                styleChanged = styleChanged,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is TextStringSimpleElement) return false

        // these three are most likely to actually change
        if (color != other.color) return false
        if (text != other.text) return false /* expensive to check, do after color */
        if (style != other.style) return false
        if (styleProvider !== other.styleProvider) return false

        // these are equally unlikely to change
        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (defaultLocaleList != other.defaultLocaleList) return false
        if (overflow != other.overflow) return false
        if (softWrap != other.softWrap) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (style?.hashCode() ?: styleProvider?.hashCode() ?: 0)
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + defaultLocaleList.hashCode()
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + (color?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}
