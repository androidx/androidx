/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.runtime.benchmark

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized

@LargeTest
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EffectsBenchmark(private val count: Int) : ComposeBenchmarkBase() {

    @Test
    fun launchedEffect_add() = runBlockingTestWithFrameClock {
        var effectsCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose { repeat(effectsCount) { LaunchedEffect(Unit) { seen++ } } }
            update { effectsCount = count }
            reset {
                effectsCount = 0
                assertEquals("Didn't see the right number of effects", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun launchedEffect_remove() = runBlockingTestWithFrameClock {
        var effectsCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose {
                repeat(effectsCount) {
                    LaunchedEffect(Unit) {
                        try {
                            awaitCancellation()
                        } catch (e: CancellationException) {
                            seen++
                        }
                    }
                }
            }
            update { effectsCount = 0 }
            reset {
                effectsCount = count
                assertEquals("Didn't see the right number of effects", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun disposableEffect_add() = runBlockingTestWithFrameClock {
        var effectsCount by mutableStateOf(0)
        measureRecompose {
            var seen = 0
            compose {
                repeat(effectsCount) {
                    DisposableEffect(Unit) {
                        seen++
                        onDispose {}
                    }
                }
            }
            update { effectsCount = count }
            reset {
                effectsCount = 0
                assertEquals("Didn't see the right number of effects", seen, count)
                seen = 0
            }
        }
    }

    @Test
    fun disposableEffect_remove() = runBlockingTestWithFrameClock {
        var effectsCount by mutableStateOf(count)
        measureRecompose {
            var seen = 0
            compose { repeat(effectsCount) { DisposableEffect(Unit) { onDispose { seen++ } } } }
            update { effectsCount = 0 }
            reset {
                effectsCount = count
                assertEquals("Didn't see the right number of effects", seen, count)
                seen = 0
            }
        }
    }

    companion object {
        @Parameterized.Parameters(name = "count={0}")
        @JvmStatic
        fun parameters() = listOf<Array<Any?>>(arrayOf(1), arrayOf(10), arrayOf(100))
    }
}
