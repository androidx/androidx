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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
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
