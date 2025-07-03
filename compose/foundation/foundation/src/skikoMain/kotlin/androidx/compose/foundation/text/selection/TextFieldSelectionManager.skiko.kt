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

import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.internal.isAutofillAvailable
import androidx.compose.foundation.text.MenuItemsAvailability
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.*
import androidx.compose.foundation.text.TextItem
import androidx.compose.runtime.State


//TODO: remove this file
// when this will be in JB fork
// https://android.googlesource.com/platform/frameworks/support/+/d8bc9d81dffa35162626e45ee68d4a7e271c6ada

internal fun TextFieldSelectionManager.contextMenuBuilder(
    contextMenuState: ContextMenuState,
    itemsAvailability: State<MenuItemsAvailability>,
): ContextMenuScope.() -> Unit = {
    fun textFieldItem(label: TextContextMenuItems, enabled: Boolean, operation: () -> Unit) {
        TextItem(contextMenuState, label, enabled, operation)
    }

    val availability: MenuItemsAvailability = itemsAvailability.value
    textFieldItem(Cut, enabled = availability.canCut) { cut() }
    textFieldItem(Copy, enabled = availability.canCopy) { copy(cancelSelection = false) }
    textFieldItem(Paste, enabled = availability.canPaste) { paste() }
    textFieldItem(SelectAll, enabled = availability.canSelectAll) { selectAll() }
    if (isAutofillAvailable()) {
        textFieldItem(Autofill, enabled = availability.canAutofill) { autofill() }
    }
}