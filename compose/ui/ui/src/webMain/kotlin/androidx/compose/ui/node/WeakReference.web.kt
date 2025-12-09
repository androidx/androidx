/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.node

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsReference
import kotlin.js.get
import kotlin.js.toJsReference
import kotlin.js.unsafeCast

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakRef
@OptIn(ExperimentalWasmJsInterop::class)
private external class WeakRef {
    constructor(target: JsAny)
    fun deref(): JsAny?
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual class WeakReference<T : Any> actual constructor(referent: T) {
    private var weakRef: WeakRef? = WeakRef(referent.toJsReference())
    actual fun get(): T? = weakRef?.deref()?.unsafeCast<JsReference<T>>()?.get()

    // Js WeakRef doesn't have a method for clearing it. So we just drop the WeakRef itself.
    actual fun clear() {
        weakRef = null
    }
}