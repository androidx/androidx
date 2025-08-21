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

package androidx.navigationevent.testing

import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventInfo

/**
 * Creates an instance of [TestNavigationEventCallback] without requiring an explicit generic type.
 *
 * This function is a convenience wrapper around the [TestNavigationEventCallback] constructor that
 * defaults its info type to `*`. Use this in tests where the specific type of [NavigationEventInfo]
 * is not relevant.
 */
public fun TestNavigationEventCallback(
    isEnabled: Boolean = true,
    onEventStarted: TestNavigationEventCallback<*>.(event: NavigationEvent) -> Unit = {},
    onEventProgressed: TestNavigationEventCallback<*>.(event: NavigationEvent) -> Unit = {},
    onEventCancelled: TestNavigationEventCallback<*>.() -> Unit = {},
    onEventCompleted: TestNavigationEventCallback<*>.() -> Unit = {},
): TestNavigationEventCallback<*> {
    return TestNavigationEventCallback(
        currentInfo = NavigationEventInfo.NotProvided,
        previousInfo = null,
        isEnabled = isEnabled,
        onEventStarted = onEventStarted,
        onEventProgressed = onEventProgressed,
        onEventCancelled = onEventCancelled,
        onEventCompleted = onEventCompleted,
    )
}

/**
 * A test implementation of [NavigationEventCallback] that records received events and invocation
 * counts.
 *
 * This class is primarily used in tests to verify that specific navigation event callbacks are
 * triggered as expected. It captures the [NavigationEvent] objects and counts how many times each
 * callback is fired.
 *
 * @param T The type of [NavigationEventInfo] this callback handles.
 * @param currentInfo The initial **current** navigation information for the callback.
 * @param previousInfo The initial **previous** navigation information. Defaults to `null`.
 * @param isEnabled Determines if the callback should process events. Defaults to `true`.
 * @param onEventStarted An optional lambda to execute when `onEventStarted` is called.
 * @param onEventProgressed An optional lambda to execute when `onEventProgressed` is called.
 * @param onEventCancelled An optional lambda to execute when `onEventCancelled` is called.
 * @param onEventCompleted An optional lambda to execute when `onEventCompleted` is called.
 */
public class TestNavigationEventCallback<T : NavigationEventInfo>(
    currentInfo: T,
    previousInfo: T? = null,
    isEnabled: Boolean = true,
    private val onEventStarted: TestNavigationEventCallback<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onEventProgressed: TestNavigationEventCallback<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onEventCancelled: TestNavigationEventCallback<T>.() -> Unit = {},
    private val onEventCompleted: TestNavigationEventCallback<T>.() -> Unit = {},
) : NavigationEventCallback<T>(isEnabled) {

    init {
        setInfo(currentInfo = currentInfo, previousInfo = previousInfo)
    }

    private val _startedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onEventStarted] callback. */
    public val startedEvents: List<NavigationEvent>
        get() = _startedEvents.toList()

    /** The number of times [onEventStarted] has been invoked. */
    public val startedInvocations: Int
        get() = _startedEvents.size

    private val _progressedEvents = mutableListOf<NavigationEvent>()

    /** A [List] of all events received by the [onEventProgressed] callback. */
    public val progressedEvents: List<NavigationEvent>
        get() = _progressedEvents.toList()

    /** The number of times [onEventProgressed] has been invoked. */
    public val progressedInvocations: Int
        get() = _progressedEvents.size

    /** The number of times [onEventCompleted] has been invoked. */
    public var completedInvocations: Int = 0
        private set

    /** The number of times [onEventCancelled] has been invoked. */
    public var cancelledInvocations: Int = 0
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
