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

package androidx.compose.ui.test

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.TestCounter
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@SmallTest
@OptIn(ExperimentalTestApi::class)
class StandardTestDispatcherPhaseOrderingTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun singlePass() {
        assumeTrue(ComposeUiTestFlags.isStandardTestDispatcherSupportEnabled)
        val counter = TestCounter()
        rule.setContent {
            // This should never recompose.
            counter.expect(1)

            LaunchedEffect(Unit) {
                // Scheduled during apply phase on a confined dispatcher, so runs after layout
                counter.expect(4)
                withFrameNanos {
                    counter.expect(5)
                    launch {
                        // No continuations resumed during a frame should be dispatched until after
                        // the frame callbacks finish running.
                        counter.expect(7)
                    }
                    counter.expect(6)
                }
                counter.expect(8)
            }

            Layout(content = {}) { _, _ ->
                counter.expect(2)
                layout(1, 1) { counter.expect(3) }
            }
        }
    }

    @Test
    fun frameCallbackRestartsLayout() {
        assumeTrue(ComposeUiTestFlags.isStandardTestDispatcherSupportEnabled)
        val counter = TestCounter()
        var firstPass by mutableStateOf(true)
        var layoutCount = 0

        rule.setContent {
            LaunchedEffect(Unit) {
                // Scheduled during apply phase on a confined dispatcher, so starts after layout
                counter.expect(3)
                withFrameNanos {
                    // This won't run until the 2nd frame, so the first layout happens before it.
                    counter.expect(4)
                    firstPass = false
                }
                counter.expect(7)
            }

            Layout(content = {}) { _, _ ->
                counter.expect(if (firstPass) 1 else 5)
                layoutCount++
                layout(1, 1) { counter.expect(if (firstPass) 2 else 6) }
            }
        }

        rule.runOnIdle { assertThat(layoutCount).isEqualTo(2) }
    }
}
