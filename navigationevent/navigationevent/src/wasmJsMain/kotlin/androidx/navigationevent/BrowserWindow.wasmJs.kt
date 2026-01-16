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
import org.w3c.dom.Window
import org.w3c.dom.events.Event

@VisibleForTesting
internal interface BrowserWindow {
    val history: BrowserHistory

    fun addEventListener(type: String, callback: (Event) -> Unit)

    fun removeEventListener(type: String, callback: (Event) -> Unit)
}

internal class BrowserWindowImpl(private val window: Window) : BrowserWindow {
    override val history: BrowserHistory = BrowserHistoryImpl(window)

    override fun addEventListener(type: String, callback: (Event) -> Unit) {
        window.addEventListener(type, callback)
    }

    override fun removeEventListener(type: String, callback: (Event) -> Unit) {
        window.removeEventListener(type, callback)
    }
}
