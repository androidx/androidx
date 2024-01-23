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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.content.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.ReceiveContentNode
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.receiveContent
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalOf

internal abstract class ReceiveContentConfiguration {
    abstract val hintMediaTypes: Set<MediaType>
    abstract val receiveContentListener: ReceiveContentListener

    fun onCommitContent(transferableContent: TransferableContent): Boolean {
        val remaining = receiveContentListener.onReceive(transferableContent)
        return remaining != transferableContent
    }

    companion object {
        operator fun invoke(
            hintMediaTypes: Set<MediaType>,
            receiveContentListener: ReceiveContentListener
        ): ReceiveContentConfiguration = ReceiveContentConfigurationImpl(
            hintMediaTypes, receiveContentListener
        )
    }
}

private data class ReceiveContentConfigurationImpl(
    override val hintMediaTypes: Set<MediaType>,
    override val receiveContentListener: ReceiveContentListener
) : ReceiveContentConfiguration()

internal val ModifierLocalReceiveContent = modifierLocalOf<ReceiveContentConfiguration?> { null }

/**
 * In a [ModifierLocalModifierNode], reads the current [ReceiveContentConfiguration] that's supplied
 * by [ModifierLocalReceiveContent] if the node is currently attached.
 */
internal fun ModifierLocalModifierNode.getReceiveContentConfiguration() = if (node.isAttached) {
    ModifierLocalReceiveContent.current
} else {
    null
}

/**
 * Combines the current [ReceiveContentNode]'s [ReceiveContentConfiguration] with the parent
 * [ReceiveContentNode]s'. It also counts the drag and drop enter/exit calls to merge drag and drop
 * areas of parent/children [ReceiveContentListener]s. Unlike regular drop targets, ReceiveContent
 * does not call onExit when the dragging item moves from parent node to child node since they
 * share the same boundaries.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class DynamicReceiveContentConfiguration(
    val receiveContentNode: ReceiveContentNode
) : ReceiveContentConfiguration() {

    /**
     * The set of media types that were read from the ancestor nodes when [cachedHintMediaTypes]
     * was last calculated.
     */
    private var lastParentHintMediaTypes: Set<MediaType>? = null

    /**
     * The set of media types that were configured for this node when [cachedHintMediaTypes]
     * was last calculated.
     */
    private var lastHintMediaTypes: Set<MediaType>? = null

    /**
     * The merged set of [lastParentHintMediaTypes] and [lastHintMediaTypes]. [hintMediaTypes]
     * should always return this value.
     */
    private var cachedHintMediaTypes: Set<MediaType> = receiveContentNode.hintMediaTypes

    override val hintMediaTypes: Set<MediaType>
        get() {
            val fromParent = with(receiveContentNode) {
                getReceiveContentConfiguration()?.hintMediaTypes
            }
            val fromNode = receiveContentNode.hintMediaTypes
            var calculatedHintMediaTypes = when {
                // do not allocate again. return the last merged set.
                fromParent == lastParentHintMediaTypes && fromNode == lastHintMediaTypes ->
                    cachedHintMediaTypes
                // nothing coming from top, we can just return this node's configuration.
                fromParent == null -> fromNode
                // there's a change from the last calculation, recalculate
                else -> fromNode + fromParent
            }

            if (calculatedHintMediaTypes.isEmpty()) {
                calculatedHintMediaTypes = setOf(MediaType.All)
            }

            // after calculating the result, cache the inputs and the output before returning.
            lastParentHintMediaTypes = fromParent
            lastHintMediaTypes = fromNode
            cachedHintMediaTypes = calculatedHintMediaTypes

            return calculatedHintMediaTypes
        }

    /**
     * A getter that returns the closest [receiveContent] modifier configuration if this node is
     * attached. It returns null if the node is detached or there is no parent [receiveContent]
     * found.
     */
    private fun getParentReceiveContentListener(): ReceiveContentListener? {
        return receiveContentNode.getReceiveContentConfiguration()?.receiveContentListener
    }

    override val receiveContentListener: ReceiveContentListener = object : ReceiveContentListener {
        /**
         * ---------
         * | A     |
         * |   |---|
         * |   | B |
         * ---------
         *
         * DragAndDrop's own callbacks do not work well with nested content. Simply, when B is
         * nested in A, and the dragging item moves from (A\B) to (A∩B), A receives an exit event
         * and B receives an enter event. From ReceiveContent's chaining perspective, anything
         * that gets dropped on B is also dropped on A. Hence, A should not receive an exit event
         * when the item moves over B.
         *
         * This variable counts the difference between number of times enter and exit are called,
         * but not just on this node. ReceiveContent chaining makes sure that every enter event
         * that B receives is also delegated A. For example;
         *
         * - Dragging item moves onto A.
         *   - A receives an enter event from DragAndDrop system. Enter=1, Exit=0
         * - Dragging item moves onto B.
         *   - A receives an exit event from DragAndDrop system. Enter=1, Exit=1.
         *   - B receives an enter event from DragAndDrop system.
         *     - B delegates this to A.
         *     - A receives an enter event from B. Enter=2, Exit=1
         *
         * In conclusion, nodeEnterCount would be 1, meaning that this node is still hovered.
         */
        private var nodeEnterCount: Int = 0

        override fun onDragStart() {
            // no need to call parent on this because all nodes are going to receive
            // onStart at the same time from DragAndDrop system.
            nodeEnterCount = 0
            receiveContentNode.receiveContentListener.onDragStart()
        }

        override fun onDragEnd() {
            // no need to call parent on this because all nodes are going to receive
            // onEnd at the same time from DragAndDrop system.
            receiveContentNode.receiveContentListener.onDragEnd()
            nodeEnterCount = 0
        }

        override fun onDragEnter() {
            nodeEnterCount++
            if (nodeEnterCount == 1) {
                // enter became 1 from 0. Trigger the callback.
                receiveContentNode.receiveContentListener.onDragEnter()
            }
            // We need to call enter on parent because they will receive onExit from their
            // own DragAndDropTarget.
            getParentReceiveContentListener()?.onDragEnter()
        }

        override fun onDragExit() {
            val previous = nodeEnterCount
            nodeEnterCount = (nodeEnterCount - 1).coerceAtLeast(0)
            if (nodeEnterCount == 0 && previous > 0) {
                receiveContentNode.receiveContentListener.onDragExit()
            }
            // We need to call exit on parent because they also received an enter from us.
            getParentReceiveContentListener()?.onDragExit()
        }

        override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
            // first let this node do whatever it wants. If it consumes everything, we can end
            // the chain here.
            val remaining = receiveContentNode
                .receiveContentListener
                .onReceive(transferableContent) ?: return null

            // Check whether we have a parent node. If not, we can return the remaining here.
            val parentReceiveContentListener = getParentReceiveContentListener()
                ?: return remaining

            // Delegate the rest to the parent node to continue the chain.
            return parentReceiveContentListener.onReceive(remaining)
        }
    }
}
