/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [LocalTextStyle] provided by the [GlimmerTheme] / components. If you
 * are setting your own style, you may want to consider first retrieving [LocalTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes you
 * want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or an unspecified type), then
 *   this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or unspecified), then the corresponding value from [style]
 *   will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then the
 * content color provided by the nearest [surface] will be used.
 *
 * @param text the text to be displayed
 * @param modifier the [Modifier] to be applied to this layout node
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 *   this will be the content color provided by the nearest [surface].
 * @param fontSize the size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle the typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight the typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily the font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing the amount of space to add between each letter. See
 *   [TextStyle.letterSpacing].
 * @param textDecoration the decorations to paint on the text (e.g., an underline). See
 *   [TextStyle.textDecoration].
 * @param textAlign the alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param lineHeight line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See
 *   [TextStyle.lineHeight].
 * @param overflow how visual overflow should be handled.
 * @param softWrap whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param onTextLayout callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param autoSize Enable auto sizing for this text composable. Finds the biggest font size that
 *   fits in the available space and lays the text out with this size. This performs multiple layout
 *   passes and can be slower than using a fixed font size. This takes precedence over sizes defined
 *   through [fontSize] and [style]. See [TextAutoSize].
 * @param style style configuration for the text such as color, font, line height etc.
 */
@Composable
public fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
    @IntRange(from = 1) minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    autoSize: TextAutoSize? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    // Color precedence rules:
    // If the explicit color parameter is provided, use it.
    // Otherwise, if the style contains a color, use it.
    // Otherwise, we use the content color.

    val userProvidedColor = color.takeOrElse { style.color }
    // Use content color if a color was not provided explicitly, or in the style
    val usingContentColor = userProvidedColor.isUnspecified

    // Workaround to access content color from outside of a node. This is needed since BasicText
    // does not expose a node, or any other way for us to provide color from a node.
    val contentColorProducer: ContentColorProducer? =
        remember(usingContentColor) {
            // `if` inside the remember block to avoid creating a group for if/else control flow
            // inside composition.
            if (usingContentColor) {
                ContentColorProducer()
            } else {
                null
            }
        }

    BasicText(
        text,
        modifier.then(contentColorProducer?.modifier ?: Modifier),
        style.merge(
            // If using content color, this will be unspecified so will no-op
            color = userProvidedColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        ),
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        color = contentColorProducer,
        autoSize = autoSize,
    )
}

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [LocalTextStyle] provided by the [GlimmerTheme] / components. If you
 * are setting your own style, you may want to consider first retrieving [LocalTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes you
 * want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]), then
 *   this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 *   from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then the
 * content color provided by the nearest [surface] will be used.
 *
 * @param text the text to be displayed
 * @param modifier the [Modifier] to be applied to this layout node
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 *   this will be the content color provided by the nearest [surface].
 * @param fontSize the size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle the typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight the typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily the font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing the amount of space to add between each letter. See
 *   [TextStyle.letterSpacing].
 * @param textDecoration the decorations to paint on the text (e.g., an underline). See
 *   [TextStyle.textDecoration].
 * @param textAlign the alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param lineHeight line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See
 *   [TextStyle.lineHeight].
 * @param overflow how visual overflow should be handled.
 * @param softWrap whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param inlineContent a map storing composables that replaces certain ranges of the text, used to
 *   insert composables into text layout. See [InlineTextContent].
 * @param onTextLayout callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param autoSize Enable auto sizing for this text composable. Finds the biggest font size that
 *   fits in the available space and lays the text out with this size. This performs multiple layout
 *   passes and can be slower than using a fixed font size. This takes precedence over sizes defined
 *   through [fontSize] and [style]. See [TextAutoSize].
 * @param style style configuration for the text such as color, font, line height etc.
 */
@Composable
public fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
    @IntRange(from = 1) minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    autoSize: TextAutoSize? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    // Color precedence rules:
    // If the explicit color parameter is provided, use it.
    // Otherwise, if the style contains a color, use it.
    // Otherwise, we use the content color.

    val userProvidedColor = color.takeOrElse { style.color }
    // Use content color if a color was not provided explicitly, or in the style
    val usingContentColor = userProvidedColor.isUnspecified

    // Workaround to access content color from outside of a node. This is needed since BasicText
    // does not expose a node, or any other way for us to provide color from a node.
    val contentColorProducer: ContentColorProducer? =
        remember(usingContentColor) {
            // `if` inside the remember block to avoid creating a group for if/else control flow
            // inside composition.
            if (usingContentColor) {
                ContentColorProducer()
            } else {
                null
            }
        }

    BasicText(
        text = text,
        modifier = modifier.then(contentColorProducer?.modifier ?: Modifier),
        style =
            style.merge(
                color = userProvidedColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
            ),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        autoSize = autoSize,
        color = contentColorProducer,
    )
}

/**
 * CompositionLocal containing the preferred [TextStyle] that will be used by [Text] components by
 * default.
 */
public val LocalTextStyle: ProvidableCompositionLocal<TextStyle> =
    compositionLocalOf(structuralEqualityPolicy()) { TextStyle.Default }

/**
 * Exposes a [DelegatableNode] so we can query [currentContentColor] from outside of a node. This is
 * not a recommended path, but given the current Text APIs there is no other way to set color -
 * future changes to text should let us remove this workaround.
 */
@VisibleForTesting
@Suppress("ModifierNodeInspectableProperties")
internal class DelegatableNodeProviderElement(
    private val onDelegatableNodeChange: (DelegatableNode?) -> Unit
) : ModifierNodeElement<DelegatableNodeProviderNode>() {
    override fun create(): DelegatableNodeProviderNode =
        DelegatableNodeProviderNode(onDelegatableNodeChange)

    override fun update(node: DelegatableNodeProviderNode) {
        node.update(onDelegatableNodeChange)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegatableNodeProviderElement) return false

        if (onDelegatableNodeChange !== other.onDelegatableNodeChange) return false

        return true
    }

    override fun hashCode(): Int {
        return onDelegatableNodeChange.hashCode()
    }
}

internal class DelegatableNodeProviderNode(
    private var onDelegatableNodeChange: (DelegatableNode?) -> Unit
) : Modifier.Node() {
    override fun onAttach() {
        onDelegatableNodeChange(this)
    }

    override fun onDetach() {
        onDelegatableNodeChange(null)
    }

    fun update(onDelegatableNodeChange: (DelegatableNode?) -> Unit) {
        this.onDelegatableNodeChange = onDelegatableNodeChange
        onDelegatableNodeChange(this)
    }
}

/**
 * [ColorProducer] that reads content color from the nearest surface using [currentContentColor].
 * Requires [modifier] to be attached to the hierarchy.
 */
private class ContentColorProducer : ColorProducer {
    private val nodeState: MutableState<DelegatableNode?> = mutableStateOf(null)

    val modifier = DelegatableNodeProviderElement { nodeState.value = it }

    override fun invoke(): Color {
        val node = nodeState.value
        return if (node?.node?.isAttached == true) {
            node.currentContentColor()
        } else {
            Color.White
        }
    }
}
