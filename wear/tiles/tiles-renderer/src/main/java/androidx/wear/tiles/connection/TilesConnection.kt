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
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.tiles.ResourcesCallback
import androidx.wear.tiles.ResourcesData
import androidx.wear.tiles.ResourcesRequestData
import androidx.wear.tiles.TileCallback
import androidx.wear.tiles.TileData
import androidx.wear.tiles.TileRequestData
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.proto.RequestProto
import androidx.wear.tiles.proto.ResourceProto
import androidx.wear.tiles.proto.TileProto
import androidx.wear.tiles.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.lang.IllegalArgumentException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Connection to a tile provider.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TilesConnection(
    context: Context,
    componentName: ComponentName,
    coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher
) {
    internal companion object {
        @VisibleForTesting
        internal const val TIMEOUT = 30000L // 30s
    }

    private val connectionBinder = TilesConnectionBinder(
        context, componentName, coroutineScope,
        coroutineDispatcher
    )

    public suspend fun getApiVersion(): Int {
        return connectionBinder.runWithTilesConnection { connection ->
            connection.apiVersion
        }
    }

    public suspend fun tileRequest(requestParams: RequestProto.TileRequest): TileBuilders.Tile {
        val requestData =
            TileRequestData(requestParams.toByteArray(), TileRequestData.VERSION_PROTOBUF)

        return connectionBinder.runWithTilesConnection { tileProvider ->
            val job = async(coroutineDispatcher) {
                suspendCancellableCoroutine<TileBuilders.Tile> { continuation ->
                    tileProvider.onTileRequest(-1, requestData, TileResultCallback(continuation))
                }
            }

            withTimeout(TIMEOUT) {
                job.await()
            }
        }
    }

    public suspend fun resourcesRequest(
        requestParams: RequestProto.ResourcesRequest
    ): ResourceBuilders.Resources {
        val requestData =
            ResourcesRequestData(requestParams.toByteArray(), ResourcesRequestData.VERSION_PROTOBUF)

        return connectionBinder.runWithTilesConnection { tileProvider ->
            val job = async(coroutineDispatcher) {
                suspendCancellableCoroutine<ResourceBuilders.Resources> { continuation ->
                    tileProvider.onResourcesRequest(
                        -1,
                        requestData,
                        ResourcesResultCallback(continuation)
                    )
                }
            }

            withTimeout(TIMEOUT) {
                job.await()
            }
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

    private class ResourcesResultCallback(
        private val continuation: Continuation<ResourceBuilders.Resources>
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
                        continuation.resume(ResourceBuilders.Resources.fromProto(resources))
                    } catch (ex: InvalidProtocolBufferException) {
                        continuation.resumeWithException(ex)
                    }
                }
            }
        }
    }
}
