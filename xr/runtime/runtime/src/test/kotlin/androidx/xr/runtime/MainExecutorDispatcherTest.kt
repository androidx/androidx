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

package androidx.xr.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainExecutorDispatcherTest {

    private lateinit var executor: ExecutorService
    private lateinit var dispatcher: MainExecutorDispatcher

    @Before
    fun setUp() {
        executor = Executors.newSingleThreadExecutor()
        dispatcher = MainExecutorDispatcher(executor)
    }

    @After
    fun tearDown() {
        executor.shutdown()
    }

    @Test
    fun isDispatchNeeded_standard_onMainThread_returnsTrue() {
        val isNeeded = dispatcher.isDispatchNeeded(EmptyCoroutineContext)

        assertThat(isNeeded).isTrue()
    }

    @Test
    fun isDispatchNeeded_immediate_onMainThread_returnsFalse() {
        val isNeeded = dispatcher.immediate.isDispatchNeeded(EmptyCoroutineContext)

        assertThat(isNeeded).isFalse()
    }

    @Test
    fun isDispatchNeeded_standard_onWorkerThread_returnsTrue() {
        val workerThread = Executors.newSingleThreadExecutor()

        try {
            val isNeeded =
                workerThread
                    .submit<Boolean> { dispatcher.isDispatchNeeded(EmptyCoroutineContext) }
                    .get()

            assertThat(isNeeded).isTrue()
        } finally {
            workerThread.shutdown()
        }
    }

    @Test
    fun isDispatchNeeded_immediate_onWorkerThread_returnsTrue() {
        val workerThread = Executors.newSingleThreadExecutor()

        try {
            val isNeeded =
                workerThread
                    .submit<Boolean> {
                        dispatcher.immediate.isDispatchNeeded(EmptyCoroutineContext)
                    }
                    .get()

            assertThat(isNeeded).isTrue()
        } finally {
            workerThread.shutdown()
        }
    }

    @Test
    fun immediate_immediate_returnsSameInstance() {
        val immediate1 = dispatcher.immediate
        val immediate2 = immediate1.immediate

        assertThat(immediate2).isSameInstanceAs(immediate1)
    }

    @Test
    fun withContext_immediate_onMainThread_executesSynchronously() = runTest {
        val callingThread = Thread.currentThread()
        var executedThread: Thread? = null

        withContext(dispatcher.immediate) { executedThread = Thread.currentThread() }

        assertThat(executedThread).isEqualTo(callingThread)
    }

    @Test
    fun toString_returnsDescriptiveString() {
        assertThat(dispatcher.toString()).isEqualTo("MainExecutorDispatcher(isImmediate=false)")
        assertThat(dispatcher.immediate.toString())
            .isEqualTo("MainExecutorDispatcher(isImmediate=true)")
    }
}
