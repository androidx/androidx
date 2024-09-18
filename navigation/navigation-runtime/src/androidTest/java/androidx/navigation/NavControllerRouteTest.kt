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

package androidx.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.os.BundleSubject.assertThat
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavControllerRouteTest {

    val nav_simple_route_graph =
        createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
            test("start_test")
            test("start_test_with_default_arg") { argument("defaultArg") { defaultValue = true } }
            test("second_test/{arg2}") {
                argument("arg2") {
                    type = NavType.StringType
                    nullable = true
                }
                argument("defaultArg") {
                    type = NavType.StringType
                    defaultValue = "defaultValue"
                }
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test/{arg2}"
                    action = "test.action"
                    mimeType = "type/test"
                }
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test/{arg1}/{arg2}"
                }
            }
            test("nonNullableArg_test/{arg3}") {
                argument("arg3") {
                    type = NavType.StringType
                    nullable = false
                }
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test/test{arg3}"
                    action = "test.action2"
                    mimeType = "type/test2"
                }
            }
        }

    val nav_start_destination_route_graph =
        createNavController().createGraph(route = "graph", startDestination = "start_test") {
            test("start_test") {
                argument("test") {
                    type = NavType.StringType
                    defaultValue = "@null"
                }
            }
        }

    val nav_nested_start_destination_route_graph =
        createNavController().createGraph(route = "graph", startDestination = "nested") {
            navigation(route = "nested", startDestination = "nested_test") {
                test("nested_test")
                test("nested_second_test")
            }
            test("second_test")
        }

    val nav_deeplink_route_graph =
        createNavController().createGraph(route = "nav_root", startDestination = "first_test") {
            test("first_test") {
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test"
                    action = "test.action"
                    mimeType = "*/*"
                }
            }
            test("second_test") {
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test"
                    action = "test.action"
                    mimeType = "image/*"
                }
            }
            test("third_test") {
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test"
                    action = "test.action"
                    mimeType = "*/test"
                }
            }
            test("forth_test") {
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test"
                    action = "test.action"
                    mimeType = "type/test"
                }
            }
        }

    val nav_multiple_navigation_route_graph =
        createNavController().createGraph(
            route = "nav_multi_module_base",
            startDestination = "simple_child_start"
        ) {
            navigation(route = "simple_child_start", startDestination = "simple_child_start_test") {
                test("simple_child_start_test")
                test("simple_child_second_test")
            }
            navigation(
                route = "deep_link_child_start",
                startDestination = "deep_link_child_start_test"
            ) {
                test("deep_link_child_start_test")
                test("deep_link_child_second_test") {
                    deepLink { uriPattern = "android-app://androidx.navigation.test/test" }
                }
                navigation(
                    route = "deep_link_child_second",
                    startDestination = "deep_link_grandchild_start_test"
                ) {
                    test("deep_link_grandchild_start_test") {
                        deepLink {
                            uriPattern = "android-app://androidx.navigation.test/grand_child_test"
                        }
                    }
                    test("deep_link_child_second_test") {
                        deepLink { uriPattern = "android-app://androidx.navigation.test/test" }
                    }
                }
            }
        }

    val nav_singleArg_graph =
        createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
            test("start_test")
            test("second_test/{arg}") { argument("arg") { type = NavType.StringType } }
        }

    val nav_multiArg_graph =
        createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
            test("start_test")
            test("second_test/{arg}/{arg2}") {
                argument("arg") { type = NavType.StringType }
                argument("arg2") { type = NavType.StringType }
            }
        }

    @Serializable class TestClass

    @Serializable class TestClassPathArg(val arg: Int)

    @Serializable class TestGraph

    private enum class TestEnum {
        ONE,
        TWO
    }

    private enum class TestEnumWithArg(val number: Int) {
        ONE(1),
        TWO(2)
    }

    @Serializable
    private class EnumWrapper {
        enum class NestedEnum(val number: Int) {
            ONE(1),
            TWO(2)
        }
    }

    companion object {
        private const val UNKNOWN_DESTINATION_ID = -1
        private const val TEST_ARG = "test"
        private const val TEST_ARG_VALUE = "value"
        private const val TEST_OVERRIDDEN_VALUE_ARG = "test_overridden_value"
        private const val TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override"
        private const val TEST_CLASS_ROUTE = "androidx.navigation.NavControllerRouteTest.TestClass"
        private const val TEST_CLASS_PATH_ARG_ROUTE =
            "androidx.navigation." + "NavControllerRouteTest.TestClassPathArg/{arg}"
        private const val TEST_GRAPH_ROUTE =
            "androidx.navigation." + "NavControllerRouteTest.TestGraph"
    }

    @UiThreadTest
    @Test
    fun testGetCurrentBackStackEntry() {
        val navController = createNavController()
        navController.graph = nav_start_destination_route_graph
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("start_test")
    }

    @UiThreadTest
    @Test
    fun testGetPreviousBackStackEntry() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        navController.navigate("second_test/arg2")
        assertThat(navController.previousBackStackEntry?.destination?.route).isEqualTo("start_test")
    }

    @UiThreadTest
    @Test
    fun testStartDestination() {
        val navController = createNavController()
        navController.graph = nav_start_destination_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
    }

    @UiThreadTest
    @Test
    fun testStartDestinationKClass() {
        @Serializable @SerialName("test") class TestClass

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())
    }

    @UiThreadTest
    @Test
    fun testStartDestinationKClassWithArgs() {
        @Serializable @SerialName("test") class TestClass(val arg: Int)

        val navController = createNavController()
        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.graph =
                    navController.createGraph(startDestination = TestClass::class) {
                        test<TestClass>()
                    }
            }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot navigate to startDestination Destination(0x693c804) " +
                    "route=test/{arg}. Missing required arguments [[arg]]"
            )
    }

    @UiThreadTest
    @Test
    fun testNestedStartDestinationKClass() {
        @Serializable class NestedGraph

        @Serializable @SerialName("test") class TestClass

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = NestedGraph::class) {
                navigation<NestedGraph>(startDestination = TestClass::class) { test<TestClass>() }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())
    }

    @UiThreadTest
    @Test
    fun testStartDestinationObject() {
        @Serializable @SerialName("test") class TestClass

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass()) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())
    }

    @UiThreadTest
    @Test
    fun testStartDestinationObjectWithPathArg() {
        @Serializable @SerialName("test") class TestClass(val arg: Int)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(0)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())
        val arg = navController.currentBackStackEntry?.arguments?.getInt("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationObjectWithQueryArg() {
        @Serializable @SerialName("test") class TestClass(val arg: Boolean = false)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(false)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())
        val arg = navController.currentBackStackEntry?.arguments?.getBoolean("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).isEqualTo(false)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationObjectNoMatch() {
        @Serializable @SerialName("test") class TestClass(val arg: Boolean = false)

        val navController = createNavController()

        // Even though route string matches, startDestinations are matched based on id which
        // is based on serializer. So this won't match. StartDestination must be added via KClass
        assertFailsWith<IllegalStateException> {
            navController.graph =
                navController.createGraph(startDestination = TestClass(false)) {
                    test(route = "test?arg={arg}")
                }
        }
    }

    @UiThreadTest
    @Test
    fun testSetGraphTwice() {
        val navController = createNavController()
        navController.graph = nav_start_destination_route_graph
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        // Now set a new graph, overriding the first
        navController.graph = nav_nested_start_destination_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("nested_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithArgs() {
        val navController = createNavController()
        val args = Bundle().apply { putString(TEST_ARG, TEST_ARG_VALUE) }
        navController.setGraph(nav_simple_route_graph, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
        val foundArgs = navigator.current.arguments
        assertThat(foundArgs).isNotNull()
        assertThat(foundArgs?.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithPathArg() {
        val navController = createNavController()
        val graph =
            navController.createGraph(route = "graph", startDestination = "start/myArg") {
                test("start/{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "myArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationMissingRequiredArg() {
        val navController = createNavController()
        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.graph =
                    navController.createGraph(
                        route = "graph",
                        startDestination = "start_test/{arg}"
                    ) {
                        test("start_test/{arg}") {
                            // does not have default value to fallback to
                            argument("arg") { type = NavType.IntType }
                        }
                    }
            }
        assertThat(exception.message)
            .isEqualTo(
                "Cannot navigate to startDestination Destination(0x67775af) " +
                    "route=start_test/{arg}. Missing required arguments [[arg]]"
            )
    }

    @UiThreadTest
    @Test
    fun testNestedStartDestinationObjectWithPathArg() {
        @Serializable @SerialName("graph") class NestedGraph(val nestedArg: Int)

        @Serializable @SerialName("test") class TestClass(val arg: Int)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = NestedGraph(0)) {
                navigation<NestedGraph>(startDestination = TestClass(1)) { test<TestClass>() }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        assertThat(navController.currentDestination?.id)
            .isEqualTo(serializer<TestClass>().generateHashCode())

        val nestedArg = navController.currentBackStackEntry?.arguments?.getInt("nestedArg")
        assertThat(nestedArg).isNotNull()
        assertThat(nestedArg).isEqualTo(0)

        val arg = navController.currentBackStackEntry?.arguments?.getInt("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNestedStartDestinationWithPathArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "graph", startDestination = "start") {
                test("start")
                navigation(route = "nestedGraph", startDestination = "nestedStart/myArg") {
                    test("nestedStart/{arg}") {
                        argument("arg") {
                            type = NavType.StringType
                            nullable = false
                            defaultValue = "defaultArg"
                        }
                    }
                }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        navController.navigate("nestedGraph")
        assertThat(navController.currentDestination?.route).isEqualTo("nestedStart/{arg}")
        val entry = navController.currentBackStackEntry
        val actual = entry!!.arguments!!.getString("arg")
        val expected = "myArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithArgAndPathArgs() {
        val navController = createNavController()
        val args = Bundle().apply { putString("arg", "startArg") }
        val graph =
            navController.createGraph(route = "graph", startDestination = "start/myArg") {
                test("start/{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                    }
                }
            }
        navController.setGraph(graph, args)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}")
        val entry = navController.currentBackStackEntry
        val actual = entry!!.arguments!!.getString("arg")
        // explicitly passed in args should be prioritized
        val expected = "startArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithArgAndMultiplePathArgs() {
        val navController = createNavController()
        val args = Bundle().apply { putString("arg", "startArg") }
        val graph =
            navController.createGraph(route = "graph", startDestination = "start/myArg/myArg2") {
                test("start/{arg}/{arg2}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                    }
                    argument("arg2") {
                        type = NavType.StringType
                        nullable = false
                    }
                }
            }
        navController.setGraph(graph, args)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}/{arg2}")
        val entry = navController.currentBackStackEntry
        val actual1 = entry!!.arguments!!.getString("arg")
        // explicitly passed in arg should be prioritized
        val expected1 = "startArg"
        assertThat(actual1).isEqualTo(expected1)
        // no explicitly passed arg2, arg2 value should be the one from the route
        val actual2 = entry.arguments!!.getString("arg2")
        val expected2 = "myArg2"
        assertThat(actual2).isEqualTo(expected2)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithQueryArgs() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "graph", startDestination = "start?arg=myArg") {
                test("start?arg={arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                    }
                }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val entry = navController.currentBackStackEntry
        val actual = entry!!.arguments!!.getString("arg")
        val expected = "myArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithArgsProgrammatic() {
        val navController = createNavController()
        val args = Bundle().apply { putString(TEST_ARG, TEST_ARG_VALUE) }

        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
            }
        navController.setGraph(navGraph, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(1)
        val foundArgs = navigator.current.arguments
        assertThat(foundArgs).isNotNull()
        assertThat(foundArgs?.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationUseDefaultPathArg() {
        val navController = createNavController()
        val graph =
            navController.createGraph(route = "graph", startDestination = "start/{arg}") {
                test("start/{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "defaultArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationUseDefaultQueryArg() {
        val navController = createNavController()
        val graph =
            navController.createGraph(route = "graph", startDestination = "start?arg={arg}") {
                test("start?arg={arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "defaultArg"
        assertThat(actual).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationUseMultipleDefaultArgs() {
        val navController = createNavController()
        val graph =
            navController.createGraph(
                route = "graph",
                startDestination = "start/{arg}?arg2={arg2}"
            ) {
                test("start/{arg}?arg2={arg2}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                    argument("arg2") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg2"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}?arg2={arg2}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "defaultArg"
        assertThat(actual).isEqualTo(expected)
        val actual2 = entry.arguments!!.getString("arg2")
        val expected2 = "defaultArg2"
        assertThat(actual2).isEqualTo(expected2)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationQueryPlaceholderArg() {
        val navController = createNavController()
        val graph =
            navController.createGraph(
                route = "graph",
                startDestination = "start/myArg?arg2={arg2}"
            ) {
                test("start/{arg}?arg2={arg2}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                    argument("arg2") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg2"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}?arg2={arg2}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "myArg"
        assertThat(actual).isEqualTo(expected)
        val actual2 = entry.arguments!!.getString("arg2")
        val expected2 = "{arg2}"
        assertThat(actual2).isEqualTo(expected2)
    }

    @UiThreadTest
    @Test
    fun testStartDestinationPathPlaceholderArg() {
        val navController = createNavController()
        val graph =
            navController.createGraph(
                route = "graph",
                startDestination = "start/{arg}?arg2=myArg2"
            ) {
                test("start/{arg}?arg2={arg2}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg"
                    }
                    argument("arg2") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "defaultArg2"
                    }
                }
            }
        navController.setGraph(graph, null)
        assertThat(navController.currentDestination?.route).isEqualTo("start/{arg}?arg2={arg2}")
        val entry = navController.currentBackStackEntry!!
        val actual = entry.arguments!!.getString("arg")
        val expected = "{arg}"
        assertThat(actual).isEqualTo(expected)
        val actual2 = entry.arguments!!.getString("arg2")
        val expected2 = "myArg2"
        assertThat(actual2).isEqualTo(expected2)
    }

    @UiThreadTest
    @Test
    fun testNestedStartDestination() {
        val navController = createNavController()
        navController.graph = nav_nested_start_destination_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("nested_test")
    }

    @UiThreadTest
    @Test
    fun testSetGraph() {
        val navController = createNavController()

        navController.graph = nav_start_destination_route_graph
        assertThat(navController.graph).isNotNull()
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
    }

    @UiThreadTest
    @Test
    fun testSetViewModelStoreOwnerAfterGraphSet() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
            }
        navController.setGraph(navGraph, null)

        try {
            navController.setViewModelStore(ViewModelStore())
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat()
                .contains("ViewModelStore should be set before setGraph call")
        }
    }

    @UiThreadTest
    @Test
    fun testSetSameViewModelStoreOwnerAfterGraphSet() {
        val navController = createNavController()
        val viewModelStore = ViewModelStore()
        navController.setViewModelStore(viewModelStore)
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
            }
        navController.setGraph(navGraph, null)

        navController.setViewModelStore(viewModelStore)
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateSharedDestination() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "home_nav") {
                navigation(route = "home_nav", startDestination = "home") {
                    test("home")
                    test("shared")
                }
                navigation(route = "list_nav", startDestination = "list") {
                    test("list")
                    test("shared")
                }
            }

        assertThat(navController.currentDestination?.route).isEqualTo("home")

        navController.navigate("shared")
        val currentDest = navController.currentDestination!!
        assertThat(currentDest.route).isEqualTo("shared")
        assertThat(currentDest.parent!!.route).isEqualTo("home_nav")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "home_nav", "home", "shared")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateContainsIntent() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test("second")
            }

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second")
        assertThat(navController.currentDestination?.route).isEqualTo("second")
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent: Intent? =
            @Suppress("DEPRECATION")
            navController.currentBackStackEntry?.arguments?.getParcelable(KEY_DEEP_LINK_INTENT)
        assertThat(intent).isNotNull()
        assertThat(intent!!.data).isEqualTo(Uri.parse("android-app://androidx.navigation/second"))
    }

    @UiThreadTest
    @Test
    fun testNavigateNestedSharedDestination() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "home_nav") {
                navigation(route = "home_nav", startDestination = "home") {
                    test("home")
                    navigation(route = "home_nested_nav", startDestination = "home_nested") {
                        test("home_nested")
                        test("shared")
                    }
                }
                navigation(route = "list_nav", startDestination = "list") {
                    test("list")
                    test("shared")
                }
            }

        assertThat(navController.currentDestination?.route).isEqualTo("home")

        navController.navigate("shared")
        val currentDest = navController.currentDestination!!
        assertThat(currentDest.route).isEqualTo("shared")
        assertThat(currentDest.parent!!.route).isEqualTo("home_nested_nav")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "home_nav", "home", "home_nested_nav", "shared")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateParentSharedDestination() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "home_nav") {
                navigation(route = "home_nav", startDestination = "home") {
                    test("home")
                    test("shared")
                    navigation(route = "home_nested_nav", startDestination = "home_nested") {
                        test("home_nested")
                    }
                }
                navigation(route = "list_nav", startDestination = "list") {
                    test("list")
                    test("shared")
                }
            }

        assertThat(navController.currentDestination?.route).isEqualTo("home")

        // first navigate into nested graph
        navController.navigate("home_nested")
        assertThat(navController.currentDestination!!.route).isEqualTo("home_nested")

        // then navigate to the shared destination that is sibling of parent
        navController.navigate("shared")
        val currentDest = navController.currentDestination!!
        assertThat(currentDest.route).isEqualTo("shared")
        assertThat(currentDest.parent!!.route).isEqualTo("home_nav")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "home_nav", "home", "home_nested_nav", "home_nested", "shared")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateSingleTopSharedStartDestination() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "graph_one") {
                navigation(route = "graph_one", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
                navigation(route = "graph_two", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
            }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "graph_one", "shared_startDest")
            .inOrder()

        navController.navigate("graph_one") { launchSingleTop = true }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "graph_one", "shared_startDest")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateSingleTopSharedStartDestinationDifferentGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "graph_one") {
                navigation(route = "graph_one", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
                navigation(route = "graph_two", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
            }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "graph_one", "shared_startDest")
            .inOrder()

        navController.navigate("graph_two") { launchSingleTop = true }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(
                "root",
                "graph_one",
                "shared_startDest",
                "graph_two",
                "shared_startDest"
            )
            .inOrder()

        // should be single top
        navController.navigate("graph_two") { launchSingleTop = true }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(
                "root",
                "graph_one",
                "shared_startDest",
                "graph_two",
                "shared_startDest"
            )
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateSingleTopSharedStartDestinationAlternatingGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "root", startDestination = "graph_one") {
                navigation(route = "graph_one", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
                navigation(route = "graph_two", startDestination = "shared_startDest") {
                    test("shared_startDest")
                }
            }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly("root", "graph_one", "shared_startDest")
            .inOrder()

        // go to different graph
        navController.navigate("graph_two") { launchSingleTop = true }
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(
                "root",
                "graph_one",
                "shared_startDest",
                "graph_two",
                "shared_startDest"
            )
            .inOrder()

        // go back to original graph
        navController.navigate("graph_one") { launchSingleTop = true }
        // should not be single top
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(
                "root",
                "graph_one",
                "shared_startDest",
                "graph_two",
                "shared_startDest",
                "graph_one",
                "shared_startDest",
            )
            .inOrder()

        // single top to original graph again
        navController.navigate("graph_one") { launchSingleTop = true }
        // should be single top
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(
                "root",
                "graph_one",
                "shared_startDest",
                "graph_two",
                "shared_startDest",
                "graph_one",
                "shared_startDest",
            )
            .inOrder()
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLink() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent =
            navigator.current.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
        assertThat(intent?.data).isEqualTo(deepLink)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectPathArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        navController.navigate(TestClassPathArg(0))
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectQueryArg() {
        @Serializable class TestClass(val arg: IntArray)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        navController.navigate(TestClass(intArrayOf(0, 1, 2)))
        assertThat(navController.currentDestination?.route)
            .isEqualTo(
                "androidx.navigation.NavControllerRouteTest." +
                    "testNavigateWithObjectQueryArg.TestClass?arg={arg}"
            )
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.arguments?.getIntArray("arg"))
            .isEqualTo(intArrayOf(0, 1, 2))
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectPathQueryArg() {
        @Serializable class TestClass(val arg: IntArray, val arg2: Boolean)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        navController.navigate(TestClass(intArrayOf(0, 1, 2), true))
        assertThat(navController.currentDestination?.route)
            .isEqualTo(
                "androidx.navigation.NavControllerRouteTest." +
                    "testNavigateWithObjectPathQueryArg.TestClass/{arg2}?arg={arg}"
            )
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.arguments?.getIntArray("arg"))
            .isEqualTo(intArrayOf(0, 1, 2))
        assertThat(navController.currentBackStackEntry?.arguments?.getBoolean("arg2"))
            .isEqualTo(true)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectCollectionNavType() {
        val navController = createNavController()
        @Serializable data class CustomType(val id: Int)
        @Serializable class TestClass(val list: List<CustomType>)

        val collectionNavType =
            object : CollectionNavType<List<CustomType>>(false) {
                override fun put(bundle: Bundle, key: String, value: List<CustomType>) {
                    val array = value.map { it.id }.toIntArray()
                    bundle.putIntArray(key, array)
                }

                override fun serializeAsValues(value: List<CustomType>) =
                    value.map { it.id.toString() }

                override fun emptyCollection(): List<CustomType> = emptyList()

                override fun get(bundle: Bundle, key: String): List<CustomType> {
                    return bundle.getIntArray(key)!!.map { CustomType(it) }
                }

                override fun parseValue(value: String) = listOf(CustomType(value.toInt()))

                override fun parseValue(
                    value: String,
                    previousValue: List<CustomType>
                ): List<CustomType> {
                    val list = mutableListOf<CustomType>()
                    list.addAll(previousValue)
                    list.add(CustomType(value.toInt()))
                    return list
                }
            }

        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>(mapOf(typeOf<List<CustomType>>() to collectionNavType))
            }

        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        val list = listOf(CustomType(1), CustomType(3), CustomType(5))
        navController.navigate(TestClass(list))
        assertThat(navController.currentDestination?.route)
            .isEqualTo(
                "androidx.navigation.NavControllerRouteTest." +
                    "testNavigateWithObjectCollectionNavType.TestClass?list={list}"
            )
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry!!.toRoute<TestClass>().list)
            .containsExactlyElementsIn(list)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectInvalidObject() {
        @Serializable class WrongTestClass

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        assertFailsWith<IllegalArgumentException> { navController.navigate(WrongTestClass()) }
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectWithPopUpTo() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        navController.navigate(TestClass(), navOptions { popUpTo("start") { inclusive = true } })
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectArgsSavedAndRestored() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        // first nav
        val dest = TestClassPathArg(0)
        navController.navigate(dest)
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(0)

        // pop + save
        val popped = navController.popBackStack(dest, true, true)
        assertThat(popped).isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // second nav with restore
        val dest2 = TestClassPathArg(1)
        navController.navigate(dest2, navOptions { restoreState = true })
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectArgsSavedNotRestored() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)

        // first nav
        val dest = TestClassPathArg(0)
        navController.navigate(dest)
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(0)

        // pop + save
        val popped = navController.popBackStack(dest, true, true)
        assertThat(popped).isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // second nav without restore
        val dest2 = TestClassPathArg(1)
        navController.navigate(dest2, navOptions { restoreState = false })
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(1)

        // now we restore
        navController.navigate(dest2, navOptions { restoreState = true })
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectDouble() {
        @Serializable class TestClass(val arg: Double = 11.11)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(11E123))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(11E123)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(11.11)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableInt() {
        @Serializable class TestClass(val arg: Int? = 10)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(15))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(15)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(10)
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableIntNoDefault() {
        @Serializable class TestClass(val arg: Int?)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(15))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(15)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableBoolean() {
        @Serializable class TestClass(val arg: Boolean? = false)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(true))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isTrue()
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isFalse()
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableBooleanNoDefault() {
        @Serializable class TestClass(val arg: Boolean?)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(true))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isTrue()
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableDouble() {
        @Serializable class TestClass(val arg: Double? = 11.11)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(11E123))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(11E123)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(11.11)
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableDoubleNoDefault() {
        @Serializable class TestClass(val arg: Double?)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(11E123))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(11E123)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableFloat() {
        @Serializable class TestClass(val arg: Float? = 1.0F)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(2.0F))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(2.0F)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(1.0F)
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableFloatNoDefault() {
        @Serializable class TestClass(val arg: Float?)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(2.0F))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(2.0F)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableLong() {
        @Serializable class TestClass(val arg: Long? = 1L)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(2L))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(2L)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(1L)
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableLongNoDefault() {
        @Serializable class TestClass(val arg: Long?)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(2L))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isEqualTo(2L)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEnum() {
        @Serializable class TestClass(val arg: TestEnum = TestEnum.ONE)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(TestEnum.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.TWO)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.ONE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEnumTopLevel() {
        @Serializable class TestClass(val arg: TestTopLevelEnum = TestTopLevelEnum.ONE)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        // passed in arg
        navController.navigate(TestClass(TestTopLevelEnum.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestTopLevelEnum.TWO)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestTopLevelEnum.ONE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEnumNullable() {
        @Serializable class TestClass(val arg: TestEnum? = TestEnum.ONE)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(TestEnum.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.TWO)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in null
        navController.navigate(TestClass(null))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg).isNull()
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        // use default
        navController.navigate(TestClass())
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.ONE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(5)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEnumNoDefault() {
        @Serializable class TestClass(val arg: TestEnum)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        // passed in arg
        navController.navigate(TestClass(TestEnum.ONE))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.ONE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        // passed in arg
        navController.navigate(TestClass(TestEnum.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()
        assertThat(navController.currentBackStackEntry?.toRoute<TestClass>()?.arg)
            .isEqualTo(TestEnum.TWO)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEnumWithArg() {
        @Serializable class TestClass(val arg: TestEnumWithArg, val arg2: TestEnumWithArg)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        navController.navigate(TestClass(TestEnumWithArg.ONE, TestEnumWithArg.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()?.arg
        assertThat(arg).isEqualTo(TestEnumWithArg.ONE)
        assertThat(arg?.number).isEqualTo(1)

        val arg2 = navController.currentBackStackEntry?.toRoute<TestClass>()?.arg2
        assertThat(arg2).isEqualTo(TestEnumWithArg.TWO)
        assertThat(arg2?.number).isEqualTo(2)

        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNestedEnum() {
        @Serializable
        class TestClass(val arg: EnumWrapper.NestedEnum, val arg2: EnumWrapper.NestedEnum)
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start")

        navController.navigate(TestClass(EnumWrapper.NestedEnum.ONE, EnumWrapper.NestedEnum.TWO))
        assertThat(navController.currentDestination?.hasRoute(TestClass::class)).isTrue()

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()?.arg
        assertThat(arg).isEqualTo(EnumWrapper.NestedEnum.ONE)
        assertThat(arg?.number).isEqualTo(1)

        val arg2 = navController.currentBackStackEntry?.toRoute<TestClass>()?.arg2
        assertThat(arg2).isEqualTo(EnumWrapper.NestedEnum.TWO)
        assertThat(arg2?.number).isEqualTo(2)

        assertThat(navController.currentBackStack.value.size).isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpToFurthestRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // series of alternate navigation between two destinations
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg1")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate("start_test")
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg}")
        assertThat(navigator.backStack.size).isEqualTo(4)

        // now popUpTo the first time we navigated to second_test with args
        val navOptions = navOptions { popUpTo("second_test/arg1") { inclusive = true } }
        navController.navigate("start_test", navOptions)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navigator.backStack.map { it.destination.route })
            .containsExactly("start_test", "start_test")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpToClosestRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // series of alternate navigation between two destinations
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg1")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate("start_test")
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg}")
        assertThat(navigator.backStack.size).isEqualTo(4)

        // now popUpTo the second time we navigated to second_test with args
        val navOptions = navOptions { popUpTo("second_test/arg2") { inclusive = true } }
        navController.navigate("start_test", navOptions)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(4)
        assertThat(navigator.backStack.map { it.destination.route })
            .containsExactly("start_test", "second_test/{arg}", "start_test", "start_test")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithExactRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // first nav with arg filed in
        navController.navigate("second_test/13")

        // second nav with arg filled in
        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13", "second_test/18"]
        assertThat(navigator.backStack.size).isEqualTo(3)

        val entry1 = navController.getBackStackEntry("second_test/13")
        assertThat(entry1).isEqualTo(navigator.backStack[1])

        val entry2 = navController.getBackStackEntry("second_test/18")
        assertThat(entry2).isEqualTo(navigator.backStack[2])
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithExactRoute_onNavGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(route = "graph", startDestination = "start") {
                test("start")
                navigation(route = "graph2/{arg}", startDestination = "start2") {
                    argument("arg") { type = NavType.StringType }
                    test("start2")
                    navigation(route = "graph3/{arg}", startDestination = "start3") {
                        argument("arg") { type = NavType.StringType }
                        test("start3")
                    }
                }
            }

        // fist nested graph
        navController.navigate("graph2/13")

        // second nested graph
        navController.navigate("graph3/18")

        val navigator = navController.navigatorProvider.getNavigator(NavGraphNavigator::class.java)
        // ["graph", "graph2/13", "graph3/18"]
        assertThat(navigator.backStack.value.size).isEqualTo(3)

        val entry1 = navController.getBackStackEntry("graph2/13")
        assertThat(entry1).isEqualTo(navigator.backStack.value[1])

        val entry2 = navController.getBackStackEntry("graph3/18")
        assertThat(entry2).isEqualTo(navigator.backStack.value[2])
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithExactRoute_multiArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        // navigate with both args filed in
        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val entry1 = navController.getBackStackEntry("second_test/13/18")
        assertThat(entry1).isEqualTo(navigator.backStack[1])
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        // navigate with args partially filed in
        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/{arg2}"]
        assertThat(navigator.backStack.size).isEqualTo(2)
        // routes with partially filled in args will also match as long as the args
        // are filled in the exact same way
        val entry1 = navController.getBackStackEntry("second_test/13/{arg2}")
        assertThat(entry1).isEqualTo(navigator.backStack[1])
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_missingNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?{arg}"
            ) {
                test("start_test?{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // getBackStack with a route that has clipped all arg segments
        val entry1 = navController.getBackStackEntry("start_test")
        assertThat(entry1).isEqualTo(navigator.backStack.first())
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_ignoredNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?opt={arg}"
            ) {
                test("start_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // getBackStack with a route with null args
        val entry1 = navController.getBackStackEntry("start_test?opt={arg}")
        assertThat(entry1).isEqualTo(navigator.backStack.first())
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_nullNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?opt=null"
            ) {
                test("start_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // getBackStack with a route with null args
        val entry1 = navController.getBackStackEntry("start_test?opt=null")
        assertThat(entry1).isEqualTo(navigator.backStack.first())
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_incorrectNullQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate with query param
        navController.navigate("second_test?opt=13")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // fails because this is a StringType arg and `null` is considered a string
        // with the word "null" rather than a null value
        val route = "second_test?opt=null"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_multiQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?opt={arg}&opt2={arg2}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                    argument("arg2") { type = NavType.StringType }
                }
            }

        // navigate with query params
        navController.navigate("second_test?opt=null&opt2=13")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.getBackStackEntry("second_test?opt=null&opt2=13")
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_missingNonNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?myArg"
            ) {
                test("start_test?{arg}") { argument("arg") { type = NavType.StringType } }
            }

        // getBackStack with a route that has clipped all arg segments
        val route = "start_test"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithPartialExactRoute_nullNonNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?opt=myArg"
            ) {
                test("start_test?opt={arg}") { argument("arg") { type = NavType.StringType } }
            }

        // getBackStack with a route that has null arguments
        val route = "start_test?opt=null"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithIncorrectExactRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // navigate with arg filed in
        navController.navigate("second_test/13")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        // route "second_test/18" should not match with any entries in backstack since we never
        // navigated with args "18"
        val route = "second_test/18"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithIncorrectExactRoute_multiArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        // navigate with args partially filed in
        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        // route "second_test/13/13" should not match with any entries in backstack
        val route = "second_test/13/19"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithAdditionalPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        // navigate with args partially filed in
        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/{arg2}"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        // route with additional arg "14" should not match
        val route = "second_test/13/14"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithMissingPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        // route missing arg "18" should not match
        val route = "second_test/13/{arg2}"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithArrayArg() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start") {
                test("start")
                test("second?arg={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        navController.navigate("second?arg=15&arg=24")
        val currentEntry = navController.currentBackStackEntry
        assertThat(currentEntry?.destination?.route).isEqualTo("second?arg={arg}")
        val expected = navController.getBackStackEntry("second?arg=15&arg=24")
        assertThat(expected).isEqualTo(currentEntry)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithDifferingArrayArg() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start") {
                test("start")
                test("second?arg={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        navController.navigate("second?arg=15&arg=24")
        val currentEntry = navController.currentBackStackEntry
        assertThat(currentEntry?.destination?.route).isEqualTo("second?arg={arg}")
        val route = "second?arg=15"
        val exception =
            assertFailsWith<IllegalArgumentException> { navController.getBackStackEntry(route) }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $route is on the NavController's " +
                    "back stack. The current destination is ${currentEntry?.destination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithMissingArrayArg() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start") {
                test("start")
                test("second?arg={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }
        navController.navigate("second?arg=15&arg=24")
        val currentEntry = navController.currentBackStackEntry
        assertThat(currentEntry?.destination?.route).isEqualTo("second?arg={arg}")
        // would still match if missing some of the original args
        val expected = navController.getBackStackEntry("second?")
        assertThat(expected).isEqualTo(currentEntry)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithKClass() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val entry = navController.getBackStackEntry<TestClass>()
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithKClassArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val entry = navController.getBackStackEntry<TestClassPathArg>()
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithKClassNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                // a sibling graph with nested KClass destination
                navigation<TestGraph>(startDestination = TestClass::class) { test<TestClass>() }
                test("second")
            }

        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        val entry = navController.getBackStackEntry<TestClass>()
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithKClassNotInGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.getBackStackEntry<TestClassPathArg>()
            }
        assertThat(exception.message)
            .isEqualTo(
                "Destination with route TestClassPathArg cannot be found in " +
                    "navigation graph ${navController.graph}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithKClassNotInBackstack() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.getBackStackEntry<TestClass>()
            }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route TestClass is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val entry = navController.getBackStackEntry(TestClass())
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryGraphWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(
                route = TestGraph::class,
                startDestination = TestClass::class
            ) {
                test<TestClass>()
            }

        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate(TEST_GRAPH_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        val entry = navController.getBackStackEntry(TestGraph())
        assertThat(entry.destination.route).isEqualTo(TEST_GRAPH_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObjectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val entry = navController.getBackStackEntry(TestClassPathArg(0))
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObjectIncorrectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        assertFailsWith<IllegalArgumentException> {
            navController.getBackStackEntry(TestClassPathArg(1))
        }
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObjectNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                // a sibling graph with nested KClass destination
                navigation<TestGraph>(startDestination = TestClassPathArg::class) {
                    test<TestClassPathArg>()
                }
                test("second")
            }

        navController.navigate(TestClassPathArg(1))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        val entry = navController.getBackStackEntry(TestClassPathArg(1))
        assertThat(entry.destination.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObjectNotInGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.getBackStackEntry(TestClassPathArg(1))
            }
        assertThat(exception.message)
            .isEqualTo(
                "Destination with route TestClassPathArg cannot be found in " +
                    "navigation graph ${navController.graph}"
            )
    }

    @UiThreadTest
    @Test
    fun testGetBackStackEntryWithObjectNotInBackstack() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.getBackStackEntry(TestClass())
            }
        assertThat(exception.message)
            .isEqualTo(
                "No destination with route $TEST_CLASS_ROUTE is on the NavController's " +
                    "back stack. The current destination is ${navController.currentDestination}"
            )
    }

    @UiThreadTest
    @Test
    fun testPopBackStack() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // first nav with arg filed in
        navController.navigate("second_test/13")

        // second nav with arg filled in
        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13", "second_test/18"]
        assertThat(navigator.backStack.size).isEqualTo(3)

        val popped = navController.popBackStack("second_test/{arg}", true)
        assertThat(popped).isTrue()
        // only last entry with "second_test/{arg}" has been popped
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithExactRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        // first nav with arg filed in
        navController.navigate("second_test/13")

        // second nav with arg filled in
        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13", "second_test/18"]
        assertThat(navigator.backStack.size).isEqualTo(3)

        val popped = navController.popBackStack("second_test/13", true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithExactRoute_multiArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/13/18", true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithPartialExactRoute() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/{arg2}"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/13/{arg2}", true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithPartialExactRoute_missingNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without filling query param
        navController.navigate("second_test?{arg}")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // popBackStack with a route that has clipped all arg segments
        val popped = navController.popBackStack("second_test", true)
        assertThat(popped).isTrue()
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithPartialExactRoute_ignoredNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(
                route = "nav_root",
                startDestination = "start_test?opt={arg}"
            ) {
                test("start_test")
                test("second_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without query param
        navController.navigate("second_test?opt={arg}")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // popBackStack with a null query param
        val popped = navController.popBackStack("second_test?opt={arg}", true)
        assertThat(popped).isTrue()
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithPartialExactRoute_nullNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without query param
        navController.navigate("second_test?opt=null")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // popBackStack with a null query param
        val popped = navController.popBackStack("second_test?opt=null", true)
        assertThat(popped).isTrue()
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithIncorrectExactRoute() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/13/19", true)
        assertThat(popped).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithAdditionalPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/{arg2}"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/13/18", true)
        assertThat(popped).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithMissingPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/13/{arg2}", true)
        assertThat(popped).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithWrongArgOrder() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        // ["start_test", "second_test/13/18"]
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack("second_test/18/13", true)
        assertThat(popped).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithKClass() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack<TestClass>(true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackGraphWithKClass() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(
                route = TestGraph::class,
                startDestination = TestClass::class
            ) {
                test<TestClass>()
            }

        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate(TEST_GRAPH_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        val popped = navController.popBackStack<TestGraph>(true)
        assertThat(popped).isTrue()
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithKClassNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                // a sibling graph with nested KClass destination
                navigation<TestGraph>(startDestination = TestClass::class) { test<TestClass>() }
                test("second")
            }

        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        val popped = navController.popBackStack<TestClass>(true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithKClassArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        // first nav
        navController.navigate("start")

        // second nav
        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        val popped = navController.popBackStack<TestClassPathArg>(true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithKClassNotInGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.popBackStack<TestClassPathArg>(true)
            }
        assertThat(exception.message)
            .isEqualTo(
                "Destination with route TestClassPathArg cannot be found in " +
                    "navigation graph ${navController.graph}"
            )
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithKClassNotInBackStack() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val popped = navController.popBackStack<TestClass>(true)
        assertThat(popped).isFalse()
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        // first nav
        navController.navigate("start")

        // second nav
        navController.navigate(TEST_CLASS_ROUTE)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        val popped = navController.popBackStack(TestClass(), true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackGraphWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(
                route = TestGraph::class,
                startDestination = TestClass::class
            ) {
                test<TestClass>()
            }

        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate(TEST_GRAPH_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)

        val popped = navController.popBackStack(TestGraph(), true)
        assertThat(popped).isTrue()
        assertThat(navController.currentBackStack.value.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObjectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        // first nav
        navController.navigate("start")

        // second nav
        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        val popped = navController.popBackStack(TestClassPathArg(0), true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObjectIncorrectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        // first nav
        navController.navigate("start")

        // second nav
        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)
        // pop with a route that has a different arg value
        val popped = navController.popBackStack(TestClassPathArg(1), true)
        assertThat(popped).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObjectNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                // a sibling graph with nested KClass destination
                navigation<TestGraph>(startDestination = TestClass::class) { test<TestClass>() }
                test("second")
            }

        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        val popped = navController.popBackStack(TestClass(), true)
        assertThat(popped).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObjectNotInGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }
        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.popBackStack(TestClassPathArg(1), true)
            }
        assertThat(exception.message)
            .isEqualTo(
                "Destination with route TestClassPathArg cannot be found in " +
                    "navigation graph ${navController.graph}"
            )
    }

    @UiThreadTest
    @Test
    fun testPopBackStackWithObjectNotInBackStack() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val popped = navController.popBackStack(TestClass(), true)
        assertThat(popped).isFalse()
    }

    @UiThreadTest
    @Test
    fun testFindDestinationWithRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        // find with general route pattern
        val foundDest = navController.findDestination("second_test/{arg}")
        assertThat(foundDest).isNotNull()
        // since we matching based on general route, should match both destinations
        assertThat(foundDest).isEqualTo(navigator.backStack[1].destination)
        assertThat(foundDest).isEqualTo(navigator.backStack[2].destination)
    }

    @UiThreadTest
    @Test
    fun testFindDestinationWithExactRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        // find with route args filled in
        val foundDest = navController.findDestination("second_test/13")
        assertThat(foundDest).isNotNull()
        // Even though NavDestinations doesn't store filled in args, an exact route should
        // still be matched with the correct NavDestination with the same general pattern
        assertThat(foundDest).isEqualTo(navigator.backStack[1].destination)
        assertThat(foundDest).isEqualTo(navigator.backStack[2].destination)
    }

    @UiThreadTest
    @Test
    fun testClearBackStack() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.popBackStack("second_test/{arg}", true, true)

        val cleared = navController.clearBackStack("second_test/{arg}")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                navigation(route = "nested", startDestination = "nestedStart") {
                    test("nestedStart")
                }
                test("second")
            }

        navController.navigate("nestedStart")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("nestedStart")

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        // make sure we are popping and clearing the nested destination directly, instead of
        // popping/clearing the nested graph itself
        val popped = navController.popBackStack("nestedStart", true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack("nestedStart")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStack_multiArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/14")

        navController.navigate("second_test/18/19")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.popBackStack("second_test/{arg}/{arg2}", true, true)
        // multiple args
        val cleared = navController.clearBackStack("second_test/{arg}/{arg2}")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithExactRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.popBackStack("second_test/13", true, true)

        val cleared = navController.clearBackStack("second_test/13")
        assertThat(cleared).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithExactRoute_multiArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/14")

        navController.navigate("second_test/18/19")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.popBackStack("second_test/13/14", true, true)
        // multiple filled-in args
        val cleared = navController.clearBackStack("second_test/13/14")
        assertThat(cleared).isTrue()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithDifferentRouteForPopping() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        navController.navigate("second_test/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(3)

        navController.popBackStack("second_test/13", true, true)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // "second_test/18" was popped but it wasn't the route passed in for popping
        val cleared = navController.clearBackStack("second_test/18")
        assertThat(cleared).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithUnpoppedRoute() {
        val navController = createNavController()
        navController.graph = nav_singleArg_graph

        navController.navigate("second_test/13")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test/13", true, true)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // start_test was never popped
        val cleared = navController.clearBackStack("start_test")
        assertThat(cleared).isFalse()
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithPartialExactRoute() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        // navigate with partial args filled in
        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test/13/{arg2}", true, true)
        val cleared = navController.clearBackStack("second_test/13/{arg2}")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithPartialExactRoute_missingNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?{arg}") {
                    argument("arg") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without filling query param
        navController.navigate("second_test?{arg}")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test?{arg}", true, true)

        // clearBackStack with a route that has clipped all arg segments
        val cleared = navController.clearBackStack("second_test")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithPartialExactRoute_ignoredNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without filling query param
        navController.navigate("second_test?opt={arg}")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test?opt={arg}", true, true)

        // clearBackStack with a route that has clipped all arg segments
        val cleared = navController.clearBackStack("second_test?opt={arg}")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithPartialExactRoute_nullNullableQueryParams() {
        val navController = createNavController()
        navController.graph =
            createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
                test("start_test")
                test("second_test?opt={arg}") {
                    argument("arg") {
                        type = NavType.IntArrayType
                        nullable = true
                        defaultValue = null
                    }
                }
            }

        // navigate without filling query param
        navController.navigate("second_test?opt=null")
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test?opt=null", true, true)

        // clearBackStack with a route that has clipped all arg segments
        val cleared = navController.clearBackStack("second_test?opt=null")
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithAdditionalPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/{arg2}")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test/13/{arg2}", true, true)
        // additional arg2 = 18
        val cleared = navController.clearBackStack("second_test/13/18")
        assertThat(cleared).isFalse()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithMissingPartialArgs() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test/13/18", true, true)
        // missing arg2 = 18
        val cleared = navController.clearBackStack("second_test/13/{arg2}")
        assertThat(cleared).isFalse()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithWrongArgOrder() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack("second_test/13/18", true, true)
        // args in wrong order
        val cleared = navController.clearBackStack("second_test/18/13")
        assertThat(cleared).isFalse()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithNoSavedState() {
        val navController = createNavController()
        navController.graph = nav_multiArg_graph

        navController.navigate("second_test/13/18")

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        // popped without saving state
        navController.popBackStack("second_test/13/18", true, false)
        val cleared = navController.clearBackStack("second_test/13/18")
        assertThat(cleared).isFalse()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithKClass() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack<TestClass>(true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack<TestClass>()
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithKClassPoppedWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack(TestClass(), true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack<TestClass>()
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithKClassArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack<TestClassPathArg>(true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack<TestClassPathArg>()
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithKClassNested() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                // a sibling graph with nested KClass destination
                navigation<TestGraph>(startDestination = TestClass::class) { test<TestClass>() }
                test("second")
            }

        navController.navigate(TestClass())

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry?.destination?.route)
            .isEqualTo(TEST_CLASS_ROUTE)

        navController.navigate("second")

        assertThat(navigator.backStack.size).isEqualTo(3)
        assertThat(navController.currentBackStackEntry?.destination?.route).isEqualTo("second")

        val popped = navController.popBackStack<TestClass>(true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack<TestClass>()
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithObject() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack(TestClass(), true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack(TestClass())
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithObjectPoppedWithKClass() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass>()
            }

        navController.navigate(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack<TestClass>(true, true)
        assertThat(popped).isTrue()

        val cleared = navController.clearBackStack(TestClass())
        assertThat(cleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testClearBackStackWithObjectIncorrectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClassPathArg>()
            }

        // second nav
        navController.navigate(TEST_CLASS_PATH_ARG_ROUTE.replace("{arg}", "0"))
        assertThat(navController.currentBackStack.value.size).isEqualTo(3)

        val popped = navController.popBackStack(TestClassPathArg(0), true, true)
        assertThat(popped).isTrue()

        // pass in different arg value
        val cleared = navController.clearBackStack(TestClassPathArg(1))
        assertThat(cleared).isFalse()
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkKClass() {
        val baseUri = "www.example.com"
        @Serializable class TestClass(val arg: Int)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass> { deepLink(navDeepLink<TestClass>(baseUri)) }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("http://$baseUri/1")

        navController.navigate(deepLink)

        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navController.currentBackStackEntry!!.arguments!!.getInt("arg")).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkKClassDefaultArg() {
        val baseUri = "www.example.com"
        @Serializable class TestClass(val arg: Int = 1)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass> { deepLink(navDeepLink<TestClass>(baseUri)) }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("http://$baseUri")

        navController.navigate(deepLink)

        assertThat(navigator.backStack.size).isEqualTo(2)
        val dest = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(dest?.arg).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkKClassDefaultArgOverride() {
        val baseUri = "www.example.com"
        @Serializable class TestClass(val arg: Int = 1)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass> { deepLink(navDeepLink<TestClass>(baseUri)) }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("http://$baseUri?arg=2")

        navController.navigate(deepLink)

        assertThat(navigator.backStack.size).isEqualTo(2)
        val dest = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(dest?.arg).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkKClassPopUpTo() {
        val baseUri = "www.example.com"
        @Serializable class TestClass(val arg: Int)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start")
                test<TestClass> { deepLink(navDeepLink<TestClass>(baseUri)) }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("http://$baseUri/1")

        navController.navigate(deepLink, navOptions { popUpTo("start") { inclusive = true } })

        assertThat(navigator.backStack.size).isEqualTo(1)
        assertThat(navController.currentBackStackEntry!!.arguments!!.getInt("arg")).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkDefaultArg() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(deepLink)

        val destination = navController.currentDestination
        assertThat(destination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkActionDifferentURI() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), "test.action", null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkActionDifferentURI_nonNullableArg() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), "test.action2", null)

        val expected =
            assertFailsWith<IllegalArgumentException> { navController.navigate(deepLink) }
        assertThat(expected.message)
            .isEqualTo(
                "Navigation destination that matches request " +
                    "NavDeepLinkRequest{ uri=invalidDeepLink.com action=test.action2 } cannot be " +
                    "found in the navigation graph NavGraph(0xc017d10b) route=nav_root " +
                    "startDestination={Destination(0x4a13399c) route=start_test}"
            )
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeDifferentUri() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), null, "type/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeDifferentUri_nonNullableArg() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), null, "type/test2")

        val expected =
            assertFailsWith<IllegalArgumentException> { navController.navigate(deepLink) }
        assertThat(expected.message)
            .isEqualTo(
                "Navigation destination that matches request " +
                    "NavDeepLinkRequest{ uri=invalidDeepLink.com mimetype=type/test2 } cannot be " +
                    "found in the navigation graph NavGraph(0xc017d10b) route=nav_root " +
                    "startDestination={Destination(0x4a13399c) route=start_test}"
            )
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLinkMimeType() {
        val navController = createNavController()
        navController.graph = nav_deeplink_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val mimeType = "type/test"
        val deepLink = NavDeepLinkRequest(null, null, mimeType)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("forth_test")
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent =
            navigator.current.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
        assertThat(intent?.type).isEqualTo(mimeType)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCard() {
        val navController = createNavController()
        navController.graph = nav_deeplink_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "any/thing")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("first_test")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardSubtype() {
        val navController = createNavController()
        navController.graph = nav_deeplink_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "image/jpg")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("second_test")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardType() {
        val navController = createNavController()
        navController.graph = nav_deeplink_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "doesNotEvenMatter/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("third_test")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigationViaDeepLinkPopUpTo() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(deepLink, navOptions { popUpTo("nav_root") { inclusive = true } })
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLink() {
        val navController = createNavController()
        navController.graph = nav_multiple_navigation_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("deep_link_child_second_test")
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLink3x() {
        val navController = createNavController()
        navController.graph = nav_multiple_navigation_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("deep_link_child_second_test")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLinkToGrandchild3x() {
        val navController = createNavController()
        navController.graph = nav_multiple_navigation_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/grand_child_test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route)
            .isEqualTo("deep_link_grandchild_start_test")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaUriOnlyIfDeepLinkExplicitlyAdded() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start") { deepLink { uriPattern = createRoute("explicit_start_deeplink") } }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse(createRoute("explicit_start_deeplink"))

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(2)

        // ensure can't deep link with destination's public route
        val deepLink2 = Uri.parse(createRoute("start"))

        val exception =
            assertFailsWith<IllegalArgumentException> { navController.navigate(deepLink2) }
        assertThat(exception.message)
            .isEqualTo(
                "Navigation destination that matches request " +
                    "NavDeepLinkRequest{ uri=android-app://androidx.navigation/start } " +
                    "cannot be found in the navigation graph NavGraph(0x0) " +
                    "startDestination={Destination(0xa2cd94f5) route=start}"
            )
    }

    @UiThreadTest
    @Test
    fun testNavigateViaRequestOnlyIfDeepLinkExplicitlyAdded() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start") { deepLink { uriPattern = createRoute("explicit_start_deeplink") } }
            }
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val request =
            NavDeepLinkRequest(Uri.parse(createRoute("explicit_start_deeplink")), null, null)

        navController.navigate(request)
        assertThat(navController.currentDestination?.route).isEqualTo("start")
        assertThat(navigator.backStack.size).isEqualTo(2)

        // ensure can't deep link with destination's public route
        val request2 = NavDeepLinkRequest(Uri.parse(createRoute("start")), null, null)

        val exception =
            assertFailsWith<IllegalArgumentException> { navController.navigate(request2) }
        assertThat(exception.message)
            .isEqualTo(
                "Navigation destination that matches request " +
                    "NavDeepLinkRequest{ uri=android-app://androidx.navigation/start } " +
                    "cannot be found in the navigation graph NavGraph(0x0) " +
                    "startDestination={Destination(0xa2cd94f5) route=start}"
            )
    }

    @LargeTest
    @Test
    fun testNavigateViaImplicitDeepLink() {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("android-app://androidx.navigation.test/test/argument1/argument2"),
                ApplicationProvider.getApplicationContext() as Context,
                TestActivity::class.java
            )

        Intents.init()

        val destroyActivityLatch = CountDownLatch(1)

        with(ActivityScenario.launchActivityForResult<TestActivity>(intent)) {
            moveToState(Lifecycle.State.CREATED)
            onActivity { activity ->
                run {
                    val navController = activity.navController
                    navController.graph = nav_simple_route_graph

                    val navigator =
                        navController.navigatorProvider.getNavigator(TestNavigator::class.java)

                    assertThat(navController.currentDestination?.route)
                        .isEqualTo("second_test/{arg2}")

                    // Only the leaf destination should be on the stack.
                    assertThat(navigator.backStack.size).isEqualTo(1)
                    // The parent will be constructed in a new Activity after navigateUp()
                    navController.navigateUp()
                }

                activity.lifecycle.addObserver(
                    object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event.targetState == Lifecycle.State.DESTROYED) {
                                destroyActivityLatch.countDown()
                            }
                        }
                    }
                )
            }

            assertThat(destroyActivityLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(this.state).isEqualTo(Lifecycle.State.DESTROYED)
        }

        // this relies on MonitoringInstrumentation.execStartActivity() which was added in API 17
        intended(
            allOf(
                toPackage((ApplicationProvider.getApplicationContext() as Context).packageName),
                not(hasData(anyString())), // The rethrow should not use the URI as primary target.
                hasExtra(
                    NavController.KEY_DEEP_LINK_IDS,
                    intArrayOf(createRoute("nav_root").hashCode())
                ),
                hasExtra(
                    Matchers.`is`(NavController.KEY_DEEP_LINK_EXTRAS),
                    allOf(
                        BundleMatchers.hasEntry("arg1", "argument1"),
                        BundleMatchers.hasEntry("arg2", "argument2"),
                        BundleMatchers.hasEntry(
                            NavController.KEY_DEEP_LINK_INTENT,
                            allOf(
                                hasAction(intent.action),
                                hasData(intent.data),
                                hasComponent(intent.component)
                            )
                        )
                    )
                )
            )
        )

        Intents.release()
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateXml() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.graph = nav_simple_route_graph
        navController.navigate("second_test/arg2")

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.graph = nav_simple_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateDestinationChanged() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.graph = nav_simple_route_graph

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        var destinationChangedCount = 0

        navController.addOnDestinationChangedListener { _, _, _ -> destinationChangedCount++ }

        // Explicitly setting a graph then restores the state
        navController.graph = nav_simple_route_graph
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
        // listener should have been fired again when state restored
        assertThat(destinationChangedCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateProgrammatic() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.graph = nav_simple_route_graph
        navController.navigate("second_test/arg2")

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.graph = nav_simple_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.graph = nav_simple_route_graph

        navigator.customParcel = CustomTestParcelable(TEST_ARG_VALUE)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.graph = nav_simple_route_graph

        // Ensure custom parcelable is present and can be read
        assertThat(navigator.customParcel?.name).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreAfterNavigateToDifferentNavGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.graph = nav_multiple_navigation_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.route).isEqualTo("deep_link_child_second_test")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate("simple_child_start")
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(3)

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.graph = nav_multiple_navigation_route_graph
        assertThat(navController.currentDestination?.route).isEqualTo("simple_child_start_test")
        assertThat(navigator.backStack.size).isEqualTo(3)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreFromSiblingGraph() {
        val navController = createNavController()
        val graph =
            createNavController().createGraph(route = "root", startDestination = "graphA") {
                navigation(route = "graphA", startDestination = "A1") {
                    test("A1")
                    test("A2")
                    test("A3")
                }
                navigation(route = "graphB", startDestination = "B1") { test("B1") }
            }
        navController.graph = graph
        navController.navigate("A2")
        navController.navigate("A3")

        assertThat(navController.currentDestination?.route).isEqualTo("A3")

        navController.navigate("graphB") {
            popUpTo("A2") {
                inclusive = true
                saveState = true
            }
        }

        navController.navigate("A2") { restoreState = true }
        // the popped stack [A2, A3] should be restored
        assertThat(navController.currentDestination?.route).isEqualTo("A3")
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testBackstackArgsBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        val backStackArg1 = Bundle()
        backStackArg1.putParcelable(TEST_ARG, CustomTestParcelable(TEST_ARG_VALUE))
        navController.setGraph(R.navigation.nav_arguments)
        navController.navigate(R.id.second_test, backStackArg1)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.setGraph(R.navigation.nav_arguments)

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments?.getParcelable<CustomTestParcelable>(TEST_ARG)?.name)
                .isEqualTo(TEST_ARG_VALUE)
        }
    }

    @UiThreadTest
    @Suppress("DEPRECATION")
    @Test
    fun testNavigateArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs).isNotNull()
        assertThat(returnedArgs!!["test_start_default"]).isEqualTo("default")

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments).isNotNull()
            assertThat(arguments!!["test_start_default"]).isEqualTo("default")
        }
    }

    @UiThreadTest
    @Test
    fun testNavigateWithNoDefaultValue() {
        val returnedArgs = navigateWithArgs(null)

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs.containsKey("test_no_default_value")).isFalse()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithDefaultArgs() {
        val returnedArgs = navigateWithArgs(null)

        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value")).isEqualTo("default")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithArgs() {
        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithOverriddenDefaultArgs() {
        val args = Bundle()
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
            .isEqualTo(TEST_OVERRIDDEN_VALUE_ARG_VALUE)
    }

    private fun navigateWithArgs(args: Bundle?): Bundle {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        navController.navigate(R.id.second_test, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs).isNotNull()

        return returnedArgs!!
    }

    @UiThreadTest
    @Test
    fun testPopRoot() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
            .that(success)
            .isFalse()
        assertThat(navController.currentDestination).isNull()
        assertThat(navigator.backStack.size).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testPopOnEmptyStack() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
            .that(success)
            .isFalse()
        assertThat(navController.currentDestination).isNull()
        assertThat(navigator.backStack.size).isEqualTo(0)

        val popped = navController.popBackStack()
        assertWithMessage(
                "popBackStack should return false when there's nothing on the " + "back stack"
            )
            .that(popped)
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPop() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPopToUnknownDestination() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack(UNKNOWN_DESTINATION_ID, false)
        assertWithMessage("Popping to an invalid destination should return false")
            .that(popped)
            .isFalse()
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateWithPop() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2") { popUpTo("start_test") { inclusive = true } }
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateWithPopRoot() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2") { popUpTo("nav_root") { inclusive = true } }
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateUp() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        // This should function identically to popBackStack()
        val success = navController.navigateUp()
        assertThat(success).isTrue()
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateUpWithDefaultArgs() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.route).isEqualTo("start_test")
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate("second_test/arg2")
        assertThat(navController.currentDestination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate("start_test_with_default_arg")
        assertThat(navController.currentDestination?.route).isEqualTo("start_test_with_default_arg")
        assertThat(navigator.backStack.size).isEqualTo(3)

        // This should function identically to popBackStack()
        val success = navController.navigateUp()
        assertThat(success).isTrue()
        val destination = navController.currentDestination
        assertThat(destination?.route).isEqualTo("second_test/{arg2}")
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopKClassInclusive() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) {
                test<TestClass>()
                test("second")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE)
            .inOrder()

        navController.navigate("second") { popUpTo<TestClass> { inclusive = true } }

        assertThat(navController.currentDestination?.route).isEqualTo("second")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, "second")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopKClassNotInclusive() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) {
                test<TestClass>()
                test("second")
                test("third")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE)
            .inOrder()

        navController.navigate("second")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE, "second")
            .inOrder()

        navController.navigate("third") { popUpTo<TestClass> { inclusive = false } }

        assertThat(navController.currentDestination?.route).isEqualTo("third")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE, "third")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopObjectInclusive() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) {
                test<TestClass>()
                test("second")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE)
            .inOrder()

        navController.navigate("second") { popUpTo(TestClass()) { inclusive = true } }

        assertThat(navController.currentDestination?.route).isEqualTo("second")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, "second")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopObjectNotInclusive() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) {
                test<TestClass>()
                test("second")
                test("third")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE)
            .inOrder()

        navController.navigate("second")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE, "second")
            .inOrder()

        navController.navigate("third") { popUpTo(TestClass()) { inclusive = false } }

        assertThat(navController.currentDestination?.route).isEqualTo("third")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_ROUTE, "third")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopObjectArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClassPathArg(0)) {
                test<TestClassPathArg>()
                test("second")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_PATH_ARG_ROUTE)
            .inOrder()

        navController.navigate("second") { popUpTo(TestClassPathArg(0)) { inclusive = true } }

        assertThat(navController.currentDestination?.route).isEqualTo("second")
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, "second")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopObjectWrongArg() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClassPathArg(0)) {
                test<TestClassPathArg>()
                test("second")
            }

        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_PATH_ARG_ROUTE)
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_PATH_ARG_ROUTE)
            .inOrder()

        navController.navigate("second") { popUpTo(TestClassPathArg(1)) { inclusive = true } }
        assertThat(navController.currentDestination?.route).isEqualTo("second")
        // should not be popped due to wrong arg
        assertThat(navController.currentBackStack.value.map { it.destination.route })
            .containsExactly(null, TEST_CLASS_PATH_ARG_ROUTE, "second")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithEmptyStringList() {

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test(route = "start?arg={arg}") {
                    argument("arg") { type = NavType.StringListType }
                }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val arg = navController.currentBackStackEntry?.arguments?.getStringArray("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).asList().isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithEmptyStringListUseDefault() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test(route = "start?arg={arg}") {
                    argument("arg") {
                        type = NavType.StringListType
                        defaultValue = listOf("one", "two")
                    }
                }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val arg = navController.currentBackStackEntry?.arguments?.getStringArray("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).asList().containsExactly("one", "two")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithEmptyIntList() {

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start?arg=") {
                test(route = "start?arg={arg}") { argument("arg") { type = NavType.IntListType } }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val arg = navController.currentBackStackEntry?.arguments?.getIntArray("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).asList().isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithEmptyIntListUseDefault() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start?arg=") {
                test(route = "start?arg={arg}") {
                    argument("arg") {
                        type = NavType.IntListType
                        defaultValue = listOf(1, 2)
                    }
                }
            }
        assertThat(navController.currentDestination?.route).isEqualTo("start?arg={arg}")
        val arg = navController.currentBackStackEntry?.arguments?.getIntArray("arg")
        assertThat(arg).isNotNull()
        assertThat(arg).asList().containsExactly(1, 2)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectListArg() {
        @Serializable class TestClass(val arg: MutableList<Boolean>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(
                startDestination = TestClass(mutableListOf(true, false, true))
            ) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route)
            .isEqualTo(
                "androidx.navigation.NavControllerRouteTest.testNavigateWithObjectListArg" +
                    ".TestClass?arg={arg}"
            )
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route?.arg is MutableList).isTrue()
        assertThat(route?.arg).containsExactly(true, false, true).inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNotInGraph() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass::class) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        val exception =
            assertFailsWith<IllegalArgumentException> {
                navController.navigate(TestClassPathArg(1))
            }
        assertThat(exception.message)
            .isEqualTo(
                "Destination with route TestClassPathArg cannot be found in navigation " +
                    "graph ${navController.graph}"
            )
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectPathNullString() {
        @Serializable @SerialName("test") class TestClass(val arg: String?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNonNullableString() {
        @Serializable @SerialName("test") class TestClass(val arg: String)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass("null")) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).isEqualTo("null")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullableString() {
        @Serializable @SerialName("test") class TestClass(val arg: String?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass("null")) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectQueryNullString() {
        @Serializable @SerialName("test") class TestClass(val arg: String? = null)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass()) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringArrayNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: Array<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(arrayOf())) { test<TestClass>() }

        navController.navigate(TestClass(arrayOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg.first()).isEqualTo("one")
        assertThat(arg.last()).isEqualTo("null")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringArrayNullableNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: Array<String>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }

        navController.navigate(TestClass(arrayOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg!!.first()).isEqualTo("one")
        assertThat(arg.last()).isEqualTo("null")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringNullableArrayNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: Array<String?>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(arrayOf())) { test<TestClass>() }

        navController.navigate(TestClass(arrayOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg.first()).isEqualTo("one")
        // should match behavior of String? type where "null" -> null
        assertThat(arg.last()).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringNullableArrayNullString() {
        @Serializable @SerialName("test") class TestClass(val arg: Array<String?>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(arrayOf())) { test<TestClass>() }

        navController.navigate(TestClass(arrayOf("one", null)))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg.first()).isEqualTo("one")
        // should match behavior of String? type where null -> null
        assertThat(arg.last()).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(listOf())) { test<TestClass>() }

        navController.navigate(TestClass(listOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg.first()).isEqualTo("one")
        assertThat(arg.last()).isEqualTo("null")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListNullableNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }

        navController.navigate(TestClass(listOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg!!.first()).isEqualTo("one")
        assertThat(arg.last()).isEqualTo("null")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringNullableListNullLiteral() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String?>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }

        navController.navigate(TestClass(listOf("one", "null")))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg!!.first()).isEqualTo("one")
        assertThat(arg.last()).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringNullableListNullString() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String?>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }

        navController.navigate(TestClass(listOf("one", null)))
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")

        val arg = navController.currentBackStackEntry?.toRoute<TestClass>()!!.arg
        assertThat(arg!!.first()).isEqualTo("one")
        assertThat(arg.last()).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullStringList() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullStringListUseDefaultNull() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>? = null)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullStringListUseDefaultList() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>? = listOf("one"))

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).containsExactly("one")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullIntList() {
        @Serializable @SerialName("test") class TestClass(val arg: List<Int>?)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectNullIntListUseDefault() {
        @Serializable @SerialName("test") class TestClass(val arg: List<Int>? = null)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(null)) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNull()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEmptyString() {
        @Serializable @SerialName("test") class TestClass(val arg: String)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass("")) { test<TestClass>() }
        assertThat(navController.currentDestination?.route).isEqualTo("test/{arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).isEqualTo("")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEmptyStringList() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(emptyList())) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEmptyStringListUseDefault() {
        @Serializable
        @SerialName("test")
        class TestClass(val arg: List<String> = listOf("one", "two"))

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(emptyList())) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly("one", "two").inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListEmptyString() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(listOf(""))) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly("")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListEmptyStringFirstValue() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(listOf("", "two", "three"))) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly("", "two", "three").inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListEmptyStringMiddleValue() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(listOf("one", "", "three"))) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly("one", "", "three").inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectStringListEmptyStringLastValue() {
        @Serializable @SerialName("test") class TestClass(val arg: List<String>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(listOf("one", "two", ""))) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly("one", "two", "").inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEmptyIntList() {
        @Serializable @SerialName("test") class TestClass(val arg: List<Int>)

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(emptyList())) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).isEmpty()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithObjectEmptyIntListUseDefault() {
        @Serializable @SerialName("test") class TestClass(val arg: List<Int> = listOf(1, 2))

        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = TestClass(emptyList())) {
                test<TestClass>()
            }
        assertThat(navController.currentDestination?.route).isEqualTo("test?arg={arg}")
        val route = navController.currentBackStackEntry?.toRoute<TestClass>()
        assertThat(route!!.arg).isNotNull()
        assertThat(route.arg).containsExactly(1, 2).inOrder()
    }

    @UiThreadTest
    @Test
    fun testDeepLinkFromNavGraph() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination("second_test/{arg2}")
                .setArguments(bundleOf("arg2" to "value"))
                .createTaskStackBuilder()
        assertThat(taskStackBuilder).isNotNull()
        assertThat(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkIntent() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph

        val args =
            bundleOf(
                "test" to "test",
                "arg2" to "value",
            )
        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination("second_test/{arg2}")
                .setArguments(args)
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        navController.handleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkIntentWithDefaultArgs() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination("second_test/{arg2}")
                .setArguments(bundleOf("arg2" to "value"))
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        navController.handleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)

        val destination = navController.currentDestination
        assertThat(destination?.route).isEqualTo("second_test/{arg2}")
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkValid() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val collectedDestinationIds = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.route)
        }

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination("second_test/{arg2}")
                .setArguments(bundleOf("arg2" to "value"))
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()
        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly("start_test", "start_test", "second_test/{arg2}")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkNestedStartDestination() {
        val navController = createNavController()
        navController.graph = nav_nested_start_destination_route_graph
        val collectedDestinationIds = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.route)
        }

        val taskStackBuilder =
            navController.createDeepLink().setDestination("second_test").createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly("nested_test", "nested_test", "second_test")
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkMultipleDestinations() {
        val navController = createNavController()
        navController.graph = nav_multiple_navigation_route_graph
        val collectedDestinationRoutes = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationRoutes.add(destination.route)
        }

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination("simple_child_second_test")
                .addDestination("deep_link_child_second_test")
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationRoutes)
            .containsExactly(
                // First to the destination added via setDestination()
                "simple_child_start_test",
                "simple_child_start_test",
                "simple_child_second_test",
                // Then to the second destination added via addDestination()
                "deep_link_child_start_test",
                "deep_link_child_second_test"
            )
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkMultipleDestinationsWithArgs() {
        val navController = createNavController()
        navController.graph = nav_multiple_navigation_route_graph
        val collectedDestinations = mutableListOf<Pair<String?, Bundle?>>()
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            collectedDestinations.add(destination.route to arguments)
        }

        val globalBundle = Bundle().apply { putString("global", "global") }
        val firstBundle = Bundle().apply { putString("test", "first") }
        val secondBundle =
            Bundle().apply {
                putString("global", "overridden")
                putString("test", "second")
            }
        val taskStackBuilder =
            navController
                .createDeepLink()
                .setDestination(createRoute("simple_child_second_test").hashCode(), firstBundle)
                .addDestination(createRoute("deep_link_child_second_test").hashCode(), secondBundle)
                .setArguments(globalBundle)
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        // First to the destination added via setDestination()
        val (destinationRoutes, bundle) = collectedDestinations[0]
        assertThat(destinationRoutes).isEqualTo("simple_child_start_test")
        assertThat(bundle).isEqualTo(null)

        val (destinationId1, bundle1) = collectedDestinations[1]
        assertThat(destinationId1).isEqualTo("simple_child_start_test")
        assertThat(bundle1).string("global").isEqualTo("global")
        assertThat(bundle1).string("test").isEqualTo("first")

        val (destinationId2, bundle2) = collectedDestinations[2]
        assertThat(destinationId2).isEqualTo("simple_child_second_test")
        assertThat(bundle2).string("global").isEqualTo("global")
        assertThat(bundle2).string("test").isEqualTo("first")

        // Then to the second destination added via addDestination()
        val (destinationId3, bundle3) = collectedDestinations[3]
        assertThat(destinationId3).isEqualTo("deep_link_child_start_test")
        assertThat(bundle3).string("global").isEqualTo("overridden")
        assertThat(bundle3).string("test").isEqualTo("second")

        val (destinationId4, bundle4) = collectedDestinations[4]
        assertThat(destinationId4).isEqualTo("deep_link_child_second_test")
        assertThat(bundle4).string("global").isEqualTo("overridden")
        assertThat(bundle4).string("test").isEqualTo("second")

        assertWithMessage("$collectedDestinations should have 5 destinations")
            .that(collectedDestinations)
            .hasSize(5)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkInvalid() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val collectedDestinationRoutes = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationRoutes.add(destination.route)
        }

        assertThat(collectedDestinationRoutes).containsExactly("start_test")

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setGraph(nav_nested_start_destination_route_graph)
                .setDestination("nested_second_test")
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        assertWithMessage("$collectedDestinationRoutes should have 1 destination id")
            .that(collectedDestinationRoutes)
            .hasSize(1)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkFromRouteOnlyIfExplicitlyAdded() {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = "start") {
                test("start") { deepLink { uriPattern = createRoute("explicit_start_deeplink") } }
            }
        val collectedDestinationRoutes = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationRoutes.add(destination.route)
        }

        assertThat(collectedDestinationRoutes).containsExactly("start")

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(createRoute("explicit_start_deeplink")),
                ApplicationProvider.getApplicationContext() as Context,
                TestActivity::class.java
            )

        assertWithMessage("handleDeepLink should return true with valid deep link")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        assertWithMessage("$collectedDestinationRoutes should have 2 destination id")
            .that(collectedDestinationRoutes)
            .hasSize(2)
        assertThat(collectedDestinationRoutes).containsExactly("start", "start")

        val intent2 =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(createRoute("start")),
                ApplicationProvider.getApplicationContext() as Context,
                TestActivity::class.java
            )

        assertWithMessage("handleDeepLink should return false with invalid deep link")
            .that(navController.handleDeepLink(intent2))
            .isFalse()

        assertWithMessage("$collectedDestinationRoutes should have 2 destination id")
            .that(collectedDestinationRoutes)
            .hasSize(2)
        assertThat(collectedDestinationRoutes).containsExactly("start", "start")
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkToRootInvalid() {
        val navController = createNavController()
        navController.graph = nav_simple_route_graph
        val collectedDestinationRoutes = mutableListOf<String?>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationRoutes.add(destination.route)
        }

        assertThat(collectedDestinationRoutes).containsExactly("start_test")

        val taskStackBuilder =
            navController
                .createDeepLink()
                .setGraph(nav_nested_start_destination_route_graph)
                .setDestination("nested_test")
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        assertWithMessage("$collectedDestinationRoutes should have 1 destination id")
            .that(collectedDestinationRoutes)
            .hasSize(1)
    }

    @UiThreadTest
    @Test
    fun testSetOnBackPressedDispatcherOnNavBackStackEntry() {
        var backPressedIntercepted = false
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner()
        val dispatcher = OnBackPressedDispatcher()

        navController.setLifecycleOwner(lifecycleOwner)
        navController.setOnBackPressedDispatcher(dispatcher)

        navController.graph = nav_simple_route_graph
        navController.navigate("second_test/arg2")
        assertThat(navController.previousBackStackEntry?.destination?.route).isEqualTo("start_test")

        dispatcher.addCallback(navController.currentBackStackEntry!!) {
            backPressedIntercepted = true
        }

        // Move to STOPPED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        // Move back up to RESUMED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        dispatcher.onBackPressed()

        assertThat(backPressedIntercepted).isTrue()
    }

    @UiThreadTest
    @Test
    fun testSetGraph13Entries() {
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner()
        val dispatcher = OnBackPressedDispatcher()

        navController.setLifecycleOwner(lifecycleOwner)
        navController.setOnBackPressedDispatcher(dispatcher)

        val navRepeatedGraph =
            createNavController().createGraph(route = "nav_root", startDestination = "0") {
                repeat(13) { index -> test("$index") }
            }

        navController.graph = navRepeatedGraph
        navController.graph = navRepeatedGraph
    }

    private fun createNavController(): NavController {
        val navController = NavController(ApplicationProvider.getApplicationContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}

private enum class TestTopLevelEnum {
    ONE,
    TWO
}
