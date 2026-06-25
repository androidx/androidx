/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.integration.test.agent

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.startService
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
@LargeTest
class DynamicRegistrationIntegrationTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var appFunctionManager: AppFunctionManager
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val targetAppApkFile =
        InstrumentationRegistry.getArguments().getString("TARGET_APP_APK")
            ?: throw IllegalStateException("TARGET_APP_APK argument not found")

    @Before
    fun setup() = doBlocking {
        uiAutomation.grantAppFunctionAccess(targetContext, TARGET_APP_PACKAGE)

        appFunctionManager = checkNotNull(AppFunctionManager.getInstance(targetContext))

        uiAutomation.apply {
            adoptShellPermissionIdentity(
                Manifest.permission.INSTALL_PACKAGES,
                Manifest.permission.EXECUTE_APP_FUNCTIONS,
            )
        }
        InstallHelper.install(targetAppApkFile)
        targetContext.awaitAppFunctionsIndexed(TARGET_APP_PACKAGE)
    }

    @After
    fun tearDown() {
        uiAutomation.revokeAppFunctionAccess()
        InstallHelper.uninstall(TARGET_APP_PACKAGE)
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun executeAppFunction_dynamicCallbackRegistration_success() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_CALLBACK,
            targetFunctionId = GLOBAL_SIGNATURE_FORMAT_MESSAGE,
        ) {
            val metadata = findAppFunctionMetadata(GLOBAL_SIGNATURE_FORMAT_MESSAGE)
            val dynamicResponse =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            GLOBAL_SIGNATURE_FORMAT_MESSAGE,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setInt("a", 42)
                                .setString("b", "hello")
                                .build(),
                        )
                )

            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(dynamicResponse)
            assertThat(
                    successResponse.returnValue.getString(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                )
                .isEqualTo("callback_result_42_hello")
        }
    }

    @Test
    fun executeAppFunction_dynamicCallbackRegistration_errorThrown() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_CALLBACK_THROWS,
            targetFunctionId = GLOBAL_SIGNATURE_FORMAT_MESSAGE,
        ) {
            val metadata = findAppFunctionMetadata(GLOBAL_SIGNATURE_FORMAT_MESSAGE)
            val dynamicResponse =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            GLOBAL_SIGNATURE_FORMAT_MESSAGE,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setInt("a", 42)
                                .setString("b", "hello")
                                .build(),
                        )
                )

            val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(dynamicResponse)
            assertIs<AppFunctionAppUnknownException>(errorResponse.error)
            assertThat(errorResponse.error.errorMessage)
                .contains("Simulated error in callback execution")
        }
    }

    @Test
    fun executeAppFunction_dynamicCallbackRegistration_appFunctionExceptionThrown() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION,
            targetFunctionId = GLOBAL_SIGNATURE_FORMAT_MESSAGE,
        ) {
            val metadata = findAppFunctionMetadata(GLOBAL_SIGNATURE_FORMAT_MESSAGE)
            val dynamicResponse =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            GLOBAL_SIGNATURE_FORMAT_MESSAGE,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setInt("a", 42)
                                .setString("b", "hello")
                                .build(),
                        )
                )

            val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(dynamicResponse)
            val appFunctionException =
                assertIs<AppFunctionInvalidArgumentException>(errorResponse.error)
            assertThat(appFunctionException.errorMessage).contains("Simulated AppFunctionException")
        }
    }

    @Test
    fun executeAppFunction_dynamicCallbackRegistration_unregisterDuringExecution() = doBlocking {
        // 1. Start service to trigger dynamic registration inside testapp
        uiAutomation.startService(
            TARGET_APP_PACKAGE,
            DYNAMIC_REGISTRATION_SERVICE,
            ACTION_REGISTER_LONG_RUNNING,
        )

        try {
            retryAssert {
                val isEnabled =
                    appFunctionManager.isAppFunctionEnabled(
                        TARGET_APP_PACKAGE,
                        GLOBAL_SIGNATURE_FORMAT_MESSAGE,
                    )
                assertThat(isEnabled).isTrue()
            }

            // 2. Execute dynamically registered signature app function
            val metadata = findAppFunctionMetadata(GLOBAL_SIGNATURE_FORMAT_MESSAGE)
            assertThat(metadata).isNotNull()

            val request =
                ExecuteAppFunctionRequest(
                    TARGET_APP_PACKAGE,
                    GLOBAL_SIGNATURE_FORMAT_MESSAGE,
                    AppFunctionData.Builder(metadata.parameters, metadata.components)
                        .setInt("a", 42)
                        .setString("b", "hello")
                        .build(),
                )

            // Start execution asynchronously
            val deferredResponse = async { appFunctionManager.executeAppFunction(request) }

            // Wait 1 second to ensure the execution has started in the target service
            delay(1000)

            // Unregister during execution
            uiAutomation.startService(
                TARGET_APP_PACKAGE,
                DYNAMIC_REGISTRATION_SERVICE,
                ACTION_UNREGISTER_CALLBACK,
            )

            // Await the response
            val response = deferredResponse.await()

            // Verify the result
            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
            assertThat(
                    successResponse.returnValue.getString(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                )
                .isEqualTo("long_running_result")
        } finally {
            // Cleanup: Unregister
            uiAutomation.startService(
                TARGET_APP_PACKAGE,
                DYNAMIC_REGISTRATION_SERVICE,
                ACTION_UNREGISTER_CALLBACK,
            )
        }
    }

    private suspend fun runWithDynamicAppFunctionRegistered(
        registerAction: String,
        targetFunctionId: String,
        block: suspend () -> Unit,
    ) {
        // 1. Start service to trigger dynamic registration inside testapp
        uiAutomation.startService(TARGET_APP_PACKAGE, DYNAMIC_REGISTRATION_SERVICE, registerAction)

        try {
            // 2. Wait for the app function to be indexed and enabled
            retryAssert {
                val isEnabled =
                    appFunctionManager.isAppFunctionEnabled(TARGET_APP_PACKAGE, targetFunctionId)
                assertThat(isEnabled).isTrue()
            }

            block()
        } finally {
            // 3. Cleanup: Unregister
            uiAutomation.startService(
                TARGET_APP_PACKAGE,
                DYNAMIC_REGISTRATION_SERVICE,
                ACTION_UNREGISTER_CALLBACK,
            )
        }
    }

    private suspend fun findAppFunctionMetadata(id: String): AppFunctionMetadata {
        return appFunctionManager
            .observeAppFunctions(AppFunctionSearchSpec())
            .first()
            .flatMap { it.appFunctions }
            .single { it.id == id }
    }

    private suspend fun Context.awaitAppFunctionsIndexed(targetPackage: String) {
        retryAssert {
            val functionIds =
                AppSearchMetadataHelper.collectFunctionIds(
                    this@awaitAppFunctionsIndexed,
                    targetPackage,
                )
            assertThat(functionIds).isNotEmpty()
        }
    }

    private companion object {
        const val TARGET_APP_PACKAGE = "androidx.appfunctions.integration.testapp"
        const val DYNAMIC_REGISTRATION_SERVICE =
            "androidx.appfunctions.integration.testapp.DynamicRegistrationService"
        const val GLOBAL_SIGNATURE_FORMAT_MESSAGE =
            "androidx.appfunctions.integration.testapp.FormatMessageSignature#formatMessage"

        const val ACTION_REGISTER_CALLBACK =
            "androidx.appfunctions.integration.action.REGISTER_CALLBACK"
        const val ACTION_UNREGISTER_CALLBACK =
            "androidx.appfunctions.integration.action.UNREGISTER_CALLBACK"
        const val ACTION_REGISTER_CALLBACK_THROWS =
            "androidx.appfunctions.integration.action.REGISTER_CALLBACK_THROWS"
        const val ACTION_REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION =
            "androidx.appfunctions.integration.action.REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION"

        const val ACTION_REGISTER_LONG_RUNNING =
            "androidx.appfunctions.integration.action.REGISTER_LONG_RUNNING"
    }
}
