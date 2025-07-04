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

import android.os.Build
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.junit.rules.TimeoutRule

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class AppFunctionTestRuleTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule val appFunctionTestRule = AppFunctionTestRule(targetContext)

    @get:Rule val timeoutRule = TimeoutRule(10, TimeUnit.SECONDS)

    private val appFunctionManagerCompat: AppFunctionManagerCompat =
        appFunctionTestRule.getAppFunctionManagerCompat()

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_observeApiNoFilter_returnsAllAppFunctions() =
        runBlocking<Unit> {
            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(AppFunctionSearchSpec())
                    .take(1)
                    .toList()

            assertThat(results.single().single().appFunctions).hasSize(8)
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_observeApi_returnsNewValueOnUpdate() =
        runBlocking<Unit> {
            val functionIdToTest = "androidx.appfunctions.testing.TestFunctions#disabledByDefault"
            val appFunctionSearchFlow =
                appFunctionManagerCompat.observeAppFunctions(
                    AppFunctionSearchSpec(packageNames = setOf(context.packageName))
                )
            val emittedValues =
                appFunctionSearchFlow.shareIn(
                    scope = CoroutineScope(Dispatchers.Default),
                    started = SharingStarted.Eagerly,
                    replay = 10,
                )
            emittedValues.first() // Allow emitting initial value and registering callback.

            // Modify the runtime document.
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )

            // Collect in a separate scope to avoid deadlock within the testcase.
            runBlocking(Dispatchers.Default) { emittedValues.take(2).collect {} }
            assertThat(emittedValues.replayCache).hasSize(2)
            // Assert first result to be default value.
            assertThat(
                    emittedValues.replayCache[0]
                        .flatMap { it.appFunctions }
                        .single { it.id == functionIdToTest }
                        .isEnabled
                )
                .isFalse()
            // Assert next update has updated value.
            assertThat(
                    emittedValues.replayCache[1]
                        .flatMap { it.appFunctions }
                        .single { it.id == functionIdToTest }
                        .isEnabled
                )
                .isTrue()
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_filterBySchemaName_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(
                        AppFunctionSearchSpec(
                            packageNames = setOf(context.packageName),
                            schemaName = "createNote",
                        )
                    )
                    .take(1)
                    .toList()

            assertThat(results.single().flatMap { it.appFunctions }.map { it.id })
                .containsExactly("androidx.appfunctions.testing.NotesFunctions#createNote")
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_filterByPackageName_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(
                        AppFunctionSearchSpec(packageNames = setOf(context.packageName))
                    )
                    .take(1)
                    .toList()

            assertThat(results.single().single().appFunctions).hasSize(8)
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_filterBySchemaCategory_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(
                        AppFunctionSearchSpec(
                            packageNames = setOf(context.packageName),
                            schemaCategory = "myNotes",
                        )
                    )
                    .take(1)
                    .toList()

            assertThat(results.single().flatMap { it.appFunctions }.map { it.id })
                .containsExactly("androidx.appfunctions.testing.NotesFunctions#createNote")
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_filterByMinSchemaVersion_success() =
        runBlocking<Unit> {
            val results =
                appFunctionManagerCompat
                    .observeAppFunctions(
                        AppFunctionSearchSpec(
                            packageNames = setOf(context.packageName),
                            minSchemaVersion = 2,
                        )
                    )
                    .take(1)
                    .toList()

            assertThat(results.single().flatMap { it.appFunctions }.map { it.id })
                .containsExactly("androidx.appfunctions.testing.NotesFunctions#createNote")
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_currentPackage_enabledByDefault_modified_success() =
        runBlocking<Unit> {
            val functionId = "androidx.appfunctions.testing.TestFunctions#enabledByDefault"
            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isTrue()

            appFunctionManagerCompat.setAppFunctionEnabled(
                functionId,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED,
            )

            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isFalse()
        }

    @Test(timeout = 5000)
    fun returnedAppFunctionManagerCompat_currentPackage_disabledByDefault_modified_success() =
        runBlocking<Unit> {
            val functionId = "androidx.appfunctions.testing.TestFunctions#disabledByDefault"
            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isFalse()

            appFunctionManagerCompat.setAppFunctionEnabled(
                functionId,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )

            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isTrue()
        }

    @Test(timeout = 5000)
    fun executeAppFunction_success() =
        runBlocking<Unit> {
            val response =
                appFunctionManagerCompat.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            context.packageName,
                            "androidx.appfunctions.testing.TestFunctions#add",
                            AppFunctionData.Builder("")
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
            val functionId = "androidx.appfunctions.testing.TestFunctions#disabledByDefault"
            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isFalse()

            appFunctionManagerCompat.setAppFunctionEnabled(
                functionId,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED,
            )
            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isTrue()

            appFunctionManagerCompat.setAppFunctionEnabled(
                functionId,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT,
            )
            assertThat(appFunctionManagerCompat.isAppFunctionEnabled(functionId)).isFalse()
        }
}
