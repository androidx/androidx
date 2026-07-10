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
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
class AppFunctionRuntimeRegistrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val metadataTestHelper: AppFunctionMetadataTestHelper =
        AppFunctionMetadataTestHelper(context)

    private val appFunctionManager: AppFunctionManager by lazy {
        checkNotNull(AppFunctionManager.getInstance(context))
    }

    private val uiAutomation: UiAutomation =
        InstrumentationRegistry.getInstrumentation().uiAutomation

    @Before
    fun setup() {
        assumeNotNull(AppFunctionManager.getInstance(context))

        uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.INSTALL_PACKAGES,
            "android.permission.EXECUTE_APP_FUNCTIONS",
        )
    }

    @After
    fun tearDown() {
        uiAutomation.dropShellPermissionIdentity()
    }

    @Test
    fun selfExecuteActivityScopedRegisteredFunction_shouldSucceed() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
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
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName =
                            AppFunctionMetadataTestHelper.FunctionMetadata
                                .DYNAMIC_REGISTRATION_RETURN_SUCCESS
                                .packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response = appFunctionManager.executeAppFunction(request)

                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse = response as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult)
            } finally {
                registration.unregister()
            }
        }
    }

    @Test
    fun registerAppFunctions_multipleFunctions_shouldSucceed() {
        val functionId1 =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val expectedResult1 = "self_execution_result_1"
        val functionId2 =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS_2
        val expectedResult2 = "self_execution_result_2"

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val callbackAppFunction1 = CallbackAppFunction { _, _, callback ->
                callback.accept(createReturnStringResponse(expectedResult1))
            }
            val callbackAppFunction2 = CallbackAppFunction { _, _, callback ->
                callback.accept(createReturnStringResponse(expectedResult2))
            }

            val request1 =
                RegisterAppFunctionRequest(functionId1, activity.mainExecutor, callbackAppFunction1)
            val request2 =
                RegisterAppFunctionRequest(functionId2, activity.mainExecutor, callbackAppFunction2)

            val registration =
                activityAppFunctionManager.registerAppFunctions(listOf(request1, request2))

            try {
                val executionRequest1 =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId1,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response1 = appFunctionManager.executeAppFunction(executionRequest1)

                assertThat(response1).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse1 = response1 as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse1.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult1)

                val executionRequest2 =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId2,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response2 = appFunctionManager.executeAppFunction(executionRequest2)

                assertThat(response2).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse2 = response2 as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse2.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult2)
            } finally {
                registration.unregister()
            }

            val unregisteredException1 = executeAppFunctionAndGetException(activity, functionId1)
            assertIs<AppFunctionDisabledException>(unregisteredException1)

            val unregisteredException2 = executeAppFunctionAndGetException(activity, functionId2)
            assertIs<AppFunctionDisabledException>(unregisteredException2)
        }
    }

    @Test
    fun handleAppFunctions_multipleFunctions_shouldSucceed() {
        val functionId1 =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val expectedResult1 = "self_execution_result_1"
        val functionId2 =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS_2
        val expectedResult2 = "self_execution_result_2"

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val suspendAppFunction1 = SuspendingAppFunction { _ ->
                createReturnStringResponse(expectedResult1)
            }
            val suspendAppFunction2 = SuspendingAppFunction { _ ->
                createReturnStringResponse(expectedResult2)
            }

            val request1 = HandleAppFunctionRequest(functionId1, suspendAppFunction1)
            val request2 = HandleAppFunctionRequest(functionId2, suspendAppFunction2)

            val handleJob =
                launch(Dispatchers.Default) {
                    activityAppFunctionManager.handleAppFunctions(listOf(request1, request2))
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId1)
            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId2)

            try {
                val executionRequest1 =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId1,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response1 = appFunctionManager.executeAppFunction(executionRequest1)

                assertThat(response1).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse1 = response1 as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse1.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult1)

                val executionRequest2 =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId2,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response2 = appFunctionManager.executeAppFunction(executionRequest2)

                assertThat(response2).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse2 = response2 as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse2.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo(expectedResult2)
            } finally {
                handleJob.cancel()
            }

            val unregisteredException1 = executeAppFunctionAndGetException(activity, functionId1)
            assertIs<AppFunctionDisabledException>(unregisteredException1)

            val unregisteredException2 = executeAppFunctionAndGetException(activity, functionId2)
            assertIs<AppFunctionDisabledException>(unregisteredException2)
        }
    }

    @Test
    fun testRegisterAppFunction_callerCancellation_propagatesCancellation() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val functionStartedDeferred = CompletableDeferred<Unit>()
            val functionCancelledDeferred = CompletableDeferred<Unit>()

            val callbackAppFunction = CallbackAppFunction { _, cancellationSignal, callback ->
                functionStartedDeferred.complete(Unit)
                cancellationSignal.setOnCancelListener { functionCancelledDeferred.complete(Unit) }
            }

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    functionId,
                    activity.mainExecutor,
                    callbackAppFunction,
                )

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
                val executionJob =
                    launch(Dispatchers.Default) {
                        try {
                            val response = appFunctionManager.executeAppFunction(request)
                            responseDeferred.complete(response)
                        } catch (e: Throwable) {
                            responseDeferred.completeExceptionally(e)
                        }
                    }

                functionStartedDeferred.await()

                executionJob.cancel()

                assertFailsWith<CancellationException> { responseDeferred.await() }

                // Verifies that the cancellation is actually received by the callback.
                functionCancelledDeferred.await()
            } finally {
                registration.unregister()
            }
        }
    }

    @Test
    fun testRegisterAppFunction_runsOnProvidedExecutor() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "my-custom-test-executor-thread")
            }

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val executionThreadNameDeferred = CompletableDeferred<String>()
            val callbackAppFunction = CallbackAppFunction { _, _, callback ->
                executionThreadNameDeferred.complete(Thread.currentThread().name)
                callback.accept(createReturnStringResponse("success"))
            }

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    functionId,
                    executor,
                    callbackAppFunction,
                )

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response = appFunctionManager.executeAppFunction(request)

                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val threadName = executionThreadNameDeferred.await()
                assertThat(threadName).isEqualTo("my-custom-test-executor-thread")
            } finally {
                registration.unregister()
                executor.shutdown()
            }
        }
    }

    @Test
    fun testHandleAppFunction_callingFromNonActivityOrServiceContext_throwsException() {
        assertFailsWith<IllegalStateException> {
            runBlocking {
                val suspendAppFunction = SuspendingAppFunction { _ ->
                    throw UnsupportedOperationException()
                }

                appFunctionManager.handleAppFunction(
                    HandleAppFunctionRequest(
                        functionIdentifier = "any_function_id",
                        appFunction = suspendAppFunction,
                    )
                )
            }
        }
    }

    @Test
    fun testHandleAppFunction_wrongFunctionId_throwsException() {
        runWithActivityAppFunctionManager { _, activityAppFunctionManager ->
            val suspendAppFunction = SuspendingAppFunction { _ ->
                throw UnsupportedOperationException()
            }

            assertFailsWith<IllegalArgumentException> {
                activityAppFunctionManager.handleAppFunction(
                    HandleAppFunctionRequest(
                        functionIdentifier = "invalid_function_id",
                        appFunction = suspendAppFunction,
                    )
                )
            }
        }
    }

    @Test
    fun testHandleAppFunction_executorCancellation_throwsCancelledException() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val functionStartedDeferred = CompletableDeferred<Unit>()
            val functionCancelledDeferred = CompletableDeferred<Unit>()

            val longRunningAppFunction = SuspendingAppFunction { _ ->
                functionStartedDeferred.complete(Unit)
                try {
                    delay(60000)
                    ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)
                } catch (e: CancellationException) {
                    functionCancelledDeferred.complete(Unit)
                    throw e
                }
            }

            val handleJob =
                launch(Dispatchers.Default) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = longRunningAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
                val executionJob =
                    launch(Dispatchers.Default) {
                        try {
                            val response = appFunctionManager.executeAppFunction(request)
                            responseDeferred.complete(response)
                        } catch (e: Throwable) {
                            responseDeferred.completeExceptionally(e)
                        }
                    }

                functionStartedDeferred.await()

                executionJob.cancel()

                assertFailsWith<CancellationException> { responseDeferred.await() }

                // Verifies that the cancellation is actually received by the callback.
                functionCancelledDeferred.await()
            } finally {
                handleJob.cancel()
            }
        }
    }

    @Test
    fun testHandleAppFunction_callerScopeCancelled_reportsUnknownError() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val functionStartedDeferred = CompletableDeferred<Unit>()

            val longRunningAppFunction = SuspendingAppFunction { _ ->
                functionStartedDeferred.complete(Unit)
                delay(60000)
                ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)
            }

            val handleJob =
                launch(Dispatchers.Default) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = longRunningAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
                launch(Dispatchers.Default) {
                    val response = appFunctionManager.executeAppFunction(request)
                    responseDeferred.complete(response)
                }

                functionStartedDeferred.await()

                handleJob.cancel()

                val response = responseDeferred.await()
                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
                val errorResponse = response as ExecuteAppFunctionResponse.Error
                assertIs<AppFunctionAppUnknownException>(errorResponse.error)
            } finally {
                handleJob.cancel()
            }
        }
    }

    @Test
    fun testHandleAppFunction_whenAppFunctionThrowsRuntimeException_reportsUnknownError() {
        val runtimeExceptionMessage = "Something went wrong inside AppFunction"
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val suspendAppFunction = SuspendingAppFunction { _ ->
                throw RuntimeException(runtimeExceptionMessage)
            }

            val handleDeferred =
                async(Dispatchers.Default + SupervisorJob()) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = suspendAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                val exception = executeAppFunctionAndGetException(activity, functionId)
                assertIs<AppFunctionAppUnknownException>(exception)
                assertThat(exception.message).contains(runtimeExceptionMessage)

                val thrown = assertFailsWith<RuntimeException> { handleDeferred.await() }
                assertThat(thrown.message).contains(runtimeExceptionMessage)

                val unregisteredException = executeAppFunctionAndGetException(activity, functionId)
                assertIs<AppFunctionDisabledException>(unregisteredException)
            } finally {
                handleDeferred.cancel()
            }
        }
    }

    @Test
    fun testHandleAppFunction_whenAppFunctionThrowsAppFunctionException_reportsCorrectError() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val suspendAppFunction = SuspendingAppFunction { _ ->
                throw AppFunctionFunctionNotFoundException("Custom function not found exception")
            }

            val handleJob =
                launch(Dispatchers.Default) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = suspendAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                val exception = executeAppFunctionAndGetException(activity, functionId)
                assertIs<AppFunctionFunctionNotFoundException>(exception)
                assertThat(exception.message).contains("Custom function not found exception")
            } finally {
                handleJob.cancel()
            }
        }
    }

    @Test
    fun testHandleAppFunction_runsInCallerScope() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "my-test-executor-thread")
            }
        val customDispatcher = executor.asCoroutineDispatcher()

        val suspendAppFunction = SuspendingAppFunction { _ ->
            val currentName = coroutineContext[CoroutineName]?.name ?: ""
            val threadName = Thread.currentThread().name
            createReturnStringResponse("$currentName:$threadName")
        }

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val handleJob =
                launch(CoroutineName("my-test-scope") + customDispatcher) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = suspendAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName =
                            AppFunctionMetadataTestHelper.FunctionMetadata
                                .DYNAMIC_REGISTRATION_RETURN_SUCCESS
                                .packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response = appFunctionManager.executeAppFunction(request)

                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse = response as ExecuteAppFunctionResponse.Success
                assertThat(
                        successResponse.returnValue.getString(
                            ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                        )
                    )
                    .isEqualTo("my-test-scope:my-test-executor-thread")
            } finally {
                handleJob.cancel()
                executor.shutdown()
            }
        }
    }

    @Test
    fun testHandleAppFunction_whenExecutorNotSpecified_runsInCallingThread() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val handleThreadDeferred = CompletableDeferred<String>()

            val suspendAppFunction = SuspendingAppFunction { _ ->
                createReturnStringResponse(Thread.currentThread().name)
            }

            val scope = CoroutineScope(Job())
            val handleJob =
                scope.launch {
                    handleThreadDeferred.complete(Thread.currentThread().name)
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = suspendAppFunction,
                        )
                    )
                }

            try {
                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName =
                            AppFunctionMetadataTestHelper.FunctionMetadata
                                .DYNAMIC_REGISTRATION_RETURN_SUCCESS
                                .packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val response = appFunctionManager.executeAppFunction(request)

                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Success::class.java)
                val successResponse = response as ExecuteAppFunctionResponse.Success
                val executionThread =
                    successResponse.returnValue.getString(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE
                    )
                val handleThread = runBlocking { handleThreadDeferred.await() }
                assertThat(handleThread).startsWith("DefaultDispatcher")
                assertThat(executionThread).startsWith("DefaultDispatcher")
            } finally {
                handleJob.cancel()
            }
        }
    }

    @Test
    fun testHandleAppFunction_cancelledBeforeDispatch_doesNotHang() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "my-custom-test-executor-thread")
            }
        val customDispatcher = executor.asCoroutineDispatcher()

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val suspendAppFunction = SuspendingAppFunction { _ ->
                createReturnStringResponse("success")
            }

            val handleJob =
                launch(customDispatcher) {
                    activityAppFunctionManager.handleAppFunction(
                        HandleAppFunctionRequest(
                            functionIdentifier = functionId,
                            appFunction = suspendAppFunction,
                        )
                    )
                }

            metadataTestHelper.awaitAppFunctionEnabled(activityAppFunctionManager, functionId)

            try {
                // Block the single-thread dispatcher
                val blockingLatch = CountDownLatch(1)
                val taskEnqueuedLatch = CountDownLatch(1)
                executor.execute {
                    taskEnqueuedLatch.countDown()
                    blockingLatch.await()
                }

                // Wait to make sure the blocking task is running
                taskEnqueuedLatch.await()

                val request =
                    ExecuteAppFunctionRequest(
                        targetPackageName = context.packageName,
                        functionIdentifier = functionId,
                        functionParameters = AppFunctionData.EMPTY,
                    )

                val responseDeferred = CompletableDeferred<ExecuteAppFunctionResponse>()
                val executionJob =
                    launch(Dispatchers.Default) {
                        try {
                            val response = appFunctionManager.executeAppFunction(request)
                            responseDeferred.complete(response)
                        } catch (e: Throwable) {
                            responseDeferred.completeExceptionally(e)
                        }
                    }

                // Wait for the execution coroutine to be queued onto the blocked customDispatcher.
                delay(WAIT_FOR_COROUTINE_ENQUEUE_DELAY_MS)

                // Cancel the handleJob while the coroutine is queued but not yet dispatched.
                handleJob.cancel()

                // Unblock the dispatcher.
                blockingLatch.countDown()

                val response = withTimeout(EXECUTION_TIMEOUT_MS) { responseDeferred.await() }
                assertThat(response).isInstanceOf(ExecuteAppFunctionResponse.Error::class.java)
                val errorResponse = response as ExecuteAppFunctionResponse.Error
                assertIs<AppFunctionAppUnknownException>(errorResponse.error)
            } finally {
                handleJob.cancel()
                executor.shutdown()
            }
        }
    }

    @Test
    fun testRegisterAppFunction_cancelledBeforeDispatch_doesNotHang() {
        val functionId =
            AppFunctionMetadataTestHelper.FunctionIds.DYNAMIC_REGISTRATION_RETURN_SUCCESS
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "my-custom-test-executor-thread")
            }

        runWithActivityAppFunctionManager { activity, activityAppFunctionManager ->
            val callbackAppFunction = CallbackAppFunction { _, _, callback ->
                callback.accept(createReturnStringResponse("success"))
            }

            val registration =
                activityAppFunctionManager.registerAppFunction(
                    functionId,
                    executor,
                    callbackAppFunction,
                )

            try {
                // We use the platform AppFunctionManager directly to independently trigger
                // the CancellationSignal. The Jetpack executeAppFunction() API immediately aborts
                // and drops its OutcomeReceiver on cancellation, which would prevent us from
                // verifying if the provider actually finishes or hangs.
                val platformManager =
                    context.getSystemService(
                        android.app.appfunctions.AppFunctionManager::class.java
                    )!!

                val platformRequest =
                    android.app.appfunctions.ExecuteAppFunctionRequest.Builder(
                            context.packageName,
                            functionId,
                        )
                        .build()

                val responseDeferred =
                    CompletableDeferred<android.app.appfunctions.ExecuteAppFunctionResponse>()

                // Block the single-thread dispatcher
                val blockingLatch = CountDownLatch(1)
                val taskEnqueuedLatch = CountDownLatch(1)
                executor.execute {
                    taskEnqueuedLatch.countDown()
                    blockingLatch.await()
                }

                // Wait to make sure the blocking task is running
                taskEnqueuedLatch.await()

                val cancellationSignal = android.os.CancellationSignal()

                platformManager.executeAppFunction(
                    platformRequest,
                    context.mainExecutor,
                    cancellationSignal,
                    object :
                        android.os.OutcomeReceiver<
                            android.app.appfunctions.ExecuteAppFunctionResponse,
                            android.app.appfunctions.AppFunctionException,
                        > {
                        override fun onResult(
                            result: android.app.appfunctions.ExecuteAppFunctionResponse
                        ) {
                            responseDeferred.complete(result)
                        }

                        override fun onError(error: android.app.appfunctions.AppFunctionException) {
                            responseDeferred.completeExceptionally(error)
                        }
                    },
                )

                // Wait for the execution coroutine to be queued onto the blocked executor.
                delay(WAIT_FOR_COROUTINE_ENQUEUE_DELAY_MS)

                // Cancel the execution via CancellationSignal.
                // This triggers the provider's CancellationSignal listener,
                // which cancels the provider's execution coroutine before it gets dispatched.
                cancellationSignal.cancel()

                // Unblock the dispatcher.
                blockingLatch.countDown()

                // Verify we receive the AppFunctionException.
                // Without CoroutineStart.ATOMIC, the OutcomeReceiver is silently dropped and this
                // await() would hang until timeout.
                val error =
                    assertFailsWith<android.app.appfunctions.AppFunctionException> {
                        withTimeout(EXECUTION_TIMEOUT_MS) { responseDeferred.await() }
                    }
            } finally {
                registration.unregister()
                executor.shutdown()
            }
        }
    }

    private fun runWithActivityAppFunctionManager(
        block:
            suspend CoroutineScope.(
                activity: TestActivity, activityAppFunctionManager: AppFunctionManager,
            ) -> Unit
    ) {
        val intent =
            Intent(context, TestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
        assumeNotNull(activity)
        check(activity is TestActivity) { "Failed to start TestActivity, got $activity" }

        runBlocking {
            try {
                val activityAppFunctionManager = AppFunctionManager.getInstance(activity)
                checkNotNull(activityAppFunctionManager)
                block(activity, activityAppFunctionManager)
            } finally {
                activity.finish()
            }
        }
    }

    private suspend fun executeAppFunctionAndGetException(
        activity: TestActivity,
        functionId: String,
    ): AppFunctionException {
        val request =
            ExecuteAppFunctionRequest(
                targetPackageName = context.packageName,
                functionIdentifier = functionId,
                functionParameters = AppFunctionData.EMPTY,
            )
        val response = appFunctionManager.executeAppFunction(request)
        val errorResponse = assertIs<ExecuteAppFunctionResponse.Error>(response)
        return assertIs<AppFunctionException>(errorResponse.error)
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

    companion object {
        private const val EXECUTION_TIMEOUT_MS = 30000L
        private const val WAIT_FOR_COROUTINE_ENQUEUE_DELAY_MS = 1000L
    }
}
