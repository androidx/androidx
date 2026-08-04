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

package androidx.appfunctions

import android.Manifest
import android.app.UiAutomation
import android.content.Context
import android.os.Build
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionManagerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private val appFunctionManager: AppFunctionManager by lazy {
        checkNotNull(AppFunctionManager.getInstance(context))
    }

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    private val resetFunctionIds =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.MEDIA_SCHEMA2_PRINT,
            AppFunctionMetadataTestHelper.FunctionIds.NOTES_SCHEMA_PRINT,
        )

    @Before
    fun setup() {
        assumeNotNull(AppFunctionManager.getInstance(context))

        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.INSTALL_PACKAGES,
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )

        runBlocking {
            metadataTestHelper.awaitAppFunctionIndexed(resetFunctionIds)

            // Reset all test ids
            for (functionIds in resetFunctionIds) {
                appFunctionManager.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManager.Companion.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
        uiAutomation.executeShellCommand("pm uninstall $ADDITIONAL_APP_PACKAGE")
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManager.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSelfIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManager.isAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultEnabledState() {
        val isEnabled = runBlocking {
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testIsAppFunctionEnabled_defaultDisabledState() {
        val isEnabled = runBlocking {
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToDisable() {
        val isEnabled = runBlocking {
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
            )
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testSetAppFunctionEnabled_overrideToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
            )
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToEnabled() {
        val isEnabled = runBlocking {
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
            )
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
            )
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun testSetAppFunctionEnabled_resetToDisabled() {
        val isEnabled = runBlocking {
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
            )
            appFunctionManager.setAppFunctionEnabled(
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
            )
            appFunctionManager.isAppFunctionEnabled(
                context.packageName,
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            )
        }

        assertThat(isEnabled).isFalse()
    }

    @Test
    fun testExecuteAppFunction_functionNotExist() {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = "fakeFunctionId",
                functionParameters = AppFunctionData.EMPTY,
            )

        val response = runBlocking { appFunctionManager.executeAppFunction(request) }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        assertThat((response as ExecuteAppFunctionResponse.Error).error)
            .isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
    }

    @Test
    fun testExecuteAppFunction_functionSucceed() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            val request =
                ExecuteAppFunctionRequest(
                    targetPackageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                    functionParameters = AppFunctionData.EMPTY,
                )

            val response = appFunctionManager.executeAppFunction(request)

            assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
            assertThat(
                    (response as ExecuteAppFunctionResponse.Success)
                        .returnValue
                        .getString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE)
                )
                .isEqualTo("result")
        }

    @Test
    fun testExecuteAppFunction_functionFail() =
        runBlocking<Unit> {
            assumeTrue(metadataTestHelper.isDynamicIndexerAvailable())
            val request =
                ExecuteAppFunctionRequest(
                    targetPackageName = context.packageName,
                    functionIdentifier =
                        AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                    functionParameters = AppFunctionData.EMPTY,
                )

            val response = appFunctionManager.executeAppFunction(request)

            assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
            assertThat((response as ExecuteAppFunctionResponse.Error).error)
                .isInstanceOf(AppFunctionInvalidArgumentException::class.java)
        }

    private companion object {
        const val ADDITIONAL_APP_PACKAGE = "com.google.android.app.notes"
    }
}
