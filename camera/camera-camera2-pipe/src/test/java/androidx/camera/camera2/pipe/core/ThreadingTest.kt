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

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadingTest {
    @Test
    fun runBlockingWithTimeoutThrowsOnTimeout() = runTest {
        val latch = CountDownLatch(1)
        assertThrows<TimeoutCancellationException> {
            Threading.runBlockingWithTimeout(Dispatchers.IO, 500L) {
                // Simulate a long call that should time out.
                latch.await(10, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun runBlockingWithTimeoutOrNullReturnsNullOnTimeout() = runTest {
        val latch = CountDownLatch(1)
        val result = Threading.runBlockingWithTimeoutOrNull(Dispatchers.IO, 500L) {
            // Simulate a long call that should time out.
            latch.await(10, TimeUnit.SECONDS)
        }
        assertThat(result).isNull()
    }
}