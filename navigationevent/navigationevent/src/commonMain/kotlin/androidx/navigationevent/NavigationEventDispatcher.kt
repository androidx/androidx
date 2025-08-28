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

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventPriority.Companion.Default
import androidx.navigationevent.NavigationEventPriority.Companion.Overlay
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

/**
 * A dispatcher for navigation events that can be organized hierarchically.
 *
 * This class acts as a localized entry point for registering [NavigationEventCallback] instances
 * and dispatching navigation events within a specific UI scope, such as a composable or a fragment.
 *
 * Dispatchers can be linked in a parent-child hierarchy. This structure allows for a sophisticated
 * system where nested UI components can handle navigation events independently while still
 * respecting the state of their parent. The core logic is delegated to a single, shared
 * [NavigationEventProcessor] instance across the entire hierarchy, ensuring consistent event
 * handling.
 *
 * It is important to call [dispose] when the owner of this dispatcher is destroyed (e.g., in a
 * `DisposableEffect`) to unregister callbacks and prevent memory leaks.
 */
public class NavigationEventDispatcher
/**
 * The primary, internal constructor for `NavigationEventDispatcher`.
 *
 * All public constructors delegate to this one to perform the actual initialization.
 *
 * @param parentDispatcher An optional reference to a parent [NavigationEventDispatcher]. Providing
 *   a parent allows this dispatcher to participate in a hierarchical event system, sharing the same
 *   underlying [NavigationEventProcessor] as its parent. If `null`, this dispatcher acts as the
 *   root of its own event handling hierarchy.
 * @param fallbackOnBackPressed An optional lambda to be invoked if a navigation event completes and
 *   no registered [NavigationEventCallback] handles it. This provides a default "back" action.
 */
private constructor(
    private var parentDispatcher: NavigationEventDispatcher?,
    private val fallbackOnBackPressed: (() -> Unit)?,
) {

    /**
     * Creates a **root** `NavigationEventDispatcher`.
     *
     * This constructor is used to establish the top-level dispatcher for a new navigation
     * hierarchy, typically within a scope like an `Activity` or a top-level composable. It creates
     * its own internal [NavigationEventProcessor].
     *
     * @param fallbackOnBackPressed An optional lambda to be invoked if a navigation event completes
     *   and no registered [NavigationEventCallback] handles it. This provides a default "back"
     *   action for the entire hierarchy.
     */
    public constructor(
        fallbackOnBackPressed: (() -> Unit)? = null
    ) : this(parentDispatcher = null, fallbackOnBackPressed = fallbackOnBackPressed)

    /**
     * Creates a **child** `NavigationEventDispatcher` linked to a parent.
     *
     * This constructor is used to create nested dispatchers within an existing hierarchy. The new
     * dispatcher will share the same underlying [NavigationEventProcessor] as its parent, allowing
     * it to participate in the same event stream.
     *
     * @param parentDispatcher The parent `NavigationEventDispatcher` to which this new dispatcher
     *   will be attached.
     */
    public constructor(
        parentDispatcher: NavigationEventDispatcher
    ) : this(parentDispatcher = parentDispatcher, fallbackOnBackPressed = null)

    /**
     * Returns `true` if this dispatcher is in a terminal state and can no longer be used.
     *
     * A dispatcher is considered disposed if it has been explicitly disposed or if its
     * [parentDispatcher] has been disposed. This state is checked by [checkInvariants] to prevent
     * use-after-dispose errors.
     */
    internal var isDisposed: Boolean = false
        get() = if (parentDispatcher?.isDisposed == true) true else field
        private set // The setter is private and should only be modified by the dispose() method.

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
        get() = if (parentDispatcher?.isEnabled == false) false else field
        set(value) {
            checkInvariants()

            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            updateEnabledCallbacks()
        }

    /**
     * The internal, shared [NavigationEventProcessor] responsible for managing all registered
     * [NavigationEventCallback]s and orchestrating event dispatching.
     *
     * This processor ensures consistent ordering and state for all navigation events across the
     * application's hierarchy. It is initialized in one of two ways:
     * - If a [parentDispatcher] is provided, this dispatcher will share its parent's processor,
     *   allowing for a hierarchical event handling structure where child dispatchers defer to their
     *   parents for core event management.
     * - If no [parentDispatcher] is provided (i.e., this is a root dispatcher), a new
     *   [NavigationEventProcessor] instance is created, becoming the root of its own event handling
     *   tree.
     */
    internal val sharedProcessor: NavigationEventProcessor =
        parentDispatcher?.sharedProcessor ?: NavigationEventProcessor()

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
     * A set of [NavigationEventCallback] instances directly registered with *this specific*
     * [NavigationEventDispatcher] instance.
     *
     * While the actual event processing and global callback management happen in the
     * [sharedProcessor], this set provides a localized record of callbacks owned by this particular
     * dispatcher.
     *
     * **This is primarily for cleanup when this dispatcher is no longer needed.**
     */
    private val callbacks = mutableSetOf<NavigationEventCallback<*>>()

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
     * The [StateFlow] from the highest-priority, enabled navigation callback.
     *
     * This represents the navigation state of the currently active component.
     */
    public val state: StateFlow<NavigationEventState<NavigationEventInfo>> = sharedProcessor.state

    /**
     * Creates a [StateFlow] that only emits states for a specific [NavigationEventInfo] type.
     *
     * @param T The [NavigationEventInfo] type to filter for.
     * @param scope The [CoroutineScope] in which the new [StateFlow] is created.
     * @param initialInfo The initial [NavigationEventInfo] of type [T] to be used when the
     *   [StateFlow] starts.
     * @return A [StateFlow] that emits values only when the state's destination is of type [T].
     */
    public inline fun <reified T : NavigationEventInfo> getState(
        scope: CoroutineScope,
        initialInfo: T,
    ): StateFlow<NavigationEventState<T>> {
        // We can't use filterIsInstance<NavigationEventState<T>> because the type argument `T`
        // is erased at runtime — so the JVM only sees NavigationEventState<*>. Instead, we filter
        // by checking whether the state's contained `currentInfo` is of type `T`.
        return state
            .filter { state ->
                when (state) {
                    is Idle -> state.currentInfo is T
                    is InProgress -> state.currentInfo is T
                }
            }
            .mapNotNull { state ->
                @Suppress("UNCHECKED_CAST")
                state as? NavigationEventState<T>
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = Idle(currentInfo = initialInfo),
            )
    }

    init {
        // If a parent dispatcher is provided, register this dispatcher as its child.
        // This establishes the hierarchical relationship and ensures the parent is aware
        // of its direct descendants for proper event propagation and cleanup.
        parentDispatcher?.childDispatchers += this
    }

    /**
     * Returns `true` if there is at least one [NavigationEventCallback.isEnabled] callback
     * registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    public fun hasEnabledCallbacks(): Boolean = sharedProcessor.hasEnabledCallbacks()

    /**
     * Recomputes and updates the current [hasEnabledCallbacks] state based on the enabled status of
     * all registered callbacks. This method should be called whenever a callback's enabled state or
     * its registration status (added/removed) changes.
     */
    internal fun updateEnabledCallbacks() {
        sharedProcessor.updateEnabledCallbacks()
    }

    /**
     * Adds a new [NavigationEventCallback] to receive navigation events.
     *
     * **Callbacks are invoked based on [priority], and then by recency.** All [Overlay] callbacks
     * are called before any [Default] callbacks. Within each priority group, callbacks are invoked
     * in a Last-In, First-Out (LIFO) order—the most recently added callback is called first.
     *
     * All callbacks are invoked on the main thread. To stop receiving events, a callback must be
     * removed via [NavigationEventCallback.remove].
     *
     * @param callback The callback instance to be added.
     * @param priority The priority of the callback, determining its invocation order relative to
     *   others. See [NavigationEventPriority].
     * @throws IllegalArgumentException if the given callback is already registered with a different
     *   dispatcher.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @Suppress("PairedRegistration") // Callback is removed via `NavigationEventCallback.remove()`
    @MainThread
    public fun addCallback(
        callback: NavigationEventCallback<*>,
        priority: NavigationEventPriority = Default,
    ) {
        checkInvariants()

        sharedProcessor.addCallback(dispatcher = this, callback, priority)
        callbacks += callback
    }

    internal fun removeCallback(callback: NavigationEventCallback<*>) {
        sharedProcessor.removeCallback(callback)
        callbacks -= callback
    }

    /**
     * Adds an input, registering it with the shared processor and binding it to this dispatcher's
     * lifecycle.
     *
     * The input is registered globally with the [sharedProcessor] to receive system-wide state
     * updates (e.g., whether any callbacks are enabled). It is also tracked locally by this
     * dispatcher for lifecycle management.
     *
     * The input's [NavigationEventInput.onAdded] method is invoked immediately upon addition. It
     * will be automatically detached when this dispatcher [dispose] is called.
     *
     * @param input The input to add.
     * @throws IllegalStateException if the dispatcher has already been disposed or if [input] is
     *   already added to a dispatcher.
     * @see removeInput
     * @see NavigationEventInput.onRemoved
     */
    @MainThread
    public fun addInput(input: NavigationEventInput) {
        checkInvariants()

        if (inputs.add(input)) {
            check(input.dispatcher == null) {
                "This input is already added to dispatcher ${input.dispatcher}."
            }
            sharedProcessor.inputs += input
            input.dispatcher = this
            input.doOnAdded(this)
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
            sharedProcessor.inputs -= input
            input.dispatcher = null
            input.doOnRemoved()
        }
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventStarted] event with the given event. This call is
     * delegated to the shared [NavigationEventProcessor].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the callbacks.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @MainThread
    internal fun dispatchOnStarted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnStarted(input, direction, event)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventProgressed] event with the given event. This call
     * is delegated to the shared [NavigationEventProcessor].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the callbacks.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @MainThread
    internal fun dispatchOnProgressed(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnProgressed(input, direction, event)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCompleted] event. This call is delegated to the
     * shared [NavigationEventProcessor], passing the fallback action.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @MainThread
    internal fun dispatchOnCompleted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
    ) {
        checkInvariants()

        if (!isEnabled) return
        sharedProcessor.dispatchOnCompleted(input, direction, fallbackOnBackPressed)
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCancelled] event. This call is delegated to the
     * shared [NavigationEventProcessor].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @throws IllegalStateException if the dispatcher has already been disposed.
     */
    @MainThread
    internal fun dispatchOnCancelled(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
    ) {
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
     * 3. It then removes all [NavigationEventCallback] instances registered with that dispatcher
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
                sharedProcessor.inputs -= input
                input.dispatcher = null
                input.doOnRemoved()
            }
            inputs.clear()

            // Remove callbacks directly owned by the currentDispatcher from the shared processor.
            for (callback in currentDispatcher.callbacks) {
                // Always use the public API for removal. This ensures the component's internal
                // state is handled correctly and prevents unexpected behavior.
                callback.remove()
            }
            currentDispatcher.callbacks.clear() // Clear local tracking for currentDispatcher

            // Clear the currentDispatcher's local tracking of its children, as they are either
            // added to the queue or have been processed.
            currentDispatcher.childDispatchers.clear()

            // Remove the currentDispatcher from its parent's list of children.
            // This step breaks upward references in the hierarchy.
            currentDispatcher.parentDispatcher?.childDispatchers?.remove(currentDispatcher)
            currentDispatcher.parentDispatcher = null // Clear local parent reference
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
}
