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
package androidx.navigation.dynamicfeatures

import androidx.annotation.IdRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavigatorProvider
import androidx.navigation.NoOpNavigator
import androidx.navigation.contains
import androidx.navigation.dynamicfeatures.shared.AndroidTestDynamicInstallManager
import androidx.navigation.get
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class DynamicNavGraphBuilderTest {

    private val provider = NavigatorProvider().apply {
        addNavigator(
            DynamicGraphNavigator(
                this,
                AndroidTestDynamicInstallManager(ApplicationProvider.getApplicationContext())
            )
        )
        addNavigator(NoOpNavigator())
    }

    @Suppress("DEPRECATION")
    @Test
    public fun navigation() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            moduleName = MODULE_NAME
            navDestination(DESTINATION_ID) {}
        } as DynamicGraphNavigator.DynamicNavGraph

        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("Module should be set in the graph")
            .that(graph.moduleName)
            .isEqualTo(MODULE_NAME)
    }

    @Suppress("DEPRECATION")
    public fun navigation_emptyModuleName() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
        }
        assertWithMessage("Without a moduleName the graph should be a NavGraph")
            .that(graph !is DynamicGraphNavigator.DynamicNavGraph)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun progressDestination() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            moduleName = MODULE_NAME
            progressDestination = PROGRESS_DESTINATION_ID
            navDestination(DESTINATION_ID) {}
            navDestination(PROGRESS_DESTINATION_ID) {}
        }

        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph)
            .isTrue()
        assertWithMessage("ProgressDestination should be added to the graph")
            .that(PROGRESS_DESTINATION_ID in graph)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    public fun progressDestination_notSet() {
        val graph = provider.navigation(startDestination = DESTINATION_ID) {
            moduleName = MODULE_NAME
        } as DynamicGraphNavigator.DynamicNavGraph

        assertWithMessage("ProgressDestination should default to 0")
            .that(graph.progressDestination)
            .isEqualTo(0)
    }

    @Test
    public fun navigationRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            moduleName = MODULE_NAME
            navDestination(DESTINATION_ROUTE) {}
        } as DynamicGraphNavigator.DynamicNavGraph

        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("Module should be set in the graph")
            .that(graph.moduleName)
            .isEqualTo(MODULE_NAME)
    }

    public fun navigation_emptyModuleNameRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
        }
        assertWithMessage("Without a moduleName the graph should be a NavGraph")
            .that(graph !is DynamicGraphNavigator.DynamicNavGraph)
    }

    @Test
    public fun progressDestinationRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            moduleName = MODULE_NAME
            progressDestinationRoute = PROGRESS_DESTINATION_ROUTE
            navDestination(DESTINATION_ROUTE) {}
            navDestination(PROGRESS_DESTINATION_ROUTE) {}
        }

        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ROUTE in graph)
            .isTrue()
        assertWithMessage("ProgressDestination should be added to the graph")
            .that(PROGRESS_DESTINATION_ROUTE in graph)
            .isTrue()
    }

    @Test
    public fun progressDestination_notSetRoute() {
        val graph = provider.navigation(startDestination = DESTINATION_ROUTE) {
            moduleName = MODULE_NAME
        } as DynamicGraphNavigator.DynamicNavGraph

        assertWithMessage("ProgressDestination should default to 0")
            .that(graph.progressDestination)
            .isEqualTo(0)
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "destination"
private const val PROGRESS_DESTINATION_ID = 2
private const val PROGRESS_DESTINATION_ROUTE = "progress_destination"
private const val MODULE_NAME = "myModule"

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
@Suppress("DEPRECATION")
public fun DynamicNavGraphBuilder.navDestination(
    @IdRes id: Int,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
): Unit = destination(NavDestinationBuilder(provider[NoOpNavigator::class], id).apply(builder))

/**
 * Create a base NavDestination. Generally, only subtypes of NavDestination should be
 * added to a NavGraph (hence why this is not in the common-ktx library)
 */
public fun DynamicNavGraphBuilder.navDestination(
    route: String,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
): Unit = destination(NavDestinationBuilder(provider[NoOpNavigator::class], route).apply(builder))