/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.navigation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.testing.TestNavHostController
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SwipeDismissableNavHostTest {
    @get:Rule val rule = createComposeRule(effectContext = StandardTestDispatcher())

    private lateinit var backPressedDispatcher: OnBackPressedDispatcher

    @Test
    fun supports_testtag() {
        rule.setContentWithBackPressedDispatcher { SwipeDismissWithNavigation() }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun navigates_to_next_level() {
        rule.setContentWithBackPressedDispatcher { SwipeDismissWithNavigation() }

        // Click to move to next destination.
        rule.onNodeWithText(START).performClick()

        // Should now display "next".
        rule.onNodeWithText(NEXT).assertExists()
    }

    @Test
    fun navigates_back_to_previous_level_after_swipe() {

        rule.setContentWithBackPressedDispatcher { SwipeDismissWithNavigation() }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()
        rule.swipeRight()

        // Should now display "start".
        rule.onNodeWithText(START).assertExists()
    }

    @Test
    fun does_not_navigate_back_to_previous_level_when_swipe_disabled() {
        rule.setContentWithBackPressedDispatcher {
            SwipeDismissWithNavigation(userSwipeEnabled = false)
        }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()
        rule.swipeRight()

        // Should still display "next".
        rule.onNodeWithText(NEXT).assertExists()
        rule.onNodeWithText(START).assertDoesNotExist()
    }

    @Test
    fun navigates_back_to_previous_level_with_back_button() {
        lateinit var navController: NavHostController

        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }
        // Move to next destination.
        rule.onNodeWithText(START).performClick()

        // Now trigger the back button
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        // Should now display "start".
        rule.onNodeWithText(START).assertExists()
        assertThat(navController.currentDestination?.route).isEqualTo(START)
    }

    @Test
    fun navigates_back_to_previous_level_with_back_button_previous_state_destroyed() {
        lateinit var navController: NavHostController

        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }
        // Move to next destination.
        rule.onNodeWithText(START).performClick()

        val nextEntry = navController.getBackStackEntry(NEXT)

        // Now trigger the back button
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        // Should now display "start".
        rule.onNodeWithText(START).assertExists()
        assertThat(nextEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun hides_previous_level_when_not_swiping() {
        rule.setContentWithBackPressedDispatcher { SwipeDismissWithNavigation() }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()

        // Should not display "start".
        rule.onNodeWithText(START).assertDoesNotExist()
    }

    @ExperimentalTestApi
    @Test
    fun displays_previous_screen_during_swipe_gesture() {
        rule.setContentWithBackPressedDispatcher {
            WithTouchSlop(0f) { SwipeDismissWithNavigation() }
        }

        // Click to move to next destination.
        rule.onNodeWithText(START).performClick()
        // Click and drag to begin a swipe gesture, but do not release the finger.
        rule.dragRight()

        // As the finger is still 'down', the background should be visible.
        rule.onNodeWithText(START).assertExists()
        // Assert that the foreground screen still holds the focus
        // Child is the one which has items and it needs to hold the focus to perform scrolling.
        rule.onNodeWithTag(TEST_TAG_NEXT).onChild().assertIsFocused()
    }

    @Test
    fun destinations_keep_saved_state() {
        val screenId = mutableStateOf(START)
        rule.setContentWithBackPressedDispatcher {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screenId) {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = START,
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    composable(START) {
                        screenId.value = START
                        var toggle by rememberSaveable { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column {
                                ToggleButton(
                                    checked = toggle,
                                    onCheckedChange = { toggle = !toggle },
                                    content = { Text(text = if (toggle) "On" else "Off") },
                                    modifier = Modifier.testTag("ToggleButton"),
                                )
                                Button(onClick = { navController.navigate(NEXT) }) { Text("Go") }
                            }
                        }
                    }
                    composable(NEXT) {
                        screenId.value = NEXT
                        CompactChip(onClick = {}, label = { Text(text = NEXT) })
                    }
                }
            }
        }

        rule.onNodeWithText("Off").performClick()
        rule.onNodeWithText("Go").performClick()
        rule.swipeRight()
        rule.onNodeWithText("On").assertExists()
    }

    @Test
    fun remembers_saved_state_on_two_screens() {
        val screenId = mutableStateOf(START)
        rule.setContentWithBackPressedDispatcher {
            val holder = rememberSaveableStateHolder()
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = START,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                composable(START) {
                    screenId.value = START
                    holder.SaveableStateProvider(START) {
                        var toggle by rememberSaveable { mutableStateOf(false) }
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ToggleButton(
                                checked = toggle,
                                onCheckedChange = { toggle = !toggle },
                                content = { Text(text = if (toggle) "On" else "Off") },
                                modifier = Modifier.testTag("ToggleButton"),
                            )
                            Button(onClick = { navController.navigate(NEXT) }) { Text("Go") }
                        }
                    }
                }
                composable(NEXT) {
                    screenId.value = NEXT
                    holder.SaveableStateProvider(NEXT) {
                        var counter by rememberSaveable { mutableStateOf(0) }
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Button(onClick = { ++counter }, modifier = Modifier.testTag(COUNTER)) {
                                Text("$counter")
                            }
                            Button(onClick = { navController.navigate(START) }) { Text("Jump") }
                        }
                    }
                }
            }
        }

        // Toggle from Off to On for the Start screen.
        rule.onNodeWithText("Off").performClick()
        rule.onNodeWithText("On").assertExists()
        // Go to the Next screen and increment the counter.
        rule.onNodeWithText("Go").performClick()
        rule.onNodeWithText("0").assertExists()
        rule.onNodeWithTag(COUNTER).performClick()
        rule.onNodeWithText("1").assertExists()
        // Jump to the Start screen - this is in a new place in the Nav hierarchy, so has new state.
        rule.onNodeWithText("Jump").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Off").assertExists()
        rule.swipeRight()
        // Next screen should still display the incremented counter.
        rule.onNodeWithText("1").assertExists()
        rule.swipeRight()
        // Start screen should still display 'On'
        rule.waitForIdle()
        rule.onNodeWithText("On").assertExists()
        // Going on to the Next screen again, this is a new instance of the Nav destination.
        rule.onNodeWithText("Go").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("0").assertExists()
    }

    @SuppressLint("LocalContextConfigurationRead")
    @Test
    fun clip_content_for_round_screens() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rule.setContent {
                val originalConfiguration = LocalConfiguration.current
                val originalContext = LocalContext.current

                val roundScreenConfiguration =
                    remember(originalConfiguration) {
                        Configuration(originalConfiguration).apply {
                            screenLayout = Configuration.SCREENLAYOUT_ROUND_YES
                        }
                    }
                originalContext.resources.configuration.updateFrom(roundScreenConfiguration)

                CompositionLocalProvider(
                    LocalContext provides originalContext,
                    LocalConfiguration provides roundScreenConfiguration,
                ) {
                    val navController = rememberSwipeDismissableNavController()
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = START,
                        modifier = Modifier.background(Color.Blue).testTag(TEST_TAG),
                    ) {
                        composable(START) {
                            // Create a full screen box inside SwipeDismissableNavHost with yellow
                            // background
                            Box(modifier = Modifier.fillMaxSize().background(Color.Yellow))
                        }
                    }
                }
            }

            // Quick check that the content, which is a full-screen yellow box is clipped. If it is
            // clipped, then background color (Blue) should be visible
            rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(Color.Blue)
        }
    }

    @Test
    fun updates_lifecycle_for_initial_destination() {
        lateinit var navController: NavHostController
        rule.mainClock.autoAdvance = false
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        val entry = navController.getBackStackEntry(START)

        rule.runOnIdle {
            assertThat(entry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_navigation() {
        lateinit var navController: NavHostController
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        // Click to move to next destination then swipe back.
        rule.onNodeWithText(START).performClick()

        rule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_navigation_and_swipe_back() {
        lateinit var navController: NavHostController
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        // Click to move to next destination then swipe back.
        rule.onNodeWithText(START).performClick()
        rule.swipeRight()

        rule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_popping_back_stack() {
        lateinit var navController: NavHostController
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        rule.waitForIdle()
        rule.onNodeWithText(START).performClick()

        rule.runOnIdle { navController.popBackStack() }

        rule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun provides_access_to_current_backstack_entry_state() {
        lateinit var navController: NavHostController
        lateinit var backStackEntry: State<NavBackStackEntry?>
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            backStackEntry = navController.currentBackStackEntryAsState()
            SwipeDismissWithNavigation(navController)
        }

        rule.onNodeWithText(START).performClick()

        rule.runOnIdle { assertThat(backStackEntry.value?.destination?.route).isEqualTo(NEXT) }
    }

    @Test
    fun testNavHostController_starts_at_default_destination() {
        lateinit var navController: TestNavHostController

        rule.setContentWithBackPressedDispatcher {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(WearNavigator())

            SwipeDismissWithNavigation(navController)
        }

        rule.onNodeWithText(START).assertExists()
    }

    @Test
    fun testNavHostController_sets_current_destination() {
        lateinit var navController: TestNavHostController

        rule.setContentWithBackPressedDispatcher {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(WearNavigator())

            SwipeDismissWithNavigation(navController)
            navController.setCurrentDestination(NEXT)
        }

        rule.onNodeWithText(NEXT).assertExists()
    }

    // The following test verifies the PredictiveBackNavHost animation and targets API 36+.
    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun press_back_during_animation_does_not_reset_animation() {
        lateinit var navController: NavHostController
        rule.setContentWithBackPressedDispatcher {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }
        // Click to move to next destination then swipe back.
        rule.onNodeWithText(START).performClick()

        rule.waitForIdle()

        rule.mainClock.autoAdvance = false

        val navHostWidth = rule.onNodeWithTag(TEST_TAG).fetchSemanticsNode().size.width

        rule.runOnIdle { backPressedDispatcher.onBackPressed() }

        // Animation now starts. Since we control animation clock manually, here we will
        // fast-forward few frames.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        var previousLeft =
            rule.onNodeWithTag(TEST_TAG_NEXT_CONTAINER).fetchSemanticsNode().boundsInWindow.left

        // Press back while animation is running
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }

        // Make sure animation continues after pressing back button
        do {
            rule.mainClock.advanceTimeByFrame()

            val currentLeft =
                rule.onNodeWithTag(TEST_TAG_NEXT_CONTAINER).fetchSemanticsNode().boundsInWindow.left
            assertTrue(previousLeft < currentLeft)
            previousLeft = currentLeft
        } while (currentLeft < 0.9 * navHostWidth)
        rule.mainClock.autoAdvance = true
    }

    @Composable
    fun SwipeDismissWithNavigation(
        navController: NavHostController = rememberSwipeDismissableNavController(),
        userSwipeEnabled: Boolean = true,
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = START,
            modifier = Modifier.testTag(TEST_TAG),
            userSwipeEnabled = userSwipeEnabled,
        ) {
            composable(START) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ScalingLazyColumn(modifier = Modifier.testTag(TEST_TAG_START)) {
                        item {
                            CompactChip(
                                onClick = { navController.navigate(NEXT) },
                                label = { Text(text = START) },
                            )
                        }
                    }
                }
            }
            composable(NEXT) {
                Box(
                    modifier = Modifier.testTag(TEST_TAG_NEXT_CONTAINER).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ScalingLazyColumn(modifier = Modifier.testTag(TEST_TAG_NEXT)) {
                        item { Text(NEXT) }
                    }
                }
            }
        }
    }

    /**
     * Depending on API level, either swipes right on the view with TEST_TAG, or emulates
     * system-level swipe using backPressedDispatcher
     */
    private fun ComposeContentTestRule.swipeRight() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runOnIdle {
                backPressedDispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1f, 0.1f, 0.1f, BackEvent.EDGE_LEFT)
                )
                backPressedDispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.1f, 0.1f, 0.8f, BackEvent.EDGE_LEFT)
                )
                backPressedDispatcher.onBackPressed()
            }
        } else {
            onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        }
    }

    /**
     * Dragss without releasing the finger.
     *
     * Depending on API level, either drags right on the view with TEST_TAG, or emulates
     * system-level drag using backPressedDispatcher
     */
    private fun ComposeContentTestRule.dragRight() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runOnIdle {
                backPressedDispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1f, 0.1f, 0.1f, BackEvent.EDGE_LEFT)
                )
                backPressedDispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.1f, 0.1f, 0.8f, BackEvent.EDGE_LEFT)
                )
            }
        } else {
            rule
                .onNodeWithTag(TEST_TAG)
                .performTouchInput({
                    down(Offset(x = 0f, y = height / 2f))
                    moveTo(Offset(x = width / 4f, y = height / 2f))
                })
        }
    }

    private fun ComposeContentTestRule.setContentWithBackPressedDispatcher(
        composable: @Composable () -> Unit
    ) {
        backPressedDispatcher = OnBackPressedDispatcher()
        val dispatcherOwner =
            object : OnBackPressedDispatcherOwner, LifecycleOwner by TestLifecycleOwner() {
                override val onBackPressedDispatcher = backPressedDispatcher
            }
        setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
                composable()
            }
        }
    }
}

private const val NEXT = "next"
private const val START = "start"
private const val COUNTER = "counter"
private const val TEST_TAG = "test-item"
private const val TEST_TAG_NEXT = "test-tag-next"
private const val TEST_TAG_NEXT_CONTAINER = "test-tag-next-container"

private const val TEST_TAG_START = "test-tag-start"
