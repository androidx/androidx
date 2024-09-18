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

import androidx.compose.foundation.text.AutoSize
import androidx.compose.foundation.text.FontSizeSearchScope
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Version of AutoSize that takes in an array and attempts to find the largest font size in the
 * array that doesn't overflow. If this is not found, `100.sp` will be returned
 *
 * @param presets The array of font sizes to be checked
 */
internal class AutoSizePreset(private val presets: Array<TextUnit>) : AutoSize {
    override fun FontSizeSearchScope.getFontSize(): TextUnit {
        var optimalFontSize = 0.sp
        for (size in presets) {
            if (
                size.toPx() > optimalFontSize.toPx() &&
                    !performLayoutAndGetOverflow(size.toPx().toSp())
            ) {
                optimalFontSize = size
            }
        }
        return if (optimalFontSize != 0.sp) optimalFontSize else 100.sp
        // 100.sp is the font size returned when all sizes in the presets array overflow
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

/**
 * [AutoSize] class with a binary implementation where `100.sp` is returned if the font size given
 * doesn't overflow, and `0.sp` if the font size does overflow.
 *
 * The aim of this class is to perform AutoSize without using density methods like `toPx()` to check
 * if [TextUnit.Unspecified] works correctly with `performLayoutAndGetOverflow()`
 */
internal class AutoSizeWithoutToPx(private val fontSize: TextUnit) : AutoSize {
    override fun FontSizeSearchScope.getFontSize(): TextUnit {
        // if there is overflow then 100.sp is returned. Otherwise 0.sp is returned
        if (performLayoutAndGetOverflow(fontSize)) return 100.sp
        return 0.sp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutoSizeWithoutToPx) return false

        return fontSize == other.fontSize
    }

    override fun hashCode(): Int {
        return fontSize.hashCode()
    }
}
