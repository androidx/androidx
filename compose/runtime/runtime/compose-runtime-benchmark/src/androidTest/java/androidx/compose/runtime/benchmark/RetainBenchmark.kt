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

package androidx.compose.runtime.benchmark

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledRetainScope
import androidx.compose.runtime.LocalRetainScope
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.RetainObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RetainBenchmark : ComposeBenchmarkBase() {

    @Test
    fun retain_newValues100() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        measureCompose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) {
                values.forEach { use(retain { it }) }
            }
        }
    }

    @Test
    fun retain_newValues100_withKeys() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        measureCompose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) {
                values.forEach { use(retain("A", "B") { it }) }
            }
        }
    }

    @Test
    fun retain_exitComposition100() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        var includeContent by mutableStateOf(true)
        measureRecompose {
            compose {
                CompositionLocalProvider(LocalRetainScope provides retainScope) {
                    if (includeContent) {
                        values.forEach { use(retain { it }) }
                    }
                }
            }
            update {
                includeContent = false
                retainScope.startKeepingExitedValues()
            }
            reset {
                includeContent = true
                retainScope.stopKeepingExitedValues()
            }
        }
    }

    @Test
    fun retain_abandonedFromComposition100() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        var includeContent by mutableStateOf(true)
        measureRecompose {
            compose {
                CompositionLocalProvider(LocalRetainScope provides retainScope) {
                    if (includeContent) {
                        values.forEach { use(retain { it }) }
                    }
                }
            }
            update { includeContent = false }
            reset { includeContent = true }
        }
    }

    @Test
    fun remember_newValues100() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        measureCompose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) {
                values.forEach { use(remember { it }) }
            }
        }
    }

    @Test
    fun remember_newValues100_withKeys() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        measureCompose {
            CompositionLocalProvider(LocalRetainScope provides retainScope) {
                values.forEach { use(remember("A", "B") { it }) }
            }
        }
    }

    @Test
    fun remember_forget100() = runBlockingTestWithFrameClock {
        val values = List(100) { ObserverString(it.toString()) }
        val retainScope = ControlledRetainScope()
        var includeContent by mutableStateOf(true)
        measureRecompose {
            compose {
                CompositionLocalProvider(LocalRetainScope provides retainScope) {
                    if (includeContent) {
                        values.forEach { use(remember { it }) }
                    }
                }
            }
            update {
                includeContent = false
                retainScope.startKeepingExitedValues()
            }
            reset {
                includeContent = true
                retainScope.stopKeepingExitedValues()
            }
        }
    }

    private fun use(value: Any?) {}

    private class ObserverString(private val value: String) : RetainObserver, RememberObserver {
        override fun onRetained() {}

        override fun onEnteredComposition() {}

        override fun onExitedComposition() {}

        override fun onRetired() {}

        override fun onRemembered() {}

        override fun onForgotten() {}

        override fun onAbandoned() {}

        override fun toString() = value
    }
}
