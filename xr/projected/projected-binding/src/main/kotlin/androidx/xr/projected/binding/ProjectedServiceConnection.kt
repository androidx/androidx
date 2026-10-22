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
import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.xr.projected.platform.IProjectedService
import kotlinx.coroutines.CancellationException
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

    // Serializes connection state across the main thread (ServiceConnection callbacks), Binder
    // threads (DeathRecipient.binderDied), and caller threads (connect / disconnect).
    private val lock = Any()
    private var projectedServiceConnection: ServiceConnection? = null
    private var projectedServiceBinder: IBinder? = null
    private var projectedServiceDeathRecipient: IBinder.DeathRecipient? = null
    private var onProjectedServiceConnectedDeferred: CompletableDeferred<IProjectedService>? = null
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
     * @return An [IProjectedService] instance.
     * @throws IllegalStateException if the projected service is not found or binding is not
     *   permitted, if the service dies or disconnects before binding completes, or if [connect] is
     *   called while a connection is already active.
     * @throws kotlinx.coroutines.TimeoutCancellationException if the connection times out.
     */
    public suspend fun connect(): IProjectedService {
        val serviceDeferred = CompletableDeferred<IProjectedService>()
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    synchronized(lock) {
                        // Ignore late callbacks if this connection was already disconnected.
                        if (projectedServiceConnection !== this) return@synchronized
                        onProjectedServiceConnectedDeferred = null
                        if (service == null) {
                            serviceDeferred.completeExceptionally(
                                IllegalStateException("Projected service binder is null.")
                            )
                            return@synchronized
                        }
                        // Scope the death recipient to both this connection and this
                        // binder so a late binderDied() from an older session cannot
                        // disconnect a newer connection or a restarted service binder.
                        val deathRecipient = IBinder.DeathRecipient { disconnect(this, service) }
                        try {
                            // Register linkToDeath under lock before completing
                            // serviceDeferred so a caller resuming from connect() on a
                            // background dispatcher cannot invoke disconnect() and
                            // unlinkToDeath() before linkToDeath() has registered.
                            service.linkToDeath(deathRecipient, /* flags= */ 0)
                            // If the service restarted after onServiceDisconnected on the
                            // same ServiceConnection, unlink the previous binder.
                            projectedServiceBinder?.let { previousBinder ->
                                projectedServiceDeathRecipient?.let { previousRecipient ->
                                    try {
                                        previousBinder.unlinkToDeath(
                                            previousRecipient,
                                            /* flags= */ 0,
                                        )
                                    } catch (_: NoSuchElementException) {}
                                }
                            }
                            projectedServiceBinder = service
                            projectedServiceDeathRecipient = deathRecipient
                            _isServiceConnected.tryEmit(true)
                            serviceDeferred.complete(IProjectedService.Stub.asInterface(service))
                        } catch (e: RemoteException) {
                            serviceDeferred.completeExceptionally(
                                IllegalStateException(
                                    "Projected service died before binding completed.",
                                    e,
                                )
                            )
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(lock) {
                        if (projectedServiceConnection === this) {
                            _isServiceConnected.tryEmit(false)
                        }
                    }
                }
            }

        synchronized(lock) {
            check(projectedServiceConnection == null) {
                "Connect called while a service connection is already active."
            }
            projectedServiceConnection = connection
            onProjectedServiceConnectedDeferred = serviceDeferred
        }

        try {
            val isBindingPermitted =
                ProjectedServiceBinding.bind(
                    context,
                    projectedIntentAction.intentActionString,
                    connection,
                )
            check(isBindingPermitted) {
                "Projected service not found or binding was not permitted."
            }
            return withTimeout(SERVICE_CONNECTION_TIMEOUT_MS) { serviceDeferred.await() }
        } catch (e: Throwable) {
            // Unbind and clear state for this connection if binding fails, times out, or is
            // cancelled, without affecting any newer connection established after a disconnect().
            disconnect(connection)
            throw e
        }
    }

    /** Disconnects from the [IProjectedService] by unbinding it. */
    public fun disconnect() {
        disconnect(targetConnection = null)
    }

    private fun disconnect(
        targetConnection: ServiceConnection?,
        targetBinder: IBinder? = null,
    ) {
        val connection: ServiceConnection
        val binder: IBinder?
        val deathRecipient: IBinder.DeathRecipient?
        val deferred: CompletableDeferred<IProjectedService>?
        synchronized(lock) {
            connection = projectedServiceConnection ?: return
            val isTargetConnection = targetConnection == null || connection === targetConnection
            val isTargetBinder = targetBinder == null || projectedServiceBinder === targetBinder
            if (!isTargetConnection || !isTargetBinder) return
            // Atomically claim and clear the active connection and binder references under lock so
            // concurrent disconnect() and binderDied() calls unbind and unlink at most once.
            binder = projectedServiceBinder
            deathRecipient = projectedServiceDeathRecipient
            deferred = onProjectedServiceConnectedDeferred
            projectedServiceBinder = null
            projectedServiceDeathRecipient = null
            projectedServiceConnection = null
            onProjectedServiceConnectedDeferred = null
            _isServiceConnected.tryEmit(false)
        }
        // If disconnect() was called before onServiceConnected ran, cancel the pending deferred so
        // connect() aborts immediately instead of waiting for SERVICE_CONNECTION_TIMEOUT_MS.
        deferred?.cancel(
            CancellationException("Projected service disconnected before binding completed.")
        )
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) {
            // Service may have already been unbound by the OS or disconnected during standby.
        }
        if (deathRecipient != null) {
            try {
                binder?.unlinkToDeath(deathRecipient, /* flags= */ 0)
            } catch (_: NoSuchElementException) {
                // Death link may have already been unlinked or cleaned up if the binder died.
            }
        }
    }

    private companion object {
        /**
         * Timeout for binding to the projected service. Selected value is commonly used, e.g. by
         * the JUnit rules for testing services.
         */
        private const val SERVICE_CONNECTION_TIMEOUT_MS = 5000L
    }
}
