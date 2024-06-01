/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.work

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ListenableFutureTest {

    @Test
    fun testFailure() {
        val executor = Executors.newSingleThreadExecutor()
        val future =
            executor.executeAsync<Unit>("testFailure") {
                throw IllegalStateException("And I'm tripping and falling")
            }
        try {
            future.get()
            throw AssertionError("It should fail")
        } catch (e: ExecutionException) {
            assertThat(e.cause).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun testSuccess() {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.executeAsync("testSuccess") { "hello" }
        assertThat(future.get()).isEqualTo("hello")
    }

    @Test
    fun testCancellation() {
        val executor = ManualExecutor()
        var wasCalled = false
        val future = executor.executeAsync("testCancellation") { wasCalled = true }
        future.cancel(true)
        executor.drain()
        assertThat(wasCalled).isFalse()
    }

    private class ManualExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>(10)

        override fun execute(runnable: Runnable) {
            tasks.add(runnable)
        }

        fun drain() {
            while (tasks.isNotEmpty()) {
                val head = tasks.removeFirst()
                head.run()
            }
        }
    }
}
