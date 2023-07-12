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

package androidx.camera.core.processing

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.ProcessingException
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeCamera
import com.google.common.truth.Truth.assertThat
import java.lang.Thread.currentThread
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SurfaceProcessorWithExecutor].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceProcessorWithExecutorTest {

    companion object {
        private val SIZE = Size(640, 480)
    }

    lateinit var executorThread: HandlerThread
    lateinit var executor: Executor

    @Before
    fun setup() {
        executorThread = HandlerThread("")
        executorThread.start()
        executor = CameraXExecutors.newHandlerExecutor(Handler(executorThread.looper))
    }

    @After
    fun tearDown() {
        executorThread.quitSafely()
    }

    @Test
    fun processorThrowsException_receivedByCameraEffect() {
        // Arrange: create a processor that throws an exception.
        val processor = object : SurfaceProcessor {
            override fun onInputSurface(surfaceRequest: SurfaceRequest) {
                throw ProcessingException()
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                throw ProcessingException()
            }
        }
        var errorReceived: Throwable? = null
        val processorWithExecutor = SurfaceProcessorWithExecutor(object : CameraEffect(
            PREVIEW,
            mainThreadExecutor(),
            processor,
            {
                errorReceived = it
            }
        ) {})

        // Act: invoke the processor.
        val fakeSurfaceRequest = SurfaceRequest(SIZE, FakeCamera()) {}
        processorWithExecutor.onInputSurface(fakeSurfaceRequest)
        shadowOf(getMainLooper()).idle()

        // Assert: the exception is received by the CameraEffect.
        assertThat(errorReceived).isInstanceOf(ProcessingException::class.java)
        fakeSurfaceRequest.willNotProvideSurface()
    }

    @Test
    fun invokeProcessor_invokedOnProcessorExecutor() {
        // Arrange: track which thread the methods are invoked on.
        var onInputSurfaceInvokedThread: Thread? = null
        var onOutputSurfaceInvokedThread: Thread? = null
        val processor = object : SurfaceProcessor {
            override fun onInputSurface(surfaceRequest: SurfaceRequest) {
                onInputSurfaceInvokedThread = currentThread()
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                onOutputSurfaceInvokedThread = currentThread()
            }
        }
        val processorWithExecutor = SurfaceProcessorWithExecutor(object : CameraEffect(
            PREVIEW,
            executor,
            processor,
            {}
        ) {})
        // Act: invoke methods.
        processorWithExecutor.onInputSurface(SurfaceRequest(SIZE, FakeCamera()) {})
        processorWithExecutor.onOutputSurface(mock(SurfaceOutput::class.java))
        shadowOf(getMainLooper()).idle()
        shadowOf(executorThread.looper).idle()
        // Assert: it's the executor thread.
        assertThat(onInputSurfaceInvokedThread).isEqualTo(executorThread)
        assertThat(onOutputSurfaceInvokedThread).isEqualTo(executorThread)
    }
}