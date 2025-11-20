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
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.internal.NullTranslatorSelector
import androidx.appfunctions.testing.internal.FakeAppFunctionManagerApi
import androidx.appfunctions.testing.internal.FakeAppFunctionReader
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
 * Any functions annotated with [androidx.appfunctions.service.AppFunction] in test code will be
 * automatically registered in this environment during initialization, provided the
 * `appfunctions-compiler` is applied to the test configuration with the
 * `appfunctions:aggregateAppFunctions` compiler option set to true.
 *
 * ### Example Gradle Setup
 *
 * ```
 * dependencies {
 *     ...
 *     kspTest("androidx.appfunctions:appfunctions-compiler:xx.xx.xx")
 * }
 *
 * ksp {
 *     arg("appfunctions:aggregateAppFunctions", "true")
 * }
 * ```
 *
 * ### Example: Testing App Functions in the Same App
 *
 * ```
 * package com.example.appfunctions
 *
 * // Sample functions under test.
 * class ExampleFunctions {
 *     @AppFunction
 *     suspend fun add(a: Int, b: Int): Int = a + b
 * }
 *
 * // Test file.
 * class ExampleFunctionsTest {
 *     @get:Rule val appFunctionTestRule = AppFunctionTestRule(context)
 *     private val appFunctionManagerCompat = appFunctionTestRule.getAppFunctionManagerCompat()
 *
 *     @Test
 *     fun addFunction_returnsCorrectSum() = runBlocking {
 *         val appFunctionPackageMetadata = appFunctionManagerCompat.observeAppFunctions(
 *                 AppFunctionSearchSpec(
 *                     packageNames = listOf(packageName),
 *                     schemaName = schemaName
 *                 )
 *             )
 *             .first()
 *             .single()
 *         val addFunctionMetadata = appFunctionPackageMetadata.appFunctions.single()
 *
 *         val response = appFunctionManagerCompat.executeAppFunction(
 *             ExecuteAppFunctionRequest(...)
 *         )
 *
 *         // assert on returned response.
 *     }
 * }
 * ```
 *
 * ### Example: Testing App Function Execution in an Agent
 *
 * ```
 * package com.example.agent.appfunctions
 *
 * class AppFunctionsAgent(
 *     private val appFunctionManagerCompat: AppFunctionManagerCompat
 * ) {
 *     suspend fun executeAppFunction(
 *         packageName: String,
 *         schemaName: String,
 *         params: Any
 *     ): AppFunctionData {
 *         val appFunctionPackageMetadata = appFunctionManagerCompat
 *             .observeAppFunctions(
 *                 AppFunctionSearchSpec(
 *                     packageNames = listOf(packageName),
 *                     schemaName = schemaName
 *                 )
 *             )
 *             .first()
 *             .single()
 *
 *         val appFunctionMetadata = appFunctionPackageMetadata.appFunctions.single()
 *
 *         val request = ExecuteAppFunctionRequest(...)
 *
 *         val response = appFunctionManagerCompat.executeAppFunction(request)
 *
 *
 *        // return response.
 *     }
 * }
 *
 * // Test file.
 * class TestFunctions {
 *     @AppFunction
 *     fun testFun(parameters: TestParam): TestReturn { ... }
 * }
 *
 * class AppFunctionsAgentTest {
 *     @get:Rule val appFunctionTestRule = AppFunctionTestRule(context)
 *     private val appFunctionsAgent = AppFunctionsAgent(
 *         appFunctionTestRule.getAppFunctionManagerCompat()
 *     )
 *
 *     @Test
 *     fun testFun_returnsExpectedResult() = runBlocking {
 *         val response = appFunctionsAgent.executeAppFunction(
 *             context.packageName,
 *             TestFunctionsIds.TEST_FUN_ID,
 *             TestParam()
 *         )
 *
 *         // assert on response.
 *     }
 * }
 * ```
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class AppFunctionTestRule(private val context: Context) : TestRule {
    // TODO: b/426219836 - Dynamic registration and changing app function enabled state API(s).
    // TODO: b/425327400 - Move to use Robolectric shadows

    private val appFunctionReader = FakeAppFunctionReader(context)
    private val appFunctionManagerApi = FakeAppFunctionManagerApi(context, appFunctionReader)

    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            override fun evaluate() {
                base?.evaluate()
                // Robolectric platform doesn't set these properties, we have checks for certain
                // AppSearch features that are only available if the sdk extensions for T are above
                // 13.
                ShadowSystemProperties.override(T_EXTENSION_PROPERTY_STRING, "13")
            }
        }

    /**
     * Returns an [AppFunctionManagerCompat] instance for interacting with AppFunctions registered
     * via the test rule.
     */
    public fun getAppFunctionManagerCompat(): AppFunctionManagerCompat {
        return AppFunctionManagerCompat(
            context = context,
            appFunctionReader = appFunctionReader,
            appFunctionManagerApi = appFunctionManagerApi,
            translatorSelector = NullTranslatorSelector(),
        )
    }

    private companion object {
        private const val T_EXTENSION_PROPERTY_STRING = "build.version.extensions.t"
    }
}
