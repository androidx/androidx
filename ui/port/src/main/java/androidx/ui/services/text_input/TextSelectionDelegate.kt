/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.services.text_input

import androidx.ui.engine.text.TextPosition

/**
 * An interface for manipulating the selection, to be used by the implementor of the toolbar widget.
 */
interface TextSelectionDelegate {

    /**
     * Gets the current text input.
     * Sets the current text input (replaces the whole line).
     */
    var textEditingValue: TextEditingValue

    /** Hides the text selection toolbar. */
    fun hideToolbar()

    /**
     * Brings the provided [TextPosition] into the visible area of the text input.
     */
    fun bringIntoView(position: TextPosition)
}