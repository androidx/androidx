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

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class SwipeDismissableNavHostTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            SwipeDismissWithNavigation()
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun navigates_to_next_level() {
        rule.setContentWithTheme {
            SwipeDismissWithNavigation()
        }

        // Click to move to next destination.
        rule.onNodeWithText(START).performClick()

        // Should now display "next".
        rule.onNodeWithText(NEXT).assertExists()
    }

    @Test
    fun navigates_back_to_previous_level_after_swipe() {
        rule.setContentWithTheme {
            SwipeDismissWithNavigation()
        }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        // Should now display "start".
        rule.onNodeWithText(START).assertExists()
    }

    @Test
    fun does_not_navigate_back_to_previous_level_when_swipe_disabled() {
        rule.setContentWithTheme {
            SwipeDismissWithNavigation(userSwipeEnabled = false)
        }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        // Should still display "next".
        rule.onNodeWithText(NEXT).assertExists()
        rule.onNodeWithText(START).assertDoesNotExist()
    }

    @Test
    fun navigates_back_to_previous_level_with_back_button() {
        val onBackPressedDispatcher = OnBackPressedDispatcher()
        val dispatcherOwner =
            object : OnBackPressedDispatcherOwner, LifecycleOwner by TestLifecycleOwner() {
                override val onBackPressedDispatcher = onBackPressedDispatcher
            }
        lateinit var navController: NavHostController

        rule.setContentWithTheme {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
                navController = rememberSwipeDismissableNavController()
                SwipeDismissWithNavigation(navController)
            }
        }
        // Move to next destination.
        rule.onNodeWithText(START).performClick()

        // Now trigger the back button
        rule.runOnIdle {
            onBackPressedDispatcher.onBackPressed()
        }
        rule.waitForIdle()

        // Should now display "start".
        rule.onNodeWithText(START).assertExists()
        assertThat(navController.currentDestination?.route).isEqualTo(START)
    }

    @Test
    fun hides_previous_level_when_not_swiping() {
        rule.setContentWithTheme {
            SwipeDismissWithNavigation()
        }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(START).performClick()

        // Should not display "start".
        rule.onNodeWithText(START).assertDoesNotExist()
    }

    @ExperimentalTestApi
    @Test
    fun displays_previous_screen_during_swipe_gesture() {
        rule.setContentWithTheme {
            WithTouchSlop(0f) {
                SwipeDismissWithNavigation()
            }
        }

        // Click to move to next destination.
        rule.onNodeWithText(START).performClick()
        // Click and drag to being a swipe gesture, but do not release the finger.
        rule.onNodeWithTag(TEST_TAG).performTouchInput(
            {
                down(Offset(x = 0f, y = height / 2f))
                moveTo(Offset(x = width / 4f, y = height / 2f))
            }
        )

        // As the finger is still 'down', the background should be visible.
        rule.onNodeWithText(START).assertExists()
    }

    @Test
    fun destinations_keep_saved_state() {
        val screenId = mutableStateOf(START)
        rule.setContentWithTheme {
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
                            contentAlignment = Alignment.Center
                        ) {
                            Column {
                                ToggleButton(
                                    checked = toggle,
                                    onCheckedChange = {
                                        toggle = !toggle
                                    },
                                    content = { Text(text = if (toggle) "On" else "Off") },
                                    modifier = Modifier.testTag("ToggleButton"),
                                )
                                Button(
                                    onClick = { navController.navigate(NEXT) },
                                ) {
                                    Text("Go")
                                }
                            }
                        }
                    }
                    composable(NEXT) {
                        screenId.value = NEXT
                        CompactChip(
                            onClick = {},
                            label = { Text(text = NEXT) }
                        )
                    }
                }
            }
        }

        rule.onNodeWithText("Off").performClick()
        rule.onNodeWithText("Go").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithText("On").assertExists()
    }

    @Test
    fun remembers_saved_state_on_two_screens() {
        val screenId = mutableStateOf(START)
        rule.setContentWithTheme {
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ToggleButton(
                                    checked = toggle,
                                    onCheckedChange = {
                                        toggle = !toggle
                                    },
                                    content = { Text(text = if (toggle) "On" else "Off") },
                                    modifier = Modifier.testTag("ToggleButton"),
                                )
                                Button(
                                    onClick = { navController.navigate(NEXT) },
                                ) {
                                    Text("Go")
                                }
                            }
                        }
                    }
                    composable(NEXT) {
                        screenId.value = NEXT
                        holder.SaveableStateProvider(NEXT) {
                            var counter by rememberSaveable { mutableStateOf(0) }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { ++counter },
                                    modifier = Modifier.testTag(COUNTER)
                                ) {
                                    Text("$counter")
                                }
                                Button(
                                    onClick = { navController.navigate(START) },
                                ) {
                                    Text("Jump")
                                }
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
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        // Next screen should still display the incremented counter.
        rule.onNodeWithText("1").assertExists()
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        // Start screen should still display 'On'
        rule.waitForIdle()
        rule.onNodeWithText("On").assertExists()
        // Going on to the Next screen again, this is a new instance of the Nav destination.
        rule.onNodeWithText("Go").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("0").assertExists()
    }

    @Test
    fun updates_lifecycle_for_initial_destination() {
        lateinit var navController: NavHostController
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
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
        rule.setContentWithTheme {
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
        rule.setContentWithTheme {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        // Click to move to next destination then swipe back.
        rule.onNodeWithText(START).performClick()
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_popping_back_stack() {
        lateinit var navController: NavHostController
        rule.setContentWithTheme {
            navController = rememberSwipeDismissableNavController()
            SwipeDismissWithNavigation(navController)
        }

        rule.waitForIdle()
        rule.onNodeWithText(START).performClick()

        rule.runOnIdle {
            navController.popBackStack()
        }

        rule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun provides_access_to_current_backstack_entry_state() {
        lateinit var navController: NavHostController
        lateinit var backStackEntry: State<NavBackStackEntry?>
        rule.setContentWithTheme {
            navController = rememberSwipeDismissableNavController()
            backStackEntry = navController.currentBackStackEntryAsState()
            SwipeDismissWithNavigation(navController)
        }

        rule.onNodeWithText(START).performClick()

        rule.runOnIdle {
            assertThat(backStackEntry.value?.destination?.route)
                .isEqualTo(NEXT)
        }
    }

    @Test
    fun testNavHostController_starts_at_default_destination() {
        lateinit var navController: TestNavHostController

        rule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(WearNavigator())

            SwipeDismissWithNavigation(navController)
        }

        rule.onNodeWithText(START).assertExists()
    }

    @Test
    fun testNavHostController_sets_current_destination() {
        lateinit var navController: TestNavHostController

        rule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(WearNavigator())

            SwipeDismissWithNavigation(navController)
            navController.setCurrentDestination(NEXT)
        }

        rule.onNodeWithText(NEXT).assertExists()
    }

    @Composable
    fun SwipeDismissWithNavigation(
        navController: NavHostController = rememberSwipeDismissableNavController(),
        userSwipeEnabled: Boolean = true
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = START,
            modifier = Modifier.testTag(TEST_TAG),
            userSwipeEnabled = userSwipeEnabled
        ) {
            composable(START) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CompactChip(
                        onClick = { navController.navigate(NEXT) },
                        label = { Text(text = START) }
                    )
                }
            }
            composable("next") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(NEXT)
                }
            }
        }
    }
}

fun ComposeContentTestRule.setContentWithTheme(
    composable: @Composable () -> Unit
) {
    setContent {
        MaterialTheme {
            composable()
        }
    }
}

private const val NEXT = "next"
private const val START = "start"
private const val COUNTER = "counter"
private const val TEST_TAG = "test-item"
