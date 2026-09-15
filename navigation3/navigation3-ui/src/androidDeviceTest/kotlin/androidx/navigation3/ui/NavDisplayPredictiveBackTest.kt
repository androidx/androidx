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
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.kruth.assertThat
import androidx.navigation3.first
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.second
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayPredictiveBackTest {
    @get:Rule val composeTestRule = createComposeRule()

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

    @Test
    fun testPredictiveBackDuringForwardAnimation() {
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
                transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(1000, easing = LinearEasing),
                    ) togetherWith
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(1000, easing = LinearEasing),
                        )
                },
                popTransitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(1000, easing = LinearEasing),
                    ) togetherWith
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(1000, easing = LinearEasing),
                        )
                },
                predictivePopTransitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(1000, easing = LinearEasing),
                    ) togetherWith
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(1000, easing = LinearEasing),
                        )
                },
            ) { key ->
                NavEntry(key) { Box(modifier = Modifier.fillMaxSize()) { Text(key.toString()) } }
            }
        }

        composeTestRule.onNodeWithText(first).assertExists()

        // 1. Trigger forward navigation with clock paused and advance halfway (500ms / 1000ms)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(second) }
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val secondSceneLeftPositionAt500ms =
            composeTestRule.onNodeWithText(second).getUnclippedBoundsInRoot().left
        assertThat(secondSceneLeftPositionAt500ms).isGreaterThan(0.dp)

        // 2. Initiate predictive back gesture towards first (progress = 0.9f)
        composeTestRule.runOnIdle {
            input.backStarted(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.1f,
                    touchX = 0.1f,
                    touchY = 0.1f,
                )
            )
            input.backProgressed(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.9f,
                    touchX = 0.1f,
                    touchY = 0.1f,
                )
            )
        }

        // Allow spring catch-up animation to transition the in-flight slide to the gesture's offset
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val secondSceneLeftPositionAt90Percent =
            composeTestRule.onNodeWithText(second).getUnclippedBoundsInRoot().left
        val firstSceneLeftPositionAt90Percent =
            composeTestRule.onNodeWithText(first).getUnclippedBoundsInRoot().left

        // Because predictivePopTransitionSpec defines slide:
        // - second is being seeked towards its pop exit to the right (> position at 500ms).
        // - first is being seeked in from the left (< 0.dp).
        assertThat(secondSceneLeftPositionAt90Percent).isGreaterThan(secondSceneLeftPositionAt500ms)
        assertThat(firstSceneLeftPositionAt90Percent).isLessThan(0.dp)
        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        // 3. Cancel back gesture and verify it settles on second
        composeTestRule.runOnIdle { input.backCancelled() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertExists()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        assertThat(composeTestRule.onNodeWithText(second).getUnclippedBoundsInRoot().left)
            .isEqualTo(0.dp)
    }

    @Test
    fun testPredictiveBackHandoff() {
        lateinit var input: DirectNavigationEventInput
        lateinit var navEventDispatcher: NavigationEventDispatcher
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            navEventDispatcher =
                LocalNavigationEventDispatcherOwner.current!!.navigationEventDispatcher
            input = DirectNavigationEventInput()
            navEventDispatcher.addInput(input)
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                predictivePopTransitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(100, easing = LinearEasing),
                        initialOffsetX = { -it / 2 },
                    ) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(100, easing = LinearEasing),
                            targetOffsetX = { it / 2 },
                        )
                },
                popTransitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(1000, easing = LinearEasing),
                        initialOffsetX = { -it / 2 },
                    ) togetherWith
                        slideOutVertically(
                            animationSpec = tween(1000, easing = LinearEasing),
                            targetOffsetY = { it / 2 },
                        )
                },
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    first -> NavEntry(first) { Box(Modifier.fillMaxSize()) { Text(first) } }
                    second -> NavEntry(second) { Box(Modifier.fillMaxSize()) { Text(second) } }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle { backStack.add(second) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            input.backStarted(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5F,
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

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        val enterXAt50 = composeTestRule.onNodeWithText(first).fetchSemanticsNode().positionInRoot.x
        val exitXAt50 = composeTestRule.onNodeWithText(second).fetchSemanticsNode().positionInRoot.x

        assertWithMessage("exitX should be > 0 at 50%").that(exitXAt50).isGreaterThan(0f)
        assertWithMessage("enterX should be < 0 at 50%").that(enterXAt50).isLessThan(0f)

        composeTestRule.runOnIdle { input.backCompleted() }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        val enterX = composeTestRule.onNodeWithText(first).fetchSemanticsNode().positionInRoot.x
        val exitX = composeTestRule.onNodeWithText(second).fetchSemanticsNode().positionInRoot.x
        val exitY = composeTestRule.onNodeWithText(second).fetchSemanticsNode().positionInRoot.y

        assertWithMessage("exitX should not jump back to 0 during handoff")
            .that(exitX)
            .isGreaterThan(0f)
        assertWithMessage("enterX should not jump back to full left during handoff")
            .that(enterX)
            .isLessThan(0f)

        // Advance further to allow popTransitionSpec to take over and start animating vertically
        composeTestRule.mainClock.advanceTimeBy(100)

        val exitYLater =
            composeTestRule.onNodeWithText(second).fetchSemanticsNode().positionInRoot.y
        assertWithMessage("exitY should start animating downwards due to popTransitionSpec")
            .that(exitYLater)
            .isGreaterThan(exitY)

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }
}
