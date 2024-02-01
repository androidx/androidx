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

package androidx.compose.ui.text.platform

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.CharacterStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.DefaultIncludeFontPadding
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.extensions.setLineHeight
import androidx.compose.ui.text.platform.extensions.setPlaceholders
import androidx.compose.ui.text.platform.extensions.setSpan
import androidx.compose.ui.text.platform.extensions.setSpanStyles
import androidx.compose.ui.text.platform.extensions.setTextIndent
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isUnspecified
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.REPLACE_STRATEGY_ALL
import androidx.emoji2.text.EmojiCompat.REPLACE_STRATEGY_DEFAULT

@OptIn(InternalPlatformTextApi::class)
internal fun createCharSequence(
    text: String,
    contextFontSize: Float,
    contextTextStyle: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface,
    useEmojiCompat: Boolean,
): CharSequence {

    val currentText = if (useEmojiCompat && EmojiCompat.isConfigured()) {
        val emojiSupportMatch = contextTextStyle.platformStyle?.paragraphStyle?.emojiSupportMatch
        val replaceStrategy = if (emojiSupportMatch == EmojiSupportMatch.All) {
            REPLACE_STRATEGY_ALL
        } else {
            REPLACE_STRATEGY_DEFAULT
        }
        EmojiCompat.get().process(
            text,
            0,
            text.length,
            Int.MAX_VALUE,
            replaceStrategy
        )!!
    } else {
        text
    }

    if (spanStyles.isEmpty() &&
        placeholders.isEmpty() &&
        contextTextStyle.textIndent == TextIndent.None &&
        contextTextStyle.lineHeight.isUnspecified
    ) {
        return currentText
    }

    val spannableString = if (currentText is Spannable) {
        currentText
    } else {
        SpannableString(currentText)
    }

    // b/199939617
    // Due to a bug in the platform's native drawText stack, some CJK characters cause a bolder
    // than intended underline to be painted when TextDecoration is set to Underline.
    // If there's a CharacterStyle span that takes the entire length of the text, even if
    // it's no-op, it causes a different native call to render the text that prevents the bug.
    if (contextTextStyle.textDecoration == TextDecoration.Underline) {
        spannableString.setSpan(NoopSpan, 0, text.length)
    }

    if (contextTextStyle.isIncludeFontPaddingEnabled() &&
        contextTextStyle.lineHeightStyle == null
    ) {
        // keep the existing line height behavior for includeFontPadding=true
        spannableString.setLineHeight(
            lineHeight = contextTextStyle.lineHeight,
            contextFontSize = contextFontSize,
            density = density
        )
    } else {
        val lineHeightStyle = contextTextStyle.lineHeightStyle ?: LineHeightStyle.Default
        spannableString.setLineHeight(
            lineHeight = contextTextStyle.lineHeight,
            lineHeightStyle = lineHeightStyle,
            contextFontSize = contextFontSize,
            density = density,
        )
    }

    spannableString.setTextIndent(contextTextStyle.textIndent, contextFontSize, density)

    spannableString.setSpanStyles(
        contextTextStyle,
        spanStyles,
        density,
        resolveTypeface
    )

    spannableString.setPlaceholders(placeholders, density)

    return spannableString
}

internal fun TextStyle.isIncludeFontPaddingEnabled(): Boolean {
    return platformStyle?.paragraphStyle?.includeFontPadding ?: DefaultIncludeFontPadding
}

private val NoopSpan = object : CharacterStyle() {
    override fun updateDrawState(p0: TextPaint?) {}
}
