/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room.coroutines

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.concurrent.AtomicInt
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test

class RunBlockingUninterruptibleTest {

    @Test
    fun interruptionIgnored() = runTest {
        val blockExecutionCount = AtomicInt(0)
        val enterLatch = Mutex(locked = true)
        val exitLatch = Mutex(locked = true)

        var result: String? = null
        val t =
            thread(start = true) {
                result = runBlockingUninterruptible {
                    blockExecutionCount.incrementAndGet()
                    enterLatch.unlock()
                    exitLatch.lock()
                    "Tom"
                }
            }

        // interrupt thread, no InterruptedException should be thrown
        enterLatch.lock()
        t.interrupt()

        // unlock thread, should complete normally
        exitLatch.unlock()
        t.join()

        assertThat(result).isEqualTo("Tom")
        assertThat(blockExecutionCount.get()).isEqualTo(1)
    }

    @Test
    fun alreadyInterrupted() {
        val blockExecutionCount = AtomicInt(0)
        Thread.currentThread().interrupt()
        val result = runBlockingUninterruptible {
            blockExecutionCount.incrementAndGet()
            yield()
            "Tom"
        }
        assertThat(result).isEqualTo("Tom")
        assertThat(blockExecutionCount.get()).isEqualTo(1)
    }

    @Test
    fun threadLocalPreserved() {
        val tLocal = ThreadLocal<String>().apply { set("TestValue") }

        val coroutineBlock: suspend CoroutineScope.() -> Unit = {
            assertThat(tLocal.get()).isEqualTo("TestValue")
            yield()
            assertThat(tLocal.get()).isEqualTo("TestValue")

            withContext(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                assertThat(tLocal.get()).isNull()
            }

            assertThat(tLocal.get()).isEqualTo("TestValue")
        }

        runBlocking(block = coroutineBlock)
        runBlockingUninterruptible(block = coroutineBlock)

        tLocal.remove()
    }

    @Test
    fun explicitExceptionThrown() {
        assertThrows<InterruptedException> {
            runBlockingUninterruptible { throw InterruptedException() }
        }
        assertThrows<IllegalStateException> {
            runBlockingUninterruptible { throw IllegalStateException() }
        }
    }

    @Test
    fun combineRunBlocking() {
        val coroutineBlock: suspend CoroutineScope.() -> Unit = {
            yield()
            val result = runBlockingUninterruptible {
                yield()
                "Tom"
            }
            assertThat(result).isEqualTo("Tom")
        }

        runBlocking(block = coroutineBlock)
        runTest(testBody = coroutineBlock)
    }
}
