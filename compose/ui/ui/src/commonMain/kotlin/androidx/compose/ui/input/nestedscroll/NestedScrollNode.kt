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
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

internal val ModifierLocalNestedScroll = modifierLocalOf<NestedScrollNode?> { null }

/**
 * This creates a Nested Scroll Modifier node that can be delegated to. In most case you should
 * use [Modifier.nestedScroll] since that implementation also uses this. Use this factory to create
 * nodes that can be delegated to.
 */
fun nestedScrollModifierNode(
    connection: NestedScrollConnection,
    dispatcher: NestedScrollDispatcher?
): DelegatableNode {
    return NestedScrollNode(connection, dispatcher)
}

/**
 * NestedScroll using ModifierLocal as implementation.
 */
internal class NestedScrollNode(
    var connection: NestedScrollConnection,
    dispatcher: NestedScrollDispatcher?
) : ModifierLocalNode, NestedScrollConnection, DelegatableNode, Modifier.Node() {

    // Resolved dispatcher for re-use in case of null dispatcher is passed.
    private var resolvedDispatcher: NestedScrollDispatcher

    init {
        resolvedDispatcher = dispatcher ?: NestedScrollDispatcher() // Resolve null dispatcher
    }

    private val parentModifierLocal: NestedScrollNode?
        get() = if (isAttached) ModifierLocalNestedScroll.current else null

    private val parentConnection: NestedScrollConnection?
        get() = if (isAttached) ModifierLocalNestedScroll.current else null

    override val providedValues: ModifierLocalMap
        get() = modifierLocalMapOf(ModifierLocalNestedScroll to this)

    private val nestedCoroutineScope: CoroutineScope
        get() = parentModifierLocal?.nestedCoroutineScope
            ?: resolvedDispatcher.scope
            ?: throw IllegalStateException(
                "in order to access nested coroutine scope you need to attach dispatcher to the " +
                    "`Modifier.nestedScroll` first."
            )

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
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
        val parentConsumed = parentConnection?.onPostScroll(
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
        val parentConsumed = parentConnection?.onPostFling(
            consumed + selfConsumed,
            available - selfConsumed
        ) ?: Velocity.Zero
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
        assert(resolvedDispatcher.modifierLocalNode == null) {
            "This dispatcher should only be used by a single Modifier.nestedScroll."
        }
        updateDispatcherFields()
    }

    override fun onDetach() {
        resetDispatcherFields()
    }

    /**
     * If the node changes (onAttach) or if the dispatcher changes (node.update). We'll need
     * to reset the dispatcher properties accordingly.
     */
    private fun updateDispatcherFields() {
        resolvedDispatcher.modifierLocalNode = this
        resolvedDispatcher.calculateNestedScrollScope = { nestedCoroutineScope }
        resolvedDispatcher.scope = coroutineScope
    }

    private fun resetDispatcherFields() {
        resolvedDispatcher.modifierLocalNode = null
    }

    internal fun updateNode(
        connection: NestedScrollConnection,
        dispatcher: NestedScrollDispatcher?
    ) {
        this.connection = connection
        updateDispatcher(dispatcher)
    }
}
