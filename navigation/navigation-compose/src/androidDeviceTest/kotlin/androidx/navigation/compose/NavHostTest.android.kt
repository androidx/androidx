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

package androidx.navigation.compose

import android.app.Activity
import android.content.Intent
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.testing.TestNavHostController
import androidx.savedstate.SavedStateRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSaveableStateClearedAfterPop() {
        lateinit var navController: NavHostController
        var viewModel: BackStackEntryIdViewModel? = null
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable("second") { viewModel = viewModel() }
            }
        }

        composeTestRule.runOnIdle { navController.navigate("second") }

        composeTestRule.runOnIdle {
            assertThat(viewModel?.saveableStateHolderRef?.get()).isNotNull()
        }

        composeTestRule.runOnIdle { navController.popBackStack() }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("first")
            assertThat(viewModel?.saveableStateHolderRef?.get()).isNull()
        }
    }

    @Test
    fun savedStateRegistryOwnerTest() {
        lateinit var registry1: SavedStateRegistry
        lateinit var registry2: SavedStateRegistry
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {
                    registry1 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                }
                composable("Second") {
                    registry2 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                }
            }
        }

        composeTestRule.runOnIdle { navController.navigate("Second") }

        composeTestRule.runOnIdle {
            assertWithMessage("Each entry should have its own SavedStateRegistry")
                .that(registry1)
                .isNotEqualTo(registry2)
        }
    }

    @Test
    fun testNavHostDeeplink() {
        lateinit var navController: NavHostController

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            // Add the flags to make NavController think this is a deep link
            val activity = LocalContext.current as? Activity
            activity?.intent?.run {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                composable(
                    second,
                    deepLinks = listOf(navDeepLink { action = Intent.ACTION_MAIN })
                ) {
                    BasicText(second)
                }
            }
        }

        composeTestRule.waitForIdle()

        val firstEntry = navController.getBackStackEntry(first)
        val secondEntry = navController.getBackStackEntry(second)

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(firstEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(secondEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun testNestedNavHostOnBackPressed() {
        var innerLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val onBackPressedDispatcher = OnBackPressedDispatcher()
        val dispatcherOwner =
            object : OnBackPressedDispatcherOwner, LifecycleOwner by TestLifecycleOwner() {
                override val onBackPressedDispatcher = onBackPressedDispatcher
            }
        lateinit var navController: NavHostController
        lateinit var innerNavController: NavHostController

        composeTestRule.setContent {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dispatcherOwner) {
                navController = rememberNavController()
                NavHost(navController, first) {
                    composable(first) {
                        CompositionLocalProvider(LocalLifecycleOwner provides innerLifecycleOwner) {
                            // Note: you should not ever do this. Use the state of the single
                            // NavHost to control the visibility of global UI
                            innerNavController = rememberNavController()
                            NavHost(innerNavController, "innerFirst") {
                                composable("innerFirst") {}
                                composable("innerSecond") {}
                            }
                        }
                    }
                    composable(second) {}
                }
            }
        }

        composeTestRule.runOnIdle {
            assertThat(onBackPressedDispatcher.hasEnabledCallbacks()).isFalse()
            innerNavController.navigate("innerSecond")
            assertThat(onBackPressedDispatcher.hasEnabledCallbacks()).isFalse()
        }

        // Now navigate to a second destination in the outer NavHost
        composeTestRule.runOnIdle { navController.navigate(second) }

        composeTestRule.runOnIdle { innerLifecycleOwner.currentState = Lifecycle.State.DESTROYED }

        // Now trigger the back button
        composeTestRule.runOnIdle {
            onBackPressedDispatcher.onBackPressed()
            innerLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        }

        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(first)
        assertThat(innerNavController.currentDestination?.route).isEqualTo("innerSecond")

        // Now trigger the back button
        composeTestRule.runOnIdle { onBackPressedDispatcher.onBackPressed() }

        composeTestRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(first)
        assertThat(innerNavController.currentDestination?.route).isEqualTo("innerFirst")
        // Assert that there's no enabled callbacks left when all of the NavControllers
        // are on their start destination
        assertThat(onBackPressedDispatcher.hasEnabledCallbacks()).isFalse()
    }

    @Test
    fun testPopWithBackHandler() {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        var backPressedDispatcher: OnBackPressedDispatcher? = null
        var count = 0
        var wasCalled = false
        composeTestRule.setContent {
            navController = rememberNavController()
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                BackHandler { wasCalled = true }
                NavHost(navController, startDestination = "first") {
                    composable("first") { BackHandler { count++ } }
                }
            }
        }

        composeTestRule.runOnUiThread {
            backPressedDispatcher?.onBackPressed()
            assertThat(count).isEqualTo(1)
        }

        // move to the back ground to unregister the BackHandlers
        composeTestRule.runOnIdle { lifecycleOwner.currentState = Lifecycle.State.CREATED }

        // register the BackHandlers again
        composeTestRule.runOnIdle { lifecycleOwner.currentState = Lifecycle.State.RESUMED }

        composeTestRule.runOnUiThread {
            backPressedDispatcher?.onBackPressed()
            assertThat(count).isEqualTo(2)
            assertThat(wasCalled).isFalse()
        }
    }
}
private const val first = "first"
private const val second = "second"
private const val third = "third"

@Composable
internal actual fun TestNavHostController() = TestNavHostController(LocalContext.current)

@Composable
internal actual fun NavHostController() = NavHostController(LocalContext.current)