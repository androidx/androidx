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
 * A test implementation of [NavigationEventCallback] that records received events and invocation
 * counts.
 *
 * This class is primarily used in tests to verify that specific navigation event callbacks are
 * triggered as expected. It captures the [NavigationEvent] objects and counts how many times each
 * callback is fired.
 *
 * @param isEnabled Determines if the callback should process events. Defaults to `true`.
 * @param isPassThrough If `true`, events are passed to the next callback in the chain. Defaults to
 *   `false`.
 * @param onEventStarted An optional lambda to execute when `onEventStarted` is called.
 * @param onEventProgressed An optional lambda to execute when `onEventProgressed` is called.
 * @param onEventCancelled An optional lambda to execute when `onEventCancelled` is called.
 * @param onEventCompleted An optional lambda to execute when `onEventCompleted` is called.
 */
internal class TestNavigationEventCallback(
    isEnabled: Boolean = true,
    isPassThrough: Boolean = false,
    private val onEventStarted: TestNavigationEventCallback.(event: NavigationEvent) -> Unit = {},
    private val onEventProgressed: TestNavigationEventCallback.(event: NavigationEvent) -> Unit =
        {},
    private val onEventCancelled: TestNavigationEventCallback.() -> Unit = {},
    private val onEventCompleted: TestNavigationEventCallback.() -> Unit = {},
) : NavigationEventCallback(isEnabled, isPassThrough) {

    private val _startedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onEventStarted] callback. */
    val startedEvents: List<NavigationEvent>
        get() = _startedEvents.toList()

    /** The number of times [onEventStarted] has been invoked. */
    val startedInvocations: Int
        get() = _startedEvents.size

    private val _progressedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onEventProgressed] callback. */
    val progressedEvents: List<NavigationEvent>
        get() = _progressedEvents.toList()

    /** The number of times [progressedInvocations] has been invoked. */
    val progressedInvocations: Int
        get() = _progressedEvents.size

    /** The number of times [completedInvocations] has been invoked. */
    var completedInvocations: Int = 0
        private set

    /** The number of times [cancelledInvocations] has been invoked. */
    var cancelledInvocations: Int = 0
        private set

    override fun onEventStarted(event: NavigationEvent) {
        _startedEvents += event
        onEventStarted.invoke(this, event)
    }

    override fun onEventProgressed(event: NavigationEvent) {
        _progressedEvents += event
        onEventProgressed.invoke(this, event)
    }

    override fun onEventCompleted() {
        completedInvocations++
        onEventCompleted(this)
    }

    override fun onEventCancelled() {
        cancelledInvocations++
        onEventCancelled(this)
    }
}
