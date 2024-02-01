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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedRow

/**
 * CurvedText is a component allowing developers to easily write curved text following
 * the curvature a circle (usually at the edge of a circular screen).
 * CurvedText can be only created within the CurvedLayout to ensure the best experience, like being
 * able to specify to positioning.
 *
 * The default [style] uses the [LocalTextStyle] provided by the [MaterialTheme] / components,
 * converting it to a [CurvedTextStyle]. Note that not all parameters are used by [curvedText].
 *
 * If you are setting your own style, you may want to consider first retrieving [LocalTextStyle],
 * and using [TextStyle.copy] to keep any theme defined attributes, only modifying the specific
 * attributes you want to override, then convert to [CurvedTextStyle]
 *
 * For ease of use, commonly used parameters from [CurvedTextStyle] are also present here. The
 * order of precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]),
 * then this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 * from [style] will be used instead.
 *
 * Additionally, for [color], if [color] is not set, and [style] does not have a color, then
 * [LocalContentColor] will be used.
 *
 * For samples explicitly specifying style see:
 * TODO(b/283777480): Add CurvedText samples
 *
 * For examples using CompositionLocal to specify the style, see:
 * TODO(b/283777480): Add CurvedText samples
 *
 * For more information, see the
 * [Curved Text](https://developer.android.com/training/wearables/compose/curved-text)
 * guide.
 *
 * @param text The text to display
 * @param modifier The [CurvedModifier] to apply to this curved text.
 * @param background The background color for the text.
 * @param color [Color] to apply to the text. If [Color.Unspecified], and [style] has no color set,
 * this will be [LocalContentColor].
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontFamily The font family to be used when rendering the text.
 * @param fontWeight The thickness of the glyphs, in a range of [1, 1000]. see [FontWeight]
 * @param fontStyle The typeface variant to use when drawing the letters (e.g. italic).
 * @param fontSynthesis Whether to synthesize font weight and/or style when the requested weight
 * or style cannot be found in the provided font family.
 * @param style Specifies the style to use.
 * @param angularDirection Specify if the text is laid out clockwise or anti-clockwise, and if
 * those needs to be reversed in a Rtl layout.
 * If not specified, it will be inherited from the enclosing [curvedRow] or [CurvedLayout]
 * See [CurvedDirection.Angular].
 * @param overflow How visual overflow should be handled.
 */
fun CurvedScope.curvedText(
    text: String,
    modifier: CurvedModifier = CurvedModifier,
    background: Color = Color.Unspecified,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontSynthesis: FontSynthesis? = null,
    style: CurvedTextStyle? = null,
    angularDirection: CurvedDirection.Angular? = null,
    overflow: TextOverflow = TextOverflow.Clip,
) = basicCurvedText(text, modifier, angularDirection, overflow) {
    val baseStyle = style ?: CurvedTextStyle(LocalTextStyle.current)
    val textColor = color.takeOrElse {
        baseStyle.color.takeOrElse {
            LocalContentColor.current
        }
    }
    baseStyle.merge(
        CurvedTextStyle(
            color = textColor,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            background = background
        )
    )
}
