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

package androidx.compose.material3.adaptive.layout.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import kotlinx.coroutines.CoroutineScope

internal interface DelegableSemanticsPropertyReceiver :
    SemanticsPropertyReceiver, CompositionLocalConsumerModifierNode {
    val coroutineScope: CoroutineScope
}

internal fun Modifier.delegableSemantics(
    mergeDescendants: Boolean = false,
    properties: (DelegableSemanticsPropertyReceiver.() -> Unit),
) = this.then(DelegableSemanticsElement(mergeDescendants, properties))

private class DelegableSemanticsElement(
    val shouldMergeDescendantSemantics: Boolean,
    val properties: (DelegableSemanticsPropertyReceiver.() -> Unit),
) : ModifierNodeElement<DelegableSemanticsNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "defaultPaneExpansionDragHandleSemantics"
        properties["shouldMergeDescendantSemantics"] = shouldMergeDescendantSemantics
        properties["properties"] = properties
    }

    override fun create(): DelegableSemanticsNode {
        return DelegableSemanticsNode(shouldMergeDescendantSemantics, properties)
    }

    override fun update(node: DelegableSemanticsNode) {
        node.shouldMergeDescendantSemantics = shouldMergeDescendantSemantics
        node.properties = properties
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = shouldMergeDescendantSemantics.hashCode()
        result = 31 * result + properties.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelegableSemanticsElement) return false

        if (shouldMergeDescendantSemantics != other.shouldMergeDescendantSemantics) return false
        if (properties !== other.properties) return false

        return true
    }
}

private class DelegableSemanticsNode(
    override var shouldMergeDescendantSemantics: Boolean,
    var properties: (DelegableSemanticsPropertyReceiver.() -> Unit),
) : Modifier.Node(), CompositionLocalConsumerModifierNode, SemanticsModifierNode {

    override fun SemanticsPropertyReceiver.applySemantics() =
        object :
                DelegableSemanticsPropertyReceiver,
                SemanticsPropertyReceiver by this,
                CompositionLocalConsumerModifierNode by this@DelegableSemanticsNode {
                override val coroutineScope
                    get() = this@DelegableSemanticsNode.coroutineScope
            }
            .properties()
}
