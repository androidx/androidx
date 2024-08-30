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

import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.contains
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.typeOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntryNavigate() {
        lateinit var navController: TestNavHostController
        val key = "key"
        val arg = "myarg"
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable("$secondRoute/{$key}") {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate("$secondRoute/$arg")
            assertThat(navController.currentBackStackEntry!!.arguments!!.getString(key))
                .isEqualTo(arg)
        }
    }

    @Test
    fun testDefaultArguments() {
        lateinit var navController: TestNavHostController
        val key = "key"
        val defaultArg = "default"
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable(
                    secondRoute,
                    arguments = listOf(navArgument(key) { defaultValue = defaultArg })
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(secondRoute)
            assertThat(navController.currentBackStackEntry!!.arguments!!.getString(key))
                .isEqualTo(defaultArg)
        }
    }

    @Test
    fun testDeepLink() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable(
                    secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(uriString.toUri())
            assertThat(navController.currentBackStackEntry!!.destination.hasDeepLink(deeplink))
                .isTrue()
        }
    }

    @Test
    fun testNavigationNestedStart() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                navigation(startDestination = secondRoute, route = firstRoute) {
                    composable(secondRoute) {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertWithMessage("Destination should be added to the graph")
                .that(firstRoute in navController.graph)
                .isTrue()
        }
    }

    @Test
    fun testNavigationNestedInGraph() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(startDestination = thirdRoute, route = secondRoute) {
                    composable(thirdRoute) {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(secondRoute)
            assertWithMessage("Destination should be added to the graph")
                .that(secondRoute in navController.graph)
                .isTrue()
        }
    }

    @Test
    fun testNestedNavigationDefaultArguments() {
        lateinit var navController: TestNavHostController
        val key = "key"
        val defaultArg = "default"
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = thirdRoute,
                    route = secondRoute,
                    arguments = listOf(navArgument(key) { defaultValue = defaultArg })
                ) {
                    composable(thirdRoute) {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(secondRoute)
            assertThat(navController.currentBackStackEntry!!.arguments!!.getString(key))
                .isEqualTo(defaultArg)
        }
    }

    @Test
    fun testNestedNavigationDeepLink() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = thirdRoute,
                    route = secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {
                    composable(thirdRoute) {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(uriString.toUri())
            assertThat(
                    navController.getBackStackEntry(secondRoute).destination.hasDeepLink(deeplink)
                )
                .isTrue()
        }
    }

    @Test
    fun testNavigationKClassStart() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass::class) { composable<TestClass> {} }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedKClassStart() {
        @Serializable class TestOuterClass

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestOuterClass::class) {
                navigation<TestOuterClass>(startDestination = TestClass::class) {
                    composable<TestClass> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestOuterClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationKClassNestedInGraph() {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClass::class) {
                    composable<TestClass> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(NestedGraph())
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            val nestedGraph = navController.graph.findNode<NestedGraph>() as NavGraph
            assertThat(nestedGraph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationObjectStart() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass()) { composable<TestClass> {} }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationObjectStartArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(15)) {
                composable<TestClassArg> {}
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationNestedObjectStart() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(1)) {
                navigation<TestClassArg>(startDestination = TestClass()) {
                    composable<TestClass> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedObjectStartArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass::class) {
                navigation<TestClass>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationNestedAllObjectsStart() {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = NestedGraph()) {
                navigation<NestedGraph>(startDestination = TestClass()) { composable<TestClass> {} }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedAllObjectsStartArgs() {
        @Serializable class NestedGraph(val graphArg: Boolean)

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = NestedGraph(false)) {
                navigation<NestedGraph>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getBoolean("graphArg"))
                .isEqualTo(false)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationObjectNestedInGraph() {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClass()) { composable<TestClass> {} }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(NestedGraph())
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            val nestedGraph = navController.graph.findNode<NestedGraph>() as NavGraph
            assertThat(nestedGraph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationObjectArgsNestedInGraph() {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(NestedGraph())
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            val nestedGraph = navController.graph.findNode<NestedGraph>() as NavGraph
            assertThat(nestedGraph.findStartDestination().route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }

    @Test
    fun testComposableKClass() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClass> {}
            }
        }
        composeTestRule.runOnIdle {
            assertThat(firstRoute in navController.graph).isTrue()
            assertThat(TestClass::class in navController.graph).isTrue()
            assertThat(navController.graph[TestClass::class].route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testComposableKClassArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClassArg> {}
            }
        }
        composeTestRule.runOnIdle {
            assertThat(TestClassArg::class in navController.graph).isTrue()
            val dest = navController.graph[TestClassArg::class]
            assertThat(dest.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testComposableKClassArgsCustomType() {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClass>(typeMap = mapOf(typeOf<CustomType>() to customNavType)) {}
            }
        }
        composeTestRule.runOnIdle {
            val dest = navController.graph[TestClass::class]
            assertThat(dest.arguments["arg"]).isNotNull()
            assertThat(dest.arguments["arg"]!!.type).isEqualTo(customNavType)
        }
    }

    @Test
    fun testNestedComposableKClassArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = TEST_CLASS_ARG_ROUTE,
                    route = secondRoute,
                ) {
                    composable<TestClassArg> {}
                }
            }
        }
        composeTestRule.runOnIdle {
            val nestedGraph = navController.graph[secondRoute] as NavGraph
            val dest = nestedGraph.findNode<TestClassArg>()
            assertThat(dest).isNotNull()
            assertThat(dest!!.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testComposableKClassArgsMissingCustomType() {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var exception: String
        lateinit var navController: TestNavHostController
        try {
            composeTestRule.setContent {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                NavHost(navController, startDestination = firstRoute) {
                    composable(firstRoute) {}
                    composable<TestClass> {}
                }
            }
        } catch (e: IllegalArgumentException) {
            exception = e.message!!
        }
        assertThat(exception)
            .isEqualTo(
                "Cannot cast arg of type androidx.navigation.compose.CustomType to a " +
                    "NavType. Make sure to provide custom NavType for this argument."
            )
    }

    @Test
    fun testDialogKClass() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            navController.navigatorProvider.addNavigator(DialogNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClass> {}
            }
        }
        composeTestRule.runOnIdle {
            assertThat(firstRoute in navController.graph).isTrue()
            assertThat(TestClass::class in navController.graph).isTrue()
            assertThat(navController.graph[TestClass::class].route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testDialogKClassArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClassArg> {}
            }
        }
        composeTestRule.runOnIdle {
            assertThat(TestClassArg::class in navController.graph).isTrue()
            val dest = navController.graph[TestClassArg::class]
            assertThat(dest.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testDialogKClassArgsCustomType() {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClass>(typeMap = mapOf(typeOf<CustomType>() to customNavType)) {}
            }
        }
        composeTestRule.runOnIdle {
            val dest = navController.graph[TestClass::class]
            assertThat(dest.arguments["arg"]).isNotNull()
            assertThat(dest.arguments["arg"]!!.type).isEqualTo(customNavType)
        }
    }

    @Test
    fun testNestedDialogKClassArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = TEST_CLASS_ARG_ROUTE,
                    route = secondRoute,
                ) {
                    dialog<TestClassArg> {}
                }
            }
        }
        composeTestRule.runOnIdle {
            val nestedGraph = navController.graph[secondRoute] as NavGraph
            val dest = nestedGraph.findNode<TestClassArg>()
            assertThat(dest).isNotNull()
            assertThat(dest!!.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testDialogKClassArgsMissingCustomType() {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var exception: String
        lateinit var navController: TestNavHostController
        try {
            composeTestRule.setContent {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(DialogNavigator())
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                NavHost(navController, startDestination = firstRoute) {
                    composable(firstRoute) {}
                    composable<TestClass> {}
                }
            }
        } catch (e: IllegalArgumentException) {
            exception = e.message!!
        }
        assertThat(exception)
            .isEqualTo(
                "Cannot cast arg of type androidx.navigation.compose.CustomType to a " +
                    "NavType. Make sure to provide custom NavType for this argument."
            )
    }

    @Test
    fun testNavigationDialogObjectStartArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(15)) { dialog<TestClassArg> {} }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationDialogNestedObjectStartArgs() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            navController.navigatorProvider.addNavigator(DialogNavigator())

            NavHost(navController, startDestination = TestClass::class) {
                navigation<TestClass>(startDestination = TestClassArg(15)) {
                    dialog<TestClassArg> {}
                }
            }
        }

        composeTestRule.runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.getInt("arg")).isEqualTo(15)
        }
    }
}

private const val firstRoute = "first"
private const val secondRoute = "second"
private const val thirdRoute = "third"
internal const val TEST_CLASS_ROUTE = "androidx.navigation.compose.TestClass"
internal const val TEST_CLASS_ARG_ROUTE = "androidx.navigation.compose.TestClassArg/{arg}"

@Serializable internal class TestClass

@Serializable internal class TestClassArg(val arg: Int)

@Serializable internal class CustomType

internal val customNavType =
    object : NavType<CustomType>(false) {
        override fun put(bundle: Bundle, key: String, value: CustomType) {}

        override fun get(bundle: Bundle, key: String): CustomType? = null

        override fun parseValue(value: String): CustomType = CustomType()

        override fun serializeAsValue(value: CustomType) = "customValue"
    }
