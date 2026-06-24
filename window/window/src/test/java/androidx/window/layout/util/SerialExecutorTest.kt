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

package androidx.window.layout.util

import android.os.Build
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [SerialExecutor]. */
class SerialExecutorTest {

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun testExecutionIsSequential() {
        val baseExecutor = Executors.newFixedThreadPool(2)
        val serialExecutor = SerialExecutor(baseExecutor)
        val result = mutableListOf<Int>()
        val concurrentCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val latch = CountDownLatch(3)

        serialExecutor.execute {
            val current = concurrentCount.incrementAndGet()
            maxConcurrent.updateAndGet { maxCount -> maxOf(maxCount, current) }

            // Simulate small work
            val dummy = mutableListOf<Int>()
            for (i in 0..1000) {
                dummy.add(i)
            }

            synchronized(result) { result.add(1) }
            concurrentCount.decrementAndGet()
            latch.countDown()
        }
        serialExecutor.execute {
            val current = concurrentCount.incrementAndGet()
            maxConcurrent.updateAndGet { maxCount -> maxOf(maxCount, current) }

            val dummy = mutableListOf<Int>()
            for (i in 0..1000) {
                dummy.add(i)
            }

            synchronized(result) { result.add(2) }
            concurrentCount.decrementAndGet()
            latch.countDown()
        }
        serialExecutor.execute {
            val current = concurrentCount.incrementAndGet()
            maxConcurrent.updateAndGet { maxCount -> maxOf(maxCount, current) }

            val dummy = mutableListOf<Int>()
            for (i in 0..1000) {
                dummy.add(i)
            }

            synchronized(result) { result.add(3) }
            concurrentCount.decrementAndGet()
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)

        assertEquals(1, maxConcurrent.get())
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun testExecutionContinuesAfterFailure() {
        val baseExecutor = Executors.newSingleThreadExecutor()
        val serialExecutor = SerialExecutor(baseExecutor)
        val result = mutableListOf<Int>()
        val latch = CountDownLatch(2)
        val exception = RuntimeException("Test failure")
        var caughtException: Throwable? = null

        serialExecutor.execute {
            synchronized(result) { result.add(1) }
            latch.countDown()
        }
        serialExecutor.execute {
            // Set an uncaught exception handler to avoid logging a default error system.
            Thread.setDefaultUncaughtExceptionHandler { _, e -> caughtException = e }
            throw exception
        }
        // Reset the uncaught exception handler to keep tests hermetic.
        Thread.setDefaultUncaughtExceptionHandler(null)
        serialExecutor.execute {
            synchronized(result) { result.add(3) }
            latch.countDown()
        }

        latch.await(1, TimeUnit.SECONDS)

        assertEquals(listOf(1, 3), result)
        assertEquals(exception, caughtException)
    }
}
