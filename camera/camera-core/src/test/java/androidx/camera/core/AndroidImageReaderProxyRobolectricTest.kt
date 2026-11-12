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

package androidx.camera.core

import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.utils.MainThreadAsyncHandler
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Verifies how [AndroidImageReaderProxy] dispatches image available callbacks. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class AndroidImageReaderProxyRobolectricTest {
    private val imageReader = mock(ImageReader::class.java)
    private val listenerInvocations = AtomicInteger()
    private val countingListener = ImageReaderProxy.OnImageAvailableListener {
        listenerInvocations.incrementAndGet()
    }
    private lateinit var imageReaderProxy: AndroidImageReaderProxy
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    @Before
    fun setUp() {
        `when`(imageReader.width).thenReturn(640)
        imageReaderProxy = AndroidImageReaderProxy(imageReader)
        backgroundThread = HandlerThread("AndroidImageReaderProxyRobolectricTest").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    @After
    fun tearDown() {
        backgroundThread.quitSafely()
    }

    @Test
    fun backgroundHandlerExecutor_registersExecutorHandler() {
        imageReaderProxy.setOnImageAvailableListener(
            countingListener,
            CameraXExecutors.newHandlerExecutor(backgroundHandler),
        )

        assertThat(captureRegisteredHandler()).isSameInstanceAs(backgroundHandler)
    }

    @Test
    fun highPriorityExecutor_registersHighPriorityHandler() {
        val highPriorityExecutor = CameraXExecutors.highPriorityExecutor()

        imageReaderProxy.setOnImageAvailableListener(countingListener, highPriorityExecutor)

        val registeredHandler = captureRegisteredHandler()
        assertThat(registeredHandler)
            .isSameInstanceAs(CameraXExecutors.unwrapHandler(highPriorityExecutor))
        assertThat(registeredHandler.looper).isNotSameInstanceAs(Looper.getMainLooper())
    }

    @Test
    fun backgroundHandlerExecutor_invokesListenerDirectly() {
        val invokingThread = AtomicReference<Thread>()
        imageReaderProxy.setOnImageAvailableListener(
            { _ ->
                invokingThread.set(Thread.currentThread())
                listenerInvocations.incrementAndGet()
            },
            CameraXExecutors.newHandlerExecutor(backgroundHandler),
        )
        val transformedListener = captureTransformedListener()
        val invocationsBeforeReturn = AtomicInteger(-1)
        val latch = CountDownLatch(1)

        // Framework callbacks run on the registered handler's looper. Running inside a posted task
        // guarantees any re-post to backgroundHandler could not execute before onImageAvailable
        // returns.
        backgroundHandler.post {
            try {
                transformedListener.onImageAvailable(imageReader)
                invocationsBeforeReturn.set(listenerInvocations.get())
            } finally {
                latch.countDown()
            }
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(invocationsBeforeReturn.get()).isEqualTo(1)
        assertThat(invokingThread.get()).isSameInstanceAs(backgroundThread)
    }

    @Test
    fun backgroundHandlerExecutor_wrongThreadCallback_postsToExecutor() {
        val invokingThread = AtomicReference<Thread>()
        val latch = CountDownLatch(1)
        imageReaderProxy.setOnImageAvailableListener(
            { _ ->
                try {
                    invokingThread.set(Thread.currentThread())
                    listenerInvocations.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            },
            CameraXExecutors.newHandlerExecutor(backgroundHandler),
        )
        val transformedListener = captureTransformedListener()
        val backgroundLooper = shadowOf(backgroundThread.looper)
        backgroundLooper.pause()
        try {
            // Simulates a stale ImageReader.ListenerHandler message from a previous Looper
            // invoking the newly registered listener on the wrong thread (here, the main thread).
            transformedListener.onImageAvailable(imageReader)
            assertThat(listenerInvocations.get()).isEqualTo(0)
        } finally {
            backgroundLooper.unPause()
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(listenerInvocations.get()).isEqualTo(1)
        assertThat(invokingThread.get()).isSameInstanceAs(backgroundThread)
    }

    @Test
    fun backgroundHandlerExecutor_doesNotHoldLockDuringListener() {
        val otherThreadProceeded = AtomicReference<Boolean>()
        val latch = CountDownLatch(1)
        imageReaderProxy.setOnImageAvailableListener(
            { _ ->
                try {
                    otherThreadProceeded.set(canAccessProxyFromAnotherThread())
                } finally {
                    latch.countDown()
                }
            },
            CameraXExecutors.newHandlerExecutor(backgroundHandler),
        )
        val transformedListener = captureTransformedListener()

        backgroundHandler.post { transformedListener.onImageAvailable(imageReader) }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(otherThreadProceeded.get()).isTrue()
    }

    @Test
    fun mainThreadExecutor_registersMainAsyncHandlerAndPostsToExecutor() {
        assertRegistersMainAsyncHandlerAndPostsToExecutor(CameraXExecutors.mainThreadExecutor())
    }

    @Test
    fun mainLooperHandlerExecutor_registersMainAsyncHandlerAndPostsToExecutor() {
        assertRegistersMainAsyncHandlerAndPostsToExecutor(
            CameraXExecutors.newHandlerExecutor(Handler(Looper.getMainLooper()))
        )
    }

    @Test
    fun handlerWithNullLooper_fallsBackToMainHandlerAndPostsToExecutor() {
        val handler = mock(Handler::class.java)
        `when`(handler.looper).thenReturn(null)
        `when`(handler.post(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            true
        }
        imageReaderProxy.setOnImageAvailableListener(
            countingListener,
            CameraXExecutors.newHandlerExecutor(handler),
        )

        assertThat(captureRegisteredHandler())
            .isSameInstanceAs(MainThreadAsyncHandler.getInstance())

        captureTransformedListener().onImageAvailable(imageReader)
        verify(handler).post(any())
        assertThat(listenerInvocations.get()).isEqualTo(1)
    }

    @Test
    fun nonHandlerExecutor_registersMainHandlerAndPostsToExecutor() {
        val executor = RecordingExecutor()
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)

        assertThat(captureRegisteredHandler())
            .isSameInstanceAs(MainThreadAsyncHandler.getInstance())

        captureTransformedListener().onImageAvailable(imageReader)
        assertThat(executor.tasks).hasSize(1)
        assertThat(listenerInvocations.get()).isEqualTo(0)

        executor.runAll()
        assertThat(listenerInvocations.get()).isEqualTo(1)
    }

    @Test
    fun nonHandlerExecutor_doesNotHoldLockDuringExecute() {
        val otherThreadProceeded = AtomicReference<Boolean>()
        val executor = Executor { otherThreadProceeded.set(canAccessProxyFromAnotherThread()) }
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)

        captureTransformedListener().onImageAvailable(imageReader)

        assertThat(otherThreadProceeded.get()).isTrue()
    }

    @Test
    fun backgroundHandlerExecutor_skipsListenerAfterCleared() {
        imageReaderProxy.setOnImageAvailableListener(
            countingListener,
            CameraXExecutors.newHandlerExecutor(backgroundHandler),
        )
        val transformedListener = captureTransformedListener()

        imageReaderProxy.clearOnImageAvailableListener()
        transformedListener.onImageAvailable(imageReader)

        assertThat(listenerInvocations.get()).isEqualTo(0)
    }

    @Test
    fun nonHandlerExecutor_skipsExecuteAfterCleared() {
        val executor = RecordingExecutor()
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)
        val transformedListener = captureTransformedListener()

        imageReaderProxy.clearOnImageAvailableListener()
        transformedListener.onImageAvailable(imageReader)

        assertThat(executor.tasks).isEmpty()
        assertThat(listenerInvocations.get()).isEqualTo(0)
    }

    @Test
    fun nonHandlerExecutor_skipsQueuedRunnableAfterCleared() {
        val executor = RecordingExecutor()
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)
        captureTransformedListener().onImageAvailable(imageReader)

        imageReaderProxy.clearOnImageAvailableListener()
        executor.runAll()

        assertThat(listenerInvocations.get()).isEqualTo(0)
    }

    @Test
    fun replacedListener_skipsStaleCallbacksAndQueuedRunnables() {
        val invocations1 = AtomicInteger()
        val invocations2 = AtomicInteger()
        val executor1 = RecordingExecutor()
        val executor2 = RecordingExecutor()
        imageReaderProxy.setOnImageAvailableListener(
            { _ -> invocations1.incrementAndGet() },
            executor1,
        )
        // Queue a runnable for the first listener before it is replaced.
        captureTransformedListeners().last().onImageAvailable(imageReader)
        assertThat(executor1.tasks).hasSize(1)

        imageReaderProxy.setOnImageAvailableListener(
            { _ -> invocations2.incrementAndGet() },
            executor2,
        )
        val (transformedListener1, transformedListener2) = captureTransformedListeners()

        transformedListener1.onImageAvailable(imageReader)
        executor1.runAll()
        assertThat(executor1.tasks).hasSize(1)
        assertThat(invocations1.get()).isEqualTo(0)

        transformedListener2.onImageAvailable(imageReader)
        executor2.runAll()
        assertThat(invocations2.get()).isEqualTo(1)
    }

    @Test
    fun executorRejectsAfterConcurrentClear_doesNotThrow() {
        // Simulates the listener being cleared and the executor shut down while the callback is
        // in flight (b/242470184).
        val executor = Executor {
            imageReaderProxy.clearOnImageAvailableListener()
            throw RejectedExecutionException()
        }
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)

        captureTransformedListener().onImageAvailable(imageReader)

        assertThat(listenerInvocations.get()).isEqualTo(0)
    }

    @Test
    fun executorRejectsWhileListenerActive_rethrows() {
        val executor = Executor { throw RejectedExecutionException() }
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)
        val transformedListener = captureTransformedListener()

        assertThrows<RejectedExecutionException> {
            transformedListener.onImageAvailable(imageReader)
        }
    }

    private fun assertRegistersMainAsyncHandlerAndPostsToExecutor(executor: Executor) {
        imageReaderProxy.setOnImageAvailableListener(countingListener, executor)

        assertThat(captureRegisteredHandler())
            .isSameInstanceAs(MainThreadAsyncHandler.getInstance())

        captureTransformedListener().onImageAvailable(imageReader)
        assertThat(listenerInvocations.get()).isEqualTo(0)

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerInvocations.get()).isEqualTo(1)
    }

    private fun captureRegisteredHandler(): Handler {
        val handlerCaptor = ArgumentCaptor.forClass(Handler::class.java)
        verify(imageReader).setOnImageAvailableListener(any(), handlerCaptor.capture())
        return handlerCaptor.value
    }

    private fun captureTransformedListener(): ImageReader.OnImageAvailableListener =
        captureTransformedListeners().single()

    private fun captureTransformedListeners(): List<ImageReader.OnImageAvailableListener> {
        val listenerCaptor =
            ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener::class.java)
        verify(imageReader, atLeastOnce())
            .setOnImageAvailableListener(listenerCaptor.capture(), any())
        return listenerCaptor.allValues
    }

    /**
     * Returns whether another thread can call into the proxy's lock-guarded methods, i.e. whether
     * the calling thread is not holding the proxy's lock.
     */
    private fun canAccessProxyFromAnotherThread(): Boolean {
        val latch = CountDownLatch(1)
        val thread = Thread {
            try {
                imageReaderProxy.width
            } finally {
                latch.countDown()
            }
        }
        thread.start()
        val proceeded = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (proceeded) {
            thread.join(TIMEOUT_SECONDS * 1000)
        }
        return proceeded
    }

    private class RecordingExecutor : Executor {
        val tasks = mutableListOf<Runnable>()

        override fun execute(command: Runnable) {
            tasks.add(command)
        }

        fun runAll() {
            tasks.forEach { it.run() }
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
