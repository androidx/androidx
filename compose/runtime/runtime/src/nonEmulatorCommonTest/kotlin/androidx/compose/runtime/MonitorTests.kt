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

package androidx.compose.runtime

import androidx.compose.runtime.internal.AtomicInt
import androidx.compose.runtime.platform.makeMonitor
import androidx.compose.runtime.platform.synchronized
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

class MonitorTests {
    @Test
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun testWaitAndNotifyAll() = runTest {
        val counter = AtomicInt(0)
        val monitor = makeMonitor()

        withContext(Dispatchers.Default) {
            val numJobs = 3
            repeat(numJobs) {
                launch {
                    if (counter.add(1) == numJobs) {
                        synchronized(monitor) { monitor.notifyAll() }
                    } else {
                        synchronized(monitor) { monitor.wait() }
                        // If `wait` does not block as expected, `counter` will not be able to
                        // reach `numJobs` and the test will time out.
                        counter.add(-1)
                    }
                }
            }
        }
    }
}
