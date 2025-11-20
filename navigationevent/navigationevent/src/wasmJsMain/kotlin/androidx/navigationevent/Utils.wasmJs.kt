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

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.PopStateEvent
import org.w3c.dom.Window
import org.w3c.dom.events.Event

internal fun Window.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    window.addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { window.removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}

internal fun BrowserWindow.createPopStateFlow() = callbackFlow {
    val callback: (Event) -> Unit = { event: Event -> trySend(event as PopStateEvent) }
    addEventListener(BrowserInput.TYPE_POPSTATE, callback)
    awaitClose { removeEventListener(BrowserInput.TYPE_POPSTATE, callback) }
}
