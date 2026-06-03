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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import java.awt.Component
import javax.swing.SwingUtilities.isEventDispatchThread
import org.jetbrains.skiko.ClipRectangle

/**
 * A helper class to back-buffer scheduled updates for Swing Interop without allocating
 * an array on each frame.
 */
private class ScheduledUpdatesSwapchain(
    private val requestRedraw: () -> Unit
) {
    private var executed = mutableListOf<() -> Unit>()
    private var scheduled = mutableListOf<() -> Unit>()
    private val lock = Any()

    /**
     * Indicates whether a redraw is requested when update is scheduled.
     */
    private var needsRequestRedrawOnUpdateScheduled = true

    /**
     * Schedule an update to be executed later.
     */
    fun scheduleUpdate(action: () -> Unit) = synchronized(lock) {
        scheduled.add(action)

        if (needsRequestRedrawOnUpdateScheduled) {
            requestRedraw()
        }
    }

    /**
     * Performs a [body], if [scheduleUpdate] is called-back from within it, no redraw requests
     * will be made.
     */
    inline fun preventingRedrawRequests(body: () -> Unit) {
        try {
            synchronized(lock) {
                check(needsRequestRedrawOnUpdateScheduled) {
                    "Reentry into ignoringRedrawRequests is not allowed"
                }

                needsRequestRedrawOnUpdateScheduled = false
            }

            body()
        } finally {
            synchronized(lock) {
                needsRequestRedrawOnUpdateScheduled = true
            }
        }
    }

    /**
     * Execute all scheduled updates.
     *
     * @return True if there were any updates to execute. False otherwise.
     */
    fun execute(): Boolean {
        // Race condition on [executed] is prevented by the fact that this method is called only
        // on the AWT EDT. We only need to synchronize [scheduled] across threads using [lock].

        synchronized(lock) {
            // Swap lists and return the one to be executed
            val t = executed
            executed = scheduled
            scheduled = t
        }

        val hasAnyUpdates = executed.isNotEmpty()

        executed.fastForEach {
            it.invoke()
        }
        executed.clear()

        return hasAnyUpdates
    }
}

/**
 * A container that controls interop views/components.
 *
 * It receives [root] native view to use it as parent for all interop views. It should be
 * the same component that is used in [ComposeSceneMediator] to avoid issues with transparency.
 *
 * @property root The Swing container to add the interop views to.
 * @param placeInteropAbove Whether to place interop components above non-interop components.
 * @param requestRedraw Function to request a redraw. It's needed because executing scheduled
 * updates is tied to the draw loop and update doesn't necessary trigger an invalidation causing
 * a redraw, so we need to request it explicitly.
 */
internal class SwingInteropContainer(
    override val root: InteropViewGroup,
    placeInteropAbove: Boolean,
    requestRedraw: () -> Unit
) : InteropContainer {

    /**
     * Whether to place interop components above non-interop components.
     */
    var placeInteropAbove = placeInteropAbove
        set(value) {
            if (field != value) {
                field = value
                updateInteropComponentsOrder()
            }
        }

    /**
     * Map to reverse-lookup of [InteropViewHolder] having an [InteropViewGroup].
     */
    private val interopComponents by lazy(LazyThreadSafetyMode.NONE) {
        mutableMapOf<InteropViewGroup, InteropViewHolder>()
    }

    override var rootModifier: TrackInteropPlacementModifierNode? = null

    override val snapshotObserver: SnapshotStateObserver by lazy(LazyThreadSafetyMode.NONE) {
        SnapshotStateObserver { command ->
            command()
        }
    }

    private val scheduledUpdatesSwapchain = ScheduledUpdatesSwapchain(requestRedraw)

    override fun contains(holder: InteropViewHolder): Boolean =
        interopComponents.contains(holder.group)

    override fun holderOfView(view: InteropView): InteropViewHolder? {
        val component = view as Component
        val group = checkNotNull(component.parent) {
            "InteropView is assumed to be added to its group for its entire lifetime"
        }
        return interopComponents[group]
    }

    private fun updateInteropComponentsOrder() {
        val orderedInteropComponents =
            interopComponentsSortedByDrawOrder(interopComponents.values)

        scheduleUpdate {
            val allComponentCount = root.components.size
            // AWT/Swing uses the **REVERSE ORDER** for drawing and events, so add in reverse
            var index = 0
            orderedInteropComponents.fastForEachReversed { holder ->
                holder.changeInteropViewIndex(
                    root = root,
                    index = if (placeInteropAbove) {
                        allComponentCount - 1  // Put each one at the end
                    } else {
                        index  // Insert at 0, 1, 2 etc.
                    }
                )
                index++
            }
        }
    }

    override fun place(holder: InteropViewHolder) {
        val group = holder.group
        if (interopComponents.isEmpty()) {
            snapshotObserver.start()
        }

        // Note that compose state must be read here, but AWT/Swing state must be read inside
        // scheduleUpdate

        // Add this component to [interopComponents] to track count and clip rects
        val isNewInteropView = interopComponents.putIfAbsent(group, holder) == null

        // [ComposeSceneMediator] might keep extra components in the same container.
        val interopComponentsCount = interopComponents.size

        // Iterate through a Compose layout tree in draw order and count interop view below this one
        val interopComponentsBelowCount = countInteropComponentsBelow(holder)

        // Update AWT/Swing hierarchy
        scheduleUpdate {
            // Based on [placeInteropAbove] interop views should go below or under all interop views
            var lastInteropIndex = interopComponentsCount - 1
            if (!placeInteropAbove) {
                val existingInteropComponentCount =
                    interopComponentsCount - if (isNewInteropView) 1 else 0
                lastInteropIndex += root.componentCount - existingInteropComponentCount
            }

            // AWT/Swing uses the **REVERSE ORDER** for drawing and events
            val awtIndex = lastInteropIndex - interopComponentsBelowCount
            if (isNewInteropView) {
                holder.insertInteropView(root = root, index = awtIndex)
            } else {
                holder.changeInteropViewIndex(root = root, index = awtIndex)
            }
        }
    }

    override fun unplace(holder: InteropViewHolder) {
        scheduleUpdate {
            holder.removeInteropView(root = root)
        }

        interopComponents.remove(holder.group)

        if (interopComponents.isEmpty()) {
            snapshotObserver.stop()
        }
    }

    private fun executeScheduledUpdates() {
        check(isEventDispatchThread())

        val hasAnyUpdates = scheduledUpdatesSwapchain.execute()

        if (hasAnyUpdates) {
            // Sometimes Swing displays the rest of interop views in incorrect order after an update
            // so we need to re-validate and repaint the root component.

            root.validate()
            root.repaint()
        }
    }

    fun dispose() {
        executeScheduledUpdates()
    }

    /**
     * Performs a [body] and then executes all scheduled updates, including those that can happen
     * inside [body].
     */
    fun postponingExecutingScheduledUpdates(body: () -> Unit) {
        scheduledUpdatesSwapchain.preventingRedrawRequests(body)

        executeScheduledUpdates()
    }

    override fun scheduleUpdate(action: () -> Unit) {
        scheduledUpdatesSwapchain.scheduleUpdate(action)
    }

    // TODO: Should be the same as [Owner.onInteropViewLayoutChange]?
//    override fun onInteropViewLayoutChange(holder: InteropViewHolder) {
//        // No-op.
//        // On Swing it's called after relayout for specific interop view was requested.
//        // It means that the validate and repaint will be executed after it.
//    }

    fun getClipRectForComponent(component: Component): ClipRectangle =
        requireNotNull(interopComponents[component]) as ClipRectangle

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalInteropContainer provides this,
        ) {
            TrackInteropPlacementContainer(
                content = content
            )
        }
    }
}
