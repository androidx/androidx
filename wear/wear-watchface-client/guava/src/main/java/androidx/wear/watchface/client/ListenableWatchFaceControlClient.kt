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

package androidx.wear.watchface.client

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.complications.data.ComplicationData
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.watchface.client.WatchFaceControlClient.ServiceNotBoundException
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * [ListenableFuture]-based compatibility wrapper around [WatchFaceControlClient]'s suspending
 * methods. This class is open to allow mocking.
 */
public open class ListenableWatchFaceControlClient(
    private val watchFaceControlClient: WatchFaceControlClient
) : WatchFaceControlClient {
    override fun getInteractiveWatchFaceClientInstance(
        instanceId: String
    ): InteractiveWatchFaceClient? =
        watchFaceControlClient.getInteractiveWatchFaceClientInstance(instanceId)

    public companion object {
        private val immediateCoroutineScope = CoroutineScope(
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    block.run()
                }
            }
        )

        /**
         * Returns a [ListenableFuture] for a [ListenableWatchFaceControlClient] which attempts to
         * connect to a watch face in the android package [watchFacePackageName].
         * Resolves as [ServiceNotBoundException] if the watch face control service can not
         * be bound.
         */
        @SuppressLint("NewApi") // For ACTION_WATCHFACE_CONTROL_SERVICE
        @JvmStatic
        public fun createWatchFaceControlClient(
            /** Calling application's [Context]. */
            context: Context,
            /** The name of the package containing the watch face control service to bind to. */
            watchFacePackageName: String
        ): ListenableFuture<ListenableWatchFaceControlClient> {
            val traceEvent = AsyncTraceEvent(
                "ListenableWatchFaceControlClient.createWatchFaceControlClient"
            )
            val future = ResolvableFuture.create<ListenableWatchFaceControlClient>()
            immediateCoroutineScope.launch {
                try {
                    future.set(
                        ListenableWatchFaceControlClient(
                            WatchFaceControlClient.createWatchFaceControlClient(
                                context,
                                watchFacePackageName
                            )
                        )
                    )
                } catch (e: Exception) {
                    future.setException(e)
                } finally {
                    traceEvent.close()
                }
            }
            return future
        }
    }

    override fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): HeadlessWatchFaceClient? =
        watchFaceControlClient.createHeadlessWatchFaceClient(
            watchFaceName,
            deviceConfig,
            surfaceWidth,
            surfaceHeight
        )

    /**
     * [ListenableFuture] wrapper around
     * [WatchFaceControlClient.getOrCreateInteractiveWatchFaceClient].
     * This is open to allow mocking.
     */
    public open fun listenableGetOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): ListenableFuture<InteractiveWatchFaceClient> = immediateCoroutineScope.async {
        watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
            id,
            deviceConfig,
            watchUiState,
            userStyle,
            idToComplicationData
        )
    }.asListenableFuture()

    override suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient =
        watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
            id,
            deviceConfig,
            watchUiState,
            userStyle,
            idToComplicationData
        )

    override fun getEditorServiceClient(): EditorServiceClient =
        watchFaceControlClient.getEditorServiceClient()

    override fun close() {
        watchFaceControlClient.close()
    }
}
