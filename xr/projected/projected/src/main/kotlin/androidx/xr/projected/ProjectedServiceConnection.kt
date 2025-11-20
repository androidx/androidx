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

package androidx.xr.projected

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.xr.projected.platform.IProjectedService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

/** Establishes a connection to the projected service. */
internal class ProjectedServiceConnection(private val context: Context) {

    private var serviceConnection: ServiceConnection? = null
    private var serviceBinder: IBinder? = null
    private val deathRecipient = IBinder.DeathRecipient { disconnect() }
    private val _serviceConnected = MutableStateFlow(false)
    internal val serviceConnected = _serviceConnected.asStateFlow()

    /**
     * Connects to the projected service and returns an [IProjectedService] instance.
     *
     * This method binds to the projected service and waits for the connection to be established.
     *
     * @param context The context to use for binding to the service.
     * @return An [IProjectedService] instance.
     * @throws IllegalStateException if the projected service is not found or binding is not
     *   permitted.
     * @throws kotlinx.coroutines.TimeoutCancellationException if the connection times out.
     * @throws IllegalStateException if the service connection is null.
     */
    internal suspend fun connect(): IProjectedService {
        val serviceDeferred = CompletableDeferred<IProjectedService>()

        serviceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    serviceDeferred.complete(IProjectedService.Stub.asInterface(service))
                    serviceBinder = service
                    service?.linkToDeath(deathRecipient, /* flags= */ 0)
                    _serviceConnected.tryEmit(true)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    _serviceConnected.tryEmit(false)
                }
            }

        val isBindingPermitted =
            ProjectedServiceBinding.bind(
                context,
                checkNotNull(serviceConnection, { "Service connection is null" }),
            )
        if (!isBindingPermitted) {
            serviceDeferred.completeExceptionally(
                IllegalStateException("Projected service not found or binding was not permitted.")
            )
        }

        return withTimeout(SERVICE_CONNECTION_TIMEOUT_MS) { serviceDeferred.await() }
    }

    /**
     * Disconnects from the [IProjectedService] by unbinding it.
     *
     * @throws IllegalStateException if the service connection is null.
     */
    internal fun disconnect() {
        _serviceConnected.tryEmit(false)
        context.unbindService(checkNotNull(serviceConnection, { "Service connection is null" }))
        serviceBinder?.unlinkToDeath(deathRecipient, /* flags= */ 0)
        serviceBinder = null
    }

    private companion object {
        /**
         * Timeout for binding to the projected service. Selected value is commonly used, e.g. by
         * the JUnit rules for testing services.
         */
        private const val SERVICE_CONNECTION_TIMEOUT_MS = 5000L
    }
}
