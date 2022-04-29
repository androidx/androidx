/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core

import android.os.Build
import android.util.Size
import androidx.camera.core.impl.CaptureProcessor
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
class CaptureProcessorPipelineTest {
    companion object {
        private val DEFAULT_SIZE = Size(640, 480)
    }

    @Test
    fun canCloseUnderlyingCaptureProcessors() {
        // Sets up pre-processor
        val preProcessor = Mockito.mock(
            CaptureProcessor::class.java
        )
        var preProcessorCompleter: CallbackToFutureAdapter.Completer<Void>? = null
        val preProcessorCloseFuture =
            CallbackToFutureAdapter.getFuture<Void> { completer ->
                preProcessorCompleter = completer
                "preProcessorCloseFuture"
            }
        Mockito.`when`(preProcessor.closeFuture).thenReturn(preProcessorCloseFuture)

        // Sets up post-processor
        val postProcessor = Mockito.mock(
            CaptureProcessor::class.java
        )
        var postProcessorCompleter: CallbackToFutureAdapter.Completer<Void>? = null
        val postProcessorCloseFuture =
            CallbackToFutureAdapter.getFuture<Void> { completer ->
                postProcessorCompleter = completer
                "postProcessorCloseFuture"
            }
        Mockito.`when`(postProcessor.closeFuture).thenReturn(postProcessorCloseFuture)

        val captureProcessorPipeline = CaptureProcessorPipeline(
            preProcessor,
            2,
            postProcessor,
            Executors.newSingleThreadExecutor()
        )

        // Sets up the resolution to create the intermediate image reader
        captureProcessorPipeline.onResolutionUpdate(DEFAULT_SIZE)

        // Calls the close() function of the CaptureProcessorPipeline
        captureProcessorPipeline.close()

        // Verifies whether close() function of the underlying capture processors are called
        Mockito.verify(preProcessor, Mockito.times(1)).close()
        Mockito.verify(postProcessor, Mockito.times(1)).close()

        // Sets up the listener to monitor whether the close future is closed or not.
        val closedLatch = CountDownLatch(1)
        captureProcessorPipeline.closeFuture.addListener(
            { closedLatch.countDown() },
            CameraXExecutors.directExecutor()
        )

        // Checks that the close future is not completed before the underlying capture processor
        // complete their close futures
        Truth.assertThat(closedLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()

        // Completes the completer of the underlying capture processors to complete their close
        // futures
        preProcessorCompleter!!.set(null)
        postProcessorCompleter!!.set(null)

        // Checks whether the close future of CaptureProcessorPipeline is completed after the
        // underlying capture processors complete their close futures
        Truth.assertThat(closedLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }
}