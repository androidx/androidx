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

package androidx.text.vertical.compose

import android.graphics.Typeface
import android.os.LocaleList
import android.text.TextPaint
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/**
 * Resets [out] and applies [style] and [typeface] to it using [density].
 *
 * When [VerticalTextStyle.fontSize] is in `em`, it scales [DefaultFontSize]. When
 * [VerticalTextStyle.fontSize] is unspecified, it uses [DefaultFontSize].
 */
internal fun setStyleToPaint(
    style: VerticalTextStyle,
    typeface: Typeface,
    density: Density,
    out: TextPaint,
) {
    out.reset()
    with(density) {
        out.textSize =
            when {
                style.fontSize.isSp -> style.fontSize.toPx()
                style.fontSize.isEm -> DefaultFontSize.toPx() * style.fontSize.value
                else -> DefaultFontSize.toPx()
            }
        out.typeface = typeface
        out.fontFeatureSettings = style.fontFeatureSettings
        if (style.color.isSpecified) {
            out.color = style.color.toArgb()
        }
        // The caller reuses the TextPaint, and Paint.reset() does not reset the fields that
        // TextPaint adds, such as bgColor. Assign each TextPaint field that this function sets
        // every time, also when the style does not specify a value.
        out.bgColor =
            if (style.background.isSpecified) {
                style.background.toArgb()
            } else {
                android.graphics.Color.TRANSPARENT
            }
        style.localeList
            ?.map { it.platformLocale }
            ?.toTypedArray()
            ?.let { out.textLocales = LocaleList(*it) }
    }
}

/** Default font size used when [VerticalTextStyle.fontSize] is unspecified or in `em`. */
internal val DefaultFontSize = 16.sp
