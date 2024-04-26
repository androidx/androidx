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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextItem

internal fun TextFieldSelectionState.contextMenuBuilder(
    state: ContextMenuState,
): ContextMenuScope.() -> Unit = {
    TextItem(state, TextContextMenuItems.Cut, enabled = canCut()) { cut() }
    TextItem(state, TextContextMenuItems.Copy, enabled = canCopy()) {
        copy(cancelSelection = false)
    }
    TextItem(state, TextContextMenuItems.Paste, enabled = canPaste()) { paste() }
    TextItem(state, TextContextMenuItems.SelectAll, enabled = canSelectAll()) { selectAll() }
}
