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

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

// @OptIn(ExperimentalWasmJsInterop::class)
@VisibleForTesting
internal interface BrowserHistory {
    val state: JsAny?

    fun push(data: JsAny?, url: String?)

    fun replace(data: JsAny?, url: String?)

    suspend fun go(delta: Int)
}

// @OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserHistoryImpl(private val window: Window) : BrowserHistory {
    override val state: JsAny?
        get() = window.history.state

    override fun push(data: JsAny?, url: String?) {
        window.history.pushState(data, "", url)
    }

    override fun replace(data: JsAny?, url: String?) {
        window.history.replaceState(data, "", url)
    }

    override suspend fun go(delta: Int) {
        if (delta == 0) return // Ignore "refresh" for now.
        window.history.go(delta)
        // TODO: Will get stuck if we go out of range. For example, if the history is [a, b*, c],
        // and we call `history.go(2)`, we'll be stuck here as the call will be ignored and we
        // won't receive a popstate event.
        window.createPopStateFlow().first()
    }
}

private fun Window.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    window.addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { window.removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}
