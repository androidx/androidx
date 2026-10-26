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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextStyleScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow

/**
 * Modifier element for any Text with [AnnotatedString] or [onTextLayout] parameters
 *
 * This is slower than [TextAnnotatedStringElement]
 */
internal class TextAnnotatedStringElement(
    private val text: AnnotatedString,
    private val style: TextStyle?,
    private val styleProvider: (TextStyleScope.() -> Unit)?,
    private val fontFamilyResolver: FontFamily.Resolver,
    private val defaultLocaleList: LocaleList,
    private val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
    private val placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    private val onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
    private val selectionController: SelectionController? = null,
    private val color: ColorProducer? = null,
    private val autoSize: TextAutoSize? = null,
    private val onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)? =
        null,
) : ModifierNodeElement<TextAnnotatedStringNode>() {

    constructor(
        text: AnnotatedString,
        style: TextStyle,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        onTextLayout: ((TextLayoutResult) -> Unit)? = null,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
        placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
        onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
        selectionController: SelectionController? = null,
        color: ColorProducer? = null,
        autoSize: TextAutoSize? = null,
        onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)? = null,
    ) : this(
        text = text,
        style = style,
        styleProvider = null,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        placeholders = placeholders,
        onPlaceholderLayout = onPlaceholderLayout,
        selectionController = selectionController,
        color = color,
        autoSize = autoSize,
        onShowTranslation = onShowTranslation,
    )

    constructor(
        text: AnnotatedString,
        styleProvider: (TextStyleScope.() -> Unit)?,
        fontFamilyResolver: FontFamily.Resolver,
        defaultLocaleList: LocaleList,
        onTextLayout: ((TextLayoutResult) -> Unit)? = null,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        minLines: Int = DefaultMinLines,
        placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
        onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
        selectionController: SelectionController? = null,
        autoSize: TextAutoSize? = null,
        onShowTranslation: ((TextAnnotatedStringNode.TextSubstitutionValue) -> Unit)? = null,
    ) : this(
        text = text,
        style = null,
        styleProvider = styleProvider,
        fontFamilyResolver = fontFamilyResolver,
        defaultLocaleList = defaultLocaleList,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        placeholders = placeholders,
        onPlaceholderLayout = onPlaceholderLayout,
        selectionController = selectionController,
        color = null,
        autoSize = autoSize,
        onShowTranslation = onShowTranslation,
    )

    override fun create(): TextAnnotatedStringNode =
        if (style != null) {
            TextAnnotatedStringNode(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                placeholders = placeholders,
                onPlaceholderLayout = onPlaceholderLayout,
                selectionController = selectionController,
                overrideColor = color,
                autoSize = autoSize,
                onShowTranslation = onShowTranslation,
            )
        } else {
            TextAnnotatedStringNode(
                text = text,
                styleProvider = styleProvider,
                fontFamilyResolver = fontFamilyResolver,
                defaultLocaleList = defaultLocaleList,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                placeholders = placeholders,
                onPlaceholderLayout = onPlaceholderLayout,
                selectionController = selectionController,
                autoSize = autoSize,
                onShowTranslation = onShowTranslation,
            )
        }

    override fun update(node: TextAnnotatedStringNode) {
        if (style != null) {
            node.doInvalidations(
                drawChanged = node.updateDraw(color, style, text),
                textChanged = node.updateText(text = text),
                layoutChanged =
                    node.updateLayoutRelatedArgs(
                        style = style,
                        placeholders = placeholders,
                        minLines = minLines,
                        maxLines = maxLines,
                        softWrap = softWrap,
                        fontFamilyResolver = fontFamilyResolver,
                        defaultLocaleList = defaultLocaleList,
                        overflow = overflow,
                        autoSize = autoSize,
                    ),
                callbacksChanged =
                    node.updateCallbacks(
                        onTextLayout = onTextLayout,
                        onPlaceholderLayout = onPlaceholderLayout,
                        selectionController = selectionController,
                        onShowTranslation = onShowTranslation,
                    ),
                styleChanged = false,
            )
        } else {
            val styleChanged = node.updateStyle(styleProvider)
            val drawChanged = node.updateDraw(color, text)
            val textChanged = node.updateText(text = text)
            val layoutChanged =
                node.updateLayoutRelatedArgs(
                    placeholders = placeholders,
                    minLines = minLines,
                    maxLines = maxLines,
                    softWrap = softWrap,
                    fontFamilyResolver = fontFamilyResolver,
                    defaultLocaleList = defaultLocaleList,
                    overflow = overflow,
                    autoSize = autoSize,
                )
            node.doInvalidations(
                drawChanged = drawChanged,
                textChanged = textChanged,
                layoutChanged = layoutChanged,
                callbacksChanged =
                    node.updateCallbacks(
                        onTextLayout = onTextLayout,
                        onPlaceholderLayout = onPlaceholderLayout,
                        selectionController = selectionController,
                        onShowTranslation = onShowTranslation,
                    ),
                styleChanged = styleChanged,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is TextAnnotatedStringElement) return false

        // these three are most likely to actually change
        if (color != other.color) return false
        if (text != other.text) return false /* expensive to check, do it after color */
        if (style != other.style) return false
        if (styleProvider !== other.styleProvider) return false
        if (placeholders != other.placeholders) return false

        // these are equally unlikely to change
        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (defaultLocaleList != other.defaultLocaleList) return false
        if (onTextLayout !== other.onTextLayout) return false
        if (onShowTranslation !== other.onShowTranslation) return false
        if (overflow != other.overflow) return false
        if (softWrap != other.softWrap) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false

        // these never change, but check anyway for correctness
        if (onPlaceholderLayout !== other.onPlaceholderLayout) return false
        if (selectionController != other.selectionController) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (style?.hashCode() ?: styleProvider?.hashCode() ?: 0)
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + defaultLocaleList.hashCode()
        result = 31 * result + (onTextLayout?.hashCode() ?: 0)
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + (placeholders?.hashCode() ?: 0)
        result = 31 * result + (onPlaceholderLayout?.hashCode() ?: 0)
        result = 31 * result + (selectionController?.hashCode() ?: 0)
        result = 31 * result + (color?.hashCode() ?: 0)
        result = 31 * result + (onShowTranslation?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}
