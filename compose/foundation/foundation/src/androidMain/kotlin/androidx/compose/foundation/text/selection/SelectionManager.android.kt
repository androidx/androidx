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

import androidx.compose.foundation.PlatformMagnifierFactory
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.Copy
import androidx.compose.foundation.text.TextContextMenuItems.SelectAll
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithContext
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.foundation.text.textItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun isCopyKeyEvent(keyEvent: KeyEvent) =
    platformDefaultKeyMapping.map(keyEvent) == KeyCommand.COPY

// We use composed{} to read a local, but don't provide inspector info because the underlying
// magnifier modifier provides more meaningful inspector info.
internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier {
    // Avoid tracking animation state on older Android versions that don't support magnifiers.
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }
        animatedSelectionMagnifier(
            magnifierCenter = { calculateSelectionMagnifierCenterAndroid(manager, magnifierSize) },
            platformMagnifier = { center ->
                Modifier.magnifier(
                    sourceCenter = { center() },
                    onSizeChanged = { size ->
                        magnifierSize =
                            with(density) {
                                IntSize(size.width.roundToPx(), size.height.roundToPx())
                            }
                    },
                    useTextDefault = true,
                    platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform(),
                )
            },
        )
    }
}

internal actual fun Modifier.addSelectionContainerTextContextMenuComponents(
    selectionManager: SelectionManager
): Modifier = addTextContextMenuComponentsWithContext { context ->
    fun TextContextMenuBuilderScope.selectionContainerItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit,
    ) {
        textItem(context.resources, item, enabled) {
            onClick()
            if (closePredicate?.invoke() ?: true) close()
        }
    }

    val textAndSelection = selectionManager.getContextTextAndSelection()
    addPlatformTextContextMenuItems(
        context = context,
        editable = false,
        text = textAndSelection?.first,
        selection = textAndSelection?.second,
        platformSelectionBehaviors = selectionManager.platformSelectionBehaviors,
    ) {
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
}
