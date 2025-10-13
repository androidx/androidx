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

package androidx.navigation3.ui

import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.CardStackSceneStrategy.Companion.CARD_KEY
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatedTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNavigateAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(first)
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backStack.add(second) }

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 2
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNestedPredictiveBackAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            NavDisplay(backStack = backStack, sceneStrategy = CardStackSceneStrategy()) {
                when (it) {
                    first -> NavEntry(first, metadata = mapOf(CARD_KEY to first)) { RedBox(first) }
                    second ->
                        NavEntry(second, metadata = mapOf(CARD_KEY to first)) { BlueBox(second) }
                    third ->
                        NavEntry(third, metadata = mapOf(CARD_KEY to first)) { GreenBox(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        // animating out, text has moved off screen but node still exists
        composeTestRule.onNodeWithText(third).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(third).assertExists()

        backPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        // animate out complete, node removed
        composeTestRule.onNodeWithText(third).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
    }

    @Test
    fun testNavigateInterruptedAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle
        lateinit var thirdLifecycle: Lifecycle

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(first) }
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(second) }
                        }
                    third ->
                        NavEntry(third) {
                            thirdLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(third) }
                        }

                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backStack.add(second) }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        // interrupt current navigation by navigating to third
        composeTestRule.runOnIdle { backStack.add(third) }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testPopInterruptedAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle
        lateinit var thirdLifecycle: Lifecycle

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(first) }
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(second) }
                        }
                    third ->
                        NavEntry(third) {
                            thirdLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(third) }
                        }

                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false

        // pop third
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        // interrupt pop third by pop second as well
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testPopNavigateInterruptedAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle
        lateinit var thirdLifecycle: Lifecycle

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(first) }
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(second) }
                        }
                    third ->
                        NavEntry(third) {
                            thirdLifecycle = LocalLifecycleOwner.current.lifecycle
                            Box(contentAlignment = Alignment.Center) { Text(third) }
                        }

                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false

        // pop second
        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        // interrupt pop by navigating to third
        composeTestRule.runOnIdle { backStack.add(third) }

        // advance a third between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 3
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertExists()
        assertThat(thirdLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNavigateAnimationsImmutableBackStack() {
        lateinit var backStack: List<String>
        lateinit var backStackState: MutableState<Int>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            backStack =
                when (backStackState.value) {
                    1 -> {
                        listOf(first)
                    }
                    else -> {
                        listOf(first, second)
                    }
                }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(first)
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backStackState.value = 2 }

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 2
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testNavigateAnimationsCustom() {
        lateinit var backStack: MutableList<Any>

        composeTestRule.mainClock.autoAdvance = false
        val customDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND * 2

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(
                            second,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(customDuration)) togetherWith
                                        fadeOut(tween(customDuration))
                                },
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backStack.add(second) }

        // advance past the default duration but not the custom duration
        composeTestRule.mainClock.advanceTimeBy(
            ((DEFAULT_TRANSITION_DURATION_MILLISECOND * 3 / 2)).toLong()
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertExists()
    }

    @Test
    fun testPopAnimationsCustom() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(
                            second,
                            metadata =
                                NavDisplay.popTransitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backStack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
    }

    @Test
    fun testPopAnimationsImmutableBackStack() {
        lateinit var backStack: List<String>
        lateinit var backStackState: MutableState<Int>
        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            backStack =
                when (backStackState.value) {
                    1 -> {
                        listOf(first, second)
                    }
                    else -> {
                        listOf(first)
                    }
                }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStackState.value = 2 }

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(
            (DEFAULT_TRANSITION_DURATION_MILLISECOND / 2).toLong()
        )

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backStack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
    }

    @Test
    fun testPopMultiple() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            metadata =
                                NavDisplay.popTransitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(2)
            backStack.removeAt(1)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backStack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
    }

    @Test
    fun testPopNavigate() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(1)
            backStack.add(third)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentrePop() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentreNavigate() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(1, second) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, second, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentrePopAndEndPop() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third, fourth) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            third,
                            NavDisplay.transitionSpec {
                                fadeIn(tween(testDuration)) togetherWith
                                    fadeOut(tween(testDuration))
                            },
                        ) {
                            Text(third)
                        }
                    fourth -> NavEntry(fourth) { Text(fourth) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second, third, fourth)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(3)
            backStack.removeAt(1)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        composeTestRule.onNodeWithText(fourth).assertDoesNotExist()
    }

    @Test
    fun testCentrePopAndEndNavigate() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    fourth ->
                        NavEntry(
                            fourth,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(1)
            backStack.add(fourth)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, third, fourth)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
    }

    @Test
    fun testCentreNavigateAndEndPop() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, third, fourth) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third ->
                        NavEntry(
                            third,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(third)
                        }
                    fourth -> NavEntry(fourth) { Text(fourth) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, third, fourth)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(2)
            backStack.add(1, second)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, second, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        composeTestRule.onNodeWithText(fourth).assertDoesNotExist()
    }

    @Test
    fun testCentreNavigateAndEndNavigate() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    fourth ->
                        NavEntry(
                            fourth,
                            fourth,
                            NavDisplay.transitionSpec {
                                fadeIn(tween(testDuration)) togetherWith
                                    fadeOut(tween(testDuration))
                            },
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.add(1, second)
            backStack.add(fourth)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, second, third, fourth)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
    }

    @Test
    fun testSameStack() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(
                            second,
                            second,
                            NavDisplay.transitionSpec {
                                fadeIn(tween(testDuration)) togetherWith
                                    fadeOut(tween(testDuration))
                            },
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(1)
            backStack.add(second)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, second)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }

    @Test
    fun testPoppedEntryIsAnimated() {
        lateinit var backstack: MutableList<Any>
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second) }
            NavDisplay(backstack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            Box(Modifier.fillMaxSize().background(Color.Red)) { Text(second) }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }

    @Test
    fun testPoppedEntryIsWrapped() {
        lateinit var backstack: MutableList<Any>
        val LocalHasProvidedToEntry = compositionLocalOf { false }
        val provider =
            NavEntryDecorator<Any> { entry ->
                CompositionLocalProvider(LocalHasProvidedToEntry provides true) { entry.Content() }
            }
        var secondEntryIsWrapped = false
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second) }
            NavDisplay(backstack, entryDecorators = listOf(provider)) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            secondEntryIsWrapped = LocalHasProvidedToEntry.current
                            Box(Modifier.fillMaxSize().background(Color.Red)) { Text(second) }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        assertTrue(secondEntryIsWrapped)
    }

    @Test
    fun testDuplicateLastEntry() {
        lateinit var backStack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backStack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(
                            second,
                            metadata =
                                NavDisplay.transitionSpec {
                                    fadeIn(tween(testDuration)) togetherWith
                                        fadeOut(tween(testDuration))
                                },
                        ) {
                            Text(second)
                        }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backStack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(second) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(first, second, third, second)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun testSharedElement() {
        lateinit var backStack: MutableList<Any>
        var transitionScope: SharedTransitionScope? = null
        val sharedStates = mutableSetOf<SharedContentState>()
        val sharedContentStateKey = 1
        val sharedText = "shared text"
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            SharedTransitionLayout {
                NavDisplay(backStack = backStack) {
                    transitionScope = this
                    when (it) {
                        first ->
                            NavEntry(first) {
                                Text(
                                    sharedText,
                                    Modifier.sharedElement(
                                        rememberSharedContentState(sharedContentStateKey).also {
                                            sharedStates.add(it)
                                        },
                                        animatedVisibilityScope =
                                            LocalNavAnimatedContentScope.current,
                                    ),
                                )
                            }
                        second ->
                            NavEntry(second) {
                                Text(
                                    sharedText,
                                    Modifier.sharedElement(
                                        rememberSharedContentState(sharedContentStateKey).also {
                                            sharedStates.add(it)
                                        },
                                        animatedVisibilityScope =
                                            LocalNavAnimatedContentScope.current,
                                    ),
                                )
                            }
                        else -> error("Invalid key passed")
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sharedText).assertIsDisplayed()
        sharedStates.forEach { state -> assertFalse(state.isMatchFound) }

        sharedStates.clear()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.mainClock.advanceTimeBy(
            (DEFAULT_TRANSITION_DURATION_MILLISECOND / 4).toLong()
        )

        composeTestRule.onAllNodesWithText(sharedText).assertCountEquals(2)
        assertTrue(transitionScope?.isTransitionActive == true)
        sharedStates.forEach { state -> assertTrue(state.isMatchFound) }
    }

    @Test
    fun testPopLifecycle() {
        lateinit var backStack: MutableList<Any>
        lateinit var firstLifecycle: Lifecycle
        lateinit var secondLifecycle: Lifecycle

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            firstLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(first)
                        }
                    second ->
                        NavEntry(second) {
                            secondLifecycle = LocalLifecycleOwner.current.lifecycle
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(backStack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(1) }

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(
            (DEFAULT_TRANSITION_DURATION_MILLISECOND / 2).toLong()
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        composeTestRule.onNodeWithText(second).assertExists()
        assertThat(secondLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backStack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        assertThat(firstLifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
private const val fourth = "fourth"
