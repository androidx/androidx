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

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NoOpNavigator
import androidx.navigation.dynamicfeatures.shared.AndroidTestDynamicInstallManager
import androidx.navigation.get
import androidx.navigation.plusAssign
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class DynamicIncludeNavGraphBuilderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val navController = NavController(context).apply {
        navigatorProvider += DynamicIncludeGraphNavigator(
            context, navigatorProvider, navInflater,
            AndroidTestDynamicInstallManager(context)
        )
        navigatorProvider += NoOpNavigator()
    }

    @Suppress("DEPRECATION")
    @Test
    public fun includeDynamic() {
        val graph = navController.navigatorProvider.navigation(startDestination = GRAPH_ID) {
            includeDynamic(GRAPH_ID, MODULE_NAME, GRAPH_RESOURCE_NAME) {
                graphPackage = GRAPH_PACKAGE
            }
        }
        val includeDynamic = graph[GRAPH_ID] as DynamicIncludeGraphNavigator.DynamicIncludeNavGraph
        assertWithMessage("Module should be set in the graph")
            .that(includeDynamic.moduleName)
            .isEqualTo(MODULE_NAME)

        assertWithMessage("graphPackage has to be set")
            .that(includeDynamic.graphPackage)
            .isEqualTo(GRAPH_PACKAGE)

        assertWithMessage("graphResourceName has to be set")
            .that(includeDynamic.graphResourceName)
            .isEqualTo(GRAPH_RESOURCE_NAME)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun includeDynamic_emptyModuleName() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ID) {
            try {
                includeDynamic(GRAPH_ID, "", GRAPH_RESOURCE_NAME)
                fail("includeDynamic should fail with an empty module name")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Module name cannot be empty")
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun includeDynamic_graphPackage_null() {
        val graph = navController.navigatorProvider.navigation(startDestination = GRAPH_ID) {
            includeDynamic(GRAPH_ID, MODULE_NAME, GRAPH_RESOURCE_NAME)
        }
        val includeDynamic = graph[GRAPH_ID] as DynamicIncludeGraphNavigator.DynamicIncludeNavGraph

        assertWithMessage("graphPackage should be filled in from package name and module name")
            .that(includeDynamic.graphPackage).isEqualTo("${context.packageName}.$MODULE_NAME")
    }

    @Suppress("DEPRECATION")
    @Test
    public fun includeDynamic_graphPackage_empty() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ID) {
            try {
                includeDynamic(GRAPH_ID, MODULE_NAME, GRAPH_RESOURCE_NAME) {
                    graphPackage = ""
                }
                fail("includeDynamic should fail with an empty graph package")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Graph package name cannot be empty")
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun includeDynamic_graphResourceName_empty() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ID) {
            try {
                includeDynamic(GRAPH_ID, MODULE_NAME, "")
                fail("includeDynamic should fail with an empty graph resource name")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Graph resource name cannot be empty")
            }
        }
    }

    @Test
    public fun includeDynamicRoute() {
        val graph = navController.navigatorProvider.navigation(startDestination = GRAPH_ROUTE) {
            includeDynamic(GRAPH_ROUTE, MODULE_NAME, GRAPH_RESOURCE_NAME) {
                graphPackage = GRAPH_PACKAGE
            }
        }
        val includeDynamic = graph[GRAPH_ROUTE]
            as DynamicIncludeGraphNavigator.DynamicIncludeNavGraph
        assertWithMessage("Module should be set in the graph")
            .that(includeDynamic.moduleName)
            .isEqualTo(MODULE_NAME)

        assertWithMessage("graphPackage has to be set")
            .that(includeDynamic.graphPackage)
            .isEqualTo(GRAPH_PACKAGE)

        assertWithMessage("graphResourceName has to be set")
            .that(includeDynamic.graphResourceName)
            .isEqualTo(GRAPH_RESOURCE_NAME)
    }

    public fun includeDynamic_emptyModuleNameRoute() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ROUTE) {
            try {
                includeDynamic(GRAPH_ROUTE, "", GRAPH_RESOURCE_NAME)
                fail("includeDynamic should fail with an empty module name")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Module name cannot be empty")
            }
        }
    }

    @Test
    public fun includeDynamic_graphPackage_nullRoute() {
        val graph = navController.navigatorProvider.navigation(startDestination = GRAPH_ROUTE) {
            includeDynamic(GRAPH_ROUTE, MODULE_NAME, GRAPH_RESOURCE_NAME)
        }
        val includeDynamic = graph[GRAPH_ROUTE]
            as DynamicIncludeGraphNavigator.DynamicIncludeNavGraph

        assertWithMessage("graphPackage should be filled in from package name and module name")
            .that(includeDynamic.graphPackage).isEqualTo("${context.packageName}.$MODULE_NAME")
    }

    @Test
    public fun includeDynamic_graphPackage_emptyRoute() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ROUTE) {
            try {
                includeDynamic(GRAPH_ROUTE, MODULE_NAME, GRAPH_RESOURCE_NAME) {
                    graphPackage = ""
                }
                fail("includeDynamic should fail with an empty graph package")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Graph package name cannot be empty")
            }
        }
    }

    @Test
    public fun includeDynamic_graphResourceName_emptyRoute() {
        navController.navigatorProvider.navigation(startDestination = GRAPH_ROUTE) {
            try {
                includeDynamic(GRAPH_ROUTE, MODULE_NAME, "")
                fail("includeDynamic should fail with an empty graph resource name")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessageThat().isEqualTo("Graph resource name cannot be empty")
            }
        }
    }
}

private const val GRAPH_ID = 1
private const val GRAPH_ROUTE = "graph"
private const val MODULE_NAME = "myModule"
private const val GRAPH_PACKAGE = "com.example.mypackage"
private const val GRAPH_RESOURCE_NAME = "graphName"