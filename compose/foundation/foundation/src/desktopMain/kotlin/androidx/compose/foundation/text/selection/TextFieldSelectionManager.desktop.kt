/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.internal.hasText
import androidx.compose.foundation.text.DesktopTextContextMenuItems
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Copy
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Cut
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Paste
import androidx.compose.foundation.text.DesktopTextContextMenuItems.SelectAll
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithLocalization
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Magnification is not supported on desktop.
 */
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
): Modifier = addTextContextMenuComponentsWithLocalization { localization ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: DesktopTextContextMenuItems,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        item(
            key = item.key,
            label = item.label(localization),
            enabled = enabled,
            onClick = {
                onClick()
                close()
            }
        )
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: DesktopTextContextMenuItems,
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

internal actual suspend fun TextFieldSelectionManager.hasAvailableTextToPaste(): Boolean {
    return clipboard?.getClipEntry()?.hasText() == true
}