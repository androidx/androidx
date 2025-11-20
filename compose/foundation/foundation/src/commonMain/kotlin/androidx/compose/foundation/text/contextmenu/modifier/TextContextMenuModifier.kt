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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo

/**
 * Adds a [builder] to be run when the text context menu is shown within this hierarchy.
 *
 * When there are multiple instances of this modifier in a layout hierarchy, the [builder]s are
 * applied in order from bottom to top. They are then filtered by every
 * [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents] in the hierarchy.
 *
 * @param builder a snapshot-aware builder function for adding components to the context menu. In
 *   this function you can use member functions from the receiver [TextContextMenuBuilderScope],
 *   such as [separator()][TextContextMenuBuilderScope.separator], to add components. The `item`
 *   function is not in the common source set, but is instead defined as an extension function in
 *   the platform specific source sets.
 * @sample androidx.compose.foundation.samples.AppendComponentsToTextContextMenu
 */
fun Modifier.appendTextContextMenuComponents(
    builder: TextContextMenuBuilderScope.() -> Unit
): Modifier = this then AddTextContextMenuDataComponentsElement(builder)

/**
 * Adds a [filter] to be run when the text context menu is shown within this hierarchy.
 *
 * [filter] will not be passed [TextContextMenuSeparator], as they pass by default.
 *
 * [filter]s added via this modifier will always run after every `builder` added via
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents]. When there are
 * multiple instances of this modifier in a layout hierarchy, every [filter] must pass in order for
 * a context menu to be shown. They are always applied after all
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents] have been applied,
 * but the order in which they run should not be depended on.
 *
 * @param filter a snapshot-aware lambda that determines whether a [TextContextMenuComponent] should
 *   be included in the context menu.
 * @sample androidx.compose.foundation.samples.AddFilterToTextContextMenu
 */
fun Modifier.filterTextContextMenuComponents(
    filter: (TextContextMenuComponent) -> Boolean
): Modifier = this then FilterTextContextMenuDataComponentsElement(filter)

private class AddTextContextMenuDataComponentsElement(
    private val builder: TextContextMenuBuilderScope.() -> Unit
) : ModifierNodeElement<AddTextContextMenuDataComponentsNode>() {
    override fun create(): AddTextContextMenuDataComponentsNode =
        AddTextContextMenuDataComponentsNode(builder)

    override fun update(node: AddTextContextMenuDataComponentsNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponents"
        properties["builder"] = builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddTextContextMenuDataComponentsElement) return false

        if (builder !== other.builder) return false

        return true
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class FilterTextContextMenuDataComponentsElement(
    private val filter: (TextContextMenuComponent) -> Boolean
) : ModifierNodeElement<FilterTextContextMenuDataComponentsNode>() {
    override fun create(): FilterTextContextMenuDataComponentsNode =
        FilterTextContextMenuDataComponentsNode(filter)

    override fun update(node: FilterTextContextMenuDataComponentsNode) {
        node.filter = filter
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "filterTextContextMenuDataComponents"
        properties["filter"] = filter
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterTextContextMenuDataComponentsElement) return false

        if (filter !== other.filter) return false

        return true
    }

    override fun hashCode(): Int = filter.hashCode()
}

private data object TextContextMenuDataTraverseKey

internal class AddTextContextMenuDataComponentsNode(
    var builder: TextContextMenuBuilderScope.() -> Unit
) : Modifier.Node(), TraversableNode {
    override val traverseKey: Any
        get() = TextContextMenuDataTraverseKey
}

private class FilterTextContextMenuDataComponentsNode(
    var filter: (TextContextMenuComponent) -> Boolean
) : Modifier.Node(), TraversableNode {
    override val traverseKey: Any
        get() = TextContextMenuDataTraverseKey
}

private const val continueTraversal = true
private const val wrongNodeTypeErrorMessage =
    "TextContextMenuDataNode.TraverseKey key must only be attached to instances of " +
        "TextContextMenuDataNode."

/**
 * Traverses ancestors to find all
 * [Modifier.appendTextContextMenuComponents][appendTextContextMenuComponents] and
 * [Modifier.filterTextContextMenuComponents][filterTextContextMenuComponents] modifiers and runs
 * [builderBlock] and [filterBlock] for each respectively. Each block allows the caller to make use
 * of the `filter` and `builder` parameters of each of the related modifiers.
 */
private fun DelegatableNode.traverseTextContextMenuDataNodes(
    filterBlock: ((TextContextMenuComponent) -> Boolean) -> Unit,
    builderBlock: (TextContextMenuBuilderScope.() -> Unit) -> Unit,
) {
    traverseAncestors(TextContextMenuDataTraverseKey) { node ->
        when (node) {
            is AddTextContextMenuDataComponentsNode -> builderBlock(node.builder)
            is FilterTextContextMenuDataComponentsNode -> filterBlock(node.filter)
            else -> throw IllegalStateException(wrongNodeTypeErrorMessage)
        }
        continueTraversal
    }
}

internal fun DelegatableNode.collectTextContextMenuData(): TextContextMenuData =
    TextContextMenuBuilderScope()
        .apply {
            traverseTextContextMenuDataNodes(
                filterBlock = ::addFilter,
                builderBlock = { builder -> this.builder() },
            )
        }
        .build()
