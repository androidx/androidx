/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified

/**
 * Version of AutoSize that takes in an array and attempts to find the largest font size in the
 * array that doesn't overflow. Returns [TextUnit.Unspecified] if no preset fits.
 *
 * @param presets The array of font sizes to be checked
 */
internal class AutoSizePreset(
    private val presets: Array<TextUnit>,
    private val fallbackFontSize: TextUnit = TextUnit.Unspecified,
) : TextAutoSize {

    override fun TextAutoSizeLayoutScope.getFontSize(
        constraints: Constraints,
        text: AnnotatedString,
    ): TextUnit {
        var optimalFontSize = fallbackFontSize
        for (size in presets) {
            if (optimalFontSize.isUnspecified || size.toPx() > optimalFontSize.toPx()) {
                val layoutResult = performLayout(constraints, text, size)
                val didOverflow = layoutResult.didOverflowWidth || layoutResult.didOverflowHeight
                if (!didOverflow) {
                    optimalFontSize = size
                }
            }
        }
        return if (optimalFontSize.isUnspecified) fallbackFontSize else optimalFontSize
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutoSizePreset) return false

        return presets.contentEquals(other.presets)
    }

    override fun hashCode(): Int {
        return presets.contentHashCode()
    }
}
