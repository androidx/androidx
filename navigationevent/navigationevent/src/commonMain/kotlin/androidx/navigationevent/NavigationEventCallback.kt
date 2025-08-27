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

import androidx.annotation.EmptySuper

/**
 * Receives and handles [NavigationEvent]s dispatched by a [NavigationEventDispatcher].
 *
 * This is the base class you should extend to create custom navigation event logic. Callbacks are
 * added to a [NavigationEventDispatcher] and will only receive events when both the callback and
 * its dispatcher are enabled.
 *
 * @param isEnabled The initial enabled state for this callback. Defaults to `true`.
 * @see NavigationEventDispatcher
 * @see NavigationEventInput
 */
public abstract class NavigationEventCallback<T : NavigationEventInfo>(isEnabled: Boolean = true) {

    /** The most recent navigation info provided via [setInfo]. */
    internal var currentInfo: T? = null
        private set

    /** Caches the navigation info from before the most recent call to [setInfo]. */
    internal var previousInfo: T? = null
        private set

    /**
     * Controls whether this callback is active and should be considered for event dispatching.
     *
     * A callback's effective enabled state is hierarchical; it is directly influenced by the
     * [NavigationEventDispatcher] it is registered with.
     *
     * **Getting the value**:
     * - This will return `false` if the associated `dispatcher` exists and its `isEnabled` state is
     *   `false`, regardless of the callback's own local setting. This provides a powerful mechanism
     *   to disable a whole group of callbacks at once by simply disabling their dispatcher.
     * - Otherwise, it returns the callback's own locally stored state.
     *
     * **Setting the value**:
     * - This updates the local enabled state of the callback itself.
     * - More importantly, it immediately notifies the `dispatcher` (if one is attached) that its
     *   list of enabled callbacks might have changed, prompting a re-evaluation. This ensures the
     *   system's state remains consistent and responsive to changes.
     *
     * For a callback to be truly active, both its local `isEnabled` property and its dispatcher's
     * `isEnabled` property must evaluate to `true`.
     */
    public var isEnabled: Boolean = isEnabled
        get() = if (dispatcher?.isEnabled == false) false else field
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            dispatcher?.updateEnabledCallbacks()
        }

    internal var dispatcher: NavigationEventDispatcher? = null

    /**
     * Removes this callback from the [NavigationEventDispatcher] it is registered with. If the
     * callback is not registered, this call does nothing.
     */
    public fun remove() {
        dispatcher?.removeCallback(this)
    }

    /**
     * Updates the current and previous navigation information for this callback.
     *
     * This method updates the callback's local info and then notifies the central
     * `NavigationEventProcessor`. The processor is responsible for deciding whether to update the
     * global navigation state, ensuring that only the highest-priority callback can influence the
     * state.
     *
     * @param currentInfo The new navigation information to be set as the current state.
     * @param previousInfo The navigation information to be set as the previous state.
     */
    public fun setInfo(currentInfo: T, previousInfo: T?) {
        this.currentInfo = currentInfo
        this.previousInfo = previousInfo

        // Simply notify the processor that info has changed.
        // The processor now owns all the logic for updating the shared state.
        dispatcher?.sharedProcessor?.updateEnabledCallbackState(callback = this)
    }

    /**
     * Internal-only method for dispatching.
     *
     * @see onEventStarted
     * @see NavigationEventDispatcher.dispatchOnStarted
     */
    internal fun doOnEventStarted(event: NavigationEvent) {
        onEventStarted(event)
    }

    /**
     * Override this to handle the beginning of a navigation event.
     *
     * This is called when a user action, such as a swipe gesture, initiates a navigation. It's the
     * ideal place to prepare UI elements for a transition.
     *
     * @param event The [NavigationEvent] that triggered this callback.
     */
    @EmptySuper protected open fun onEventStarted(event: NavigationEvent) {}

    /**
     * Internal-only method for dispatching.
     *
     * @see onEventProgressed
     * @see NavigationEventDispatcher.dispatchOnProgressed
     */
    internal fun doOnEventProgressed(event: NavigationEvent) {
        onEventProgressed(event)
    }

    /**
     * Override this to handle the progress of an ongoing navigation event.
     *
     * This is called repeatedly during a gesture-driven navigation (e.g., a predictive back swipe)
     * to update the UI in real-time based on the user's input.
     *
     * @param event The [NavigationEvent] containing progress information.
     */
    @EmptySuper protected open fun onEventProgressed(event: NavigationEvent) {}

    /**
     * Internal-only method for dispatching.
     *
     * @see onEventCompleted
     * @see NavigationEventDispatcher.dispatchOnCompleted
     */
    internal fun doOnEventCompleted() {
        onEventCompleted()
    }

    /**
     * Override this to handle the completion of a navigation event.
     *
     * This is called when the user commits to the navigation action (e.g., by lifting their finger
     * at the end of a swipe), signaling that the navigation should be finalized.
     */
    protected abstract fun onEventCompleted()

    /**
     * Internal-only method for dispatching.
     *
     * @see onEventCancelled
     * @see NavigationEventDispatcher.dispatchOnCancelled
     */
    internal fun doOnEventCancelled() {
        onEventCancelled()
    }

    /**
     * Override this to handle the cancellation of a navigation event.
     *
     * This is called when the user cancels the navigation action (e.g., by returning their finger
     * to the edge of the screen), signaling that the UI should return to its original state.
     */
    @EmptySuper protected open fun onEventCancelled() {}
}
