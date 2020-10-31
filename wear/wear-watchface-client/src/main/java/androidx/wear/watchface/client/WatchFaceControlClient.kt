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
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import com.google.common.util.concurrent.ListenableFuture

/**
 * Connects to a watch face's WatchFaceControlService which allows the user to control the
 * watch face.
 */
public class WatchFaceControlClient internal constructor(
    private val context: Context,
    serviceIntent: Intent
) : AutoCloseable {

    /** Constructs a client which connects to a watch face in the given android package. */
    public constructor(
        /** Calling application's [Context]. */
        context: Context,
        /** The name of the package containing the watch face control service to bind to. */
        watchFacePackageName: String
    ) : this(
        context,
        Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
            this.setPackage(watchFacePackageName)
        }
    )

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
    public fun getInteractiveWatchFaceInstanceSysUi(
        instanceId: String
    ): ListenableFuture<InteractiveWatchFaceSysUiClient?> {
        val resultFuture = ResolvableFuture.create<InteractiveWatchFaceSysUiClient>()
        serviceFuture.addListener(
            {
                val service = serviceFuture.get()
                if (service == null) {
                    resultFuture.setException(ServiceNotBoundException())
                } else {
                    resultFuture.set(
                        InteractiveWatchFaceSysUiClient(
                            service.getInteractiveWatchFaceInstanceSysUI(instanceId)
                        )
                    )
                }
            },
            { runnable -> runnable.run() }
        )
        return resultFuture
    }

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
    ): ListenableFuture<HeadlessWatchFaceClient?> {
        val resultFuture = ResolvableFuture.create<HeadlessWatchFaceClient?>()
        serviceFuture.addListener(
            {
                val service = serviceFuture.get()
                if (service == null) {
                    resultFuture.setException(ServiceNotBoundException())
                } else {
                    resultFuture.set(
                        HeadlessWatchFaceClient(
                            service.createHeadlessWatchFaceInstance(
                                HeadlessWatchFaceInstanceParams(
                                    watchFaceName,
                                    androidx.wear.watchface.data.DeviceConfig(
                                        deviceConfig.hasLowBitAmbient,
                                        deviceConfig.hasBurnInProtection,
                                        deviceConfig.screenShape
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

    /**
     * Releases the controls service.  It is an error to issue any further commands on this
     * interface.
     */
    override fun close() {
        context.unbindService(serviceConnection)
        serviceFuture.set(null)
    }
}
