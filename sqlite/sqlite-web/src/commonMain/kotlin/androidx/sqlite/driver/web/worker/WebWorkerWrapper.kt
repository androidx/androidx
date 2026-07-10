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

package androidx.sqlite.driver.web.worker

internal expect class Worker

/** A common wrapper around web workers since their API is not common between JS and wasmJs. */
internal expect abstract class WebWorkerWrapper(worker: Worker) {
    protected abstract fun onMessage(message: WebWorkerMessage): Unit

    protected abstract fun onError(errorMsg: String): Unit

    protected fun postMessage(message: WebWorkerMessage)
}

/* Enclosing object definition of all requests and responses. */
internal expect class WebWorkerMessage {
    val id: Int
    val error: String?
}

internal expect fun <T : Any> createWebWorkerMessage(id: Int, data: T): WebWorkerMessage

internal class WebWorkerException(message: String) : RuntimeException(message)
