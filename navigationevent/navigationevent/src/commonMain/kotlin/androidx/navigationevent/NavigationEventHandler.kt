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
import kotlin.jvm.JvmOverloads

/**
 * Base class for handling navigation gestures dispatched by a [NavigationEventDispatcher].
 *
 * A [NavigationEventHandler] defines how an active component responds to system navigation gestures
 * (such as predictive back) and exposes the directional context needed to represent the app’s
 * current navigation affordances:
 * - [backInfo]: contextual information describing what is available when navigating back.
 * - [currentInfo]: the single active destination represented by this callback.
 * - [forwardInfo]: contextual information describing what is available when navigating forward.
 *
 * Subclasses can override lifecycle methods (e.g., `onBackStarted`, `onBackProgressed`,
 * `onBackCompleted`, `onBackCancelled`, and their forward equivalents) to respond to gesture
 * progression and terminal outcomes.
 *
 * A callback must be registered with a [NavigationEventDispatcher] to receive events. It will only
 * be invoked while both the dispatcher and this callback are enabled.
 *
 * @param isBackEnabled If `true`, this handler will process back navigation gestures.
 * @param isForwardEnabled If `true`, this handler will process forward navigation gestures.
 * @see NavigationEventDispatcher
 * @see NavigationEventInput
 * @see NavigationEventState
 */
public abstract class NavigationEventHandler<T : NavigationEventInfo>
public constructor(isBackEnabled: Boolean, isForwardEnabled: Boolean) {

    /**
     * Creates a handler that is only enabled for back navigation gestures.
     *
     * Forward navigation will be disabled by default.
     *
     * @param isBackEnabled If `true`, this handler will process back navigation gestures.
     */
    public constructor(isBackEnabled: Boolean) : this(isBackEnabled, isForwardEnabled = false)

    /**
     * The contextual information representing the active destination for this callback.
     *
     * This is always a single value, provided by the currently active handler, and reflects the
     * foreground navigation state at this point in time.
     */
    internal var currentInfo: T? = null
        private set

    /**
     * Contextual information describing the application's *back* state for this callback.
     *
     * This is **not** a back stack. Instead, it contains app-defined [NavigationEventInfo] values
     * (for example, titles or metadata) that help render back affordances in the UI. The list may
     * be empty if no back navigation is possible in this scope.
     */
    internal var backInfo: List<T> = emptyList()
        private set

    /**
     * Contextual information describing the application's *forward* state for this callback.
     *
     * This is **not** a forward stack. Instead, it contains app-defined [NavigationEventInfo]
     * values that help render forward affordances in the UI. The list may be empty if no forward
     * navigation is possible in this scope.
     */
    internal var forwardInfo: List<T> = emptyList()
        private set

    /**
     * Controls whether this callback is active and should be considered for back event dispatching.
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
    public var isBackEnabled: Boolean = isBackEnabled
        get() = if (dispatcher?.isEnabled == false) false else field
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            dispatcher?.updateEnabledHandlers()
        }

    /**
     * Controls whether this callback is active for forward events and should be considered for
     * forward event dispatching.
     *
     * For a callback to be truly active for forward events, both its local `isForwardEnabled`
     * property and its dispatcher's `isForwardEnabled` property must evaluate to `true`.
     */
    public var isForwardEnabled: Boolean = isForwardEnabled
        get() = if (dispatcher?.isEnabled == false) false else field
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            dispatcher?.updateEnabledHandlers()
        }

    internal var dispatcher: NavigationEventDispatcher? = null

    /**
     * Removes this callback from the [NavigationEventDispatcher] it is registered with. If the
     * callback is not registered, this call does nothing.
     */
    public fun remove() {
        dispatcher?.removeHandler(this)
    }

    /**
     * Sets the directional navigation context for this callback.
     *
     * Updates the three pieces of contextual information used to describe navigation affordances:
     * - [currentInfo]: the active destination.
     * - [backInfo]: contextual information for back navigation (nearest-first).
     * - [forwardInfo]: contextual information for forward navigation (nearest-first).
     *
     * The lists are app-defined [NavigationEventInfo] values (e.g., titles or metadata) that help
     * the UI present navigation affordances or previews. An empty list indicates no affordance in
     * that direction.
     *
     * @param currentInfo The contextual information representing the active destination.
     * @param backInfo Context describing what is available when navigating back (nearest-first).
     * @param forwardInfo Context describing what is available when navigating forward
     *   (nearest-first).
     */
    @JvmOverloads
    public fun setInfo(
        currentInfo: T,
        backInfo: List<T> = emptyList(),
        forwardInfo: List<T> = emptyList(),
    ) {
        this.currentInfo = currentInfo
        this.backInfo = backInfo
        this.forwardInfo = forwardInfo

        // Simply notify the processor that info has changed.
        // The processor now owns all the logic for updating the shared state.
        dispatcher?.sharedProcessor?.updateEnabledHandlerInfo(handler = this)
    }

    /**
     * Internal-only method for dispatching.
     *
     * @see onBackStarted
     * @see NavigationEventDispatcher.dispatchOnStarted
     */
    internal fun doOnBackStarted(event: NavigationEvent) {
        onBackStarted(event)
    }

    /**
     * Override this to handle the beginning of a navigation event.
     *
     * This is called when a user action, such as a swipe gesture, initiates a navigation. It's the
     * ideal place to prepare UI elements for a transition.
     *
     * @param event The [NavigationEvent] that triggered this callback.
     */
    @EmptySuper protected open fun onBackStarted(event: NavigationEvent) {}

    /**
     * Internal-only method for dispatching.
     *
     * @see onBackProgressed
     * @see NavigationEventDispatcher.dispatchOnProgressed
     */
    internal fun doOnBackProgressed(event: NavigationEvent) {
        onBackProgressed(event)
    }

    /**
     * Override this to handle the progress of an ongoing navigation event.
     *
     * This is called repeatedly during a gesture-driven navigation (e.g., a predictive back swipe)
     * to update the UI in real-time based on the user's input.
     *
     * @param event The [NavigationEvent] containing progress information.
     */
    @EmptySuper protected open fun onBackProgressed(event: NavigationEvent) {}

    /**
     * Internal-only method for dispatching.
     *
     * @see onBackCompleted
     * @see NavigationEventDispatcher.dispatchOnCompleted
     */
    internal fun doOnBackCompleted() {
        onBackCompleted()
    }

    /**
     * Override this to handle the completion of a navigation event.
     *
     * This is called when the user commits to the navigation action (e.g., by lifting their finger
     * at the end of a swipe), signaling that the navigation should be finalized.
     */
    @EmptySuper protected open fun onBackCompleted() {}

    /**
     * Internal-only method for dispatching.
     *
     * @see onBackCancelled
     * @see NavigationEventDispatcher.dispatchOnCancelled
     */
    internal fun doOnBackCancelled() {
        onBackCancelled()
    }

    /**
     * Override this to handle the cancellation of a navigation event.
     *
     * This is called when the user cancels the navigation action (e.g., by returning their finger
     * to the edge of the screen), signaling that the UI should return to its original state.
     */
    @EmptySuper protected open fun onBackCancelled() {}

    internal fun doOnForwardStarted(event: NavigationEvent) {
        onForwardStarted(event)
    }

    /** Override this to handle the beginning of a forward navigation event. */
    @EmptySuper protected open fun onForwardStarted(event: NavigationEvent) {}

    internal fun doOnForwardProgressed(event: NavigationEvent) {
        onForwardProgressed(event)
    }

    /** Override this to handle the progress of an ongoing forward navigation event. */
    @EmptySuper protected open fun onForwardProgressed(event: NavigationEvent) {}

    internal fun doOnForwardCompleted() {
        onForwardCompleted()
    }

    /** Override this to handle the completion of a forward navigation event. */
    @EmptySuper protected open fun onForwardCompleted() {}

    internal fun doOnForwardCancelled() {
        onForwardCancelled()
    }

    /** Override this to handle the cancellation of a forward navigation event. */
    @EmptySuper protected open fun onForwardCancelled() {}
}
