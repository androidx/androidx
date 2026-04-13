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

// ExperimentalWasmJsInterop is only available in Kotlin 2.2 and newer versions.
@file:Suppress("OPT_IN_USAGE")

package androidx.navigationevent

import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window

/**
 * A [NavigationEventInput] that translates browser history navigation events (popstate) into
 * [NavigationEventDispatcher] events.
 *
 * This implementation uses the browser's History API to synchronize the application's internal
 * navigation state with the browser's history stack.
 */
internal class BrowserInput(
    private val window: WindowCompat,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : NavigationEventInput() {

    /** Creates a [BrowserInput] for the given [window]. */
    constructor(window: Window) : this(WindowCompat(window))

    private var coroutineScope: CoroutineScope? = null

    /**
     * Controls whether to process [onPopState] from the [WindowCompat].
     *
     * This is used to suppress the 'echo' effect, where programmatic history changes (like
     * [WindowCompat.go]) trigger a [WindowCompat.TYPE_POP_STATE] event that should not be
     * re-processed as a user-initiated navigation.
     */
    private var isOnPopStateEnabled = true

    /**
     * Controls whether to process [onHistoryChanged] from the [NavigationEventDispatcher].
     *
     * This is used to prevent redundant history synchronization requests while we are manually
     * winding or unwinding the state in response to a multistep browser navigation.
     */
    private var isOnHistoryChangedEnabled = true

    /**
     * The current index in the browser's history stack that matches our application's state. This
     * corresponds to the integer value stored in the browser's history state object.
     */
    private var browserIndex = 0

    /**
     * The number of valid navigation entries currently managed by the [NavigationEventDispatcher].
     * Any browser history entry with an index equal to or greater than this is considered
     * "invalid".
     */
    private var logicalHistorySize = 1

    /**
     * The total number of entries we have pushed to the browser's history stack. This helps us
     * determine if we need to call [WindowCompat.pushState] or if we can simply use
     * [WindowCompat.go].
     */
    private var pushedHistorySize = 1

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        coroutineScope = CoroutineScope(Job() + coroutineDispatcher)

        // Starts the main synchronization loop that converts native browser navigation events
        // (e.g., the back button) into application-level navigation events.
        coroutineScope!!.launch { window.popStateEvents.collect { onPopState(it) } }

        // Attempt to recover the index from the browser's history state (e.g., after a page
        // refresh). If no state exists, seed it with 0.
        val currentState = window.state
        val recoveredIndex = (currentState as? JsNumber)?.toInt()

        if (recoveredIndex != null) {
            browserIndex = recoveredIndex
            logicalHistorySize = recoveredIndex + 1
            pushedHistorySize = recoveredIndex + 1
        } else {
            // TODO(mgalhardo): Blindly replacing the state with a primitive number overwrites
            //  any existing application state stored in history.state. We should instead use
            //  a wrapper JS object and merge our navigation index property (e.g., { __index: 0 })
            //  with the existing state to preserve app data.
            window.replaceState(0.toJsNumber())
        }
    }

    /**
     * Handles the browser's `popstate` event, converting it into [dispatchOnBackCompleted] or
     * [dispatchOnForwardCompleted] calls.
     */
    private suspend fun onPopState(popStateEvent: PopStateEvent) {
        if (!isOnPopStateEnabled) return

        val state = popStateEvent.state ?: return
        val newIndex = (state as? JsNumber)?.toInt() ?: return
        if (newIndex == browserIndex) return

        // If the browser attempts to navigate to a history state we no longer track (e.g.,
        // after a manual state replacement), force the browser to revert the native navigation
        // to stay in sync with our internal history stack.
        if (newIndex !in 0 until logicalHistorySize) {
            isOnPopStateEnabled = false
            window.go(browserIndex - newIndex)
            isOnPopStateEnabled = true
            return
        }

        // A user can jump multiple pages at once via the browser's history dropdown. We must
        // unwind/wind our internal state sequentially for each step. We suppress callbacks on
        // intermediate steps to prevent jarring, unnecessary UI churn.
        val steps = abs(newIndex - browserIndex)
        val isForward = newIndex > browserIndex

        isOnHistoryChangedEnabled = false
        repeat(steps) {
            if (isForward) {
                dispatchOnForwardCompleted()
            } else {
                dispatchOnBackCompleted()
            }
        }
        isOnHistoryChangedEnabled = true

        browserIndex = newIndex
    }

    override fun onRemoved() {
        coroutineScope?.cancel()
        coroutineScope = null
        browserIndex = 0
        logicalHistorySize = 1
        pushedHistorySize = 1
        isOnPopStateEnabled = true
        isOnHistoryChangedEnabled = true
    }

    override fun onHistoryChanged(history: NavigationEventHistory) {
        if (!isOnHistoryChangedEnabled) return
        if (history.currentIndex < 0) return
        // TODO: We may get None first when disposing the previous Composable destination.
        if (history.mergedHistory[history.currentIndex] == NavigationEventInfo.None) return

        coroutineScope!!.launch {
            isOnPopStateEnabled = false
            updateBrowserHistory(history)
            isOnPopStateEnabled = true
        }
    }

    /**
     * Synchronizes the browser's history stack with the provided [newHistory].
     *
     * If the new history is larger than the current pushed history, it will push new states to
     * increase the stack size before navigating to the target index.
     */
    private suspend fun updateBrowserHistory(newHistory: NavigationEventHistory) {
        val newSize = newHistory.mergedHistory.size
        val newIndex = newHistory.currentIndex

        if (pushedHistorySize >= newSize) {
            window.go(newIndex - browserIndex)
        } else {
            // Browser History API restricts direct stack manipulation. To expand history
            // capacity, we must physically move to the end of the current stack, push new
            // placeholder states to increase the length, and then rewind to the target index.
            // This sequence triggers multiple native PopStateEvents.

            window.go(pushedHistorySize - 1 - browserIndex)

            for (i in pushedHistorySize until newSize) {
                window.pushState(i.toJsNumber())
            }

            window.go(newIndex - (newSize - 1))

            pushedHistorySize = newSize
        }

        browserIndex = newIndex
        logicalHistorySize = newSize
    }
}
