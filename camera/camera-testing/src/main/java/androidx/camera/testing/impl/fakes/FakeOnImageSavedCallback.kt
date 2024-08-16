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

package androidx.camera.testing.impl.fakes

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.google.common.truth.Truth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A fake implementation of the [ImageCapture.OnImageCapturedCallback] that is used for test.
 *
 * @param captureCount Number of captures to wait for.
 */
public class FakeOnImageSavedCallback(captureCount: Int = 1) : ImageCapture.OnImageSavedCallback {
    private val latch = CountdownDeferred(captureCount)
    public val results: MutableList<ImageCapture.OutputFileResults> = mutableListOf()
    public val errors: MutableList<ImageCaptureException> = mutableListOf()

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        results.add(outputFileResults)
        latch.countDown()
    }

    override fun onError(exception: ImageCaptureException) {
        errors.add(exception)
        latch.countDown()
    }

    public suspend fun awaitCaptures(timeout: Duration = CAPTURE_TIMEOUT) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
    }

    public suspend fun awaitCapturesAndAssert(
        timeout: Duration = CAPTURE_TIMEOUT,
        capturedImagesCount: Int = 0,
        errorsCount: Int = 0
    ) {
        Truth.assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
        Truth.assertThat(results.size).isEqualTo(capturedImagesCount)
        Truth.assertThat(errors.size).isEqualTo(errorsCount)
    }

    private class CountdownDeferred(val count: Int) {

        private val deferredItems =
            mutableListOf<CompletableDeferred<Unit>>().apply {
                repeat(count) { add(CompletableDeferred()) }
            }
        private var index = 0

        fun countDown() {
            if (index < count) {
                deferredItems[index++].complete(Unit)
            } else {
                throw IllegalStateException("Countdown already finished")
            }
        }

        suspend fun await() {
            deferredItems.forEach { it.await() }
        }
    }

    public companion object {
        private val CAPTURE_TIMEOUT = 15.seconds
    }
}
