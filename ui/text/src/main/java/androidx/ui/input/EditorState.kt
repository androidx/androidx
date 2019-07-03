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

package androidx.ui.input

import androidx.annotation.RestrictTo
import androidx.ui.core.TextRange

/**
 * An editing state
 *
 * This class represents the editing state. The text input process is represented of updating this
 * state. TextInputService sends the latest editing state to TextInputClient when the platform input
 * service sends some input events.
 */
class EditorState {

    /**
     * The text
     *
     * The text buffer updated by InputMethodService.
     */
    val text: String

    /**
     * The selection range.
     *
     * If the selection is collapsed, it represents caret location.
     * @see android.view.inputmethod.InputConnection.getSelectedText
     */
    val selection: TextRange

    /**
     * The composition range.
     *
     * The composition text is a text that the input method is currently updating. For example,
     * Japanese language has Hiragana-to-Kanji conversion. Input method typically sets Hiragana
     * characters as the composition text and keep showing list of Kanji candidates. Once user
     * decided to use certain Kanji characters, the input method replaces composition text with
     * final text.
     *
     * In this model, the text has both finalized text and composition text and editor can get
     * composition text by using composition range.
     *
     * This composition can be null if there is no composition string in the text.
     * @see android.view.inputmethod.InputConnection.setComposingRegion
     */
    val composition: TextRange?

    /**
     * Hidden constructor from the developers since composition is owned by IMEs and there is no way
     * of setting composition from developers.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    constructor(
        text: String = String(),
        selection: TextRange = TextRange(0, 0),
        composition: TextRange? = null
    ) {
        this.text = text
        this.selection = selection
        this.composition = composition
    }

    constructor(text: String = String(), selection: TextRange = TextRange(0, 0))
            : this(text, selection, null)
}
