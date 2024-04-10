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
import androidx.navigation.serialization.generateRoutePattern
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderTest {
    private val provider = NavigatorProvider().apply {
        addNavigator(NavGraphNavigator(this))
        addNavigator(NoOpNavigator())
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigation() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            navDestination(DESTINATION_ID) {}
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationRoute() {
        val graph = provider.navigation(
            startDestination = DESTINATION_ROUTE
        ) {
            navDestination(DESTINATION_ROUTE) {}
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigationUnaryPlus() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            +provider[NoOpNavigator::class].createDestination().apply {
                id = DESTINATION_ID
            }
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationUnaryPlusRoute() {
        val graph = provider.navigation(
            startDestination = DESTINATION_ROUTE
        ) {
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
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            val destination = provider[NoOpNavigator::class].createDestination().apply {
                id = DESTINATION_ID
            }
            addDestination(destination)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
    }

    @Test
    fun navigationAddDestinationRoute() {
        val graph = provider.navigation(
            startDestination = DESTINATION_ROUTE
        ) {
            val destination = provider[NoOpNavigator::class].createDestination().apply {
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
        @Serializable
        class TestClass

        val serializer = serializer<TestClass>()
        val route = serializer.generateRoutePattern()
        val graph = provider.navigation(
            startDestination = route
        ) {
            val builder = NavDestinationBuilder(provider[NoOpNavigator::class], TestClass::class)
            addDestination(builder.build())
        }
        assertWithMessage("Destination route should be added to the graph")
            .that(route in graph)
            .isTrue()
        assertWithMessage("Destination id should be added to the graph")
            .that(serializer.hashCode() in graph)
            .isTrue()
    }

    @Test
    fun navigationAddDestinationWithArgsKClassBuilder() {
        @Serializable
        class TestClass(val arg: Int)

        val serializer = serializer<TestClass>()
        val route = serializer.generateRoutePattern()
        val graph = provider.navigation(
            startDestination = route
        ) {
            val builder = NavDestinationBuilder(provider[NoOpNavigator::class], TestClass::class)
            addDestination(builder.build())
        }
        assertWithMessage("Destination route should be added to the graph")
            .that(route in graph)
            .isTrue()
        assertWithMessage("Destination id should be added to the graph")
            .that(serializer.hashCode() in graph)
            .isTrue()
    }

    @Test fun navigationStartDestinationKClass() {
        @Serializable
        class TestClass(val arg: Int)

        @Serializable
        class Graph(val arg: Int)

        val serializer = serializer<TestClass>()
        val expected = serializer.generateRoutePattern()
        val graph = provider.navigation(
            route = Graph::class,
            startDestination = TestClass::class
        ) {
            navDestination(TestClass::class) { }
        }
        assertWithMessage("Destination route should be added to the graph")
            .that(expected in graph)
            .isTrue()
        assertWithMessage("startDestinationRoute should be set")
            .that(graph.startDestinationRoute)
            .isEqualTo(expected)
        assertWithMessage("startDestinationId should be set")
            .that(graph.startDestinationId)
            .isEqualTo(serializer.hashCode())
    }

    @Suppress("DEPRECATION")
    @Test(expected = IllegalStateException::class)
    fun navigationMissingStartDestination() {
        provider.navigation(startDestination = 0) {
            navDestination(DESTINATION_ID) {}
        }
        fail("NavGraph should throw IllegalStateException if startDestination is zero")
    }

    @Test(expected = IllegalArgumentException::class)
    fun navigationMissingStartDestinationRoute() {
        provider.navigation(startDestination = "") {
            navDestination(DESTINATION_ROUTE) {}
        }
        fail("NavGraph should throw IllegalStateException if no startDestinationRoute is set")
    }

    @Suppress("DEPRECATION")
    @Test
    fun navigationNested() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
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
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
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
        @Serializable
        class TestClass(val arg: Int)

        @Serializable
        class NestedGraph(val arg: Int)

        val serializer = serializer<TestClass>()
        val expected = serializer.generateRoutePattern()
        val graph = provider.navigation(startDestination = NestedGraph::class) {
            navigation(startDestination = TestClass::class, route = NestedGraph::class) {
                navDestination(TestClass::class) {}
            }
        }
        val nestedGraph = graph.findNode(
            serializer<NestedGraph>().generateRoutePattern()
        ) as NavGraph
        assertThat(nestedGraph.startDestinationRoute).isEqualTo(expected)
        assertThat(nestedGraph.startDestinationId).isEqualTo(serializer.hashCode())
        assertWithMessage("Destination should be added to the nested graph")
            .that(expected in nestedGraph)
            .isTrue()
    }
}

private const val DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
private const val DESTINATION_ROUTE = "first"
private const val SECOND_DESTINATION_ROUTE = "second"

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
@Suppress("DEPRECATION")
fun NavGraphBuilder.navDestination(
    @IdRes id: Int,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[NoOpNavigator::class], id).apply(builder))

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
fun NavGraphBuilder.navDestination(
    route: String,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[NoOpNavigator::class], route).apply(builder))

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
fun NavGraphBuilder.navDestination(
    route: KClass<*>,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
) = destination(NavDestinationBuilder(provider[NoOpNavigator::class], route).apply(builder))
