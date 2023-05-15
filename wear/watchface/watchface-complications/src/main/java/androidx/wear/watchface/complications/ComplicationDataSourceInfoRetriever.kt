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
package androidx.wear.watchface.complications

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.support.wearable.complications.ComplicationProviderInfo as WireComplicationProviderInfo
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.complications.ComplicationDataSourceInfoRetriever.Result
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ComplicationType.Companion.fromWireType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.utility.iconEquals
import androidx.wear.watchface.utility.iconHashCode
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Retrieves [Result] for a watch face's complications.
 *
 * To use construct an instance and call [retrieveComplicationDataSourceInfo] which returns an array
 * of [Result] objects.
 *
 * Further calls to [retrieveComplicationDataSourceInfo] may be made using the same instance of this
 * class, but [close] must be called when it is no longer needed. Once release has been called,
 * further retrieval attempts will fail.
 */
public class ComplicationDataSourceInfoRetriever : AutoCloseable {
    /** Results for [retrieveComplicationDataSourceInfo]. */
    public class Result
    internal constructor(
        /** The id for the complication slot, as passed to [retrieveComplicationDataSourceInfo]. */
        public val slotId: Int,

        /**
         * Details of the complication data source for that complication, or `null` if no
         * complication data source is currently configured.
         */
        public val info: ComplicationDataSourceInfo?
    )

    private inner class ProviderInfoServiceConnection : ServiceConnection {
        @SuppressLint("SyntheticAccessor")
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            deferredService.complete(IProviderInfoService.Stub.asInterface(service))
        }

        @SuppressLint("SyntheticAccessor")
        override fun onBindingDied(name: ComponentName?) {
            synchronized(lock) { closed = true }
            deferredService.completeExceptionally(ServiceDisconnectedException())
        }

        @SuppressLint("SyntheticAccessor")
        override fun onServiceDisconnected(name: ComponentName) {
            synchronized(lock) { closed = true }
            deferredService.completeExceptionally(ServiceDisconnectedException())
        }
    }

    @SuppressLint("SyntheticAccessor")
    private val serviceConnection: ServiceConnection = ProviderInfoServiceConnection()
    private var context: Context? = null
    private val deferredService = CompletableDeferred<IProviderInfoService>()
    private val lock = Any()

    /** @hide */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public var closed: Boolean = false
        private set

    internal constructor(context: Context, intent: Intent) {
        this.context = context
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /** @param context the current context */
    public constructor(
        context: Context
    ) : this(
        context,
        Intent(ACTION_GET_COMPLICATION_CONFIG).apply { setPackage(PROVIDER_INFO_SERVICE_PACKAGE) }
    )

    /** Exception thrown if the service disconnects. */
    public class ServiceDisconnectedException : Exception()

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public constructor(service: IProviderInfoService) {
        deferredService.complete(service)
    }

    /**
     * Requests [Result] for the specified complication ids on the specified watch face. When the
     * info is received, the listener will receive a callback for each id. These callbacks will
     * occur on the main thread.
     *
     * This will only work if the package of the current app is the same as the package of the
     * specified watch face.
     *
     * @param watchFaceComponent the ComponentName of the WatchFaceService for which info is being
     *   requested
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
            synchronized(lock) {
                require(!closed) { "retrieveComplicationDataSourceInfo called after close" }
            }
            awaitDeferredService()
                .getProviderInfos(watchFaceComponent, watchFaceComplicationIds)
                ?.mapIndexed { index, info ->
                    Result(watchFaceComplicationIds[index], info?.toApiComplicationDataSourceInfo())
                }
                ?.toTypedArray()
        }

    /**
     * Requests preview [ComplicationData] for a complication data source [ComponentName] and
     * [ComplicationType]. Note if `null` is returned
     * [ComplicationDataSourceInfo.fallbackPreviewData] can be used to generate fallback preview
     * data based on the name and icon of the provider.
     *
     * @param complicationDataSourceComponent The [ComponentName] of the complication data source
     *   from which preview data is requested.
     * @param complicationType The requested [ComplicationType] for the preview data.
     * @return The preview [ComplicationData] or `null` if the complication data source component
     *   doesn't exist, or if it doesn't support complicationType, or if the remote service doesn't
     *   support this API.
     * @throws [ServiceDisconnectedException] if the service disconnected during the call.
     */
    @Throws(ServiceDisconnectedException::class)
    @RequiresApi(Build.VERSION_CODES.R)
    public suspend fun retrievePreviewComplicationData(
        complicationDataSourceComponent: ComponentName,
        complicationType: ComplicationType
    ): ComplicationData? =
        TraceEvent("ComplicationDataSourceInfoRetriever.requestPreviewComplicationData").use {
            synchronized(lock) {
                require(!closed) { "retrievePreviewComplicationData called after close" }
            }
            val service = awaitDeferredService()
            if (service.apiVersion < 1) {
                return null
            }

            return suspendCancellableCoroutine { continuation ->
                val callback = PreviewComplicationDataCallback(service, continuation)
                if (
                    !service.requestPreviewComplicationData(
                        complicationDataSourceComponent,
                        complicationType.toWireComplicationType(),
                        callback
                    )
                ) {
                    callback.safeUnlinkToDeath()
                    continuation.resume(null)
                }
            }
        }

    private class PreviewComplicationDataCallback(
        val service: IProviderInfoService,
        var continuation: CancellableContinuation<ComplicationData?>?
    ) : IPreviewComplicationDataCallback.Stub() {
        val deathObserver: IBinder.DeathRecipient =
            IBinder.DeathRecipient {
                continuation?.resumeWithException(ServiceDisconnectedException())
            }

        init {
            service.asBinder().linkToDeath(deathObserver, 0)

            // Not a huge deal but we might as well unlink the deathObserver.
            continuation?.invokeOnCancellation { safeUnlinkToDeath() }
        }

        override fun updateComplicationData(
            data: android.support.wearable.complications.ComplicationData?
        ) {
            safeUnlinkToDeath()
            continuation!!.resume(data?.toApiComplicationData())

            // Re http://b/249121838 this is important, it prevents a memory leak.
            continuation = null
        }

        internal fun safeUnlinkToDeath() {
            try {
                service.asBinder().unlinkToDeath(deathObserver, 0)
            } catch (e: NoSuchElementException) {
                // This really shouldn't happen.
                Log.w(TAG, "retrievePreviewComplicationData encountered", e)
            }
        }
    }

    private suspend fun awaitDeferredService(): IProviderInfoService =
        TraceEvent("ComplicationDataSourceInfoRetriever.awaitDeferredService").use {
            deferredService.await()
        }

    /**
     * Releases the connection to the complication system used by this class. This must be called
     * when the retriever is no longer needed.
     *
     * Any outstanding or subsequent futures returned by [retrieveComplicationDataSourceInfo] will
     * resolve with null.
     *
     * This class implements the Java `AutoClosable` interface and may be used with
     * try-with-resources.
     */
    override fun close() {
        synchronized(lock) {
            if (closed) {
                Log.e(
                    TAG,
                    "Error ComplicationDataSourceInfoRetriever.close called when already closed",
                    Throwable()
                )
            } else {
                closed = true
                try {
                    context?.unbindService(serviceConnection)
                    context = null
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "unbindService failed", e)
                }
            }
        }
    }

    private companion object {
        private const val TAG = "ComplicationDataS"

        /** The package of the service that supplies complication data source info. */
        private const val PROVIDER_INFO_SERVICE_PACKAGE = "com.google.android.wearable.app"
        private const val ACTION_GET_COMPLICATION_CONFIG =
            "android.support.wearable.complications.ACTION_GET_COMPLICATION_CONFIG"
    }
}

/**
 * Holder of details of a complication data source, for use by watch faces (for example, to show the
 * current complication data source in settings). A [ComplicationDataSourceInfoRetriever] can be
 * used to obtain references of this class for each of a watch face's complications.
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
    /**
     * Lazily constructed fallback preview [ComplicationData] based on this
     * ComplicationDataSourceInfo. This is useful when
     * [ComplicationDataSourceInfoRetriever.retrievePreviewComplicationData] returns `null` (e.g. on
     * a pre-android R device).
     */
    public val fallbackPreviewData: ComplicationData by lazy {
        val contentDescription = PlainComplicationText.Builder(name).build()
        when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(
                        PlainComplicationText.Builder(
                                name.take(ShortTextComplicationData.MAX_TEXT_LENGTH)
                            )
                            .build(),
                        contentDescription
                    )
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
                    .build()
            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder(name).build(),
                        contentDescription
                    )
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
                    .build()
            ComplicationType.SMALL_IMAGE ->
                SmallImageComplicationData.Builder(
                        SmallImage.Builder(icon, SmallImageType.ICON).build(),
                        contentDescription
                    )
                    .build()
            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(
                        MonochromaticImage.Builder(icon).build(),
                        contentDescription
                    )
                    .build()
            ComplicationType.PHOTO_IMAGE ->
                PhotoImageComplicationData.Builder(icon, contentDescription).build()
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(42f, 0f, 100f, contentDescription)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
                    .setText(PlainComplicationText.Builder(name).build())
                    .build()
            else -> NoDataComplicationData()
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            require(componentName != null) { "ComponentName is required on Android R and above" }
        }
    }

    override fun toString(): String =
        "ComplicationDataSourceInfo(appName=$appName, name=$name, type=$type" +
            ", icon=$icon, componentName=$componentName)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationDataSourceInfo

        if (appName != other.appName) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (!(icon iconEquals other.icon)) return false
        if (componentName != other.componentName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + icon.iconHashCode()
        result = 31 * result + componentName.hashCode()
        return result
    }

    /** Converts this value to [WireComplicationProviderInfo] object used for serialization. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toWireComplicationProviderInfo(): WireComplicationProviderInfo =
        WireComplicationProviderInfo(
            appName,
            name,
            icon,
            type.toWireComplicationType(),
            componentName
        )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireComplicationProviderInfo.toApiComplicationDataSourceInfo() =
    ComplicationDataSourceInfo(
        appName!!,
        providerName!!,
        providerIcon!!,
        fromWireType(complicationType),
        providerComponentName
    )
