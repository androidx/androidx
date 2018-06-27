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

import android.support.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class BenchmarkStateTest {
    fun ms2ns(ms: Long): Long = TimeUnit.MILLISECONDS.toNanos(ms)

    @Test
    fun simple() {
        // would be better to mock the clock, but going with minimal changes for now
        val state = BenchmarkState()
        while (state.keepRunning()) {
            Thread.sleep(3)
            state.pauseTiming()
            Thread.sleep(5)
            state.resumeTiming()
        }
        val median = state.stats.median
        assertTrue("median $median should be between 2ms and 4ms",
                ms2ns(2) < median && median < ms2ns(4))
    }
}
