/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalWasmJsInterop::class)

package androidx.datastore.core.okio

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import org.w3c.dom.events.Event

// https://developer.mozilla.org/en-US/docs/Web/API/BroadcastChannel
internal external class BroadcastChannel(name: String) : JsAny {
    fun postMessage(message: JsAny?)

    fun close()

    // Must be exactly 'onmessage' to bind correctly in JS
    var onmessage: ((Event) -> Unit)?
}

internal external interface FileSystemHandleOptions : JsAny {
    var create: Boolean
}

internal fun createCreateOptions(): FileSystemHandleOptions = js("({create: true})")

internal external interface LockManagerOptions : JsAny {
    var mode: String // "exclusive" or "shared"
    var ifAvailable: Boolean
    var steal: Boolean
}

internal external interface Lock : JsAny {
    val name: String
    val mode: String
}
