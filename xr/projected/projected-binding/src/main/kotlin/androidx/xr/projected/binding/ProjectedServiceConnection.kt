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

package androidx.xr.projected.binding

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.xr.projected.platform.IProjectedService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

/** Establishes a connection to the projected service. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProjectedServiceConnection(
    private val context: Context,
    private val projectedIntentAction: ProjectedIntentAction,
) {

    /** Supported Projected intent actions. */
    public class ProjectedIntentAction
    private constructor(internal val intentActionString: String) {

        public companion object {
            /** Action used to bind to the Projected service. */
            public val ACTION_BIND: ProjectedIntentAction =
                ProjectedIntentAction("androidx.xr.projected.ACTION_BIND")
        }
    }

    private var projectedServiceConnection: ServiceConnection? = null
    private var projectedServiceBinder: IBinder? = null
    private val projectedServiceDeathRecipient = IBinder.DeathRecipient { disconnect() }
    private val _isServiceConnected = MutableStateFlow(false)

    /**
     * A [StateFlow] that emits `true` when the connection to the projected service is established
     * and `false` when it is disconnected.
     */
    public val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    /**
     * Connects to the projected service and returns an [IProjectedService] instance.
     *
     * This method binds to the projected service and waits for the connection to be established. A
     * new connection can be established after a successful call to [disconnect].
     *
     * @param context The context to use for binding to the service.
     * @return An [IProjectedService] instance.
     * @throws IllegalStateException if the projected service is not found or binding is not
     *   permitted, or if [connect] is called while a connection is already active.
     * @throws kotlinx.coroutines.TimeoutCancellationException if the connection times out.
     */
    public suspend fun connect(): IProjectedService {
        if (projectedServiceConnection != null) {
            throw IllegalStateException(
                "Connect called while a service connection is already active."
            )
        }

        val serviceDeferred = CompletableDeferred<IProjectedService>()

        projectedServiceConnection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    serviceDeferred.complete(IProjectedService.Stub.asInterface(service))
                    projectedServiceBinder = service
                    service?.linkToDeath(projectedServiceDeathRecipient, /* flags= */ 0)
                    _isServiceConnected.tryEmit(true)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    _isServiceConnected.tryEmit(false)
                }
            }

        val isBindingPermitted =
            ProjectedServiceBinding.bind(
                context,
                projectedIntentAction.intentActionString,
                checkNotNull(projectedServiceConnection, { "Service connection is null" }),
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
    public fun disconnect() {
        _isServiceConnected.tryEmit(false)
        context.unbindService(
            checkNotNull(projectedServiceConnection, { "Service connection is null" })
        )
        projectedServiceBinder?.unlinkToDeath(projectedServiceDeathRecipient, /* flags= */ 0)
        projectedServiceBinder = null
        projectedServiceConnection = null
    }

    private companion object {
        /**
         * Timeout for binding to the projected service. Selected value is commonly used, e.g. by
         * the JUnit rules for testing services.
         */
        private const val SERVICE_CONNECTION_TIMEOUT_MS = 5000L
    }
}
