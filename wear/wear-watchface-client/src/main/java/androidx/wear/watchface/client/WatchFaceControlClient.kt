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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.Px
import androidx.wear.complications.data.ComplicationData
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.data.UserStyleWireFormat
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
         *     service to bind to.
         * @return The [WatchFaceControlClient] if there is one.
         * @throws [ServiceNotBoundException] if the watch face control service can not be bound or
         * a [ServiceStartFailureException] if the watch face dies during startup.
         */
        @SuppressLint("NewApi") // For ACTION_WATCHFACE_CONTROL_SERVICE
        @JvmStatic
        public suspend fun createWatchFaceControlClient(
            context: Context,
            watchFacePackageName: String
        ): WatchFaceControlClient = createWatchFaceControlClientImpl(
            context,
            Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                setPackage(watchFacePackageName)
            }
        )

        internal suspend fun createWatchFaceControlClientImpl(
            context: Context,
            intent: Intent
        ): WatchFaceControlClient {
            val deferredService = CompletableDeferred<IWatchFaceControlService>()
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    deferredService.complete(IWatchFaceControlService.Stub.asInterface(binder))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    // Note if onServiceConnected is called first completeExceptionally will do
                    // nothing because the CompletableDeferred is already completed.
                    deferredService.completeExceptionally(ServiceStartFailureException())
                }
            }
            if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                throw ServiceNotBoundException()
            }
            return WatchFaceControlClientImpl(
                context,
                deferredService.await(),
                serviceConnection
            )
        }
    }

    /**
     * Exception thrown by [createWatchFaceControlClient] if the remote service can't be bound.
     */
    public class ServiceNotBoundException : Exception()

    /** Exception thrown by [WatchFaceControlClient] methods if the service dies during start up. */
    public class ServiceStartFailureException : Exception()

    /**
     * Returns the [InteractiveWatchFaceClient] for the given instance id, or null if no such
     * instance exists.
     *
     * When finished call [InteractiveWatchFaceClient.close] to release resources.
     *
     * @param instanceId The name of the interactive watch face instance to retrieve
     * @return The [InteractiveWatchFaceClient] or `null` if [instanceId] is unrecognized,
     *    or [ServiceNotBoundException] if the WatchFaceControlService is not bound.
     */
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
     *    must be in the same APK the WatchFaceControlClient is connected to. NB a single apk can
     *    contain multiple watch faces.
     * @param deviceConfig The hardware [DeviceConfig]
     * @param surfaceWidth The width of screen shots taken by the [HeadlessWatchFaceClient]
     * @param surfaceHeight The height of screen shots taken by the [HeadlessWatchFaceClient]
     * @return The [HeadlessWatchFaceClient] or `null` if [watchFaceName] is unrecognized.
     */
    public fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        @Px surfaceWidth: Int,
        @Px surfaceHeight: Int
    ): HeadlessWatchFaceClient?

    /**
     * Requests either an existing [InteractiveWatchFaceClient] with the specified [id] or
     * schedules creation of an [InteractiveWatchFaceClient] for the next time the
     * WallpaperService creates an engine.
     *
     * NOTE that currently only one [InteractiveWatchFaceClient] per process can exist at a time.
     *
     * @param id The ID for the requested [InteractiveWatchFaceClient].
     * @param deviceConfig The [DeviceConfig] for the wearable.
     * @param watchUiState The initial [WatchUiState] for the wearable.
     * @param userStyle The initial style map (see [UserStyle]), or null if the default should be
     *     used.
     * @param idToComplicationData The initial complication data, or null if unavailable.
     * @return The [InteractiveWatchFaceClient], this should be closed when finished.
     * @throws [ServiceStartFailureException] if the watchface dies during startup.
     */
    public suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient

    public fun getEditorServiceClient(): EditorServiceClient
}

internal class WatchFaceControlClientImpl internal constructor(
    private val context: Context,
    private val service: IWatchFaceControlService,
    private val serviceConnection: ServiceConnection
) : WatchFaceControlClient {
    private var closed = false

    override fun getInteractiveWatchFaceClientInstance(
        instanceId: String
    ) = service.getInteractiveWatchFaceInstance(instanceId)?.let {
        InteractiveWatchFaceClientImpl(it)
    }

    override fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): HeadlessWatchFaceClient? = TraceEvent(
        "WatchFaceControlClientImpl.createHeadlessWatchFaceClient"
    ).use {
        requireNotClosed()
        return service.createHeadlessWatchFaceInstance(
            HeadlessWatchFaceInstanceParams(
                watchFaceName,
                androidx.wear.watchface.data.DeviceConfig(
                    deviceConfig.hasLowBitAmbient,
                    deviceConfig.hasBurnInProtection,
                    deviceConfig.analogPreviewReferenceTimeMillis,
                    deviceConfig.digitalPreviewReferenceTimeMillis
                ),
                surfaceWidth,
                surfaceHeight
            )
        )?.let {
            HeadlessWatchFaceClientImpl(it)
        }
    }

    override suspend fun getOrCreateInteractiveWatchFaceClient(
        id: String,
        deviceConfig: DeviceConfig,
        watchUiState: androidx.wear.watchface.client.WatchUiState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): InteractiveWatchFaceClient {
        requireNotClosed()
        val traceEvent = AsyncTraceEvent(
            "WatchFaceControlClientImpl" +
                ".getOrCreateWallpaperServiceBackedInteractiveWatchFaceClientAsync"
        )
        return suspendCoroutine { continuation ->
            // [IWatchFaceControlService.getOrCreateInteractiveWatchFaceWCS] has an asynchronous
            // callback and it's possible the watch face might crash during start up so we register
            // a death observer.
            val deathObserver = IBinder.DeathRecipient {
                continuation.resumeWithException(
                    WatchFaceControlClient.ServiceStartFailureException()
                )
            }
            val serviceBinder = service.asBinder()
            serviceBinder.linkToDeath(deathObserver, 0)

            service.getOrCreateInteractiveWatchFace(
                WallpaperInteractiveWatchFaceInstanceParams(
                    id,
                    androidx.wear.watchface.data.DeviceConfig(
                        deviceConfig.hasLowBitAmbient,
                        deviceConfig.hasBurnInProtection,
                        deviceConfig.analogPreviewReferenceTimeMillis,
                        deviceConfig.digitalPreviewReferenceTimeMillis
                    ),
                    WatchUiState(
                        watchUiState.inAmbientMode,
                        watchUiState.interruptionFilter
                    ),
                    UserStyleWireFormat(userStyle ?: emptyMap()),
                    idToComplicationData?.map {
                        IdAndComplicationDataWireFormat(
                            it.key,
                            it.value.asWireComplicationData()
                        )
                    }
                ),
                object : IPendingInteractiveWatchFace.Stub() {
                    override fun getApiVersion() = IPendingInteractiveWatchFace.API_VERSION

                    override fun onInteractiveWatchFaceCreated(
                        iInteractiveWatchFace: IInteractiveWatchFace
                    ) {
                        serviceBinder.unlinkToDeath(deathObserver, 0)
                        traceEvent.close()
                        continuation.resume(
                            InteractiveWatchFaceClientImpl(iInteractiveWatchFace)
                        )
                    }
                }
            )?.let {
                // There was an existing watchface.onInteractiveWatchFaceCreated
                serviceBinder.unlinkToDeath(deathObserver, 0)
                traceEvent.close()
                continuation.resume(InteractiveWatchFaceClientImpl(it))
            }
        }
    }

    override fun getEditorServiceClient(): EditorServiceClient = TraceEvent(
        "WatchFaceControlClientImpl.getEditorServiceClient"
    ).use {
        requireNotClosed()
        return EditorServiceClientImpl(service.editorService)
    }

    private fun requireNotClosed() {
        require(!closed) {
            "WatchFaceControlClient method called after close"
        }
    }

    override fun close() = TraceEvent("WatchFaceControlClientImpl.close").use {
        closed = true
        context.unbindService(serviceConnection)
    }
}
