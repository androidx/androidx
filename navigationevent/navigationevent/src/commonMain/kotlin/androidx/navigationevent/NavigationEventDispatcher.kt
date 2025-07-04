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

/**
 * Dispatcher that can be used to register [NavigationEventCallback] instances for handling the
 * in-app callbacks via composition.
 */
public class NavigationEventDispatcher(
    private val fallbackOnBackPressed: (() -> Unit)? = null,
    private val onHasEnabledCallbacksChanged: ((Boolean) -> Unit)? = null,
) {

    /**
     * A list of callbacks for a navigation event that is currently in progress.
     *
     * Callbacks in this list have the highest dispatch priority, ensuring that terminal events
     * (like [dispatchOnCompleted] or [dispatchOnCancelled]) are delivered only to the participants
     * of the active navigation. The list is cleared after the event is terminated.
     *
     * Notably, if a callback is removed while in this list, it is implicitly treated as a terminal
     * event and receives an [dispatchOnCancelled] call before being removed.
     */
    private val inProgressCallbacks: MutableList<NavigationEventCallback> = mutableListOf()

    /** Callbacks that should be processed with higher priority, before [normalCallbacks]. */
    private val overlayCallbacks = ArrayDeque<NavigationEventCallback>()

    /** Standard or default callbacks for navigation events. */
    private val normalCallbacks = ArrayDeque<NavigationEventCallback>()

    /**
     * Returns `true` if there is at least one enabled callback registered with this dispatcher. The
     * dispatcher itself is excluded.
     */
    private var hasEnabledCallbacks: Boolean = false
        set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                for (callback in onHasEnabledCallbacksChangedCallbacks) {
                    callback.invoke(value)
                }
            }
        }

    private val onHasEnabledCallbacksChangedCallbacks: MutableList<((Boolean) -> Unit)> =
        if (onHasEnabledCallbacksChanged != null) {
            mutableListOf(onHasEnabledCallbacksChanged)
        } else {
            mutableListOf()
        }

    internal fun addOnHasEnabledCallbacksChangedCallback(callback: (Boolean) -> Unit) {
        onHasEnabledCallbacksChangedCallbacks += callback
    }

    /**
     * Recomputes and updates the current [hasEnabledCallbacks] state based on the enabled status of
     * all registered callbacks.
     */
    internal fun updateEnabledCallbacks() {
        // `any` and `||` are both efficient as they short-circuit on the first `true` result.
        this.hasEnabledCallbacks =
            overlayCallbacks.any { it.isEnabled } || normalCallbacks.any { it.isEnabled }
    }

    /**
     * Returns `true` if there is at least one [NavigationEventCallback.isEnabled] callback
     * registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    public fun hasEnabledCallbacks(): Boolean = hasEnabledCallbacks

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
     */
    @Suppress("PairedRegistration") // Callback is removed via `NavigationEventCallback.remove()`
    @MainThread
    public fun addCallback(
        callback: NavigationEventCallback,
        priority: NavigationEventPriority = Default,
    ) {
        require(callback.dispatcher == null) {
            "Callback '$callback' is already registered with a dispatcher"
        }
        when (priority) {
            Overlay -> overlayCallbacks.addFirst(callback)
            Default -> normalCallbacks.addFirst(callback)
        }

        callback.dispatcher = this

        updateEnabledCallbacks()
    }

    internal fun removeCallback(callback: NavigationEventCallback) {
        // If the callback is currently being processed (i.e., it's in `inProgressCallbacks`),
        // it needs to be notified of cancellation and then removed from the in-progress tracking.
        if (callback in inProgressCallbacks) {
            callback.onEventCancelled()
            inProgressCallbacks -= callback
        }

        // Attempt to remove the callback from both overlay and normal callback lists.
        // It's okay if the callback is not present.
        overlayCallbacks.remove(callback)
        normalCallbacks.remove(callback)

        callback.dispatcher = null

        updateEnabledCallbacks()
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventStarted] event with the given event to the proper
     * callbacks
     *
     * @param event [NavigationEvent] to dispatch to the callbacks.
     */
    @MainThread
    public fun dispatchOnStarted(event: NavigationEvent) {
        if (inProgressCallbacks.isNotEmpty()) {
            // It's important to ensure that any ongoing operations from previous events are
            // properly cancelled before starting new ones to maintain a consistent state.
            dispatchOnCancelled()
        }

        for (callback in getEnabledCallbacksForDispatching()) {
            // Add callback to `inProgressCallbacks` *before* execution. This ensures `onCancelled`
            // can be called even if the callback removes itself during `onEventStarted`.
            inProgressCallbacks += callback
            callback.onEventStarted(event)
        }
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventProgressed] event with the given event to the
     * proper callbacks
     *
     * @param event [NavigationEvent] to dispatch to the callbacks.
     */
    @MainThread
    public fun dispatchOnProgressed(event: NavigationEvent) {
        // If there is callbacks in progress, only those are notified.
        // Otherwise, all enabled callbacks are notified.
        val callbacks = inProgressCallbacks.toList().ifEmpty { getEnabledCallbacksForDispatching() }
        // Do not clear in-progress, as `progressed` is not a terminal event.

        for (callback in callbacks) {
            callback.onEventProgressed(event)
        }
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCompleted] event with the given event to the
     * proper callbacks
     */
    @MainThread
    public fun dispatchOnCompleted() {
        // If there is callbacks in progress, only those are notified.
        // Otherwise, all enabled callbacks are notified.
        val callbacks = inProgressCallbacks.toList().ifEmpty { getEnabledCallbacksForDispatching() }
        inProgressCallbacks.clear() // Clear in-progress, as 'completed' is a terminal event.

        if (callbacks.isEmpty()) {
            fallbackOnBackPressed?.invoke()
        } else {
            for (callback in callbacks) {
                callback.onEventCompleted()
            }
        }
    }

    /**
     * Dispatch an [NavigationEventCallback.onEventCancelled] event with the given event to the
     * proper callbacks
     */
    @MainThread
    public fun dispatchOnCancelled() {
        // If there is callbacks in progress, only those are notified.
        // Otherwise, all enabled callbacks are notified.
        val callbacks = inProgressCallbacks.toList().ifEmpty { getEnabledCallbacksForDispatching() }
        inProgressCallbacks.clear() // Clear in-progress, as 'cancelled' is a terminal event.

        for (callback in callbacks) {
            callback.onEventCancelled()
        }
    }

    /**
     * Builds the prioritized list of callbacks for event dispatch.
     *
     * Callbacks are added in a strict priority order: [overlayCallbacks] first, then
     * [normalCallbacks]. The process stops if a callback has
     * [NavigationEventCallback.isPassThrough] is `false`, allowing it to "consume" the event and
     * prevent further propagation.
     *
     * **Performance Considerations:** This method avoids unnecessary allocations by iterating
     * directly over the source collections. The early exit on a non-pass-through callback ensures
     * that only the relevant callbacks are included in the final result.
     *
     * @return The list of callbacks to dispatch to, truncated at the first consuming callback.
     */
    private fun getEnabledCallbacksForDispatching(): List<NavigationEventCallback> {
        val callbacksForDispatching = mutableListOf<NavigationEventCallback>()

        // Process higher-priority overlay callbacks first.
        for (callback in overlayCallbacks) {
            if (callback.isEnabled) {
                callbacksForDispatching += callback
                // This callback consumes the event, so we stop here.
                if (!callback.isPassThrough) {
                    return callbacksForDispatching
                }
            }
        }

        // Then, process normal priority callbacks.
        for (callback in normalCallbacks) {
            if (callback.isEnabled) {
                callbacksForDispatching += callback
                if (!callback.isPassThrough) {
                    return callbacksForDispatching
                }
            }
        }

        return callbacksForDispatching
    }
}
