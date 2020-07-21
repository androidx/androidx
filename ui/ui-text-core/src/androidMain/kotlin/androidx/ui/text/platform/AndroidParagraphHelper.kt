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

package androidx.ui.text.platform

import android.text.SpannableString
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.ui.text.AnnotatedString
import androidx.ui.text.Placeholder
import androidx.ui.text.SpanStyle
import androidx.ui.text.platform.extensions.setLineHeight
import androidx.ui.text.platform.extensions.setPlaceholders
import androidx.ui.text.platform.extensions.setSpanStyles
import androidx.ui.text.platform.extensions.setTextIndent
import androidx.ui.text.style.TextIndent
import androidx.ui.unit.Density
import androidx.ui.unit.TextUnit

@OptIn(InternalPlatformTextApi::class)
internal fun createCharSequence(
    text: String,
    contextFontSize: Float,
    lineHeight: TextUnit,
    textIndent: TextIndent?,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (spanStyles.isEmpty() &&
        placeholders.isEmpty() &&
        textIndent == TextIndent.None &&
        lineHeight.isInherit
    ) {
        return text
    }

    val spannableString = SpannableString(text)

    spannableString.setLineHeight(lineHeight, contextFontSize, density)

    spannableString.setTextIndent(textIndent, contextFontSize, density)

    spannableString.setSpanStyles(spanStyles, density, typefaceAdapter)

    spannableString.setPlaceholders(placeholders, density)

    return spannableString
}
