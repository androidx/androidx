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

import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.ResolvableFuture
import androidx.core.util.Consumer
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFaceRuntimeService
import androidx.wear.watchface.client.WatchFaceControlClient.Companion.createWatchFaceControlClient
import androidx.wear.watchface.client.WatchFaceControlClient.ServiceNotBoundException
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.utility.AsyncTraceEvent
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
        internal fun createImmediateCoroutineScope() =
            CoroutineScope(
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
         * connect to a watch face in the android package [watchFacePackageName]. Resolves as
         * [ServiceNotBoundException] if the watch face control service can not be bound.
         *
         * Note the returned future may resolve immediately on the calling thread or it may resolve
         * asynchronously when the service is connected on a background thread.
         *
         * @param context Calling application's [Context].
         * @param watchFacePackageName Name of the package containing the watch face control service
         *   to bind to.
         * @return [ListenableFuture]<[ListenableWatchFaceControlClient]> which on success resolves
         *   to a [ListenableWatchFaceControlClient] or throws a [ServiceNotBoundException] if the
         *   watch face control service can not be bound.
         */
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

        /**
         * Similar [createWatchFaceControlClient] this constructs a [WatchFaceControlClient] which
         * attempts to connect to the watch face runtime in the android package
         * [runtimePackageName].
         *
         * A watch face runtime is a special type of watch face, which renders a watch face
         * described by resources in another package [resourceOnlyWatchFacePackageName].
         *
         * Note only one watch face definition per resource only watch face package is supported.
         *
         * Currently Wear OS only supports the runtime for the Android Watch Face Format (see
         * https://developer.android.com/training/wearables/wff for more details).
         *
         * @param context Calling application's [Context].
         * @param runtimePackageName The name of the package containing the watch face runtime's
         *   control service to bind to.
         * @param resourceOnlyWatchFacePackageName The name of the package from which to load the
         *   resource only watch face. This is exposed to the runtime via the
         *   `resourceOnlyWatchFacePackageName` parameter passed to
         *   [WatchFaceRuntimeService.createUserStyleSchema],
         *   [WatchFaceRuntimeService.createComplicationSlotsManager],
         *   [WatchFaceRuntimeService.createUserStyleFlavors] and
         *   [WatchFaceRuntimeService.createWatchFace]).
         * @return [ListenableFuture]<[ListenableWatchFaceControlClient]> which on success resolves
         *   to a [ListenableWatchFaceControlClient] or throws a [ServiceNotBoundException] if the
         *   watch face control service can not be bound.
         */
        @JvmStatic
        public fun createWatchFaceRuntimeControlClientAsync(
            context: Context,
            runtimePackageName: String,
            resourceOnlyWatchFacePackageName: String
        ): ListenableFuture<ListenableWatchFaceControlClient> =
            launchFutureCoroutine(
                "ListenableWatchFaceControlClient.createWatchFaceRuntimeControlClient",
            ) {
                ListenableWatchFaceControlClient(
                    WatchFaceControlClient.createWatchFaceRuntimeControlClient(
                        context,
                        runtimePackageName,
                        resourceOnlyWatchFacePackageName
                    )
                )
            }
    }

    @Suppress("DEPRECATION")
    @Deprecated("createHeadlessWatchFaceClient without an id is deprecated")
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

    override fun createHeadlessWatchFaceClient(
        id: String,
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): HeadlessWatchFaceClient? =
        watchFaceControlClient.createHeadlessWatchFaceClient(
            id,
            watchFaceName,
            deviceConfig,
            surfaceWidth,
            surfaceHeight
        )

    /**
     * [ListenableFuture] wrapper around
     * [WatchFaceControlClient.getOrCreateInteractiveWatchFaceClient]. This is open to allow
     * mocking.
     *
     * @param id The Id of the interactive instance to get or create.
     * @param deviceConfig The [DeviceConfig] of the interactive instance (only used when creating)
     * @param watchUiState The initial [WatchUiState] for the wearable.
     * @param userStyle Optional [UserStyleData] to apply to the instance (whether or not it's
     *   created). If `null` then the pre-existing user style is preserved (if the instance is
     *   created this will be the [androidx.wear.watchface.style.UserStyleSchema]'s default).
     * @param slotIdToComplicationData The initial [androidx.wear.watchface.ComplicationSlot] data,
     *   or `null` if unavailable.
     * @return a [ListenableFuture] for the [InteractiveWatchFaceClient].
     */
    @Suppress("AsyncSuffixFuture")
    @Deprecated(
        "Use an overload that specifies Consumer<String>",
        ReplaceWith(
            "listenableGetOrCreateInteractiveWatchFaceClient(" +
                "String, DeviceConfig, WatchUiState, UserStyleData?, Map<Int, ComplicationData>?," +
                " Executor, Consumer<String>)"
        )
    )
    public open fun listenableGetOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): ListenableFuture<InteractiveWatchFaceClient> =
        launchFutureCoroutine(
            "ListenableWatchFaceControlClient.listenableGetOrCreateInteractiveWatchFaceClient",
        ) {
            @Suppress("Deprecation")
            watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
                id,
                deviceConfig,
                watchUiState,
                userStyle,
                slotIdToComplicationData
            )
        }

    /**
     * [ListenableFuture] wrapper around
     * [WatchFaceControlClient.getOrCreateInteractiveWatchFaceClient]. This is open to allow
     * mocking.
     *
     * @param id The Id of the interactive instance to get or create.
     * @param deviceConfig The [DeviceConfig] of the interactive instance (only used when creating)
     * @param watchUiState The initial [WatchUiState] for the wearable.
     * @param userStyle Optional [UserStyleData] to apply to the instance (whether or not it's
     *   created). If `null` then the pre-existing user style is preserved (if the instance is
     *   created this will be the [androidx.wear.watchface.style.UserStyleSchema]'s default).
     * @param slotIdToComplicationData The initial [androidx.wear.watchface.ComplicationSlot] data,
     *   or `null` if unavailable.
     * @param previewImageUpdateRequestedExecutor The [Executor] on which to run
     *   [previewImageUpdateRequestedListener] if the watch face calls
     *   [Renderer.sendPreviewImageNeedsUpdateRequest].
     * @param previewImageUpdateRequestedListener The [Consumer] fires when the watch face calls
     *   [Renderer.sendPreviewImageNeedsUpdateRequest], indicating that it now looks visually
     *   different. The string passed to the [Consumer] is the ID of the watch face (see [id] passed
     *   into [getOrCreateInteractiveWatchFaceClient]) requesting the update. This will usually
     *   match the current watch face but it could also be from a previous watch face if
     *   [InteractiveWatchFaceClient.updateWatchFaceInstance] is called shortly after
     *   [Renderer.sendPreviewImageNeedsUpdateRequest]. The [Consumer] should Schedule creation of a
     *   headless instance to render a new preview image for the instanceId. This is likely an
     *   expensive operation and should be rate limited.
     * @return a [ListenableFuture] for the [InteractiveWatchFaceClient].
     */
    @Suppress("AsyncSuffixFuture")
    public open fun listenableGetOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?,
        previewImageUpdateRequestedExecutor: Executor,
        previewImageUpdateRequestedListener: Consumer<String>
    ): ListenableFuture<InteractiveWatchFaceClient> =
        launchFutureCoroutine(
            "ListenableWatchFaceControlClient.listenableGetOrCreateInteractiveWatchFaceClient",
        ) {
            watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
                id,
                deviceConfig,
                watchUiState,
                userStyle,
                slotIdToComplicationData,
                previewImageUpdateRequestedExecutor,
                previewImageUpdateRequestedListener
            )
        }

    @Deprecated(
        "Use an overload that specifies a Consumer<String>",
        replaceWith =
            ReplaceWith(
                "getOrCreateInteractiveWatchFaceClient(String, DeviceConfig, WatchUiState," +
                    " UserStyleData?, Map<Int, ComplicationData>?, Executor, " +
                    "Consumer<String>)"
            )
    )
    @Suppress("deprecation")
    override suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient =
        watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
            id,
            deviceConfig,
            watchUiState,
            userStyle,
            slotIdToComplicationData
        )

    override suspend fun getOrCreateInteractiveWatchFaceClient(
        instanceId: String,
        deviceConfig: DeviceConfig,
        watchUiState: WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?,
        previewImageUpdateRequestedExecutor: Executor,
        previewImageUpdateRequestedListener: Consumer<String>
    ): InteractiveWatchFaceClient =
        watchFaceControlClient.getOrCreateInteractiveWatchFaceClient(
            instanceId,
            deviceConfig,
            watchUiState,
            userStyle,
            slotIdToComplicationData,
            previewImageUpdateRequestedExecutor,
            previewImageUpdateRequestedListener
        )

    override fun hasComplicationDataCache(): Boolean =
        watchFaceControlClient.hasComplicationDataCache()

    override fun getEditorServiceClient(): EditorServiceClient =
        watchFaceControlClient.getEditorServiceClient()

    @Deprecated("Use the WatchFaceMetadataClient instead.")
    @Suppress("DEPRECATION")
    override fun getDefaultComplicationDataSourcePoliciesAndType(
        watchFaceName: ComponentName
    ): Map<Int, DefaultComplicationDataSourcePolicyAndType> =
        watchFaceControlClient.getDefaultComplicationDataSourcePoliciesAndType(watchFaceName)

    override fun close() {
        watchFaceControlClient.close()
    }
}
