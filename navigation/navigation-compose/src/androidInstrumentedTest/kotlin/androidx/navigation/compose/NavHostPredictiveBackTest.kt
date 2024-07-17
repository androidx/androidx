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

import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostPredictiveBackTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNavHostAnimations() {
        lateinit var navController: NavHostController
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            navController = rememberNavController()
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                composable(second) { BasicText(second) }
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
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.STARTED)
            assertThat(navController.previousBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.STARTED)
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

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.runOnIdle { backPressedDispatcher.dispatchOnBackCancelled() }

        composeTestRule.runOnIdle {
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        }
    }
}

private const val first = "first"
private const val second = "second"
