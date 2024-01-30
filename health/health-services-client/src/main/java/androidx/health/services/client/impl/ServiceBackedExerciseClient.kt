/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.BatchingMode
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseTypeConfig
import androidx.health.services.client.data.WarmUpConfig
import androidx.health.services.client.impl.IpcConstants.EXERCISE_API_BIND_ACTION
import androidx.health.services.client.impl.IpcConstants.SERVICE_PACKAGE_NAME
import androidx.health.services.client.impl.internal.ExerciseInfoCallback
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest
import androidx.health.services.client.impl.request.BatchingModeConfigRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.ExerciseGoalRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PrepareExerciseRequest
import androidx.health.services.client.impl.request.StartExerciseRequest
import androidx.health.services.client.impl.request.UpdateExerciseTypeConfigRequest
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executor

/**
 * [ExerciseClient] implementation that is backed by Health Services.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ServiceBackedExerciseClient(
    private val context: Context,
    connectionManager: ConnectionManager = HsConnectionManager.getInstance(context)
) :
    ExerciseClient,
    Client<IExerciseApiService>(
        CLIENT_CONFIGURATION,
        connectionManager,
        { binder -> IExerciseApiService.Stub.asInterface(binder) },
        { service -> service.apiVersion }
    ) {

    private val requestedDataTypesLock = Any()
    @GuardedBy("requestedDataTypesLock")
    private val requestedDataTypes: MutableSet<DataType<*, *>> = mutableSetOf()
    private val packageName = context.packageName

    override fun prepareExerciseAsync(configuration: WarmUpConfig): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.prepareExercise(
                PrepareExerciseRequest(packageName, configuration),
                object : StatusCallback(resultFuture) {
                    override fun onSuccess() {
                        synchronized(requestedDataTypesLock) {
                            requestedDataTypes.clear()
                            requestedDataTypes.addAll(configuration.dataTypes)
                        }
                        super.onSuccess()
                    }
                }
            )
        }

    override fun startExerciseAsync(configuration: ExerciseConfig): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.startExercise(
                StartExerciseRequest(packageName, configuration),
                object : StatusCallback(resultFuture) {
                    override fun onSuccess() {
                        synchronized(requestedDataTypesLock) {
                            requestedDataTypes.clear()
                            requestedDataTypes.addAll(configuration.dataTypes)
                        }
                        super.onSuccess()
                    }
                }
            )
        }

    override fun pauseExerciseAsync(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.pauseExercise(packageName, StatusCallback(resultFuture))
    }

    override fun resumeExerciseAsync(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.resumeExercise(packageName, StatusCallback(resultFuture))
    }

    override fun endExerciseAsync(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.endExercise(packageName, StatusCallback(resultFuture))
    }

    override fun flushAsync(): ListenableFuture<Void> {
        val request = FlushRequest(packageName)
        return execute { service, resultFuture ->
            service.flushExercise(request, StatusCallback(resultFuture))
        }
    }

    override fun markLapAsync(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.markLap(packageName, StatusCallback(resultFuture))
    }

    override fun getCurrentExerciseInfoAsync(): ListenableFuture<ExerciseInfo> {
        return execute { service, resultFuture ->
            service.getCurrentExerciseInfo(packageName, ExerciseInfoCallback(resultFuture))
        }
    }

    override fun setUpdateCallback(callback: ExerciseUpdateCallback) {
        setUpdateCallback(ContextCompat.getMainExecutor(context), callback)
    }

    override fun setUpdateCallback(
        executor: Executor,
        callback: ExerciseUpdateCallback
    ) {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.getOrCreate(
                callback,
                executor,
                requestedDataTypesProvider = {
                    synchronized(requestedDataTypesLock) {
                        requestedDataTypes.toSet()
                    }
                }
            )
        val future =
            registerListener(listenerStub.listenerKey) { service, result: SettableFuture<Void?> ->
                service.setUpdateListener(packageName, listenerStub, StatusCallback(result))
            }
        Futures.addCallback(
            future,
            object : FutureCallback<Void?> {
                override fun onSuccess(result: Void?) {
                    callback.onRegistered()
                }

                override fun onFailure(t: Throwable) {
                    callback.onRegistrationFailed(t)
                }
            },
            executor)
    }

    override fun clearUpdateCallbackAsync(
        callback: ExerciseUpdateCallback
    ): ListenableFuture<Void> {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.remove(callback)
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Given listener was not added.")
                )
        return unregisterListener(listenerStub.listenerKey) { service, resultFuture ->
            service.clearUpdateListener(packageName, listenerStub, StatusCallback(resultFuture))
        }
    }

    override fun addGoalToActiveExerciseAsync(
        exerciseGoal: ExerciseGoal<*>
    ): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.addGoalToActiveExercise(
                ExerciseGoalRequest(packageName, exerciseGoal),
                StatusCallback(resultFuture)
            )
        }

    override fun removeGoalFromActiveExerciseAsync(
        exerciseGoal: ExerciseGoal<*>
    ): ListenableFuture<Void> = execute { service, resultFuture ->
        service.removeGoalFromActiveExercise(
            ExerciseGoalRequest(packageName, exerciseGoal),
            StatusCallback(resultFuture)
        )
    }

    override fun overrideAutoPauseAndResumeForActiveExerciseAsync(
        enabled: Boolean
    ): ListenableFuture<Void> = execute { service, resultFuture ->
        service.overrideAutoPauseAndResumeForActiveExercise(
            AutoPauseAndResumeConfigRequest(packageName, enabled),
            StatusCallback(resultFuture)
        )
    }

    override fun overrideBatchingModesForActiveExerciseAsync(
        batchingModes: Set<BatchingMode>
    ): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.overrideBatchingModesForActiveExercise(
                    BatchingModeConfigRequest(packageName, batchingModes),
                    StatusCallback(resultFuture)
                )
            },
            /* minApiVersion= */ 4
        )
    }

    override fun getCapabilitiesAsync(): ListenableFuture<ExerciseCapabilities> =
        Futures.transform(
            execute { service -> service.getCapabilities(CapabilitiesRequest(packageName)) },
            { response -> response!!.exerciseCapabilities },
            ContextCompat.getMainExecutor(context)
        )

    override fun updateExerciseTypeConfigAsync(
        exerciseTypeConfig: ExerciseTypeConfig
    ): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.updateExerciseTypeConfigForActiveExercise(
                    UpdateExerciseTypeConfigRequest(packageName, exerciseTypeConfig),
                    StatusCallback(resultFuture)
                )
            },
            3
        )
    }

    internal companion object {
        internal const val CLIENT = "HealthServicesExerciseClient"
        internal val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, EXERCISE_API_BIND_ACTION)
    }
}
