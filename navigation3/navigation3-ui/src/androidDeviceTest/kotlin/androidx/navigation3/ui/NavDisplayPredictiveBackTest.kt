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

package androidx.navigation3.ui

import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayPredictiveBackTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testStateIsRestoredOnBackPressedPredictiveBack() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var numberOnScreen2: MutableState<Int>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                        }
                    second ->
                        NavEntry(second) {
                            numberOnScreen2 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen2: ${numberOnScreen2.value}")
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 2").that(numberOnScreen1.value).isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2.value).isEqualTo(0)
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 4").that(numberOnScreen2.value).isEqualTo(4)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen2: 4").isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()
    }

    @Test
    fun testStateIsRestoredOnNavEventPredictiveBack() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var numberOnScreen2: MutableState<Int>
        lateinit var navEventDispatcher: NavigationEventDispatcher
        lateinit var input: DirectNavigationEventInput
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            navEventDispatcher =
                LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher
            input = DirectNavigationEventInput()
            navEventDispatcher.addInput(input)
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                        }
                    second ->
                        NavEntry(second) {
                            numberOnScreen2 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen2: ${numberOnScreen2.value}")
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 2").that(numberOnScreen1.value).isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2.value).isEqualTo(0)
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 4").that(numberOnScreen2.value).isEqualTo(4)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen2: 4").isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            input.backStarted(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.1F,
                    touchX = 0.1F,
                    touchY = 0.1F,
                )
            )
            input.backProgressed(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5F,
                    touchX = 0.1F,
                    touchY = 0.1F,
                )
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { input.backCompleted() }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()
    }

    @Test
    fun verifyZIndexAfterInterruptedBackNavigation() {
        var clicksOnA = 0
        var clicksOnB = 0
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    first ->
                        NavEntry(first) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().background(Color.Red).clickable {
                                        clicksOnA++
                                    }
                            ) {
                                Text(first)
                            }
                        }
                    second ->
                        NavEntry(second) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().background(Color.Blue).clickable {
                                        clicksOnB++
                                    }
                            ) {
                                Text(second)
                            }
                        }
                    else -> error("Unknown key")
                }
            }
        }

        // 1. Start at A.
        composeTestRule.onNodeWithText(first).assertExists()

        // 2. Navigate A -> B.
        composeTestRule.runOnIdle { backStack.add(second) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertExists()

        // 3. Navigate B -> A (Back).
        // We need to interrupt this.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        // Advance slightly to start transition (B exiting, A entering)
        composeTestRule.mainClock.advanceTimeBy(100)

        // 4. Interrupt: Navigate A -> B (Forward) AGAIN.
        // We are effectively cancelling the back nav and going back to B.
        composeTestRule.runOnIdle { backStack.add(second) }

        // Let the transition to B finish.
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Now we are at B.
        composeTestRule.onNodeWithText(second).assertExists()

        // 5. Navigate B -> A (Back) again.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        // Advance slightly to be in the middle of B -> A.
        // B should be exiting (on top), A should be entering (below).
        composeTestRule.mainClock.advanceTimeBy(100)

        // Click on the center of the screen.
        // If B is on top (correct), B gets the click.
        composeTestRule.onNodeWithText(second).performClick()
        Truth.assertThat(clicksOnA).isEqualTo(0)
        Truth.assertThat(clicksOnB).isGreaterThan(0)
        composeTestRule.mainClock.autoAdvance = true
    }
}

private const val first = "first"
private const val second = "second"
