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

package androidx.compose.runtime.tooling

import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompositionRegistrationObserverWithUnconfinedDispatcherTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("ComposeTestRuleDispatcher")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>(UnconfinedTestDispatcher())

    // Regression test for b/434701720
    @Test
    fun forceRecompositionDuringInitialComposition() = runTest {
        var someState by mutableStateOf(true)

        setContent {
            val someStateValue = someState

            if (someStateValue) {
                someState = false
                // This is unnecessary cursed, but robolectric does that sometimes
                composeTestRule.mainClock.advanceTimeByFrame()
            }

            Text("$someStateValue", Modifier.testTag("text"))
        }

        composeTestRule.onNodeWithTag("text").assertTextEquals("true")
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent { content() }
    }
}
