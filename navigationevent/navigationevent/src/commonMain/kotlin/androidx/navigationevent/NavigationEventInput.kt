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
import androidx.navigationevent.NavigationEventDirection.Companion.Backward

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
    internal fun doOnHasEnabledCallbacksChanged(hasEnabledCallbacks: Boolean) {
        onHasEnabledCallbacksChanged(hasEnabledCallbacks)
    }

    /**
     * Callback that will be notified when the connected dispatcher's `hasEnabledCallbacks` changes.
     *
     * @param hasEnabledCallbacks Whether the connected dispatcher has any enabled callbacks.
     */
    @MainThread
    @EmptySuper
    protected open fun onHasEnabledCallbacksChanged(hasEnabledCallbacks: Boolean) {}

    /**
     * Call `dispatchOnStarted` on the connected dispatcher.
     *
     * @param event The event to dispatch.
     */
    @MainThread
    protected fun dispatchOnStarted(event: NavigationEvent) {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher?.dispatchOnStarted(input = this, direction = Backward, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /**
     * Call `dispatchOnProgressed` on the connected dispatcher.
     *
     * @param event The event to dispatch.
     */
    @MainThread
    protected fun dispatchOnProgressed(event: NavigationEvent) {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher?.dispatchOnProgressed(input = this, direction = Backward, event)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Call `dispatchOnCancelled` on the connected dispatcher. */
    @MainThread
    protected fun dispatchOnCancelled() {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher?.dispatchOnCancelled(input = this, direction = Backward)
            ?: error("This input is not added to any dispatcher.")
    }

    /** Call `dispatchOnCompleted` on the connected dispatcher. */
    @MainThread
    protected fun dispatchOnCompleted() {
        // TODO(kuanyingchou): Accept a direction parameter instead of hardcoding `Backward`.
        dispatcher?.dispatchOnCompleted(input = this, direction = Backward)
            ?: error("This input is not added to any dispatcher.")
    }
}
