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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.SelectionMagnifierElement
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.addTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable

internal actual fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean =
    platformDefaultKeyMapping.map(keyEvent) == KeyCommand.COPY

internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier = if (isPlatformMagnifierSupported()) {
    this.then(
        SelectionMagnifierElement(
            manager = manager,
            hapticFeedback = { hapticFeedBack },
            calculateCenter = { size ->
                calculateSelectionMagnifierCenterAndroid(manager, size)
            }
        )
    )
} else {
    inspectable(
        // Publish inspector info even if magnification isn't supported.
        inspectorInfo = debugInspectorInfo {
            name = "selectionMagnifier (not supported)"
            properties["manager"] = manager
        }
    ) { this }
}

internal actual fun Modifier.addSelectionContainerTextContextMenuComponents(
    selectionManager: SelectionManager
): Modifier = addTextContextMenuComponents {
    fun TextContextMenuBuilderScope.selectionContainerItem(
        key: Any,
        enabled: Boolean,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit
    ) {
        addComponent(
            TextContextMenuItemWithComposableLeadingIcon(
                key = key,
                label = "$key",
                enabled = enabled,
                onClick = {
                    onClick()
                    if (closePredicate?.invoke() != false) close()
                })
        )
    }

    with(selectionManager) {
        separator()
        selectionContainerItem(
            key = TextContextMenuKeys.CopyKey,
            enabled = isNonEmptySelection()
        ) { copy() }
        selectionContainerItem(
            key = TextContextMenuKeys.SelectAllKey,
            enabled = !isEntireContainerSelected(),
            closePredicate = { !showToolbar || !isInTouchMode },
        ) {
            selectAll()
        }
        separator()
    }
}
