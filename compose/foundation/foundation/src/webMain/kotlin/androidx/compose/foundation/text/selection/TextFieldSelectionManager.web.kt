/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.Copy
import androidx.compose.foundation.text.TextContextMenuItems.Cut
import androidx.compose.foundation.text.TextContextMenuItems.Paste
import androidx.compose.foundation.text.TextContextMenuItems.SelectAll
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.getLocalizedString
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier = this

/**
 * Whether the selection handle is in the visible bound of the TextField.
 */
internal actual fun TextFieldSelectionManager.isSelectionHandleInVisibleBound(
    isStartHandle: Boolean
): Boolean = isSelectionHandleInVisibleBoundDefault(isStartHandle)

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    manager: TextFieldSelectionManager,
    coroutineScope: CoroutineScope,
): Modifier = appendTextContextMenuComponents {
    fun TextContextMenuBuilderScope.textFieldItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        item(
            key = item.key,
            label = getLocalizedString(item.stringId),
            enabled = enabled,
            onClick = {
                onClick()
                close()
            }
        )
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        textFieldItem(item, enabled) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { onClick() }
        }
    }

    with(manager) {
        separator()
        textFieldSuspendItem(Cut, enabled = canShowCutMenuItem()) { cut() }
        textFieldSuspendItem(Copy, enabled = canShowCopyMenuItem()) { copy(cancelSelection = false) }
        textFieldSuspendItem(Paste, enabled = canShowPasteMenuItem()) { paste() }
        textFieldItem(SelectAll, enabled = canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}

// the paste state is needed to show or hide the "paste" context menu item.
// in browsers we don't want to bother users by a permission request,
// so we return unconditionally true
internal actual suspend fun TextFieldSelectionManager.hasAvailableTextToPaste(): Boolean = true