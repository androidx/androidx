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

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext.ACCESSIBLE_CARET_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_TEXT_PROPERTY
import javax.accessibility.AccessibleContext.ACCESSIBLE_VALUE_PROPERTY
import javax.accessibility.AccessibleState
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Manages the accessibility aspects of a [SemanticsOwner].
 *
 * Creates a [ComposeAccessible] for each [SemanticsNode] in the semantics tree, and keeps them
 * up to date with the semantics tree.
 *
 * @param onFocusReceived a callback that will be called with [ComposeAccessible]
 * when a [SemanticsNode] from [owner] received a focus. `null` is passed when no nodes in the scene
 * are focused.
 *
 * @see ComposeSceneAccessibility
 * @see ComposeAccessible
 */
internal class SemanticsOwnerAccessibility(
    private val owner: SemanticsOwner,
    val desktopComponent: PlatformComponent,
    private val sceneAccessibility: ComposeSceneAccessibility,
    private val onFocusReceived: (ComposeAccessible?) -> Unit,
) {

    /**
     * Maps the [ComposeAccessible]s we have created by the [SemanticsNode.id] for which they were
     * created.
     */
    private var accessibleByNodeId = mutableIntObjectMapOf<ComposeAccessible>()

    /**
     * Whether [accessibleByNodeId] is up to date.
     */
    private var nodeMappingIsValid = false

    /**
     * Returns the [ComposeAccessible] associated with the given semantics node id.
     */
    fun accessibleByNodeId(nodeId: Int): ComposeAccessible? {
        syncNodesIfInvalid()
        return accessibleByNodeId[nodeId]
    }

    /**
     * Syncs the accessible nodes if the current mapping is invalid.
     */
    private fun syncNodesIfInvalid() {
        if (!nodeMappingIsValid) {
            syncNodes()
        }
    }

    /**
     * Returns the index of this [SemanticsOwnerAccessibility]'s root node in the scene.
     */
    fun indexInScene(): Int {
        return sceneAccessibility.indexOfChild(this)
    }

    /**
     * Returns the [Accessible] parent of the given [ComposeAccessible].
     */
    fun accessibleParentOf(accessible: ComposeAccessible): Accessible? {
        // This can happen during onNodeRemoved. When the property change listeners are called, they
        // can call `accessible.getAccessibleParent()`.
        if (accessible.semanticsNode.id !in accessibleByNodeId) return null

        sceneAccessibility.accessibleParentOverride(accessible)?.let { return it }

        val parentNode = accessible.semanticsNode.parent ?: return sceneAccessibility.accessible()
        return accessibleByNodeId(parentNode.id)!!
    }

    /**
     * Invoked when a new [ComposeAccessible] is created.
     */
    private fun onNodeAdded(accessible: ComposeAccessible) {
        for (entry in accessible.semanticsConfig) {
            when (entry.key) {
                SemanticsProperties.Focused -> {
                    if (entry.value as Boolean) {
                        invokeLaterOnAccessible(accessible.semanticsNode.id) { accessible, config ->
                            // Check that it's still focused
                            if (config.getOrNull(SemanticsProperties.Focused) == true) {
                                notifyOnFocusReceived(accessible)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Invoke [action] on the next EDT iteration, if the node still exists then.
     *
     * This is needed when firing property change events on newly created nodes because [syncNodes],
     * and consequently [onNodeAdded], can be called as the result of the accessibility system
     * trying to obtain the [Accessible]. If we fire the event directly in [onNodeAdded], it will
     * fire before the system had a chance to register the property listener to the event.
     */
    private fun invokeLaterOnAccessible(
        nodeId: Int,
        action: (ComposeAccessible, SemanticsConfiguration) -> Unit
    ) = SwingUtilities.invokeLater {
        if (disposed) return@invokeLater
        val accessible = accessibleByNodeId(nodeId) ?: return@invokeLater
        action(accessible, accessible.semanticsConfig)
    }

    /**
     * Invoked when a [ComposeAccessible] is removed.
     */
    private fun onNodeRemoved(accessible: ComposeAccessible) {
        val accessibleContext = accessible.composeAccessibleContext
        if (accessibleContext.focused == true) {
            notifyOnFocusLost(accessible)

            // Testing showed that when focus is transferred manually when a node is removed (e.g.,
            // via FocusRequester), the removed node doesn't report it's focused at this point, but
            // just in case, check that no other nodes are focused before calling
            // onFocusReceived(null)
            val anyNodeFocused = accessibleByNodeId.any { _, a ->
                a.composeAccessibleContext.focused == true
            }
            if (!anyNodeFocused) {
                onFocusReceived(null)
            }
        }
        if (accessibleContext.isVisible) {
            accessibleContext.firePropertyChange(
                ACCESSIBLE_STATE_PROPERTY,
                AccessibleState.VISIBLE, null
            )
        }

        // dispose() can only be called after the code above because after dispose(), the
        // accessible.accessibleContext returns `null`, but the accessibility system can call it
        // in the property change listener(s)
        accessible.dispose()
    }

    /**
     * Invoked when the [SemanticsNode] a [ComposeAccessible] represents changes.
     */
    private fun onNodeChanged(
        accessible: ComposeAccessible,
        prevConfig: SemanticsConfiguration,
    ) {
        val accessibleContext by lazy { accessible.composeAccessibleContext }
        for (entry in accessible.semanticsConfig) {
            val prevValue = prevConfig.getOrNull(entry.key)
            val newValue = entry.value
            if (newValue == prevValue) continue

            when (entry.key) {
                SemanticsProperties.Text -> {
                    accessibleContext.firePropertyChange(
                        ACCESSIBLE_TEXT_PROPERTY,
                        prevValue, newValue
                    )
                }

                SemanticsProperties.EditableText -> {
                    // The docs on ACCESSIBLE_TEXT_PROPERTY say that the value should be
                    // an AccessibleTextSequence, but in reality, AccessibleJTextComponent
                    // sends the position of the start of the change
                    accessibleContext.firePropertyChange(
                        ACCESSIBLE_TEXT_PROPERTY,
                        null,
                        0  // Ideally, we should track the position of the change; 0 means everything changed
                    )
                }

                SemanticsProperties.TextSelectionRange -> {
                    val prevTextSelectionRange = prevValue as? TextRange
                    val newTextSelectionRange = newValue as TextRange

                    val prevCaretPosition = prevTextSelectionRange?.end
                    val newCaretPosition = newTextSelectionRange.end
                    if (prevCaretPosition != newCaretPosition) {
                        accessibleContext.firePropertyChange(
                            ACCESSIBLE_CARET_PROPERTY,
                            prevCaretPosition, newCaretPosition
                        )
                    }

                    val text = accessible.semanticsConfig.getOrNull(SemanticsProperties.EditableText)
                    val prevHasSelection = prevTextSelectionRange?.collapsed == false
                    val nowHasSelection = !newTextSelectionRange.collapsed
                    if (prevHasSelection != nowHasSelection) {
                        accessibleContext.firePropertyChange(
                            ACCESSIBLE_SELECTION_PROPERTY,
                            null,  // AccessibleJTextComponent also sends oldValue = null
                            text?.subSequence(newTextSelectionRange)
                        )
                    }
                }

                SemanticsProperties.Focused ->
                    if (newValue as Boolean) {
                        notifyOnFocusReceived(accessible)
                    } else {
                        notifyOnFocusLost(accessible)
                    }

                SemanticsProperties.ToggleableState -> {
                    when (newValue as ToggleableState) {
                        ToggleableState.On ->
                            accessibleContext.firePropertyChange(
                                ACCESSIBLE_STATE_PROPERTY,
                                null, AccessibleState.CHECKED
                            )

                        ToggleableState.Off, ToggleableState.Indeterminate ->
                            accessibleContext.firePropertyChange(
                                ACCESSIBLE_STATE_PROPERTY,
                                AccessibleState.CHECKED, null
                            )
                    }
                }

                SemanticsProperties.ProgressBarRangeInfo -> {
                    val value = newValue as ProgressBarRangeInfo
                    accessibleContext.firePropertyChange(
                        ACCESSIBLE_VALUE_PROPERTY,
                        (prevValue as? ProgressBarRangeInfo)?.current,
                        value.current
                    )
                }
            }
        }
    }

    /**
     * Notifies the system when the given accessible becomes focused.
     */
    private fun notifyOnFocusReceived(accessible: ComposeAccessible) {
        accessible.accessibleContext?.firePropertyChange(
            ACCESSIBLE_STATE_PROPERTY,
            null, AccessibleState.FOCUSED
        )
        onFocusReceived(accessible)
    }

    /**
     * Notifies the system when the given accessible loses focused.
     */
    private fun notifyOnFocusLost(accessible: ComposeAccessible) {
        accessible.accessibleContext?.firePropertyChange(
            ACCESSIBLE_STATE_PROPERTY,
            AccessibleState.FOCUSED, null
        )
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
    private var auxAccessibleByNodeId = mutableIntObjectMapOf<ComposeAccessible>()

    /**
     * A list of callbacks ([onNodeAdded], [onNodeRemoved], [onNodeChanged]) to be made after
     * syncing the semantics node tree is completed.
     *
     * This is kept just to avoid allocating a new one each time.
     */
    private val delayedNodeNotifications = mutableListOf<() -> Unit>()

    /**
     * The coroutine syncing the [ComposeAccessible]s with the semantics node tree.
     */
    private var syncingJob: Job? = null

    /**
     * Whether this [SemanticsOwnerAccessibility] has been disposed.
     */
    @Volatile
    private var disposed = false

    /**
     * Disposes of this [SemanticsOwnerAccessibility], releasing any resources associated with it.
     */
    fun dispose() {
        syncingJob?.cancel()
        disposed = true
    }

    /**
     * Launches a coroutine to continuously sync [ComposeAccessible]s with the semantics node tree.
     */
    fun launchSyncLoop(context: CoroutineContext) {
        if (syncingJob != null)
            throw IllegalStateException("Sync loop already running")

        syncingJob = CoroutineScope(context).launch {
            AccessibilityUsage.runActiveInstance(this@SemanticsOwnerAccessibility) {
                while (true) {
                    nodeSyncChannel.receive()
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
        // `InvisibleToUser` and `HideFromAccessibility` are unmerged properties, so it's ok to get
        // them from `unmergedConfig`.
        fun SemanticsNode.isInvisibleToA11y() = unmergedConfig.let {
            @Suppress("DEPRECATION")
            it.contains(SemanticsProperties.InvisibleToUser) ||
                it.contains(SemanticsProperties.HideFromAccessibility)
        }

        // Build a new mapping of ComposeAccessible by node id
        val previous = accessibleByNodeId
        val updated = auxAccessibleByNodeId
        if (rootSemanticNode.isValid())
            bfsDeque.add(rootSemanticNode)
        while (bfsDeque.isNotEmpty()) {
            val node = bfsDeque.removeFirst()
            if (node.isInvisibleToA11y()) continue

            val existingAccessible = previous[node.id]
            updated[node.id] = if (existingAccessible != null) {
                val prevSemanticsConfig = existingAccessible.semanticsConfig
                existingAccessible.semanticsNode = node
                delayedNodeNotifications.add {
                    onNodeChanged(existingAccessible, prevSemanticsConfig)
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

            node.replacedChildren.fastForEachReversed { child ->
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
        delayedNodeNotifications.fastForEach { notification ->
            notification()
        }
        delayedNodeNotifications.clear()
    }

    /**
     * Schedules [syncNodes] to be called later.
     */
    private fun scheduleNodeSyncIfNeeded() {
        if (AccessibilityUsage.recentlyUsed && !nodeMappingIsValid) {
            nodeSyncChannel.trySend(Unit)
        }
    }

    /**
     * Invoked when the semantics node tree changes.
     */
    fun onSemanticsChange() {
        nodeMappingIsValid = false
        scheduleNodeSyncIfNeeded()
    }

    /**
     * Invoked when the position and/or size of the [SemanticsNode] with the given semantics id
     * changed.
     */
    fun onLayoutChanged(@Suppress("UNUSED_PARAMETER") nodeId: Int) {
        // TODO: Only recompute the layout-related properties of the node
        nodeMappingIsValid = false
        scheduleNodeSyncIfNeeded()
    }

    /**
     * Returns the [ComposeAccessible] associated with the currently focused node.
     */
    private fun focusedAccessible(): ComposeAccessible? {
        syncNodesIfInvalid()
        accessibleByNodeId.forEachValue { accessible ->
            if (accessible.semanticsConfig.getOrNull(SemanticsProperties.Focused) == true) {
                return accessible
            }
        }

        return null
    }

    /**
     * Invoked when the AWT component of the Compose content gains focus.
     */
    fun onFocusGained() {
        if (!AccessibilityUsage.recentlyUsed) return
        focusedAccessible()?.let { notifyOnFocusReceived(it) }
    }

    /**
     * Invoked when the AWT component of the Compose content loses focus.
     */
    fun onFocusLost() {
        if (!AccessibilityUsage.recentlyUsed) return
        focusedAccessible()?.let { notifyOnFocusLost(it) }
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

    /**
     * Holds how recently the system has queried the program's accessibility state and manages
     * enabling/disabling the syncing of [SemanticsOwnerAccessibility]s with the semantic tree when
     * the system has not queried the program's accessibility state for a while.
     */
    object AccessibilityUsage {

        /**
         * The time before we stop actively syncing [ComposeAccessible]s with the semantics node
         * tree if we don't receive any accessibility calls from the system.
         */
        private val MaxIdleTimeNanos = 5.minutes.inWholeNanoseconds

        /**
         * The set of "live" [SemanticsOwnerAccessibility]s.
         */
        // Using a list instead of a set because set iterator is expensive (memory wise)
        private val activeInstances = mutableListOf<SemanticsOwnerAccessibility>()

        /**
         * The time of the latest accessibility call from the system.
         */
        // Set the initial value such that `recentlyUsed` is initially `false`
        private var lastUseTimeNanos: Long = System.nanoTime() - (MaxIdleTimeNanos + 1)

        /**
         * Resets this object to its initial state. This is needed for tests.
         */
        internal fun reset() {
            assert(activeInstances.isEmpty())
            lastUseTimeNanos = System.nanoTime() - (MaxIdleTimeNanos + 1)
        }

        /**
         * Called to notify us when an accessibility query is received from the system.
         *
         * This starts a process that actively synchronized the [ComposeAccessible]s with the
         * semantics node tree.
         */
        fun notifyInUse() {
            lastUseTimeNanos = System.nanoTime()
            activeInstances.fastForEach { instance ->
                instance.scheduleNodeSyncIfNeeded()
            }
        }

        /**
         * Whether an accessibility call from the system has been received "recently".
         *
         * When this returns `false` the active syncing of [ComposeAccessible]s with the semantics
         * node tree is paused.
         */
        val recentlyUsed
            get() = System.nanoTime() - lastUseTimeNanos < MaxIdleTimeNanos


        /**
         * Registers the given [SemanticsOwnerAccessibility] as an active one until [block] returns.
         */
        suspend fun runActiveInstance(
            ownerAccessibility: SemanticsOwnerAccessibility,
            block: suspend () -> Unit
        ) {
            try {
                activeInstances.add(ownerAccessibility)
                block()
            } finally {
                activeInstances.remove(ownerAccessibility)
            }
        }
    }
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
