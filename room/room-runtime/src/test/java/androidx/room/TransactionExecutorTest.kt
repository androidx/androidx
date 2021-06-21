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

package androidx.room

import androidx.test.filters.FlakyTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class TransactionExecutorTest {

    private val testExecutor = Executors.newCachedThreadPool()
    private val transactionExecutor = TransactionExecutor(testExecutor)

    @After
    fun teardown() {
        testExecutor.shutdownNow()
    }

    @FlakyTest(bugId = 187828770)
    @Test
    @Throws(InterruptedException::class)
    fun testSerialExecution() {

        val latch = CountDownLatch(3)
        val runnableA = TimingRunnable(latch)
        val runnableB = TimingRunnable(latch)
        val runnableC = TimingRunnable(latch)

        transactionExecutor.execute(runnableA)
        transactionExecutor.execute(runnableB)
        transactionExecutor.execute(runnableC)

        // Await for the runnables to finish.
        latch.await(1, TimeUnit.SECONDS)

        // Assert that everything ran.
        assertThat(runnableA.run).isTrue()
        assertThat(runnableB.run).isTrue()
        assertThat(runnableC.run).isTrue()

        // Assert that runnables were run in order of submission.
        assertThat(runnableA.start).isLessThan(runnableB.start)
        assertThat(runnableB.start).isLessThan(runnableC.start)

        // Assert that a runnable finishes before the runnable after it starts.
        assertThat(runnableA.finish).isLessThan(runnableB.start)
        assertThat(runnableB.finish).isLessThan(runnableC.start)
    }

    private class TimingRunnable(val latch: CountDownLatch) : Runnable {
        var start: Long = 0
        var finish: Long = 0
        var run: Boolean = false

        override fun run() {
            run = true
            start = System.nanoTime()
            try {
                // Sleep for a bit as if we were doing real work.
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            finish = System.nanoTime()
            latch.countDown()
        }
    }
}