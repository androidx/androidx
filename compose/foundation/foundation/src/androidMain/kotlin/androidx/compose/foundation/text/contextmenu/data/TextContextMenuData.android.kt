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

import android.content.res.Resources
import android.view.textclassifier.TextClassification
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents

/**
 * A [TextContextMenuComponent] that represents a clickable item with a label in a context menu.
 *
 * @param key A unique key that identifies this component, mainly for use in filtering a component
 *   in [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents]. It is advisable
 *   to use a `data object` as a key here.
 * @param label The label text to be shown in the context menu.
 * @param leadingIcon Icon that precedes the label in the context menu. This is a drawable resource
 *   reference.
 * @param onClick The action to be performed when this item is clicked. Call
 *   [TextContextMenuSession.close] on the [TextContextMenuSession] receiver to close the context
 *   menu item as a result of the click.
 */
class TextContextMenuItem(
    key: Any,
    val label: String,
    val leadingIcon: Int = Resources.ID_NULL,
    val onClick: TextContextMenuSession.() -> Unit,
) : TextContextMenuComponent(key) {
    override fun toString(): String =
        "TextContextMenuItem(key=$key, label=\"$label\", leadingIcon=$leadingIcon)"
}

internal class TextContextMenuTextClassificationItem(
    key: Any,
    val textClassification: TextClassification,
    val index: Int,
) : TextContextMenuComponent(key) {
    override fun toString(): String =
        "TextContextMenuRemoteActionItem(key=$key, textClassification=$textClassification, index=$index)"
}

/**
 * Key for context menu items added for the Android PROCESS_TEXT intent actions. You can use this
 * key to filter the PROCESS_TEXT components by calling
 * [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents].
 *
 * @sample androidx.compose.foundation.samples.FilterProcessTextItemsInTextContextMenu
 */
class ProcessTextKey
internal constructor(
    /**
     * There can be multiple PROCESS_TEXT items in the context menu and each of them has a different
     * id.
     */
    val id: Int
) {
    override fun equals(other: Any?): Boolean {
        if (other !is ProcessTextKey) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}
