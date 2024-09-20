/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AtMostOnceAfterSetUpTest {

    @Test
    fun setUpOnce_callbackExecutesOnce() {
        var callbackCounter = 0
        val callback: () -> Unit = { callbackCounter++ }
        val callbackAtMostOnce = AtMostOnceAfterSetUp(callback)

        val runnable = callbackAtMostOnce.setUp()
        assertThat(callbackCounter).isEqualTo(0)
        runnable.run()
        assertThat(callbackCounter).isEqualTo(1)
    }

    @Test
    fun setUpMultipleTimes_callbackExecutesOnce() {
        var callbackCounter = 0
        val callback: () -> Unit = { callbackCounter++ }
        val callbackAtMostOnce = AtMostOnceAfterSetUp(callback)

        val runnables =
            listOf(
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp(),
            )
        assertThat(callbackCounter).isEqualTo(0)
        runnables.forEach(Runnable::run)
        assertThat(callbackCounter).isEqualTo(1)
    }

    @Test
    fun setUpAgainAfterRun_callbackExecutesAgain() {
        var callbackCounter = 0
        val callback: () -> Unit = { callbackCounter++ }
        val callbackAtMostOnce = AtMostOnceAfterSetUp(callback)

        val runnables =
            listOf(
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp(),
                callbackAtMostOnce.setUp()
            )
        assertThat(callbackCounter).isEqualTo(0)
        runnables.forEach(Runnable::run)
        assertThat(callbackCounter).isEqualTo(1)

        val runnables2 = listOf(callbackAtMostOnce.setUp(), callbackAtMostOnce.setUp())
        assertThat(callbackCounter).isEqualTo(1)
        runnables2.forEach(Runnable::run)
        assertThat(callbackCounter).isEqualTo(2)
    }
}
