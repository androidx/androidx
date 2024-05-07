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
import androidx.compose.foundation.contextmenu.close
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

@ReadOnlyComposable
@Composable
internal fun TextFieldSelectionState.contextMenuBuilder(
    state: ContextMenuState,
): ContextMenuScope.() -> Unit {
    val cutString = TextContextMenuItems.Cut.resolvedString()
    val copyString = TextContextMenuItems.Copy.resolvedString()
    val pasteString = TextContextMenuItems.Paste.resolvedString()
    val selectAllString = TextContextMenuItems.SelectAll.resolvedString()
    return {
        item(state, label = cutString, enabled = canCut()) { cut() }
        item(state, label = copyString, enabled = canCopy()) { copy(cancelSelection = false) }
        item(state, label = pasteString, enabled = canPaste()) { paste() }
        item(state, label = selectAllString, enabled = canSelectAll()) { selectAll() }
    }
}

private inline fun ContextMenuScope.item(
    state: ContextMenuState,
    label: String,
    enabled: Boolean,
    crossinline operation: () -> Unit
) {
    item(label, enabled = enabled) {
        operation()
        state.close()
    }
}
