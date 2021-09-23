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

import android.hardware.camera2.CaptureFailure
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class VerifyResultListener(capturesCount: Int) : Request.Listener {
    private val captureRequests = mutableListOf<RequestMetadata>()
    private val captureResults = mutableListOf<FrameInfo>()
    private val latch = CountDownLatch(capturesCount)

    override fun onAborted(request: Request) {
        latch.countDown()
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        captureRequests.add(requestMetadata)
        captureResults.add(result)
        latch.countDown()
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureFailure: CaptureFailure
    ) {
        latch.countDown()
    }

    suspend fun verify(
        verifyBlock: (
            captureRequests: List<RequestMetadata>,
            captureResults: List<FrameInfo>
        ) -> Unit,
        timeout: Long = TimeUnit.SECONDS.toMillis(5),
    ) {
        withTimeout(timeout) {
            latch.await()
            verifyBlock(captureRequests, captureResults)
        }
    }
}