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

/**
 * An input that can send events to a [NavigationEventDispatcher]. Instead of subclassing
 * [NavigationEventInput], users can create instances of this class and use it directly.
 */
public class DirectNavigationEventInput() : NavigationEventInput() {
    /**
     * Dispatch a back started event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    public fun backStarted(event: NavigationEvent) {
        dispatchOnBackStarted(event)
    }

    /**
     * Dispatch a back progressed event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    public fun backProgressed(event: NavigationEvent) {
        dispatchOnBackProgressed(event)
    }

    /** Dispatch a back cancelled event with the connected dispatcher. */
    @MainThread
    public fun backCancelled() {
        dispatchOnBackCancelled()
    }

    /** Dispatch a back completed event with the connected dispatcher. */
    @MainThread
    public fun backCompleted() {
        dispatchOnBackCompleted()
    }

    /**
     * Dispatch a forward started event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    public fun forwardStarted(event: NavigationEvent) {
        dispatchOnForwardStarted(event)
    }

    /**
     * Dispatch a forward progressed event with the connected dispatcher.
     *
     * @param event The [NavigationEvent] to dispatch.
     */
    @MainThread
    public fun forwardProgressed(event: NavigationEvent) {
        dispatchOnForwardProgressed(event)
    }

    /** Dispatch a forward cancelled event with the connected dispatcher. */
    @MainThread
    public fun forwardCancelled() {
        dispatchOnForwardCancelled()
    }

    /** Dispatch a forward completed event with the connected dispatcher. */
    @MainThread
    public fun forwardCompleted() {
        dispatchOnForwardCompleted()
    }
}
