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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

// @OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserInput(
    private val browserWindow: BrowserWindow,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : NavigationEventInput() {

    private var coroutineScope: CoroutineScope? = null

    internal companion object {
        const val TYPE_POPSTATE = "popstate"
    }

    private val currentHistory: SessionHistory = SessionHistory()

    private var processPopState = true

    private var processHistoryChange = true

    private val browserHistory = browserWindow.history

    public constructor(window: Window) : this(BrowserWindowImpl(window))

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        // Only start listening to popstate events after the input is connected to a dispatcher.
        val scope = CoroutineScope(Job() + coroutineDispatcher)
        scope.launch { browserWindow.createPopStateFlow().collect(::onPopState) }
        coroutineScope = scope

        // Initialize the browser history to [entries from other apps or instances, ... , 0*].
        browserHistory.replace(0.toJsNumber(), null)
    }

    override fun onRemoved() {
        coroutineScope?.cancel()
        currentHistory.reset()
        processPopState = true
        processHistoryChange = true
    }

    private fun onPopState(popStateEvent: PopStateEvent) {
        if (!processPopState) {
            return
        }
        val state = popStateEvent.state ?: return
        val newIndex = (state as? JsNumber)?.toInt() ?: return
        if (
            newIndex != currentHistory.index &&
                (newIndex < 0 || newIndex >= currentHistory.actualSize)
        ) {
            // User goes to an invalid entry, so we move them back.
            coroutineScope!!.launch {
                disableOnPopStateCallback { browserHistory.go(newIndex, currentHistory.index) }
            }
        } else {
            if (newIndex < currentHistory.index) {
                // Trigger one or more dispatchOnBackCompleted. Only process onHistoryChanged
                // on the last one.
                val timesToGoBack = currentHistory.index - newIndex
                disableHistoryUpdateCallback {
                    repeat(timesToGoBack - 1) { dispatchOnBackCompleted() }
                }
                dispatchOnBackCompleted()
            } else if (newIndex > currentHistory.index) {
                // Trigger one or more dispatchOnForwardCompleted. Only process onHistoryChanged
                // on the last one.
                val timesToGoForward = newIndex - currentHistory.index
                disableHistoryUpdateCallback {
                    repeat(timesToGoForward - 1) { dispatchOnForwardCompleted() }
                }
                dispatchOnForwardCompleted()
            }
            currentHistory.index = newIndex
        }
    }

    override fun onHistoryChanged(history: NavigationEventHistory) {
        if (!processHistoryChange) {
            return
        }
        // We may get None first when disposing the previous Composable destination.
        if (
            history.currentIndex < 0 ||
                history.mergedHistory[history.currentIndex] == NavigationEventInfo.None
        ) {
            return
        }

        coroutineScope!!.launch { disableOnPopStateCallback { updateBrowserHistory(history) } }
    }

    private suspend fun updateBrowserHistory(newHistory: NavigationEventHistory) {
        if (currentHistory.availableSize >= newHistory.mergedHistory.size) {
            // We have enough entries already. Go to the new currentIndex directly.
            browserHistory.go(currentHistory.index, newHistory.currentIndex)
        } else { // newHistory.entries.size > oldHistory.entries.size
            // We don't have enough entries, so we start pushing at the end.

            // Move to the last entry
            browserHistory.go(currentHistory.index, currentHistory.availableSize - 1)

            var index = currentHistory.availableSize
            while (index < newHistory.mergedHistory.size) {
                browserHistory.push(index.toJsNumber(), null)
                index++
            }

            // Go back to currentIndex.
            browserHistory.go(newHistory.mergedHistory.size - 1, newHistory.currentIndex)

            currentHistory.availableSize = newHistory.mergedHistory.size
        }
        currentHistory.index = newHistory.currentIndex
        currentHistory.actualSize = newHistory.mergedHistory.size
    }

    private inline fun disableOnPopStateCallback(content: () -> Unit) {
        processPopState = false
        content()
        processPopState = true
    }

    private inline fun disableHistoryUpdateCallback(content: () -> Unit) {
        processHistoryChange = false
        content()
        processHistoryChange = true
    }

    // `SessionHistory(1, 2, 3)` means a browser history like [0, 1*#, 2]:
    // We have three entries in the browser history, two entries in the
    // NavigationEventHistory (denoted by #), and the current index is one (denoted by *).
    private class SessionHistory(
        var index: Int = 0,
        var actualSize: Int = 1,
        var availableSize: Int = 1,
    ) {
        fun reset() {
            index = 0
            actualSize = 1
            availableSize = 1
        }

        override fun toString(): String {
            val result = buildString {
                append("[")
                for (i in 0 until availableSize) {
                    append(i)
                    if (i == index) {
                        append("*")
                    }
                    if (i == actualSize - 1) {
                        append("#")
                    }
                    if (i < availableSize - 1) {
                        append(", ")
                    }
                }
                append("]")
            }
            return result
        }
    }

    private suspend fun BrowserHistory.go(source: Int, destination: Int) {
        val delta = destination - source
        if (delta != 0) {
            go(delta)
        }
    }
}

private fun BrowserWindow.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}
