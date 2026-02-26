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

import androidx.sqlite.driver.web.util.initExternal
import org.w3c.dom.ErrorEvent

internal actual typealias Worker = org.w3c.dom.Worker

internal actual abstract class WebWorkerWrapper actual constructor(private val worker: Worker) {

    init {
        worker.onmessage = { resultMsg ->
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            val externalMsg = resultMsg.data as WebWorkerMessageExternal
            onMessage(WebWorkerMessage(externalMsg.id, externalMsg.data, externalMsg.error))
        }
        worker.onerror = { errorMsg ->
            if (errorMsg is ErrorEvent) {
                onError(errorMsg.message)
            } else {
                onError("An unknown error has occurred in the worker.")
            }
        }
    }

    protected actual abstract fun onMessage(message: WebWorkerMessage)

    protected actual abstract fun onError(errorMsg: String)

    protected actual fun postMessage(message: WebWorkerMessage) {
        worker.postMessage(WebWorkerMessageExternal(message.id, message.data, message.error))
    }
}

internal actual class WebWorkerMessage(
    actual val id: Int,
    val data: JsAny?,
    actual val error: String?,
)

private external interface WebWorkerMessageExternal : JsAny {
    var id: Int
    var data: JsAny?
    var error: String?
}

private fun WebWorkerMessageExternal(
    id: Int,
    data: JsAny?,
    error: String?,
): WebWorkerMessageExternal =
    initExternal<WebWorkerMessageExternal>().apply {
        this.id = id
        this.data = data
        this.error = error
    }

internal actual fun <T : Any> createWebWorkerMessage(id: Int, data: T): WebWorkerMessage {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return WebWorkerMessage(id, data as JsAny, null)
}
