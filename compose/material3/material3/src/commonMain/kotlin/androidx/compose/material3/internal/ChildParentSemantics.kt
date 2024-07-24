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

package androidx.compose.material3.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

internal fun Modifier.childSemantics(properties: SemanticsPropertyReceiver.() -> Unit = {}) =
    this then ChildSemanticsNodeElement(properties)

internal fun Modifier.parentSemantics(properties: SemanticsPropertyReceiver.() -> Unit) =
    this then ParentSemanticsNodeElement(properties)

internal data class ChildSemanticsNodeElement(
    val properties: SemanticsPropertyReceiver.() -> Unit
) : ModifierNodeElement<ChildSemanticsNode>() {
    override fun create(): ChildSemanticsNode = ChildSemanticsNode(properties)

    override fun update(node: ChildSemanticsNode) {
        node.properties = properties
        node.invalidateSemantics()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "childSemantics"
        this@inspectableProperties.properties["properties"] =
            this@ChildSemanticsNodeElement.properties
    }
}

internal data class ParentSemanticsNodeElement(
    val properties: SemanticsPropertyReceiver.() -> Unit
) : ModifierNodeElement<ParentSemanticsNode>() {
    override fun create(): ParentSemanticsNode = ParentSemanticsNode(properties)

    override fun update(node: ParentSemanticsNode) {
        node.properties = properties
        node.invalidateSemantics()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "parentSemantics"
        this@inspectableProperties.properties["properties"] =
            this@ParentSemanticsNodeElement.properties
    }
}

internal class ChildSemanticsNode(var properties: SemanticsPropertyReceiver.() -> Unit) :
    Modifier.Node(), SemanticsModifierNode {

    override fun SemanticsPropertyReceiver.applySemantics() {
        traverseAncestors(ParentSemanticsNodeKey) { node ->
            with(node as ParentSemanticsNode) {
                obtainSemantics()
                false
            }
        }
        properties()
    }

    override fun onDetach() {
        super.onDetach()
        traverseAncestors(ParentSemanticsNodeKey) { node ->
            (node as ParentSemanticsNode)
            node.releaseSemantics()
            false
        }
    }
}

internal class ParentSemanticsNode(var properties: SemanticsPropertyReceiver.() -> Unit) :
    Modifier.Node(), TraversableNode, SemanticsModifierNode {

    private var semanticsConsumed: Boolean = false

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    override val traverseKey: Any = ParentSemanticsNodeKey

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (!semanticsConsumed) {
            properties()
        }
    }

    fun SemanticsPropertyReceiver.obtainSemantics() {
        semanticsConsumed = true
        properties()
        invalidateSemantics()
    }

    fun releaseSemantics() {
        semanticsConsumed = false
        invalidateSemantics()
    }
}

internal object ParentSemanticsNodeKey
