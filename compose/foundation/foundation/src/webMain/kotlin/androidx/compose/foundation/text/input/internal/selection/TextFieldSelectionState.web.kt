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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.Copy
import androidx.compose.foundation.text.TextContextMenuItems.Cut
import androidx.compose.foundation.text.TextContextMenuItems.Paste
import androidx.compose.foundation.text.TextContextMenuItems.SelectAll
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.getLocalizedString
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** Runs platform-specific text tap gestures logic. */
internal actual suspend fun TextFieldSelectionState.detectTextFieldTapGestures(
    pointerInputScope: PointerInputScope,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit
) = defaultDetectTextFieldTapGestures(pointerInputScope, interactionSource, requestFocus, showKeyboard)

/** Runs platform-specific text selection gestures logic. */
internal actual suspend fun TextFieldSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) = pointerInputScope.defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope
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

    with(state) {
        separator()
        textFieldSuspendItem(Cut, enabled = canShowCutMenuItem()) { cut() }
        textFieldSuspendItem(Copy, enabled = canShowCopyMenuItem()) { copy(cancelSelection = false) }
        textFieldSuspendItem(Paste, enabled = canShowPasteMenuItem()) { paste() }
        textFieldItem(SelectAll, enabled = canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}