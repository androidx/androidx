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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavUri
import androidx.navigation.contains
import androidx.navigation.get
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.testing.TestNavHostController
import androidx.savedstate.SavedState
import androidx.savedstate.read
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@OptIn(ExperimentalTestApi::class)
@ExperimentalCoroutinesApi
class NavGraphBuilderTest {

    @Test
    fun testDeepLink() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(NavUri(uriString)).build()
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable(
                    secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {}
            }
        }

        runOnUiThread {
            navController.navigate(NavUri(uriString))
            assertThat(navController.currentBackStackEntry!!.destination.hasDeepLink(deeplink))
                .isTrue()
        }
    }

    @Test
    fun testNestedNavigationDeepLink() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(NavUri(uriString)).build()
        setContent {
            navController = TestNavHostController()
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

        runOnUiThread {
            navController.navigate(NavUri(uriString))
            assertThat(
                navController.getBackStackEntry(secondRoute).destination.hasDeepLink(deeplink)
            )
                .isTrue()
        }
    }

    @Test
    fun testCurrentBackStackEntryNavigate() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        val key = "key"
        val arg = "myarg"
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable("$secondRoute/{$key}") {}
            }
        }

        runOnUiThread {
            navController.navigate("$secondRoute/$arg")
            assertThat(navController.currentBackStackEntry!!.arguments!!.read { getString(key) })
                .isEqualTo(arg)
        }
    }

    @Test
    fun testDefaultArguments() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        val key = "key"
        val defaultArg = "default"
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable(
                    secondRoute,
                    arguments = listOf(navArgument(key) { defaultValue = defaultArg }),
                ) {}
            }
        }

        runOnUiThread {
            navController.navigate(secondRoute)
            assertThat(navController.currentBackStackEntry!!.arguments!!.read { getString(key) })
                .isEqualTo(defaultArg)
        }
    }

    @Test
    fun testNavigationNestedStart() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                navigation(startDestination = secondRoute, route = firstRoute) {
                    composable(secondRoute) {}
                }
            }
        }

        runOnUiThread {
            assertWithMessage("Destination should be added to the graph")
                .that(firstRoute in navController.graph)
                .isTrue()
        }
    }

    @Test
    fun testNavigationNestedInGraph() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(startDestination = thirdRoute, route = secondRoute) {
                    composable(thirdRoute) {}
                }
            }
        }

        runOnUiThread {
            navController.navigate(secondRoute)
            assertWithMessage("Destination should be added to the graph")
                .that(secondRoute in navController.graph)
                .isTrue()
        }
    }

    @Test
    fun testNestedNavigationDefaultArguments() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        val key = "key"
        val defaultArg = "default"
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(
                    startDestination = thirdRoute,
                    route = secondRoute,
                    arguments = listOf(navArgument(key) { defaultValue = defaultArg }),
                ) {
                    composable(thirdRoute) {}
                }
            }
        }

        runOnUiThread {
            navController.navigate(secondRoute)
            assertThat(navController.currentBackStackEntry!!.arguments!!.read { getString(key) })
                .isEqualTo(defaultArg)
        }
    }

    @Test
    fun testNavigationKClassStart() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass::class) { composable<TestClass> {} }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedKClassStart() = runComposeUiTestOnUiThread {
        @Serializable class TestOuterClass
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestOuterClass::class) {
                navigation<TestOuterClass>(startDestination = TestClass::class) {
                    composable<TestClass> {}
                }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestOuterClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationKClassNestedInGraph() = runComposeUiTestOnUiThread {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClass::class) {
                    composable<TestClass> {}
                }
            }
        }

        runOnUiThread {
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
    fun testNavigationObjectStart() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass()) { composable<TestClass> {} }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationObjectStartArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(15)) {
                composable<TestClassArg> {}
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationNestedObjectStart() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(1)) {
                navigation<TestClassArg>(startDestination = TestClass()) {
                    composable<TestClass> {}
                }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedObjectStartArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClass::class) {
                navigation<TestClass>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationNestedAllObjectsStart() = runComposeUiTestOnUiThread {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = NestedGraph()) {
                navigation<NestedGraph>(startDestination = TestClass()) { composable<TestClass> {} }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testNavigationNestedAllObjectsStartArgs() = runComposeUiTestOnUiThread {
        @Serializable class NestedGraph(val graphArg: Boolean)

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = NestedGraph(false)) {
                navigation<NestedGraph>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getBoolean("graphArg") })
                .isEqualTo(false)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationObjectNestedInGraph() = runComposeUiTestOnUiThread {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClass()) { composable<TestClass> {} }
            }
        }

        runOnUiThread {
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
    fun testNavigationObjectArgsNestedInGraph() = runComposeUiTestOnUiThread {
        @Serializable class NestedGraph

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation<NestedGraph>(startDestination = TestClassArg(15)) {
                    composable<TestClassArg> {}
                }
            }
        }

        runOnUiThread {
            navController.navigate(NestedGraph())
            assertWithMessage("Destination should be added to the graph")
                .that(NestedGraph::class in navController.graph)
                .isTrue()
            val nestedGraph = navController.graph.findNode<NestedGraph>() as NavGraph
            assertThat(nestedGraph.findStartDestination().route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
        }
    }

    @Test
    fun testComposableKClass() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClass> {}
            }
        }
        runOnIdle {
            assertThat(firstRoute in navController.graph).isTrue()
            assertThat(TestClass::class in navController.graph).isTrue()
            assertThat(navController.graph[TestClass::class].route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testComposableKClassArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClassArg> {}
            }
        }
        runOnIdle {
            assertThat(TestClassArg::class in navController.graph).isTrue()
            val dest = navController.graph[TestClassArg::class]
            assertThat(dest.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testComposableKClassArgsCustomType() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                composable<TestClass>(typeMap = mapOf(typeOf<CustomType>() to customNavType)) {}
            }
        }
        runOnIdle {
            val dest = navController.graph[TestClass::class]
            assertThat(dest.arguments["arg"]).isNotNull()
            assertThat(dest.arguments["arg"]!!.type).isEqualTo(customNavType)
        }
    }

    @Test
    fun testNestedComposableKClassArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(startDestination = TEST_CLASS_ARG_ROUTE, route = secondRoute) {
                    composable<TestClassArg> {}
                }
            }
        }
        runOnIdle {
            val nestedGraph = navController.graph[secondRoute] as NavGraph
            val dest = nestedGraph.findNode<TestClassArg>()
            assertThat(dest).isNotNull()
            assertThat(dest!!.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testComposableKClassArgsMissingCustomType() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var exception: String
        lateinit var navController: TestNavHostController
        try {
            setContent {
                navController = TestNavHostController()
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
                "Route ${TestClass.serializer().descriptor.serialName} could " +
                    "not find any NavType for argument arg of type androidx" +
                    ".navigation.compose.CustomType - typeMap received was {}"
            )
    }

    @Test
    fun testDialogKClass() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            navController.navigatorProvider.addNavigator(DialogNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClass> {}
            }
        }
        runOnIdle {
            assertThat(firstRoute in navController.graph).isTrue()
            assertThat(TestClass::class in navController.graph).isTrue()
            assertThat(navController.graph[TestClass::class].route).isEqualTo(TEST_CLASS_ROUTE)
        }
    }

    @Test
    fun testDialogKClassArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClassArg> {}
            }
        }
        runOnIdle {
            assertThat(TestClassArg::class in navController.graph).isTrue()
            val dest = navController.graph[TestClassArg::class]
            assertThat(dest.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testDialogKClassArgsCustomType() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                dialog<TestClass>(typeMap = mapOf(typeOf<CustomType>() to customNavType)) {}
            }
        }
        runOnIdle {
            val dest = navController.graph[TestClass::class]
            assertThat(dest.arguments["arg"]).isNotNull()
            assertThat(dest.arguments["arg"]!!.type).isEqualTo(customNavType)
        }
    }

    @Test
    fun testNestedDialogKClassArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = firstRoute) {
                composable(firstRoute) {}
                navigation(startDestination = TEST_CLASS_ARG_ROUTE, route = secondRoute) {
                    dialog<TestClassArg> {}
                }
            }
        }
        runOnIdle {
            val nestedGraph = navController.graph[secondRoute] as NavGraph
            val dest = nestedGraph.findNode<TestClassArg>()
            assertThat(dest).isNotNull()
            assertThat(dest!!.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(dest.arguments["arg"]).isNotNull()
        }
    }

    @Test
    fun testDialogKClassArgsMissingCustomType() = runComposeUiTestOnUiThread {
        @Serializable class TestClass(val arg: CustomType)

        lateinit var exception: String
        lateinit var navController: TestNavHostController
        try {
            setContent {
                navController = TestNavHostController()
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
                "Route ${TestClass.serializer().descriptor.serialName} could not " +
                    "find any NavType for argument arg of type androidx.navigation" +
                    ".compose.CustomType - typeMap received was {}"
            )
    }

    @Test
    fun testNavigationDialogObjectStartArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(DialogNavigator())
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController, startDestination = TestClassArg(15)) { dialog<TestClassArg> {} }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClassArg::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
        }
    }

    @Test
    fun testNavigationDialogNestedObjectStartArgs() = runComposeUiTestOnUiThread {
        lateinit var navController: TestNavHostController
        setContent {
            navController = TestNavHostController()
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            navController.navigatorProvider.addNavigator(DialogNavigator())

            NavHost(navController, startDestination = TestClass::class) {
                navigation<TestClass>(startDestination = TestClassArg(15)) {
                    dialog<TestClassArg> {}
                }
            }
        }

        runOnUiThread {
            assertThat(navController.currentDestination?.route).isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertWithMessage("Destination should be added to the graph")
                .that(TestClass::class in navController.graph)
                .isTrue()
            assertThat(navController.graph.findStartDestination().route)
                .isEqualTo(TEST_CLASS_ARG_ROUTE)
            assertThat(navController.currentBackStackEntry?.arguments?.read { getInt("arg") }).isEqualTo(15)
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
        override fun put(bundle: SavedState, key: String, value: CustomType) {}

        override fun get(bundle: SavedState, key: String): CustomType? = null

        override fun parseValue(value: String): CustomType = CustomType()

        override fun serializeAsValue(value: CustomType) = "customValue"
    }


@OptIn(InternalSerializationApi::class)
private operator fun NavGraph.get(route: KClass<*>) = findNode(route.serializer().generateHashCode())!!

@OptIn(InternalSerializationApi::class)
private operator fun NavGraph.contains(route: KClass<*>) = findNode(route.serializer().generateHashCode()) != null