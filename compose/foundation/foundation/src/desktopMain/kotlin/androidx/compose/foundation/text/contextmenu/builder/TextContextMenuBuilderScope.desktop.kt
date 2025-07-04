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

package androidx.compose.foundation.text.contextmenu.builder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Adds an item to the list of text context menu components.
 *
 * @param key A unique key that identifies this item. Used to identify context menu items in the
 *   context menu. It is advisable to use a `data object` as a key here.
 * @param label string to display as the text of the item.
 * @param leadingIcon Icon that precedes the label in the context menu.
 * @param onClick Action to perform upon the item being clicked/pressed.
 */
@ExperimentalFoundationApi
fun TextContextMenuBuilderScope.item(
    key: Any,
    label: String,
    enabled: Boolean = true,
    leadingIcon: (@Composable (color: Color) -> Unit)? = null,
    onClick: TextContextMenuSession.() -> Unit,
) {
    addComponent(TextContextMenuItemWithComposableLeadingIcon(key, label, enabled, leadingIcon, onClick))
}
