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

package androidx.compose.ui

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class CurrentTimeTest {

    @Test
    fun timeMonotonicallyIncreases() = runTest {
        var time = currentTimeMillis()

        suspend fun realDelay() {
            withContext(Dispatchers.Default) {
                delay(2)
            }
        }

        repeat(100) {
            realDelay()
            val newTime = currentTimeMillis()
            assertTrue(newTime > time, "Time did not increase: $newTime > $time")
            time = newTime
        }
    }
}