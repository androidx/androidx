/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.LocaleList

/** The interface for platform specific selection behaviors. */
internal interface PlatformSelectionBehaviors {

    /**
     * After user perform a long press or double click selection, ask platform to suggest the
     * selection range.
     *
     * @param text the text being selected.
     * @param selection the selection range of on the text.
     * @return null if there is no need to update the selection range. Or the new selection range.
     */
    suspend fun suggestSelectionForLongPressOrDoubleClick(
        text: CharSequence,
        selection: TextRange,
    ): TextRange?

    /**
     * This method is invoked by the selectable text containers (e.g. BasicTextField,
     * SelectionContainer) just before the context menu is displayed. It provides an opportunity for
     * the [PlatformSelectionBehaviors] implementation to perform any necessary setup or
     * modifications based on the current text and selection, particularly for platform-specific
     * context menu items or behaviors.
     *
     * For example, an implementation might use this hook to:
     * - Prepare or pre-fetch data required for platform-specific context menu items.
     * - Update internal state based on the selection that will be shown in the menu.
     *
     * @param text The full text content from which the selection is made.
     * @param selection The [TextRange] representing the current selection in the text.
     * @param secondaryClickLocation The [Offset] representing the location of the mouse right-click
     *   that triggered the showing of the context menu; `null` if it was not triggered via
     *   right-click.
     */
    suspend fun onShowContextMenu(
        text: CharSequence,
        selection: TextRange,
        secondaryClickLocation: Offset?,
    )

    /**
     * This method is invoked by the selectable text containers (e.g. BasicTextField,
     * SelectionContainer) just before the selection toolbar is displayed. It provides an
     * opportunity for the [PlatformSelectionBehaviors] implementation to perform any necessary
     * setup or modifications based on the current text and selection, particularly for
     * platform-specific toolbar items or behaviors.
     *
     * For example, an implementation might use this hook to:
     * - Prepare or pre-fetch data required for platform-specific selection toolbar items.
     * - Update internal state based on the selection that will be shown in the toolbar.
     *
     * @param text The full text content from which the selection is made.
     * @param selection The [TextRange] representing the current selection in the text.
     */
    suspend fun onShowSelectionToolbar(text: CharSequence, selection: TextRange)
}

/**
 * The type of the text being selected, it can be editable text or static text. This provides
 * platform some context to decide the selection behaviors.
 */
internal enum class SelectedTextType {
    EditableText,
    StaticText,
}

@Composable
internal expect fun rememberPlatformSelectionBehaviors(
    selectedTextType: SelectedTextType,
    localeList: LocaleList?,
): PlatformSelectionBehaviors?
