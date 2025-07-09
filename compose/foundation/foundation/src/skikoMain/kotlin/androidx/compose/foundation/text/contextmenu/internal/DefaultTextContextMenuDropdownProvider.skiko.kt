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

package androidx.compose.foundation.text.contextmenu.internal

import androidx.compose.foundation.DefaultOpenContextMenu
import androidx.compose.foundation.contextmenu.ContextMenuPopupPositionProvider
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.BasicTextContextMenuProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.ProvideBasicTextContextMenu
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.basicTextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider

// TODO: This is (mostly) a copy of DefaultTextContextMenuDropdownProvider.android.kt;  we should
//       move it to common and upstream
//   https://youtrack.jetbrains.com/issue/CMP-8453/Commonize-and-upstream-shared-code-in-new-context-menu

// TODO(grantapher) Consider making public.
@Composable
internal fun ProvideDefaultTextContextMenuDropdown(
    content: @Composable () -> Unit
) {
    ProvideBasicTextContextMenu(
        providableCompositionLocal = LocalTextContextMenuDropdownProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
        },
        content = content
    )
}

@Composable
internal fun ProvideDefaultTextContextMenuDropdown(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    ProvideBasicTextContextMenu(
        modifier = modifier,
        providableCompositionLocal = LocalTextContextMenuDropdownProvider,
        contextMenu = { session, dataProvider, anchorLayoutCoordinates ->
            OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
        },
        content = content
    )
}

@Composable
internal fun defaultTextContextMenuDropdown(): BasicTextContextMenuProvider =
    basicTextContextMenuProvider { session, dataProvider, anchorLayoutCoordinates ->
        OpenContextMenu(session, dataProvider, anchorLayoutCoordinates)
    }

@Composable
private fun OpenContextMenu(
    session: TextContextMenuSession,
    dataProvider: TextContextMenuDataProvider,
    anchorLayoutCoordinates: () -> LayoutCoordinates,
) {
    val popupPositionProvider =
        remember(dataProvider) {
            MaintainWindowPositionPopupPositionProvider(
                ContextMenuPopupPositionProvider({
                    dataProvider.position(anchorLayoutCoordinates()).round()
                })
            )
        }
    val data by remember(dataProvider) { derivedStateOf(dataProvider::data) }
    DefaultOpenContextMenu(
        session = session,
        components = data.components,
        popupPositionProvider = popupPositionProvider,
    )
}

/**
 * Delegates to the [popupPositionProvider], but re-uses the previous calculated position if the
 * only change is the `anchorBounds` in the window. This ensures that anchor layout movement such as
 * scrolls do not cause the popup to move, but other relevant layout changes do move the popup.
 *
 * We do want to re-calculate a new position for any `windowSize`, `layoutDirection`, and
 * `popupContentSize` changes since they may make the previous popup position un-viable.
 */
// TODO(grantapher) Consider making public.
private class MaintainWindowPositionPopupPositionProvider(
    val popupPositionProvider: PopupPositionProvider
) : PopupPositionProvider {
    var previousWindowSize: IntSize? = null
    var previousLayoutDirection: LayoutDirection? = null
    var previousPopupContentSize: IntSize? = null

    var previousPosition: IntOffset? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val position = previousPosition
        if (
            position != null &&
            previousWindowSize == windowSize &&
            previousLayoutDirection == layoutDirection &&
            previousPopupContentSize == popupContentSize
        ) {
            return position
        }

        val newPosition =
            popupPositionProvider.calculatePosition(
                anchorBounds,
                windowSize,
                layoutDirection,
                popupContentSize,
            )

        previousWindowSize = windowSize
        previousLayoutDirection = layoutDirection
        previousPopupContentSize = popupContentSize
        previousPosition = newPosition
        return newPosition
    }
}
