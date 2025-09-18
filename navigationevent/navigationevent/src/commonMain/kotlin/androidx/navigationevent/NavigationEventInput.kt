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

import androidx.annotation.EmptySuper
import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventDirection.Companion.Back
import androidx.navigationevent.NavigationEventDirection.Companion.Forward

/** A class that can send events to a [NavigationEventDispatcher]. */
public abstract class NavigationEventInput() {

    /** The [NavigationEventDispatcher] that this input is connected to. */
    internal var dispatcher: NavigationEventDispatcher? = null

    @MainThread
    internal fun doOnAdded(dispatcher: NavigationEventDispatcher) {
        onAdded(dispatcher)
    }

    @MainThread
    internal fun doOnRemoved() {
        onRemoved()
    }

    /**
     * Called after this [NavigationEventInput] is added to [dispatcher]. This can happen when
     * calling [NavigationEventDispatcher.addInput]. A [NavigationEventInput] can only be added to
     * one [NavigationEventDispatcher] at a time.
     *
     * @param dispatcher The [NavigationEventDispatcher] that this input is now added to.
     */
    @MainThread @EmptySuper protected open fun onAdded(dispatcher: NavigationEventDispatcher) {}

    /**
     * Called after this [NavigationEventInput] is removed from a [NavigationEventDispatcher]. This
     * can happen when calling [NavigationEventDispatcher.removeInput] or
     * [NavigationEventDispatcher.dispose] on the containing [NavigationEventDispatcher].
     */
    @MainThread @EmptySuper protected open fun onRemoved() {}

    @MainThread
    internal fun doOnHasEnabledHandlerChanged(hasEnabledHandler: Boolean) {
        onHasEnabledHandlerChanged(hasEnabledHandler)
    }

    /**
     * Handler that will be notified when the connected dispatcher's `hasEnabledHandlers` changes.
     *
     * @param hasEnabledHandler Whether the connected dispatcher has any enabled handlers.
     */
    @MainThread
    @EmptySuper
    protected open fun onHasEnabledHandlerChanged(hasEnabledHandler: Boolean) {}

    /**
     * Dispatch a back started event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    protected fun dispatchOnBackStarted(event: NavigationEvent) {
        dispatcher?.dispatchOnStarted(input = this, direction = Back, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /**
     * Dispatch a back progressed event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    protected fun dispatchOnBackProgressed(event: NavigationEvent) {
        dispatcher?.dispatchOnProgressed(input = this, direction = Back, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Dispatch a back cancelled event with the connected dispatcher. */
    @MainThread
    protected fun dispatchOnBackCancelled() {
        dispatcher?.dispatchOnCancelled(input = this, direction = Back)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Dispatch a back completed event with the connected dispatcher. */
    @MainThread
    protected fun dispatchOnBackCompleted() {
        dispatcher?.dispatchOnCompleted(input = this, direction = Back)
            ?: error("This input is not added to any dispatcher.")
    }

    /**
     * Dispatch a forward started event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    protected fun dispatchOnForwardStarted(event: NavigationEvent) {
        dispatcher?.dispatchOnStarted(input = this, direction = Forward, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /**
     * Dispatch a forward progressed event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    protected fun dispatchOnForwardProgressed(event: NavigationEvent) {
        dispatcher?.dispatchOnProgressed(input = this, direction = Forward, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Dispatch a forward cancelled event with the connected dispatcher. */
    @MainThread
    protected fun dispatchOnForwardCancelled() {
        dispatcher?.dispatchOnCancelled(input = this, direction = Forward)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Dispatch a forward completed event with the connected dispatcher. */
    @MainThread
    protected fun dispatchOnForwardCompleted() {
        dispatcher?.dispatchOnCompleted(input = this, direction = Forward)
            ?: error("This input is not added to any dispatcher.")
    }
}
