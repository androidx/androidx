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

package androidx.compose.foundation.text

import androidx.compose.foundation.contextmenu.ContextMenuPopupPositionProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.WebTextToolbar
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

@Composable
internal fun WebTextToolbarArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier, propagateMinConstraints = true) {
        content()
        WebTextToolbarPopup()
    }
}

@OptIn(InternalComposeUiApi::class)
@Composable
private fun WebTextToolbarPopup() {
    val textToolbar = LocalTextToolbar.current as? WebTextToolbar ?: return

    // we must remove a parent offset because
    // TextToolbar's show callback position is already in the window
    var layoutParentBoundsInWindow: IntRect by remember { mutableStateOf(IntRect.Zero) }
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            childCoordinates.parentCoordinates?.let {
                val layoutPosition = it.positionInWindow().round()
                val layoutSize = it.size
                layoutParentBoundsInWindow = IntRect(layoutPosition, layoutSize)
            }
        },
        measurePolicy = { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {}
        }
    )

    val menu = textToolbar.menu ?: return
    val toolbarOffset = with(LocalDensity.current) {
        (WebContextMenuToolbarSpec.ContainerHeight + 16.dp).roundToPx()
    }

    val popupPositionProvider = remember(menu) {
        ContextMenuPopupPositionProvider(
            anchorPosition = Offset(
                menu.rect.left,
                menu.rect.top - toolbarOffset
            ).round() - layoutParentBoundsInWindow.topLeft,
        )
    }

    WebContextMenuToolbarPopup(
        popupPositionProvider = popupPositionProvider,
        onDismiss = { /* visibility depends on a text selection */ },
        contextMenuBuilderBlock = {
            item(
                label = { getString(Strings.Copy) },
                enabled = menu.onCopyRequested != null,
                onClick = { menu.onCopyRequested?.invoke() }
            )
            item(
                label = { getString(Strings.Paste) },
                enabled = menu.onPasteRequested != null,
                onClick = { menu.onPasteRequested?.invoke() }
            )
            item(
                label = { getString(Strings.Cut) },
                enabled = menu.onCutRequested != null,
                onClick = { menu.onCutRequested?.invoke() }
            )
            item(
                label = { getString(Strings.SelectAll) },
                enabled = menu.onSelectAllRequested != null,
                onClick = { menu.onSelectAllRequested?.invoke() }
            )
        }
    )
}
