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

package androidx.compose.foundation.text.contextmenu.data

import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A [TextContextMenuComponent] that represents a clickable item with a label in a context menu.
 *
 * @param key A unique key that identifies this component, mainly for use in filtering a component
 *   in [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents]. It is advisable
 *   to use a `data object` as a key here.
 * @param label The label text to be shown in the context menu.
 * @param leadingIcon Icon that precedes the label in the context menu.
 * @param onClick The action to be performed when this item is clicked. Call
 *   [TextContextMenuSession.close] on the [TextContextMenuSession] receiver to close the context
 *   menu item as a result of the click.
 */
// TODO(grantapher-cm-api-publicize) Make class public
internal class TextContextMenuItemWithComposableLeadingIcon
internal constructor(
    key: Any,
    val label: String,
    val enabled: Boolean,
    val leadingIcon: (@Composable (color: Color) -> Unit)? = null,
    val onClick: TextContextMenuSession.() -> Unit,
) : TextContextMenuComponent(key) {
    override fun toString(): String =
        "TextContextMenuItem(key=$key, label=\"$label\", enabled=$enabled)"
}
