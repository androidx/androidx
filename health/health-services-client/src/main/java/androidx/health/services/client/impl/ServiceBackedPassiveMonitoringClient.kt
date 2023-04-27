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
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServicesException
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.impl.IpcConstants.PASSIVE_API_BIND_ACTION
import androidx.health.services.client.impl.IpcConstants.SERVICE_PACKAGE_NAME
import androidx.health.services.client.impl.PassiveListenerCallbackStub.PassiveListenerCallbackCache
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PassiveListenerCallbackRegistrationRequest
import androidx.health.services.client.impl.request.PassiveListenerServiceRegistrationRequest
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executor

/**
 * [PassiveMonitoringClient] implementation that is backed by Health Services.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceBackedPassiveMonitoringClient(
    private val applicationContext: Context,
    private val connectionManager: ConnectionManager =
        HsConnectionManager.getInstance(applicationContext)
) :
    PassiveMonitoringClient,
    Client<IPassiveMonitoringApiService>(
        CLIENT_CONFIGURATION,
        connectionManager,
        { binder -> IPassiveMonitoringApiService.Stub.asInterface(binder) },
        { service -> service.apiVersion }
    ) {

    private val packageName = applicationContext.packageName

    override fun setPassiveListenerServiceAsync(
        service: Class<out PassiveListenerService>,
        config: PassiveListenerConfig
    ): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { remoteService, resultFuture ->
                if (config.isValidPassiveGoal()) {
                    remoteService.registerPassiveListenerService(
                        PassiveListenerServiceRegistrationRequest(
                            packageName,
                            service.name,
                            config
                        ),
                        StatusCallback(resultFuture)
                    )
                } else {
                    resultFuture.setException(HealthServicesException(
                            "DataType for the requested passive goal is not tracked"
                        ))
                }
            },
            /* minApiVersion= */ 4
        )
    }

    override fun setPassiveListenerCallback(
        config: PassiveListenerConfig,
        callback: PassiveListenerCallback
    ) {
        setPassiveListenerCallback(
            config,
            ContextCompat.getMainExecutor(applicationContext),
            callback
        )
    }

    override fun setPassiveListenerCallback(
        config: PassiveListenerConfig,
        executor: Executor,
        callback: PassiveListenerCallback
    ) {
        val callbackStub =
            PassiveListenerCallbackCache.INSTANCE.getOrCreate(packageName, executor, callback)
        val future =
            registerListener(callbackStub.listenerKey) { service, result: SettableFuture<Void?> ->
                if (config.isValidPassiveGoal()) {
                    service.registerPassiveListenerCallback(
                        PassiveListenerCallbackRegistrationRequest(packageName, config),
                        callbackStub,
                        StatusCallback(result)
                    )
                } else {
                    result.setException(
                        HealthServicesException(
                            "DataType for the requested passive goal is not tracked"
                        )
                    )
                }
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
            executor
        )
    }

    override fun clearPassiveListenerServiceAsync(): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.unregisterPassiveListenerService(packageName, StatusCallback(resultFuture))
            },
            /* minApiVersion= */ 4
        )
    }

    override fun clearPassiveListenerCallbackAsync(): ListenableFuture<Void> {
        val callbackStub = PassiveListenerCallbackCache.INSTANCE.remove(packageName)
        if (callbackStub != null) {
            return unregisterListener(callbackStub.listenerKey) { service, resultFuture ->
                service.unregisterPassiveListenerCallback(packageName, StatusCallback(resultFuture))
            }
        }
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.unregisterPassiveListenerCallback(packageName, StatusCallback(resultFuture))
            },
            /* minApiVersion= */ 4
        )
    }

    override fun flushAsync(): ListenableFuture<Void> {
        val request = FlushRequest(packageName)
        return execute { service, resultFuture ->
            service.flush(request, StatusCallback(resultFuture))
        }
    }

    override fun getCapabilitiesAsync(): ListenableFuture<PassiveMonitoringCapabilities> =
        Futures.transform(
            execute { service -> service.getCapabilities(CapabilitiesRequest(packageName)) },
            { response -> response!!.passiveMonitoringCapabilities },
            ContextCompat.getMainExecutor(applicationContext)
        )

    internal companion object {
        internal const val CLIENT = "HealthServicesPassiveMonitoringClient"
        internal val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, PASSIVE_API_BIND_ACTION)
    }
}
