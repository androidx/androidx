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

import androidx.compose.foundation.internal.nativeClipboardHasText
import androidx.compose.foundation.text.DesktopTextContextMenuItems
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Copy
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Cut
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Paste
import androidx.compose.foundation.text.DesktopTextContextMenuItems.SelectAll
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithLocalization
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
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

    with(state) {
        separator()
        textFieldSuspendItem(Cut, enabled = canCut()) { cut() }
        textFieldSuspendItem(Copy, enabled = canCopy()) { copy(cancelSelection = false) }
        textFieldSuspendItem(Paste, enabled = canPaste()) { paste() }
        textFieldItem(SelectAll, enabled = canSelectAll()) { selectAll() }
        separator()
    }
}

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    actual suspend fun update() {
        val nativeClipboard = (clipboard.nativeClipboard as? java.awt.datatransfer.Clipboard)
        _hasClip = nativeClipboard?.availableDataFlavors?.isNotEmpty() ?: false
        _hasText = nativeClipboard?.nativeClipboardHasText() ?: false
    }
}