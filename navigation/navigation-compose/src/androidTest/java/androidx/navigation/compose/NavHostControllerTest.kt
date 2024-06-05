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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NoOpNavigator
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostControllerTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testRememberNavController() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            // get state to trigger recompose on navigate
            navController.currentBackStackEntryAsState().value
            NavHost(navController, startDestination = "first") {
                composable("first") { BasicText("first") }
                composable("second") { BasicText("second") }
            }
        }

        val navigator =
            composeTestRule.runOnIdle { navController.navigatorProvider[ComposeNavigator::class] }

        // trigger recompose
        composeTestRule.runOnIdle { navController.navigate("second") }

        composeTestRule.runOnIdle {
            assertThat(navController.navigatorProvider[ComposeNavigator::class])
                .isEqualTo(navigator)
        }
    }

    @Test
    fun testRememberNavControllerAddsCustomNavigator() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
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
            composeTestRule.runOnIdle { navController.navigatorProvider[NoOpNavigator::class] }

        // trigger recompose
        composeTestRule.runOnIdle { navController.navigate("second") }

        composeTestRule.runOnIdle {
            assertThat(navController.navigatorProvider[NoOpNavigator::class]).isEqualTo(navigator)
        }
    }

    @Test
    fun testCurrentBackStackEntrySetGraph() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        composeTestRule.setContent {
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
    fun testCurrentBackStackEntryNavigate() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
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

        composeTestRule.runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
            navController.popBackStack()
        }

        assertWithMessage("the currentBackStackEntry should return to first destination after pop")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testNavigateThenNavigateWithPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
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

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) { popUpTo("first") { inclusive = true } }
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
        assertWithMessage("the second destination should be the only one on the back stack")
            .that(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testNavigateOptionSingleTop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
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

        composeTestRule.runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        assertWithMessage("there should be 2 destinations on the back stack after navigate")
            .that(navigator.backStack.size)
            .isEqualTo(2)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) { launchSingleTop = true }
        }

        assertWithMessage("there should be 2 destination on back stack when using singleTop")
            .that(navigator.backStack.size)
            .isEqualTo(2)
    }

    @Test
    fun testNavigateOptionSingleTopDifferentArguments() {
        var value = ""
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first?arg={arg}") {
                composable("first?arg={arg}") { entry ->
                    if (entry.arguments?.containsKey("arg") == true) {
                        value = entry.arguments?.getString("arg", "").toString()
                    }
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate("first?arg=value2") { launchSingleTop = true }
        }
        composeTestRule.runOnIdle {
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
    fun testNavigateOptionSingleTopDifferentListArguments() {
        var value: List<String> = listOf()
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first?arg=value1&arg=value2") {
                composable(
                    "first?arg={arg}",
                    arguments = listOf(navArgument("arg") { type = NavType.StringListType })
                ) { entry ->
                    if (entry.arguments?.containsKey("arg") == true) {
                        value = NavType.StringListType.get(entry.arguments!!, "arg")!!
                    }
                }
            }
        }
        composeTestRule.runOnUiThread {
            assertThat(value).containsExactly("value1", "value2")
            navController.navigate("first?arg=value3&arg=value4") { launchSingleTop = true }
        }
        composeTestRule.runOnIdle {
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
    fun testNavigateKClass() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {}
            }
        }

        composeTestRule.runOnUiThread { navController.navigate(TestClass()) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigateKClassArgsBundle() {
        lateinit var args: TestClassArg
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClassArg> { args = it.toRoute<TestClassArg>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(args.arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateKClassArgsSavedStateHandle() {
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(vm.handle.toRoute<TestClassArg>().arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateKClassCustomArgsSavedStateHandle() {
        @Serializable
        class CustomType(val nestedArg: Int) : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        val navType =
            object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) {
                    bundle.putString(key, value.nestedArg.toString())
                }

                override fun get(bundle: Bundle, key: String): CustomType =
                    CustomType(nestedArg = bundle.getString(key)!!.toInt())

                override fun parseValue(value: String): CustomType = CustomType(value.toInt())

                override fun serializeAsValue(value: CustomType) = value.nestedArg.toString()
            }

        @Serializable class TestClass(val arg: CustomType)

        val typeMap = mapOf(typeOf<CustomType>() to navType)
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass>(typeMap) {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(CustomType(12))) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.hasRoute<TestClass>()).isTrue()
            assertThat(vm.handle.toRoute<TestClass>(typeMap).arg.nestedArg).isEqualTo(12)
        }
    }

    @Test
    fun testNavigateKClassMultipleArgsBundle() {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateKClassMultipleArgsBundle.TestClass/{arg}/{arg2}"
                )
            assertThat(args.arg).isEqualTo(1)
            assertThat(args.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateKClassMultipleArgsSavedStateHandle() {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateKClassMultipleArgsSavedStateHandle.TestClass/{arg}/{arg2}"
                )
            val vmRoute = vm.handle.toRoute<TestClass>()
            assertThat(vmRoute.arg).isEqualTo(1)
            assertThat(vmRoute.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateKClassArgsNullValueBundle() {
        @Serializable class TestClass(val arg: String?)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(null)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateKClassArgsNullValueBundle.TestClass/{arg}"
                )
            assertThat(args.arg).isNull()
        }
    }

    @Test
    fun testNavigateKClassArgsNullValueSavedStateHandle() {
        @Serializable class TestClass(val arg: String?)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClass(null)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateKClassArgsNullValueSavedStateHandle.TestClass/{arg}"
                )
            assertThat(vm.handle.toRoute<TestClass>().arg).isNull()
        }
    }

    @Test
    fun testNavigateDialogKClass() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> {}
            }
        }

        composeTestRule.runOnUiThread { navController.navigate(TestClass()) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsBundle() {
        lateinit var bundle: TestClassArg
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClassArg> { bundle = it.toRoute<TestClassArg>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(bundle.arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsSavedStateHandle() {
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClassArg(1)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(vm.handle.toRoute<TestClassArg>().arg).isEqualTo(1)
        }
    }

    @Test
    fun testNavigateDialogKClassMultipleArgsBundle() {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateDialogKClassMultipleArgsBundle.TestClass/{arg}/{arg2}"
                )
            assertThat(args.arg).isEqualTo(1)
            assertThat(args.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateDialogKClassMultipleArgsSavedStateHandle() {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClass(1, false)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateDialogKClassMultipleArgsSavedStateHandle.TestClass/{arg}/{arg2}"
                )
            val vmRoute = vm.handle.toRoute<TestClass>()
            assertThat(vmRoute.arg).isEqualTo(1)
            assertThat(vmRoute.arg2).isEqualTo(false)
        }
    }

    @Test
    fun testNavigateDialogKClassArgsNullValueBundle() {
        @Serializable class TestClass(val arg: String?)

        lateinit var args: TestClass
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                dialog<TestClass> { args = it.toRoute<TestClass>() }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(null)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateDialogKClassArgsNullValueBundle.TestClass/{arg}"
                )
            assertThat(args.arg).isNull()
        }
    }

    @Test
    fun testNavigateDialogKClassArgsNullValueSavedStateHandle() {
        @Serializable class TestClass(val arg: String?)

        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
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
        composeTestRule.runOnUiThread { navController.navigate(TestClass(null)) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.route)
                .isEqualTo(
                    "androidx.navigation.compose.NavHostControllerTest." +
                        "testNavigateDialogKClassArgsNullValueSavedStateHandle.TestClass/{arg}"
                )
            assertThat(vm.handle.toRoute<TestClass>().arg).isNull()
        }
    }

    @Test
    fun testGetBackStackEntry() {
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }
        }

        composeTestRule.runOnUiThread { navController.navigate(SECOND_DESTINATION) }

        assertWithMessage("first destination should be on back stack")
            .that(navController.getBackStackEntry(FIRST_DESTINATION).destination.route)
            .isEqualTo(FIRST_DESTINATION)

        assertWithMessage("second destination should be on back stack")
            .that(navController.getBackStackEntry(SECOND_DESTINATION).destination.route)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testGetBackStackEntryNoEntryFound() {
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph =
                navController.createGraph(startDestination = FIRST_DESTINATION) {
                    test(FIRST_DESTINATION)
                    test(SECOND_DESTINATION)
                }
        }

        composeTestRule.runOnUiThread { navController.navigate(SECOND_DESTINATION) }

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
    fun testGetBackStackEntryKClass() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass> {}
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass()) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.getBackStackEntry<TestClass>().destination.route)
                .isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    class TestVM(val handle: SavedStateHandle) : ViewModel()
}

private const val FIRST_DESTINATION = "first"
private const val SECOND_DESTINATION = "second"
