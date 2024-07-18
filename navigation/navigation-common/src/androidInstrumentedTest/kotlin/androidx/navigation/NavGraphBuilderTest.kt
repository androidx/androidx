/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.IdRes
import androidx.navigation.serialization.expectedSafeArgsId
import androidx.navigation.serialization.generateRoutePattern
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderTest {
    private val provider =
        NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(NoOpNavigator())
        }

    @Suppress("DEPRECATION")
    @Test
    fun navigation() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ID) {
                navDestination(DESTINATION_ID) {}
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationRoute() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ROUTE) {
                navDestination(DESTINATION_ROUTE) {}
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigationUnaryPlus() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ID) {
                +provider[NoOpNavigator::class].createDestination().apply { id = DESTINATION_ID }
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationUnaryPlusRoute() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ROUTE) {
                +provider[NoOpNavigator::class].createDestination().apply {
                    route = DESTINATION_ROUTE
                }
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigationAddDestination() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ID) {
                val destination =
                    provider[NoOpNavigator::class].createDestination().apply { id = DESTINATION_ID }
                addDestination(destination)
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationAddDestinationRoute() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ROUTE) {
                val destination =
                    provider[NoOpNavigator::class].createDestination().apply {
                        route = DESTINATION_ROUTE
                    }
                addDestination(destination)
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Test
    fun navigationAddDestinationKClassBuilder() {
        @Serializable class TestClass

        val serializer = serializer<TestClass>()
        val route = serializer.generateRoutePattern()
        val graph =
            provider.navigation(startDestination = route) {
                val builder =
                    NavDestinationBuilder(
                        provider[NoOpNavigator::class],
                        TestClass::class,
                        emptyMap()
                    )
                addDestination(builder.build())
            }
        assertWithMessage("Destination route should be added to the graph")
            .that(route in graph)
            .isTrue()
        assertWithMessage("Destination id should be added to the graph")
            .that(serializer.expectedSafeArgsId() in graph)
            .isTrue()
    }

    @Test
    fun navigationAddDestinationWithArgsKClassBuilder() {
        @Serializable class TestClass(val arg: Int)

        val serializer = serializer<TestClass>()
        val route = serializer.generateRoutePattern()
        val graph =
            provider.navigation(startDestination = route) {
                val builder =
                    NavDestinationBuilder(
                        provider[NoOpNavigator::class],
                        TestClass::class,
                        emptyMap()
                    )
                addDestination(builder.build())
            }
        assertWithMessage("Destination route should be added to the graph")
            .that(route in graph)
            .isTrue()
        assertWithMessage("Destination id should be added to the graph")
            .that(serializer.expectedSafeArgsId() in graph)
            .isTrue()
    }

    @Test
    fun navigationStartDestinationKClass() {
        @Serializable class Graph(val arg: Int)

        @Serializable class TestClass(val arg: Int)

        val graph =
            provider.navigation(route = Graph::class, startDestination = TestClass::class) {
                navDestination(TestClass::class) {}
            }

        // assert graph info
        val expectedGraphRoute =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationStartDestinationKClass.Graph/{arg}"
        assertWithMessage("graph route should be set")
            .that(graph.route)
            .isEqualTo(expectedGraphRoute)
        assertWithMessage("graph id should be set")
            .that(graph.id)
            .isEqualTo(serializer<Graph>().expectedSafeArgsId())

        // assert start destination info
        val expectedStartRoute =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationStartDestinationKClass.TestClass/{arg}"
        assertWithMessage("Destination route should be added to the graph")
            .that(expectedStartRoute in graph)
            .isTrue()
        assertWithMessage("startDestinationRoute should be set")
            .that(graph.startDestinationRoute)
            .isEqualTo(expectedStartRoute)
        assertWithMessage("startDestinationId should be set")
            .that(graph.startDestinationId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
    }

    @Test
    fun navigationStartDestinationObject() {
        @Serializable class Graph(val arg: Int)

        @Serializable class TestClass(val arg2: Int)

        val graph =
            provider.navigation(route = Graph::class, startDestination = TestClass(1)) {
                navDestination(TestClass::class) {}
            }

        // assert graph info
        val expectedGraphRoute =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationStartDestinationObject.Graph/{arg}"
        assertWithMessage("graph route should be set")
            .that(graph.route)
            .isEqualTo(expectedGraphRoute)
        assertWithMessage("graph id should be set")
            .that(graph.id)
            .isEqualTo(serializer<Graph>().expectedSafeArgsId())

        // assert start destination info
        val expectedStartRoute =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationStartDestinationObject.TestClass/1"
        assertWithMessage("Destination route should be added to the graph")
            .that(expectedStartRoute in graph)
            .isTrue()
        assertWithMessage("startDestinationRoute should be set")
            .that(graph.startDestinationRoute)
            .isEqualTo(expectedStartRoute)
        assertWithMessage("startDestinationId should be set")
            .that(graph.startDestinationId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
    }

    @Suppress("DEPRECATION")
    @Test(expected = IllegalStateException::class)
    fun navigationMissingStartDestination() {
        provider.navigation(startDestination = 0) { navDestination(DESTINATION_ID) {} }
        fail("NavGraph should throw IllegalStateException if startDestination is zero")
    }

    @Test(expected = IllegalArgumentException::class)
    fun navigationMissingStartDestinationRoute() {
        provider.navigation(startDestination = "") { navDestination(DESTINATION_ROUTE) {} }
        fail("NavGraph should throw IllegalStateException if no startDestinationRoute is set")
    }

    @Test
    fun navigationMissingStartDestinationKClass() {
        @Serializable class TestClass(val arg: Int)

        assertFailsWith<IllegalStateException> {
            provider.navigation(startDestination = TestClass::class) {
                // nav destination must have been added via route from KClass
                navDestination("route") {}
            }
        }
    }

    @Test
    fun navigationMissingStartDestinationObject() {
        @Serializable class TestClass(val arg: Int)

        assertFailsWith<IllegalStateException> {
            provider.navigation(startDestination = TestClass(0)) {
                // nav destination must have been added via route from KClass
                navDestination("route") {}
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigationNested() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ID) {
                navigation(DESTINATION_ID, startDestination = SECOND_DESTINATION_ID) {
                    navDestination(SECOND_DESTINATION_ID) {}
                }
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationNestedRoute() {
        val graph =
            provider.navigation(startDestination = DESTINATION_ROUTE) {
                navigation(startDestination = SECOND_DESTINATION_ROUTE, route = DESTINATION_ROUTE) {
                    navDestination(SECOND_DESTINATION_ROUTE) {}
                }
            }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Test
    fun navigationNestedKClass() {
        @Serializable class TestClass(val arg: Int)

        @Serializable class NestedGraph(val arg: Int)

        val graph =
            provider.navigation(startDestination = NestedGraph::class) {
                navigation<NestedGraph>(startDestination = TestClass::class) {
                    navDestination(TestClass::class) {}
                }
            }
        val nestedGraph =
            graph.findNode(serializer<NestedGraph>().generateRoutePattern()) as NavGraph
        // assert graph
        val expectedNestedGraph =
            "androidx.navigation.NavGraphBuilderTest." + "navigationNestedKClass.NestedGraph/{arg}"
        assertThat(nestedGraph.route).isEqualTo(expectedNestedGraph)
        assertThat(nestedGraph.id).isEqualTo(serializer<NestedGraph>().expectedSafeArgsId())
        // assert nested startDestination
        val expectedNestedStart =
            "androidx.navigation.NavGraphBuilderTest." + "navigationNestedKClass.TestClass/{arg}"
        assertThat(nestedGraph.startDestinationRoute).isEqualTo(expectedNestedStart)
        assertThat(nestedGraph.startDestinationId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("Destination should be added to the nested graph")
            .that(expectedNestedStart in nestedGraph)
            .isTrue()
    }

    @Test
    fun navigationNestedObject() {
        @Serializable class TestClass(val arg2: Int)

        @Serializable class NestedGraph(val arg: Int)

        val graph =
            provider.navigation(startDestination = NestedGraph::class) {
                navigation<NestedGraph>(startDestination = TestClass(15)) {
                    navDestination(TestClass::class) {}
                }
            }
        val nestedGraph =
            graph.findNode(serializer<NestedGraph>().generateRoutePattern()) as NavGraph

        // assert graph
        val expectedNestedGraph =
            "androidx.navigation.NavGraphBuilderTest." + "navigationNestedObject.NestedGraph/{arg}"
        assertThat(nestedGraph.route).isEqualTo(expectedNestedGraph)
        assertThat(nestedGraph.id).isEqualTo(serializer<NestedGraph>().expectedSafeArgsId())

        // assert nested StartDestination
        val expectedNestedStart =
            "androidx.navigation.NavGraphBuilderTest." + "navigationNestedObject.TestClass/15"
        assertThat(nestedGraph.startDestinationRoute).isEqualTo(expectedNestedStart)
        assertThat(nestedGraph.startDestinationId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("Destination should be added to the nested graph")
            .that(expectedNestedStart in nestedGraph)
            .isTrue()
    }

    @Test
    fun navigationNestedObjectAndKClass() {
        @Serializable class TestClass(val arg2: Int)

        @Serializable class NestedGraph(val arg: Int)

        val graph =
            provider.navigation(startDestination = NestedGraph(0)) {
                navigation<NestedGraph>(startDestination = TestClass(15)) {
                    navDestination(TestClass::class) {}
                }
            }
        val nestedGraph =
            graph.findNode(serializer<NestedGraph>().generateRoutePattern()) as NavGraph

        // assert graph
        val expectedStart =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationNestedObjectAndKClass.NestedGraph/0"
        assertThat(graph.startDestinationRoute).isEqualTo(expectedStart)
        assertThat(graph.startDestinationId)
            .isEqualTo(serializer<NestedGraph>().expectedSafeArgsId())

        // assert nested graph
        val expectedNestedGraph =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationNestedObjectAndKClass.NestedGraph/{arg}"
        assertThat(nestedGraph.route).isEqualTo(expectedNestedGraph)
        assertThat(nestedGraph.id).isEqualTo(serializer<NestedGraph>().expectedSafeArgsId())

        // assert nested StartDestination
        val expectedNestedStart =
            "androidx.navigation.NavGraphBuilderTest." +
                "navigationNestedObjectAndKClass.TestClass/15"
        assertThat(nestedGraph.startDestinationRoute).isEqualTo(expectedNestedStart)
        assertThat(nestedGraph.startDestinationId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("Destination should be added to the nested graph")
            .that(expectedNestedStart in nestedGraph)
            .isTrue()
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
private const val DESTINATION_ROUTE = "first"
private const val SECOND_DESTINATION_ROUTE = "second"

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be added to a
 * NavGraph (hence why this is not in the common-ktx library)
 */
@Suppress("DEPRECATION")
fun NavGraphBuilder.navDestination(
    @IdRes id: Int,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[NoOpNavigator::class], id).apply(builder))

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be added to a
 * NavGraph (hence why this is not in the common-ktx library)
 */
fun NavGraphBuilder.navDestination(
    route: String,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[NoOpNavigator::class], route).apply(builder))

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be added to a
 * NavGraph (hence why this is not in the common-ktx library)
 */
fun NavGraphBuilder.navDestination(
    route: KClass<*>,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) =
    destination(
        NavDestinationBuilder(provider[NoOpNavigator::class], route, emptyMap()).apply(builder)
    )
