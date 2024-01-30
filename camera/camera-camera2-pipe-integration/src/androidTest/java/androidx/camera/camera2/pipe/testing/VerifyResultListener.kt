/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class VerifyResultListener(capturesCount: Int) : Request.Listener {
    private val captureRequests = mutableListOf<RequestMetadata>()
    private val captureResults = mutableListOf<FrameInfo>()

    private val waitingCount = atomic(capturesCount)
    private val failureException =
        TimeoutException("Test doesn't complete after waiting for $capturesCount frames.")

    @Volatile
    private var startReceiving = false

    @Volatile
    private var _verifyBlock: (
        captureRequest: RequestMetadata,
        captureResult: FrameInfo
    ) -> Boolean = { _, _ -> false }
    private val signal = CompletableDeferred<Unit>()

    override fun onAborted(request: Request) {
        if (!startReceiving) {
            return
        }

        if (waitingCount.decrementAndGet() < 0) {
            signal.completeExceptionally(failureException)
            return
        }
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        if (!startReceiving) {
            return
        }
        captureRequests.add(requestMetadata)
        captureResults.add(result)
        if (waitingCount.decrementAndGet() < 0) {
            signal.completeExceptionally(failureException)
            return
        }
        try {
            if (_verifyBlock(requestMetadata, result)) {
                signal.complete(Unit)
            }
        } catch (e: Throwable) {
            signal.completeExceptionally(e)
        }
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure
    ) {
        if (!startReceiving) {
            return
        }
        if (waitingCount.decrementAndGet() < 0) {
            signal.completeExceptionally(failureException)
            return
        }
    }

    suspend fun verify(
        verifyBlock: (
            captureRequest: RequestMetadata,
            captureResult: FrameInfo
        ) -> Boolean = { _, _ -> false },
        timeout: Long = TimeUnit.SECONDS.toMillis(5),
    ) {
        withTimeout(timeout) {
            _verifyBlock = verifyBlock
            startReceiving = true
            signal.await()
        }
    }

    suspend fun verifyAllResults(
        verifyBlock: (
            captureRequests: List<RequestMetadata>,
            captureResults: List<FrameInfo>
        ) -> Unit,
        timeout: Long = TimeUnit.SECONDS.toMillis(5),
    ) {
        withTimeout(timeout) {
            startReceiving = true
            signal.join()
            verifyBlock(captureRequests, captureResults)
        }
    }
}
