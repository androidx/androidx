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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit

/**
 * This interface is used by classes responsible for laying out text. Layout will be performed here
 * alongside logic that checks if the text overflows.
 *
 * These methods are used by [TextAutoSize] in the [TextAutoSize.getFontSize] method, where
 * developers can lay out text with different font sizes and do certain logic depending on whether
 * or not the text overflows.
 *
 * This may be implemented in unit tests when testing [TextAutoSize.getFontSize] to see if the
 * method works as intended.
 */
internal interface AutoSizeTextLayoutScope : Density {
    /**
     * Lay out the text with the given font size.
     *
     * @return true if the text overflows.
     */
    fun performLayoutAndGetOverflow(fontSize: TextUnit): Boolean
}
