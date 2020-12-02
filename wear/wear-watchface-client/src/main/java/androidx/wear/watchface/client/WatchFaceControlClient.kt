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
import androidx.concurrent.futures.ResolvableFuture
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IPendingInteractiveWatchFaceWCS
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.data.UserStyleWireFormat
import com.google.common.util.concurrent.ListenableFuture

/**
 * Connects to a watch face's WatchFaceControlService which allows the user to control the watch
 * face.
 */
public interface WatchFaceControlClient : AutoCloseable {

    public companion object {
        /**
         * Constructs a [WatchFaceControlClient] which attempts to connect to a watch face in the
         * android package [watchFacePackageName]. If this fails the [ListenableFuture]s returned by
         * WatchFaceControlClient methods will fail with [ServiceNotBoundException].
         */
        @JvmStatic
        public fun createWatchFaceControlClient(
            /** Calling application's [Context]. */
            context: Context,
            /** The name of the package containing the watch face control service to bind to. */
            watchFacePackageName: String
        ): WatchFaceControlClient = WatchFaceControlClientImpl(
            context,
            Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                this.setPackage(watchFacePackageName)
            }
        )
    }

    /**
     * Exception thrown by [WatchFaceControlClient] methods when the remote service is not bound.
     */
    public class ServiceNotBoundException : Exception()

    /**
     * Returns the [InteractiveWatchFaceSysUiClient] for the given instance id, or null if no such
     * instance exists.
     *
     * When finished call [InteractiveWatchFaceSysUiClient.close] to release resources.
     *
     * @param instanceId The name of the interactive watch face instance to retrieve
     * @return A [ListenableFuture] for the [InteractiveWatchFaceSysUiClient] or `null` if
     *    [instanceId] is unrecognized, or [ServiceNotBoundException] if the
     *    WatchFaceControlService is not bound.
     */
    public fun getInteractiveWatchFaceSysUiClientInstance(
        instanceId: String
    ): ListenableFuture<InteractiveWatchFaceSysUiClient?>

    /**
     * Creates a [HeadlessWatchFaceClient] with the specified [DeviceConfig]. Screenshots made with
     * [HeadlessWatchFaceClient.takeWatchFaceScreenshot] will be `surfaceWidth` x `surfaceHeight` in
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
     * @return A [ListenableFuture] for the [HeadlessWatchFaceClient] or `null` if [watchFaceName]
     *    is unrecognized, or [ServiceNotBoundException] if the WatchFaceControlService is not
     *    bound.
     */
    public fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): ListenableFuture<HeadlessWatchFaceClient?>

    /**
     * Requests either an existing [InteractiveWatchFaceWcsClient] with the specified [id] or
     * schedules creation of an [InteractiveWatchFaceWcsClient] for the next time the
     * WallpaperService creates an engine.
     *
     * NOTE that currently only one [InteractiveWatchFaceWcsClient] per process can exist at a time.
     *
     * @param id The ID for the requested [InteractiveWatchFaceWcsClient].
     * @param deviceConfig The [DeviceConfig] for the wearable.
     * @param systemState The initial [SystemState] for the wearable.
     * @param userStyle The initial style map (see [UserStyle]), or null if the default should be
     *     used.
     * @param idToComplicationData The initial complication data, or null if unavailable.
     * @return a [ListenableFuture] for a [InteractiveWatchFaceWcsClient]
     */
    public fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
        id: String,
        deviceConfig: DeviceConfig,
        systemState: SystemState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): ListenableFuture<InteractiveWatchFaceWcsClient>
}

internal class WatchFaceControlClientImpl internal constructor(
    private val context: Context,
    serviceIntent: Intent
) : WatchFaceControlClient {

    internal var serviceFuture = ResolvableFuture.create<IWatchFaceControlService?>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            serviceFuture.set(IWatchFaceControlService.Stub.asInterface(binder))
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceFuture.set(null)
        }
    }

    init {
        if (!context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
            serviceFuture.set(null)
        }
    }

    override fun getInteractiveWatchFaceSysUiClientInstance(
        instanceId: String
    ): ListenableFuture<InteractiveWatchFaceSysUiClient?> {
        val resultFuture = ResolvableFuture.create<InteractiveWatchFaceSysUiClient>()
        serviceFuture.addListener(
            {
                val service = serviceFuture.get()
                if (service == null) {
                    resultFuture.setException(WatchFaceControlClient.ServiceNotBoundException())
                } else {
                    resultFuture.set(
                        InteractiveWatchFaceSysUiClientImpl(
                            service.getInteractiveWatchFaceInstanceSysUI(instanceId)
                        )
                    )
                }
            },
            { runnable -> runnable.run() }
        )
        return resultFuture
    }

    override fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName,
        deviceConfig: DeviceConfig,
        surfaceWidth: Int,
        surfaceHeight: Int
    ): ListenableFuture<HeadlessWatchFaceClient?> {
        val resultFuture = ResolvableFuture.create<HeadlessWatchFaceClient?>()
        serviceFuture.addListener(
            {
                val service = serviceFuture.get()
                if (service == null) {
                    resultFuture.setException(WatchFaceControlClient.ServiceNotBoundException())
                } else {
                    resultFuture.set(
                        HeadlessWatchFaceClientImpl(
                            service.createHeadlessWatchFaceInstance(
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
                            )
                        )
                    )
                }
            },
            { runnable -> runnable.run() }
        )
        return resultFuture
    }

    override fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
        id: String,
        deviceConfig: DeviceConfig,
        systemState: SystemState,
        userStyle: Map<String, String>?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): ListenableFuture<InteractiveWatchFaceWcsClient> {
        val resultFuture = ResolvableFuture.create<InteractiveWatchFaceWcsClient>()
        serviceFuture.addListener(
            {
                val service = serviceFuture.get()
                if (service == null) {
                    resultFuture.setException(WatchFaceControlClient.ServiceNotBoundException())
                } else {
                    val existingInstance = service.getOrCreateInteractiveWatchFaceWCS(
                        WallpaperInteractiveWatchFaceInstanceParams(
                            id,
                            androidx.wear.watchface.data.DeviceConfig(
                                deviceConfig.hasLowBitAmbient,
                                deviceConfig.hasBurnInProtection,
                                deviceConfig.analogPreviewReferenceTimeMillis,
                                deviceConfig.digitalPreviewReferenceTimeMillis
                            ),
                            androidx.wear.watchface.data.SystemState(
                                systemState.inAmbientMode,
                                systemState.interruptionFilter
                            ),
                            UserStyleWireFormat(userStyle ?: emptyMap()),
                            idToComplicationData?.map {
                                IdAndComplicationDataWireFormat(
                                    it.key,
                                    it.value.asWireComplicationData()
                                )
                            }
                        ),
                        object : IPendingInteractiveWatchFaceWCS.Stub() {
                            override fun getApiVersion() =
                                IPendingInteractiveWatchFaceWCS.API_VERSION

                            override fun onInteractiveWatchFaceWcsCreated(
                                iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS
                            ) {
                                resultFuture.set(
                                    InteractiveWatchFaceWcsClientImpl(iInteractiveWatchFaceWcs)
                                )
                            }
                        }
                    )
                    existingInstance?.let {
                        resultFuture.set(InteractiveWatchFaceWcsClientImpl(it))
                    }
                }
            },
            { runnable -> runnable.run() }
        )
        return resultFuture
    }

    override fun close() {
        context.unbindService(serviceConnection)
        serviceFuture.set(null)
    }
}
