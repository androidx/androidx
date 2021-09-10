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
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.ExerciseGoalRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PrepareExerciseRequest
import androidx.health.services.client.impl.request.StartExerciseRequest
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
    ExerciseClient,
    Client<IExerciseApiService>(
        CLIENT_CONFIGURATION,
        connectionManager,
        { binder -> IExerciseApiService.Stub.asInterface(binder) },
        { service -> service.apiVersion }
    ) {

    private val packageName = context.packageName

    override fun prepareExercise(configuration: WarmUpConfig): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.prepareExercise(
                PrepareExerciseRequest(packageName, configuration),
                StatusCallback(resultFuture)
            )
        }

    override fun startExercise(configuration: ExerciseConfig): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.startExercise(
                StartExerciseRequest(packageName, configuration),
                StatusCallback(resultFuture)
            )
        }

    override fun pauseExercise(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.pauseExercise(packageName, StatusCallback(resultFuture))
    }

    override fun resumeExercise(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.resumeExercise(packageName, StatusCallback(resultFuture))
    }

    override fun endExercise(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.endExercise(packageName, StatusCallback(resultFuture))
    }

    override fun flushExercise(): ListenableFuture<Void> {
        val request = FlushRequest(packageName)
        return execute { service, resultFuture ->
            service.flushExercise(request, StatusCallback(resultFuture))
        }
    }

    override fun markLap(): ListenableFuture<Void> = execute { service, resultFuture ->
        service.markLap(packageName, StatusCallback(resultFuture))
    }

    override val currentExerciseInfo: ListenableFuture<ExerciseInfo>
        get() = execute { service, resultFuture ->
            service.getCurrentExerciseInfo(packageName, ExerciseInfoCallback(resultFuture))
        }

    override fun setUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void> =
        setUpdateListener(listener, ContextCompat.getMainExecutor(context))

    override fun setUpdateListener(
        listener: ExerciseUpdateListener,
        executor: Executor
    ): ListenableFuture<Void> {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.getOrCreate(
                listener,
                executor
            )
        return registerListener(listenerStub.listenerKey) { service, resultFuture ->
            service.setUpdateListener(packageName, listenerStub, StatusCallback(resultFuture))
        }
    }

    override fun clearUpdateListener(listener: ExerciseUpdateListener): ListenableFuture<Void> {
        val listenerStub =
            ExerciseUpdateListenerStub.ExerciseUpdateListenerCache.INSTANCE.remove(listener)
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Given listener was not added.")
                )
        return unregisterListener(listenerStub.listenerKey) { service, resultFuture ->
            service.clearUpdateListener(packageName, listenerStub, StatusCallback(resultFuture))
        }
    }

    override fun addGoalToActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.addGoalToActiveExercise(
                ExerciseGoalRequest(packageName, exerciseGoal),
                StatusCallback(resultFuture)
            )
        }

    override fun removeGoalFromActiveExercise(exerciseGoal: ExerciseGoal): ListenableFuture<Void> =
        execute { service, resultFuture ->
            service.removeGoalFromActiveExercise(
                ExerciseGoalRequest(packageName, exerciseGoal),
                StatusCallback(resultFuture)
            )
        }

    override fun overrideAutoPauseAndResumeForActiveExercise(
        enabled: Boolean
    ): ListenableFuture<Void> = execute { service, resultFuture ->
        service.overrideAutoPauseAndResumeForActiveExercise(
            AutoPauseAndResumeConfigRequest(packageName, enabled),
            StatusCallback(resultFuture)
        )
    }

    override val capabilities: ListenableFuture<ExerciseCapabilities>
        get() =
            Futures.transform(
                execute { service -> service.getCapabilities(CapabilitiesRequest(packageName)) },
                { response -> response?.exerciseCapabilities },
                ContextCompat.getMainExecutor(context)
            )

    internal companion object {
        private const val CLIENT = "HealthServicesExerciseClient"
        private val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, EXERCISE_API_BIND_ACTION)

        @JvmStatic
        fun getClient(context: Context): ServiceBackedExerciseClient {
            return ServiceBackedExerciseClient(context, HsConnectionManager.getInstance(context))
        }
    }
}
