/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext.ACCESSIBLE_CARET_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_TEXT_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_VALUE_PROPERTY
import javax.accessibility.AccessibleState
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * This class provides a mapping from compose tree of [owner] to tree of [ComposeAccessible],
 * so that each [SemanticsNode] has [ComposeAccessible].
 *
 * @param onFocusReceived a callback that will be called with [ComposeAccessible]
 * when a [SemanticsNode] from [owner] received a focus
 *
 * @see ComposeSceneAccessible
 * @see ComposeAccessible
 */
internal class AccessibilityController(
    val owner: SemanticsOwner,
    val desktopComponent: PlatformComponent,
    coroutineContext: CoroutineContext,
    private val onFocusReceived: (ComposeAccessible) -> Unit
) {
    private var nodeMappingIsValid = false
    private var accessibleByNodeId: Map<Int, ComposeAccessible> = emptyMap()

    fun accessibleByNodeId(nodeId: Int): ComposeAccessible? {
        if (!nodeMappingIsValid) {
            syncNodes()
        }

        return accessibleByNodeId[nodeId]
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onNodeAdded(accessible: ComposeAccessible) {}

    private fun onNodeRemoved(accessible: ComposeAccessible) {
        accessible.removed = true
    }

    private fun onNodeChanged(
        component: ComposeAccessible,
        previousSemanticsNode: SemanticsNode,
        newSemanticsNode: SemanticsNode
    ) {
        for (entry in newSemanticsNode.config) {
            val prev = previousSemanticsNode.config.getOrNull(entry.key)
            if (entry.value != prev) {
                when (entry.key) {
                    SemanticsProperties.Text -> {
                        component.composeAccessibleContext.firePropertyChange(
                            ACCESSIBLE_TEXT_PROPERTY,
                            prev, entry.value
                        )
                    }

                    SemanticsProperties.EditableText -> {
                        component.composeAccessibleContext.firePropertyChange(
                            ACCESSIBLE_TEXT_PROPERTY,
                            prev, entry.value
                        )
                    }

                    SemanticsProperties.TextSelectionRange -> {
                        component.composeAccessibleContext.firePropertyChange(
                            ACCESSIBLE_CARET_PROPERTY,
                            prev, (entry.value as TextRange).start
                        )
                    }

                    SemanticsProperties.Focused ->
                        if (entry.value as Boolean) {
                            component.composeAccessibleContext.firePropertyChange(
                                ACCESSIBLE_STATE_PROPERTY,
                                null, AccessibleState.FOCUSED
                            )
                            onFocusReceived(component)
                        } else {
                            component.composeAccessibleContext.firePropertyChange(
                                ACCESSIBLE_STATE_PROPERTY,
                                AccessibleState.FOCUSED, null
                            )
                        }

                    SemanticsProperties.ToggleableState -> {
                        when (entry.value as ToggleableState) {
                            ToggleableState.On ->
                                component.composeAccessibleContext.firePropertyChange(
                                    ACCESSIBLE_STATE_PROPERTY,
                                    null, AccessibleState.CHECKED
                                )

                            ToggleableState.Off, ToggleableState.Indeterminate ->
                                component.composeAccessibleContext.firePropertyChange(
                                    ACCESSIBLE_STATE_PROPERTY,
                                    AccessibleState.CHECKED, null
                                )
                        }
                    }

                    SemanticsProperties.ProgressBarRangeInfo -> {
                        val value = entry.value as ProgressBarRangeInfo
                        component.composeAccessibleContext.firePropertyChange(
                            ACCESSIBLE_VALUE_PROPERTY,
                            prev,
                            value.current
                        )
                    }
                }
            }
        }
    }

    private object SyncLoopState {
        val MaxIdleTimeMillis = 5.minutes.inWholeMilliseconds // Stop syncing after 5 minutes of inactivity
        var lastUseTimeMillis: Long = 0

        val accessibilityRecentlyInUse
            get() = System.currentTimeMillis() - lastUseTimeMillis < MaxIdleTimeMillis
    }

    /**
     * When called wakes up the sync loop, which may be stopped after
     * some period of inactivity
     */
    fun notifyIsInUse() {
        SyncLoopState.lastUseTimeMillis = System.currentTimeMillis()
        syncNodesChannel.trySend(Unit)
    }

    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)
    private val syncNodesChannel =
        Channel<Unit>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
    private val bfsDeque = ArrayDeque<SemanticsNode>()

    fun dispose() {
        job.cancel()
    }

    fun syncLoop() {
        coroutineScope.launch {
            while (true) {
                syncNodesChannel.receive()
                if (SyncLoopState.accessibilityRecentlyInUse && !nodeMappingIsValid) {
                    syncNodes()
                }
            }
        }
    }

    private fun syncNodes() {
        if (!rootSemanticNode.layoutNode.isPlaced)
            return

        // Build new mapping of ComposeAccessible by node id
        val previous = accessibleByNodeId
        val nodes = mutableMapOf<Int, ComposeAccessible>()
        bfsDeque.add(rootSemanticNode)
        while (bfsDeque.isNotEmpty()) {
            val node = bfsDeque.removeFirst()

            nodes[node.id] = previous[node.id]?.let {
                val prevSemanticsNode = it.semanticsNode
                it.semanticsNode = node
                onNodeChanged(it, prevSemanticsNode, node)
                it
            } ?: ComposeAccessible(node, this).also {
                onNodeAdded(it)
            }

            for (child in node.replacedChildren.asReversed()) {
                if (child.layoutNode.let { it.isAttached && it.isPlaced }) {
                    bfsDeque.add(child)
                }
            }
        }

        // Call onNodeRemoved with nodes that no longer exist
        for ((id, prevNode) in previous.entries) {
            if (id !in nodes) {
                onNodeRemoved(prevNode)
            }
        }
        accessibleByNodeId = nodes
        nodeMappingIsValid = true
    }

    fun onSemanticsChange() {
        nodeMappingIsValid = false
        syncNodesChannel.trySend(Unit)
    }

    private val rootSemanticNode: SemanticsNode
        get() = owner.rootSemanticsNode

    val rootAccessible: ComposeAccessible
        get() = accessibleByNodeId(rootSemanticNode.id)!!
}

internal fun Accessible.print(level: Int = 0) {
    val id = if (this is ComposeAccessible) {
        this.semanticsNode.id.toString()
    } else {
        "unknown"
    }
    with(accessibleContext) {
        println(
            buildString {
                append("\t".repeat(level))
                append("ID: ").append(id)
                append(" Name: ").append(accessibleName)
                append(" Description: ").append(accessibleDescription)
                append(" Role: ").append(accessibleRole)
                append(" Bounds: ").append((this@with as? AccessibleComponent)?.bounds)
            }
        )

        for (childIndex in 0  until accessibleChildrenCount) {
            getAccessibleChild(childIndex).print(level + 1)
        }
    }
}
