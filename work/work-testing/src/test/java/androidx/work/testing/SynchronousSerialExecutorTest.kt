/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.work.testing

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Test

class SynchronousSerialExecutorTest {

    @Test
    fun stackOverFlow() {
        val executor = SynchronousSerialExecutor()
        class Recursive(val depth: Int = 0) : Runnable {
            override fun run() {
                if (depth != 10000) {
                    executor.execute(Recursive(depth + 1))
                }
            }
        }
        executor.execute(Recursive(0))
    }

    @Test
    fun multithread() {
        val executor = SynchronousSerialExecutor()
        val latch = CountDownLatch(1)
        var executed = false
        executor.execute {
            Executors.newSingleThreadExecutor().execute {
                // this call shouldn't be blocked, even though executor is "busy", executing
                // latch.await()
                executor.execute { executed = true }
                latch.countDown()
            }
            latch.await()
        }
        assertThat(executed).isTrue()
    }
}
