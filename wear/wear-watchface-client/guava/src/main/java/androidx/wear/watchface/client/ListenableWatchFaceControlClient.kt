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
import androidx.wear.watchface.style.UserStyleData
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
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
        internal fun createImmediateCoroutineScope() = CoroutineScope(
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    block.run()
                }
            }
        )

        /**
         * Launches a coroutine with a new scope and returns a future that correctly handles
         * cancellation.
         */
        // TODO(flerda): Move this to a location where it can be shared.
        internal fun <T> launchFutureCoroutine(
            traceTag: String,
            block: suspend CoroutineScope.() -> T
        ): ListenableFuture<T> {
            val traceEvent = AsyncTraceEvent(traceTag)
            val future = ResolvableFuture.create<T>()
            val coroutineScope = createImmediateCoroutineScope()
            coroutineScope.launch {
                // Propagate future cancellation.
                future.addListener(
                    {
                        if (future.isCancelled) {
                            coroutineScope.cancel()
                        }
                    },
                    { runner -> runner.run() }
                )
                try {
                    future.set(block())
                } catch (e: Exception) {
                    future.setException(e)
                } finally {
                    traceEvent.close()
                }
            }
            return future
        }

        /**
         * Returns a [ListenableFuture] for a [ListenableWatchFaceControlClient] which attempts to
         * connect to a watch face in the android package [watchFacePackageName].
         * Resolves as [ServiceNotBoundException] if the watch face control service can not
         * be bound.
         *
         * Note the returned future may resolve immediately on the calling thread or it may resolve
         * asynchronously when the service is connected on a background thread.
         *
         * @param context Calling application's [Context].
         * @param watchFacePackageName Name of the package containing the watch face control
         * service to bind to.
         * @return [ListenableFuture]<[ListenableWatchFaceControlClient]> which on success resolves
         * to a [ListenableWatchFaceControlClient] or throws a [ServiceNotBoundException] if the
         * watch face control service can not be bound.
         */
        @SuppressLint("NewApi") // For ACTION_WATCHFACE_CONTROL_SERVICE
        @JvmStatic
        public fun createWatchFaceControlClient(
            context: Context,
            watchFacePackageName: String
        ): ListenableFuture<ListenableWatchFaceControlClient> =
            launchFutureCoroutine(
                "ListenableWatchFaceControlClient.createWatchFaceControlClient",
            ) {
                ListenableWatchFaceControlClient(
                    WatchFaceControlClient.createWatchFaceControlClient(
                        context,
                        watchFacePackageName
                    )
                )
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
        userStyle: UserStyleData?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): ListenableFuture<InteractiveWatchFaceClient> =
        launchFutureCoroutine(
            "ListenableWatchFaceControlClient.listenableGetOrCreateInteractiveWatchFaceClient",
        ) {
            watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
                id,
                deviceConfig,
                watchUiState,
                userStyle,
                idToComplicationData
            )
        }

    override suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: UserStyleData?,
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

    override fun getDefaultComplicationProviderPoliciesAndType(
        watchFaceName: ComponentName
    ): Map<Int, DefaultComplicationProviderPolicyAndType> =
        watchFaceControlClient.getDefaultComplicationProviderPoliciesAndType(watchFaceName)

    override fun close() {
        watchFaceControlClient.close()
    }
}
