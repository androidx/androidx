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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit

/**
 * An entity that allows performing text layout for auto sizing text.
 *
 * These methods are used by [TextAutoSize] in the [TextAutoSize.getFontSize] method, where
 * developers can lay out text with different font sizes and do certain logic depending on whether
 * or not the text overflows.
 */
sealed interface TextAutoSizeLayoutScope : Density {
    /**
     * Lay out the text and return the result of the measurement
     *
     * @param constraints The constraints to lay the text out with
     * @param text The text to lay out
     * @param fontSize The font size to lay the text out with
     * @return The result of the measurement
     */
    fun performLayout(
        constraints: Constraints,
        text: AnnotatedString,
        fontSize: TextUnit,
    ): TextLayoutResult
}

/** Used only in tests */
internal abstract class SimpleTextAutoSizeLayoutScope : TextAutoSizeLayoutScope
