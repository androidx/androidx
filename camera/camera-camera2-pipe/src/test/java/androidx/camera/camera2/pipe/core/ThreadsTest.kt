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

package androidx.camera.camera2.pipe.core

import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadingTest {
    private val threads = FakeThreads.fromDispatcher(Dispatchers.IO)

    @Test
    fun runBlockingCheckedThrowsOnTimeout() = runTest {
        val latch = CountDownLatch(1)
        assertThrows<IllegalStateException> {
            threads.runBlockingChecked(500L) {
                // Simulate a long call that should time out.
                latch.await(10, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun runBlockingCheckedDoesNotThrowWhenNotTimedOut() = runTest {
        val latch = CountDownLatch(1)
        threads.runBlockingChecked(10_000L) { latch.await(500, TimeUnit.MILLISECONDS) }
    }

    @Test
    fun runBlockingCheckedOrNullReturnsNullOnTimeout() = runTest {
        val latch = CountDownLatch(1)
        val result =
            threads.runBlockingCheckedOrNull(500L) {
                // Simulate a long call that should time out.
                latch.await(10, TimeUnit.SECONDS)
            }
        assertThat(result).isNull()
    }

    @Test
    fun runBlockingCheckedOrNullReturnsNonNullWhenNotTimeout() = runTest {
        val latch = CountDownLatch(1)
        val result =
            threads.runBlockingCheckedOrNull(10_000L) {
                // Simulate a long call that should time out.
                latch.await(500, TimeUnit.MILLISECONDS)
            }
        assertThat(result).isNotNull()
    }
}
