/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.annotation.UiThreadTest
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
class RememberSaveableBenchmark : ComposeBenchmarkBase() {
    @UiThreadTest
    @Test
    fun rememberSaveable_1() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            @Suppress("UNUSED_VARIABLE")
            val i: Int = rememberSaveable {
                10
            }
        }
    }

    @UiThreadTest
    @Test
    fun rememberSaveable_10() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            repeat(10) {
                @Suppress("UNUSED_VARIABLE")
                val i: Int = rememberSaveable {
                    10
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun rememberSaveable_100() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            repeat(100) {
                @Suppress("UNUSED_VARIABLE")
                val i: Int = rememberSaveable {
                    10
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun rememberSaveable_mutable_1() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            @Suppress("UNUSED_VARIABLE")
            val i = rememberSaveable(stateSaver = autoSaver()) {
                mutableStateOf(10)
            }
        }
    }

    @UiThreadTest
    @Test
    fun rememberSaveable_mutable_10() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            repeat(10) {
                @Suppress("UNUSED_VARIABLE")
                val i = rememberSaveable(stateSaver = autoSaver()) {
                    mutableStateOf(10)
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun rememberSaveable_mutable_100() = runBlockingTestWithFrameClock {
        measureComposeFocused {
            repeat(100) {
                @Suppress("UNUSED_VARIABLE")
                val i = rememberSaveable(stateSaver = autoSaver()) {
                    mutableStateOf(10)
                }
            }
        }
    }
}
