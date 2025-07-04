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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.internal.isAutofillAvailable
import androidx.compose.foundation.text.MenuItemsAvailability
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.*
import androidx.compose.foundation.text.TextItem
import androidx.compose.runtime.State
import androidx.compose.ui.platform.Clipboard

internal fun TextFieldSelectionState.contextMenuBuilder(
    state: ContextMenuState,
    itemsAvailability: State<MenuItemsAvailability>,
    onMenuItemClicked: TextFieldSelectionState.(TextContextMenuItems) -> Unit,
): ContextMenuScope.() -> Unit = {
    fun textFieldItem(label: TextContextMenuItems, enabled: Boolean) {
        TextItem(state, label, enabled) { onMenuItemClicked(label) }
    }

    val availability: MenuItemsAvailability = itemsAvailability.value

    textFieldItem(Cut, enabled = availability.canCut)
    textFieldItem(Copy, enabled = availability.canCopy)
    textFieldItem(Paste, enabled = availability.canPaste)
    textFieldItem(SelectAll, enabled = availability.canSelectAll)
    if (isAutofillAvailable()) {
        textFieldItem(Autofill, enabled = availability.canAutofill)
    }
}
