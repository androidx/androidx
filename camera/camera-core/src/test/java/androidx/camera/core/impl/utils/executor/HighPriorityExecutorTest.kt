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

package androidx.camera.core.impl.utils.executor

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class HighPriorityExecutorTest {
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    @Before
    fun setUp() {
        backgroundThread = HandlerThread("HighPriorityExecutorTest").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    @After
    fun tearDown() {
        backgroundThread.quitSafely()
    }

    @Test
    fun highPriorityExecutor_isSingleton() {
        assertThat(CameraXExecutors.highPriorityExecutor())
            .isSameInstanceAs(CameraXExecutors.highPriorityExecutor())
    }

    @Test
    fun highPriorityExecutor_isPureExecutor() {
        assertThat(CameraXExecutors.highPriorityExecutor())
            .isNotInstanceOf(ExecutorService::class.java)
    }

    @Test
    fun construction_doesNotStartThread() {
        val threadCountBefore = countHighPriorityThreads()

        val executor = HighPriorityExecutor()

        assertThat(countHighPriorityThreads()).isEqualTo(threadCountBefore)
        assertThat(executor).isNotSameInstanceAs(HighPriorityExecutor.getInstance())
    }

    @Test
    fun unwrapHandler_highPriorityExecutor_returnsBackgroundHandler() {
        assertBackgroundLooper(
            CameraXExecutors.unwrapHandler(CameraXExecutors.highPriorityExecutor())
        )
    }

    @Test
    fun unwrapHandler_highPriorityExecutor_returnsSameHandler() {
        val executor = CameraXExecutors.highPriorityExecutor()

        assertThat(CameraXExecutors.unwrapHandler(executor))
            .isSameInstanceAs(CameraXExecutors.unwrapHandler(executor))
    }

    @Test
    fun unwrapHandler_newHandlerExecutor_returnsHandler() {
        assertThat(
                CameraXExecutors.unwrapHandler(
                    CameraXExecutors.newHandlerExecutor(backgroundHandler)
                )
            )
            .isSameInstanceAs(backgroundHandler)
    }

    @Test
    fun unwrapHandler_myLooperExecutorOnBackgroundThread_returnsBackgroundLooperHandler() {
        val unwrapped = AtomicReference<Handler?>()
        val latch = CountDownLatch(1)
        backgroundHandler.post {
            try {
                unwrapped.set(CameraXExecutors.unwrapHandler(CameraXExecutors.myLooperExecutor()))
            } finally {
                latch.countDown()
            }
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(requireNotNull(unwrapped.get()).looper).isSameInstanceAs(backgroundThread.looper)
    }

    @Test
    fun unwrapHandler_mainThreadExecutor_returnsMainLooperHandler() {
        val handler = CameraXExecutors.unwrapHandler(CameraXExecutors.mainThreadExecutor())

        assertThat(requireNotNull(handler).looper).isSameInstanceAs(Looper.getMainLooper())
    }

    @Test
    fun unwrapHandler_nonHandlerExecutors_returnsNull() {
        assertThat(CameraXExecutors.unwrapHandler(CameraXExecutors.directExecutor())).isNull()
        assertThat(CameraXExecutors.unwrapHandler(CameraXExecutors.ioExecutor())).isNull()
        assertThat(
                CameraXExecutors.unwrapHandler(
                    CameraXExecutors.newSequentialExecutor(CameraXExecutors.highPriorityExecutor())
                )
            )
            .isNull()
    }

    // Thread priority is not asserted: Robolectric does not apply HandlerThread priorities, so
    // Process.getThreadPriority() always returns 0 on the host JVM.
    @Test
    fun execute_runsTasksInOrderOnHighPriorityThread() {
        val executor = CameraXExecutors.highPriorityExecutor()
        val order = Collections.synchronizedList(mutableListOf<Int>())
        val threadName = AtomicReference<String>()
        val looper = AtomicReference<Looper?>()
        val latch = CountDownLatch(TASK_COUNT)

        for (i in 0 until TASK_COUNT) {
            executor.execute {
                try {
                    order.add(i)
                    threadName.set(Thread.currentThread().name)
                    looper.set(Looper.myLooper())
                } finally {
                    latch.countDown()
                }
            }
        }

        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(order).containsExactlyElementsIn((0 until TASK_COUNT).toList()).inOrder()
        assertThat(threadName.get()).isEqualTo(THREAD_NAME)
        assertBackgroundLooper(CameraXExecutors.unwrapHandler(executor))
        assertThat(looper.get())
            .isSameInstanceAs(requireNotNull(CameraXExecutors.unwrapHandler(executor)).looper)
    }

    @Test
    fun execute_nullCommand_throwsNullPointerException() {
        val executor: Executor = CameraXExecutors.highPriorityExecutor()

        assertThrows<NullPointerException> { executor.execute(null) }
    }

    private fun assertBackgroundLooper(handler: Handler?) {
        val looper = requireNotNull(handler).looper
        assertThat(looper).isNotNull()
        assertThat(looper).isNotSameInstanceAs(Looper.getMainLooper())
    }

    private fun countHighPriorityThreads(): Int =
        Thread.getAllStackTraces().keys.count { it.name == THREAD_NAME }

    private companion object {
        const val THREAD_NAME = "CameraX-camerax_high_priority"
        const val TASK_COUNT = 10
        const val TIMEOUT_SECONDS = 5L
    }
}
