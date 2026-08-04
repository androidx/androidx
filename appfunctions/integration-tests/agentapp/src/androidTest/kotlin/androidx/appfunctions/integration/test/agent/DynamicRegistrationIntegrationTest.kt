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
import androidx.appfunctions.integration.test.agent.TestUtil.assertAppFunctionEnabledState
import androidx.appfunctions.integration.test.agent.TestUtil.doBlocking
import androidx.appfunctions.integration.test.agent.TestUtil.grantAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.retryAssert
import androidx.appfunctions.integration.test.agent.TestUtil.revokeAppFunctionAccess
import androidx.appfunctions.integration.test.agent.TestUtil.startService
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertThrows
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
    fun executeAppFunction_suspendImplementation_success() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_SUSPEND_FORMAT_MESSAGE,
            targetFunctionId = GLOBAL_SIGNATURE_FORMAT_MESSAGE,
        ) {
            val metadata = findAppFunctionMetadata(GLOBAL_SIGNATURE_FORMAT_MESSAGE)
            val response =
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

            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
            assertThat(
                    successResponse.returnValue.getString(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                )
                .isEqualTo("suspend_result_42_hello")
        }
    }

    @Test
    fun executeAppFunction_adapterAllPrimitivesImplementation_success() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_ADAPTER_ALL_PRIMITIVES,
            targetFunctionId = DYNAMIC_ALL_PRIMITIVES_INPUTS_SIGNATURE_ID,
        ) {
            val metadata = findAppFunctionMetadata(DYNAMIC_ALL_PRIMITIVES_INPUTS_SIGNATURE_ID)

            val response =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            DYNAMIC_ALL_PRIMITIVES_INPUTS_SIGNATURE_ID,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setInt("intValue", 42)
                                .setLong("longValue", 100L)
                                .setFloat("floatValue", 3.14f)
                                .setDouble("doubleValue", 2.718)
                                .setBoolean("booleanValue", true)
                                .setString("stringValue", "hello")
                                .setIntArray("intArrayValue", intArrayOf(1, 2))
                                .setLongArray("longArrayValue", longArrayOf(10L, 20L))
                                .setFloatArray("floatArrayValue", floatArrayOf(1.1f, 2.2f))
                                .setDoubleArray("doubleArrayValue", doubleArrayOf(3.3, 4.4))
                                .setBooleanArray("booleanArrayValue", booleanArrayOf(true, false))
                                .setByteArray("byteArrayValue", byteArrayOf(5, 6))
                                .setStringList("stringListValue", listOf("a", "b"))
                                .build(),
                        )
                )

            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
            assertThat(
                    successResponse.returnValue.getBoolean(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun executeAppFunction_adapterComplexSerializableImplementation_success() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_ADAPTER_COMPLEX_SERIALIZABLE,
            targetFunctionId = DYNAMIC_COMPLEX_SERIALIZABLE_SIGNATURE_ID,
        ) {
            val metadata = findAppFunctionMetadata(DYNAMIC_COMPLEX_SERIALIZABLE_SIGNATURE_ID)

            val innerObjectType =
                checkNotNull(
                    metadata.components.dataTypes[
                            "androidx.appfunctions.integration.testapp.InnerComplexData"]
                        as? androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
                )
            val outerObjectType =
                checkNotNull(
                    metadata.components.dataTypes[
                            "androidx.appfunctions.integration.testapp.OuterComplexData"]
                        as? androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
                )

            val innerData =
                AppFunctionData.Builder(innerObjectType, metadata.components)
                    .setString("id", "inner1")
                    .setIntArray("scores", intArrayOf(10, 20))
                    .setString("optionalTag", "tag1")
                    .build()

            val innerData2 =
                AppFunctionData.Builder(innerObjectType, metadata.components)
                    .setString("id", "inner2")
                    .setIntArray("scores", intArrayOf(30, 40))
                    .build()

            val outerData =
                AppFunctionData.Builder(outerObjectType, metadata.components)
                    .setString("title", "outerTitle")
                    .setAppFunctionData("primaryInner", innerData)
                    .setAppFunctionDataList("innerList", listOf(innerData, innerData2))
                    .setString("optionalMetadata", "meta")
                    .build()

            val response =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            DYNAMIC_COMPLEX_SERIALIZABLE_SIGNATURE_ID,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setAppFunctionData("input", outerData)
                                .build(),
                        )
                )

            val successResponse = assertIs<ExecuteAppFunctionResponse.Success>(response)
            val outputData =
                successResponse.returnValue.getAppFunctionData(
                    ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                )
            assertThat(outputData).isNotNull()
            assertThat(outputData!!.getString("title")).isEqualTo("echo_outerTitle")
            val primaryInnerResult = outputData.getAppFunctionData("primaryInner")
            assertThat(primaryInnerResult).isNotNull()
            assertThat(primaryInnerResult!!.getString("id")).isEqualTo("echo_inner1")
            assertThat(primaryInnerResult.getIntArray("scores")).isEqualTo(intArrayOf(10, 20))
            assertThat(primaryInnerResult.getString("optionalTag")).isEqualTo("tag1")

            val innerListResult = outputData.getAppFunctionDataList("innerList")
            assertThat(innerListResult).hasSize(2)
            assertThat(innerListResult!![0].getString("id")).isEqualTo("inner1")
            assertThat(innerListResult[1].getString("id")).isEqualTo("inner2")
            assertThat(outputData.getString("optionalMetadata")).isEqualTo("meta")
        }
    }

    @Test
    fun executeAppFunction_adapterVoidImplementation_success() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_ADAPTER_VOID,
            targetFunctionId = DYNAMIC_VOID_RETURN_SIGNATURE_ID,
        ) {
            val metadata = findAppFunctionMetadata(DYNAMIC_VOID_RETURN_SIGNATURE_ID)

            val response =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            DYNAMIC_VOID_RETURN_SIGNATURE_ID,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setString("message", "hello")
                                .build(),
                        )
                )

            assertIs<ExecuteAppFunctionResponse.Success>(response)
        }
    }

    @Test
    fun executeAppFunction_adapterThrowingImplementation_throwsException() = doBlocking {
        runWithDynamicAppFunctionRegistered(
            registerAction = ACTION_REGISTER_ADAPTER_THROWING,
            targetFunctionId = DYNAMIC_THROWING_SIGNATURE_ID,
        ) {
            val metadata = findAppFunctionMetadata(DYNAMIC_THROWING_SIGNATURE_ID)

            val response =
                appFunctionManager.executeAppFunction(
                    request =
                        ExecuteAppFunctionRequest(
                            TARGET_APP_PACKAGE,
                            DYNAMIC_THROWING_SIGNATURE_ID,
                            AppFunctionData.Builder(metadata.parameters, metadata.components)
                                .setString("exceptionType", "invalid_arg")
                                .build(),
                        )
                )

            val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(response)
            assertThat(errorResponse.error.errorMessage).contains("Simulated adapter exception")
        }
    }

    @Test
    fun getAppFunctionAdapter_adapterNotFound_throwsException() = doBlocking {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                appFunctionManager.getAppFunctionAdapter(UnadaptedSignature::class.java)
            }
        assertThat(exception.message).contains("@AppFunctionSignature")
    }

    private interface UnadaptedSignature

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
                appFunctionManager.assertAppFunctionEnabledState(
                    AppFunctionName(TARGET_APP_PACKAGE, GLOBAL_SIGNATURE_FORMAT_MESSAGE),
                    true,
                )
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
                appFunctionManager.assertAppFunctionEnabledState(
                    AppFunctionName(TARGET_APP_PACKAGE, targetFunctionId),
                    true,
                )
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
        return appFunctionManager.searchAppFunctions(AppFunctionSearchSpec()).single { it.id == id }
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
        const val ACTION_REGISTER_SUSPEND_FORMAT_MESSAGE =
            "androidx.appfunctions.integration.action.REGISTER_SUSPEND"
        const val ACTION_REGISTER_ADAPTER_ALL_PRIMITIVES =
            "androidx.appfunctions.integration.action.REGISTER_ADAPTER_ALL_PRIMITIVES"
        const val DYNAMIC_ALL_PRIMITIVES_INPUTS_SIGNATURE_ID =
            "androidx.appfunctions.integration.testapp.DynamicAllPrimitivesInputsSignature#processPrimitives"

        const val ACTION_REGISTER_ADAPTER_COMPLEX_SERIALIZABLE =
            "androidx.appfunctions.integration.action.REGISTER_ADAPTER_COMPLEX_SERIALIZABLE"
        const val DYNAMIC_COMPLEX_SERIALIZABLE_SIGNATURE_ID =
            "androidx.appfunctions.integration.testapp.DynamicComplexSerializableSignature#processComplex"

        const val ACTION_REGISTER_ADAPTER_VOID =
            "androidx.appfunctions.integration.action.REGISTER_ADAPTER_VOID"
        const val DYNAMIC_VOID_RETURN_SIGNATURE_ID =
            "androidx.appfunctions.integration.testapp.DynamicVoidReturnSignature#processVoid"

        const val ACTION_REGISTER_ADAPTER_THROWING =
            "androidx.appfunctions.integration.action.REGISTER_ADAPTER_THROWING"
        const val DYNAMIC_THROWING_SIGNATURE_ID =
            "androidx.appfunctions.integration.testapp.DynamicThrowingSignature#processAndThrow"
    }
}
