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

package androidx.appfunctions

import android.Manifest
import android.app.UiAutomation
import android.content.Context
import android.os.Build
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.core.AppFunctionMetadataTestHelper.FunctionNames
import androidx.appfunctions.internal.runWithActivityAppFunctionManager
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class GetAppFunctionStatesTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private lateinit var appFunctionManager: AppFunctionManager

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    private val resetFunctionIds =
        setOf(
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
            AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
        )

    @Before
    fun setup() {
        val appFunctionManagerOrNull = AppFunctionManager.getInstance(context)
        assumeNotNull(appFunctionManagerOrNull)
        appFunctionManager = checkNotNull(appFunctionManagerOrNull)

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
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun getAppFunctionState_functionDoesNotExist_returnsEmpty() {
        val fakeFunction = AppFunctionName("fake.package", "doesNotExist")
        val states = runBlocking { appFunctionManager.getAppFunctionStates(listOf(fakeFunction)) }

        assertThat(states).isEmpty()
    }

    @Test
    fun getAppFunctionStates_returnsCorrectDefaultState() {
        val states = runBlocking {
            appFunctionManager.getAppFunctionStates(
                listOf(
                    FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    FunctionNames.NO_SCHEMA_DISABLED_BY_DEFAULT,
                )
            )
        }

        assertThat(states)
            .containsExactly(
                AppFunctionState(
                    functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    isEnabled = true,
                ),
                AppFunctionState(
                    functionName = FunctionNames.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    isEnabled = false,
                ),
            )
    }

    @Test
    fun getAppFunctionStates_reflectsRuntimeChangesAfterDisabled() {
        val functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT
        try {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )

                val state = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()

                assertThat(state.isEnabled).isFalse()
            }
        } finally {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @Test
    fun getAppFunctionStates_reflectsRuntimeChangesAfterDisabledThenEnabled() {
        val functionName = FunctionNames.NO_SCHEMA_ENABLED_BY_DEFAULT
        try {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DISABLED,
                )
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_ENABLED,
                )

                val state = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()

                assertThat(state.isEnabled).isTrue()
            }
        } finally {
            runBlocking {
                appFunctionManager.setAppFunctionEnabled(
                    functionName.functionIdentifier,
                    AppFunctionManager.APP_FUNCTION_STATE_DEFAULT,
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test
    fun getAppFunctionState_dynamicRegistrationAfterRegisterThenUnregister() {
        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val callbackAppFunction = CallbackAppFunction { _, _, callback ->
                callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
            }
            val functionName =
                AppFunctionName(
                    context.packageName,
                    AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS,
                )
            val stateBeforeRegistering =
                appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
            assertThat(stateBeforeRegistering.isEnabled).isFalse()

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS,
                    activity.mainExecutor,
                    callbackAppFunction,
                )
            try {
                val stateAfterRegistering =
                    appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
                assertThat(stateAfterRegistering.isEnabled).isTrue()

                registration.unregister()

                val stateAfterUnregistering =
                    appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
                assertThat(stateAfterUnregistering.isEnabled).isFalse()
            } finally {
                registration.unregister()
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test
    fun getAppFunctionStates_registerFromActivity_reportsActivityId() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.ACTIVITY_DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val functionName = AppFunctionName("androidx.appfunctions.test", functionId)
        val expectedResult = "self_execution_result"

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val callbackAppFunction = CallbackAppFunction { _, _, callback ->
                callback.accept(createReturnStringResponse(expectedResult))
            }

            val state1 = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
            assertThat(state1.isEnabled).isFalse()
            assertThat(state1.activityIds).isNull()

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    functionId,
                    activity.mainExecutor,
                    callbackAppFunction,
                )

            try {
                val state2 = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
                assertThat(state2.isEnabled).isTrue()
                assertThat(state2.activityIds).isNotNull()
                assertThat(state2.activityIds).hasSize(1)
            } finally {
                registration.unregister()
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test
    fun getAppFunctionStates_registerFromActivity_canBeExecutedWithActivityId() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.ACTIVITY_DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val functionName = AppFunctionName("androidx.appfunctions.test", functionId)
        val expectedResult = "self_execution_result"

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val callbackAppFunction = CallbackAppFunction { _, _, callback ->
                callback.accept(createReturnStringResponse(expectedResult))
            }

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    functionId,
                    activity.mainExecutor,
                    callbackAppFunction,
                )

            try {
                val state = appFunctionManager.getAppFunctionStates(listOf(functionName)).single()
                assertThat(state.isEnabled).isTrue()
                assertThat(state.activityIds).isNotNull()
                assertThat(state.activityIds).hasSize(1)

                val platformManager =
                    context.getSystemService(
                        android.app.appfunctions.AppFunctionManager::class.java
                    )!!
                val platformRequest =
                    android.app.appfunctions.ExecuteAppFunctionRequest.Builder(
                            context.packageName,
                            functionId,
                        )
                        .setActivityId(state.activityIds!!.first())
                        .build()

                val response =
                    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                        platformManager.executeAppFunction(
                            platformRequest,
                            context.mainExecutor,
                            android.os.CancellationSignal(),
                            object :
                                android.os.OutcomeReceiver<
                                    android.app.appfunctions.ExecuteAppFunctionResponse,
                                    android.app.appfunctions.AppFunctionException,
                                > {
                                override fun onResult(
                                    result: android.app.appfunctions.ExecuteAppFunctionResponse
                                ) {
                                    cont.resume(result)
                                }

                                override fun onError(
                                    error: android.app.appfunctions.AppFunctionException
                                ) {
                                    cont.resumeWithException(error)
                                }
                            },
                        )
                    }

                assertThat(
                        response.resultDocument.getPropertyString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult)
            } finally {
                registration.unregister()
            }
        }
    }

    private fun createReturnStringResponse(
        returnValue: String
    ): ExecuteAppFunctionResponse.Success {
        val responseType =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE to
                            AppFunctionStringTypeMetadata(isNullable = false)
                    ),
                required = emptyList(),
                qualifiedName = "androidx.appfunctions.test#noSchema_executionSucceedResponse",
                isNullable = false,
            )
        val responseData =
            AppFunctionData.Builder(responseType, AppFunctionComponentsMetadata(emptyMap()))
                .setString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, returnValue)
                .build()
        return ExecuteAppFunctionResponse.Success(responseData)
    }
}
