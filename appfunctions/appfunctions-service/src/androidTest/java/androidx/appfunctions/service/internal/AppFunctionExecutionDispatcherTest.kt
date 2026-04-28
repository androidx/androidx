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

package androidx.appfunctions.service.internal

import android.os.Build
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionExecutionDispatcherTest {

    @Test
    fun executeAppFunction_succeeds() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                AppFunctionData.Builder(
                        AppFunctionMetadataTestHelper.FunctionMetadata.NO_SCHEMA_ENABLED_BY_DEFAULT
                            .parameters,
                        AppFunctionComponentsMetadata(),
                    )
                    .setInt("intParam", 100)
                    .build(),
            )

        var intParamValue: Any? = null
        val response =
            AppFunctionExecutionDispatcher.executeAppFunction(FakeAppFunctionInventory, request) {
                params ->
                intParamValue = params["intParam"]
                Unit
            }

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(intParamValue).isEqualTo(100)
    }

    @Test
    fun executeAppFunction_throwsFunctionNotFoundException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                "non_existent_function_id",
                AppFunctionData.EMPTY,
            )

        val exception =
            assertThrows(AppFunctionFunctionNotFoundException::class.java) {
                runBlocking {
                    AppFunctionExecutionDispatcher.executeAppFunction(
                        FakeAppFunctionInventory,
                        request,
                    ) { params ->
                        "Result"
                    }
                }
            }
        assertThat(exception.errorMessage).isEqualTo("non_existent_function_id is not available")
    }

    @Test
    fun executeAppFunction_throwsAppFunctionCancelledException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val exception =
            assertThrows(AppFunctionCancelledException::class.java) {
                runBlocking {
                    AppFunctionExecutionDispatcher.executeAppFunction(
                        FakeAppFunctionInventory,
                        request,
                    ) { params ->
                        throw CancellationException("Cancelled")
                    }
                }
            }
        assertThat(exception.message).isEqualTo("Cancelled")
    }

    @Test
    fun executeAppFunction_throwsAppFunctionException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val exception =
            assertThrows(AppFunctionDeniedException::class.java) {
                runBlocking {
                    AppFunctionExecutionDispatcher.executeAppFunction(
                        FakeAppFunctionInventory,
                        request,
                    ) { params ->
                        throw AppFunctionDeniedException("Specific Exception")
                    }
                }
            }
        assertThat(exception.errorMessage).isEqualTo("Specific Exception")
    }

    @Test
    fun executeAppFunction_throwsAppFunctionAppUnknownException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val exception =
            assertThrows(AppFunctionAppUnknownException::class.java) {
                runBlocking {
                    AppFunctionExecutionDispatcher.executeAppFunction(
                        FakeAppFunctionInventory,
                        request,
                    ) { params ->
                        throw IllegalStateException("Generic Exception")
                    }
                }
            }
        assertThat(exception.message).isEqualTo("Generic Exception")
    }

    private object FakeAppFunctionInventory : AppFunctionInventory {
        override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
            get() =
                mapOf(
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT to
                        CompileTimeAppFunctionMetadata(
                            id =
                                AppFunctionMetadataTestHelper.FunctionIds
                                    .NO_SCHEMA_ENABLED_BY_DEFAULT,
                            isEnabledByDefault = true,
                            schema = null,
                            parameters =
                                listOf(
                                    AppFunctionParameterMetadata(
                                        name = "intParam",
                                        isRequired = true,
                                        dataType = AppFunctionIntTypeMetadata(isNullable = false),
                                    )
                                ),
                            response =
                                AppFunctionResponseMetadata(
                                    valueType = AppFunctionUnitTypeMetadata(isNullable = false)
                                ),
                        ),
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED to
                        CompileTimeAppFunctionMetadata(
                            id =
                                AppFunctionMetadataTestHelper.FunctionIds
                                    .NO_SCHEMA_EXECUTION_SUCCEED,
                            isEnabledByDefault = true,
                            schema = null,
                            parameters = listOf(),
                            response =
                                AppFunctionResponseMetadata(
                                    valueType = AppFunctionStringTypeMetadata(isNullable = false)
                                ),
                        ),
                )

        override val componentsMetadata: AppFunctionComponentsMetadata
            get() = AppFunctionComponentsMetadata()
    }
}
