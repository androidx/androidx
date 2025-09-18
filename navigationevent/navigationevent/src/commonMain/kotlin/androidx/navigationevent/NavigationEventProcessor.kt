/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventPriority.Companion.Default
import androidx.navigationevent.NavigationEventPriority.Companion.Overlay
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the lifecycle and dispatching of [NavigationEventHandler] instances across all
 * NavigationEventDispatcher instances. This class ensures consistent ordering, state management,
 * and prioritized dispatch for navigation events.
 */
internal class NavigationEventProcessor {

    /** The private, mutable source of truth for the navigation state. */
    internal val _state = MutableStateFlow<NavigationEventState<*>>(Idle(currentInfo = NotProvided))

    /**
     * The [StateFlow] from the highest-priority, enabled navigation handler.
     *
     * This represents the navigation state of the currently active component.
     */
    val state: StateFlow<NavigationEventState<*>> = _state.asStateFlow()

    /**
     * Stores high-priority handlers that should be evaluated before default handlers.
     *
     * [ArrayDeque] is used for efficient `addFirst()` and `remove()` operations, which is ideal for
     * maintaining a Last-In, First-Out (LIFO) dispatch order. This means the most recently added
     * overlay handler is the first to be checked.
     *
     * @see [defaultHandler]
     * @see [inProgressHandler]
     */
    private val overlayHandlers = ArrayDeque<NavigationEventHandler<*>>()

    /**
     * Stores standard-priority handlers.
     *
     * Like [overlayHandlers`, this uses [ArrayDeque] to efficiently manage a LIFO queue, ensuring
     * the most recently added default handler is checked first within its priority level.
     *
     * @see [overlayHandlers]
     * @see [inProgressHandler]
     */
    private val defaultHandler = ArrayDeque<NavigationEventHandler<*>>()

    /**
     * The handler for a navigation event that is currently in progress.
     *
     * This handler has the highest dispatch priority, ensuring that terminal events (like
     * [dispatchOnCompleted] or [dispatchOnCancelled]) are delivered only to the participant of the
     * active navigation. This is cleared after the event is terminated.
     *
     * Notably, if this handler is removed while an event is in progress, it is implicitly treated
     * as a terminal event and receives a cancellation call before being removed.
     *
     * @see [overlayHandlers]
     * @see [defaultHandler]
     */
    private var inProgressHandler: NavigationEventHandler<*>? = null

    /**
     * The direction of the navigation event currently in progress.
     *
     * This is non-null only when [inProgressHandler] is also non-null. Its lifecycle is tied
     * directly to the active navigation event.
     */
    private var inProgressDirection: NavigationEventDirection? = null

    /**
     * A central registry of all active [NavigationEventInput] instances associated with this
     * processor.
     *
     * This set is managed by the [NavigationEventDispatcher] and allows the processor to
     * communicate global state changes—such as whether any handlers are enabled—to all relevant
     * input sources.
     *
     * It is not intended for direct public use and is exposed internally for the dispatcher.
     */
    val inputs = mutableSetOf<NavigationEventInput>()

    /**
     * Represents whether there is at least one enabled handler registered across all dispatchers.
     *
     * This property serves as a global flag that input handlers can observe to enable or disable
     * system back gestures. For example, on Android, this would control `OnBackInvokedDispatcher.`
     * `OnBackPressedDispatcher.setEnabled()`.
     *
     * It is updated automatically when handlers are added, removed, or their enabled state changes.
     * When its value changes, it notifies all registered [NavigationEventInput] instances.
     */
    private var hasEnabledHandlers: Boolean = false
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            for (input in inputs) {
                input.doOnHasEnabledHandlerChanged(hasEnabledHandler = value)
            }
        }

    /**
     * Recomputes and updates [hasEnabledHandler] based on the enabled status of all registered
     * handlers. This should be called whenever a handler’s enabled state or its registration status
     * (added or removed) changes.
     */
    fun updateEnabledHandlers() {
        // `any` and `||` are efficient as they short-circuit on the first `true` result.
        hasEnabledHandlers =
            overlayHandlers.any { it.isBackEnabled } || defaultHandler.any { it.isBackEnabled }

        // Whenever the set of enabled handlers changes, we must immediately
        // synchronize the global navigation state. This picks the new highest-priority
        // active handler and updates the state to reflect its info, preventing stale data.
        val enabledHandler =
            inProgressHandler
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Back)
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Forward)
        if (enabledHandler != null) {
            updateEnabledHandlerInfo(enabledHandler)
        }
    }

    /**
     * Called by a [NavigationEventHandler] when its info changes via
     * [NavigationEventHandler.setInfo].
     *
     * This method centralizes the state update logic. It checks if the handler that changed is the
     * authoritative one (either the [inProgressHandler] or the highest-priority idle handler)
     * before updating the shared `_state`. This prevents lower-priority handlers from incorrectly
     * overwriting the state.
     */
    internal fun updateEnabledHandlerInfo(handler: NavigationEventHandler<*>) {
        // Pick the single handler that is allowed to control state right now.
        val currentHandler =
            inProgressHandler
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Back)
                ?: resolveEnabledHandler(direction = NavigationEventDirection.Forward)

        if (currentHandler != handler) return

        val currentInfo = currentHandler.currentInfo ?: NotProvided
        val combinedBackInfo = resolveCombinedBackInfo()
        val forwardInfo = currentHandler.forwardInfo

        _state.update { state ->
            when (state) {
                is Idle -> Idle(currentInfo, combinedBackInfo, forwardInfo)
                is InProgress ->
                    InProgress(currentInfo, combinedBackInfo, forwardInfo, state.latestEvent)
            }
        }
    }

    /**
     * Returns `true` if there is at least one [NavigationEventHandler.isBackEnabled] handler
     * registered globally within this processor.
     *
     * @return `true` if any handler is enabled, `false` otherwise.
     */
    fun hasEnabledHandler(): Boolean = hasEnabledHandlers

    /**
     * Checks if there are any registered handlers, either overlay or normal.
     *
     * @return `true` if there is at least one overlay handler or one normal handler registered,
     *   `false` otherwise.
     */
    fun hasHandlers(): Boolean = overlayHandlers.isNotEmpty() || defaultHandler.isNotEmpty()

    /**
     * Adds a new [NavigationEventHandler] to receive navigation events, associating it with its
     * [NavigationEventDispatcher].
     *
     * Handlers are placed into priority-specific queues ([Overlay] or [Default]) and within those
     * queues, they are ordered in Last-In, First-Out (LIFO) manner. This ensures that the most
     * recently added handler of a given priority is considered first.
     *
     * All handlers are invoked on the main thread. To stop receiving events, a handler must be
     * removed via [NavigationEventHandler.remove].
     *
     * @param dispatcher The [NavigationEventDispatcher] instance registering this handler. This
     *   link is stored on the handler itself to enable self-removal and state tracking.
     * @param handler The handler instance to be added.
     * @param priority The priority of the handler, determining its invocation order relative to
     *   others. See [NavigationEventPriority].
     * @throws IllegalArgumentException if the given handler is already registered with a different
     *   dispatcher.
     */
    @Suppress("PairedRegistration") // Handler is removed via `NavigationEventHandler.remove()`
    @MainThread
    fun addHandler(
        dispatcher: NavigationEventDispatcher,
        handler: NavigationEventHandler<*>,
        priority: NavigationEventPriority = Default,
    ) {
        // Enforce that a handler is not already registered with another dispatcher.
        require(handler.dispatcher == null) {
            "Handler '$handler' is already registered with a dispatcher"
        }

        // Add to the front of the appropriate queue to achieve LIFO ordering.
        when (priority) {
            Overlay -> overlayHandlers.addFirst(handler)
            Default -> defaultHandler.addFirst(handler)
        }

        // Store the dispatcher reference on the handler for self-management and internal tracking.
        handler.dispatcher = dispatcher
        updateEnabledHandlers()
    }

    /**
     * Removes a [NavigationEventHandler] from the processor's registry.
     *
     * If the handler is currently part of an active event (i.e., it is the [inProgressHandler]), it
     * will be notified of cancellation before being removed. This method is idempotent and can be
     * called safely even if the handler is not currently registered.
     *
     * @param handler The [NavigationEventHandler] to remove.
     */
    @MainThread
    fun removeHandler(handler: NavigationEventHandler<*>) {
        // If the handler is the one currently being processed, it needs to be notified of
        // cancellation and then cleared from the in-progress state.
        if (handler == inProgressHandler) {
            when (inProgressDirection) {
                NavigationEventDirection.Back -> handler.doOnBackCancelled()
                NavigationEventDirection.Forward -> handler.doOnForwardCancelled()
            }
            inProgressHandler = null
            inProgressDirection = null
        }

        // The `remove()` operation on ArrayDeque is efficient and simply returns `false` if the
        // element is not found. There's no need for a preceding `contains()` check.
        overlayHandlers.remove(handler)
        defaultHandler.remove(handler)

        // Clear the dispatcher reference to mark the handler as unregistered and available for
        // re-registration.
        handler.dispatcher = null
        updateEnabledHandlers()
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackStarted] event with the given event to the
     * highest-priority enabled handler.
     *
     * If an event is currently in progress, it will be cancelled first to ensure a clean state for
     * the new event. Only the single, highest-priority enabled handler is notified and becomes the
     * [inProgressHandler].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the handler.
     */
    @MainThread
    fun dispatchOnStarted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        if (inProgressHandler != null) {
            // It's important to ensure that any ongoing operations from previous events are
            // properly cancelled before starting new ones to maintain a consistent state.
            dispatchOnCancelled(input, direction)
        }

        // Find the highest-priority enabled handler to handle this event.
        val handler = resolveEnabledHandler(direction)
        if (handler != null) {
            // Set this handler as the one in progress *before* execution. This ensures
            // `onCancelled` can be correctly handled if the handler removes itself during
            // `onEventStarted`.
            inProgressHandler = handler
            inProgressDirection = direction
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackStarted(event)
                NavigationEventDirection.Forward -> handler.doOnForwardStarted(event)
            }
            _state.update { state ->
                InProgress(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                    latestEvent = event,
                )
            }
        }
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackProgressed] event with the given event.
     *
     * If a handler is currently in progress (from a [dispatchOnStarted] call), only that handler
     * will be notified. Otherwise, the highest-priority enabled handler will receive the progress
     * event. This is not a terminal event, so [inProgressHandler] is not cleared.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the handler.
     */
    @MainThread
    fun dispatchOnProgressed(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)
        // Progressed is not a terminal event, so `inProgress` is not cleared.

        if (handler != null) {
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackProgressed(event)
                NavigationEventDirection.Forward -> handler.doOnForwardProgressed(event)
            }
            _state.update { state ->
                InProgress(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                    latestEvent = event,
                )
            }
        }
    }

    /**
     * Dispatches a navigation completion event to the appropriate handler.
     *
     * If a handler is currently in progress, only that one will be notified. Otherwise, the
     * highest-priority enabled handler for the given [direction] is chosen. Completion is a
     * terminal event, so [inProgressHandler] is always cleared afterward.
     *
     * If no handler handles the event:
     * - For [NavigationEventDirection.Back], the [fallbackOnBackPressed] action is invoked.
     * - For [NavigationEventDirection.Forward], no fallback is triggered.
     *
     * After dispatching, the dispatcher always transitions back to [Idle] state.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event that completed.
     * @param fallbackOnBackPressed The action to invoke if no handler handles a back completion
     *   event.
     */
    @MainThread
    fun dispatchOnCompleted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        fallbackOnBackPressed: (() -> Unit)?,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'completed' is a terminal event.
        inProgressHandler = null
        inProgressDirection = null

        // No handler: only back events have a fallback to invoke.
        if (handler == null && direction == NavigationEventDirection.Back) {
            fallbackOnBackPressed?.invoke()
        }

        // No handler: does nothing.
        when (direction) {
            NavigationEventDirection.Back -> handler?.doOnBackCompleted()
            NavigationEventDirection.Forward -> handler?.doOnForwardCompleted()
        }

        // Completion is terminal regardless of handler outcome; return to Idle.
        _state.update { state ->
            Idle(
                currentInfo = state.currentInfo,
                backInfo = state.backInfo,
                forwardInfo = state.forwardInfo,
            )
        }
    }

    /**
     * Dispatches an [NavigationEventHandler.onBackCancelled] event.
     *
     * If a handler is currently in progress, only it will be notified. Otherwise, the
     * highest-priority enabled handler will be notified. This is a terminal event, clearing the
     * [inProgressHandler].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     */
    @MainThread
    fun dispatchOnCancelled(input: NavigationEventInput, direction: NavigationEventDirection) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'cancelled' is a terminal event.
        inProgressHandler = null
        inProgressDirection = null

        if (handler != null) {
            when (direction) {
                NavigationEventDirection.Back -> handler.doOnBackCancelled()
                NavigationEventDirection.Forward -> handler.doOnForwardCancelled()
            }

            _state.update { state ->
                Idle(
                    currentInfo = state.currentInfo,
                    backInfo = state.backInfo,
                    forwardInfo = state.forwardInfo,
                )
            }
        }
    }

    /**
     * Resolves which handler should handle a navigation event based on priority and enabled state.
     *
     * This function is the core of the priority dispatch system. It ensures that only one handler
     * is selected to receive an event by strictly enforcing dispatch order. The resolution process
     * is:
     * 1. It first scans **overlay** handlers, from most-to-least recently added.
     * 2. If no enabled overlay handler is found, it then scans **default** handlers in the same
     *    LIFO order.
     *
     * The very first handler that is found to be `isEnabled` is returned immediately.
     *
     * @return The single highest-priority [NavigationEventHandler] that is currently enabled, or
     *   `null` if no enabled handlers exist.
     */
    private fun resolveEnabledHandler(
        direction: NavigationEventDirection
    ): NavigationEventHandler<*>? {
        // `firstOrNull` is efficient and respects the LIFO order of the ArrayDeque.
        return when (direction) {
            NavigationEventDirection.Back -> {
                overlayHandlers.firstOrNull { it.isBackEnabled }
                    ?: defaultHandler.firstOrNull { it.isBackEnabled }
            }
            NavigationEventDirection.Forward -> {
                overlayHandlers.firstOrNull { it.isForwardEnabled }
                    ?: defaultHandler.firstOrNull { it.isForwardEnabled }
            }
            else -> error("Unsupported NavigationEventDirection: '$direction'.")
        }
    }

    /**
     * Resolves and aggregates [NavigationEventHandler.backInfo] from all enabled handlers to
     * provide a comprehensive view of the back navigation history.
     *
     * This method constructs a unified list of [NavigationEventInfo] by traversing the registered
     * handlers in order of priority: it first collects `backInfo` from all enabled **overlay**
     * handlers, followed by all enabled **default** handlers. This ordering ensures that the
     * resulting list reflects the hierarchical navigation state, with higher-priority contexts
     * appearing first.
     *
     * This is crucial for UIs that need to display a preview of the back stack, as it allows them
     * to accurately represent the destinations that the user will navigate through when repeatedly
     * going back.
     *
     * @return A `List<NavigationEventInfo>` containing the combined back navigation history,
     *   ordered by handler priority. The list will be empty if no enabled handlers provide
     *   `backInfo`.
     */
    private fun resolveCombinedBackInfo(): List<NavigationEventInfo> {
        // TODO(b/436248277): Finalize back-info combination policy.
        //  Ambiguity: when a parent (K4) hosts a child with its own back item (L1),
        //  should the combined path be `L1 -> K4 -> parentStack` or `L1 -> parentStack`?
        //  Decide if/when to include the host's current node, and document it.

        // This function intentionally uses loops and a single mutable list. This is a
        // performance optimization to avoid the intermediate list allocations that would
        // be created by using chained collection functions like `filter` or `flatMap`.
        val combinedBackInfo = mutableListOf<NavigationEventInfo>()

        // Process overlay handlers first to respect their higher priority.
        for (handler in overlayHandlers) {
            if (handler.isBackEnabled && handler.backInfo.isNotEmpty()) {
                combinedBackInfo.addAll(handler.backInfo)
            }
        }

        // Process default handlers second.
        for (handler in defaultHandler) {
            if (handler.isBackEnabled && handler.backInfo.isNotEmpty()) {
                combinedBackInfo.addAll(handler.backInfo)
            }
        }

        return combinedBackInfo
    }
}
