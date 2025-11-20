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

import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_DEFAULT
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_OVERLAY
import androidx.navigationevent.NavigationEventDispatcher.Priority
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_BACK
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_FORWARD
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_UNKNOWN
import androidx.navigationevent.NavigationEventTransitionState.Direction
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the lifecycle and dispatching of [NavigationEventHandler] instances across all
 * NavigationEventDispatcher instances. This class ensures consistent ordering, state management,
 * and prioritized dispatch for navigation events.
 */
internal class NavigationEventProcessor {

    /**
     * The private, mutable source of truth for the global [NavigationEventTransitionState]. This
     * flow is updated by the processor based on the active handler's gesture state.
     */
    private val _transitionState = MutableStateFlow<NavigationEventTransitionState>(Idle)

    /** @see [NavigationEventDispatcher.transitionState] */
    val transitionState = _transitionState.asStateFlow()

    /**
     * The private, mutable source of truth for the global [NavigationEventHistory]. This flow is
     * updated by the processor whenever the active handler changes or updates its info.
     */
    private val _history = MutableStateFlow(NavigationEventHistory())

    /**
     * The globally observable, read-only state of the navigation history stack.
     *
     * This flow represents *only* the navigation stack (the [NavigationEventHistory.mergedHistory]
     * and [NavigationEventHistory.currentIndex]) and is the counterpart to [transitionState].
     *
     * A key contract of this state is that it remains **stable** during a navigation gesture. It
     * only updates when the navigation stack itself changes (e.g., when a new handler becomes
     * active, or the active handler's info is updated), which typically occurs *after* a gesture
     * completes or *before* one begins.
     *
     * This allows UI components to subscribe only to changes in the history stack without being
     * notified of rapid gesture progress updates from [transitionState].
     */
    val history = _history.asStateFlow()

    /**
     * Stores high-priority handlers that should be evaluated before default handlers.
     *
     * [ArrayDeque] is used for efficient `addFirst()` and `remove()` operations, which is ideal for
     * maintaining a Last-In, First-Out (LIFO) dispatch order. This means the most recently added
     * overlay handler is the first to be checked.
     *
     * @see [defaultHandlers]
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
    private val defaultHandlers = ArrayDeque<NavigationEventHandler<*>>()

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
     * @see [defaultHandlers]
     */
    private var inProgressHandler: NavigationEventHandler<*>? = null

    /**
     * The direction of the navigation event currently in progress.
     *
     * This is non-null only when [inProgressHandler] is also non-null. Its lifecycle is tied
     * directly to the active navigation event.
     */
    @Direction private var inProgressDirection = TRANSITIONING_UNKNOWN

    /**
     * The [NavigationEventInput] that initiated the currently active gesture.
     *
     * This property is set alongside [inProgressHandler] when a `dispatchOnStarted` event is
     * successfully processed. Its lifecycle is tied directly to the active gesture (i.e., it is
     * non-null only when [inProgressHandler] is non-null).
     *
     * Its primary purpose is to distinguish event sources. Subsequent dispatch calls (like
     * `onProgressed` or `onCompleted`) must originate from this same input to be considered part of
     * the active gesture. Events from other inputs will be ignored or will trigger a cancellation
     * of this in-progress event.
     */
    private var inProgressInput: NavigationEventInput? = null

    /**
     * Holds inputs that were registered without a specific priority.
     *
     * These are typically treated as the lowest priority level, processed only after
     * [defaultInputs] and [overlayInputs].
     */
    private val unspecifiedInputs = mutableSetOf<NavigationEventInput>()

    /**
     * Holds inputs registered with the [PRIORITY_DEFAULT] priority.
     *
     * This level is for primary UI content and is processed before [unspecifiedInputs] but after
     * [overlayInputs].
     */
    private val defaultInputs = mutableSetOf<NavigationEventInput>()

    /**
     * Holds inputs registered with the [PRIORITY_OVERLAY] priority.
     *
     * This is the highest priority level, intended for UI elements like dialogs or bottom sheets
     * that appear on top of other content.
     */
    private val overlayInputs = mutableSetOf<NavigationEventInput>()

    /** Whether at least one handler with [PRIORITY_DEFAULT] is enabled. */
    private var hasEnabledDefaultHandlers = false

    /** Whether at least one handler with [PRIORITY_OVERLAY] is enabled. */
    private var hasEnabledOverlayHandlers = false

    /** Whether at least one handler with is enabled. */
    private var hasEnabledAnyHandlers = false

    /**
     * Recalculates the enabled status for all callback priorities, notifies listeners of any
     * changes, and synchronizes the global navigation state.
     *
     * This is the central update method and should be called whenever a callback is added, removed,
     * or its own enabled status changes.
     */
    fun refreshEnabledHandlers() {
        // 1) Snapshot new truth from current callbacks.
        // Use `any` instead of `filter` to avoid allocating intermediate lists.
        // (`any` also short-circuits on the first match, making it strictly cheaper.)
        val newOverlayEnabled = overlayHandlers.any { it.isBackEnabled || it.isForwardEnabled }
        val newDefaultEnabled = defaultHandlers.any { it.isBackEnabled || it.isForwardEnabled }
        val newAnyEnabled = newOverlayEnabled || newDefaultEnabled

        val overlayEnabledChanged = hasEnabledOverlayHandlers != newOverlayEnabled
        val defaultEnabledChanged = hasEnabledDefaultHandlers != newDefaultEnabled
        val anyEnabledChanged = hasEnabledAnyHandlers != newAnyEnabled

        // 2) Notify only when a priority’s state actually changed.
        if (overlayEnabledChanged) {
            for (input in overlayInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = newOverlayEnabled)
            }
        }

        if (defaultEnabledChanged) {
            for (input in defaultInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = newDefaultEnabled)
            }
        }

        if (anyEnabledChanged) {
            for (input in unspecifiedInputs) {
                input.doOnHasEnabledHandlersChanged(hasEnabledHandlers = newAnyEnabled)
            }
        }

        // 3) Commit new flags *after* notifications so change detection compares against the
        // previous published state. This prevents spurious notifications within the same cycle.
        hasEnabledOverlayHandlers = newOverlayEnabled
        hasEnabledDefaultHandlers = newDefaultEnabled
        hasEnabledAnyHandlers = newAnyEnabled

        // 4) Synchronize the global navigation state to the active (highest-priority) enabled
        // callback.
        updateEnabledHandlerInfo(handler = inProgressHandler ?: resolveEnabledHandler())
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
    internal fun updateEnabledHandlerInfo(handler: NavigationEventHandler<*>?) {
        // Pick the single handler that is allowed to control state right now.
        val activeHandler = inProgressHandler ?: resolveEnabledHandler()

        if (activeHandler != handler) {
            return
        }

        val newHistory =
            if (activeHandler == null) {
                // If all handlers are removed or disabled (making 'activeHandler' null),
                // we must reset the global state to the default empty history or we will
                // get stuck on the state of the last-known active handler.
                NavigationEventHistory()
            } else {
                NavigationEventHistory(
                    backInfo = resolveCombinedBackInfo(),
                    currentInfo = activeHandler.currentInfo,
                    forwardInfo = activeHandler.forwardInfo,
                )
            }

        // To avoid redundant state updates and notifications, exit if nothing has changed.
        val oldHistory = _history.value
        if (oldHistory == newHistory) {
            return
        }

        _history.value = newHistory

        // Notify inputs directly for immediate, synchronous updates. This avoids
        // delays from the coroutine dispatcher, ensuring that consumers can react
        // to the state change within the same frame.
        for (input in overlayInputs) {
            input.doOnHistoryChanged(newHistory)
        }
        for (input in defaultInputs) {
            input.doOnHistoryChanged(newHistory)
        }
        for (input in unspecifiedInputs) {
            input.doOnHistoryChanged(newHistory)
        }
    }

    /** [NavigationEventDispatcher.addHandler] */
    fun addHandler(
        dispatcher: NavigationEventDispatcher,
        handler: NavigationEventHandler<*>,
        @Priority priority: Int = PRIORITY_DEFAULT,
    ) {
        // Enforce that a handler is not already registered with another dispatcher.
        require(handler.dispatcher == null) {
            "Handler '$handler' is already registered with a dispatcher"
        }

        // Add to the front of the appropriate queue to achieve LIFO ordering.
        when (priority) {
            PRIORITY_OVERLAY -> overlayHandlers.addFirst(handler)
            PRIORITY_DEFAULT -> defaultHandlers.addFirst(handler)
            else -> {
                // Since this method may be called from other targets (e.g., Swift),
                // IntDef lint checks may not be available. We must validate at runtime.
                throw IllegalArgumentException("Unsupported priority value: $priority")
            }
        }

        // Store the dispatcher reference on the callback for self-management and internal tracking.
        handler.dispatcher = dispatcher
        refreshEnabledHandlers()
    }

    /** [NavigationEventHandler.remove] */
    fun removeHandler(handler: NavigationEventHandler<*>) {
        // If the handler is the one currently being processed, it needs to be notified of
        // cancellation and then cleared from the in-progress state.
        if (handler == inProgressHandler) {
            when (inProgressDirection) {
                TRANSITIONING_BACK -> handler.doOnBackCancelled()
                TRANSITIONING_FORWARD -> handler.doOnForwardCancelled()
            }
            inProgressHandler = null
            inProgressDirection = TRANSITIONING_UNKNOWN
            inProgressInput = null
        }

        // The `remove()` operation on ArrayDeque is efficient and simply returns `false` if the
        // element is not found. There's no need for a preceding `contains()` check.
        overlayHandlers.remove(handler)
        defaultHandlers.remove(handler)

        // Clear the dispatcher reference to mark the handler as unregistered and available for
        // re-registration.
        handler.dispatcher = null
        refreshEnabledHandlers()
    }

    /** [NavigationEventDispatcher.addInput] */
    fun addInput(
        dispatcher: NavigationEventDispatcher,
        input: NavigationEventInput,
        priority: Int,
    ) {
        require(input.dispatcher == null) {
            "Input '$input' is already added to dispatcher ${input.dispatcher}."
        }

        val inputs =
            when (priority) {
                PRIORITY_OVERLAY -> overlayInputs
                PRIORITY_DEFAULT -> defaultInputs
                else -> unspecifiedInputs
            }
        inputs += input

        input.dispatcher = dispatcher
        input.doOnAdded(dispatcher)

        // Input must get 'history' immediately to avoid missing initial state.
        input.doOnHistoryChanged(history = history.value)

        // Input must get 'hasEnabledHandlers' immediately to avoid missing initial state.
        val hasEnabledHandlers =
            when (priority) {
                PRIORITY_OVERLAY -> hasEnabledOverlayHandlers
                PRIORITY_DEFAULT -> hasEnabledDefaultHandlers
                else -> hasEnabledAnyHandlers
            }
        input.doOnHasEnabledHandlersChanged(hasEnabledHandlers)
    }

    /** [NavigationEventDispatcher.removeInput] */
    fun removeInput(input: NavigationEventInput) {
        // The `remove()` operation on `Set` is efficient and simply returns `false` if the
        // element is not found. There's no need for a preceding `contains()` check.
        overlayInputs.remove(input)
        defaultInputs.remove(input)
        unspecifiedInputs.remove(input)
        input.dispatcher = null
        input.doOnRemoved()
    }

    /**
     * Starts a navigation event, which may be predictive or non-predictive.
     *
     * If [event] is non-null, this starts a **predictive** gesture. The handler's
     * `doOn...Started()` callback is invoked with the [event] (which contains edge information) and
     * the state is moved to [NavigationEventTransitionState.InProgress].
     *
     * If [event] is null, this starts a **non-predictive** event (e.g., a button press).
     * `doOn...Started()` is skipped, and the handler will only be notified upon completion or
     * cancellation.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event.
     * @param event The [NavigationEvent], or `null` for non-predictive events.
     */
    fun dispatchOnStarted(
        input: NavigationEventInput,
        @Direction direction: Int,
        event: NavigationEvent? = null,
    ) {
        if (inProgressDirection != TRANSITIONING_UNKNOWN) {
            return
        }

        // Find the highest-priority enabled handler to handle this event.
        val handler = resolveEnabledHandler(direction)

        // Set this handler as the one in progress *before* execution. This ensures
        // `onCancelled` can be correctly handled if the handler removes itself during
        // `onEventStarted`.
        inProgressHandler = handler
        inProgressDirection = direction
        inProgressInput = input

        // A non-null event indicates a new predictive gesture is starting.
        if (event != null) {
            when (direction) {
                TRANSITIONING_BACK -> handler?.doOnBackStarted(event)
                TRANSITIONING_FORWARD -> handler?.doOnForwardStarted(event)
                TRANSITIONING_UNKNOWN -> {}
            }

            _transitionState.value = InProgress(latestEvent = event, direction = direction)
        } else {
            // We skip 'doOn...Started()' here. That callback (with NavigationEvent) is only for
            // predictive gestures (edge data, etc.). Non-predictive events should go straight to
            // onCompleted to match existing behavior in OnBackPressedDispatcher and Fragments.
        }
    }

    /**
     * Reports progress for a predictive navigation event.
     *
     * If a handler is already in progress (from `dispatchOnStarted`), only that handler is
     * notified. Otherwise, the highest-priority enabled handler is resolved and receives the
     * progress event. Progress is non-terminal; [inProgressHandler] is not cleared. The transition
     * state is updated to [NavigationEventTransitionState.InProgress].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event.
     * @param event The [NavigationEvent] to dispatch to the handler.
     */
    fun dispatchOnProgressed(
        input: NavigationEventInput,
        @Direction direction: Int,
        event: NavigationEvent,
    ) {
        // Ignore progress events that don't match the currently active predictive gesture.
        if (input != inProgressInput || direction != inProgressDirection) {
            return
        }

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)
        // Progressed is not a terminal event, so `inProgress` is not cleared.

        when (direction) {
            TRANSITIONING_BACK -> handler?.doOnBackProgressed(event)
            TRANSITIONING_FORWARD -> handler?.doOnForwardProgressed(event)
            TRANSITIONING_UNKNOWN -> {}
        }

        _transitionState.value = InProgress(latestEvent = event, direction = direction)
    }

    /**
     * Completes a navigation event.
     *
     * If a handler is in progress, only that handler is notified. Otherwise, the highest-priority
     * enabled handler for [direction] is resolved. Completion is terminal: [inProgressHandler] and
     * [inProgressDirection] are cleared and the transition returns to
     * [NavigationEventTransitionState.Idle].
     *
     * Fallbacks:
     * - For [TRANSITIONING_BACK], invoke [onBackCompletedFallback] if no handler is resolved.
     * - For [TRANSITIONING_FORWARD], no fallback is invoked.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event.
     * @param onBackCompletedFallback Action to invoke if no back handler completes the event.
     */
    fun dispatchOnCompleted(
        input: NavigationEventInput,
        @Direction direction: Int,
        onBackCompletedFallback: OnBackCompletedFallback?,
    ) {
        if (input != inProgressInput || direction != inProgressDirection) {
            return
        }

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'completed' is a terminal event.
        inProgressHandler = null
        inProgressDirection = TRANSITIONING_UNKNOWN
        inProgressInput = null

        when (direction) {
            TRANSITIONING_BACK -> {
                if (handler == null) {
                    // No handler: only back events have a fallback to invoke.
                    onBackCompletedFallback?.onBackCompletedFallback()
                } else {
                    handler.doOnBackCompleted()
                }
            }
            TRANSITIONING_FORWARD -> handler?.doOnForwardCompleted()
            TRANSITIONING_UNKNOWN -> {}
        }

        // Completion is terminal regardless of handler outcome; return to Idle.
        _transitionState.value = Idle
    }

    /**
     * Dispatches a cancellation event.
     *
     * If a handler is currently in progress, only it will be notified. Otherwise, the
     * highest-priority enabled handler will be notified. This is a terminal event, clearing the
     * [inProgressHandler] and returning the state to [NavigationEventTransitionState.Idle].
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being cancelled.
     */
    fun dispatchOnCancelled(input: NavigationEventInput, @Direction direction: Int) {
        if (input != inProgressInput || direction != inProgressDirection) {
            return
        }

        // If there is a handler in progress, only that one is notified.
        // Otherwise, the highest-priority enabled handler is notified.
        val handler = inProgressHandler ?: resolveEnabledHandler(direction)

        // Clear in-progress, as 'cancelled' is a terminal event.
        inProgressHandler = null
        inProgressDirection = TRANSITIONING_UNKNOWN
        inProgressInput = null

        when (direction) {
            TRANSITIONING_BACK -> handler?.doOnBackCancelled()
            TRANSITIONING_FORWARD -> handler?.doOnForwardCancelled()
            TRANSITIONING_UNKNOWN -> {}
        }

        _transitionState.value = Idle
    }

    /**
     * Resolves which handler should handle a navigation event based on priority and its enabled
     * state for a given direction.
     *
     * This function is the core of the priority dispatch system. It ensures that only one handler
     * is selected by strictly enforcing a Last-In, First-Out (LIFO) dispatch order:
     * 1. It first scans **overlay** handlers, from most-to-least recently added.
     * 2. If no enabled overlay handler is found, it then scans **default** handlers in the same
     *    LIFO order.
     *
     * The very first handler found to be enabled for the requested direction is returned
     * immediately.
     *
     * @param direction The navigation direction to check for. If `null` (the default), the function
     *   looks for a handler enabled for **either** back or forward navigation.
     * @return The highest-priority [NavigationEventHandler] that is enabled for the specified
     *   `direction`, or `null` if none is found. If `direction` is `null`, it returns the first
     *   handler enabled for any direction.
     */
    private fun resolveEnabledHandler(
        @Direction direction: Int = TRANSITIONING_UNKNOWN
    ): NavigationEventHandler<*>? {
        return when (direction) {
            // For a 'TRANSITIONING_UNKNOWN', find the first available handler for any direction.
            TRANSITIONING_UNKNOWN -> findHandler { it.isBackEnabled || it.isForwardEnabled }
            TRANSITIONING_BACK -> findHandler { it.isBackEnabled }
            TRANSITIONING_FORWARD -> findHandler { it.isForwardEnabled }
            else -> error("Unsupported direction: '$direction'.")
        }
    }

    /**
     * Finds the highest-priority handler that matches the given [predicate].
     *
     * Handlers are searched in last-in-first-out (LIFO) order: it scans [overlayHandlers] first
     * (most recent to oldest), then [defaultHandlers]. The first handler for which [predicate]
     * returns `true` is returned.
     *
     * @param predicate Condition to test against each handler.
     * @return The first matching [NavigationEventHandler], or `null` if none match.
     */
    private inline fun findHandler(
        predicate: (NavigationEventHandler<*>) -> Boolean
    ): NavigationEventHandler<*>? {
        // Inlined, so no function call overhead or lambda allocation.
        // 'firstOrNull' is efficient and respects the LIFO order of the ArrayDeque.
        return overlayHandlers.firstOrNull(predicate) ?: defaultHandlers.firstOrNull(predicate)
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
        for (handler in defaultHandlers) {
            if (handler.isBackEnabled && handler.backInfo.isNotEmpty()) {
                combinedBackInfo.addAll(handler.backInfo)
            }
        }

        return combinedBackInfo
    }
}
