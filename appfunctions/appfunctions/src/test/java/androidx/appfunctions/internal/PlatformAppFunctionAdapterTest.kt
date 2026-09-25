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

import android.app.appfunctions.AppFunctionException as PlatformAppFunctionException
import android.app.appfunctions.ExecuteAppFunctionRequest as PlatformExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse as PlatformExecuteAppFunctionResponse
import android.app.appsearch.GenericDocument
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.test.assertIs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
@OptIn(ExperimentalAppFunctionsApi::class)
class PlatformAppFunctionAdapterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val executor = Executor { it.run() }

    @After
    fun tearDown() {
        CallerAccessVerifier.testingSender = null
    }

    private fun createMetadata(
        functionId: String,
        accessLevel: Int = AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED,
        isCompatEnforcementEnabled: Boolean = true,
        packageName: String = context.packageName,
    ): AppFunctionMetadata {
        val name = AppFunctionName(packageName, functionId)
        return AppFunctionMetadata(
            name = name,
            schema = null,
            parameters = emptyList<AppFunctionParameterMetadata>(),
            response =
                AppFunctionResponseMetadata(
                    valueType = AppFunctionUnitTypeMetadata(isNullable = false)
                ),
            packageMetadata =
                AppFunctionPackageMetadata(packageName, AppFunctionComponentsMetadata()),
            accessLevel = accessLevel,
            isCompatEnforcementEnabled = isCompatEnforcementEnabled,
        )
    }

    private class FakeAppFunctionReader(
        private val metadataMap: Map<String, AppFunctionMetadata> = emptyMap()
    ) : AppFunctionReader {
        override suspend fun searchAppFunctionsMetadata(
            searchFunctionSpec: androidx.appfunctions.AppFunctionSearchSpec
        ): List<AppFunctionMetadata> = metadataMap.values.toList()

        override suspend fun getAppFunctionMetadata(
            functionId: String,
            packageName: String,
        ): AppFunctionMetadata? = metadataMap[functionId]

        override suspend fun getAppFunctionStates(
            appFunctionNames: List<AppFunctionName>
        ): List<androidx.appfunctions.AppFunctionState> = emptyList()

        override fun observeAppFunctions():
            kotlinx.coroutines.flow.Flow<androidx.appfunctions.AppFunctionsChangeEvent> =
            kotlinx.coroutines.flow.emptyFlow()
    }

    private suspend fun android.app.appfunctions.AppFunction.execute(
        request: PlatformExecuteAppFunctionRequest
    ): Result<PlatformExecuteAppFunctionResponse> = suspendCancellableCoroutine { cont ->
        onExecuteAppFunction(
            request,
            CancellationSignal(),
            object :
                OutcomeReceiver<
                    PlatformExecuteAppFunctionResponse,
                    PlatformAppFunctionException,
                > {
                override fun onResult(result: PlatformExecuteAppFunctionResponse) {
                    cont.resume(Result.success(result))
                }

                override fun onError(error: PlatformAppFunctionException) {
                    cont.resume(Result.failure(error))
                }
            },
        )
    }

    @Test
    fun onExecuteAppFunction_metadataNotFound_failsWithFunctionNotFoundException() = runTest {
        val reader = FakeAppFunctionReader()
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn("testFunction")
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.extras).thenReturn(Bundle())

        val caughtError =
            assertIs<PlatformAppFunctionException>(
                platformAppFunction.execute(request).exceptionOrNull()
            )
        assertThat(caughtError.errorCode)
            .isEqualTo(PlatformAppFunctionException.ERROR_FUNCTION_NOT_FOUND)
    }

    @Test
    fun onExecuteAppFunction_selfAccess_deniedWhenNoToken() = runTest {
        val functionId = "selfFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = true,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.extras).thenReturn(Bundle())

        val caughtError =
            assertIs<PlatformAppFunctionException>(
                platformAppFunction.execute(request).exceptionOrNull()
            )
        assertThat(caughtError.errorCode).isEqualTo(PlatformAppFunctionException.ERROR_DENIED)
    }

    @Test
    fun onExecuteAppFunction_selfAccess_deniedWhenMismatchedBinderToken() = runTest {
        val functionId = "selfFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = true,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val extras =
            Bundle().apply {
                putBinder(
                    CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN,
                    Binder(), // Mismatched token
                )
            }
        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.extras).thenReturn(extras)

        val caughtError =
            assertIs<PlatformAppFunctionException>(
                platformAppFunction.execute(request).exceptionOrNull()
            )
        assertThat(caughtError.errorCode).isEqualTo(PlatformAppFunctionException.ERROR_DENIED)
    }

    private fun createEmptyGenericDocument(): GenericDocument =
        GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build()

    @Test
    fun onExecuteAppFunction_selfAccess_successWithToken() = runTest {
        val functionId = "selfFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = true,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val extras =
            Bundle().apply {
                putBinder(
                    CallerAccessVerifier.EXTRA_CALLER_BINDER_TOKEN,
                    CallerAccessVerifier.selfBinderToken,
                )
            }
        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.parameters).thenReturn(createEmptyGenericDocument())
        `when`(request.extras).thenReturn(extras)

        val executionResult = platformAppFunction.execute(request)
        assertThat(executionResult.exceptionOrNull()).isNull()
        val successResult = executionResult.getOrThrow()
        assertThat(successResult).isNotNull()
    }

    @Test
    fun onExecuteAppFunction_androidTrusted_successWithoutToken() = runTest {
        val functionId = "trustedFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED,
                isCompatEnforcementEnabled = true,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.parameters).thenReturn(createEmptyGenericDocument())
        `when`(request.extras).thenReturn(Bundle())

        val executionResult = platformAppFunction.execute(request)
        assertThat(executionResult.exceptionOrNull()).isNull()
        val successResult = executionResult.getOrThrow()
        assertThat(successResult).isNotNull()
    }

    @Test
    fun onExecuteAppFunction_selfAccessWithCompatDisabled_successWithoutToken() = runTest {
        val functionId = "compatDisabledFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SELF,
                isCompatEnforcementEnabled = false,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.parameters).thenReturn(createEmptyGenericDocument())
        `when`(request.extras).thenReturn(Bundle())

        val executionResult = platformAppFunction.execute(request)
        assertThat(executionResult.exceptionOrNull()).isNull()
        val successResult = executionResult.getOrThrow()
        assertThat(successResult).isNotNull()
    }

    @Test
    fun onExecuteAppFunction_systemAccess_deniedWhenNoToken() = runTest {
        val functionId = "systemFunction"
        val metadata =
            createMetadata(
                functionId = functionId,
                accessLevel = AppFunctionMetadata.ACCESS_LEVEL_SYSTEM,
                isCompatEnforcementEnabled = true,
            )
        val reader = FakeAppFunctionReader(mapOf(functionId to metadata))
        val appFunction = AppFunction { _, _, callback ->
            callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
        }
        val platformAppFunction = appFunction.toPlatformAppFunction(reader, executor, context)

        val request = mock(PlatformExecuteAppFunctionRequest::class.java)
        `when`(request.functionIdentifier).thenReturn(functionId)
        `when`(request.targetPackageName).thenReturn(context.packageName)
        `when`(request.extras).thenReturn(Bundle())

        val caughtError =
            assertIs<PlatformAppFunctionException>(
                platformAppFunction.execute(request).exceptionOrNull()
            )
        assertThat(caughtError.errorCode).isEqualTo(PlatformAppFunctionException.ERROR_DENIED)
    }
}
