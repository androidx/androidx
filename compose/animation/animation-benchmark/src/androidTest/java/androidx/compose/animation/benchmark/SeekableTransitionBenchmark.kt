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

package androidx.compose.animation.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.SeekableTransitionState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SeekableTransitionBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    private enum class State {
        Visible,
        Gone,
    }

    /** Measures the cost of creating a new instance of [SeekableTransitionState]. */
    @Test
    fun instantiation() {
        benchmarkRule.measureRepeated { SeekableTransitionState(State.Visible) }
    }

    /** Measures the cost of using seekTo for a [SeekableTransitionState]. */
    @Test
    fun seekTo() {
        val seekableState = SeekableTransitionState(State.Visible)
        runBlocking {
            seekableState.snapTo(State.Visible)
            seekableState.seekTo(fraction = 0.1f, targetState = State.Gone)
        }
        benchmarkRule.measureRepeated {
            runBlocking {
                val fraction = if (seekableState.fraction > 0.4f) 0.2f else 0.5f
                seekableState.seekTo(fraction)
            }
        }
    }
}
