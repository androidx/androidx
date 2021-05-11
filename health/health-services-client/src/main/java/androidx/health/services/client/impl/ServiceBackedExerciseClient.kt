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
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateListener
import androidx.health.services.client.data.ExerciseCapabilities
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseGoal
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.impl.ExerciseIpcClient.Companion.getServiceInterface
import androidx.health.services.client.impl.internal.ExerciseInfoCallback
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.ServiceOperation
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.AutoPauseAndResumeConfigRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.ExerciseGoalRequest
import androidx.health.services.client.impl.request.StartExerciseRequest
import androidx.health.services.client.impl.response.ExerciseCapabilitiesResponse
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * [ExerciseClient] implementation that is backed by Health Services.
 *
 * @hide
 */
internal class ServiceBackedExerciseClient
private constructor(private val context: Context, connectionManager: ConnectionManager) :
    ExerciseClient {

    private val ipcClient: ExerciseIpcClient = ExerciseIpcClient(connectionManager)

    override fun startExercise(configuration: ExerciseConfig): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .startExercise(
                        StartExerciseRequest(context.packageName, configuration),
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun pauseExercise(): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .pauseExercise(context.packageName, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun resumeExercise(): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .resumeExercise(context.packageName, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun endExercise(): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .endExercise(context.packageName, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun markLap(): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .markLap(context.packageName, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override val currentExerciseInfo: ListenableFuture<ExerciseInfo>
        get() {
            val serviceOperation =
                ServiceOperation<ExerciseInfo> { binder, resultFuture ->
                    getServiceInterface(binder)
                        .getCurrentExerciseInfo(
                            context.packageName,
                            ExerciseInfoCallback(resultFuture)
                        )
                }
            return ipcClient.execute(serviceOperation)
        }

    override fun setUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void> {
        return setUpdateListener(listener, ContextCompat.getMainExecutor(context))
    }

    override fun setUpdateListener(
        listener: ExerciseUpdateListener,
        executor: Executor
    ): ListenableFuture<Void> {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.getOrCreate(
                listener,
                executor
            )
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .setUpdateListener(
                        context.packageName,
                        listenerStub,
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.registerListener(listenerStub.listenerKey, serviceOperation)
    }

    override fun clearUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void> {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.remove(listener)
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Given listener was not added.")
                )
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .clearUpdateListener(
                        context.packageName,
                        listenerStub,
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.unregisterListener(listenerStub.listenerKey, serviceOperation)
    }

    override fun addGoalToActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .addGoalToActiveExercise(
                        ExerciseGoalRequest(context.packageName, exerciseGoal),
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun overrideAutoPauseAndResumeForActiveExercise(
        enabled: Boolean
    ): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .overrideAutoPauseAndResumeForActiveExercise(
                        AutoPauseAndResumeConfigRequest(context.packageName, enabled),
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.execute(serviceOperation)
    }

    override val capabilities: ListenableFuture<ExerciseCapabilities>
        get() {
            val request = CapabilitiesRequest(context.packageName)
            val serviceOperation =
                ServiceOperation<ExerciseCapabilitiesResponse> { binder, resultFuture ->
                    resultFuture.set(getServiceInterface(binder).getCapabilities(request))
                }
            return Futures.transform(
                ipcClient.execute(serviceOperation),
                { response -> response?.exerciseCapabilities },
                ContextCompat.getMainExecutor(context)
            )
        }

    internal companion object {
        @JvmStatic
        fun getClient(context: Context): ServiceBackedExerciseClient {
            return ServiceBackedExerciseClient(context, HsConnectionManager.getInstance(context))
        }
    }
}
