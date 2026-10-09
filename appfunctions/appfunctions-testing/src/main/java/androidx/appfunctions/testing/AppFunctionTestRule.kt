/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.testing

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.testing.internal.FakeAppFunctionInventory
import androidx.appfunctions.testing.internal.FakeAppFunctionManagerApi
import androidx.appfunctions.testing.internal.FakeAppFunctionReader
import kotlin.OptIn
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.shadows.ShadowSystemProperties

/**
 * A JUnit TestRule for setting up an environment to exercise AppFunction APIs in unit or
 * Robolectric tests.
 *
 * Prefer real system-level testing where possible. This rule is intended only for local tests that
 * simulate cross-app interactions via AppFunctions.
 *
 * ### Setup
 *
 * 1. Apply the `appfunctions-compiler` annotation processor to the test configuration:
 *    ```groovy
 *    dependencies {
 *        // ...
 *        kspTest("androidx.appfunctions:appfunctions-compiler:xx.xx.xx")
 *    }
 *    ```
 *
 * 2. Define the service entry point class with `@AppFunctionServiceEntryPoint`. This is either an
 *    existing service in the app under test or a test service created specifically for agent
 *    integration testing. See [androidx.appfunctions.AppFunctionServiceEntryPoint] for set-up
 *    instructions.<br> **Note:** If a service's enabled state changes dynamically via
 *    [android.content.pm.PackageManager.setComponentEnabledSetting], call [reloadAppFunctions] to
 *    reload its functions.
 *
 * ### Example:
 * ```kotlin
 * package com.example.appfunctions
 *
 * class ExampleFunctionsTest {
 *     @get:Rule val appFunctionTestRule = AppFunctionTestRule(context)
 *     private val appFunctionManager = appFunctionTestRule.getAppFunctionManager()
 *
 *     @Test
 *     fun addFunction_returnsCorrectSum() = runBlocking {
 *         val appFunctionName = AppFunctionName(
 *             packageName = "com.example.appfunctions",
 *             functionIdentifier = "com.example.pkg.TestAppFunctionService#addFunction"
 *         )
 *         val appFunctions = appFunctionManager.searchAppFunctions(
 *             AppFunctionSearchSpec(functionNames = setOf(appFunctionName))
 *         )
 *         val addFunctionMetadata = appFunctions.single()
 *
 *         val response = appFunctionManager.executeAppFunction(
 *             ExecuteAppFunctionRequest(
 *                 targetPackageName = context.packageName,
 *                 functionIdentifier = addFunctionMetadata.id,
 *                 functionParameters = AppFunctionData.Builder(
 *                     addFunctionMetadata.parameters,
 *                     addFunctionMetadata.components
 *                 )
 *                     .setLong("a", 2)
 *                     .setLong("b", 3)
 *                     .build()
 *             )
 *         )
 *
 *         // Assert on returned response.
 *     }
 * }
 * ```
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
public class AppFunctionTestRule(private val context: Context) : TestRule {
    // TODO: b/426219836 - Dynamic registration and changing app function enabled state API(s).
    // TODO: b/425327400 - Move to use Robolectric shadows

    private val inventory = FakeAppFunctionInventory(context)
    // TODO(b/426219836): appFunctionReader is internal to set dynamic AppFunctionMetadata manually
    //  in tests. Make it private once dynamic app functions are supported in test rule API.
    internal val appFunctionReader = FakeAppFunctionReader(context, inventory)
    private val appFunctionManagerApi =
        FakeAppFunctionManagerApi(context, appFunctionReader, inventory)

    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            @OptIn(ExperimentalAppFunctionsApi::class)
            override fun evaluate() {
                reloadAppFunctions()
                // Robolectric platform doesn't set these properties, we have checks for certain
                // AppSearch features that are only available if the sdk extensions for T are above
                // 13.
                ShadowSystemProperties.override(T_EXTENSION_PROPERTY_STRING, "13")
                base?.evaluate()
            }
        }

    /**
     * Returns an [AppFunctionManager] instance for interacting with AppFunctions registered via the
     * test rule.
     */
    public fun getAppFunctionManager(): AppFunctionManager {
        return AppFunctionManager(
            context = context,
            appFunctionReader = appFunctionReader,
            appFunctionManagerApi = appFunctionManagerApi,
        )
    }

    /**
     * Triggers AppFunction indexing.
     *
     * Call this function after updating service component state with
     * [android.content.pm.PackageManager.setComponentEnabledSetting] to re-index available
     * functions.
     *
     * Once returned, the AppFunctions indexation is guaranteed to be finished. If any static
     * metadata has changed, active observers of [AppFunctionManager.observeAppFunctions] will be
     * notified of changes. [AppFunctionManager.searchAppFunctions] will return the updated
     * metadata, if queried, and [AppFunctionManager.executeAppFunction] will use the updated
     * metadata.
     *
     * ### Example: Enabling a Disabled Service Component
     *
     * ```kotlin
     * // Target the generated service specified by serviceName in @AppFunctionServiceEntryPoint.
     * val componentName = ComponentName(
     *     context,
     *     "com.example.appfunctions.TestAppFunctionService"
     * )
     * context.packageManager.setComponentEnabledSetting(
     *     componentName,
     *     PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
     *     PackageManager.DONT_KILL_APP
     * )
     * // Re-index to discover updated service functions.
     * appFunctionTestRule.reloadAppFunctions()
     * ```
     */
    @ExperimentalAppFunctionsApi
    public fun reloadAppFunctions() {
        appFunctionReader.reloadAppFunctions()
    }

    private companion object {
        private const val T_EXTENSION_PROPERTY_STRING = "build.version.extensions.t"
    }
}
