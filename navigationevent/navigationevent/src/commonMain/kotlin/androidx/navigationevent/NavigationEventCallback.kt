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

/**
 * Callback for handling [NavigationEvent]s.
 *
 * This class maintains its own [isEnabled] state and will only receive callbacks when enabled.
 *
 * @param isEnabled The enabled state for this callback.
 * @param isPassThrough Whether this callback should consume the events from
 *   [NavigationEventDispatcher] or allow it to continue.
 * @see NavigationEventDispatcher
 */
public abstract class NavigationEventCallback(
    isEnabled: Boolean,
    /**
     * Whether this callback should consume the events from [NavigationEventDispatcher] or allow it
     * to continue.
     */
    public val isPassThrough: Boolean = false,
) {

    public var isEnabled: Boolean = isEnabled
        set(value) {
            field = value
            dispatcher?.updateEnabledCallbacks()
        }

    internal var dispatcher: NavigationEventDispatcher? = null

    public fun remove() {
        dispatcher?.removeCallback(this)
    }

    /** Callback for handling [NavigationEventDispatcher.dispatchOnStarted]. */
    public open fun onEventStarted(event: NavigationEvent) {}

    /** Callback for handling [NavigationEventDispatcher.dispatchOnProgressed]. */
    public open fun onEventProgressed(event: NavigationEvent) {}

    /** Callback for handling [NavigationEventDispatcher.dispatchOnCompleted]. */
    public open fun onEventCompleted() {}

    /** Callback for handling [NavigationEventDispatcher.dispatchOnCancelled]. */
    public open fun onEventCancelled() {}
}
