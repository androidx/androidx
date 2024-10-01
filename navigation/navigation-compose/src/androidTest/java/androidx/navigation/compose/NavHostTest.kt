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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.contains
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
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
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSingleDestinationSet() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = createNavController(LocalContext.current)

            NavHost(navController, startDestination = "first") { test("first") }
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

        runOnUiThread { navController.navigate("second") }

        assertWithMessage("second destination should be current")
            .that(navController.currentDestination?.route)
            .isEqualTo("second")
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
                    composable("first") {}
                    composable("second") {}
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

        composeTestRule.runOnIdle { navController.navigate("second") }

        composeTestRule.runOnIdle {
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
        }

        composeTestRule.onNodeWithText(text).performClick()

        composeTestRule.runOnIdle {
            // ensure our click listener was fired
            assertThat(counter).isEqualTo(1)
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
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
            .that(navController.currentDestination?.route)
            .isEqualTo("first")
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
            navController = remember {
                createNavController(context)
            }

            NavHost(navController, startDestination = state.value) {
                test("first")
                test("second")
            }
        }

        runOnUiThread { state.value = "second" }

        composeTestRule.runOnIdle {
            assertWithMessage("Second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
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
            navController = remember {
                createNavController(context)
            }
            if (state.value == 0) {
                NavHost(navController, startDestination = "first") { test("first") }
            }
        }

        runOnUiThread {
            // dispose the NavHost
            state.value = 1
        }

        // wait for recompose without NavHost then recompose with the NavHost
        composeTestRule.runOnIdle { state.value = 0 }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("first")
        }
    }

    @Test
    fun testDialogSavedAfterConfigChange() {
        lateinit var navController: NavHostController
        val defaultText = "dialogText"
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "dialog") {
                dialog("dialog") { Text(defaultText) }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun testViewModelSavedAfterConfigChange() {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel: TestViewModel
        var savedState: Bundle? = null
        composeTestRule.setContent {
            val context = LocalContext.current
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                navController =
                    if (savedState == null) {
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
        }
        val savedViewModel: TestViewModel = viewModel
        savedViewModel.value = "testing"
        savedState = navController.saveState()

        runOnUiThread {
            // dispose the NavHost
            state.value = 1
            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        }

        // wait for recompose without NavHost then recompose with the NavHost
        composeTestRule.runOnIdle {
            state.value = 0
            lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("first")
            assertThat(savedViewModel.value).isEqualTo(viewModel.value)
        }
    }

    @Test
    fun testViewModelClearedAfterPopWithConfigChange() {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel: TestViewModel
        composeTestRule.setContent {
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                navController = rememberNavController()

                if (state.value == 0) {
                    NavHost(navController, route = "graph", startDestination = "first") {
                        composable("first") {}
                        composable("second") { viewModel = viewModel<TestViewModel>() }
                    }
                }
            }
        }

        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")

        runOnUiThread { navController.navigate("second") }

        composeTestRule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")
            assertThat(viewModel.wasCleared).isFalse()
        }

        runOnUiThread {
            navController.popBackStack("second", inclusive = true, saveState = false)
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")
            // dispose the NavHost and move to destroy to simulate config change
            state.value = 1
            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        }

        composeTestRule.runOnIdle { assertThat(viewModel.wasCleared).isTrue() }
    }

    @Test
    fun testViewModelClearedAfterPopMultipleWithConfigChange() {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel_second: TestViewModel
        lateinit var viewModel_third: TestViewModel

        composeTestRule.setContent {
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                navController = rememberNavController()

                if (state.value == 0) {
                    NavHost(navController, route = "graph", startDestination = "first") {
                        composable("first") {}
                        composable("second") { viewModel_second = viewModel<TestViewModel>() }
                        composable("third") { viewModel_third = viewModel<TestViewModel>() }
                    }
                }
            }
        }

        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")

        runOnUiThread { navController.navigate("second") }

        composeTestRule.waitForIdle()

        runOnUiThread { navController.navigate("third") }

        composeTestRule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("third")
            assertThat(navController.currentBackStack.value.map { it.destination.route })
                .containsExactly("graph", "first", "second", "third")
                .inOrder()
            assertThat(viewModel_second.wasCleared).isFalse()
            assertThat(viewModel_third.wasCleared).isFalse()
        }

        runOnUiThread {
            navController.popBackStack("second", inclusive = true, saveState = false)
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")
            // dispose the NavHost and move to destroy to simulate config change
            state.value = 1
            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        }

        composeTestRule.runOnIdle {
            assertThat(viewModel_second.wasCleared).isTrue()
            assertThat(viewModel_third.wasCleared).isTrue()
        }
    }

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
    fun testStateOfInactiveScreenIsRestoredWhenWeGoBackToIt() {
        var increment = 0
        var numberOnScreen1 = -1
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") { numberOnScreen1 = rememberSaveable { increment++ } }
                composable("Second") {}
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1).isEqualTo(0)
            numberOnScreen1 = -1
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle { navController.popBackStack() }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored").that(numberOnScreen1).isEqualTo(0)
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
                composable("First") {}
                composable("Second") { numberOnScreen2 = rememberSaveable { increment++ } }
            }
        }

        composeTestRule.runOnIdle { navController.navigate("Second") }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2).isEqualTo(0)
            numberOnScreen2 = -1
            navController.popBackStack()
        }

        composeTestRule.runOnIdle { navController.navigate("Second") }

        composeTestRule.runOnIdle {
            assertWithMessage("The number shouldn't be restored").that(numberOnScreen2).isEqualTo(1)
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
    fun setSameGraph() {
        var currentGraph by mutableStateOf<NavGraph?>(null)
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            graph2 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            currentGraph = graph1
            NavHost(navController, currentGraph!!)
        }

        composeTestRule.runOnIdle { navController.navigate("Second") }

        composeTestRule.runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }

        composeTestRule.runOnIdle { currentGraph = graph2 }

        composeTestRule.runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }
    }

    @Test
    fun setSameGraph_replacesGraphDestination() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            graph2 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
            // make sure the two graphs are equal but different instances
            assertThat(graph1).isEqualTo(graph2)
            assertThat(graph1).isNotSameInstanceAs(graph2)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            // make sure navController didn't replace graph1 with graph2 since they are considered
            // same graphs
            assertThat(navController.graph).isSameInstanceAs(graph1)
            assertThat(navController.graph).isNotSameInstanceAs(graph2)

            // even though we didn't replace graph1, graph1's entry destinations should be
            // replaced with graph2's new destination instances
            graph1.onEachIndexed { index, node ->
                val otherNode = graph2Nodes[index]
                assertThat(node).isEqualTo(otherNode)
                assertThat(node).isSameInstanceAs(otherNode)
            }
        }
    }

    @Test
    fun setSameGraphWithRoutes_replacesGraphDestination() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(route = "route", startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            graph2 =
                navController.createGraph(route = "route", startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
            // make sure the two graphs are equal but different instances
            assertThat(graph1).isEqualTo(graph2)
            assertThat(graph1).isNotSameInstanceAs(graph2)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            // make sure navController didn't replace graph1 with graph2 since they are considered
            // same graphs
            assertThat(navController.graph).isSameInstanceAs(graph1)
            assertThat(navController.graph).isNotSameInstanceAs(graph2)

            // even though we didn't replace graph1, graph1's entry destinations should be
            // replaced with graph2's new destination instances
            graph1.onEachIndexed { index, node ->
                val otherNode = graph2Nodes[index]
                assertThat(node).isEqualTo(otherNode)
                assertThat(node).isSameInstanceAs(otherNode)
            }
        }
    }

    @Test
    fun setSameGraphWithNestedGraph_replacesNestedGraphDestinations() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    navigation(startDestination = "Third", route = "Second") {
                        composable("Third") {}
                        composable("Fourth") {}
                    }
                }
            graph2 =
                navController.createGraph(startDestination = "First") {
                    composable("First") {}
                    navigation(startDestination = "Third", route = "Second") {
                        composable("Third") {}
                        composable("Fourth") {}
                    }
                }
            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            // make sure navController didn't replace graph1 with graph2 since they are considered
            // same graphs
            assertThat(navController.graph).isSameInstanceAs(graph1)

            // even though we didn't replace graph1, graph1's entry destinations should be
            // replaced with graph2's new destination instances
            graph1.onEachIndexed { index, node ->
                val otherNode = graph2Nodes[index]
                assertThat(node).isEqualTo(otherNode)
                assertThat(node).isSameInstanceAs(otherNode)
            }

            // check that nested graphs/destinations are also replaced
            val graph2NestedNodes = (graph2Nodes.get(1) as NavGraph).toMutableList()
            (graph1.nodes.valueAt(1) as NavGraph).onEachIndexed { index, node ->
                val otherNode = graph2NestedNodes[index]
                assertThat(node).isEqualTo(otherNode)
                assertThat(node).isSameInstanceAs(otherNode)
            }
        }
    }

    @Test
    fun setSameGraphWithNestedGraph_updatesNavControllerBackstack() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    navigation(route = "Second", startDestination = "Third") {
                        composable("Third") {}
                        composable("Fourth") {}
                    }
                }
            graph2 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    navigation(route = "Second", startDestination = "Third") {
                        composable("Third") {}
                        composable("Fourth") {}
                    }
                }
            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            navController.navigate("Fourth")
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("Fourth")
            // Root, First, Second, Fourth
            assertThat(navController.currentBackStack.value.size).isEqualTo(4)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            // make sure NavController backQueue is updated with new nested destinations
            val entryDestinations =
                navController.currentBackStack.value
                    .filter { !it.destination.route.equals("Root") }
                    .map { it.destination }

            val entryRoutes = entryDestinations.map { it.route }
            assertThat(entryRoutes)
                .containsExactlyElementsIn(listOf("First", "Second", "Fourth"))
                .inOrder()

            assertThat(entryDestinations[0]).isSameInstanceAs(graph2Nodes[0]) // First
            assertThat(entryDestinations[1]).isSameInstanceAs(graph2Nodes[1]) // Second
            // make sure nested node is updated
            val nestedNode = (graph2Nodes[1] as NavGraph).nodes.valueAt(1)
            assertThat(nestedNode.route).isEqualTo("Fourth")
            assertThat(entryDestinations[2]).isSameInstanceAs(nestedNode)
        }
    }

    @Test
    fun setSameGraphWithNestedGraphDuplicatedRoutes_updatesNavControllerBackstack() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    navigation(route = "Second", startDestination = "Third") {
                        composable("Third") {}
                        navigation(route = "Fourth", startDestination = "First") {
                            composable("First") {}
                        }
                    }
                }
            graph2 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    navigation(route = "Second", startDestination = "Third") {
                        composable("Third") {}
                        navigation(route = "Fourth", startDestination = "First") {
                            composable("First") {}
                        }
                    }
                }
            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("First")

            // navigate to duplicated destination
            navController.navigate("Fourth")
            // Root, First, Second, Fourth, First
            assertThat(navController.currentBackStack.value.size).isEqualTo(5)
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("First")
            // make sure current destination's parent is the nested graph
            assertThat(navController.currentBackStackEntry?.destination?.parent?.route)
                .isEqualTo("Fourth")
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            val entryDestinations =
                navController.currentBackStack.value
                    .filter { !it.destination.route.equals("Root") }
                    .map { it.destination }

            val entryRoutes = entryDestinations.map { it.route }
            assertThat(entryRoutes)
                .containsExactlyElementsIn(listOf("First", "Second", "Fourth", "First"))
                .inOrder()

            // make sure duplicated nodes are updated with correct instances
            val dup1 = graph2Nodes[0]
            assertThat(dup1.route).isEqualTo("First")
            assertThat(entryDestinations[0]).isSameInstanceAs(dup1)

            val dup2 = ((graph2Nodes[1] as NavGraph).nodes.valueAt(1) as NavGraph).nodes.valueAt(0)
            assertThat(dup2.route).isEqualTo("First")
            assertThat(entryDestinations[3]).isSameInstanceAs(dup2)
        }
    }

    @Test
    fun setSameGraph_findsExistingHierarchyWhenNavigating() {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            graph1 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }
            graph2 =
                navController.createGraph(route = "Root", startDestination = "First") {
                    composable("First") {}
                    composable("Second") {}
                }

            NavHost(navController, graph1)
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Current destination should be First")
                .that(navController.currentDestination?.route)
                .isEqualTo("First")
        }

        // set same graph
        navController.setGraph(graph2, null)

        composeTestRule.runOnIdle {
            // When navigating to Second, NavController should find an instance of it already
            // within current NavGraph and does not rebuild its hierarchy when navigating
            navController.navigate("Second")

            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }
        // Root, First, Second
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
        val entryRoutes = navController.currentBackStack.value.map { it.destination.route }
        // ensure that Root did not get added a second time
        assertThat(entryRoutes)
            .containsExactlyElementsIn(listOf("Root", "First", "Second"))
            .inOrder()
    }

    @Test
    fun testNavHostAnimations() {
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
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { navController.navigate(second) }

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        assertThat(navController.visibleEntries.value)
            .containsExactly(firstEntry, navController.currentBackStackEntry)
            .inOrder()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.mainClock.autoAdvance = false

        val secondEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle { navController.popBackStack() }

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)

        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testNavHostAnimationsBackInterrupt() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) {
                    Scaffold {
                        NavHost(rememberNavController(), startDestination = "one") {
                            composable("one") {
                                BasicText("one")
                                viewModel<TestViewModel>()
                            }
                        }
                    }
                }
                composable(second) {}
            }
        }

        val firstEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        val secondEntry = navController.currentBackStackEntry

        composeTestRule.runOnIdle {
            navController.popBackStack()
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
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
    fun testStateSaved() {
        lateinit var navController: NavHostController
        lateinit var text: MutableState<String>

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, "start") {
                composable("start") {
                    text = rememberSaveable { mutableStateOf("") }
                    Column { TextField(value = text.value, onValueChange = { text.value = it }) }
                }
                composable("second") {}
            }
        }

        composeTestRule.onNodeWithText("test").assertDoesNotExist()

        text.value = "test"

        composeTestRule.onNodeWithText("test").assertExists()

        composeTestRule.runOnIdle {
            navController.navigate("second") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate("start") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }

        composeTestRule.onNodeWithText("test").assertExists()
    }

    @Test
    fun testGetGraphViewModel() {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, first) {
                composable(first) {}
                navigation(second, "subGraph") {
                    composable(second) {
                        model = viewModel(remember { navController.getBackStackEntry("subGraph") })
                    }
                }
            }
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        composeTestRule.runOnIdle { navController.popBackStack() }

        assertThat(model.wasCleared).isFalse()

        composeTestRule.waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testGetDialogViewModel() {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, first) {
                composable(first) {}
                dialog(second) { model = viewModel(it) }
            }
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        composeTestRule.runOnIdle { navController.popBackStack() }

        assertThat(model.wasCleared).isFalse()

        composeTestRule.waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testGetGraphViewModelAfterRecompose() {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        composeTestRule.setContent {
            navController = rememberNavController()
            // this causes a recompose
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            // this causes the NavHost to be recomposed with different builder so the graph
            // instance is different
            navBackStackEntry?.destination
            NavHost(navController, first) {
                composable(first) {}
                navigation(second, "subGraph") {
                    composable(second) {
                        model = viewModel(remember { navController.getBackStackEntry("subGraph") })
                    }
                }
            }
        }

        composeTestRule.runOnIdle { navController.navigate(second) }

        composeTestRule.runOnIdle { navController.popBackStack() }

        assertThat(model.wasCleared).isFalse()

        composeTestRule.waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testNestedNavHostNullLambda() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                navigation(second, "subGraph", enterTransition = { null }) {
                    composable(second) { BasicText(second) }
                }
            }
        }

        composeTestRule.runOnIdle { navController.navigate(second) }
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
    fun navBackStackEntryLifecycleTest() {
        var stopCount = 0
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "First") {
                composable("First") {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) {
                                stopCount++
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not have been stopped").that(stopCount).isEqualTo(0)
        }
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

    @Test(expected = IllegalStateException::class)
    fun nestedNavHostRestore() {
        lateinit var navController: NavHostController
        lateinit var innerNavController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            innerNavController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) {
                    NavHost(innerNavController, "nested1") {
                        composable("nested1") {}
                        composable("nested2") {}
                    }
                }
                composable(second) {}
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate(second) {
                popUpTo(first) {
                    inclusive = true
                    saveState = true
                }
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate(first) {
                restoreState = true
                popUpTo(second) { inclusive = true }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(first)

            // Setting graph early to force failure on test thread
            innerNavController.graph =
                innerNavController.createGraph(startDestination = "nested1") {
                    composable("nested1") {}
                    composable("nested2") {}
                }
        }

        composeTestRule.waitForIdle()
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
private const val third = "third"

class TestViewModel : ViewModel() {
    var value: String = "nothing"
    var wasCleared = false

    override fun onCleared() {
        super.onCleared()
        wasCleared = true
    }
}
