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

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.text.DesktopTextContextMenuItems
import androidx.compose.foundation.text.DesktopTextContextMenuItems.Copy
import androidx.compose.foundation.text.DesktopTextContextMenuItems.SelectAll
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithLocalization
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key

// this doesn't sounds very sustainable
// it would end up being a function for any conceptual keyevent (selectall, cut, copy, paste)
// TODO(b/1564937)
internal actual fun isCopyKeyEvent(keyEvent: KeyEvent) =
    keyEvent.key == Key.C && when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> keyEvent.isMetaPressed
        else -> keyEvent.isCtrlPressed
    } || keyEvent.key == Key.Copy

/**
 * Magnification is not supported on desktop.
 */
internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier = this

internal actual fun Modifier.addSelectionContainerTextContextMenuComponents(
    selectionManager: SelectionManager,
): Modifier = addTextContextMenuComponentsWithLocalization { localization ->
    fun TextContextMenuBuilderScope.selectionContainerItem(
        item: DesktopTextContextMenuItems,
        enabled: Boolean,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit
    ) {
        item(
            key = item.key,
            label = item.label(localization),
            enabled = enabled,
            onClick = {
                onClick()
                if (closePredicate?.invoke() != false) close()
            }
        )
    }

    with(selectionManager) {
        separator()
        selectionContainerItem(Copy, enabled = isNonEmptySelection()) { copy() }
        selectionContainerItem(
            item = SelectAll,
            enabled = !isEntireContainerSelected(),
            closePredicate = { !showToolbar || !isInTouchMode },
        ) {
            selectAll()
        }
        separator()
    }
}
