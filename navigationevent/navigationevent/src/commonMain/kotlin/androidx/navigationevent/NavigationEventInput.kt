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
import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_BACK
import androidx.navigationevent.NavigationEventTransitionState.Companion.TRANSITIONING_FORWARD
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress

/**
 * An abstract class for components that generate and dispatch navigation events.
 *
 * This class acts as the "input" side of the navigation system, translating platform-specific
 * events (like system back gestures or button clicks) into standardized events that can be sent to
 * a [NavigationEventDispatcher].
 *
 * Subclasses are responsible for implementing the logic that sources these events and calling the
 * `dispatchOn...` methods (e.g., [dispatchOnBackStarted]) to propagate them through the system.
 *
 * An input must be registered with a [NavigationEventDispatcher] using
 * [NavigationEventDispatcher.addInput] to function.
 *
 * @see NavigationEventDispatcher
 * @see NavigationEventHandler
 */
public abstract class NavigationEventInput() {

    /** The [NavigationEventDispatcher] that this input is connected to. */
    internal var dispatcher: NavigationEventDispatcher? = null

    /**
     * Tracks if a predictive **back** gesture is currently in progress from this input.
     *
     * This is set to `true` by [dispatchOnBackStarted] and reset to `false` by
     * [dispatchOnBackCompleted] or [dispatchOnBackCancelled].
     *
     * It's used to identify non-predictive back taps (when this flag is `false`) so that the
     * required [NavigationEventDispatcher.dispatchOnStarted] call can be injected first.
     *
     * @see [NavigationEventProcessor.isPredictiveInProgress]
     */
    private var isPredictiveBackInProgress = false

    /**
     * Tracks if a predictive **forward** gesture is currently in progress from this input.
     *
     * This is set to `true` by [dispatchOnForwardStarted] and reset to `false` by
     * [dispatchOnForwardCompleted] or [dispatchOnForwardCancelled].
     *
     * It's used to identify non-predictive forward taps (when this flag is `false`) so that the
     * required [NavigationEventDispatcher.dispatchOnStarted] call can be injected first.
     *
     * @see [NavigationEventProcessor.isPredictiveInProgress]
     */
    private var isPredictiveForwardInProgress = false

    /** @see [NavigationEventProcessor.addInput] */
    @MainThread
    internal fun doOnAdded(dispatcher: NavigationEventDispatcher) {
        onAdded(dispatcher)
    }

    /**
     * Called after this [NavigationEventInput] is added to [dispatcher]. This can happen when
     * calling [NavigationEventDispatcher.addInput]. A [NavigationEventInput] can only be added to
     * one [NavigationEventDispatcher] at a time.
     *
     * @param dispatcher The [NavigationEventDispatcher] that this input is now added to.
     */
    @MainThread @EmptySuper protected open fun onAdded(dispatcher: NavigationEventDispatcher) {}

    /** @see [NavigationEventProcessor.removeInput] */
    @MainThread
    internal fun doOnRemoved() {
        onRemoved()
    }

    /**
     * Called after this [NavigationEventInput] is removed from a [NavigationEventDispatcher]. This
     * can happen when calling [NavigationEventDispatcher.removeInput] or
     * [NavigationEventDispatcher.dispose] on the containing [NavigationEventDispatcher].
     */
    @MainThread @EmptySuper protected open fun onRemoved() {}

    @MainThread
    internal fun doOnHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        onHasEnabledHandlersChanged(hasEnabledHandlers)
    }

    /**
     * Called when the enabled state of handlers in the connected [NavigationEventDispatcher]
     * changes.
     *
     * This allows the input to enable or disable its own event sourcing. For example, a system back
     * gesture input might only register for gestures when `hasEnabledHandlers` is `true`.
     *
     * The exact set of handlers this reflects depends on the
     * [Priority][NavigationEventDispatcher.Priority] this input was registered with.
     *
     * @param hasEnabledHandlers Whether the connected dispatcher has any enabled handlers matching
     *   this input's priority scope.
     */
    @MainThread
    @EmptySuper
    protected open fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {}

    @MainThread
    internal fun doOnHistoryChanged(history: NavigationEventHistory) {
        onHistoryChanged(history)
    }

    /**
     * Called when the [NavigationEventHistory] state in the connected [NavigationEventDispatcher]
     * changes.
     *
     * @param history The new, immutable snapshot of the navigation history.
     */
    @MainThread @EmptySuper protected open fun onHistoryChanged(history: NavigationEventHistory) {}

    /**
     * Notifies the dispatcher that a [TRANSITIONING_BACK] navigation gesture has **started**.
     *
     * The [NavigationEventDispatcher.transitionState] will become [InProgress].
     *
     * @param event The [NavigationEvent] describing the start of the gesture (e.g., touch
     *   position).
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnBackStarted(event: NavigationEvent) {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        // Don't allow a new gesture to start if one is already in progress.
        if (!isPredictiveBackInProgress) {
            dispatcher.dispatchOnStarted(input = this, direction = TRANSITIONING_BACK, event)

            // This is a predictive event; set the flag before dispatching.
            isPredictiveBackInProgress = true
        }
    }

    /**
     * Notifies the dispatcher that an ongoing [TRANSITIONING_BACK] navigation gesture has
     * **progressed**.
     *
     * The [NavigationEventDispatcher.transitionState] will become [InProgress].
     *
     * @param event The [NavigationEvent] describing the progress of the gesture.
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnBackProgressed(event: NavigationEvent) {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (isPredictiveBackInProgress) {
            dispatcher.dispatchOnProgressed(input = this, direction = TRANSITIONING_BACK, event)
        }
    }

    /**
     * Notifies the dispatcher that the ongoing [TRANSITIONING_BACK] navigation gesture has been
     * **cancelled**.
     *
     * This is a **terminal** event. The [NavigationEventDispatcher.transitionState] will become
     * [Idle] and ready for a new gesture.
     *
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnBackCancelled() {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (!isPredictiveBackInProgress) {
            // This is a non-predictive tap.
            // To satisfy the processor's "Start -> Terminal" state machine, we must
            // inject the non-predictive `dispatchOnStarted()` call first.
            dispatcher.dispatchOnStarted(input = this, direction = TRANSITIONING_BACK, event = null)
        }

        dispatcher.dispatchOnCancelled(input = this, direction = TRANSITIONING_BACK)

        // This is a terminal event; always reset the predictive flag.
        isPredictiveBackInProgress = false
    }

    /**
     * Notifies the dispatcher that the ongoing [TRANSITIONING_BACK] navigation gesture has
     * **completed**.
     *
     * This is a **terminal** event, signaling that the navigation should be finalized (e.g.,
     * popping the back stack). The [NavigationEventDispatcher.transitionState] will become [Idle]
     * and ready for a new gesture.
     *
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnBackCompleted() {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (!isPredictiveBackInProgress) {
            // This is a non-predictive tap.
            // To satisfy the processor's "Start -> Terminal" state machine, we must
            // inject the non-predictive `dispatchOnStarted()` call first.
            dispatcher.dispatchOnStarted(input = this, direction = TRANSITIONING_BACK, event = null)
        }

        dispatcher.dispatchOnCompleted(input = this, direction = TRANSITIONING_BACK)

        // This is a terminal event; always reset the predictive flag.
        isPredictiveBackInProgress = false
    }

    /**
     * Notifies the dispatcher that a [TRANSITIONING_FORWARD] navigation gesture has **started**.
     *
     * The [NavigationEventDispatcher.transitionState] will become [InProgress].
     *
     * @param event The [NavigationEvent] describing the start of the gesture (e.g., touch
     *   position).
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnForwardStarted(event: NavigationEvent) {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        // Don't allow a new gesture to start if one is already in progress.
        if (!isPredictiveForwardInProgress) {
            dispatcher.dispatchOnStarted(input = this, direction = TRANSITIONING_FORWARD, event)

            // This is a predictive event; set the flag before dispatching.
            isPredictiveForwardInProgress = true
        }
    }

    /**
     * Notifies the dispatcher that an ongoing [TRANSITIONING_FORWARD] navigation gesture has
     * **progressed**.
     *
     * The [NavigationEventDispatcher.transitionState] will become [InProgress].
     *
     * @param event The [NavigationEvent] describing the progress of the gesture.
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnForwardProgressed(event: NavigationEvent) {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (isPredictiveForwardInProgress) {
            dispatcher.dispatchOnProgressed(input = this, direction = TRANSITIONING_FORWARD, event)
        }
    }

    /**
     * Notifies the dispatcher that the ongoing [TRANSITIONING_FORWARD] navigation gesture has been
     * **cancelled**.
     *
     * This is a **terminal** event. The [NavigationEventDispatcher.transitionState] will become
     * [Idle] and ready for a new gesture.
     *
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnForwardCancelled() {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (!isPredictiveForwardInProgress) {
            // This is a non-predictive tap.
            // To satisfy the processor's "Start -> Terminal" state machine, we must
            // inject the non-predictive `dispatchOnStarted()` call first.
            dispatcher.dispatchOnStarted(
                input = this,
                direction = TRANSITIONING_FORWARD,
                event = null,
            )
        }

        dispatcher.dispatchOnCancelled(input = this, direction = TRANSITIONING_FORWARD)

        // This is a terminal event; always reset the predictive flag.
        isPredictiveForwardInProgress = false
    }

    /**
     * Notifies the dispatcher that the ongoing [TRANSITIONING_FORWARD] navigation gesture has
     * **completed**.
     *
     * This is a **terminal** event, signaling that the navigation should be finalized (e.g.,
     * popping the back stack). The [NavigationEventDispatcher.transitionState] will become [Idle]
     * and ready for a new gesture.
     *
     * @throws IllegalStateException if this input is not added to a dispatcher.
     * @throws IllegalStateException if this dispatcher is disposed.
     */
    @MainThread
    protected fun dispatchOnForwardCompleted() {
        val dispatcher = checkNotNull(dispatcher) { "This input is not added to any dispatcher." }

        if (!isPredictiveForwardInProgress) {
            // This is a non-predictive tap.
            // To satisfy the processor's "Start -> Terminal" state machine, we must
            // inject the non-predictive `dispatchOnStarted()` call first.
            dispatcher.dispatchOnStarted(
                input = this,
                direction = TRANSITIONING_FORWARD,
                event = null,
            )
        }

        dispatcher.dispatchOnCompleted(input = this, direction = TRANSITIONING_FORWARD)

        // This is a terminal event; always reset the predictive flag.
        isPredictiveForwardInProgress = false
    }
}
