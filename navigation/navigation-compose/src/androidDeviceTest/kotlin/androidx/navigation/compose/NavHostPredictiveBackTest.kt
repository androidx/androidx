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

package androidx.navigation.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostPredictiveBackTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testNavHostAnimations() {
        lateinit var navController: NavHostController
        val owner = TestNavigationEventDispatcherOwner()

        composeTestRule.setContent {
            navController = rememberNavController()
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                NavHost(navController, startDestination = first) {
                    composable(first) { BasicText(first) }
                    composable(second) { BasicText(second) }
                }
            }
        }

        val firstEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        val secondEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            owner.navigationEventInput.backStarted(NavigationEvent(progress = 0.1F))
        }

        composeTestRule.waitForIdle()

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(navController.previousBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.runOnIdle {
            owner.navigationEventInput.backProgressed(NavigationEvent(progress = 0.5F))
        }

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.runOnIdle { owner.navigationEventInput.backCancelled() }

        composeTestRule.runOnIdle {
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        }
    }

    @Test
    fun testDisabledInSameFramePredictiveBack() {
        lateinit var navController: NavHostController
        val owner = TestNavigationEventDispatcherOwner()

        composeTestRule.setContent {
            navController = rememberNavController()
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                NavHost(navController, startDestination = first) {
                    composable(first) { BasicText(first) }
                    composable(second) { BasicText(second) }
                }
            }
        }

        val firstEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        val secondEntry = navController.currentBackStackEntry

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle {
            navController.popBackStack()
            owner.navigationEventInput.backStarted(NavigationEvent(progress = 0.1F))
            owner.navigationEventInput.backCompleted()
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }
}

private const val first = "first"
private const val second = "second"
