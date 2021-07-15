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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraph
import androidx.navigation.contains
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.navigation.plusAssign
import androidx.navigation.testing.TestNavHostController
import androidx.savedstate.SavedStateRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSingleDestinationSet() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = createNavController(LocalContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()
    }

    @Test
    fun testNavigate() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = createNavController(LocalContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
                test("second")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()

        runOnUiThread {
            navController.navigate("second")
        }

        assertWithMessage("second destination should be current")
            .that(navController.currentDestination?.route).isEqualTo("second")
    }

    @Test
    fun testNavigateOutsideStateChange() {
        lateinit var navController: NavHostController
        val text = "myButton"
        var counter = 0
        composeTestRule.setContent {
            navController = rememberNavController()
            var state by remember { mutableStateOf(0) }
            Column(Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = "first") {
                    composable("first") { }
                    composable("second") { }
                }
                Button(
                    onClick = {
                        state++
                        counter = state
                    }
                ) {
                    Text(text)
                }
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()

        composeTestRule.runOnIdle {
            navController.navigate("second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route).isEqualTo("second")
        }

        composeTestRule.onNodeWithText(text)
            .performClick()

        composeTestRule.runOnIdle {
            // ensure our click listener was fired
            assertThat(counter).isEqualTo(1)
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route).isEqualTo("second")
        }
    }

    @Test
    fun testPop() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = createNavController(LocalContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
                test("second")
            }
        }

        runOnUiThread {
            navController.setCurrentDestination("second")
            navController.popBackStack()
        }

        assertWithMessage("First destination should be current")
            .that(navController.currentDestination?.route).isEqualTo("first")
    }

    @Test
    fun testChangeStartDestination() {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<String>
        composeTestRule.setContent {
            state = remember { mutableStateOf("first") }
            val context = LocalContext.current
            // added to avoid lint error b/184349025
            @SuppressLint("RememberReturnType")
            navController = remember { createNavController(context) }

            NavHost(navController, startDestination = state.value) {
                test("first")
                test("second")
            }
        }

        runOnUiThread {
            state.value = "second"
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Second destination should be current")
                .that(navController.currentDestination?.route).isEqualTo("second")
        }
    }

    @Test
    fun testSameControllerAfterDisposingNavHost() {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<Int>
        composeTestRule.setContent {
            val context = LocalContext.current
            state = remember { mutableStateOf(0) }
            // added to avoid lint error b/184349025
            @SuppressLint("RememberReturnType")
            navController = remember { createNavController(context) }
            if (state.value == 0) {
                NavHost(navController, startDestination = "first") {
                    test("first")
                }
            }
        }

        runOnUiThread {
            // dispose the NavHost
            state.value = 1
        }

        // wait for recompose without NavHost then recompose with the NavHost
        composeTestRule.runOnIdle {
            state.value = 0
        }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route).isEqualTo("first")
        }
    }

    @Test
    fun testViewModelSavedAfterConfigChange() {
        lateinit var navController: NavHostController
        lateinit var state: MutableState<Int>
        lateinit var viewModel: TestViewModel
        var savedState: Bundle? = null
        composeTestRule.setContent {
            val context = LocalContext.current
            state = remember { mutableStateOf(0) }
            navController = if (savedState == null) {
                rememberNavController()
            } else {
                NavHostController(context).apply {
                    restoreState(savedState)
                    setViewModelStore(LocalViewModelStoreOwner.current!!.viewModelStore)
                    navigatorProvider += ComposeNavigator()
                    navigatorProvider += DialogNavigator()
                }
            }
            if (state.value == 0) {
                NavHost(navController, startDestination = "first") {
                    composable("first") {
                        val provider = ViewModelProvider(it)
                        viewModel = provider.get("key", TestViewModel::class.java)
                    }
                }
            }
        }
        val savedViewModel: TestViewModel = viewModel
        savedViewModel.value = "testing"
        savedState = navController.saveState()

        runOnUiThread {
            // dispose the NavHost
            state.value = 1
        }

        // wait for recompose without NavHost then recompose with the NavHost
        composeTestRule.runOnIdle {
            state.value = 0
        }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route).isEqualTo("first")
            assertThat(savedViewModel.value).isEqualTo(viewModel.value)
        }
    }

    @Test
    fun testStateOfInactiveScreenIsRestoredWhenWeGoBackToIt() {
        var increment = 0
        var numberOnScreen1 = -1
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {
                    numberOnScreen1 = rememberSaveable { increment++ }
                }
                composable("Second") {}
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0")
                .that(numberOnScreen1)
                .isEqualTo(0)
            numberOnScreen1 = -1
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1)
                .isEqualTo(0)
        }
    }

    @Test
    fun stateForScreenRemovedFromBackStackIsNotRestored() {
        var increment = 0
        var numberOnScreen2 = -1
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {
                }
                composable("Second") {
                    numberOnScreen2 = rememberSaveable { increment++ }
                }
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0")
                .that(numberOnScreen2)
                .isEqualTo(0)
            numberOnScreen2 = -1
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number shouldn't be restored")
                .that(numberOnScreen2)
                .isEqualTo(1)
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

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Each entry should have its own SavedStateRegistry")
                .that(registry1)
                .isNotEqualTo(registry2)
        }
    }

    @Test
    fun setSameGraph() {
        var currentGraph by mutableStateOf<NavGraph?>(null)
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 = navController.createGraph(startDestination = "First") {
                composable("First") { }
                composable("Second") { }
            }
            graph2 = navController.createGraph(startDestination = "First") {
                composable("First") { }
                composable("Second") { }
            }
            currentGraph = graph1
            NavHost(navController, currentGraph!!)
        }

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }

        composeTestRule.runOnIdle {
            currentGraph = graph2
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }
    }

    @Test
    fun testNavHostCrossFade() {
        lateinit var navController: NavHostController

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                composable(second) { BasicText(second) }
            }
        }

        val firstEntry = navController.currentBackStackEntry

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle {
            navController.navigate(second)
        }

        assertThat(firstEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        // advance half way between the crossfade
        composeTestRule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(firstEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.CREATED)
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.mainClock.autoAdvance = false

        val secondEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.CREATED)

        // advance half way between the crossfade
        composeTestRule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.CREATED)

        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
            assertThat(secondEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    private fun createNavController(context: Context): TestNavHostController {
        val navController = TestNavHostController(context)
        val navigator = TestNavigator()
        navController.navigatorProvider += navigator
        return navController
    }
}

private const val first = "first"
private const val second = "second"

class TestViewModel : ViewModel() {
    var value: String = "nothing"
}
