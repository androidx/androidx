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
import androidx.camera.core.SurfaceEffect
import androidx.camera.core.SurfaceOutput
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
 * Unit tests for [SurfaceEffectWithExecutor].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceEffectWithExecutorTest {

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

    @Test(expected = IllegalStateException::class)
    fun initWithSurfaceEffectInternal_throwsException() {
        SurfaceEffectWithExecutor(object : SurfaceEffectInternal {
            override fun onInputSurface(request: SurfaceRequest) {}

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {}

            override fun release() {}
        }, mainThreadExecutor())
    }

    @Test
    fun invokeEffect_invokedOnEffectExecutor() {
        // Arrange: track which thread the methods are invoked on.
        var onInputSurfaceInvokedThread: Thread? = null
        var onOutputSurfaceInvokedThread: Thread? = null
        val effectWithExecutor = SurfaceEffectWithExecutor(object : SurfaceEffect {
            override fun onInputSurface(request: SurfaceRequest) {
                onInputSurfaceInvokedThread = currentThread()
            }

            override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
                onOutputSurfaceInvokedThread = currentThread()
            }
        }, executor)
        // Act: invoke methods.
        effectWithExecutor.onInputSurface(SurfaceRequest(SIZE, FakeCamera(), false))
        effectWithExecutor.onOutputSurface(mock(SurfaceOutput::class.java))
        shadowOf(getMainLooper()).idle()
        shadowOf(executorThread.looper).idle()
        // Assert: it's the executor thread.
        assertThat(onInputSurfaceInvokedThread).isEqualTo(executorThread)
        assertThat(onOutputSurfaceInvokedThread).isEqualTo(executorThread)
    }
}