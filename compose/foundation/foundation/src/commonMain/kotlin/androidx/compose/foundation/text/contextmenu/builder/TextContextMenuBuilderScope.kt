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

package androidx.compose.foundation.text.contextmenu.builder

import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents

/**
 * Scope for building a text context menu in
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents]. See member functions
 * for how to add context menu components to this scope as part of the
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents] modifier. The `item`
 * function is not in the common source set, but is instead defined as an extension function in the
 * platform specific source sets.
 */
class TextContextMenuBuilderScope internal constructor() {
    private val components = mutableObjectListOf<TextContextMenuComponent>()
    private val filters = mutableObjectListOf<(TextContextMenuComponent) -> Boolean>()

    /**
     * Build the current state of this into a [TextContextMenuData]. This applies a few filters to
     * the components:
     * * No back-to-back separators.
     * * No heading or tailing separators.
     * * Applies [filters] to each component, excluding separators.
     */
    internal fun build(): TextContextMenuData {
        val resultList = mutableObjectListOf<TextContextMenuComponent>()

        var headIsSeparator = true
        var previous: TextContextMenuComponent? = null
        components.forEach { current ->
            // remove heading separators
            if (headIsSeparator && current === TextContextMenuSeparator) return@forEach
            headIsSeparator = false

            // remove back-to-back separators
            if (current.isSeparator && previous.isSeparator) return@forEach

            // apply `filters` unless a component is a separator
            if (!current.isSeparator && filters.any { filter -> !filter(current) }) return@forEach

            resultList += current
            previous = current
        }

        // remove the remaining trailing separator, if there is one.
        if (resultList.lastOrNull().isSeparator) {
            @Suppress("Range") // lastIndex will not be -1 because the list cannot be empty
            resultList.removeAt(resultList.lastIndex)
        }

        @Suppress("AsCollectionCall") // need to use asList as this enters a public api.
        return TextContextMenuData(components = resultList.asList())
    }

    /**
     * Adds a [filter] to be applied to each component in [components] when the [build] method is
     * called.
     *
     * @param filter A predicate that determines whether a [TextContextMenuComponent] should be
     *   added to the final list of components. This filter will never receive a
     *   [TextContextMenuSeparator] since they are excluded from filtering.
     */
    internal fun addFilter(filter: (TextContextMenuComponent) -> Boolean) {
        filters += filter
    }

    internal fun addComponent(component: TextContextMenuComponent) {
        components += component
    }

    /**
     * Adds a separator to the list of text context menu components. Successive separators will be
     * combined into a single separator.
     */
    fun separator() {
        components += TextContextMenuSeparator
    }
}

internal val TextContextMenuComponent?.isSeparator: Boolean
    get() = this === TextContextMenuSeparator
