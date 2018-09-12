/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.ui.foundation.binding.BindingBaseImpl
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReassembleTest {

    class TestFoundationFlutterBinding : BindingBaseImpl() {
        var wasLocked: Boolean = false

        override fun performReassemble(): Deferred<Unit> {
            wasLocked = locked
            return super.performReassemble()
        }
    }

    @Test
    fun `Pointer events are locked during reassemble`() {
        val binding = TestFoundationFlutterBinding()

        runBlocking { binding.reassembleApplication() }

        assertTrue(binding.wasLocked)
    }
}