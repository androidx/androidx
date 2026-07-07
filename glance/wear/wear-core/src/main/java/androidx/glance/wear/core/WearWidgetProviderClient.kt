/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.UserHandle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.glance.wear.parcel.ActiveWearWidgetHandleParcel
import androidx.glance.wear.parcel.IExecutionCallback
import androidx.glance.wear.parcel.IWearWidgetProvider
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Client for a [androidx.glance.wear.parcel.IWearWidgetProvider] provider.
 *
 * This class handles the connection with the provider service. Each call will create a new
 * temporary connection to the service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WearWidgetProviderClient(
    private val context: Context,
    private val componentName: ComponentName,
    private val userHandle: UserHandle? = null,
) {
    private val serviceIntent =
        Intent(WearWidgetProviderInfo.Companion.ACTION_BIND_WIDGET_PROVIDER).apply {
            component = componentName
        }

    /**
     * Call [androidx.glance.wear.parcel.IWearWidgetProvider.onAdded] on the provider and wait for
     * completion.
     */
    public suspend fun sendAddEvent(
        instanceId: WidgetInstanceId,
        @ContainerInfo.ContainerType containerType: Int,
    ): Unit = sendEvent(instanceId, containerType, "onAdded", IWearWidgetProvider::onAdded)

    /** ListenableFuture version of [sendAddEvent]. */
    public fun sendAddEventAsync(
        instanceId: WidgetInstanceId,
        @ContainerInfo.ContainerType containerType: Int,
        executor: Executor,
    ): ListenableFuture<Void?> =
        sendEventAsync(executor) { sendAddEvent(instanceId, containerType) }

    /** Call [IWearWidgetProvider.onRemoved] on the provider and wait for completion. */
    public suspend fun sendRemoveEvent(
        instanceId: WidgetInstanceId,
        @ContainerInfo.ContainerType containerType: Int,
    ): Unit = sendEvent(instanceId, containerType, "onRemoved", IWearWidgetProvider::onRemoved)

    /** ListenableFuture version of [sendRemoveEvent]. */
    public fun sendRemoveEventAsync(
        instanceId: WidgetInstanceId,
        @ContainerInfo.ContainerType containerType: Int,
        executor: Executor,
    ): ListenableFuture<Void?> =
        sendEventAsync(executor) { sendRemoveEvent(instanceId, containerType) }

    private suspend fun sendEvent(
        instanceId: WidgetInstanceId,
        @ContainerInfo.ContainerType containerType: Int,
        eventTag: String,
        eventSender: IWearWidgetProvider.(ActiveWearWidgetHandleParcel, IExecutionCallback) -> Unit,
    ) {
        withBoundService { service ->
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    Log.d(
                        TAG,
                        "$eventTag event for instanceId=${instanceId.flattenToString()} was cancelled",
                    )
                }
                eventSender.invoke(
                    service,
                    ActiveWearWidgetHandle(componentName, instanceId, containerType).toParcel(),
                    ContinuationCallback(continuation),
                )
            }
        }
    }

    private fun sendEventAsync(
        executor: Executor,
        block: suspend () -> Unit,
    ): ListenableFuture<Void?> =
        CoroutineScope(executor.asCoroutineDispatcher()).future {
            block()
            null
        }

    /** Binds to the service, runs the given [block] and then unbinds. */
    private suspend fun <T> withBoundService(block: suspend (IWearWidgetProvider) -> T): T {
        val connection = Connection()
        val isBound =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && userHandle != null) {
                context.bindServiceAsUser(
                    serviceIntent,
                    connection,
                    Context.BIND_AUTO_CREATE,
                    userHandle,
                )
            } else {
                context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
            }
        if (!isBound) {
            throw RuntimeException("Could not bind to service ${componentName.flattenToString()}")
        }

        try {
            val service = connection.serviceDeferred.await()
            return block(service)
        } finally {
            context.unbindService(connection)
        }
    }

    private class Connection : ServiceConnection {
        val serviceDeferred = CompletableDeferred<IWearWidgetProvider>()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null) {
                serviceDeferred.completeExceptionally(
                    RuntimeException("Null binder for ${name?.flattenToString()}")
                )
                return
            }
            serviceDeferred.complete(IWearWidgetProvider.Stub.asInterface(service))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceDeferred.completeExceptionally(
                RuntimeException("Service disconnected for ${name?.flattenToString()}")
            )
        }

        override fun onBindingDied(name: ComponentName?) {
            serviceDeferred.completeExceptionally(
                RuntimeException("Binding died for ${name?.flattenToString()}")
            )
        }
    }

    private class ContinuationCallback(private val continuation: Continuation<Unit>) :
        IExecutionCallback.Stub() {
        override fun onSuccess() = continuation.resume(Unit)

        // This means the provider failed to handle the event. This client takes no action on error.
        override fun onError(errorCode: Int, message: String?) = continuation.resume(Unit)

        override fun getInterfaceVersion(): Int = VERSION
    }

    private companion object {
        private const val TAG = "WearWidgetProviderClient"
    }
}
