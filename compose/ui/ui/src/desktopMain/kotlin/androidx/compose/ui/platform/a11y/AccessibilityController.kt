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

package androidx.compose.ui.platform.a11y

import androidx.collection.mutableScatterMapOf
import androidx.compose.ui.platform.PlatformComponent
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
    private val onFocusReceived: (ComposeAccessible) -> Unit
) {

    /**
     * Maps the [ComposeAccessible]s we have created by the [SemanticsNode.id] for which they were
     * created.
     */
    private var accessibleByNodeId = mutableScatterMapOf<Int, ComposeAccessible>()

    /**
     * Whether [accessibleByNodeId] is up-to-date.
     */
    private var nodeMappingIsValid = false

    /**
     * Returns the [ComposeAccessible] associated with the given semantics node id.
     */
    fun accessibleByNodeId(nodeId: Int): ComposeAccessible? {
        if (!nodeMappingIsValid) {
            syncNodes()
        }

        return accessibleByNodeId[nodeId]
    }

    /**
     * Invoked when a new [ComposeAccessible] is created.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onNodeAdded(accessible: ComposeAccessible) {}

    /**
     * Invoked when a [ComposeAccessible] is removed.
     */
    private fun onNodeRemoved(accessible: ComposeAccessible) {
        accessible.removed = true
    }

    /**
     * Invoked when the [SemanticsNode] a [ComposeAccessible] represents changes.
     */
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

    /**
     * Called to notify us when an accessibility call is received from the system.
     *
     * This starts a process that actively synchronized the [ComposeAccessible]s with the semantics
     * node tree.
     */
    fun notifyIsInUse() {
        lastUseTimeNanos = System.nanoTime()
        scheduleNodeSync()
    }

    /**
     * A channel that triggers the syncing of [ComposeAccessible]s with the semantics node tree.
     */
    private val nodeSyncChannel = Channel<Unit>(Channel.RENDEZVOUS)

    /**
     * An [ArrayDeque] used in the BFS algorithm that syncs [ComposeAccessible]s with the semantics
     * tree node.
     *
     * This is kept just to avoid allocating a new one each time.
     */
    private val bfsDeque = ArrayDeque<SemanticsNode>()

    /**
     * An auxiliary mapping of semantics node ids to [ComposeAccessible]s that is swapped with
     * [accessibleByNodeId] on each sync, to avoid allocating memory on each sync.
     */
    private var auxAccessibleByNodeId = mutableScatterMapOf<Int, ComposeAccessible>()

    /**
     * A list of callbacks ([onNodeAdded], [onNodeRemoved], [onNodeChanged]) to be made after
     * syncing the semantics node tree is completed.
     *
     * This is kept just to avoid allocating a new one each time.
     */
    private val delayedNodeNotifications = mutableListOf<() -> Unit>()

    /**
     * The time of the latest accessibility call from the system.
     */
    // Set initial value such that accessibilityRecentlyUsed is initially `false`
    private var lastUseTimeNanos: Long = System.nanoTime() - (MaxIdleTimeNanos + 1)

    /**
     * Whether an accessibility call from the system has been received "recently".
     *
     * When this returns `false` the active syncing of [ComposeAccessible]s with the semantics node
     * tree is paused.
     */
    private val accessibilityRecentlyUsed
        get() = System.nanoTime() - lastUseTimeNanos < MaxIdleTimeNanos

    /**
     * The coroutine syncing the [ComposeAccessible]s with the semantics node tree.
     */
    private var syncingJob: Job? = null

    /**
     * Disposes of this [AccessibilityController], releasing any resources associated with it.
     */
    fun dispose() {
        syncingJob?.cancel()
    }

    /**
     * Launches a coroutine to continuously sync [ComposeAccessible]s with the semantics node tree.
     */
    fun launchSyncLoop(context: CoroutineContext) {
        if (syncingJob != null)
            throw IllegalStateException("Sync loop already running")

        syncingJob = CoroutineScope(context).launch {
            while (true) {
                nodeSyncChannel.receive()
                if (accessibilityRecentlyUsed && !nodeMappingIsValid) {
                    syncNodes()
                }
            }
        }
    }

    /**
     * Syncs [accessibleByNodeId] with the semantics node tree.
     */
    private fun syncNodes() {
        fun SemanticsNode.isValid() = layoutNode.let { it.isPlaced && it.isAttached }

        // Build new mapping of ComposeAccessible by node id
        val previous = accessibleByNodeId
        val updated = auxAccessibleByNodeId
        if (rootSemanticNode.isValid())
            bfsDeque.add(rootSemanticNode)
        while (bfsDeque.isNotEmpty()) {
            val node = bfsDeque.removeFirst()

            val existingAccessible = previous[node.id]
            updated[node.id] = if (existingAccessible != null) {
                val prevSemanticsNode = existingAccessible.semanticsNode
                existingAccessible.semanticsNode = node
                delayedNodeNotifications.add {
                    onNodeChanged(existingAccessible, prevSemanticsNode, node)
                }
                existingAccessible
            }
            else {
                val newAccessible = ComposeAccessible(node, this)
                delayedNodeNotifications.add {
                    onNodeAdded(newAccessible)
                }
                newAccessible
            }

            for (child in node.replacedChildren.asReversed()) {
                if (child.isValid()) {
                    bfsDeque.add(child)
                }
            }
        }

        // Call onNodeRemoved with nodes that no longer exist
        previous.forEach { id, node ->
            if (id !in updated) {
                delayedNodeNotifications.add {
                    onNodeRemoved(node)
                }
            }
        }
        auxAccessibleByNodeId = previous.also { it.clear() }
        accessibleByNodeId = updated
        nodeMappingIsValid = true

        // Call the onNodeX functions
        for (notification in delayedNodeNotifications) {
            notification()
        }
        delayedNodeNotifications.clear()
    }

    /**
     * Schedules [syncNodes] to be called later.
     */
    private fun scheduleNodeSync() {
        nodeSyncChannel.trySend(Unit)
    }

    /**
     * Invoked when the semantics node tree changes.
     */
    fun onSemanticsChange() {
        nodeMappingIsValid = false
        scheduleNodeSync()
    }

    /**
     * Invoked when the position and/or size of the [SemanticsNode] with the given semantics id
     * changed.
     */
    fun onLayoutChanged(@Suppress("UNUSED_PARAMETER") nodeId: Int) {
        // TODO: Only recompute the layout-related properties of the node
        nodeMappingIsValid = false
        scheduleNodeSync()
    }

    /**
     * The [SemanticsNode] that is the root of the semantics node tree.
     */
    private val rootSemanticNode: SemanticsNode
        get() = owner.rootSemanticsNode

    /**
     * The [ComposeAccessible] associated with the root of the semantics node tree.
     */
    val rootAccessible: ComposeAccessible
        get() = accessibleByNodeId(rootSemanticNode.id)!!
}

/**
 * Prints debugging info of the given [Accessible].
 */
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

/**
 * The time before we stop actively syncing [ComposeAccessible]s with the semantics node tree if we
 * don't receive any accessibility calls from the system.
 */
private val MaxIdleTimeNanos = 5.minutes.inWholeNanoseconds
