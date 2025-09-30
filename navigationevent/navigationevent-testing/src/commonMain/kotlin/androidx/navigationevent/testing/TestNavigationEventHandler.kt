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
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

/**
 * Creates an instance of [TestNavigationEventHandler] without requiring an explicit generic type.
 *
 * This function is a convenience wrapper around the [TestNavigationEventHandler] constructor that
 * defaults its info type to `*`. Use this in tests where the specific type of [NavigationEventInfo]
 * is not relevant.
 */
public fun TestNavigationEventHandler(
    // ---- Forward Events ----
    isForwardEnabled: Boolean = true,
    onForwardStarted: TestNavigationEventHandler<*>.(event: NavigationEvent) -> Unit = {},
    onForwardProgressed: TestNavigationEventHandler<*>.(event: NavigationEvent) -> Unit = {},
    onForwardCancelled: TestNavigationEventHandler<*>.() -> Unit = {},
    onForwardCompleted: TestNavigationEventHandler<*>.() -> Unit = {},
    // ---- Back Events ----
    isBackEnabled: Boolean = true,
    onBackStarted: TestNavigationEventHandler<*>.(event: NavigationEvent) -> Unit = {},
    onBackProgressed: TestNavigationEventHandler<*>.(event: NavigationEvent) -> Unit = {},
    onBackCancelled: TestNavigationEventHandler<*>.() -> Unit = {},
    onBackCompleted: TestNavigationEventHandler<*>.() -> Unit = {},
): TestNavigationEventHandler<*> {
    return TestNavigationEventHandler(
        currentInfo = NavigationEventInfo.None,
        backInfo = emptyList(),
        forwardInfo = emptyList(),
        // ---- Back Events ----
        isBackEnabled = isBackEnabled,
        onBackStarted = onBackStarted,
        onBackProgressed = onBackProgressed,
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
        // ---- Forward Events ----
        isForwardEnabled = isForwardEnabled,
        onForwardStarted = onForwardStarted,
        onForwardProgressed = onForwardProgressed,
        onForwardCancelled = onForwardCancelled,
        onForwardCompleted = onForwardCompleted,
    )
}

/**
 * A test implementation of [NavigationEventHandler] that records received events and invocation
 * counts.
 *
 * This class is primarily used in tests to verify that specific navigation event callbacks are
 * triggered as expected. It captures the [NavigationEvent] objects and counts how many times each
 * callback is fired.
 *
 * @param T The [NavigationEventInfo] type this callback handles.
 * @param currentInfo Initial current navigation info.
 * @param backInfo Initial back stack info list. Defaults to empty.
 * @param forwardInfo Initial forward stack info list. Defaults to empty.
 * @param isForwardEnabled Determines if forward callbacks should process events. Defaults to
 *   `true`.
 * @param onForwardStarted Optional lambda to execute when `onForwardStarted` is called.
 * @param onForwardProgressed Optional lambda to execute when `onForwardProgressed` is called.
 * @param onForwardCancelled Optional lambda to execute when `onForwardCancelled` is called.
 * @param onForwardCompleted Optional lambda to execute when `onForwardCompleted` is called.
 * @param isBackEnabled Determines if back callbacks should process events. Defaults to `true`.
 * @param onBackStarted Optional lambda to execute when `onBackStarted` is called.
 * @param onBackProgressed Optional lambda to execute when `onBackProgressed` is called.
 * @param onBackCancelled Optional lambda to execute when `onBackCancelled` is called.
 * @param onBackCompleted Optional lambda to execute when `onBackCompleted` is called.
 */
public class TestNavigationEventHandler<T : NavigationEventInfo>(
    currentInfo: T,
    backInfo: List<T> = emptyList(),
    forwardInfo: List<T> = emptyList(),
    // ---- Forward Events ----
    isForwardEnabled: Boolean = true,
    private val onForwardStarted: TestNavigationEventHandler<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onForwardProgressed:
        TestNavigationEventHandler<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onForwardCancelled: TestNavigationEventHandler<T>.() -> Unit = {},
    private val onForwardCompleted: TestNavigationEventHandler<T>.() -> Unit = {},
    // ---- Back Events ----
    isBackEnabled: Boolean = true,
    private val onBackStarted: TestNavigationEventHandler<T>.(event: NavigationEvent) -> Unit = {},
    private val onBackProgressed: TestNavigationEventHandler<T>.(event: NavigationEvent) -> Unit =
        {},
    private val onBackCancelled: TestNavigationEventHandler<T>.() -> Unit = {},
    private val onBackCompleted: TestNavigationEventHandler<T>.() -> Unit = {},
) : NavigationEventHandler<T>(initialInfo = currentInfo, isBackEnabled, isForwardEnabled) {

    init {
        setInfo(currentInfo = currentInfo, backInfo = backInfo, forwardInfo = forwardInfo)
    }

    // ---- Back Events ----
    private val _onBackStartedEvents = mutableListOf<NavigationEvent>()
    /** All events received by [onBackStarted]. */
    public val onBackStartedEvents: List<NavigationEvent>
        get() = _onBackStartedEvents.toList()

    /** Number of times [onBackStarted] has been invoked. */
    public val onBackStartedInvocations: Int
        get() = _onBackStartedEvents.size

    private val _onBackProgressedEvents = mutableListOf<NavigationEvent>()
    /** All events received by [onBackProgressed]. */
    public val onBackProgressedEvents: List<NavigationEvent>
        get() = _onBackProgressedEvents.toList()

    /** Number of times [onBackProgressed] has been invoked. */
    public val onBackProgressedInvocations: Int
        get() = _onBackProgressedEvents.size

    /** Number of times [onBackCompleted] has been invoked. */
    public var onBackCompletedInvocations: Int = 0
        private set

    /** Number of times [onBackCancelled] has been invoked. */
    public var onBackCancelledInvocations: Int = 0
        private set

    override fun onBackStarted(event: NavigationEvent) {
        _onBackStartedEvents += event
        onBackStarted(this, event)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        _onBackProgressedEvents += event
        onBackProgressed(this, event)
    }

    override fun onBackCompleted() {
        onBackCompletedInvocations++
        onBackCompleted(this)
    }

    override fun onBackCancelled() {
        onBackCancelledInvocations++
        onBackCancelled(this)
    }

    // ---- Forward Events ----
    private val _onForwardStartedEvents = mutableListOf<NavigationEvent>()
    /** All events received by [onForwardStarted]. */
    public val onForwardStartedEvents: List<NavigationEvent>
        get() = _onForwardStartedEvents.toList()

    /** Number of times [onForwardStarted] has been invoked. */
    public val onForwardStartedInvocations: Int
        get() = _onForwardStartedEvents.size

    private val _onForwardProgressedEvents = mutableListOf<NavigationEvent>()
    /** All events received by [onForwardProgressed]. */
    public val onForwardProgressedEvents: List<NavigationEvent>
        get() = _onForwardProgressedEvents.toList()

    /** Number of times [onForwardProgressed] has been invoked. */
    public val onForwardProgressedInvocations: Int
        get() = _onForwardProgressedEvents.size

    /** Number of times [onForwardCompleted] has been invoked. */
    public var onForwardCompletedInvocations: Int = 0
        private set

    /** Number of times [onForwardCancelled] has been invoked. */
    public var onForwardCancelledInvocations: Int = 0
        private set

    override fun onForwardStarted(event: NavigationEvent) {
        _onForwardStartedEvents += event
        onForwardStarted(this, event)
    }

    override fun onForwardProgressed(event: NavigationEvent) {
        _onForwardProgressedEvents += event
        onForwardProgressed(this, event)
    }

    override fun onForwardCompleted() {
        onForwardCompletedInvocations++
        onForwardCompleted(this)
    }

    override fun onForwardCancelled() {
        onForwardCancelledInvocations++
        onForwardCancelled(this)
    }
}
