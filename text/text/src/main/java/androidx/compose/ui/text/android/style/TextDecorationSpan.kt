/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.android.style

import android.text.TextPaint
import android.text.style.CharacterStyle
import androidx.compose.ui.text.android.InternalPlatformTextApi

/**
 * A span which applies the underline and strike through to the affected text.
 *
 * @property isUnderlineText whether to draw the under for the affected text.
 * @property isStrikethroughText whether to draw strikethrough line for the affected text.
 * @suppress
 */
@InternalPlatformTextApi
class TextDecorationSpan(
    val isUnderlineText: Boolean,
    val isStrikethroughText: Boolean
) : CharacterStyle() {
    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.isUnderlineText = isUnderlineText
        textPaint.isStrikeThruText = isStrikethroughText
    }
}