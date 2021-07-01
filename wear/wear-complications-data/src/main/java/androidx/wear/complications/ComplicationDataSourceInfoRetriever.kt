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
package androidx.wear.complications

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.complications.ComplicationDataSourceInfoRetriever.Result
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ComplicationType.Companion.fromWireType
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.utility.TraceEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private typealias WireComplicationProviderInfo =
    android.support.wearable.complications.ComplicationProviderInfo

/**
 * Retrieves [Result] for a watch face's complications.
 *
 *
 * To use construct an instance and call [retrieveComplicationDataSourceInfo] which returns an array
 * of [Result] objects.
 *
 *
 * Further calls to [retrieveComplicationDataSourceInfo] may be made using the same instance of this
 * class, but [close] must be called when it is no longer needed. Once release has been
 * called, further retrieval attempts will fail.
 */
public class ComplicationDataSourceInfoRetriever : AutoCloseable {
    /** Results for [retrieveComplicationDataSourceInfo]. */
    public class Result internal constructor(
        /** The id for the complication slot, as passed to [retrieveComplicationDataSourceInfo]. */
        public val slotId: Int,

        /**
         * Details of the complication data source for that complication, or `null` if no
         * complication data source is currently configured.
         */
        public val info: androidx.wear.complications.ComplicationDataSourceInfo?
    )

    private inner class ProviderInfoServiceConnection : ServiceConnection {
        @SuppressLint("SyntheticAccessor")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            deferredService.complete(IProviderInfoService.Stub.asInterface(service))
        }

        @SuppressLint("SyntheticAccessor")
        override fun onServiceDisconnected(name: ComponentName) {
            deferredService.completeExceptionally(ServiceDisconnectedException())
        }
    }

    @SuppressLint("SyntheticAccessor")
    private val serviceConnection: ServiceConnection = ProviderInfoServiceConnection()
    private var context: Context? = null
    private val deferredService = CompletableDeferred<IProviderInfoService>()

    /**
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public var closed: Boolean = false
        private set

    /** @param context the current context */
    public constructor(context: Context) {
        this.context = context
        val intent = Intent(ACTION_GET_COMPLICATION_CONFIG)
        intent.setPackage(PROVIDER_INFO_SERVICE_PACKAGE)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /** Exception thrown if the service disconnects. */
    public class ServiceDisconnectedException : Exception()

    /**
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public constructor(service: IProviderInfoService) {
        deferredService.complete(service)
    }

    /**
     * Requests [Result] for the specified complication ids on the specified
     * watch face. When the info is received, the listener will receive a callback for each id.
     * These callbacks will occur on the main thread.
     *
     *
     * This will only work if the package of the current app is the same as the package of the
     * specified watch face.
     *
     * @param watchFaceComponent the ComponentName of the WatchFaceService for which info is
     * being requested
     * @param watchFaceComplicationIds ids of the complications that info is being requested for
     * @return An array of [Result]. If the look up fails null will be returned.
     * @throws [ServiceDisconnectedException] if the service disconnected during the call.
     */
    @Throws(ServiceDisconnectedException::class)
    public suspend fun retrieveComplicationDataSourceInfo(
        watchFaceComponent: ComponentName,
        watchFaceComplicationIds: IntArray
    ): Array<Result>? =
        TraceEvent("ComplicationDataSourceInfoRetriever.retrieveComplicationDataSourceInfo").use {
            require(!closed) {
                "retrieveComplicationDataSourceInfo called after close"
            }
            awaitDeferredService().getProviderInfos(
                watchFaceComponent, watchFaceComplicationIds
            )?.mapIndexed { index, info ->
                Result(
                    watchFaceComplicationIds[index],
                    info?.toApiComplicationDataSourceInfo()
                )
            }?.toTypedArray()
        }

    /**
     * Requests preview [ComplicationData] for a complication data source [ComponentName] and
     * [ComplicationType].
     *
     * @param complicationDataSourceComponent The [ComponentName] of the complication data source
     * from which preview data is requested.
     * @param complicationType The requested [ComplicationType] for the preview data.
     * @return The preview [ComplicationData] or `null` if the complication data source component
     * doesn't exist, or if it doesn't support complicationType, or if the remote service doesn't
     * support this API.
     * @throws [ServiceDisconnectedException] if the service disconnected during the call.
     */
    @Throws(ServiceDisconnectedException::class)
    @RequiresApi(Build.VERSION_CODES.R)
    public suspend fun retrievePreviewComplicationData(
        complicationDataSourceComponent: ComponentName,
        complicationType: ComplicationType
    ): ComplicationData? = TraceEvent(
        "ComplicationDataSourceInfoRetriever.requestPreviewComplicationData"
    ).use {
        require(!closed) {
            "retrievePreviewComplicationData called after close"
        }
        val service = awaitDeferredService()
        if (service.apiVersion < 1) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val deathObserver = IBinder.DeathRecipient {
                continuation.resumeWithException(ServiceDisconnectedException())
            }
            service.asBinder().linkToDeath(deathObserver, 0)

            // Not a huge deal but we might as well unlink the deathObserver.
            continuation.invokeOnCancellation {
                service.asBinder().unlinkToDeath(deathObserver, 0)
            }

            if (!service.requestPreviewComplicationData(
                    complicationDataSourceComponent,
                    complicationType.toWireComplicationType(),
                    object : IPreviewComplicationDataCallback.Stub() {
                        override fun updateComplicationData(
                            data: android.support.wearable.complications.ComplicationData?
                        ) {
                            service.asBinder().unlinkToDeath(deathObserver, 0)
                            continuation.resume(data?.toApiComplicationData())
                        }
                    }
                )
            ) {
                service.asBinder().unlinkToDeath(deathObserver, 0)
                continuation.resume(null)
            }
        }
    }

    private suspend fun awaitDeferredService(): IProviderInfoService =
        TraceEvent("ComplicationDataSourceInfoRetriever.awaitDeferredService").use {
            deferredService.await()
        }

    /**
     * Releases the connection to the complication system used by this class. This must
     * be called when the retriever is no longer needed.
     *
     *
     * Any outstanding or subsequent futures returned by [retrieveComplicationDataSourceInfo] will
     * resolve with null.
     *
     * This class implements the Java `AutoClosable` interface and
     * may be used with try-with-resources.
     */
    override fun close() {
        closed = true
        context?.unbindService(serviceConnection)
    }

    private companion object {
        /** The package of the service that supplies complication data source info.  */
        private const val PROVIDER_INFO_SERVICE_PACKAGE = "com.google.android.wearable.app"
        private const val ACTION_GET_COMPLICATION_CONFIG =
            "android.support.wearable.complications.ACTION_GET_COMPLICATION_CONFIG"
    }
}

/**
 * Holder of details of a complication data source, for use by watch faces (for example,
 * to show the current complication data source in settings). A
 * [ComplicationDataSourceInfoRetriever] can be used to obtain references of this class for each
 * of a watch face's complications.
 */
public class ComplicationDataSourceInfo(
    /** The name of the application containing the complication data source. */
    public val appName: String,

    /** The name of the complication data source. */
    public val name: String,

    /** The icon for the complication data source. */
    public val icon: Icon,

    /** The type of the complication provided by the data source. */
    public val type: ComplicationType,

    /**
     * The complication data source's {@link ComponentName}.
     *
     * This field is populated only on Android R and above and it is `null` otherwise.
     */
    public val componentName: ComponentName?,
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            require(componentName != null) {
                "ComponentName is required on Android R and above"
            }
        }
    }

    /**
     * Converts this value to [WireComplicationProviderInfo] object used for serialization.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toWireComplicationProviderInfo(): WireComplicationProviderInfo =
        WireComplicationProviderInfo(
            appName, name, icon, type.toWireComplicationType(),
            componentName
        )
}

// Ugh we need this since the linter wants the method signature all on one line...
typealias ApiInfo = androidx.wear.complications.ComplicationDataSourceInfo

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationProviderInfo.toApiComplicationDataSourceInfo(): ApiInfo =
    ComplicationDataSourceInfo(
        appName!!, providerName!!, providerIcon!!, fromWireType(complicationType),
        providerComponentName
    )
