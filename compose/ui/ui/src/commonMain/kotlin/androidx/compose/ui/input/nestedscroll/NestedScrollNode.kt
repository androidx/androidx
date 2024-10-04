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

package androidx.compose.ui.input.nestedscroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

/**
 * This creates a Nested Scroll Modifier node that can be delegated to. In most case you should use
 * [Modifier.nestedScroll] since that implementation also uses this. Use this factory to create
 * nodes that can be delegated to.
 */
fun nestedScrollModifierNode(
    connection: NestedScrollConnection,
    dispatcher: NestedScrollDispatcher?
): DelegatableNode {
    return NestedScrollNode(connection, dispatcher)
}

/** NestedScroll using ModifierLocal as implementation. */
internal class NestedScrollNode(
    var connection: NestedScrollConnection,
    dispatcher: NestedScrollDispatcher?
) : TraversableNode, NestedScrollConnection, Modifier.Node() {

    // Resolved dispatcher for re-use in case of null dispatcher is passed.
    private var resolvedDispatcher: NestedScrollDispatcher

    init {
        resolvedDispatcher = dispatcher ?: NestedScrollDispatcher() // Resolve null dispatcher
    }

    internal val parentNestedScrollNode: NestedScrollNode?
        get() = if (isAttached) findNearestAncestor() else null

    private val parentConnection: NestedScrollConnection?
        get() = if (isAttached) parentNestedScrollNode else null

    override val traverseKey: Any = "androidx.compose.ui.input.nestedscroll.NestedScrollNode"

    private val nestedCoroutineScope: CoroutineScope
        get() =
            parentNestedScrollNode?.nestedCoroutineScope
                ?: resolvedDispatcher.scope
                ?: throw IllegalStateException(
                    "in order to access nested coroutine scope you need to attach dispatcher to the " +
                        "`Modifier.nestedScroll` first."
                )

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val parentPreConsumed = parentConnection?.onPreScroll(available, source) ?: Offset.Zero
        val selfPreConsumed = connection.onPreScroll(available - parentPreConsumed, source)
        return parentPreConsumed + selfPreConsumed
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val selfConsumed = connection.onPostScroll(consumed, available, source)
        val parentConsumed =
            parentConnection?.onPostScroll(
                consumed + selfConsumed,
                available - selfConsumed,
                source
            ) ?: Offset.Zero
        return selfConsumed + parentConsumed
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val parentPreConsumed = parentConnection?.onPreFling(available) ?: Velocity.Zero
        val selfPreConsumed = connection.onPreFling(available - parentPreConsumed)
        return parentPreConsumed + selfPreConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {

        val selfConsumed = connection.onPostFling(consumed, available)
        val parentConsumed =
            parentConnection?.onPostFling(consumed + selfConsumed, available - selfConsumed)
                ?: Velocity.Zero
        return selfConsumed + parentConsumed
    }

    // On receiving a new dispatcher, re-setting fields
    private fun updateDispatcher(newDispatcher: NestedScrollDispatcher?) {
        resetDispatcherFields() // Reset fields of current dispatcher.

        // Update dispatcher associated with this node.
        if (newDispatcher == null) {
            resolvedDispatcher = NestedScrollDispatcher()
        } else if (newDispatcher != resolvedDispatcher) {
            resolvedDispatcher = newDispatcher
        }

        // Update fields of the newly set dispatcher.
        if (isAttached) {
            updateDispatcherFields()
        }
    }

    override fun onAttach() {
        // NOTE: It is possible for the dispatcher of a yet-to-be-removed node above this one in the
        // chain is being used here where the dispatcher's modifierLocalNode will not be null. As a
        // result, we should not check to see if the dispatcher's node is null, we should just set
        // it assuming that it is not going to be used by the previous node anymore.
        updateDispatcherFields()
    }

    override fun onDetach() {
        resetDispatcherFields()
    }

    /**
     * If the node changes (onAttach) or if the dispatcher changes (node.update). We'll need to
     * reset the dispatcher properties accordingly.
     */
    private fun updateDispatcherFields() {
        resolvedDispatcher.nestedScrollNode = this
        resolvedDispatcher.calculateNestedScrollScope = { nestedCoroutineScope }
        resolvedDispatcher.scope = coroutineScope
    }

    private fun resetDispatcherFields() {
        // only null this out if the modifier local node is what we set it to, since it is possible
        // it has already been reused in a different node
        if (resolvedDispatcher.nestedScrollNode === this) resolvedDispatcher.nestedScrollNode = null
    }

    internal fun updateNode(
        connection: NestedScrollConnection,
        dispatcher: NestedScrollDispatcher?
    ) {
        this.connection = connection
        updateDispatcher(dispatcher)
    }
}
