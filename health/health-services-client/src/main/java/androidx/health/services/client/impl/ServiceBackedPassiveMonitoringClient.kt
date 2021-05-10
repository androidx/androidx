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

import android.app.PendingIntent
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.health.services.client.PassiveMonitoringCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveMonitoringCapabilities
import androidx.health.services.client.data.event.Event
import androidx.health.services.client.impl.PassiveMonitoringIpcClient.Companion.getServiceInterface
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.ServiceOperation
import androidx.health.services.client.impl.request.BackgroundRegistrationRequest
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.EventRequest
import androidx.health.services.client.impl.response.PassiveMonitoringCapabilitiesResponse
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * [PassiveMonitoringClient] implementation that is backed by Health Services.
 *
 * @hide
 */
internal class ServiceBackedPassiveMonitoringClient(private val applicationContext: Context) :
    PassiveMonitoringClient {

    private val ipcClient: PassiveMonitoringIpcClient =
        PassiveMonitoringIpcClient(HsConnectionManager.getInstance(applicationContext))

    override fun registerDataCallback(
        dataTypes: Set<DataType>,
        callbackIntent: PendingIntent
    ): ListenableFuture<Void> {
        return registerDataCallbackInternal(dataTypes, callbackIntent, callback = null)
    }

    override fun registerDataCallback(
        dataTypes: Set<DataType>,
        callbackIntent: PendingIntent,
        callback: PassiveMonitoringCallback
    ): ListenableFuture<Void> {
        return registerDataCallbackInternal(dataTypes, callbackIntent, callback)
    }

    override fun unregisterDataCallback(): ListenableFuture<Void> {
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .unregisterDataCallback(
                        applicationContext.packageName,
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun registerEventCallback(
        event: Event,
        callbackIntent: PendingIntent
    ): ListenableFuture<Void> {
        val request = EventRequest(applicationContext.packageName, event)
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .registerEventCallback(request, callbackIntent, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override fun unregisterEventCallback(event: Event): ListenableFuture<Void> {
        val request = EventRequest(applicationContext.packageName, event)
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .unregisterEventCallback(request, StatusCallback(resultFuture))
            }
        return ipcClient.execute(serviceOperation)
    }

    override val capabilities: ListenableFuture<PassiveMonitoringCapabilities>
        get() {
            val request = CapabilitiesRequest(applicationContext.packageName)
            val serviceOperation =
                ServiceOperation<PassiveMonitoringCapabilitiesResponse> { binder, resultFuture ->
                    resultFuture.set(getServiceInterface(binder).getCapabilities(request))
                }
            return Futures.transform(
                ipcClient.execute(serviceOperation),
                { response -> response?.passiveMonitoringCapabilities },
                ContextCompat.getMainExecutor(applicationContext)
            )
        }

    private fun registerDataCallbackInternal(
        dataTypes: Set<DataType>,
        callbackIntent: PendingIntent,
        callback: PassiveMonitoringCallback?
    ): ListenableFuture<Void> {
        val request = BackgroundRegistrationRequest(applicationContext.packageName, dataTypes)
        val serviceOperation =
            ServiceOperation<Void> { binder, resultFuture ->
                getServiceInterface(binder)
                    .registerDataCallback(
                        request,
                        callbackIntent,
                        callback?.let { PassiveMonitoringCallbackStub(it) },
                        StatusCallback(resultFuture)
                    )
            }
        return ipcClient.execute(serviceOperation)
    }
}
