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

package androidx.a2ui.core.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SynchronizedObjectTest {

    @Test
    fun synchronized_basicAction_executesAndReturnsValue() {
        val lock = SynchronizedObject()

        val result = synchronized(lock) { "success" }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun synchronized_actionThrowsException_propagatesException() {
        val lock = SynchronizedObject()

        assertThrows(IllegalStateException::class.java) {
            synchronized(lock) { throw IllegalStateException("error") }
        }
    }

    @Test
    fun synchronized_multipleThreads_ensuresMutualExclusion() = runBlocking {
        val lock = SynchronizedObject()
        var counter = 0
        val numCoroutines = 50
        val incrementsPerCoroutine = 1000

        val jobs =
            List(numCoroutines) {
                launch(Dispatchers.Default) {
                    repeat(incrementsPerCoroutine) { synchronized(lock) { counter++ } }
                }
            }
        jobs.joinAll()

        assertThat(counter).isEqualTo(numCoroutines * incrementsPerCoroutine)
    }
}
