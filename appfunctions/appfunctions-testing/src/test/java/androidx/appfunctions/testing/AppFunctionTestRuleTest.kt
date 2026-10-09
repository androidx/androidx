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

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.appfunctions.AppFunctionData.Builder
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.AppFunctionsChangeEvent
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionLongTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata.Companion.SCOPE_ACTIVITY
import androidx.appfunctions.metadata.AppFunctionMetadata.Companion.SCOPE_GLOBAL
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.testing.internal.AppFunctionRuntimeMetadata
import androidx.appfunctions.testing.internal.AppFunctionStaticAndRuntimeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.collections.single
import kotlin.test.assertIs
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.junit.rules.TimeoutRule
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@OptIn(FlowPreview::class, ExperimentalAppFunctionsApi::class)
class AppFunctionTestRuleTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule val appFunctionTestRule = AppFunctionTestRule(targetContext)

    @get:Rule val timeoutRule = TimeoutRule(10, TimeUnit.SECONDS)

    private val appFunctionManager: AppFunctionManager = appFunctionTestRule.getAppFunctionManager()

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_noFilter_returnsAllAppFunctions() =
        runBlocking<Unit> {
            val results = appFunctionManager.searchAppFunctions(AppFunctionSearchSpec())

            assertThat(results).hasSize(TOTAL_FUNCTION_COUNT)
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterBySchemaName_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(context.packageName),
                        schemaName = "createNote",
                    )
                )

            assertThat(results.map { it.id })
                .containsExactly(
                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
                )
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterByPackageName_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(packageNames = setOf(context.packageName))
                )

            assertThat(results).hasSize(TOTAL_FUNCTION_COUNT)
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterBySchemaCategory_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(context.packageName),
                        schemaCategory = "myNotes",
                    )
                )

            assertThat(results.map { it.id })
                .containsExactly(
                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
                )
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterByFunctionName_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(
                        functionNames =
                            setOf(
                                AppFunctionName(
                                    context.packageName,
                                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote",
                                )
                            )
                    )
                )

            assertThat(results.map { it.id })
                .containsExactly(
                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
                )
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterByMinSchemaVersion_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(
                        packageNames = setOf(context.packageName),
                        minSchemaVersion = 2,
                    )
                )

            assertThat(results.map { it.id })
                .containsExactly(
                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
                )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterByGlobalScope_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(scopes = setOf(SCOPE_GLOBAL))
                )

            assertThat(results.map { it.id })
                .contains("androidx.appfunctions.testing.BaseTestAppFunctionService#createNote")
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_searchAppFunctions_filterByActivityScope_success() =
        runBlocking<Unit> {
            val activityScopeFunctionId =
                "androidx.appfunctions.testing.ActivityScopeFunction#activityScopeFunction"
            // TODO(b/426219836): Manually setting metadata with activity scope because registering
            //  dynamic app functions is not yet supported in test rule. Test using API once
            //  supported.
            appFunctionTestRule.appFunctionReader.setAppFunctionStaticAndRuntimeMetadata(
                context.packageName,
                AppFunctionStaticAndRuntimeMetadata(
                    staticMetadata =
                        CompileTimeAppFunctionMetadata(
                            id = activityScopeFunctionId,
                            isEnabledByDefault = true,
                            schema = null,
                            parameters = emptyList(),
                            response =
                                AppFunctionResponseMetadata(
                                    valueType = AppFunctionUnitTypeMetadata(isNullable = false)
                                ),
                            components = AppFunctionComponentsMetadata(),
                            description = "",
                            deprecation = null,
                            scope = SCOPE_ACTIVITY,
                        ),
                    runtimeMetadata =
                        AppFunctionRuntimeMetadata(
                            enabled = AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
                        ),
                ),
            )
            val results =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(scopes = setOf(SCOPE_ACTIVITY))
                )

            assertThat(results.map { it.id }).contains(activityScopeFunctionId)
            assertThat(results.map { it.id })
                .doesNotContain(
                    "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
                )
        }

    @Test(timeout = 5000)
    fun reloadAppFunctions_serviceDisabled_reflectsOnSearchResults() =
        runBlocking<Unit> {
            val functionId = "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
            val functionName = AppFunctionName(context.packageName, functionId)
            val componentName =
                ComponentName(
                    targetContext,
                    Class.forName("androidx.appfunctions.testing.TestAppFunctionService"),
                )

            val initialResults =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(functionNames = setOf(functionName))
                )
            assertThat(initialResults).hasSize(1)

            try {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
                appFunctionTestRule.reloadAppFunctions()

                val resultsAfterDisabled =
                    appFunctionManager.searchAppFunctions(
                        AppFunctionSearchSpec(functionNames = setOf(functionName))
                    )
                assertThat(resultsAfterDisabled).isEmpty()
            } finally {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

    @Test(timeout = 5000)
    fun reloadAppFunctions_serviceEnabled_reflectsOnSearchResults() =
        runBlocking<Unit> {
            val functionId =
                "androidx.appfunctions.testing.BaseDisabledAppFunctionService#disabledFunction"
            val functionName = AppFunctionName(context.packageName, functionId)
            val componentName =
                ComponentName(
                    targetContext,
                    Class.forName("androidx.appfunctions.testing.DisabledAppFunctionService"),
                )

            val initialResults =
                appFunctionManager.searchAppFunctions(
                    AppFunctionSearchSpec(functionNames = setOf(functionName))
                )
            assertThat(initialResults).isEmpty()

            try {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
                appFunctionTestRule.reloadAppFunctions()

                val resultsAfterEnabled =
                    appFunctionManager.searchAppFunctions(
                        AppFunctionSearchSpec(functionNames = setOf(functionName))
                    )
                assertThat(resultsAfterEnabled).hasSize(1)
            } finally {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

    @Test(timeout = 5000)
    fun executeAppFunction_disabledService_functionFailsAfterIndexation() =
        runBlocking<Unit> {
            val functionId = "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
            val componentName =
                ComponentName(
                    targetContext,
                    Class.forName("androidx.appfunctions.testing.TestAppFunctionService"),
                )

            try {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
                appFunctionTestRule.reloadAppFunctions()

                val response =
                    appFunctionManager.executeAppFunction(
                        request =
                            ExecuteAppFunctionRequest(
                                context.packageName,
                                functionId,
                                Builder(emptyList(), AppFunctionComponentsMetadata()).build(),
                            )
                    )

                val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(response)
                assertThat(errorResponse.error)
                    .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
            } finally {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_observeAppFunctions_enabledStateChanged_emitsChange() =
        runBlocking<Unit> {
            val functionIdToTest =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#disabledByDefault"
            val changeEventFlow = appFunctionManager.observeAppFunctions()

            try {
                launch {
                    appFunctionManager.setAppFunctionEnabled(
                        functionIdToTest,
                        AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
                    )
                }

                val event = changeEventFlow.take(1).single()
                assertIs<AppFunctionsChangeEvent.StatesChanged>(event)
                assertThat(event.changedFunctionNames)
                    .containsExactly(AppFunctionName(context.packageName, functionIdToTest))
            } finally {
                appFunctionManager.setAppFunctionEnabled(
                    functionIdToTest,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_observeAppFunctions_serviceDisabled_emitsChange() =
        runBlocking<Unit> {
            val functionIdToTest =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#createNote"
            val componentName =
                ComponentName(
                    targetContext,
                    Class.forName("androidx.appfunctions.testing.TestAppFunctionService"),
                )
            val changeEventFlow = appFunctionManager.observeAppFunctions()

            try {
                launch {
                    targetContext.packageManager.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                    appFunctionTestRule.reloadAppFunctions()
                }

                val event = changeEventFlow.take(1).single()
                assertIs<AppFunctionsChangeEvent.StatesChanged>(event)
                assertThat(event.changedFunctionNames)
                    .contains(AppFunctionName(context.packageName, functionIdToTest))
            } finally {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_observeAppFunctions_serviceEnabled_emitsChange() =
        runBlocking<Unit> {
            val functionIdToTest =
                "androidx.appfunctions.testing.BaseDisabledAppFunctionService#disabledFunction"
            val componentName =
                ComponentName(
                    targetContext,
                    Class.forName("androidx.appfunctions.testing.DisabledAppFunctionService"),
                )
            val changeEventFlow = appFunctionManager.observeAppFunctions()

            try {
                launch {
                    targetContext.packageManager.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                    appFunctionTestRule.reloadAppFunctions()
                }

                val event = changeEventFlow.take(1).single()
                assertIs<AppFunctionsChangeEvent.StatesChanged>(event)
                assertThat(event.changedFunctionNames)
                    .containsExactly(AppFunctionName(context.packageName, functionIdToTest))
            } finally {
                targetContext.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_currentPackage_enabledByDefault_modified_success() =
        runBlocking<Unit> {
            val functionId =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#enabledByDefault"
            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isTrue()

            appFunctionManager.setAppFunctionEnabled(
                functionId,
                AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
            )

            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isFalse()
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_currentPackage_disabledByDefault_modified_success() =
        runBlocking<Unit> {
            val functionId =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#disabledByDefault"
            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isFalse()

            appFunctionManager.setAppFunctionEnabled(
                functionId,
                AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
            )

            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isTrue()
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_getAppFunctionStates_multipleAppFunctionNames_returnsAllStates() =
        runBlocking<Unit> {
            val functionId1 =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#enabledByDefault"
            val functionId2 =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#disabledByDefault"

            val states =
                appFunctionManager.getAppFunctionStates(
                    listOf(
                        AppFunctionName(context.packageName, functionId1),
                        AppFunctionName(context.packageName, functionId2),
                    )
                )

            assertThat(states).hasSize(2)
            assertThat(
                    states
                        .single {
                            it.functionName == AppFunctionName(context.packageName, functionId1)
                        }
                        .isEnabled
                )
                .isTrue()
            assertThat(
                    states
                        .single {
                            it.functionName == AppFunctionName(context.packageName, functionId2)
                        }
                        .isEnabled
                )
                .isFalse()
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_getAppFunctionStates_multipleAppFunctionNames_skipsInvalid() =
        runBlocking<Unit> {
            val validFunctionId =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#enabledByDefault"
            val invalidFunctionId =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#unknown"

            val states =
                appFunctionManager.getAppFunctionStates(
                    listOf(
                        AppFunctionName(context.packageName, validFunctionId),
                        AppFunctionName(context.packageName, invalidFunctionId),
                    )
                )

            assertThat(states).hasSize(1)
            assertThat(states.single().functionName)
                .isEqualTo(AppFunctionName(context.packageName, validFunctionId))
            assertThat(states.single().isEnabled).isTrue()
        }

    @Test(timeout = 5000)
    fun executeAppFunction_success() =
        runBlocking<Unit> {
            val response =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            context.packageName,
                            "androidx.appfunctions.testing.BaseTestAppFunctionService#add",
                            Builder(
                                    listOf(
                                        AppFunctionParameterMetadata(
                                            name = "num1",
                                            isRequired = true,
                                            dataType =
                                                AppFunctionLongTypeMetadata(isNullable = false),
                                        ),
                                        AppFunctionParameterMetadata(
                                            name = "num2",
                                            isRequired = true,
                                            dataType =
                                                AppFunctionLongTypeMetadata(isNullable = false),
                                        ),
                                    ),
                                    AppFunctionComponentsMetadata(),
                                )
                                .setLong("num1", 1)
                                .setLong("num2", 2)
                                .build(),
                        )
                )

            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
            assertThat(
                    successResponse.returnValue.getLong(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                )
                .isEqualTo(3)
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_currentPackage_disabledByDefault_modifiedAndRestoredToDefault_success() =
        runBlocking<Unit> {
            val functionId =
                "androidx.appfunctions.testing.BaseTestAppFunctionService#disabledByDefault"
            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isFalse()

            appFunctionManager.setAppFunctionEnabled(
                functionId,
                AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
            )
            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isTrue()

            appFunctionManager.setAppFunctionEnabled(
                functionId,
                AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
            )
            assertThat(
                    appFunctionManager
                        .getAppFunctionStates(
                            listOf(AppFunctionName(context.packageName, functionId))
                        )
                        .single()
                        .isEnabled
                )
                .isFalse()
        }

    @Test(timeout = 5000)
    fun apply_correctlyOverridesTExtensionPropertyForTestBody() {
        val rule = AppFunctionTestRule(targetContext)
        // The class-level rule already set the property for this test method; reset it to a
        // sentinel
        // so this assertion observes the fresh rule's effect, not the outer rule's.
        ShadowSystemProperties.override(T_EXTENSION_PROPERTY, SENTINEL_T_EXTENSION_PROPERTY)
        val propertyBeforeApply = readTExtensionProperty()
        var propertyDuringTest: String? = null
        val statement =
            object : Statement() {
                override fun evaluate() {
                    propertyDuringTest = readTExtensionProperty()
                }
            }

        rule.apply(statement, Description.EMPTY).evaluate()

        assertThat(propertyBeforeApply).isEqualTo(SENTINEL_T_EXTENSION_PROPERTY)
        assertThat(propertyDuringTest).isEqualTo("13")
    }

    /**
     * Reads the T-extension system property. [android.os.SystemProperties] is a hidden API, so it
     * is read reflectively to let the Robolectric shadow intercept the call and return the
     * override.
     */
    private fun readTExtensionProperty(): String {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get = systemProperties.getMethod("get", String::class.java, String::class.java)
        return get.invoke(null, T_EXTENSION_PROPERTY, "") as String
    }

    private companion object {
        const val T_EXTENSION_PROPERTY = "build.version.extensions.t"
        // A value other than "13" so a reversed set/evaluate order is observable.
        const val SENTINEL_T_EXTENSION_PROPERTY = "0"

        val TOTAL_FUNCTION_COUNT = 8
    }
}
