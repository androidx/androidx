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

import kotlinx.coroutines.CompletableDeferred

/**
 * A helper to communicate with a worker, bridging between its callback based APIs to Coroutines.
 *
 * A single `onmessage` callback should be installed in the worker and messages are multiplexed
 * though a map of 'message id' to [CompletableDeferred].
 */
internal abstract class CoroutineWebWorker(worker: Worker) : WebWorkerWrapper(worker) {
    private var nextMessageId = 0
    private val pendingMessages = mutableMapOf<Int, CompletableDeferred<WebWorkerMessage>>()

    override fun onMessage(message: WebWorkerMessage) {
        val pendingCompletable =
            pendingMessages[message.id]
                ?: error("Error processing result, message id not found: ${message.id}.}")
        val completed = pendingCompletable.complete(message)
        if (!completed) {
            error("Error processing result, message with id ${message.id} was already delivered.")
        }
    }

    override fun onError(errorMsg: String) {
        val exception = WebWorkerException(errorMsg)
        pendingMessages.values.forEach { it.completeExceptionally(exception) }
    }

    protected abstract fun handleResultError(error: String): Throwable

    /** Sends a one-way request without waiting for a response. */
    fun <Request : Any> sendRequest(request: Request) {
        val requestMsg = createWebWorkerMessage(nextMessageId++, request)
        postMessage(requestMsg)
    }

    /** Sends a request and await for a response. */
    suspend fun <Request : Any, Result : Any> sendRequest(
        request: Request,
        resultFactory: (WebWorkerMessage) -> Result,
    ): Result {
        val requestMsg = createWebWorkerMessage(nextMessageId++, request)
        pendingMessages[requestMsg.id] = CompletableDeferred()
        postMessage(requestMsg)
        val resultMsg = pendingMessages.getValue(requestMsg.id).await()
        pendingMessages.remove(requestMsg.id)
        check(requestMsg.id == resultMsg.id) {
            "Error processing result, message id mismatch: request id: " +
                "${requestMsg.id}, result id: ${resultMsg.id}"
        }
        val errorMsg = resultMsg.error
        if (errorMsg != null) {
            throw handleResultError(errorMsg)
        }
        return resultFactory(resultMsg)
    }
}
