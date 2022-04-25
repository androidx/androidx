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

import android.content.BroadcastReceiver
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.PassiveMonitoringCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.PassiveGoal
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.PassiveMonitoringConfig
import androidx.health.services.client.impl.IpcConstants.PASSIVE_API_BIND_ACTION
import androidx.health.services.client.impl.IpcConstants.SERVICE_PACKAGE_NAME
import androidx.health.services.client.impl.PassiveListenerCallbackStub.PassiveListenerCallbackCache
import androidx.health.services.client.impl.PassiveMonitoringCallbackStub.PassiveMonitoringCallbackCache
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.BackgroundRegistrationRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.FlushRequest
import androidx.health.services.client.impl.request.PassiveGoalRequest
import androidx.health.services.client.impl.request.PassiveListenerCallbackRegistrationRequest
import androidx.health.services.client.impl.request.PassiveListenerServiceRegistrationRequest
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * [PassiveMonitoringClient] implementation that is backed by Health Services.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
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

    override fun registerDataCallbackAsync(
        configuration: PassiveMonitoringConfig
    ): ListenableFuture<Void> {
        val callbackStub = PassiveMonitoringCallbackCache.INSTANCE.remove(packageName)
        // If the client had a previous callback registered, make sure it's cleared.
        if (callbackStub != null) {
            return unregisterListener(callbackStub.listenerKey) { service, resultFuture ->
                service.registerDataCallback(
                    BackgroundRegistrationRequest(configuration),
                    null,
                    StatusCallback(resultFuture)
                )
            }
        }

        return execute { service, resultFuture ->
            service.registerDataCallback(
                BackgroundRegistrationRequest(configuration),
                null,
                StatusCallback(resultFuture)
            )
        }
    }

    override fun registerDataCallbackAsync(
        configuration: PassiveMonitoringConfig,
        callback: PassiveMonitoringCallback
    ): ListenableFuture<Void> {
        val callbackStub =
            PassiveMonitoringCallbackCache.INSTANCE.getOrCreate(packageName, callback)
        return registerListener(callbackStub.listenerKey) { service, resultFuture ->
            service.registerDataCallback(
                BackgroundRegistrationRequest(configuration),
                callbackStub,
                StatusCallback(resultFuture)
            )
        }
    }

    override fun unregisterDataCallbackAsync(): ListenableFuture<Void> {
        val callbackStub = PassiveMonitoringCallbackCache.INSTANCE.remove(packageName)
        if (callbackStub != null) {
            return unregisterListener(callbackStub.listenerKey) { service, resultFuture ->
                service.unregisterDataCallback(packageName, StatusCallback(resultFuture))
            }
        }
        return execute { service, resultFuture ->
            service.unregisterDataCallback(packageName, StatusCallback(resultFuture))
        }
    }

    override fun <T : BroadcastReceiver> registerPassiveGoalCallbackAsync(
        passiveGoal: PassiveGoal,
        broadcastReceiver: Class<T>,
    ): ListenableFuture<Void> {
        val request = PassiveGoalRequest(packageName, broadcastReceiver.getName(), passiveGoal)
        return execute { service, resultFuture ->
            service.registerPassiveGoalCallback(request, StatusCallback(resultFuture))
        }
    }

    override fun unregisterPassiveGoalCallbackAsync(
        passiveGoal: PassiveGoal
    ): ListenableFuture<Void> {
        val request = PassiveGoalRequest(packageName, /*unused*/ "", passiveGoal)
        return execute { service, resultFuture ->
            service.unregisterPassiveGoalCallback(request, StatusCallback(resultFuture))
        }
    }

    // TODO(b/227475943): make public and add override after adding this back to the interface.
    internal fun registerPassiveListenerServiceAsync(
        passiveListenerService: Class<out PassiveListenerService>,
        passiveListenerConfig: PassiveListenerConfig
    ): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.registerPassiveListenerService(
                    PassiveListenerServiceRegistrationRequest(
                        packageName,
                        passiveListenerService.getName(),
                        passiveListenerConfig
                    ),
                    StatusCallback(resultFuture)
                )
            },
            /* minApiVersion= */ 4
        )
    }

    // TODO(b/227475943): make public and add override after adding this back to the interface.
    internal fun registerPassiveListenerCallbackAsync(
        passiveListenerCallback: PassiveListenerCallback,
        passiveListenerConfig: PassiveListenerConfig
    ): ListenableFuture<Void> {
        val callbackStub =
            PassiveListenerCallbackCache.INSTANCE.getOrCreate(packageName, passiveListenerCallback)
        return registerListener(callbackStub.listenerKey) { service, resultFuture ->
            service.registerPassiveListenerCallback(
                PassiveListenerCallbackRegistrationRequest(packageName, passiveListenerConfig),
                callbackStub,
                StatusCallback(resultFuture)
            )
        }
    }

    // TODO(b/227475943): make public and add override after adding this back to the interface.
    internal fun unregisterPassiveListenerServiceAsync(): ListenableFuture<Void> {
        return executeWithVersionCheck(
            { service, resultFuture ->
                service.unregisterPassiveListenerService(packageName, StatusCallback(resultFuture))
            },
            /* minApiVersion= */ 4
        )
    }

    // TODO(b/227475943): make public and add override after adding this back to the interface.
    internal fun unregisterPassiveListenerCallbackAsync(): ListenableFuture<Void> {
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

    override val capabilities: ListenableFuture<PassiveMonitoringCapabilities>
        get() =
            Futures.transform(
                execute { service -> service.getCapabilities(CapabilitiesRequest(packageName)) },
                { response -> response!!.passiveMonitoringCapabilities },
                ContextCompat.getMainExecutor(applicationContext)
            )

    private companion object {
        const val CLIENT = "HealthServicesPassiveMonitoringClient"
        private val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, PASSIVE_API_BIND_ACTION)
    }
}
