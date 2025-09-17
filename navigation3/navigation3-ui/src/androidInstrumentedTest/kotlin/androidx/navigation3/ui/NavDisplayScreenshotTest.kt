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

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.NavigationEventSwipeEdge
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class NavDisplayScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("navigation3/navigation3-ui")

    private val navHostTag = "NavHostTag"

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testNavDisplayPredictiveBackAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            NavDisplay(
                backStack = backStack,
                transitionSpec = {
                    slideInHorizontally { it / 2 } togetherWith slideOutHorizontally { -it / 2 }
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it / 2 }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                                Text(second, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testNavDisplayPredictiveBackAnimations")
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testNavDisplayPredictiveBackAnimationsRightSwipeEdge() {
        lateinit var backStack: MutableList<Any>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            NavDisplay(
                backStack = backStack,
                transitionSpec = {
                    slideInHorizontally { it / 2 } togetherWith slideOutHorizontally { -it / 2 }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    if (swipeEdge == NavigationEventSwipeEdge.Left) {
                        EnterTransition.None togetherWith slideOutHorizontally { it / 2 }
                    } else {
                        EnterTransition.None togetherWith slideOutHorizontally { -it / 2 }
                    }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                                Text(second, Modifier.size(50.dp))
                            }
                        }

                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_RIGHT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_RIGHT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_RIGHT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "testNavDisplayPredictiveBackAnimationsRightSwipeEdge",
            )
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testNavDisplayPredictiveBackAnimationsLeftSwipeEdge() {
        lateinit var backStack: MutableList<Any>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            NavDisplay(
                backStack = backStack,
                transitionSpec = {
                    slideInHorizontally { it / 2 } togetherWith slideOutHorizontally { -it / 2 }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    if (swipeEdge == NavigationEventSwipeEdge.Left) {
                        EnterTransition.None togetherWith slideOutHorizontally { it / 2 }
                    } else {
                        EnterTransition.None togetherWith slideOutHorizontally { -it / 2 }
                    }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                                Text(second, Modifier.size(50.dp))
                            }
                        }

                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "testNavDisplayPredictiveBackAnimationsLeftSwipeEdge",
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testNavigateZIndex() {
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack,
                // both screens slide left to right, entering screens should be on top
                transitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(first, Modifier.size(50.dp))
                            }
                        }
                    second ->
                        NavEntry(second) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(second, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(second) }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        // second screen should be on top with "second" text visible
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testNavigateZIndex")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testPopZIndex() {
        lateinit var backStack: MutableList<Any>
        val duration = 500
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(backStack, modifier = Modifier.testTag(navHostTag)) { key ->
                when (key) {
                    first ->
                        NavEntry(first) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(first, Modifier.size(50.dp))
                            }
                        }
                    second ->
                        NavEntry(
                            second,
                            // both screens slide right to left, exiting screen should be on top
                            metadata =
                                NavDisplay.popTransitionSpec {
                                    slideInHorizontally(tween(duration)) { it / 2 } togetherWith
                                        slideOutHorizontally(tween(duration)) { -it / 2 }
                                },
                        ) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(second, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.removeAt(1) }

        composeTestRule.mainClock.advanceTimeBy((duration / 2).toLong())

        // second screen should be on top with "second" text visible
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testPopZIndex")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testPopNavigateDuplicateZIndex() {
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(
                backStack,
                // both screens slide left to right, entering screen should be on top
                transitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first -> NavEntry(first) {}
                    second -> NavEntry(second) {}
                    third ->
                        NavEntry(third) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(third, Modifier.size(50.dp))
                            }
                        }
                    forth ->
                        NavEntry(forth) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Green),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(forth, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(forth) }
        assertThat(composeTestRule.onNodeWithText(forth).isDisplayed()).isTrue()
        assertThat(backStack).containsExactly(first, second, third, forth).inOrder()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(3)
            backStack.removeAt(2)
            backStack.removeAt(1)
            backStack.add(third)
            // resulting in (first, third)
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        // should be a navigate to third screen, with "third" text visible and on top
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testPopNavigateDuplicateZIndex")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testPopNavigateZIndex() {
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(
                backStack,
                // both screens slide left to right, entering screen should be on top
                transitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(first, Modifier.size(50.dp))
                            }
                        }
                    second -> NavEntry(second) {}
                    third ->
                        NavEntry(third) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(third, Modifier.size(50.dp))
                            }
                        }
                    forth ->
                        NavEntry(forth) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Green),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(forth, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backStack.removeAt(2)
            backStack.removeAt(1)
            backStack.add(forth)
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        assertThat(composeTestRule.onNodeWithText(forth).isDisplayed()).isTrue()

        // forth screen should be on top with "forth" text visible
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testPopNavigateZIndex")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testNavigateDuplicateZIndex() {
        lateinit var backStack: MutableList<Any>

        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack,
                // both screens slide left to right, entering screens should be on top
                transitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier.testTag(navHostTag),
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(first, Modifier.size(50.dp))
                            }
                        }
                    second ->
                        NavEntry(second) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(second, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backStack.add(first) }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        // first screen should be on top with "first" text visible
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testNavigateDuplicateZIndex")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testPopDuplicateZIndex() {
        lateinit var backStack: MutableList<Any>
        val duration = 200
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack,
                // both screens slide left to right, entering screens should be on top
                transitionSpec = {
                    slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier.testTag(navHostTag),
            ) { key ->
                when (key) {
                    first ->
                        NavEntry(first) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Blue),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(first, Modifier.size(50.dp))
                            }
                        }
                    second ->
                        NavEntry(
                            second,
                            // both screens slide right to left, exiting screen should be on top
                            metadata =
                                NavDisplay.popTransitionSpec {
                                    slideInHorizontally(tween(duration)) { it / 2 } togetherWith
                                        slideOutHorizontally(tween(duration)) { -it / 2 }
                                },
                        ) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Red),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(second, Modifier.size(50.dp))
                            }
                        }
                    third ->
                        NavEntry(third) {
                            Box(
                                Modifier.fillMaxSize().background(Color.Green),
                                contentAlignment = Alignment.TopEnd,
                            ) {
                                BasicText(third, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        // navigate one by one to register every screen's initial zIndex
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(third) }
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        // duplicate destination
        composeTestRule.runOnIdle { backStack.add(second) }
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            // pop the duplicate destination
            backStack.removeAt(backStack.lastIndex)
        }

        composeTestRule.mainClock.advanceTimeBy((duration / 2).toLong())
        // when navigating to "second" for a second time, its zIndex should have been updated
        // so that popping this duplicate would still go from a higher zIndex to lower zIndex,
        // meaning we should see the exiting screen on top with "second" text visible
        composeTestRule
            .onNodeWithTag(navHostTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testPopDuplicateZIndex2")
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
private const val forth = "forth"
