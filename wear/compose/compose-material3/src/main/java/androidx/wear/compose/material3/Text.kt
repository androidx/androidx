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

package androidx.wear.compose.material3

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
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
 * The default [style] uses the [LocalTextStyle] provided by the [MaterialTheme] / components. If
 * you are setting your own style, you may want to consider first retrieving [LocalTextStyle], and
 * using [TextStyle.copy] to keep any theme defined attributes, only modifying the specific
 * attributes you want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]), then
 *   this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 *   from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then
 * [LocalContentColor] will be used.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 *   this will be [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter. See
 *   [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline). See
 *   [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See
 *   [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. If it is not null, then it must be greater than zero.
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param style Style configuration for the text such as color, font, line height etc.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = LocalTextConfiguration.current.overflow,
    softWrap: Boolean = true,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        AnnotatedString(text),
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        minLines,
        emptyMap(),
        onTextLayout,
        style
    )
}

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [LocalTextStyle] provided by the [MaterialTheme] / components. If
 * you are setting your own style, you may want to consider first retrieving [LocalTextStyle], and
 * using [TextStyle.copy] to keep any theme defined attributes, only modifying the specific
 * attributes you want to override.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]), then
 *   this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 *   from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then
 * [LocalContentColor] will be used.
 *
 * @param text The text to be displayed, where [AnnotatedString] allows multiple styles to be used.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 *   this will be [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter. See
 *   [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline). See
 *   [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM. See
 *   [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 *   text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 *   [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if necessary.
 *   If the text exceeds the given number of lines, it will be truncated according to [overflow] and
 *   [softWrap]. If it is not null, then it must be greater than zero.
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines].
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's used
 *   to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 *   [TextLayoutResult] object that callback provides contains paragraph information, size of the
 *   text, baselines and other details. The callback can be used to add additional decoration or
 *   functionality to the text. For example, to draw selection around the text.
 * @param style Style configuration for the text such as color, font, line height etc.
 */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = LocalTextConfiguration.current.overflow,
    softWrap: Boolean = true,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

    androidx.wear.compose.materialcore.Text(
        text = text,
        modifier = modifier,
        color = textColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * CompositionLocal containing the preferred [TextStyle] that will be used by [Text] components by
 * default. To set the value for this CompositionLocal, see [ProvideTextStyle] which will merge any
 * missing [TextStyle] properties with the existing [TextStyle] set in this CompositionLocal.
 *
 * @see ProvideTextStyle
 */
val LocalTextStyle: ProvidableCompositionLocal<TextStyle> =
    compositionLocalOf(structuralEqualityPolicy()) { DefaultTextStyle }

/**
 * This function is used to set the current value of [LocalTextStyle], merging the given style with
 * the current style values for any missing attributes. Any [Text] components included in this
 * component's [content] will be styled with this style unless styled explicitly.
 *
 * @see LocalTextStyle
 */
@Composable
fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit) {
    val mergedStyle = LocalTextStyle.current.merge(value)
    CompositionLocalProvider(LocalTextStyle provides mergedStyle, content = content)
}

/**
 * CompositionLocal containing the preferred [TextConfiguration] that will be used by [Text]
 * components by default consisting of text alignment, overflow specification and max lines.
 * Material3 components related to text such as [Button], [CheckboxButton], [SwitchButton],
 * [RadioButton] use [LocalTextConfiguration] to set values with which to style child text
 * components.
 */
val LocalTextConfiguration: ProvidableCompositionLocal<TextConfiguration> =
    compositionLocalOf(structuralEqualityPolicy()) {
        TextConfiguration(
            TextConfigurationDefaults.TextAlign,
            TextConfigurationDefaults.Overflow,
            TextConfigurationDefaults.MaxLines
        )
    }

class TextConfiguration(
    val textAlign: TextAlign?,
    val overflow: TextOverflow,
    val maxLines: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextConfiguration) return false

        if (textAlign != other.textAlign) return false
        if (overflow != other.overflow) return false
        if (maxLines != other.maxLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textAlign.hashCode()
        result = 31 * result + overflow.hashCode()
        result = 31 * result + maxLines.hashCode()
        return result
    }
}

object TextConfigurationDefaults {
    val TextAlign: TextAlign? = null
    val Overflow: TextOverflow = TextOverflow.Clip
    const val MaxLines: Int = Int.MAX_VALUE
}
