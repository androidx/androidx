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

package androidx.appfunctions.integration.testapp

import android.annotation.SuppressLint
import android.app.Service
import android.app.appfunctions.AppFunctionRegistration
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionCancelledException
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.CallbackAppFunction
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A background [Service] running in the target test app process that enables out-of-process dynamic
 * app function registration and unregistration for E2E integration testing.
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
@SuppressLint("RestrictedApiAndroidX")
class DynamicRegistrationService : Service() {

    private lateinit var appFunctionManager: AppFunctionManager
    private var registration: AppFunctionRegistration? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        appFunctionManager =
            AppFunctionManager.getInstance(this)
                ?: throw IllegalStateException("AppFunctionManager is null")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_REGISTER -> {
                val appFunction = CallbackAppFunction { request, _, callback ->
                    val a = request.functionParameters.getInt("a")
                    val b = request.functionParameters.getString("b")
                    callback.accept(buildSuccessResponse("callback_result_${a}_${b}"))
                }
                registration =
                    appFunctionManager.registerAppFunction(
                        FORMAT_MESSAGE_FUNCTION_ID,
                        Dispatchers.Default.asExecutor(),
                        appFunction,
                    )
            }
            ACTION_REGISTER_CALLBACK_THROWS -> {
                val appFunction = CallbackAppFunction { _, _, _ ->
                    throw RuntimeException("Simulated error in callback execution")
                }
                registration =
                    appFunctionManager.registerAppFunction(
                        FORMAT_MESSAGE_FUNCTION_ID,
                        Dispatchers.Default.asExecutor(),
                        appFunction,
                    )
            }
            ACTION_REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION -> {
                val appFunction = CallbackAppFunction { _, _, _ ->
                    throw AppFunctionInvalidArgumentException("Simulated AppFunctionException")
                }
                registration =
                    appFunctionManager.registerAppFunction(
                        FORMAT_MESSAGE_FUNCTION_ID,
                        Dispatchers.Default.asExecutor(),
                        appFunction,
                    )
            }

            ACTION_REGISTER_LONG_RUNNING -> {
                val appFunction = CallbackAppFunction { request, cancellationSignal, callback ->
                    val job =
                        scope.launch {
                            try {
                                delay(5000)
                                callback.accept(buildSuccessResponse("long_running_result"))
                            } catch (e: CancellationException) {
                                callback.accept(
                                    ExecuteAppFunctionResponse.Error(
                                        AppFunctionCancelledException(e.message)
                                    )
                                )
                            }
                        }
                    cancellationSignal.setOnCancelListener { job.cancel() }
                }
                registration =
                    appFunctionManager.registerAppFunction(
                        FORMAT_MESSAGE_FUNCTION_ID,
                        Dispatchers.Default.asExecutor(),
                        appFunction,
                    )
            }
            ACTION_UNREGISTER -> {
                registration?.unregister()
                registration = null
            }
        }
        return START_NOT_STICKY
    }

    private fun buildSuccessResponse(value: String): ExecuteAppFunctionResponse.Success {
        val responseData =
            AppFunctionData.Builder(RESPONSE_TYPE, AppFunctionComponentsMetadata(emptyMap()))
                .setString(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, value)
                .build()
        return ExecuteAppFunctionResponse.Success(responseData)
    }

    override fun onDestroy() {

        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val FORMAT_MESSAGE_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.FormatMessageSignature#formatMessage"

        private val RESPONSE_TYPE =
            AppFunctionObjectTypeMetadata(
                properties =
                    mapOf(
                        ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE to
                            AppFunctionStringTypeMetadata(isNullable = false)
                    ),
                required = emptyList(),
                qualifiedName =
                    "androidx.appfunctions.integration.testapp.FormatMessageSignature#formatMessageResponse",
                isNullable = false,
            )

        /**
         * Registers a standard dynamic callback function that concatenates arguments "a" and "b".
         */
        const val ACTION_REGISTER = "androidx.appfunctions.integration.action.REGISTER_CALLBACK"

        /** Unregisters the currently registered dynamic callback function. */
        const val ACTION_UNREGISTER = "androidx.appfunctions.integration.action.UNREGISTER_CALLBACK"

        /** Registers a dynamic callback that throws a standard [RuntimeException]. */
        const val ACTION_REGISTER_CALLBACK_THROWS =
            "androidx.appfunctions.integration.action.REGISTER_CALLBACK_THROWS"

        /** Registers a dynamic callback that throws an [AppFunctionInvalidArgumentException]. */
        const val ACTION_REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION =
            "androidx.appfunctions.integration.action.REGISTER_CALLBACK_THROWS_APP_FUNCTION_EXCEPTION"

        /**
         * Registers a long-running dynamic callback that delays its response and supports
         * cancellation.
         */
        const val ACTION_REGISTER_LONG_RUNNING =
            "androidx.appfunctions.integration.action.REGISTER_LONG_RUNNING"
    }
}
