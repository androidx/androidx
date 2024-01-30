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

package androidx.compose.platform

import androidx.compose.ui.platform.FlushCoroutineDispatcher
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class FlushCoroutineDispatcherTest {
    // we can't write this test in skikoTest because we can't wait blocking on JS target.
    @Test
    fun flushing_in_another_thread() = runBlocking {
        val actualNumbers = mutableListOf<Int>()
        lateinit var dispatcher: FlushCoroutineDispatcher
        val random = Random(123)

        withContext(Dispatchers.Default) {
            dispatcher = FlushCoroutineDispatcher(this)

            val addJob = launch(dispatcher) {
                repeat(10000) {
                    actualNumbers.add(it)
                    repeat(random.nextInt(5)) {
                        yield()
                    }
                }
            }

            launch {
                while (addJob.isActive) {
                    dispatcher.flush()
                    yield()
                }
            }
        }

        assertEquals((0 until 10000).toList(), actualNumbers)
        assertFalse(dispatcher.hasTasks())
    }
}
