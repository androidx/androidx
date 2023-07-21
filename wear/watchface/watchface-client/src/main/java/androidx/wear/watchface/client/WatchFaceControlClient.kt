/*
 * Copyright 2020 The Android Open Source Project
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
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.DefaultProviderPoliciesParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Connects to a watch face's WatchFaceControlService which allows the user to control the watch
 * face.
 */
public interface WatchFaceControlClient : AutoCloseable {

    public companion object {
        /**
         * Constructs a [WatchFaceControlClient] which attempts to connect to a watch face in the
         * android package [watchFacePackageName].
         *
         * @param context Calling application's [Context].
         * @param watchFacePackageName The name of the package containing the watch face control
         *   service to bind to.
         * @return The [WatchFaceControlClient] if there is one.
         * @throws [ServiceNotBoundException] if the watch face control service can not be bound or
         *   a [ServiceStartFailureException] if the watch face dies during startup.
         */
        @JvmStatic
        @Throws(ServiceNotBoundException::class, ServiceStartFailureException::class)
        public suspend fun createWatchFaceControlClient(
            context: Context,
            watchFacePackageName: String
        ): WatchFaceControlClient =
            createWatchFaceControlClientImpl(
                context,
                Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                    setPackage(watchFacePackageName)
                }
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public suspend fun createWatchFaceControlClientImpl(
            context: Context,
            intent: Intent
        ): WatchFaceControlClient {
            val deferredService = CompletableDeferred<IWatchFaceControlService>()
            val traceEvent = AsyncTraceEvent("WatchFaceControlClientImpl.bindService")
            val serviceConnection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        traceEvent.close()
                        deferredService.complete(IWatchFaceControlService.Stub.asInterface(binder))
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        // Note if onServiceConnected is called first completeExceptionally will do
                        // nothing because the CompletableDeferred is already completed.
                        traceEvent.close()
                        deferredService.completeExceptionally(ServiceStartFailureException())
                    }
                }
            if (!BindHelper.bindService(context, intent, serviceConnection)) {
                traceEvent.close()
                throw ServiceNotBoundException()
            }
            return WatchFaceControlClientImpl(context, deferredService.await(), serviceConnection)
        }
    }

    /** Exception thrown by [createWatchFaceControlClient] if the remote service can't be bound. */
    public class ServiceNotBoundException : Exception()

    /** Exception thrown by [WatchFaceControlClient] methods if the service dies during start up. */
    public class ServiceStartFailureException(message: String = "") : Exception(message)

    /**
     * Returns the [InteractiveWatchFaceClient] for the given instance id, or null if no such
     * instance exists.
     *
     * When finished call [InteractiveWatchFaceClient.close] to release resources.
     *
     * @param instanceId The name of the interactive watch face instance to retrieve
     * @return The [InteractiveWatchFaceClient] or `null` if [instanceId] is unrecognized, or
     *   [ServiceNotBoundException] if the WatchFaceControlService is not bound.
     */
    @Throws(RemoteException::class)
    public fun getInteractiveWatchFaceClientInstance(
        instanceId: String
    ): InteractiveWatchFaceClient?

    /**
     * Creates a [HeadlessWatchFaceClient] with the specified [DeviceConfig]. Screenshots made with
     * [HeadlessWatchFaceClient.renderWatchFaceToBitmap] will be `surfaceWidth` x `surfaceHeight` in
     * size.
     *
     * When finished call [HeadlessWatchFaceClient.close] to release resources.
     *
     * @param watchFaceName The [ComponentName] of the watch face to create a headless instance for
     *   must be in the same APK the WatchFaceControlClient is connected to. NB a single apk can
     *   contain multiple watch faces.
     * @param deviceConfig The hardware [DeviceConfig]
     * @param surfaceWidth The width of screen shots taken by the [HeadlessWatchFaceClient]
     * @param surfaceHeight The height of screen shots taken by the [HeadlessWatchFaceClient]
     * @return The [HeadlessWatchFaceClient] or `null` if [watchFaceName] is unrecognized.
     */
    @Deprecated(
        "Creating a headless client without a watchface ID is deprecated",
        ReplaceWith(
            "[createHeadlessWatchFaceClient(String, ComponentName, DeviceConfig, Int, Int)]"
        )
    )
    @Suppress("DEPRECATION")
    @Throws(RemoteException::class)
    public fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        @Px surfaceWidth: Int,
        @Px surfaceHeight: Int,
    ): HeadlessWatchFaceClient?

    /**
     * Creates a [HeadlessWatchFaceClient] with the specified [DeviceConfig]. Screenshots made with
     * [HeadlessWatchFaceClient.renderWatchFaceToBitmap] will be `surfaceWidth` x `surfaceHeight` in
     * size.
     *
     * When finished call [HeadlessWatchFaceClient.close] to release resources.
     *
     * @param id The ID for the requested [HeadlessWatchFaceClient], will be exposed to the watch
     *   face via [androidx.wear.watchface.WatchState.watchFaceInstanceId].
     * @param watchFaceName The [ComponentName] of the watch face to create a headless instance for
     *   must be in the same APK the WatchFaceControlClient is connected to. NB a single apk can
     *   contain multiple watch faces.
     * @param deviceConfig The hardware [DeviceConfig]
     * @param surfaceWidth The width of screen shots taken by the [HeadlessWatchFaceClient]
     * @param surfaceHeight The height of screen shots taken by the [HeadlessWatchFaceClient]
     * @return The [HeadlessWatchFaceClient] or `null` if [watchFaceName] is unrecognized.
     */
    @Throws(RemoteException::class)
    @Suppress("DEPRECATION") // createHeadlessWatchFaceClient
    public fun createHeadlessWatchFaceClient(
        id: String,
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        @Px surfaceWidth: Int,
        @Px surfaceHeight: Int,
    ): HeadlessWatchFaceClient? =
        // NB this default implementation is usually overridden below by one that does use id.
        createHeadlessWatchFaceClient(watchFaceName, deviceConfig, surfaceWidth, surfaceHeight)

    /**
     * Requests either an existing [InteractiveWatchFaceClient] with the specified [id] or schedules
     * creation of an [InteractiveWatchFaceClient] for the next time the WallpaperService creates an
     * engine.
     *
     * NOTE that currently only one [InteractiveWatchFaceClient] per process can exist at a time.
     *
     * @param id The ID for the requested [InteractiveWatchFaceClient], will be exposed to the watch
     *   face via [androidx.wear.watchface.WatchState.watchFaceInstanceId].
     * @param deviceConfig The [DeviceConfig] for the wearable.
     * @param watchUiState The initial [WatchUiState] for the wearable.
     * @param userStyle Optional [UserStyleData] to apply to the instance (whether or not it's
     *   created). If `null` then the pre-existing user style is preserved (if the instance is
     *   created this will be the [androidx.wear.watchface.style.UserStyleSchema]'s default).
     * @param slotIdToComplicationData The initial [androidx.wear.watchface.ComplicationSlot] data,
     *   or `null` if unavailable.
     * @return The [InteractiveWatchFaceClient], this should be closed when finished.
     * @throws [ServiceStartFailureException] if the watchface dies during startup.
     */
    @Throws(RemoteException::class)
    @Deprecated(
        "Use an overload that specifies Consumer<String>",
        ReplaceWith(
            "getOrCreateInteractiveWatchFaceClient(" +
                "String, DeviceConfig, WatchUiState, UserStyleData?, Map<Int, ComplicationData>?," +
                " Executor, Consumer<String>)"
        )
    )
    public suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient

    /**
     * Requests either an existing [InteractiveWatchFaceClient] with the specified [instanceId] or
     * schedules creation of an [InteractiveWatchFaceClient] for the next time the WallpaperService
     * creates an engine.
     *
     * NOTE that currently only one [InteractiveWatchFaceClient] per process can exist at a time.
     *
     * @param instanceId The ID for the requested [InteractiveWatchFaceClient], will be exposed to
     *   the watch face via [androidx.wear.watchface.WatchState.watchFaceInstanceId].
     * @param deviceConfig The [DeviceConfig] for the wearable.
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
     *   different. The string passed to the [Consumer] is the ID of the watch face (see
     *   [instanceId] passed into [getOrCreateInteractiveWatchFaceClient]) requesting the update.
     *   This will usually match the current watch face but it could also be from a previous watch
     *   face if [InteractiveWatchFaceClient.updateWatchFaceInstance] is called shortly after
     *   [Renderer.sendPreviewImageNeedsUpdateRequest]. The [Consumer] should Schedule creation of a
     *   headless instance to render a new preview image for the instanceId. This is likely an
     *   expensive operation and should be rate limited.
     * @return The [InteractiveWatchFaceClient], this should be closed when finished.
     * @throws [ServiceStartFailureException] if the watchface dies during startup.
     */
    @Throws(RemoteException::class)
    @Suppress("deprecation")
    public suspend fun getOrCreateInteractiveWatchFaceClient(
        instanceId: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?,
        previewImageUpdateRequestedExecutor: Executor,
        previewImageUpdateRequestedListener: Consumer<String>
    ): InteractiveWatchFaceClient =
        getOrCreateInteractiveWatchFaceClient(
            instanceId,
            deviceConfig,
            watchUiState,
            userStyle,
            slotIdToComplicationData
        )

    @Throws(RemoteException::class) public fun getEditorServiceClient(): EditorServiceClient

    /**
     * Returns a map of [androidx.wear.watchface.ComplicationSlot] id to the
     * [DefaultComplicationDataSourcePolicyAndType] for each
     * [androidx.wear.watchface.ComplicationSlot] in the watchface corresponding to [watchFaceName].
     * Where possible a fast path is used that doesn't need to fully construct the corresponding
     * watch face.
     *
     * @param watchFaceName The [ComponentName] of the watch face to obtain the map of
     *   [DefaultComplicationDataSourcePolicyAndType]s for. It must be in the same APK the
     *   WatchFaceControlClient is connected to. NB a single apk can contain multiple watch faces.
     */
    @Deprecated("Use the WatchFaceMetadataClient instead.")
    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    @Throws(RemoteException::class)
    public fun getDefaultComplicationDataSourcePoliciesAndType(
        watchFaceName: ComponentName
    ): Map<Int, DefaultComplicationDataSourcePolicyAndType>

    /**
     * Whether or not the watch face has a [ComplicationData] cache. Based on this the system may
     * wish to adopt a different strategy for sending complication data. E.g. sending initial blank
     * complications before fetching the real ones is not necessary.
     */
    public fun hasComplicationDataCache(): Boolean = false
}

/**
 * A pair of [DefaultComplicationDataSourcePolicy] and [ComplicationType] describing the default
 * state of a [androidx.wear.watchface.ComplicationSlot].
 *
 * @param policy The [DefaultComplicationDataSourcePolicy] for the
 *   [androidx.wear.watchface.ComplicationSlot].
 * @param type The default [ComplicationType] for the [androidx.wear.watchface.ComplicationSlot].
 */
@Deprecated("Use the WatchFaceMetadataClient instead.")
public class DefaultComplicationDataSourcePolicyAndType(
    public val policy: DefaultComplicationDataSourcePolicy,
    public val type: ComplicationType
) {
    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultComplicationDataSourcePolicyAndType

        if (policy != other.policy) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = policy.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

internal class WatchFaceControlClientImpl
internal constructor(
    private val context: Context,
    private val service: IWatchFaceControlService,
    private val serviceConnection: ServiceConnection
) : WatchFaceControlClient {
    private var closed = false

    internal companion object {
        const val TAG = "WatchFaceControlClientImpl"
    }

    override fun getInteractiveWatchFaceClientInstance(instanceId: String) =
        service.getInteractiveWatchFaceInstance(instanceId)?.let {
            InteractiveWatchFaceClientImpl(
                it,
                previewImageUpdateRequestedExecutor = null,
                previewImageUpdateRequestedListener = null
            )
        }

    @Deprecated(
        "Creating a headless client without a watchface ID is deprecated",
        ReplaceWith(
            "[createHeadlessWatchFaceClient(String, ComponentName, DeviceConfig, Int, Int)]"
        )
    )
    override fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): HeadlessWatchFaceClient? =
        TraceEvent("WatchFaceControlClientImpl.createHeadlessWatchFaceClient").use {
            requireNotClosed()
            return service
                .createHeadlessWatchFaceInstance(
                    HeadlessWatchFaceInstanceParams(
                        watchFaceName,
                        deviceConfig.asWireDeviceConfig(),
                        surfaceWidth,
                        surfaceHeight,
                        null
                    )
                )
                ?.let { HeadlessWatchFaceClientImpl(it) }
        }

    override fun createHeadlessWatchFaceClient(
        id: String,
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): HeadlessWatchFaceClient? =
        TraceEvent("WatchFaceControlClientImpl.createHeadlessWatchFaceClient").use {
            requireNotClosed()
            return service
                .createHeadlessWatchFaceInstance(
                    HeadlessWatchFaceInstanceParams(
                        watchFaceName,
                        deviceConfig.asWireDeviceConfig(),
                        surfaceWidth,
                        surfaceHeight,
                        id
                    )
                )
                ?.let { HeadlessWatchFaceClientImpl(it) }
        }

    @Deprecated(
        "Use an overload that specifies Consumer<String>",
        replaceWith =
            ReplaceWith(
                "getOrCreateInteractiveWatchFaceClient(String, DeviceConfig, WatchUiState, " +
                    "UserStyleData?, Map<Int, ComplicationData>?, Executor, " +
                    "Consumer<String>)"
            )
    )
    override suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient =
        getOrCreateInteractiveWatchFaceClientImpl(
            id,
            deviceConfig,
            watchUiState,
            userStyle,
            slotIdToComplicationData,
            previewImageUpdateRequestedExecutor = null,
            previewImageUpdateRequestedListener = null
        )

    override suspend fun getOrCreateInteractiveWatchFaceClient(
        instanceId: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?,
        previewImageUpdateRequestedExecutor: Executor,
        previewImageUpdateRequestedListener: Consumer<String>
    ): InteractiveWatchFaceClient =
        getOrCreateInteractiveWatchFaceClientImpl(
            instanceId,
            deviceConfig,
            watchUiState,
            userStyle,
            slotIdToComplicationData,
            previewImageUpdateRequestedExecutor,
            previewImageUpdateRequestedListener
        )

    private suspend fun getOrCreateInteractiveWatchFaceClientImpl(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: UserStyleData?,
        slotIdToComplicationData: Map<Int, ComplicationData>?,
        previewImageUpdateRequestedExecutor: Executor?,
        previewImageUpdateRequestedListener: Consumer<String>?
    ): InteractiveWatchFaceClient {
        requireNotClosed()
        val traceEvent =
            AsyncTraceEvent(
                "WatchFaceControlClientImpl" +
                    ".getOrCreateWallpaperServiceBackedInteractiveWatchFaceClientAsync"
            )
        return suspendCancellableCoroutine { continuation ->
            // [IWatchFaceControlService.getOrCreateInteractiveWatchFaceWCS] has an asynchronous
            // callback and it's possible the watch face might crash during start up so we register
            // a death observer.
            val deathObserver =
                IBinder.DeathRecipient {
                    continuation.resumeWithException(
                        WatchFaceControlClient.ServiceStartFailureException()
                    )
                }
            val serviceBinder = service.asBinder()
            serviceBinder.linkToDeath(deathObserver, 0)

            service
                .getOrCreateInteractiveWatchFace(
                    WallpaperInteractiveWatchFaceInstanceParams(
                        id,
                        androidx.wear.watchface.data.DeviceConfig(
                            deviceConfig.hasLowBitAmbient,
                            deviceConfig.hasBurnInProtection,
                            deviceConfig.analogPreviewReferenceTimeMillis,
                            deviceConfig.digitalPreviewReferenceTimeMillis
                        ),
                        WatchUiState(watchUiState.inAmbientMode, watchUiState.interruptionFilter),
                        userStyle?.toWireFormat() ?: UserStyleWireFormat(emptyMap()),
                        slotIdToComplicationData?.map {
                            IdAndComplicationDataWireFormat(
                                it.key,
                                it.value.asWireComplicationData()
                            )
                        },
                        null,
                        null
                    ),
                    object : IPendingInteractiveWatchFace.Stub() {
                        override fun getApiVersion() = API_VERSION

                        override fun onInteractiveWatchFaceCreated(
                            iInteractiveWatchFace: IInteractiveWatchFace
                        ) {
                            safeUnlinkToDeath(serviceBinder, deathObserver)
                            traceEvent.close()
                            continuation.resume(
                                InteractiveWatchFaceClientImpl(
                                    iInteractiveWatchFace,
                                    previewImageUpdateRequestedExecutor,
                                    previewImageUpdateRequestedListener
                                )
                            )
                        }

                        override fun onInteractiveWatchFaceCrashed(exception: CrashInfoParcel) {
                            safeUnlinkToDeath(serviceBinder, deathObserver)
                            traceEvent.close()
                            continuation.resumeWithException(
                                WatchFaceControlClient.ServiceStartFailureException(
                                    "Watchface crashed during init: $exception"
                                )
                            )
                        }
                    }
                )
                ?.let {
                    // There was an existing watchface.onInteractiveWatchFaceCreated
                    safeUnlinkToDeath(serviceBinder, deathObserver)
                    traceEvent.close()
                    continuation.resume(
                        InteractiveWatchFaceClientImpl(
                            it,
                            previewImageUpdateRequestedExecutor,
                            previewImageUpdateRequestedListener
                        )
                    )
                }
        }
    }

    internal fun safeUnlinkToDeath(binder: IBinder, deathObserver: IBinder.DeathRecipient) {
        try {
            binder.unlinkToDeath(deathObserver, 0)
        } catch (e: NoSuchElementException) {
            // This really shouldn't happen.
            Log.w(TAG, "getOrCreateInteractiveWatchFaceClient encountered", e)
        }
    }

    override fun getEditorServiceClient(): EditorServiceClient =
        TraceEvent("WatchFaceControlClientImpl.getEditorServiceClient").use {
            requireNotClosed()
            return EditorServiceClientImpl(service.editorService)
        }

    @Deprecated("Use the WatchFaceMetadataClient instead.")
    @Suppress("DEPRECATION") // DefaultComplicationDataSourcePolicyAndType
    override fun getDefaultComplicationDataSourcePoliciesAndType(
        watchFaceName: ComponentName
    ): Map<Int, DefaultComplicationDataSourcePolicyAndType> =
        TraceEvent("WatchFaceControlClientImpl.getDefaultProviderPolicies").use {
            requireNotClosed()
            if (service.apiVersion >= 2) {
                // Fast path.
                service
                    .getDefaultProviderPolicies(DefaultProviderPoliciesParams(watchFaceName))
                    .associateBy(
                        { it.id },
                        {
                            DefaultComplicationDataSourcePolicyAndType(
                                DefaultComplicationDataSourcePolicy(
                                    it.defaultProvidersToTry ?: emptyList(),
                                    it.fallbackSystemProvider,
                                    ComplicationType.fromWireType(it.defaultProviderType),
                                    ComplicationType.fromWireType(it.defaultProviderType),
                                    ComplicationType.fromWireType(it.defaultProviderType)
                                ),
                                ComplicationType.fromWireType(it.defaultProviderType)
                            )
                        }
                    )
            } else {
                // Slow backwards compatible path.
                val headlessClient =
                    createHeadlessWatchFaceClient(
                        "id",
                        watchFaceName,
                        DeviceConfig(false, false, 0, 0),
                        1,
                        1,
                    )!!

                // NB .use {} syntax doesn't compile here.
                try {
                    headlessClient.complicationSlotsState.mapValues {
                        DefaultComplicationDataSourcePolicyAndType(
                            it.value.defaultDataSourcePolicy,
                            it.value.defaultDataSourceType
                        )
                    }
                } finally {
                    headlessClient.close()
                }
            }
        }

    private fun requireNotClosed() {
        require(!closed) { "WatchFaceControlClient method called after close" }
    }

    override fun close() =
        TraceEvent("WatchFaceControlClientImpl.close").use {
            closed = true
            context.unbindService(serviceConnection)
        }

    override fun hasComplicationDataCache(): Boolean {
        if (service.apiVersion < 4) {
            return false
        }
        return service.hasComplicationCache()
    }
}
