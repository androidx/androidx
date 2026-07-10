/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.modifier

import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.BackwardsCompatNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.visitSubtreeIf

/**
 * This is a (maybe temporary?) thing to provide proper backwards compatibility with ModifierLocals.
 * This class makes it possible to properly "update" ModifierLocalConsumers whenever
 * ModiferLocalProviders get added/removed in the tree somewhere dynamically. This can be quite
 * costly, so we attempt to do this in a way where we don't waste time on nodes that are being
 * inserted for the first time and thus don't have any consumers below them that need to be
 * invalidated, and also ignore the case where a provider is being detached because a chunk of UI is
 * being removed, meaning that no consumers below them are going to be around to be updated anyway.
 *
 * I think we need to have a bigger discussion around what modifier locals should look like in the
 * Modifer.Node world.
 */
internal class ModifierLocalManager(val owner: Owner) {
    private var _inserted: MutableObjectList<BackwardsCompatNode>? = null
    private val inserted: MutableObjectList<BackwardsCompatNode>
        get() = _inserted ?: mutableObjectListOf<BackwardsCompatNode>().also { _inserted = it }

    private var _insertedLocal: MutableObjectList<ModifierLocal<*>>? = null
    private val insertedLocal: MutableObjectList<ModifierLocal<*>>
        get() =
            _insertedLocal ?: mutableObjectListOf<ModifierLocal<*>>().also { _insertedLocal = it }

    private var _removed: MutableObjectList<LayoutNode>? = null
    private val removed: MutableObjectList<LayoutNode>
        get() = _removed ?: mutableObjectListOf<LayoutNode>().also { _removed = it }

    private var _removedLocal: MutableObjectList<ModifierLocal<*>>? = null
    private val removedLocal: MutableObjectList<ModifierLocal<*>>
        get() = _removedLocal ?: mutableObjectListOf<ModifierLocal<*>>().also { _removedLocal = it }

    private var invalidated: Boolean = false

    fun invalidate() {
        if (!invalidated) {
            invalidated = true
            owner.registerOnEndApplyChangesListener { this.triggerUpdates() }
        }
    }

    fun triggerUpdates() {
        invalidated = false
        // We want to make sure that we only call update on a node once, but if a provider gets
        // removed and a new one gets inserted in its place, we will encounter it when we iterate
        // both the rmoved node and the inserted one, so we store all of the consumers we want to
        // update in a set and call update on them at the end.
        val toUpdate = hashSetOf<BackwardsCompatNode>()
        _removed?.let { removed ->
            removed.forEachIndexed { i, layout ->
                val key = removedLocal[i]
                if (layout.nodes.head.isAttached) {
                    // if the layout is still attached, that means that this provider got removed
                    // and
                    // there's possible some consumers below it that need to be updated
                    invalidateConsumersOfNodeForKey(layout.nodes.head, key, toUpdate)
                }
            }
            removed.clear()
            _removedLocal?.clear()
        }
        // TODO(lmr): we could potentially opt for a more sophisticated strategy here where we
        //  start from the higher up nodes, and invalidate in a way where during traversal if we
        //  happen upon other inserted nodes we can remove them from the inserted set
        _inserted?.let { inserted ->
            inserted.forEachIndexed { i, node ->
                val key = insertedLocal[i]
                if (node.isAttached) {
                    invalidateConsumersOfNodeForKey(node, key, toUpdate)
                }
            }
            inserted.clear()
            _insertedLocal?.clear()
        }
        toUpdate.forEach { it.updateModifierLocalConsumer() }
    }

    private fun invalidateConsumersOfNodeForKey(
        node: Modifier.Node,
        key: ModifierLocal<*>,
        set: MutableSet<BackwardsCompatNode>,
    ) {
        node.visitSubtreeIf(Nodes.Locals) {
            if (it is BackwardsCompatNode && it.element is ModifierLocalConsumer) {
                if (it.readValues.contains(key)) {
                    set.add(it)
                }
            }
            // only continue if this node didn't also provide it
            !it.providedValues.contains(key)
        }
    }

    fun updatedProvider(node: BackwardsCompatNode, key: ModifierLocal<*>) {
        inserted += node
        insertedLocal += key
        invalidate()
    }

    fun insertedProvider(node: BackwardsCompatNode, key: ModifierLocal<*>) {
        inserted += node
        insertedLocal += key
        invalidate()
    }

    fun removedProvider(node: BackwardsCompatNode, key: ModifierLocal<*>) {
        removed += node.requireLayoutNode()
        removedLocal += key
        invalidate()
    }
}
