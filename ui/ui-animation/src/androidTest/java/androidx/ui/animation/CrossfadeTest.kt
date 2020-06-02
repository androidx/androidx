/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.ui.animation

import androidx.animation.DefaultDuration
import androidx.animation.TweenBuilder
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.foundation.Text
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.waitForIdle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@MediumTest
class CrossfadeTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = false)

    @Test
    fun crossfadeTest_showsContent() {
        composeTestRule.clockTestRule.pauseClock()

        composeTestRule.setContent {
            val showFirst by state { true }
            Crossfade(showFirst) {
                Text(if (it) First else Second)
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDuration.toLong())

        findByText(First).assertExists()
    }

    @Test
    fun crossfadeTest_disposesContentOnChange() {
        composeTestRule.clockTestRule.pauseClock()

        var showFirst by mutableStateOf(true)
        var disposed = false
        composeTestRule.setContent {
            Crossfade(showFirst) {
                Text(if (it) First else Second)
                onDispose {
                    disposed = true
                }
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDuration.toLong())

        runOnIdleCompose {
            showFirst = false
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(DefaultDuration.toLong())

        runOnIdleCompose {
            assertTrue(disposed)
        }

        findByText(First).assertDoesNotExist()
        findByText(Second).assertExists()
    }

    @Test
    fun crossfadeTest_durationCanBeModifierUsingAnimationBuilder() {
        composeTestRule.clockTestRule.pauseClock()

        val duration = 100L // smaller than default 300
        var showFirst by mutableStateOf(true)
        var disposed = false
        composeTestRule.setContent {
            Crossfade(showFirst, TweenBuilder<Float>().apply {
                this.duration = duration.toInt()
            }) {
                Text(if (it) First else Second)
                onDispose {
                    disposed = true
                }
            }
        }
        composeTestRule.clockTestRule.advanceClock(duration)

        runOnIdleCompose {
            showFirst = false
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(duration)

        runOnIdleCompose {
            assertTrue(disposed)
        }
    }

    @Test
    fun nullInitialValue() {
        composeTestRule.clockTestRule.pauseClock()
        var current by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            Crossfade(current) { value ->
                Text(if (value == null) First else Second)
            }
        }
        composeTestRule.clockTestRule.advanceClock(DefaultDuration.toLong())

        findByText(First).assertExists()
        findByText(Second).assertDoesNotExist()

        runOnIdleCompose {
            current = "other"
        }

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(DefaultDuration.toLong())

        findByText(First).assertDoesNotExist()
        findByText(Second).assertExists()
    }

    companion object {
        private const val First = "first"
        private const val Second = "second"
    }
}
