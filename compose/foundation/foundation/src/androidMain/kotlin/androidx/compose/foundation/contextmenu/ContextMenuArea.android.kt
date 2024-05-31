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
import androidx.compose.ui.unit.round

/**
 * Wraps [content] with the necessary components to show a context menu in it.
 *
 * @param state The state that controls the context menu popup.
 * @param onDismiss Lambda to execute when the user clicks outside of the popup.
 * @param contextMenuBuilderBlock Block which builds the context menu.
 * @param modifier Modifier to apply to the Box surrounding the context menu and the content.
 * @param enabled Whether the context menu is enabled.
 * @param content The content that will have the context menu enabled.
 */
@Composable
internal fun ContextMenuArea(
    state: ContextMenuState,
    onDismiss: () -> Unit,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val finalModifier = if (enabled) modifier.contextMenuGestures(state) else modifier
    Box(finalModifier, propagateMinConstraints = true) {
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

    val popupPositionProvider =
        remember(status) { ContextMenuPopupPositionProvider(status.offset.round()) }

    ContextMenuPopup(
        modifier = modifier,
        popupPositionProvider = popupPositionProvider,
        onDismiss = onDismiss,
        contextMenuBuilderBlock = contextMenuBuilderBlock,
    )
}
