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

import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Text
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@MediumTest
class CrossfadeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = false)

    @Test
    fun crossfadeTest_showsContent() {
        composeTestRule.setContent {
            var showFirst by state { true }
            Crossfade(showFirst) {
                Text(if (it) First else Second)
            }
        }
        composeTestRule.clockTestRule.advanceClock(300)

        findByText(First).assertExists()
    }

    @Test
    fun crossfadeTest_disposesContentOnChange() {
        var showFirst by mutableStateOf(true)
        val disposeLatch = CountDownLatch(1)
        composeTestRule.setContent {

            Crossfade(showFirst) {
                Text(if (it) First else Second)
                onDispose {
                    disposeLatch.countDown()
                }
            }
        }
        composeTestRule.clockTestRule.advanceClock(300)

        composeTestRule.runOnIdleCompose {
            showFirst = false
        }
        composeTestRule.clockTestRule.advanceClock(300)

        disposeLatch.await(5, TimeUnit.SECONDS)

        findByText(First).assertDoesNotExist()
        findByText(Second).assertExists()
    }

    companion object {
        private const val First = "first"
        private const val Second = "second"
    }
}