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

package androidx.pdf.selection

import android.content.Context
import androidx.pdf.selection.model.GoToLinkSelection
import androidx.pdf.selection.model.HyperLinkSelection
import androidx.pdf.selection.model.TextSelection

/**
 * Manages the retrieval and caching of context menu items for various types of selections within a
 * PDF viewer.
 *
 * This class serves as a centralized manager to provide context-specific menu options when a user
 * interacts with selected content. It optimizes [ContextMenuComponent] list by caching the
 * previously generated menu items.
 *
 * @property context The [Context] used to initialize underlying menu providers, such as
 *   [TextSelectionMenuProvider].
 */
internal class SelectionMenuManager(private val context: Context) {

    /**
     * Caches the most recent [Selection] and its generated [ContextMenuComponent] list.
     *
     * This optimization prevents redundant computations for identical selections, especially during
     * frequent UI updates like zooming, scrolling, or orientation changes.
     */
    private data class SelectionCache(
        val selection: Selection,
        val menuItems: List<ContextMenuComponent>,
    )

    private var cachedSelection: SelectionCache? = null
    private val goToLinkSelectionMenuProvider = GoToLinkSelectionMenuProvider(context)
    private val hyperLinkSelectionMenuProvider = HyperLinkSelectionMenuProvider(context)
    private val textSelectionMenuProvider = TextSelectionMenuProvider(context)

    suspend fun getSelectionMenuItems(selection: Selection): List<ContextMenuComponent> {
        // Check if the current selection is already cached
        cachedSelection?.let { localCachedSelection ->
            if (localCachedSelection.selection == selection) {
                return localCachedSelection.menuItems
            }
        }
        // If it's a new selection or no selection cached, compute and cache it
        return when (selection) {
            is TextSelection -> {
                val newMenuItems = textSelectionMenuProvider.getMenuItems(selection)
                cachedSelection = SelectionCache(selection, newMenuItems)
                newMenuItems
            }
            is HyperLinkSelection -> {
                val newMenuItems: MutableList<ContextMenuComponent> = mutableListOf()
                // Filter link to provide better context menu options depending upon link type.
                val link = HyperLinkSelectionMenuProvider.filterLink(selection.link.toString())
                // We use the TextSelectionMenuProvider here because it's already designed to
                // generate smart action items using the TextClassifier API, which is ideal for
                // creating relevant menu options for a hyperlink's URL.
                newMenuItems += textSelectionMenuProvider.getSmartMenuItems(link)
                newMenuItems += hyperLinkSelectionMenuProvider.getMenuItems(selection)
                cachedSelection = SelectionCache(selection, newMenuItems)
                newMenuItems
            }
            is GoToLinkSelection -> {
                val newMenuItems = goToLinkSelectionMenuProvider.getMenuItems(selection)
                cachedSelection = SelectionCache(selection, newMenuItems)
                newMenuItems
            }
            else -> emptyList()
        }
    }
}
