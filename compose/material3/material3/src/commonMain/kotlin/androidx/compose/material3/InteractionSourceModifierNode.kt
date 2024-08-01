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

package androidx.compose.material3

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * Traversable node that holds the interaction source of the current node and provides the ability
 * to obtain the interaction sources of its descendants.
 *
 * @property interactionSource the [MutableInteractionSource] associated with this current node.
 */
private class InteractionSourceModifierNode(var interactionSource: MutableInteractionSource) :
    Modifier.Node(), TraversableNode {
    override val traverseKey: Any = InteractionSourceModifierNodeTraverseKey
}

/**
 * Node that calls [onChildrenInteractionSourceChange] when there is a remeasure due to child
 * elements changing.
 *
 * @property onChildrenInteractionSourceChange callback that is invoked on remeasure.
 */
private class OnChildrenInteractionSourceChangeModifierNode(
    var onChildrenInteractionSourceChange: (List<InteractionSource>) -> Unit
) : LayoutAwareModifierNode, Modifier.Node(), DelegatableNode {
    override fun onRemeasured(size: IntSize) {
        super.onRemeasured(size)
        onChildrenInteractionSourceChange(findInteractionSources())
    }
}

/**
 * Finds the interaction sources of the descendants of this node that have the same traverse key.
 */
internal fun DelegatableNode.findInteractionSources(): List<MutableInteractionSource> {
    val interactionSources = mutableListOf<MutableInteractionSource>()
    traverseDescendants(InteractionSourceModifierNodeTraverseKey) {
        if (it is InteractionSourceModifierNode) {
            interactionSources.add(it.interactionSource)
        }
        TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
    }
    return interactionSources
}

/**
 * Modifier used to expose an interaction source to a parent.
 *
 * @param interactionSource the [MutableInteractionSource] associated with this current node.
 */
internal fun Modifier.interactionSourceData(
    interactionSource: MutableInteractionSource? = null
): Modifier =
    this then InteractionSourceModifierElement(interactionSource ?: MutableInteractionSource())

/**
 * Modifier used to observe interaction sources that are exposed by child elements, child elements
 * can provide interaction sources using [Modifier.interactionSourceData].
 *
 * @param onChildrenInteractionSourceChange callback invoked when children update their interaction
 *   sources.
 */
internal fun Modifier.onChildrenInteractionSourceChange(
    onChildrenInteractionSourceChange: (List<InteractionSource>) -> Unit
): Modifier =
    this then OnChildrenInteractionSourceChangeModifierElement(onChildrenInteractionSourceChange)

private data class InteractionSourceModifierElement(
    private val interactionSource: MutableInteractionSource
) : ModifierNodeElement<InteractionSourceModifierNode>() {
    override fun create() = InteractionSourceModifierNode(interactionSource)

    override fun update(node: InteractionSourceModifierNode) {
        node.interactionSource = interactionSource
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "interactionSourceModifierNode"
        properties["interactionSource"] = interactionSource
    }
}

private data class OnChildrenInteractionSourceChangeModifierElement(
    private val onChildrenInteractionSourceChange: (List<InteractionSource>) -> Unit
) : ModifierNodeElement<OnChildrenInteractionSourceChangeModifierNode>() {
    override fun create() =
        OnChildrenInteractionSourceChangeModifierNode(onChildrenInteractionSourceChange)

    override fun update(node: OnChildrenInteractionSourceChangeModifierNode) {
        node.onChildrenInteractionSourceChange = onChildrenInteractionSourceChange
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onChildrenInteractionSourceChangeModifierNode"
        properties["onChildrenInteractionSourceChange"] = onChildrenInteractionSourceChange
    }
}

private object InteractionSourceModifierNodeTraverseKey
