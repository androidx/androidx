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

import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.modifier.AddTextContextMenuDataComponentsNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal fun Modifier.addTextContextMenuComponents(
    builder: TextContextMenuBuilderScope.() -> Unit,
): Modifier = this then AddTextContextMenuDataComponentsElement(builder)

private class AddTextContextMenuDataComponentsElement(
    private val builder: TextContextMenuBuilderScope.() -> Unit,
) : ModifierNodeElement<androidx.compose.foundation.text.AddTextContextMenuDataComponentsNode>() {
    override fun create(): androidx.compose.foundation.text.AddTextContextMenuDataComponentsNode =
        androidx.compose.foundation.text.AddTextContextMenuDataComponentsNode(builder)

    override fun update(node: androidx.compose.foundation.text.AddTextContextMenuDataComponentsNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponentsWithLocalization"
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

private class AddTextContextMenuDataComponentsNode(
    var builder: TextContextMenuBuilderScope.() -> Unit,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(AddTextContextMenuDataComponentsNode { builder() })
    }
}
