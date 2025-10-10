/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent

import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_DEFAULT
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_OVERLAY
import androidx.navigationevent.NavigationEventTransitionState.Direction
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.flow.StateFlow

/**
 * A dispatcher for navigation events that can be organized hierarchically.
 *
 * This class acts as a localized entry point for registering [NavigationEventHandler] instances and
 * dispatching navigation events within a specific UI scope, such as a composable or a fragment.
 *
 * Dispatchers can be linked in a parent-child hierarchy. This structure allows for a sophisticated
 * system where nested UI components can handle navigation events independently while still
 * respecting the state of their parent. The core logic is delegated to a single, shared
 * [NavigationEventProcessor] instance across the entire hierarchy, ensuring consistent event
 * handling.
 *
 * It is important to call [dispose] when the owner of this dispatcher is destroyed (e.g., in a
 * `DisposableEffect`) to unregister handlers and prevent memory leaks.
 */
public class NavigationEventDispatcher
/**
 * The primary, internal constructor for `NavigationEventDispatcher`.
 *
 * All public constructors delegate to this one to perform the actual initialization.
 *
 * @param parent An optional reference to a parent [NavigationEventDispatcher]. Providing a parent
 *   allows this dispatcher to participate in a hierarchical event system, sharing the same
 *   underlying [NavigationEventProcessor] as its parent. If `null`, this dispatcher acts as the
 *   root of its own event handling hierarchy.
 * @param onBackCompletedFallback An optional lambda to be invoked if a back event completes and no
 *   registered [NavigationEventHandler] handles it. This provides a default "back" action.
 */
private constructor(
    private var parent: NavigationEventDispatcher?,
    private val onBackCompletedFallback: OnBackCompletedFallback?,
) {

    /**
     * Creates a **root** `NavigationEventDispatcher` with no default fallback action.
     *
     * Establishes the top-level dispatcher for a new navigation hierarchy, typically within an
     * `Activity` or a top-level composable. It creates its own internal [NavigationEventProcessor].
     *
     * If a navigation event completes without being handled by any registered
     * [NavigationEventHandler], nothing further will happen.
     */
    public constructor() : this(parent = null, onBackCompletedFallback = null)

    /**
     * Creates a **root** `NavigationEventDispatcher` with a fallback action.
     *
     * Establishes the top-level dispatcher for a new navigation hierarchy, typically within an
     * `Activity` or a top-level composable. It creates its own internal [NavigationEventProcessor].
     *
     * @param onBackCompletedFallback A lambda to be invoked if a navigation event **completes** and
     *   no registered [NavigationEventHandler] handles it. This provides a default "back" action
     *   for the entire hierarchy. **It will not be invoked if the event is cancelled.**
     */
    public constructor(
        onBackCompletedFallback: OnBackCompletedFallback
    ) : this(parent = null, onBackCompletedFallback = onBackCompletedFallback)

    /**
     * Creates a **child** `NavigationEventDispatcher` linked to a parent.
     *
     * Used to create nested dispatchers within an existing hierarchy. The new dispatcher shares the
     * same underlying [NavigationEventProcessor] as its parent, allowing it to participate in the
     * same event stream.
     *
     * @param parent The parent `NavigationEventDispatcher` to which this dispatcher will be
     *   attached.
     */
    public constructor(
        parent: NavigationEventDispatcher
    ) : this(parent = parent, onBackCompletedFallback = null)

    /**
     * Returns `true` if this dispatcher is in a terminal state and can no longer be used.
     *
     * A dispatcher is considered disposed if it has been explicitly disposed or if its [parent] has
     * been disposed. This state is checked by [checkInvariants] to prevent use-after-dispose
     * errors.
     */
    private var isDisposed: Boolean = false
        get() = if (parent?.isDisposed == true) true else field

    /**
     * Controls whether this dispatcher is active and will process navigation events.
     *
     * A dispatcher's effective enabled state is hierarchical. It depends on both its own local
     * `isEnabled` state and the state of its parent.
     *
     * **Getting the value**:
     * - This will return `false` if the `parentDispatcher` exists and its `isEnabled` state is
     *   `false`, regardless of this dispatcher's own setting. This provides a simple way to disable
     *   an entire branch of a navigation hierarchy by disabling its root.
     * - If there is no parent or the parent is enabled, it will return the local value of this
     *   property (`true` by default).
     *
     * **Setting the value**:
     * - This only updates the local enabled state for this specific dispatcher. The getter will
     *   always re-evaluate the effective state based on the parent.
     *
     * For this dispatcher to be truly active, its local `isEnabled` property must be `true`, and
     * the `isEnabled` properties of all its ancestors must also be `true`.
     */
    public var isEnabled: Boolean = true
        get() = if (parent?.isEnabled == false) false else field
        set(value) {
            checkInvariants()

            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            sharedProcessor.refreshEnabledHandlers()
        }

    /**
     * The internal, shared [NavigationEventProcessor] responsible for managing all registered
     * [NavigationEventHandler]s and orchestrating event dispatching.
     *
     * This processor ensures consistent ordering and state for all navigation events across the
     * application's hierarchy. It is initialized in one of two ways:
     * - If a [parent] is provided, this dispatcher will share its parent's processor, allowing for
     *   a hierarchical event handling structure where child dispatchers defer to their parents for
     *   core event management.
     * - If no [parent] is provided (i.e., this is a root dispatcher), a new
     *   [NavigationEventProcessor] instance is created, becoming the root of its own event handling
     *   tree.
     */
    internal val sharedProcessor: NavigationEventProcessor =
        parent?.sharedProcessor ?: NavigationEventProcessor()

    /**
     * A collection of child [NavigationEventDispatcher] instances that have registered with this
     * dispatcher.
     *
     * This set helps establish and maintain the hierarchical structure of dispatchers, allowing
     * parent dispatchers to be aware of their direct children.
     *
     * **This is primarily for cleanup when this dispatcher is no longer needed.**
     */
    internal val childDispatchers = mutableSetOf<NavigationEventDispatcher>()

    /**
     * A set of [NavigationEventHandler] instances directly registered with *this specific*
     * [NavigationEventDispatcher] instance.
     *
     * While the actual event processing and global handler management happen in the
     * [sharedProcessor], this set provides a localized record of handlers owned by this particular
     * dispatcher.
     *
     * **This is primarily for cleanup when this dispatcher is no longer needed.**
     */
    private val handlers = mutableSetOf<NavigationEventHandler<*>>()

    /**
     * A set of [NavigationEventInput] instances that are directly managed by this dispatcher.
     *
     * This dispatcher controls the lifecycle of its registered handlers, calling
     * [NavigationEventInput.onAdded] and [NavigationEventInput.onRemoved] as its own state changes.
     *
     * **This is primarily for cleanup when this dispatcher is no longer needed.**
     */
    private val inputs = mutableSetOf<NavigationEventInput>()

    /**
     * The globally observable, read-only state of the physical navigation gesture.
     *
     * This flow represents *only* the gesture's progress (e.g., [Idle] or [InProgress]) and is
     * separate from the navigation history state.
     *
     * System-level components or UI animations can subscribe to this flow to react to the start,
     * progress, and end of a gesture without needing to know about the specific, generic
     * [NavigationEventInfo] types involved in the history.
     *
     * This state is derived from the [NavigationEventTransitionState] of the currently active
     * [NavigationEventHandler].
     */
    public val transitionState: StateFlow<NavigationEventTransitionState>
        get() = sharedProcessor.transitionState

    /**
     * The globally observable, read-only state of the navigation history stack.
     *
     * This flow represents *only* the navigation stack (the [NavigationEventHistory.mergedHistory]
     * and [NavigationEventHistory.currentIndex]) and is the counterpart to transition state.
     *
     * A key contract of this state is that it remains **stable** during a navigation gesture. It
     * only updates when the navigation stack itself changes (e.g., when a new handler becomes
     * active, or the active handler's info is updated), which typically occurs *after* a gesture
     * completes or *before* one begins.
     *
     * This allows UI components to subscribe only to changes in the history stack without being
     * notified of rapid gesture progress updates from transition state.
     */
    public val history: StateFlow<NavigationEventHistory>
        get() = sharedProcessor.history

    init {
        // If a parent dispatcher is provided, register this dispatcher as its child.
        // This establishes the hierarchical relationship and ensures the parent is aware
        // of its direct descendants for proper event propagation and cleanup.
        parent?.childDispatchers += this
    }

    /**
     * Adds a new [NavigationEventHandler] to receive navigation events.
     *
     * **Handlers are invoked based on [priority], and then by recency.** All [PRIORITY_OVERLAY]
     * handlers are called before any [PRIORITY_DEFAULT] handlers. Within each priority group,
     * handlers are invoked in a Last-In, First-Out (LIFO) order—the most recently added handler is
     * called first.
     *
     * All handlers are invoked on the main thread. To stop receiving events, a handler must be
     * removed via [NavigationEventHandler.remove].
     *
     * @param handler The handler instance to be added.
     * @param priority The priority of the handler, determining its invocation order relative to
     *   others. See [NavigationEventDispatcher.Priority].
     * @throws IllegalArgumentException if the given handler is already registered with a different
     *   dispatcher.
     * @throws IllegalArgumentException if [priority] is not one of the supported constants.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @Suppress("PairedRegistration") // handler is removed via `NavigationEventHandler.remove()`
    @MainThread
    @JvmOverloads
    public fun addHandler(
        handler: NavigationEventHandler<*>,
        @Priority priority: Int = PRIORITY_DEFAULT,
    ) {
        checkInvariants()

        if (handlers.add(handler)) {
            sharedProcessor.addHandler(dispatcher = this, handler, priority)
        }
    }

    /** [NavigationEventHandler.remove] */
    internal fun removeHandler(handler: NavigationEventHandler<*>) {
        if (handlers.remove(handler)) {
            sharedProcessor.removeHandler(handler)
        }
    }

    /**
     * Adds an input with an unspecified priority, registering it with the shared processor and
     * binding it to this dispatcher's lifecycle.
     *
     * The input is registered globally with the [sharedProcessor] to receive system-wide state
     * updates (e.g., whether any handlers are enabled). It is also tracked locally by this
     * dispatcher for lifecycle management.
     *
     * The input's [NavigationEventInput.onAdded] method is invoked immediately upon addition. It
     * will be automatically detached when this dispatcher [dispose] is called.
     *
     * @param input The input to add.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     * @throws IllegalArgumentException if [input] is already added to a dispatcher.
     * @see removeInput
     * @see NavigationEventInput.onRemoved
     */
    @MainThread
    public fun addInput(input: NavigationEventInput) {
        checkInvariants()

        if (inputs.add(input)) {
            sharedProcessor.addInput(dispatcher = this, input, priority = -1)
        }
    }

    /**
     * Adds an input with a specific priority, registering it with the shared processor and binding
     * it to this dispatcher's lifecycle.
     *
     * The input is registered globally with the [sharedProcessor] to receive system-wide state
     * updates (e.g., whether any handlers are enabled). It is also tracked locally by this
     * dispatcher for lifecycle management.
     *
     * The input's [NavigationEventInput.onAdded] method is invoked immediately upon addition. It
     * will be automatically detached when this dispatcher [dispose] is called.
     *
     * @param input The input to add.
     * @param priority The priority to associate with this input. Must be one of the supported
     *   constants: [PRIORITY_OVERLAY], [PRIORITY_DEFAULT].
     * @throws IllegalStateException if the dispatcher has already been disposed.
     * @throws IllegalArgumentException if [input] is already added to a dispatcher.
     * @throws IllegalArgumentException if [priority] is not one of the supported constants.
     * @see removeInput
     * @see NavigationEventInput.onRemoved
     */
    @MainThread
    public fun addInput(input: NavigationEventInput, @Priority priority: Int) {
        checkInvariants()
        require(priority == PRIORITY_DEFAULT || priority == PRIORITY_OVERLAY) {
            // Since this method may be called from other targets (e.g., Swift),
            // IntDef lint checks may not be available. We must validate at runtime.
            "Unsupported priority value: $priority"
        }

        if (inputs.add(input)) {
            sharedProcessor.addInput(dispatcher = this, input, priority)
        }
    }

    /**
     * Removes and detaches an input from this dispatcher and the shared processor.
     *
     * This severs the input's lifecycle link to the dispatcher. Its
     * [NavigationEventInput.onRemoved] method is invoked, and it will no longer receive lifecycle
     * calls or global state updates from the processor.
     *
     * @param input The input to remove.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     * @see addInput
     * @see NavigationEventInput.onAdded
     */
    @MainThread
    public fun removeInput(input: NavigationEventInput) {
        checkInvariants()

        if (inputs.remove(input)) {
            sharedProcessor.removeInput(input)
        }
    }

    /** @see [NavigationEventProcessor.dispatchOnStarted] */
    internal fun dispatchOnStarted(
        input: NavigationEventInput,
        @Direction direction: Int,
        event: NavigationEvent?,
    ) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnStarted(input, direction, event)
    }

    /** @see [NavigationEventProcessor.dispatchOnProgressed] */
    internal fun dispatchOnProgressed(
        input: NavigationEventInput,
        @Direction direction: Int,
        event: NavigationEvent,
    ) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnProgressed(input, direction, event)
    }

    /** @see [NavigationEventProcessor.dispatchOnCompleted] */
    internal fun dispatchOnCompleted(input: NavigationEventInput, @Direction direction: Int) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnCompleted(input, direction, onBackCompletedFallback)
    }

    /** @see [NavigationEventProcessor.dispatchOnCancelled] */
    internal fun dispatchOnCancelled(input: NavigationEventInput, @Direction direction: Int) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnCancelled(input, direction)
    }

    /**
     * Removes this dispatcher and its entire chain of descendants from the hierarchy.
     *
     * This is the primary cleanup method and should be called when the component owning this
     * dispatcher is destroyed (e.g., in `DisposableEffect` in Compose).
     *
     * This is a **terminal** operation; once a dispatcher is disposed, it cannot be reused.
     *
     * Calling this method triggers a comprehensive, iterative cleanup:
     * 1. It iteratively processes and disposes of all child dispatchers and their descendants,
     *    ensuring a complete top-down cleanup of the entire sub-hierarchy without recursion.
     * 2. For each dispatcher, it first detaches all registered [NavigationEventInput] instances by
     *    calling [NavigationEventInput.onRemoved]. This severs their lifecycle link to the
     *    dispatcher and allows them to release any tied resources.
     * 3. It then removes all [NavigationEventHandler] instances registered with that dispatcher
     *    from the shared processor, preventing memory leaks.
     * 4. Finally, it removes the dispatcher from its parent's list of children, fully dismantling
     *    the hierarchy.
     *
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @MainThread
    public fun dispose() {
        checkInvariants()
        isDisposed = true // Set immediately to block potential re-entrant calls.

        // Iteratively dispose of all child dispatchers and their sub-hierarchies. We use a mutable
        // list as a work queue to process dispatchers.
        val dispatchersToDispose = ArrayDeque<NavigationEventDispatcher>()
        dispatchersToDispose += this // Start the queue with 'this' dispatcher itself.

        while (dispatchersToDispose.isNotEmpty()) {
            val currentDispatcher = dispatchersToDispose.removeFirst()

            // Set immediately to prevent changes (like adding new children) while we tear it down.
            currentDispatcher.isDisposed = true

            // Add 'currentDispatcher's children to the queue before processing 'currentDispatcher's
            // own cleanup. This ensures a complete traversal of the sub-hierarchy.
            dispatchersToDispose += currentDispatcher.childDispatchers

            // Notify all registered inputs that this dispatcher is being disposed.
            // This gives them a chance to clean up their own state, severing the lifecycle link
            // and preventing them from interacting with a disposed object.
            for (input in currentDispatcher.inputs) {
                sharedProcessor.removeInput(input)
            }
            currentDispatcher.inputs.clear()

            // Remove handlers directly owned by the currentDispatcher from the shared processor.
            for (handler in currentDispatcher.handlers) {
                // Always use the public API for removal. This ensures the component's internal
                // state is handled correctly and prevents unexpected behavior.
                handler.remove()
            }
            currentDispatcher.handlers.clear() // Clear local tracking for currentDispatcher

            // Clear the currentDispatcher's local tracking of its children, as they are either
            // added to the queue or have been processed.
            currentDispatcher.childDispatchers.clear()

            // Remove the currentDispatcher from its parent's list of children.
            // This step breaks upward references in the hierarchy.
            currentDispatcher.parent?.childDispatchers?.remove(currentDispatcher)
            currentDispatcher.parent = null // Clear local parent reference
        }
    }

    /**
     * Checks that the dispatcher has not already been disposed, guarding against use-after-dispose
     * errors or double-disposal.
     *
     * @throws IllegalStateException if [isDisposed] is true.
     */
    private fun checkInvariants() {
        check(!isDisposed) {
            "This NavigationEventDispatcher has already been disposed and cannot be used."
        }
    }

    /**
     * Defines priority levels for registering components like [NavigationEventHandler] or
     * [NavigationEventInput] with a [NavigationEventDispatcher].
     *
     * Priority determines the order of event processing.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(PRIORITY_OVERLAY, PRIORITY_DEFAULT)
    public annotation class Priority

    public companion object {
        /**
         * Highest priority level, intended for overlay UI components.
         *
         * Components at this level (e.g., dialogs, bottom sheets, navigation drawers) will receive
         * navigation events before components at [PRIORITY_DEFAULT].
         */
        public const val PRIORITY_OVERLAY: Int = 0

        /**
         * Default priority level for primary UI content.
         *
         * Components at this level will receive navigation events after [PRIORITY_OVERLAY]
         * components have been given a chance to handle them.
         */
        public const val PRIORITY_DEFAULT: Int = 1
    }
}
