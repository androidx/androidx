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

package androidx.compose.foundation.text.contextmenu.provider

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * The provider determines how the context menu is shown and its appearance.
 *
 * The context menu can be customized by providing another implementation of this to
 * [LocalTextContextMenuDropdownProvider] or [LocalTextContextMenuToolbarProvider] via a
 * [CompositionLocalProvider].
 *
 * If you want to modify the contents of the context menu, see
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents] and
 * [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents]
 */
interface TextContextMenuProvider {
    /**
     * Shows the text context menu.
     *
     * This function suspends until the context menu is closed. If the coroutine is cancelled, the
     * context menu will be closed.
     *
     * @param dataProvider provides the data necessary to show the text context menu.
     */
    suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider)
}

/** Provide a [TextContextMenuProvider] to be used for the text context menu dropdown. */
val LocalTextContextMenuDropdownProvider: ProvidableCompositionLocal<TextContextMenuProvider?> =
    compositionLocalOf {
        null
    }

/** Provide a [TextContextMenuProvider] to be used for the text context menu toolbar. */
val LocalTextContextMenuToolbarProvider: ProvidableCompositionLocal<TextContextMenuProvider?> =
    compositionLocalOf {
        null
    }

/**
 * Provides the data necessary to show the text context menu.
 *
 * All functions on this interface are expected to be snapshot-aware.
 */
interface TextContextMenuDataProvider {
    /**
     * Provides the position to place the context menu around. The position should be relative to
     * the provided [destinationCoordinates].
     *
     * This function is snapshot-aware.
     */
    fun position(destinationCoordinates: LayoutCoordinates): Offset

    /**
     * Provides a bounding box to place the context menu around. The position should be relative to
     * the provided [destinationCoordinates].
     *
     * This function is snapshot-aware.
     */
    fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect

    /**
     * Provides the components used to fill the context menu.
     *
     * This function is snapshot-aware.
     */
    fun data(): TextContextMenuData
}
