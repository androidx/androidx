/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SerialExecutorTest {

    lateinit var executor: SerialExecutorImpl

    @Before
    fun setUp() {
        executor = SerialExecutorImpl(Executors.newCachedThreadPool())
    }

    @Test
    @LargeTest
    fun testSerialExecutor() {
        val latch = CountDownLatch(3)
        val first = TimestampTrackingRunnable(latch)
        val second = TimestampTrackingRunnable(latch)
        val third = TimestampTrackingRunnable(latch)
        val commands = listOf(first, second, third)
        commands.forEach(executor::execute)
        latch.await(1, TimeUnit.SECONDS)
        var lastStart = 0L
        for (runnable in commands) {
            assertThat(runnable.start, greaterThanOrEqualTo(lastStart))
            lastStart = runnable.end
        }
    }

    @Test
    @MediumTest
    fun testExecutorFailureDoesNotLock() = runBlocking {
        val delegate =
            object : Executor {
                var shouldThrow = true

                override fun execute(command: Runnable) {
                    if (shouldThrow) {
                        shouldThrow = false
                        throw RejectedExecutionException("Forced failure")
                    }
                    command.run()
                }
            }
        val serialExecutor = SerialExecutorImpl(delegate)

        // First execution should fail to schedule
        try {
            serialExecutor.execute(Runnable {})
            fail("Expected RejectedExecutionException")
        } catch (e: RejectedExecutionException) {
            // expected
        }

        // Second execution should succeed because delegate no longer throws
        val deferred = CompletableDeferred<Unit>()
        serialExecutor.execute(Runnable { deferred.complete(Unit) })

        withTimeout(10000) { deferred.await() }
    }

    companion object {
        class TimestampTrackingRunnable(private val latch: CountDownLatch) : Runnable {
            var start: Long = 0
            var end: Long = 0

            override fun run() {
                start = System.nanoTime()
                val sleepTime = (Math.random() * 100).toLong()
                try {
                    // Sleep for a random amount of time to simulate real work.
                    Thread.sleep(sleepTime)
                } catch (exception: InterruptedException) {
                    throw RuntimeException(exception)
                } finally {
                    end = System.nanoTime()
                    latch.countDown()
                }
            }
        }
    }
}
