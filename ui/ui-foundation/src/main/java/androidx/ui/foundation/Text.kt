/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.StructurallyEqual
import androidx.compose.ambientOf
import androidx.ui.core.CoreText
import androidx.ui.core.Modifier
import androidx.ui.graphics.Color
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.accessibilityLabel
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextOverflow

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [currentTextStyle] defined by a theme. If you are setting your
 * own style, you may want to consider first retrieving [currentTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes
 * you want to override.
 *
 * If [color] is explicitly set, it will always be used instead of the value inside [style]. If
 * [color] is not set, and [style] does not have a color, then [contentColor] will be used - this
 * allows this [Text] or element containing this [Text] to adapt to different background
 * colors and still maintain contrast and accessibility.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If `null`, and [style] has no color set, this will be
 * [contentColor].
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier.None,
    color: Color? = null,
    style: TextStyle = currentTextStyle(),
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        AnnotatedString(text),
        modifier,
        color,
        style,
        softWrap,
        overflow,
        maxLines,
        onTextLayout
    )
}

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * The default [style] uses the [currentTextStyle] defined by a theme. If you are setting your
 * own style, you may want to consider first retrieving [currentTextStyle], and using
 * [TextStyle.copy] to keep any theme defined attributes, only modifying the specific attributes
 * you want to override.
 *
 * If [color] is explicitly set, it will always be used instead of the value inside [style]. If
 * [color] is not set, and [style] does not have a color, then [contentColor] will be used - this
 * allows this [Text] or element containing this [Text] to adapt to different background
 * colors and still maintain contrast and accessibility.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text. If `null`, and [style] has no color set, this will be
 * [contentColor].
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier.None,
    color: Color? = null,
    style: TextStyle = currentTextStyle(),
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val textColor = color ?: style.color ?: contentColor()
    val mergedStyle = style.merge(TextStyle(color = textColor))
    Semantics(properties = { accessibilityLabel = text.text }) {
        CoreText(
            text,
            modifier,
            mergedStyle,
            softWrap,
            overflow,
            maxLines,
            onTextLayout
        )
    }
}

private val TextStyleAmbient = ambientOf(StructurallyEqual) { TextStyle() }

/**
 * This component is used to set the current value of the Text style ambient. The given style will
 * be merged with the current style values for any missing attributes. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Composable
fun ProvideTextStyle(value: TextStyle, children: @Composable() () -> Unit) {
    val mergedStyle = currentTextStyle().merge(value)
    Providers(TextStyleAmbient provides mergedStyle, children = children)
}

/**
 * This effect is used to read the current value of the Text style ambient. Any [Text]
 * components included in this component's children will be styled with this style unless
 * styled explicitly.
 */
@Composable
fun currentTextStyle(): TextStyle = TextStyleAmbient.current
