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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NoOpNavigator
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import androidx.savedstate.read
import androidx.testutils.TestNavigator
import androidx.testutils.test
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@OptIn(
    ExperimentalTestApi::class,
    InternalSerializationApi::class,
    ExperimentalSerializationApi::class
)
class NavHostControllerTest {

    @Test
    fun testRememberNavController() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            navController = rememberNavController()
            // get state to trigger recompose on navigate
            navController.currentBackStackEntryAsState().value
            NavHost(navController, startDestination = "first") {
                composable("first") { BasicText("first") }
                composable("second") { BasicText("second") }
            }
        }

        val navigator =
            runOnIdle { navController.navigatorProvider[ComposeNavigator::class] }

        // trigger recompose
        runOnIdle { navController.navigate("second") }

        runOnIdle {
            assertThat(navController.navigatorProvider[ComposeNavigator::class])
                .isEqualTo(navigator)
        }
    }

    @Test
    fun testRememberNavControllerAddsCustomNavigator() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController

        setContent {
            val customNavigator = remember { NoOpNavigator() }
            navController = rememberNavController(customNavigator)
            // get state to trigger recompose on navigate
            navController.currentBackStackEntryAsState().value
            NavHost(navController, startDestination = "first") {
                composable("first") { BasicText("first") }
                composable("second") { BasicText("second") }
            }
        }

        val navigator =
            runOnIdle { navController.navigatorProvider[NoOpNavigator::class] }

        // trigger recompose
        runOnIdle { navController.navigate("second") }

        runOnIdle {
            assertThat(navController.navigatorProvider[NoOpNavigator::class]).isEqualTo(navigator)
        }
    }

    @Test
    fun testCurrentBackStackEntrySetGraph() = runComposeUiTestOnUiThread {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        setContent {
            val navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryNavigate() = runComposeUiTestOnUiThread {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)

        runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        waitForIdle()

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryPop() = runComposeUiTestOnUiThread {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
            navController.popBackStack()
        }

        assertWithMessage("the currentBackStackEntry should return to first destination after pop")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testNavigateThenNavigateWithPop() = runComposeUiTestOnUiThread {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        val navigator = navController.navigatorProvider[TestNavigator::class]

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        runOnUiThread {
            navController.navigate(SECOND_DESTINATION) { popUpTo("first") { inclusive = true } }
        }

        waitForIdle()

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
        assertWithMessage("the second destination should be the only one on the back stack")
            .that(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testNavigateOptionSingleTop() = runComposeUiTestOnUiThread {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        assertWithMessage("there should be 2 destinations on the back stack after navigate")
            .that(navigator.backStack.size)
            .isEqualTo(2)

        runOnUiThread {
            navController.navigate(SECOND_DESTINATION) { launchSingleTop = true }
        }

        assertWithMessage("there should be 2 destination on back stack when using singleTop")
            .that(navigator.backStack.size)
            .isEqualTo(2)
    }

    @Test
    fun testNavigateOptionSingleTopDifferentArguments() = runComposeUiTestOnUiThread {
        var value = ""
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first?arg={arg}") {
                composable("first?arg={arg}") { entry ->
                    entry.arguments?.read {
                        if (contains("arg") == true) {
                            value = getString("arg").toString()
                        }
                    }
                }
            }
        }

        runOnUiThread {
            navController.navigate("first?arg=value2") { launchSingleTop = true }
        }
        runOnIdle {
            val navigator =
                navController.navigatorProvider.get<ComposeNavigator>(
                    navController.currentDestination?.navigatorName!!
                )
            assertWithMessage("there should be 1 destination on back stack when using singleTop")
                .that(navigator.backStack.value.size)
                .isEqualTo(1)
            assertThat(value).isEqualTo("value2")
        }
    }

    @Test
    fun testNavigateOptionSingleTopDifferentListArguments() = runComposeUiTestOnUiThread {
        var value: List<String> = listOf()
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first?arg=value1&arg=value2") {
                composable(
                    "first?arg={arg}",
                    arguments = listOf(navArgument("arg") { type = NavType.StringListType }),
                ) { entry ->
                    if (entry.arguments?.read {  contains("arg") } == true) {
                        value = NavType.StringListType.get(entry.arguments!!, "arg")!!
                    }
                }
            }
        }
        runOnUiThread {
            assertThat(value).containsExactly("value1", "value2")
            navController.navigate("first?arg=value3&arg=value4") { launchSingleTop = true }
        }
        runOnIdle {
            val navigator =
                navController.navigatorProvider.get<ComposeNavigator>(
                    navController.currentDestination?.navigatorName!!
                )
            assertWithMessage("there should be 1 destination on back stack when using singleTop")
                .that(navigator.backStack.value.size)
                .isEqualTo(1)
            assertThat(value).containsExactly("value3", "value4")
        }
    }

    @Test
    fun testNavigateKClass() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {}
            }
        }

        runOnUiThread { navController.navigate(TestClass()) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigateKClassArgsBundle() = runComposeUiTestOnUiThread {
        lateinit var args: TestClassArg
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClassArg> { args = it.toRoute<TestClassArg>() }
            }
        }
        runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(args.arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateKClassArgsSavedStateHandle() = runComposeUiTestOnUiThread {
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClassArg> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(vm.handle.toRoute<TestClassArg>().arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateKClassMultipleArgsBundle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}/{arg2}"
                )
            assertThat(args.arg).isEqualTo(1)
            assertThat(args.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateKClassMultipleArgsSavedStateHandle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}/{arg2}"
                )
            val vmRoute = vm.handle.toRoute<TestClass>()
            assertThat(vmRoute.arg).isEqualTo(1)
            assertThat(vmRoute.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateKClassArgsNullValueBundle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: String?)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        runOnUiThread { navController.navigate(TestClass(null)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}"
                )
            assertThat(args.arg).isNull()
        }
    }

    @Test
    fun testNavigateKClassArgsNullValueSavedStateHandle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: String?)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClass(null)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}"
                )
            assertThat(vm.handle.toRoute<TestClass>().arg).isNull()
        }
    }

    @Test
    fun testNavigateDialogKClass() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> {}
            }
        }

        runOnUiThread { navController.navigate(TestClass()) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsBundle() = runComposeUiTestOnUiThread {
        lateinit var bundle: TestClassArg
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClassArg> { bundle = it.toRoute<TestClassArg>() }
            }
        }
        runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(bundle.arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsSavedStateHandle() = runComposeUiTestOnUiThread {
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClassArg> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(vm.handle.toRoute<TestClassArg>().arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateDialogKClassMultipleArgsBundle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}/{arg2}"
                )
            assertThat(args.arg).isEqualTo(1)
            assertThat(args.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateDialogKClassMultipleArgsSavedStateHandle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}/{arg2}"
                )
            val vmRoute = vm.handle.toRoute<TestClass>()
            assertThat(vmRoute.arg).isEqualTo(1)
            assertThat(vmRoute.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsNullValueBundle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: String?)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        runOnUiThread { navController.navigate(TestClass(null)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}"
                )
            assertThat(args.arg).isNull()
        }
    }

    @Test
    fun testNavigateDialogKClassArgsNullValueSavedStateHandle() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: String?)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        runOnUiThread { navController.navigate(TestClass(null)) {} }
        runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "${TestClass::class.serializer().descriptor.serialName}/{arg}"
                )
            assertThat(vm.handle.toRoute<TestClass>().arg).isNull()
        }
    }

    @Test
    fun testGetBackStackEntry() = runComposeUiTestOnUiThread {
        lateinit var navController: NavController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }
        }

        runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        assertWithMessage("first destination should be on back stack")
            .that(navController.getBackStackEntry(FIRST_DESTINATION).destination.route)
            .isEqualTo(FIRST_DESTINATION)

        assertWithMessage("second destination should be on back stack")
            .that(navController.getBackStackEntry(SECOND_DESTINATION).destination.route)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testGetBackStackEntryNoEntryFound() = runComposeUiTestOnUiThread {
        lateinit var navController: NavController
        setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }
        }

        runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        try {
            navController.getBackStackEntry(SECOND_DESTINATION)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "No destination with route $SECOND_DESTINATION is on the NavController's " +
                        "back stack. The current destination is " +
                        navController.currentBackStackEntry?.destination
                )
        }
    }

    @Test
    fun testGetBackStackEntryKClass() = runComposeUiTestOnUiThread {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {}
            }
        }
        runOnUiThread { navController.navigate(TestClass()) {} }
        runOnIdle {
            assertThat(navController.getBackStackEntry<TestClass>().destination.route)
                .isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    class TestVM(val handle: SavedStateHandle) : ViewModel()
}

private const val FIRST_DESTINATION = "first"
private const val SECOND_DESTINATION = "second"
