/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.connection

import android.content.ComponentName
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourcesCallback
import androidx.wear.tiles.ResourcesData
import androidx.wear.tiles.ResourcesRequestData
import androidx.wear.tiles.TileCallback
import androidx.wear.tiles.TileData
import androidx.wear.tiles.TileRequestData
import androidx.wear.tiles.TileAddEventData
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileEnterEventData
import androidx.wear.tiles.TileLeaveEventData
import androidx.wear.tiles.TileProvider
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileRemoveEventData
import androidx.wear.tiles.client.TileClient
import androidx.wear.protolayout.proto.ResourceProto
import androidx.wear.tiles.proto.TileProto
import androidx.wear.protolayout.protobuf.InvalidProtocolBufferException
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.lang.IllegalArgumentException
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of [TileClient] which can connect to a `TileService` in either the local
 * process, or in a remote app.
 *
 * This implementation will only stay connected for as long as required. Each call will cause this
 * client to connect to the `TileService`, and call the specified remote method. It will then
 * disconnect again after one second of inactivity (so calls in quick succession will share the
 * same binder).
 *
 * Note that there is a timeout of 10s when connecting to the `TileService`, and a timeout of 30s
 * for [requestTile] and [requestResources] to return a payload.
 */
public class DefaultTileClient : TileClient {
    internal companion object {
        @VisibleForTesting
        internal const val TIMEOUT_MILLIS = 30000L // 30s
        private const val TILE_ID = -1

        // These don't contain a useful payload right now, so just pre-build them.
        private val TILE_ADD_EVENT = TileAddEventData(
            EventBuilders.TileAddEvent.Builder().build().toProto().toByteArray(),
            TileAddEventData.VERSION_PROTOBUF
        )
        private val TILE_REMOVE_EVENT = TileRemoveEventData(
            EventBuilders.TileRemoveEvent.Builder().build().toProto().toByteArray(),
            TileRemoveEventData.VERSION_PROTOBUF
        )
        private val TILE_ENTER_EVENT = TileEnterEventData(
            EventBuilders.TileEnterEvent.Builder().build().toProto().toByteArray(),
            TileEnterEventData.VERSION_PROTOBUF
        )
        private val TILE_LEAVE_EVENT = TileLeaveEventData(
            EventBuilders.TileLeaveEvent.Builder().build().toProto().toByteArray(),
            TileLeaveEventData.VERSION_PROTOBUF
        )
    }

    private val coroutineScope: CoroutineScope
    private val coroutineDispatcher: CoroutineDispatcher
    private val connectionBinder: TilesConnectionBinder

    /**
     * Build an instance of [DefaultTileClient] for use with a coroutine dispatcher.
     *
     * @param context The application context to use when binding to the [TileService].
     * @param componentName The [ComponentName] of the [TileService] to bind to.
     * @param coroutineScope A [CoroutineScope] to use when dispatching calls to the
     *   [TileService]. Cancelling the passed [CoroutineScope] will also cancel any pending
     *   work in this class.
     * @param coroutineDispatcher A [CoroutineDispatcher] to use when dispatching work from this
     *   class.
     */
    public constructor(
        context: Context,
        componentName: ComponentName,
        coroutineScope: CoroutineScope,
        coroutineDispatcher: CoroutineDispatcher
    ) {
        this.coroutineScope = coroutineScope
        this.coroutineDispatcher = coroutineDispatcher
        this.connectionBinder =
            TilesConnectionBinder(context, componentName, coroutineScope, coroutineDispatcher)
    }

    /**
     * Build an instance of [DefaultTileClient] for use with a given [Executor].
     *
     * @param context The application context to use when binding to the [TileService].
     * @param componentName The [ComponentName] of the [TileService] to bind to.
     * @param executor An [Executor] to use when dispatching calls to the [TileService].
     */
    public constructor(context: Context, componentName: ComponentName, executor: Executor) {
        this.coroutineDispatcher = executor.asCoroutineDispatcher()
        this.coroutineScope = CoroutineScope(this.coroutineDispatcher)
        this.connectionBinder =
            TilesConnectionBinder(context, componentName, coroutineScope, coroutineDispatcher)
    }

    public override fun requestApiVersion(): ListenableFuture<Int> {
        return runForFuture { it.apiVersion }
    }

    public override fun requestTile(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        return runForFuture {
            val params = TileRequestData(
                requestParams.toProto().toByteArray(),
                TileRequestData.VERSION_PROTOBUF
            )

            suspendCancellableCoroutine<TileBuilders.Tile> { continuation ->
                it.onTileRequest(TILE_ID, params, TileResultCallback(continuation))
            }
        }
    }

    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    public override fun requestResources(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources> {
        return runForFuture {
            val params = ResourcesRequestData(
                requestParams.toProto().toByteArray(),
                ResourcesRequestData.VERSION_PROTOBUF
            )

            suspendCancellableCoroutine { continuation ->
                it.onResourcesRequest(
                    TILE_ID, params,
                    ResourcesResultCallback(continuation)
                )
            }
        }
    }

    public override fun sendOnTileAddedEvent(): ListenableFuture<Void?> {
        return runForFuture {
            it.onTileAddEvent(TILE_ADD_EVENT)
            null
        }
    }

    public override fun sendOnTileEnterEvent(): ListenableFuture<Void?> {
        return runForFuture {
            it.onTileEnterEvent(TILE_ENTER_EVENT)
            null
        }
    }

    public override fun sendOnTileLeaveEvent(): ListenableFuture<Void?> {
        return runForFuture {
            it.onTileLeaveEvent(TILE_LEAVE_EVENT)
            null
        }
    }

    public override fun sendOnTileRemovedEvent(): ListenableFuture<Void?> {
        return runForFuture {
            it.onTileRemoveEvent(TILE_REMOVE_EVENT)
            null
        }
    }

    private class TileResultCallback(
        private val continuation: Continuation<TileBuilders.Tile>
    ) : TileCallback.Stub() {
        override fun updateTileData(tileData: TileData?) {
            when {
                tileData == null -> {
                    continuation.resumeWithException(
                        IllegalArgumentException("Returned Tile Data was null")
                    )
                }
                tileData.version != TileData.VERSION_PROTOBUF -> {
                    continuation.resumeWithException(
                        IllegalArgumentException(
                            "Returned Tile Data " +
                                "has unexpected version (" + tileData.version + ")"
                        )
                    )
                }
                else -> {
                    try {
                        val tile = TileProto.Tile.parseFrom(tileData.contents)
                        continuation.resume(TileBuilders.Tile.fromProto(tile))
                    } catch (ex: InvalidProtocolBufferException) {
                        continuation.resumeWithException(ex)
                    }
                }
            }
        }
    }

    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types
    private class ResourcesResultCallback(
        private val continuation: Continuation<androidx.wear.tiles.ResourceBuilders.Resources>
    ) : ResourcesCallback.Stub() {
        override fun updateResources(resourcesData: ResourcesData?) {
            when {
                resourcesData == null -> {
                    continuation.resumeWithException(
                        IllegalArgumentException("Returned ResourcesData was null")
                    )
                }
                resourcesData.version != ResourcesData.VERSION_PROTOBUF -> {
                    continuation.resumeWithException(
                        IllegalArgumentException(
                            "Returned Resources " +
                                "Data has unexpected version (" + resourcesData.version + ")"
                        )
                    )
                }
                else -> {
                    try {
                        val resources = ResourceProto.Resources.parseFrom(resourcesData.contents)
                        continuation.resume(
                            androidx.wear.tiles.ResourceBuilders.Resources.fromProto(resources))
                    } catch (ex: InvalidProtocolBufferException) {
                        continuation.resumeWithException(ex)
                    }
                }
            }
        }
    }

    private fun <T> runForFuture(
        fn: suspend (TileProvider) -> T
    ): ListenableFuture<T> {
        val future = ResolvableFuture.create<T>()

        coroutineScope.launch(coroutineDispatcher) {
            try {
                withTimeout(TIMEOUT_MILLIS) {
                    connectionBinder.runWithTilesConnection {
                        future.set(fn(it))
                    }
                }
            } catch (ex: Exception) {
                future.setException(ex)
            }
        }

        return future
    }
}
