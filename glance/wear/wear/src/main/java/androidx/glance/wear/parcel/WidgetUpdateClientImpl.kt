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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.util.Log
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WearWidgetUpdateRequest
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.legacy.TileUpdateRequestData
import androidx.glance.wear.parcel.legacy.TileUpdateRequesterService
import androidx.glance.wear.proto.legacy.TileUpdateRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

internal class WidgetUpdateClientImpl(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WidgetUpdateClient {
    private val binderMutex = Mutex()

    // Writes must be guarded by binderMutex.
    @Volatile
    private var legacyBinder:
        WidgetUpdateBinder<TileUpdateRequesterService, PullUpdateIdentifier>? =
        null

    // Writes must be guarded by binderMutex.
    @Volatile
    private var widgetUpdateBinder:
        WidgetUpdateBinder<IWearWidgetUpdateRequester, PushUpdateData>? =
        null

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    override fun sendUpdateBroadcast(context: Context, provider: ComponentName) {
        val intent =
            Intent(ACTION_REQUEST_TILE_UPDATE_BROADCAST_LEGACY).apply {
                putExtra(Intent.EXTRA_COMPONENT_NAME, provider)
            }
        context.sendBroadcast(intent)
    }

    override fun requestUpdate(
        context: Context,
        provider: ComponentName,
        instanceId: WidgetInstanceId?,
    ) {
        val pullId = PullUpdateIdentifier(provider, instanceId)
        scope.launch {
            try {
                withTimeout(UPDATE_TIMEOUT) {
                    val binder =
                        legacyBinder
                            ?: binderMutex.withLock {
                                legacyBinder
                                    ?: createLegacyBinder(context.applicationContext, dispatcher)
                                        .also { legacyBinder = it }
                            }
                    binder.requestUpdate(pullId)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to request widget update", ex)
            }
        }
    }

    override suspend fun pushUpdate(
        context: Context,
        updateRequest: WearWidgetUpdateRequest,
        rawContent: WearWidgetRawContent,
    ) {
        val pushData = PushUpdateData(updateRequest, rawContent)
        withTimeout(UPDATE_TIMEOUT) {
            val binder =
                widgetUpdateBinder
                    ?: binderMutex.withLock {
                        widgetUpdateBinder
                            ?: createBinder(context.applicationContext, dispatcher).also {
                                widgetUpdateBinder = it
                            }
                    }
            binder.requestUpdate(pushData)
        }
    }

    internal companion object {
        const val TAG = "WidgetUpdateClientImpl"
        const val ACTION_BIND_UPDATE_REQUESTER_LEGACY =
            "androidx.wear.tiles.action.BIND_UPDATE_REQUESTER"
        const val ACTION_BIND_UPDATE_REQUESTER = "androidx.glance.wear.action.BIND_UPDATE_REQUESTER"

        data class PushUpdateData(
            val request: WearWidgetUpdateRequest,
            val rawContent: WearWidgetRawContent,
        )

        data class PullUpdateIdentifier(
            val componentName: ComponentName,
            val instanceId: WidgetInstanceId?,
        )

        private val UPDATE_TIMEOUT = 10.seconds

        /** Intent action to broadcast debugging update requests. */
        internal const val ACTION_REQUEST_TILE_UPDATE_BROADCAST_LEGACY =
            "androidx.wear.tiles.action.REQUEST_TILE_UPDATE"

        /** Create a binder that can be used to request updates from the legacy Tile Renderer. */
        fun createLegacyBinder(
            context: Context,
            dispatcher: CoroutineDispatcher,
        ): WidgetUpdateBinder<TileUpdateRequesterService, PullUpdateIdentifier> =
            WidgetUpdateBinder(
                context = context,
                action = ACTION_BIND_UPDATE_REQUESTER_LEGACY,
                asInterface = { TileUpdateRequesterService.Stub.asInterface(it) },
                dispatcher = dispatcher,
                sendRequest = { service, pullData ->
                    try {
                        val requestProto = TileUpdateRequest(tile_id = pullData.instanceId?.id)
                        val request =
                            TileUpdateRequestData(
                                requestProto.encode(),
                                TileUpdateRequestData.VERSION_1,
                            )
                        service.requestUpdate(pullData.componentName, request)
                    } catch (ex: RemoteException) {
                        Log.e(TAG, "while requesting widget update", ex)
                    }
                },
            )

        /** Create a binder that can be used to push updates to the Tile Renderer. */
        fun createBinder(
            context: Context,
            dispatcher: CoroutineDispatcher,
        ): WidgetUpdateBinder<IWearWidgetUpdateRequester, PushUpdateData> =
            WidgetUpdateBinder(
                context = context,
                action = ACTION_BIND_UPDATE_REQUESTER,
                asInterface = { IWearWidgetUpdateRequester.Stub.asInterface(it) },
                dispatcher = dispatcher,
                sendRequest = { service, pushData ->
                    suspendCancellableCoroutine { continuation ->
                        val contCallback = ContinuationCallback(continuation)

                        // The service doesn't have an API to cancel in-flight requests,
                        // so we don't register a cancellation handler here. If the coroutine
                        // is cancelled, suspendCancellableCoroutine automatically handles
                        // throwing CancellationException to the caller.
                        try {
                            service.requestUpdate(
                                pushData.request.toParcel(),
                                pushData.rawContent.toParcel(),
                                contCallback,
                            )
                        } catch (ex: RemoteException) {
                            Log.w(
                                TAG,
                                "while pushing widget update for instanceId: ${pushData.request.instanceId.flattenToString()}",
                                ex,
                            )
                            contCallback.onError(
                                IWearWidgetUpdateRequester.UPDATE_ERROR_CODE_INTERNAL_ERROR,
                                ex.message,
                            )
                        }
                    }
                },
            )

        private class ContinuationCallback(
            private val continuation: CancellableContinuation<Unit>
        ) : IExecutionCallback.Stub() {
            override fun getInterfaceVersion(): Int = VERSION

            override fun onSuccess() {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }

            override fun onError(errorCode: Int, errorMessage: String?) {
                if (continuation.isActive) {
                    val exception =
                        when (errorCode) {
                            IWearWidgetUpdateRequester.UPDATE_ERROR_CODE_INVALID_REQUEST_ERROR ->
                                IllegalArgumentException(errorMessage)

                            else ->
                                RuntimeException("Update failed (code=$errorCode): $errorMessage")
                        }
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}
