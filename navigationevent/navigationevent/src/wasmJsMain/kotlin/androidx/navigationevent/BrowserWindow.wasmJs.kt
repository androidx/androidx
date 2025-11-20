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
import org.w3c.dom.events.Event

@VisibleForTesting
public interface BrowserWindow {
    public val history: BrowserHistory

    public fun addEventListener(type: String, callback: (Event) -> Unit)

    public fun removeEventListener(type: String, callback: (Event) -> Unit)
}

// @OptIn(ExperimentalWasmJsInterop::class)
@VisibleForTesting
public interface BrowserHistory {
    public val state: JsAny?

    public fun push(data: JsAny?, url: String?)

    public fun replace(data: JsAny?, url: String?)

    public suspend fun go(delta: Int)
}
