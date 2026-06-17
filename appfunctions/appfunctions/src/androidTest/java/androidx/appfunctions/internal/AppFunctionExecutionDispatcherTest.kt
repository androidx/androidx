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

package androidx.appfunctions.internal

import android.os.Build
import android.os.CancellationSignal
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionExecutionDispatcherTest {

    @Test
    fun dispatchExecuteAppFunction_succeeds() = runBlocking {
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

        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
        var intParamValue: Any? = null
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            this,
            request,
            FakeAppFunctionInventory,
            CancellationSignal(),
            { response -> responseDeferred.complete(response) },
        ) { params ->
            intParamValue = params["intParam"]
            Unit
        }

        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
        assertThat(intParamValue).isEqualTo(100)
    }

    @Test
    fun dispatchExecuteAppFunction_returnsError_whenFunctionNotFound() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                "non_existent_function_id",
                AppFunctionData.EMPTY,
            )

        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            this,
            request,
            FakeAppFunctionInventory,
            CancellationSignal(),
            { response -> responseDeferred.complete(response) },
        ) { params ->
            "Result"
        }

        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertThat(error).isInstanceOf(AppFunctionFunctionNotFoundException::class.java)
        assertThat(error.errorMessage).isEqualTo("non_existent_function_id is not available")
    }

    @Test
    fun dispatchExecuteAppFunction_returnsError_whenCancelled() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            this,
            request,
            FakeAppFunctionInventory,
            CancellationSignal(),
            { response -> responseDeferred.complete(response) },
        ) { params ->
            throw CancellationException("Cancelled")
        }

        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertThat(error).isInstanceOf(AppFunctionCancelledException::class.java)
        assertThat(error.errorMessage).isEqualTo("Cancelled")
    }

    @Test
    fun dispatchExecuteAppFunction_returnsError_onAppFunctionException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            this,
            request,
            FakeAppFunctionInventory,
            CancellationSignal(),
            { response -> responseDeferred.complete(response) },
        ) { params ->
            throw AppFunctionDeniedException("Specific Exception")
        }

        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertThat(error).isInstanceOf(AppFunctionDeniedException::class.java)
        assertThat(error.errorMessage).isEqualTo("Specific Exception")
    }

    @Test
    fun dispatchExecuteAppFunction_returnsError_onUnknownException() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )

        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            this,
            request,
            FakeAppFunctionInventory,
            CancellationSignal(),
            { response -> responseDeferred.complete(response) },
        ) { params ->
            throw IllegalStateException("Generic Exception")
        }

        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val error = (response as ExecuteAppFunctionResponse.Error).error
        assertThat(error).isInstanceOf(AppFunctionAppUnknownException::class.java)
        assertThat(error.errorMessage).isEqualTo("Generic Exception")
    }

    @Test
    fun dispatchExecuteAppFunction_cancelsExecution_onCancellationSignal() = runBlocking {
        val request =
            ExecuteAppFunctionRequest(
                "test_target_package",
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                AppFunctionData.EMPTY,
            )
        val cancellationSignal = CancellationSignal()
        val blockStarted = CompletableDeferred<Unit>()
        val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()

        launch {
            AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
                this,
                request,
                FakeAppFunctionInventory,
                cancellationSignal,
                { response -> responseDeferred.complete(response) },
            ) { params ->
                blockStarted.complete(Unit)
                while (true) {
                    delay(1000.milliseconds)
                }
            }
        }
        blockStarted.await()
        cancellationSignal.cancel()
        val response = responseDeferred.await()

        assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
        val exception = (response as ExecuteAppFunctionResponse.Error).error
        assertThat(exception).isInstanceOf(AppFunctionCancelledException::class.java)
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
