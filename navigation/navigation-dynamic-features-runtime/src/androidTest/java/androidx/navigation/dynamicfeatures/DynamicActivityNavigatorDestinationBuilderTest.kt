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

import android.content.ComponentName
import android.content.Context
import androidx.navigation.ExperimentalSafeArgsApi
import androidx.navigation.NavController
import androidx.navigation.plusAssign
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
public class DynamicActivityNavigatorDestinationBuilderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val navController =
        NavController(context).also { controller ->
            val installManager = DynamicInstallManager(
                context,
                SplitInstallManagerFactory.create(context)
            )
            val navigatorProvider = controller.navigatorProvider
            navigatorProvider += DynamicActivityNavigator(
                context,
                installManager
            )
        }

    @Suppress("DEPRECATION")
    @Test
    public fun module() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                moduleName = MODULE_NAME
            }
        }

        assertThat(
            (graph.findNode(DESTINATION_ID) as DynamicActivityNavigator.Destination).moduleName
        )
            .isEqualTo(MODULE_NAME)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun noModule() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
            }
        }
        assertThat(
            (graph.findNode(DESTINATION_ID) as DynamicActivityNavigator.Destination).moduleName
        ).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    public fun activity() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
            }
        }
        val destination = graph.findNode(DESTINATION_ID) as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isEqualTo(
            ComponentName(
                context,
                CLASS_NAME
            )
        )
    }

    @Suppress("DEPRECATION")
    @Test
    public fun noActivity() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
            }
        }
        val destination = graph.findNode(DESTINATION_ID) as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    public fun modulePackage() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            activity(DESTINATION_ID) {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
                targetPackage = PACKAGE_NAME
            }
        }
        val destination = graph.findNode(DESTINATION_ID) as DynamicActivityNavigator.Destination
        assertThat(destination.component).isEqualTo(ComponentName(PACKAGE_NAME, CLASS_NAME))
    }

    @Test
    public fun moduleRoute() {
        val graph = navController.createGraph(startDestination = DESTINATION_ROUTE) {
            activity(DESTINATION_ROUTE) {
                moduleName = MODULE_NAME
            }
        }

        assertThat(
            (graph.findNode(DESTINATION_ROUTE) as DynamicActivityNavigator.Destination).moduleName
        )
            .isEqualTo(MODULE_NAME)
    }

    @Test
    public fun noModuleRoute() {
        val graph = navController.createGraph(startDestination = DESTINATION_ROUTE) {
            activity(DESTINATION_ROUTE) {
            }
        }
        assertThat(
            (graph.findNode(DESTINATION_ROUTE) as DynamicActivityNavigator.Destination).moduleName
        ).isNull()
    }

    @Test
    public fun activityRoute() {
        val graph = navController.createGraph(startDestination = DESTINATION_ROUTE) {
            activity(DESTINATION_ROUTE) {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
            }
        }
        val destination = graph.findNode(DESTINATION_ROUTE) as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isEqualTo(
            ComponentName(
                context,
                CLASS_NAME
            )
        )
    }

    @Test
    public fun noActivityRoute() {
        val graph = navController.createGraph(startDestination = DESTINATION_ROUTE) {
            activity(DESTINATION_ROUTE) {
            }
        }
        val destination = graph.findNode(DESTINATION_ROUTE) as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isNull()
    }

    @Test
    public fun modulePackageRoute() {
        val graph = navController.createGraph(startDestination = DESTINATION_ROUTE) {
            activity(DESTINATION_ROUTE) {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
                targetPackage = PACKAGE_NAME
            }
        }
        val destination = graph.findNode(DESTINATION_ROUTE) as DynamicActivityNavigator.Destination
        assertThat(destination.component).isEqualTo(ComponentName(PACKAGE_NAME, CLASS_NAME))
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun moduleKClass() {
        val graph = navController.createGraph(startDestination = TestClass::class) {
            activity<TestClass> {
                moduleName = MODULE_NAME
            }
        }

        assertThat(
            (graph.findNode<TestClass>() as DynamicActivityNavigator.Destination).moduleName
        )
            .isEqualTo(MODULE_NAME)
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun noModuleKClass() {
        val graph = navController.createGraph(startDestination = TestClass::class) {
            activity<TestClass> {
            }
        }
        assertThat(
            (graph.findNode<TestClass>() as DynamicActivityNavigator.Destination).moduleName
        ).isNull()
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun activityKClass() {
        val graph = navController.createGraph(startDestination = TestClass::class) {
            activity<TestClass> {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
            }
        }
        val destination = graph.findNode<TestClass>() as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isEqualTo(
            ComponentName(
                context,
                CLASS_NAME
            )
        )
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun noActivityKClass() {
        val graph = navController.createGraph(startDestination = TestClass::class) {
            activity<TestClass> {
            }
        }
        val destination = graph.findNode<TestClass>() as DynamicActivityNavigator.Destination
        assertThat(
            destination.component
        ).isNull()
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun modulePackageKClass() {
        val graph = navController.createGraph(startDestination = TestClass::class) {
            activity<TestClass> {
                moduleName = MODULE_NAME
                activityClassName = CLASS_NAME
                targetPackage = PACKAGE_NAME
            }
        }
        val destination = graph.findNode<TestClass>() as DynamicActivityNavigator.Destination
        assertThat(destination.component).isEqualTo(ComponentName(PACKAGE_NAME, CLASS_NAME))
    }

    @OptIn(ExperimentalSafeArgsApi::class)
    @Test
    public fun moduleObject() {
        @Serializable
        class TestClass(val arg: Int)
        val graph = navController.createGraph(startDestination = TestClass(0)) {
            activity<TestClass> {
                moduleName = MODULE_NAME
            }
        }

        val dest = graph.findNode<TestClass>() as DynamicActivityNavigator.Destination
        assertThat(dest.moduleName).isEqualTo(MODULE_NAME)
        assertThat(dest.arguments["arg"]).isNotNull()
    }
}

private const val CLASS_NAME = "com.example.DynamicDestination"
private const val PACKAGE_NAME = "com.example.myPackage"
private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "route"
private const val MODULE_NAME = "myModule"
