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

import android.os.Build
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.text.MenuItemsAvailability
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.Autofill
import androidx.compose.foundation.text.TextContextMenuItems.Copy
import androidx.compose.foundation.text.TextContextMenuItems.Cut
import androidx.compose.foundation.text.TextContextMenuItems.Paste
import androidx.compose.foundation.text.TextContextMenuItems.SelectAll
import androidx.compose.foundation.text.TextItem
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithContext
import androidx.compose.foundation.text.selection.addPlatformTextContextMenuItems
import androidx.compose.foundation.text.textItem
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

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
    if (Build.VERSION.SDK_INT >= 26) {
        textFieldItem(Autofill, enabled = availability.canAutofill)
    }
}

// TODO(halilibo): Add a new TextToolbar option "paste as plain text".
internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier = addTextContextMenuComponentsWithContext { context ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        desiredState: TextToolbarState = TextToolbarState.None,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit,
    ) {
        textItem(context.resources, item, enabled) {
            onClick()
            if (closePredicate?.invoke() ?: true) close()
            state.updateTextToolbarState(desiredState)
        }
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

    addPlatformTextContextMenuItems(
        context = context,
        editable = state.editable,
        text = state.textFieldState.visualText.text,
        selection = state.textFieldState.visualText.selection,
        platformSelectionBehaviors = state.platformSelectionBehaviors,
    ) {
        with(state) {
            separator()
            textFieldSuspendItem(Cut, enabled = canCut()) { cut() }
            textFieldSuspendItem(Copy, enabled = canCopy()) {
                copy(cancelSelection = textToolbarShown)
            }
            textFieldSuspendItem(Paste, enabled = canPaste()) { paste() }
            textFieldItem(
                item = SelectAll,
                enabled = canSelectAll(),
                desiredState = TextToolbarState.Selection,
                closePredicate = { !textToolbarShown },
            ) {
                selectAll()
            }
            if (Build.VERSION.SDK_INT >= 26) {
                textFieldItem(Autofill, enabled = canAutofill()) { autofill() }
            }
            separator()
        }
    }
}

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip: Boolean = false
    private var _hasText: Boolean = false

    actual val hasText: Boolean
        get() = _hasText

    actual val hasClip: Boolean
        get() = _hasClip

    actual suspend fun update() {
        // On Android, we don't need to read `clipEntry` to evaluate `canPaste`.
        // Reading `clipEntry` directly can trigger a "App pasted from Clipboard" system warning.
        _hasClip = clipboard.nativeClipboard.hasPrimaryClip()
        _hasText =
            _hasClip &&
                clipboard.nativeClipboard.primaryClipDescription?.hasMimeType("text/*") == true
    }
}
