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

package androidx.pdf.compose

import androidx.pdf.selection.ContextMenuComponent
import androidx.pdf.selection.SelectionMenuComponent
import androidx.pdf.selection.SelectionMenuSession

/** Scope for building a selection menu in [PdfViewer]. */
public class PdfSelectionMenuBuilderScope internal constructor() {

    internal val menuItems: List<ContextMenuComponent>
        get() = _menuItems

    private val _menuItems = mutableListOf<ContextMenuComponent>()

    /**
     * Adds an item in the selection menu.
     *
     * @param key A unique identifier for this component.
     * @param label The text to display for this menu item.
     * @param contentDescription An optional accessibility description for screen readers.
     * @param onClick A lambda function invoked when the item is clicked, providing access to the
     *   [androidx.pdf.selection.SelectionMenuSession].
     */
    public fun item(
        key: Any,
        label: String,
        contentDescription: String?,
        onClick: SelectionMenuSession.() -> Unit,
    ) {
        _menuItems.add(SelectionMenuComponent(key, label, contentDescription, onClick))
    }
}
