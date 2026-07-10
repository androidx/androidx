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

package androidx.wear.compose.navigation3

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.google.common.truth.Truth.assertThat
import kotlin.String
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SwipeDismissableSceneStrategyTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private lateinit var backPressedDispatcher: OnBackPressedDispatcher

    @Test
    fun calculateScene_currentEntry() {
        var scene: Scene<String>? = null
        val entry =
            NavEntry(FIRST_KEY) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ScalingLazyColumn(modifier = Modifier.testTag(FIRST_SCREEN)) {
                        item { Text(text = FIRST_SCREEN) }
                    }
                }
            }
        val scope = SceneStrategyScope<String>()
        rule.setContent {
            val sceneStrategy = rememberSwipeDismissableSceneStrategy<String>()
            scene = with(sceneStrategy) { scope.calculateScene(entries = listOf(entry)) }

            scene!!.content()
        }

        assertThat(scene).isNotNull()
        assertThat(scene!!.entries.first()).isEqualTo(entry)
        rule.onNodeWithTag(FIRST_SCREEN).assertIsDisplayed()
    }

    @Test
    fun calculateScene_emptyBackStack() {
        var scene: Scene<String>? = null
        val scope = SceneStrategyScope<String>()
        rule.setContent {
            val sceneStrategy = rememberSwipeDismissableSceneStrategy<String>()
            scene = with(sceneStrategy) { scope.calculateScene(entries = listOf()) }
            scene?.content()
        }

        assertThat(scene).isNull()
    }

    @Test
    fun navigates_to_next_level() {
        rule.setContent { TestNavDisplay() }

        rule.onNodeWithTag(FIRST_SCREEN).assertIsDisplayed()
        rule.onNodeWithText(FIRST_SCREEN).performClick()

        rule.onNodeWithTag(FIRST_SCREEN).assertIsNotDisplayed()
        rule.onNodeWithTag(SECOND_SCREEN).assertIsDisplayed()
    }

    @Test
    fun navigates_back_to_previous_level_after_swipe() {
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(modifier = Modifier.testTag(SCENE_TAG))
        }

        rule.onNodeWithTag(FIRST_SCREEN).assertIsDisplayed()
        // navigate to second entry
        rule.onNodeWithText(FIRST_SCREEN).performClick()

        // swipe back
        rule.swipeRight(SCENE_TAG)

        rule.onNodeWithTag(FIRST_SCREEN).assertIsDisplayed()
        rule.onNodeWithTag(SECOND_SCREEN).assertIsNotDisplayed()
    }

    @Test
    fun does_not_navigate_back_to_previous_level_when_swipe_disabled() {
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                modifier = Modifier.testTag(SCENE_TAG),
                sceneStrategy = rememberSwipeDismissableSceneStrategy(isUserSwipeEnabled = false),
            )
        }

        // Click to move to next destination then swipe to dismiss.
        rule.onNodeWithText(FIRST_SCREEN).performClick()

        // swipe back
        rule.swipeRight(SCENE_TAG)

        // Should still display second destination
        rule.onNodeWithText(SECOND_SCREEN).assertIsDisplayed()
        rule.onNodeWithText(FIRST_SCREEN).assertDoesNotExist()
    }

    @Test
    fun navigates_back_to_previous_level_with_back_button() {
        rule.setContentWithBackPressedDispatcher { TestNavDisplay() }
        // navigate to second entry
        rule.onNodeWithText(FIRST_SCREEN).performClick()

        // Now trigger the back button
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        // back to first entry
        rule.onNodeWithTag(FIRST_SCREEN).assertIsDisplayed()
        rule.onNodeWithTag(SECOND_SCREEN).assertIsNotDisplayed()
    }

    @Test
    fun navigates_back_to_previous_level_with_back_button_previous_state_destroyed() {
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        var lifecycleState: Lifecycle? = null
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) { Text(FIRST_SCREEN) }
                        entry(SECOND_KEY) { lifecycleState = LocalLifecycleOwner.current.lifecycle }
                    },
            )
        }

        // Move to next destination.
        backStack.add(SECOND_KEY)
        rule.waitForIdle()

        assertThat(lifecycleState!!.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // Now trigger the back button
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }
        rule.waitForIdle()

        // Should now display "start".
        rule.onNodeWithText(FIRST_SCREEN).assertIsDisplayed()
        assertThat(lifecycleState.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun does_not_intercept_back_pressed_with_single_item_backstack() {
        rule.setContentWithBackPressedDispatcher { TestNavDisplay() }
        // If NavDisplay is handling back, 'hasEnabledCallbacks' would be true.
        // For a single item, it should be false so the system (Activity) can handle it.
        assertFalse(backPressedDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun does_not_crash_when_swiping_back_with_single_item_backstack() {
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(modifier = Modifier.testTag(SCENE_TAG))
        }

        val result = runCatching {
            rule.swipeRight(SCENE_TAG)
            rule.waitForIdle()
        }

        assertTrue("Caught exception while swiping back.", result.isSuccess)
    }

    @Test
    fun displays_previous_screen_during_swipe_gesture() {
        rule.setContentWithBackPressedDispatcher {
            WithTouchSlop(0f) { TestNavDisplay(modifier = Modifier.testTag(SCENE_TAG)) }
        }

        // Click to move to next destination.
        rule.onNodeWithText(FIRST_SCREEN).performClick()
        // Click and drag to begin a swipe gesture, but do not release the finger.
        rule.dragRight(SCENE_TAG)

        // As the finger is still 'down', the background should be visible.
        rule.onNodeWithText(FIRST_SCREEN).assertExists()
        rule.onNodeWithText(SECOND_SCREEN).assertExists()
    }

    @Test
    fun focus_updates_after_swipe_gesture() {
        rule.setContentWithBackPressedDispatcher {
            WithTouchSlop(0f) { TestNavDisplay(modifier = Modifier.testTag(SCENE_TAG)) }
        }

        rule.onNodeWithTag(FIRST_SCREEN).onChild().assertIsFocused()

        // Click to move to next destination.
        rule.onNodeWithText(FIRST_SCREEN).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(SECOND_SCREEN).onChild().assertIsFocused()

        // swipe back to starting screen
        rule.swipeRight(SCENE_TAG)

        rule.onNodeWithTag(FIRST_SCREEN).onChild().assertIsFocused()
    }

    @Test
    fun destinations_keep_saved_state() {
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        lateinit var intState: MutableIntState
        val updateState = "updateState"
        val navigate = "navigate"
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) {
                            Column(modifier = Modifier.fillMaxSize().testTag(ENTRY_TAG)) {
                                intState = rememberSaveable { mutableIntStateOf(0) }
                                Text("${intState.intValue}")
                                Button(onClick = { intState.intValue += 1 }) {
                                    Text(text = updateState)
                                }
                                Button(onClick = { backStack.add(SECOND_KEY) }) {
                                    Text(text = navigate)
                                }
                            }
                        }
                        entry(SECOND_KEY) {
                            Box(modifier = Modifier.fillMaxSize().testTag(ENTRY_TAG)) {
                                Text(text = SECOND_SCREEN)
                            }
                        }
                    },
            )
        }

        rule.onNodeWithText("0").assertIsDisplayed()
        rule.onNodeWithText(updateState).performClick()
        rule.onNodeWithText("1").assertIsDisplayed()

        // go to next screen
        rule.onNodeWithText(navigate).performClick()
        rule.onNodeWithText("1").assertIsNotDisplayed()
        rule.onNodeWithText(SECOND_SCREEN).assertIsDisplayed()

        // go back to first screen, make sure rememberSaveable state is restored
        rule.swipeRight(ENTRY_TAG)
        rule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun remembers_saved_state_on_two_screens() {
        val backStack = mutableStateListOf<Any>(1)
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry<Int> {
                            val storedState = rememberSaveable { mutableIntStateOf(10) }
                            // column layout enables proper edge swipe for this test
                            Column(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .testTag(ENTRY_TAG),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("${storedState.intValue}")
                                Button(onClick = { storedState.intValue += 1 }) {
                                    Text(text = "increment")
                                }
                            }
                        }
                        entry<String> {
                            val storedState = rememberSaveable { mutableStateOf(false) }
                            // column layout enables proper edge swipe for this test
                            Column(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .testTag(ENTRY_TAG),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("${storedState.value}")
                                Button(onClick = { storedState.value = true }) {
                                    Text(text = "make true")
                                }
                            }
                        }
                    },
            )
        }

        rule.onNodeWithText("10").assertIsDisplayed()
        rule.onNodeWithText("increment").performClick()
        rule.onNodeWithText("11").assertIsDisplayed()

        // navigate to Second screen
        backStack.add("second")

        rule.onNodeWithText("false").assertIsDisplayed()
        rule.onNodeWithText("make true").performClick()
        rule.onNodeWithText("true").assertIsDisplayed()

        // navigate to new instance of "first" screen (any Int type key)
        backStack.add(2)

        // new entry with brand new state
        rule.onNodeWithText("10").assertIsDisplayed()

        // now go back and make sure the previous state is preserved
        rule.swipeRight(ENTRY_TAG)
        rule.onNodeWithText("true").assertIsDisplayed()

        // go back again and make sure the very first screen's state is preserved
        rule.swipeRight(ENTRY_TAG)
        rule.onNodeWithText("11").assertIsDisplayed()

        // navigate to new instance of "second" screen (any String type key), should have brand new
        // state
        backStack.add("other")
        rule.onNodeWithText("false").assertIsDisplayed()
    }

    @Test
    fun updates_lifecycle_for_initial_destination() {
        var lifecycleState: Lifecycle? = null

        rule.setContent {
            TestNavDisplay(
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) { lifecycleState = LocalLifecycleOwner.current.lifecycle }
                    }
            )
        }

        rule.runOnIdle {
            assertThat(lifecycleState?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_navigation() {
        var lifecycleState: Lifecycle? = null
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) { Text(FIRST_SCREEN) }
                        entry(SECOND_KEY) { lifecycleState = LocalLifecycleOwner.current.lifecycle }
                    },
            )
        }

        // move to next destination
        backStack.add(SECOND_KEY)

        rule.runOnIdle {
            assertThat(lifecycleState?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_navigation_and_swipe_back() {
        var lifecycleState: Lifecycle? = null
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) {
                            Box(modifier = Modifier.fillMaxSize().testTag(ENTRY_TAG)) {
                                lifecycleState = LocalLifecycleOwner.current.lifecycle
                            }
                        }

                        entry(SECOND_KEY) {
                            Box(modifier = Modifier.fillMaxSize().testTag(ENTRY_TAG))
                        }
                    },
            )
        }

        // move to next destination
        backStack.add(SECOND_KEY)
        rule.runOnIdle {
            assertThat(lifecycleState?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }

        // swipe back
        rule.swipeRight(ENTRY_TAG)

        rule.runOnIdle {
            assertThat(lifecycleState?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun updates_lifecycle_after_popping_back_stack() {
        var lifecycleState: Lifecycle? = null
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        rule.setContentWithBackPressedDispatcher {
            TestNavDisplay(
                backStack = backStack,
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) { lifecycleState = LocalLifecycleOwner.current.lifecycle }
                        entry(SECOND_KEY) {}
                    },
            )
        }

        // move to next destination
        backStack.add(SECOND_KEY)

        // pop back
        backStack.removeLastOrNull()

        rule.runOnIdle {
            assertThat(lifecycleState?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    // The following test verifies the PredictiveBackNavHost animation and targets API 36+.
    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun press_back_during_animation_does_not_reset_animation() {
        val backStack = mutableStateListOf<Any>(FIRST_KEY, SECOND_KEY, THIRD_KEY)
        rule.setContentWithBackPressedDispatcher { TestNavDisplay(backStack = backStack) }

        rule.waitForIdle()

        rule.mainClock.autoAdvance = false

        val navHostWidth =
            rule.onNodeWithTag(TEST_TAG_THIRD_CONTAINER).fetchSemanticsNode().size.width

        rule.runOnIdle { backPressedDispatcher.onBackPressed() }

        // Animation now starts. Since we control animation clock manually, here we will
        // fast-forward few frames.
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
        var previousLeft =
            rule.onNodeWithTag(TEST_TAG_THIRD_CONTAINER).fetchSemanticsNode().boundsInWindow.left

        // Press back while animation is running
        rule.runOnIdle { backPressedDispatcher.onBackPressed() }

        // Make sure animation continues after pressing back button
        do {
            rule.mainClock.advanceTimeByFrame()

            val currentLeft =
                rule
                    .onNodeWithTag(TEST_TAG_THIRD_CONTAINER)
                    .fetchSemanticsNode()
                    .boundsInWindow
                    .left
            assertTrue(previousLeft < currentLeft)
            previousLeft = currentLeft
        } while (currentLeft < 0.9 * navHostWidth)
        rule.mainClock.autoAdvance = true
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun navigate_interrupted_with_pop() {
        val backStack = mutableStateListOf<Any>(FIRST_KEY)
        val testDuration = 300
        rule.setContentWithBackPressedDispatcher {
            NavDisplay(
                backStack = backStack,
                transitionSpec = {
                    slideInHorizontally(tween(testDuration)) { it / 2 } togetherWith
                        slideOutHorizontally(tween(testDuration)) { -it / 2 }
                },
                popTransitionSpec = {
                    slideInHorizontally(tween(testDuration)) { -it / 2 } togetherWith
                        slideOutHorizontally(tween(testDuration)) { it / 2 }
                },
                sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
                entryProvider =
                    entryProvider {
                        entry(FIRST_KEY) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = FIRST_SCREEN)
                            }
                        }
                        entry(SECOND_KEY) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Text(text = SECOND_SCREEN)
                            }
                        }
                    },
            )
        }

        rule.waitForIdle()

        rule.mainClock.autoAdvance = false

        // navigate to Second
        rule.runOnIdle { backStack.add(SECOND_KEY) }

        // run half the transitions
        rule.mainClock.advanceTimeBy(testDuration.toLong() / 2)

        // interrupt navigation with pop
        rule.runOnIdle { backStack.removeLastOrNull() }

        // run half the transitions
        rule.mainClock.advanceTimeBy(testDuration.toLong() / 2)

        // ensure text on the left side of the screen is visible (ensure screen slides out from
        // left to right)
        rule.onNodeWithText(SECOND_SCREEN).assertIsDisplayed()
    }

    @Composable
    private fun TestNavDisplay(
        modifier: Modifier = Modifier,
        backStack: SnapshotStateList<Any> = mutableStateListOf(FIRST_KEY),
        sceneStrategy: SwipeDismissableSceneStrategy<Any> = rememberSwipeDismissableSceneStrategy(),
        @Suppress("UNCHECKED_CAST")
        entryProvider: (key: Any) -> NavEntry<Any> =
            buildEntryProvider(backStack, modifier) as (key: Any) -> NavEntry<Any>,
    ) {
        NavDisplay(
            backStack = backStack,
            sceneStrategies = listOf(sceneStrategy),
            entryProvider = entryProvider,
        )
    }

    private fun ComposeContentTestRule.setContentWithBackPressedDispatcher(
        composable: @Composable () -> Unit
    ) {
        setContent {
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            val dispatcherOwner =
                object : OnBackPressedDispatcherOwner, LifecycleOwner by TestLifecycleOwner() {
                    override val onBackPressedDispatcher = backPressedDispatcher
                }
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
                composable()
            }
        }
    }

    private fun DirectNavigationEventInput.onBackPressed() {
        backProgressed(NavigationEvent(progress = 0.0F))
        backCompleted()
    }

    /**
     * Depending on API level, either swipes right on the view with SCENE_TAG, or emulates
     * system-level swipe using backPressedDispatcher
     */
    private fun ComposeContentTestRule.swipeRight(tag: String) {
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
            onNodeWithTag(tag).performTouchInput { swipeRight() }
        }
    }

    /**
     * Dragss without releasing the finger.
     *
     * Depending on API level, either drags right on the view with SCENE_TAG, or emulates
     * system-level drag using backPressedDispatcher
     */
    private fun ComposeContentTestRule.dragRight(tag: String) {
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
                .onNodeWithTag(tag)
                .performTouchInput({
                    down(Offset(x = 0f, y = height / 2f))
                    moveTo(Offset(x = width / 4f, y = height / 2f))
                })
        }
    }

    private fun buildEntryProvider(
        backStack: SnapshotStateList<Any>,
        modifier: Modifier = Modifier,
    ) = entryProvider {
        entry(FIRST_KEY) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScalingLazyColumn(modifier = Modifier.testTag(FIRST_SCREEN)) {
                    item {
                        Button(
                            onClick = { backStack.add(SECOND_KEY) },
                            label = { Text(text = FIRST_SCREEN) },
                        )
                    }
                }
            }
        }
        entry(SECOND_KEY) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ScalingLazyColumn(modifier = Modifier.testTag(SECOND_SCREEN)) {
                    item { Text(SECOND_SCREEN) }
                }
            }
        }
        entry(THIRD_KEY) {
            Box(
                modifier = modifier.fillMaxSize().testTag(TEST_TAG_THIRD_CONTAINER),
                contentAlignment = Alignment.Center,
            ) {
                ScalingLazyColumn(modifier = Modifier.testTag(THIRD_SCREEN)) {
                    item { Text(THIRD_SCREEN) }
                }
            }
        }
    }

    val FIRST_KEY = "First"
    val FIRST_SCREEN = "FirstTag"
    val SECOND_KEY = "Second"
    val SECOND_SCREEN = "SecondTag"
    val THIRD_KEY = "Third"
    val THIRD_SCREEN = "ThirdTag"
    val SCENE_TAG = "SceneModifierTag"
    val ENTRY_TAG = "EntryTag"
    val TEST_TAG_THIRD_CONTAINER = "ThirdContainerTag"
}
