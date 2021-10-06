/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Returns true if the this TextLayoutResult can be reused for given parameters.
 *
 * @param text a text to be used for computing text layout.
 * @param style a text style to be used for computing text layout.
 * @param placeholders a list of [Placeholder]s to be used for computing text layout.
 * @param maxLines a maximum number of lines to be used for computing text layout.
 * @param softWrap whether doing softwrap or not to be used for computing text layout.
 * @param overflow an overflow type to be used for computing text layout.
 * @param density a density to be used for computing text layout.
 * @param layoutDirection a layout direction to be used for computing text layout.
 * @param constraints a constraint to be used for computing text layout.
 */
@OptIn(InternalFoundationTextApi::class)
internal fun TextLayoutResult.canReuse(
    text: AnnotatedString,
    style: TextStyle,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    softWrap: Boolean,
    overflow: TextOverflow,
    density: Density,
    layoutDirection: LayoutDirection,
    resourceLoader: Font.ResourceLoader,
    constraints: Constraints
): Boolean {

    // NOTE(text-perf-review): might make sense to short-circuit instance equality here

    // Check if this is created from the same parameter.
    val layoutInput = this.layoutInput
    if (!(
        layoutInput.text == text &&
            layoutInput.style.canReuseLayout(style) &&
            layoutInput.placeholders == placeholders &&
            layoutInput.maxLines == maxLines &&
            layoutInput.softWrap == softWrap &&
            layoutInput.overflow == overflow &&
            layoutInput.density == density &&
            layoutInput.layoutDirection == layoutDirection &&
            layoutInput.resourceLoader == resourceLoader
        )
    ) {
        return false
    }

    // Check the given constraints can produces the same result.
    if (constraints.minWidth != layoutInput.constraints.minWidth) return false

    if (!(softWrap || overflow == TextOverflow.Ellipsis)) {
        // If width does not matter, we can result the same layout.
        return true
    }
    return constraints.maxWidth == layoutInput.constraints.maxWidth
}

/**
 * Returns true if text layout created with this TextStyle can be reused for the [other] TextStyle.
 */
internal fun TextStyle.canReuseLayout(other: TextStyle): Boolean {
    return (this === other) || (
        fontSize == other.fontSize &&
        fontWeight == other.fontWeight &&
        fontStyle == other.fontStyle &&
        fontSynthesis == other.fontSynthesis &&
        fontFamily == other.fontFamily &&
        fontFeatureSettings == other.fontFeatureSettings &&
        letterSpacing == other.letterSpacing &&
        baselineShift == other.baselineShift &&
        textGeometricTransform == other.textGeometricTransform &&
        localeList == other.localeList &&
        background == other.background &&
        textAlign == other.textAlign &&
        textDirection == other.textDirection &&
        lineHeight == other.lineHeight &&
        textIndent == other.textIndent
        )
}