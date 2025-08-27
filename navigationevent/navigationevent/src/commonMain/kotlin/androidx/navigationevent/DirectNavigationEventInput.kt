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
    /** Send "start" event to the connected dispatcher. */
    @MainThread
    public fun start(event: NavigationEvent) {
        dispatchOnStarted(event)
    }

    /** Send "progress" event to the connected dispatcher. */
    @MainThread
    public fun progress(event: NavigationEvent) {
        dispatchOnProgressed(event)
    }

    /** Send "complete" event to the connected dispatcher. */
    @MainThread
    public fun complete() {
        dispatchOnCompleted()
    }

    /** Send "cancel" event to the connected dispatcher. */
    @MainThread
    public fun cancel() {
        dispatchOnCancelled()
    }
}
