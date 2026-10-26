/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.text.ReusableTextStyleScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.BaselineShift

/** Converts this [ReusableTextStyleScope] into a [TextStyle]. */
internal fun ReusableTextStyleScope.toTextStyle(): TextStyle {
    val currentBrush = brush
    val effectiveBaselineShift =
        if (baselineShift == BaselineShift.Unspecified) null else baselineShift
    return if (currentBrush != null) {
        TextStyle(
            brush = currentBrush,
            alpha = alpha,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = effectiveBaselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow,
            drawStyle = drawStyle,
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion,
        )
    } else {
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = effectiveBaselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow,
            drawStyle = drawStyle,
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            platformStyle = platformStyle,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion,
        )
    }
}
