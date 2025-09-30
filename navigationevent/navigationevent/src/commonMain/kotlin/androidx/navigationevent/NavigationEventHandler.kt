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
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_BACK
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_FORWARD
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import kotlin.jvm.JvmOverloads

/**
 * Base class for handling navigation gestures dispatched by a [NavigationEventDispatcher].
 *
 * A [NavigationEventHandler] defines how an active component responds to system navigation gestures
 * (such as predictive back) and exposes the directional context needed to represent the app’s
 * current navigation affordances:
 * - [backInfo]: Contextual information describing what is available when navigating back.
 * - [currentInfo]: The single active destination represented by this handler.
 * - [forwardInfo]: Contextual information describing what is available when navigating forward.
 *
 * Subclasses can override lifecycle methods (e.g., [onBackStarted], [onBackProgressed],
 * [onBackCompleted], [onBackCancelled], and their forward equivalents) to respond to gesture
 * progression and terminal outcomes.
 *
 * A handler must be registered with a [NavigationEventDispatcher] to receive events. It will only
 * be invoked while both the dispatcher and this handler are enabled.
 *
 * @param T The type of [NavigationEventInfo] associated with this handler.
 * @param initialInfo The initial value for [currentInfo].
 * @param isBackEnabled If `true`, this handler will process back navigation gestures.
 * @param isForwardEnabled If `true`, this handler will process forward navigation gestures.
 * @see NavigationEventDispatcher
 * @see NavigationEventInput
 */
public abstract class NavigationEventHandler<T : NavigationEventInfo>
public constructor(initialInfo: T, isBackEnabled: Boolean, isForwardEnabled: Boolean) {

    /**
     * Creates a handler that is only enabled for back navigation gestures.
     *
     * Forward navigation will be disabled by default.
     *
     * @param initialInfo The initial value for [currentInfo].
     * @param isBackEnabled If `true`, this handler will process back navigation gestures.
     */
    public constructor(
        initialInfo: T,
        isBackEnabled: Boolean,
    ) : this(initialInfo, isBackEnabled, isForwardEnabled = false)

    /**
     * The contextual information representing the active destination for this handler.
     *
     * This is always a single value, provided by the currently active handler, and reflects the
     * foreground navigation state at this point in time.
     */
    public var currentInfo: T = initialInfo
        private set

    /**
     * Contextual information describing the application's *back* state for this handler.
     *
     * This is **not** a back stack. Instead, it contains app-defined [NavigationEventInfo] values
     * (for example, titles or metadata) that help render back affordances in the UI. The list may
     * be empty if no back navigation is possible in this scope.
     */
    public var backInfo: List<T> = emptyList()
        private set

    /**
     * Contextual information describing the application's *forward* state for this handler.
     *
     * This is **not** a forward stack. Instead, it contains app-defined [NavigationEventInfo]
     * values that help render forward affordances in the UI. The list may be empty if no forward
     * navigation is possible in this scope.
     */
    public var forwardInfo: List<T> = emptyList()
        private set

    /**
     * The current transition state of this specific handler (e.g., [Idle] or [InProgress]).
     *
     * This state is updated by the dispatcher *before* the corresponding `on...` lifecycle methods
     * (e.g., [onBackStarted]) are called.
     */
    public var transitionState: NavigationEventTransitionState = Idle
        private set

    /**
     * Controls whether this handler is active and should be considered for back event dispatching.
     *
     * A handler's effective enabled state is hierarchical; it is directly influenced by the
     * [NavigationEventDispatcher] it is registered with.
     *
     * **Getting the value**:
     * - This will return `false` if the associated `dispatcher` exists and its `isEnabled` state is
     *   `false`, regardless of the handler's own local setting. This provides a powerful mechanism
     *   to disable a whole group of handlers at once by simply disabling their dispatcher.
     * - Otherwise, it returns the handler's own locally stored state.
     *
     * **Setting the value**:
     * - This updates the local enabled state of the handler itself.
     * - More importantly, it immediately notifies the `dispatcher` (if one is attached) that its
     *   list of enabled handlers might have changed, prompting a re-evaluation. This ensures the
     *   system's state remains consistent and responsive to changes.
     *
     * For a handler to be truly active, both its local `isEnabled` property and its dispatcher's
     * `isEnabled` property must evaluate to `true`.
     */
    public var isBackEnabled: Boolean = isBackEnabled
        get() = if (dispatcher?.isEnabled == false) false else field
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            dispatcher?.sharedProcessor?.refreshEnabledHandlers()
        }

    /**
     * Controls whether this handler is active and should be considered for forward event
     * dispatching.
     *
     * A handler's effective enabled state is hierarchical; it is directly influenced by the
     * [NavigationEventDispatcher] it is registered with.
     *
     * **Getting the value**:
     * - This will return `false` if the associated `dispatcher` exists and its `isEnabled` state is
     *   `false`, regardless of the handler's own local setting.
     * - Otherwise, it returns the handler's own locally stored state.
     *
     * **Setting the value**:
     * - This updates the local enabled state of the handler itself.
     * - It immediately notifies the `dispatcher` (if one is attached) that its list of enabled
     *   handlers might have changed, prompting a re-evaluation.
     *
     * For a handler to be truly active for forward events, both its local `isForwardEnabled`
     * property and its dispatcher's `isEnabled` property must evaluate to `true`.
     */
    public var isForwardEnabled: Boolean = isForwardEnabled
        get() = if (dispatcher?.isEnabled == false) false else field
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            dispatcher?.sharedProcessor?.refreshEnabledHandlers()
        }

    internal var dispatcher: NavigationEventDispatcher? = null

    /**
     * Removes this handler from the [NavigationEventDispatcher] it is registered with. If the
     * handler is not registered, this call does nothing.
     */
    public fun remove() {
        dispatcher?.removeHandler(this)
    }

    /**
     * Sets the directional navigation context for this handler.
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

    /** @see [NavigationEventDispatcher.dispatchOnStarted] */
    internal fun doOnBackStarted(event: NavigationEvent) {
        transitionState = InProgress(latestEvent = event, direction = TRANSITIONING_BACK)
        onBackStarted(event)
    }

    /**
     * Override this to handle the beginning of a back navigation event.
     *
     * This is called when a user action initiates a back navigation. It's the ideal place to
     * prepare UI elements for a transition.
     *
     * @param event The [NavigationEvent] that triggered this handler.
     */
    @EmptySuper protected open fun onBackStarted(event: NavigationEvent) {}

    /** @see [NavigationEventDispatcher.dispatchOnProgressed] */
    internal fun doOnBackProgressed(event: NavigationEvent) {
        transitionState = InProgress(latestEvent = event, direction = TRANSITIONING_BACK)
        onBackProgressed(event)
    }

    /**
     * Override this to handle the progress of an ongoing back navigation event.
     *
     * This is called repeatedly during a gesture-driven back navigation to update the UI in
     * real-time based on the user's input.
     *
     * @param event The [NavigationEvent] containing progress information.
     */
    @EmptySuper protected open fun onBackProgressed(event: NavigationEvent) {}

    /** @see [NavigationEventDispatcher.dispatchOnCompleted] */
    internal fun doOnBackCompleted() {
        transitionState = Idle
        onBackCompleted()
    }

    /**
     * Override this to handle the completion of a back navigation event.
     *
     * This is called when the user commits to the back navigation action, signaling that the
     * navigation should be finalized.
     *
     * The default implementation throws an [UnsupportedOperationException]. Any handler that can be
     * completed **must** override this method to handle the navigation.
     */
    @EmptySuper
    protected open fun onBackCompleted() {
        throw UnsupportedOperationException(
            "A handler that receives a 'backCompleted' event must override " +
                "'onBackCompleted()' to handle the callback."
        )
    }

    /** @see [NavigationEventDispatcher.dispatchOnCancelled] */
    internal fun doOnBackCancelled() {
        transitionState = Idle
        onBackCancelled()
    }

    /**
     * Override this to handle the cancellation of a back navigation event.
     *
     * This is called when the user cancels the navigation action, signaling that the UI should
     * return to its original state.
     */
    @EmptySuper protected open fun onBackCancelled() {}

    /** @see [NavigationEventDispatcher.dispatchOnStarted] */
    internal fun doOnForwardStarted(event: NavigationEvent) {
        transitionState = InProgress(latestEvent = event, direction = TRANSITIONING_FORWARD)
        onForwardStarted(event)
    }

    /**
     * Override this to handle the beginning of a forward navigation event.
     *
     * This is called when a user action initiates a forward navigation. It's the ideal place to
     * prepare UI elements for a transition.
     *
     * @param event The [NavigationEvent] that triggered this handler.
     */
    @EmptySuper protected open fun onForwardStarted(event: NavigationEvent) {}

    /** @see [NavigationEventDispatcher.dispatchOnProgressed] */
    internal fun doOnForwardProgressed(event: NavigationEvent) {
        transitionState = InProgress(latestEvent = event, direction = TRANSITIONING_FORWARD)
        onForwardProgressed(event)
    }

    /**
     * Override this to handle the progress of an ongoing forward navigation event.
     *
     * This is called repeatedly during a gesture-driven forward navigation to update the UI in
     * real-time based on the user's input.
     *
     * @param event The [NavigationEvent] containing progress information.
     */
    @EmptySuper protected open fun onForwardProgressed(event: NavigationEvent) {}

    /** @see [NavigationEventDispatcher.dispatchOnCompleted] */
    internal fun doOnForwardCompleted() {
        transitionState = Idle
        onForwardCompleted()
    }

    /**
     * Override this to handle the completion of a forward navigation event.
     *
     * This is called when the user commits to the forward navigation action, signaling that the
     * navigation should be finalized.
     *
     * The default implementation throws an [UnsupportedOperationException]. Any handler that can be
     * completed **must** override this method to handle the navigation.
     */
    @EmptySuper
    protected open fun onForwardCompleted() {
        throw UnsupportedOperationException(
            "A handler that receives a 'forwardCompleted' event must override " +
                "'onForwardCompleted()' to handle the callback."
        )
    }

    /** @see [NavigationEventDispatcher.dispatchOnCancelled] */
    internal fun doOnForwardCancelled() {
        transitionState = Idle
        onForwardCancelled()
    }

    /**
     * Override this to handle the cancellation of a forward navigation event.
     *
     * This is called when the user cancels the navigation action, signaling that the UI should
     * return to its original state.
     */
    @EmptySuper protected open fun onForwardCancelled() {}
}
