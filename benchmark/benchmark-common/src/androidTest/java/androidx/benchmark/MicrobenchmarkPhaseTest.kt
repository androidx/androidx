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

package androidx.benchmark

import androidx.benchmark.MicrobenchmarkPhase.LoopMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MicrobenchmarkPhaseTest {
    @Test
    fun loopMode_getIterations() {
        val durationLoopMode = LoopMode.Duration(1_000_000)

        // validate division
        assertEquals(1, durationLoopMode.getIterations(1_000_000))
        assertEquals(1_000, durationLoopMode.getIterations(1_000))
        assertEquals(1_000_000, durationLoopMode.getIterations(1))

        // low boundary condition
        assertEquals(LoopMode.MIN_TEST_ITERATIONS, durationLoopMode.getIterations(1_000_000_000))

        // high boundary condition
        // Note that 0 is allowed due to clock imprecision
        // E.g. on Mako API 17, have observed 30us granularity in System.nanoTime()
        assertEquals(LoopMode.MAX_TEST_ITERATIONS, durationLoopMode.getIterations(0))

        // fail - warmup hasn't occurred
        assertFailsWith<IllegalStateException> {
            durationLoopMode.getIterations(-1)
        }
    }
}
