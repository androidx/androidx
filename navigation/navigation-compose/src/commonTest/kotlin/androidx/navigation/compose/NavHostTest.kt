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

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.contains
import androidx.navigation.createGraph
import androidx.navigation.navigation
import androidx.navigation.plusAssign
import androidx.navigation.testing.TestNavHostController
import androidx.savedstate.SavedState
import androidx.testutils.TestNavigator
import androidx.testutils.test
import kotlin.reflect.KClass
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class NavHostTest {
    
    @Test
    fun testSingleDestinationSet() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = createNavController()

            NavHost(navController, startDestination = "first") { test("first") }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()
    }

    @Test
    fun testNavigate() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = createNavController()

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
    fun testNavigateOutsideStateChange() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        val text = "myButton"
        var counter = 0
        setContent {
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

        runOnIdle { navController.navigate("second") }

        runOnIdle {
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
        }

        onNodeWithText(text).performClick()

        runOnIdle {
            // ensure our click listener was fired
            assertThat(counter).isEqualTo(1)
            assertWithMessage("second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
        }
    }

    @Test
    fun testPop() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = createNavController()

            NavHost(navController, startDestination = "first") {
                test("first")
                test("second")
            }
        }

        runOnUiThread {
            navController.navigate("second")
            navController.popBackStack()
        }

        assertWithMessage("First destination should be current")
            .that(navController.currentDestination?.route)
            .isEqualTo("first")
    }

    @Test
    fun testChangeStartDestination() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<String>
        setContent {
            state = remember { mutableStateOf("first") }
            navController = createNavController()

            NavHost(navController, startDestination = state.value) {
                test("first")
                test("second")
            }
        }

        runOnUiThread { state.value = "second" }

        runOnIdle {
            assertWithMessage("Second destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("second")
        }
    }

    @Test
    fun testSameControllerAfterDisposingNavHost() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<Int>
        setContent {
            state = remember { mutableStateOf(0) }
            navController = createNavController()
            if (state.value == 0) {
                NavHost(navController, startDestination = "first") { test("first") }
            }
        }

        runOnUiThread {
            // dispose the NavHost
            state.value = 1
        }

        // wait for recompose without NavHost then recompose with the NavHost
        runOnIdle { state.value = 0 }

        runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("first")
        }
    }

    @Test
    fun testDialogSavedAfterConfigChange() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        val defaultText = "dialogText"
        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "dialog") {
                dialog("dialog") { Text(defaultText) }
            }
        }

        waitForIdle()

        onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun testViewModelSavedAfterConfigChange() = runComposeUiTestOnUiThread {
        val ttt = TestViewModelStoreOwnerWithDefaults()
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel: TestViewModel
        var savedState: SavedState? = null
        setContent {
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides ttt,
                LocalLifecycleOwner provides lifecycleOwner
            ) {
                navController =
                    if (savedState == null) {
                        rememberNavController()
                    } else {
                        NavHostController().apply {
                            restoreState(savedState)
                            setViewModelStore(LocalViewModelStoreOwner.current!!.viewModelStore)
                            navigatorProvider += ComposeNavigator()
                            navigatorProvider += DialogNavigator()
                        }
                    }
                if (state.value == 0) {
                    NavHost(navController, startDestination = "first") {
                        composable("first") {
                            val provider = ViewModelProvider.create(it, TestViewModelFactory())
                            println("DESTINATION ID = ${it.id}")
                            viewModel = provider.get("key", TestViewModel::class)
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
        runOnIdle {
            state.value = 0
            lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        }

        runOnIdle {
            assertWithMessage("First destination should be current")
                .that(navController.currentDestination?.route)
                .isEqualTo("first")
            assertThat(savedViewModel.value).isEqualTo(viewModel.value)
        }
    }

    @Test
    fun testViewModelClearedAfterPopWithConfigChange() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel: TestViewModel
        setContent {
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                navController = rememberNavController()

                if (state.value == 0) {
                    NavHost(navController, route = "graph", startDestination = "first") {
                        composable("first") {}
                        composable("second") {
                            viewModel = viewModel<TestViewModel>(factory = TestViewModelFactory())
                        }
                    }
                }
            }
        }

        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")

        runOnUiThread { navController.navigate("second") }

        runOnIdle {
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

        runOnIdle { assertThat(viewModel.wasCleared).isTrue() }
    }

    @Test
    fun testViewModelClearedAfterPopMultipleWithConfigChange() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        var lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var state: MutableState<Int>
        lateinit var viewModel_second: TestViewModel
        lateinit var viewModel_third: TestViewModel

        setContent {
            state = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                navController = rememberNavController()

                if (state.value == 0) {
                    NavHost(navController, route = "graph", startDestination = "first") {
                        composable("first") {}
                        composable("second") {
                            viewModel_second = viewModel<TestViewModel>(
                                factory = TestViewModelFactory()
                            )
                        }
                        composable("third") {
                            viewModel_third = viewModel<TestViewModel>(
                                factory = TestViewModelFactory()
                            )
                        }
                    }
                }
            }
        }

        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("first")

        runOnUiThread { navController.navigate("second") }

        waitForIdle()

        runOnUiThread { navController.navigate("third") }

        runOnIdle {
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

        runOnIdle {
            assertThat(viewModel_second.wasCleared).isTrue()
            assertThat(viewModel_third.wasCleared).isTrue()
        }
    }

    @Test
    fun testStateOfInactiveScreenIsRestoredWhenWeGoBackToIt() = runComposeUiTestOnUiThread {
        var increment = 0
        var numberOnScreen1 = -1
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") { numberOnScreen1 = rememberSaveable { increment++ } }
                composable("Second") {}
            }
        }

        runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1).isEqualTo(0)
            numberOnScreen1 = -1
            navController.navigate("Second")
        }

        runOnIdle { navController.popBackStack() }

        runOnIdle {
            assertWithMessage("The number should be restored").that(numberOnScreen1).isEqualTo(0)
        }
    }

    @Test
    fun stateForScreenRemovedFromBackStackIsNotRestored() = runComposeUiTestOnUiThread {
        var increment = 0
        var numberOnScreen2 = -1
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {}
                composable("Second") { numberOnScreen2 = rememberSaveable { increment++ } }
            }
        }

        runOnIdle { navController.navigate("Second") }

        runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2).isEqualTo(0)
            numberOnScreen2 = -1
            navController.popBackStack()
        }

        runOnIdle { navController.navigate("Second") }

        runOnIdle {
            assertWithMessage("The number shouldn't be restored").that(numberOnScreen2).isEqualTo(1)
        }
    }

    @Test
    fun setSameGraph() = runComposeUiTestOnUiThread {
        var currentGraph by mutableStateOf<NavGraph?>(null)
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController
        setContent {
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

        runOnIdle { navController.navigate("Second") }

        runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }

        runOnIdle { currentGraph = graph2 }

        runOnIdle {
            assertWithMessage("Current destination should be Second")
                .that(navController.currentDestination?.route)
                .isEqualTo("Second")
        }
    }

    @Test
    fun setSameGraph_replacesGraphDestination() = runComposeUiTestOnUiThread {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        setContent {
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

        runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
            // make sure the two graphs are equal but different instances
            assertThat(graph1).isEqualTo(graph2)
            assertThat(graph1).isNotSameInstanceAs(graph2)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        runOnIdle {
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
    fun setSameGraphWithRoutes_replacesGraphDestination() = runComposeUiTestOnUiThread {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        setContent {
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

        runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
            // make sure the two graphs are equal but different instances
            assertThat(graph1).isEqualTo(graph2)
            assertThat(graph1).isNotSameInstanceAs(graph2)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        runOnIdle {
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
    fun setSameGraphWithNestedGraph_replacesNestedGraphDestinations() = runComposeUiTestOnUiThread {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        setContent {
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

        runOnIdle {
            // check current graph is graph1
            assertThat(navController.graph).isSameInstanceAs(graph1)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        runOnIdle {
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
            val graph1NestedNodes =
                (graph1.toList().first { it.route == "Second" } as NavGraph).toList()
            val graph2NestedNodes =
                (graph2Nodes.first { it.route == "Second" } as NavGraph).toList()
            graph1NestedNodes.onEach { node ->
                val otherNode = graph2NestedNodes.first { it.route == node.route }
                assertThat(node).isEqualTo(otherNode)
                assertThat(node).isSameInstanceAs(otherNode)
            }
        }
    }

    @Test
    fun setSameGraphWithNestedGraph_updatesNavControllerBackstack() = runComposeUiTestOnUiThread {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController

        setContent {
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

        runOnIdle {
            navController.navigate("Fourth")
            assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("Fourth")
            // Root, First, Second, Fourth
            assertThat(navController.currentBackStack.value.size).isEqualTo(4)
        }

        // copy to assert later on that graph1 nodes replaced by graph2 nodes instead of vice versa
        val graph2Nodes = graph2.toMutableList()
        navController.setGraph(graph2, null)

        runOnIdle {
            // make sure NavController backQueue is updated with new nested destinations
            val entryDestinations =
                navController.currentBackStack.value
                    .filter { !it.destination.route.equals("Root") }
                    .map { it.destination }

            val entryRoutes = entryDestinations.map { it.route }
            assertThat(entryRoutes)
                .containsExactlyElementsIn(listOf("First", "Second", "Fourth"))
                .inOrder()

            assertThat(entryDestinations.first { it.route == "First" }).isSameInstanceAs(graph2Nodes.first { it.route == "First" }) // First
            assertThat(entryDestinations.first { it.route == "Second" }).isSameInstanceAs(
                graph2Nodes.first { it.route == "Second" }) // Second
            // make sure nested node is updated
            val nestedNode =
                (graph2Nodes.first { it.route == "Second" } as NavGraph).first { it.route == "Fourth" }
            assertThat(entryDestinations[2]).isSameInstanceAs(nestedNode)
        }
    }

    @Test
    fun setSameGraphWithNestedGraphDuplicatedRoutes_updatesNavControllerBackstack() =
        runComposeUiTestOnUiThread {
            lateinit var graph1: NavGraph
            lateinit var graph2: NavGraph
            lateinit var navController: NavHostController

            setContent {
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

            runOnIdle {
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

            runOnIdle {
                val entryDestinations =
                    navController.currentBackStack.value
                        .filter { !it.destination.route.equals("Root") }
                        .map { it.destination }

                val entryRoutes = entryDestinations.map { it.route }
                assertThat(entryRoutes)
                    .containsExactlyElementsIn(listOf("First", "Second", "Fourth", "First"))
                    .inOrder()

                // make sure duplicated nodes are updated with correct instances
                val dup1 = graph2Nodes.first { it.route == "First" }
                assertThat(entryDestinations[0]).isSameInstanceAs(dup1)

                val dup2 = graph2Nodes.filterIsInstance<NavGraph>().single().toList()
                    .filterIsInstance<NavGraph>().single().toList()
                    .first { it.route == "First" }
                assertThat(entryDestinations[3]).isSameInstanceAs(dup2)
            }
        }

    @Test
    fun setSameGraph_findsExistingHierarchyWhenNavigating() = runComposeUiTestOnUiThread {
        lateinit var graph1: NavGraph
        lateinit var graph2: NavGraph
        lateinit var navController: NavHostController
        setContent {
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

        runOnIdle {
            assertWithMessage("Current destination should be First")
                .that(navController.currentDestination?.route)
                .isEqualTo("First")
        }

        // set same graph
        navController.setGraph(graph2, null)

        runOnIdle {
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
    fun testNavHostAnimations() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        mainClock.autoAdvance = false

        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                composable(second) { BasicText(second) }
            }
        }

        val firstEntry = navController.currentBackStackEntry

        mainClock.autoAdvance = true

        runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        mainClock.autoAdvance = false

        runOnIdle { navController.navigate(second) }

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        // advance half way between animations
        mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)

        onNodeWithText(first).assertExists()
        onNodeWithText(second).assertExists()

        assertThat(navController.visibleEntries.value)
            .containsExactly(firstEntry, navController.currentBackStackEntry)
            .inOrder()

        mainClock.autoAdvance = true

        runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
        }

        mainClock.autoAdvance = false

        val secondEntry = navController.currentBackStackEntry

        runOnIdle { navController.popBackStack() }

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)

        // advance half way between animations
        mainClock.advanceTimeBy(DefaultDurationMillis.toLong() / 2)

        assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.CREATED)

        onNodeWithText(first).assertExists()
        onNodeWithText(second).assertExists()

        mainClock.autoAdvance = true

        runOnIdle {
            assertThat(navController.currentBackStackEntry?.lifecycle?.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testNavHostAnimationsBackInterrupt() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) {
                    Scaffold {
                        NavHost(rememberNavController(), startDestination = "one") {
                            composable("one") {
                                BasicText("one")
                                viewModel<TestViewModel>(factory = TestViewModelFactory())
                            }
                        }
                    }
                }
                composable(second) {}
            }
        }

        val firstEntry = navController.currentBackStackEntry

        runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.RESUMED)
        }

        runOnIdle { navController.navigate(second) }

        val secondEntry = navController.currentBackStackEntry

        runOnIdle {
            navController.popBackStack()
            navController.popBackStack()
        }

        runOnIdle {
            assertThat(firstEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(secondEntry?.lifecycle?.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testStateSaved() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        lateinit var text: MutableState<String>

        setContent {
            navController = rememberNavController()
            NavHost(navController, "start") {
                composable("start") {
                    text = rememberSaveable { mutableStateOf("") }
                    Column { TextField(value = text.value, onValueChange = { text.value = it }) }
                }
                composable("second") {}
            }
        }

        onNodeWithText("test").assertDoesNotExist()

        text.value = "test"

        onNodeWithText("test").assertExists()

        runOnIdle {
            navController.navigate("second") {
                popUpTo(navController.graph.findStartDestination().route!!) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }

        runOnIdle {
            navController.navigate("start") {
                popUpTo(navController.graph.findStartDestination().route!!) { saveState = true }

                launchSingleTop = true
                restoreState = true
            }
        }

        onNodeWithText("test").assertExists()
    }

    @Test
    fun testGetGraphViewModel() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        setContent {
            navController = rememberNavController()
            NavHost(navController, first) {
                composable(first) {}
                navigation(second, "subGraph") {
                    composable(second) {
                        model = viewModel(
                            remember { navController.getBackStackEntry("subGraph") },
                            factory = TestViewModelFactory()
                        )
                    }
                }
            }
        }

        runOnIdle { navController.navigate(second) }

        waitForIdle()

        navController.popBackStack()

        assertThat(model.wasCleared).isFalse()

        waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testGetDialogViewModel() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        setContent {
            navController = rememberNavController()
            NavHost(navController, first) {
                composable(first) {}
                dialog(second) { model = viewModel(it, factory = TestViewModelFactory()) }
            }
        }

        runOnIdle { navController.navigate(second) }

        waitForIdle()

        navController.popBackStack()

        assertThat(model.wasCleared).isFalse()

        waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testGetGraphViewModelAfterRecompose() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        lateinit var model: TestViewModel

        setContent {
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
                        model = viewModel(remember { navController.getBackStackEntry("subGraph") }, factory = TestViewModelFactory())
                    }
                }
            }
        }

        runOnIdle { navController.navigate(second) }

        runOnIdle { navController.popBackStack() }

        assertThat(model.wasCleared).isFalse()

        waitForIdle()

        assertThat(model.wasCleared).isTrue()
    }

    @Test
    fun testNestedNavHostNullLambda() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = first) {
                composable(first) { BasicText(first) }
                navigation(second, "subGraph", enterTransition = { null }) {
                    composable(second) { BasicText(second) }
                }
            }
        }

        runOnIdle { navController.navigate(second) }
    }

    @Test
    fun navBackStackEntryLifecycleTest() = runComposeUiTestOnUiThread {
        var stopCount = 0
        lateinit var navController: NavHostController
        setContent {
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

        runOnIdle {
            assertWithMessage("Lifecycle should not have been stopped").that(stopCount).isEqualTo(0)
        }
    }

    @Test
    fun navBackStackEntrySingleTopLifecycleTest() = runComposeUiTestOnUiThread {
        var lastEvent: Lifecycle.Event? = null
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "First") {
                composable("First") {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event -> lastEvent = event }
                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                }
                composable("Second") {}
            }
        }

        runOnIdle { navController.navigate("Second") }

        runOnIdle {
            navController.navigate("First") {
                popUpTo("First")
                launchSingleTop = true
            }
        }

        runOnIdle {
            assertWithMessage("Lifecycle should have been resumed")
                .that(lastEvent)
                .isEqualTo(Lifecycle.Event.ON_RESUME)
        }
    }

    @Test
    fun testPopWithBackHandler() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()
            val innerNavController = rememberNavController()
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

        runOnIdle {
            navController.navigate(second) {
                popUpTo(first) {
                    inclusive = true
                    saveState = true
                }
            }
        }

        waitForIdle()
        navController.navigate(first) {
            restoreState = true
            popUpTo(second) { inclusive = true }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(first)
        }
    }

    @Test
    fun testLifecycleStateOnAtomicNavigateToComposableAndDialog() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        lateinit var screen1Lifecycle: State<Lifecycle.State>
        lateinit var screen2Lifecycle: State<Lifecycle.State>
        lateinit var dialogLifecycle: State<Lifecycle.State>
        setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = "screen1") {
                composable("screen1") {
                    screen1Lifecycle = it.lifecycle.currentStateFlow.collectAsState()
                    Text("screen1")
                }
                composable("screen2") {
                    screen2Lifecycle = it.lifecycle.currentStateFlow.collectAsState()
                    Text("screen1")
                }
                dialog("dialog") {
                    dialogLifecycle = it.lifecycle.currentStateFlow.collectAsState()
                    Text("dialog")
                }
            }
        }

        waitForIdle()
        onNodeWithText("screen1").assertIsDisplayed()
        assertThat(screen1Lifecycle.value).isEqualTo(Lifecycle.State.RESUMED)

        runOnUiThread {
            navController.navigate("screen2")
            navController.navigate("dialog")
        }

        waitForIdle()
        onNodeWithText("dialog").assertIsDisplayed()
        assertThat(screen2Lifecycle.value).isEqualTo(Lifecycle.State.STARTED)
        assertThat(dialogLifecycle.value).isEqualTo(Lifecycle.State.RESUMED)

        runOnUiThread { navController.popBackStack() }

        waitForIdle()
        assertThat(screen2Lifecycle.value).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Composable
    private fun createNavController(): TestNavHostController {
        val navController = TestNavHostController()
        val navigator = TestNavigator()
        navController.navigatorProvider += navigator
        return navController
    }
}

@Composable
internal expect fun TestNavHostController(): TestNavHostController

@Composable
internal expect fun NavHostController(): NavHostController

private const val first = "first"
private const val second = "second"
private const val third = "third"

internal class TestViewModel : ViewModel() {
    var value: String = "nothing"
    var wasCleared = false

    override fun onCleared() {
        super.onCleared()
        wasCleared = true
    }
}

@Suppress("UNCHECKED_CAST")
internal class TestViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return TestViewModel() as T
    }
}

private class TestViewModelStoreOwnerWithDefaults(
    override val viewModelStore: ViewModelStore = ViewModelStore(),
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = TestViewModelFactory(),
    override val defaultViewModelCreationExtras: CreationExtras = CreationExtras.Empty,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory
