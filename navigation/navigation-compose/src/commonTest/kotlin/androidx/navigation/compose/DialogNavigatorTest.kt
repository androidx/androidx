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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.testing.TestNavigatorState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DialogNavigatorTest {

    private val defaultText = "dialogText"

    @Test
    fun testDialogs() = runComposeUiTestOnUiThread {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)

        setContent { DialogHost(navigator) }

        onNodeWithText(defaultText).assertDoesNotExist()

        val dialog = DialogNavigator.Destination(navigator) { Text(defaultText) }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun testPopDismissesDialog() = runComposeUiTestOnUiThread {
        val navigator = DialogNavigator()
        val navigatorState = TestNavigatorState()
        navigator.onAttach(navigatorState)
        val dialog = DialogNavigator.Destination(navigator) { Text(defaultText) }
        val entry = navigatorState.createBackStackEntry(dialog, null)
        navigator.navigate(listOf(entry), null, null)

        setContent { DialogHost(navigator) }

        onNodeWithText(defaultText).assertIsDisplayed()

        navigator.popBackStack(entry, false)

        onNodeWithText(defaultText).assertDoesNotExist()
    }

    @Test
    fun testNestedNavHostInDialogDismissed() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") {}
                dialog("second") {
                    viewModel<TestViewModel>(
                        viewModelStoreOwner = it,
                        factory = TestViewModelFactory()
                    )
                }
            }
        }

        runOnIdle { navController.navigate("second") }

        // Now trigger the back button
        runOnIdle {
            navController.navigatorProvider
                .getNavigator<DialogNavigator>(DialogNavigator.NAME)
                .dismiss(navController.getBackStackEntry("second"))
        }

        waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo("first")
    }

    @Test
    fun testDialogMarkedTransitionComplete() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") {}
                dialog("second") {}
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val bottomDialog = dialogNavigator.backStack.value[0]
        val topDialog = dialogNavigator.backStack.value[1]

        assertThat(bottomDialog.destination.route).isEqualTo("second")
        assertThat(topDialog.destination.route).isEqualTo("second")
        assertThat(topDialog).isNotEqualTo(bottomDialog)

        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        runOnUiThread { dialogNavigator.dismiss(topDialog) }
        waitForIdle()

        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testDialogMarkedTransitionCompleteInOrder() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") {}
                dialog("second") {}
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
            navController.navigate("second")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val bottomDialog = dialogNavigator.backStack.value[0]
        val middleDialog = dialogNavigator.backStack.value[1]
        val topDialog = dialogNavigator.backStack.value[2]

        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(middleDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        runOnUiThread { dialogNavigator.dismiss(topDialog) }
        waitForIdle()

        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(middleDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        runOnUiThread { dialogNavigator.dismiss(middleDialog) }
        waitForIdle()

        assertThat(middleDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testDialogNavigateConsecutively() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, "first") {
                composable("first") {}
                dialog("second") {}
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.navigate("second")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val bottomDialog = dialogNavigator.backStack.value[0]
        val topDialog = dialogNavigator.backStack.value[1]

        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun testDialogNavigatePopNavigate() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, route = "graph", startDestination = "first") {
                composable("first") {}
                dialog("second") {}
                dialog("third") { Text(defaultText) }
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.popBackStack()
            navController.navigate("third")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val dialog = dialogNavigator.backStack.value[0]
        assertThat(dialog.destination.route).isEqualTo("third")
        assertThat(dialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        onNodeWithText(defaultText).assertIsDisplayed()
        assertThat(navController.visibleEntries.value.map { it.destination.route })
            .containsExactly("first", "third")
            .inOrder()
    }

    @Test
    fun testDialogNavigatePopNavigateSameDialog() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, route = "graph", startDestination = "first") {
                composable("first") {}
                dialog("second") { Text(defaultText) }
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.popBackStack()
            navController.navigate("second")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val dialog = dialogNavigator.backStack.value[0]
        assertThat(dialog.destination.route).isEqualTo("second")
        assertThat(dialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        onNodeWithText(defaultText).assertIsDisplayed()
        assertThat(navController.visibleEntries.value.map { it.destination.route })
            .containsExactly("first", "second")
            .inOrder()
    }

    @Test
    fun testDialogNavigatePopPopNavigate() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, route = "graph", startDestination = "first") {
                composable("first") {}
                dialog("second") {}
                dialog("third") {}
                dialog("fourth") { Text(defaultText) }
            }
        }

        runOnIdle {
            navController.navigate("second")
            navController.navigate("third")
            navController.popBackStack()
            navController.popBackStack()
            navController.navigate("fourth")
        }

        waitForIdle()
        val dialogNavigator =
            navController.navigatorProvider.getNavigator<DialogNavigator>(DialogNavigator.NAME)
        val dialog = dialogNavigator.backStack.value[0]
        assertThat(dialog.destination.route).isEqualTo("fourth")
        assertThat(dialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        onNodeWithText(defaultText).assertIsDisplayed()
        assertThat(navController.visibleEntries.value.map { it.destination.route })
            .containsExactly("first", "fourth")
            .inOrder()
    }

    @Test
    fun testDialogObserveRemovedOnPopNavigate() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()
            NavHost(navController, route = "graph", startDestination = "first") {
                composable("first") {}
                dialog("second") {}
                dialog("third") { Text(defaultText) }
            }
        }

        runOnUiThread { navController.navigate("second") }

        val secondEntry = navController.currentBackStackEntry
        val entryLifecycle = secondEntry?.lifecycle as LifecycleRegistry

        runOnIdle {
            assertThat(secondEntry.destination.route).isEqualTo("second")
            // observers added
            assertThat(entryLifecycle.observerCount).isEqualTo(2)

            // now pop dialog and navigate to another dialog
            navController.popBackStack()
            navController.navigate("third")
        }

        waitForIdle()
        onNodeWithText(defaultText).assertIsDisplayed()
        runOnUiThread {
            // make sure when secondEntry was disposed, observer was removed
            assertThat(entryLifecycle.observerCount).isEqualTo(0)
        }
    }
}
