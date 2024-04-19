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

package androidx.compose.foundation.contextmenu

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Wraps [content] with the necessary components to show a context menu in it.
 *
 * @param state The state that controls the context menu popup.
 * @param modifier Modifier to apply to the Box surrounding the context menu and the content.
 * @param onDismiss Lambda to execute when the user clicks outside of the popup.
 * @param contextMenuBuilderBlock Block which builds the context menu.
 * @param content The content that will have the context menu enabled.
 */
@Composable
internal fun ContextMenuArea(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.contextMenuGestures(state), propagateMinConstraints = true) {
        content()
        ContextMenu(
            state = state,
            onDismiss = onDismiss,
            contextMenuBuilderBlock = contextMenuBuilderBlock
        )
    }
}

@VisibleForTesting
@Composable
internal fun ContextMenu(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    val status = state.status
    if (status !is Status.Open) return

    val popupPositionProvider = remember(status) { StaticPositionProvider(status.offset.round()) }

    ContextMenuPopup(
        modifier = modifier,
        popupPositionProvider = popupPositionProvider,
        onDismiss = onDismiss,
        contextMenuBuilderBlock = contextMenuBuilderBlock,
    )
}

// TODO(b/331958453) Create a smarter provider that will handle
//  moving the menu outside of the click position when
//  the menu is in the bottom right corner.
private class StaticPositionProvider(
    // The position should be local to the layout the popup is attached to.
    private val localPosition: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = anchorBounds.topLeft + localPosition
}
