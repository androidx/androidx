/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.testing.TestNavigatorState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogNavigatorTest {
    @get:Rule
    val rule = createComposeRule()

    private val defaultText = "dialogText"

    @Test
    fun testDialogs() {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)

        rule.setContent {
            DialogHost(navigator)
        }

        rule.onNodeWithText(defaultText).assertDoesNotExist()

        val dialog = DialogNavigator.Destination(navigator) {
            Text(defaultText)
        }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun testPopDismissesDialog() {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)
        val dialog = DialogNavigator.Destination(navigator) {
            Text(defaultText)
        }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        rule.setContent {
            DialogHost(navigator)
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        navigator.popBackStack(entry, false)

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @Test
    fun testNestedNavHostInDialogDismissed() {
        lateinit var navController: NavHostController

        rule.setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { }
                dialog("second") {
                    viewModel<TestViewModel>(it)
                }
            }
        }

        rule.runOnIdle {
            navController.navigate("second")
        }

        // Now trigger the back button
        rule.runOnIdle {
            navController.navigatorProvider.getNavigator(DialogNavigator::class.java).dismiss(
                navController.getBackStackEntry("second")
            )
        }

        rule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo("first")
    }

    @Test
    fun testDialogMarkedTransitionComplete() {
        lateinit var navController: NavHostController

        rule.setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { }
                dialog("second") {
                    Text(defaultText)
                }
            }
        }

        rule.runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
        }

        rule.waitForIdle()
        val dialogNavigator = navController.navigatorProvider.getNavigator(
            DialogNavigator::class.java
        )
        val bottomDialog = dialogNavigator.backStack.value[0]
        val topDialog = dialogNavigator.backStack.value[1]

        assertThat(bottomDialog.destination.route).isEqualTo("second")
        assertThat(topDialog.destination.route).isEqualTo("second")
        assertThat(topDialog).isNotEqualTo(bottomDialog)

        assertThat(topDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.STARTED
        )

        rule.runOnUiThread {
            dialogNavigator.dismiss(topDialog)
        }
        rule.waitForIdle()

        assertThat(topDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.DESTROYED
        )
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
    }

    @Test
    fun testDialogMarkedTransitionCompleteInOrder() {
        lateinit var navController: NavHostController

        rule.setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { }
                dialog("second") {
                    Text(defaultText)
                }
            }
        }

        rule.runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
            navController.navigate("second")
        }

        rule.waitForIdle()
        val dialogNavigator = navController.navigatorProvider.getNavigator(
            DialogNavigator::class.java
        )
        val bottomDialog = dialogNavigator.backStack.value[0]
        val middleDialog = dialogNavigator.backStack.value[1]
        val topDialog = dialogNavigator.backStack.value[2]

        assertThat(topDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
        assertThat(middleDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.STARTED
        )
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.STARTED
        )

        rule.runOnUiThread {
            dialogNavigator.dismiss(topDialog)
        }
        rule.waitForIdle()

        assertThat(topDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.DESTROYED
        )
        assertThat(middleDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.STARTED
        )

        rule.runOnUiThread {
            dialogNavigator.dismiss(middleDialog)
        }
        rule.waitForIdle()

        assertThat(middleDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.DESTROYED
        )
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
    }

    @Test
    fun testDialogNavigateConsecutively() {
        lateinit var navController: NavHostController

        rule.setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") { }
                dialog("second") {
                    Text(defaultText)
                }
            }
        }

        rule.runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
        }

        rule.waitForIdle()
        val dialogNavigator = navController.navigatorProvider.getNavigator(
            DialogNavigator::class.java
        )
        val bottomDialog = dialogNavigator.backStack.value[0]
        val topDialog = dialogNavigator.backStack.value[1]

        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.STARTED
        )
        assertThat(topDialog.lifecycle.currentState).isEqualTo(
            Lifecycle.State.RESUMED
        )
    }
}
