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
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.MeasureCapabilities
import androidx.health.services.client.impl.IpcConstants.MEASURE_API_BIND_ACTION
import androidx.health.services.client.impl.IpcConstants.SERVICE_PACKAGE_NAME
import androidx.health.services.client.impl.MeasureCallbackStub.MeasureCallbackCache
import androidx.health.services.client.impl.internal.HsConnectionManager
import androidx.health.services.client.impl.internal.StatusCallback
import androidx.health.services.client.impl.ipc.Client
import androidx.health.services.client.impl.ipc.ClientConfiguration
import androidx.health.services.client.impl.ipc.internal.ConnectionManager
import androidx.health.services.client.impl.request.CapabilitiesRequest
import androidx.health.services.client.impl.request.MeasureRegistrationRequest
import androidx.health.services.client.impl.request.MeasureUnregistrationRequest
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * [MeasureClient] implementation that is backed by Health Services.
 *
 * @hide
 */
@VisibleForTesting
public class ServiceBackedMeasureClient(
    private val context: Context,
    connectionManager: ConnectionManager
) :
    MeasureClient,
    Client<IMeasureApiService>(
        CLIENT_CONFIGURATION,
        connectionManager,
        { binder -> IMeasureApiService.Stub.asInterface(binder) },
        { service -> service.apiVersion }
    ) {

    override fun registerCallback(
        dataType: DataType,
        callback: MeasureCallback
    ): ListenableFuture<Void> =
        registerCallback(dataType, callback, ContextCompat.getMainExecutor(context))

    override fun registerCallback(
        dataType: DataType,
        callback: MeasureCallback,
        executor: Executor
    ): ListenableFuture<Void> {
        val request = MeasureRegistrationRequest(context.packageName, dataType)
        val callbackStub = MeasureCallbackCache.INSTANCE.getOrCreate(dataType, callback, executor)
        return registerListener(callbackStub.listenerKey) { service, resultFuture ->
            service.registerCallback(request, callbackStub, StatusCallback(resultFuture))
        }
    }

    override fun unregisterCallback(
        dataType: DataType,
        callback: MeasureCallback
    ): ListenableFuture<Void> {
        val callbackStub =
            MeasureCallbackCache.INSTANCE.remove(dataType, callback)
                ?: return Futures.immediateFailedFuture(
                    IllegalArgumentException("Given callback was not registered.")
                )
        val request = MeasureUnregistrationRequest(context.packageName, dataType)
        return unregisterListener(callbackStub.listenerKey) { service, resultFuture ->
            service.unregisterCallback(request, callbackStub, StatusCallback(resultFuture))
        }
    }

    override val capabilities: ListenableFuture<MeasureCapabilities>
        get() =
            Futures.transform(
                execute { service ->
                    service.getCapabilities(CapabilitiesRequest(context.packageName))
                },
                { response -> response?.measureCapabilities },
                ContextCompat.getMainExecutor(context)
            )

    internal companion object {
        const val CLIENT = "HealthServicesMeasureClient"
        private val CLIENT_CONFIGURATION =
            ClientConfiguration(CLIENT, SERVICE_PACKAGE_NAME, MEASURE_API_BIND_ACTION)

        @JvmStatic
        fun getClient(context: Context): ServiceBackedMeasureClient {
            return ServiceBackedMeasureClient(context, HsConnectionManager.getInstance(context))
        }
    }
}
