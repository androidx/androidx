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
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.RectF
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.client.WatchFaceControlClient.Companion.createWatchFaceControlClient
import androidx.wear.watchface.control.IWatchFaceControlService
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.control.data.GetComplicationSlotMetadataParams
import androidx.wear.watchface.control.data.GetUserStyleSchemaParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import kotlinx.coroutines.CompletableDeferred

/**
 * Interface for fetching watch face metadata. E.g. the [UserStyleSchema] and
 * [ComplicationSlotMetadata].
 */
@WatchFaceClientExperimental
public interface WatchFaceMetadataClient : AutoCloseable {

    public companion object {
        /**
         * Constructs a [WatchFaceMetadataClient] for fetching metadata for the specified watch
         * face.
         *
         * @param context Calling application's [Context].
         * @param watchFaceName The [ComponentName] of the watch face to fetch meta data from.
         * @return The [WatchFaceMetadataClient] if there is one.
         * @throws [ServiceNotBoundException] if the underlying watch face control service can not
         * be bound or a [ServiceStartFailureException] if the watch face dies during startup.
         */
        @JvmStatic
        @Throws(ServiceNotBoundException::class, ServiceStartFailureException::class)
        public suspend fun createWatchFaceMetadataClient(
            context: Context,
            watchFaceName: ComponentName
        ): WatchFaceMetadataClient = createWatchFaceMetadataClientImpl(
            context,
            Intent(WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE).apply {
                setPackage(watchFaceName.packageName)
            },
            watchFaceName
        )

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Suppress("ShowingMemberInHiddenClass") // Spurious warning about exposing the
        // 'hidden' companion object, which _isn't_ hidden.
        public suspend fun createWatchFaceMetadataClientImpl(
            context: Context,
            intent: Intent,
            watchFaceName: ComponentName
        ): WatchFaceMetadataClient {
            val deferredService = CompletableDeferred<IWatchFaceControlService>()
            val traceEvent = AsyncTraceEvent("WatchFaceMetadataClientImpl.bindService")
            val serviceConnection = object : ServiceConnection {
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
            return WatchFaceMetadataClientImpl(
                context,
                deferredService.await(),
                serviceConnection,
                watchFaceName
            )
        }
    }

    /**
     * Exception thrown by [createWatchFaceControlClient] if the remote service can't be bound.
     */
    public class ServiceNotBoundException : Exception()

    /** Exception thrown by [WatchFaceControlClient] methods if the service dies during start up. */
    public class ServiceStartFailureException(message: String = "") : Exception(message)

    /**
     * Returns the watch face's [UserStyleSchema].
     */
    @Throws(RemoteException::class)
    public fun getUserStyleSchema(): UserStyleSchema

    /**
     * Returns a map of [androidx.wear.watchface.ComplicationSlot] ID to [ComplicationSlotMetadata]
     * for each slot in the watch face's [androidx.wear.watchface.ComplicationSlotsManager].
     */
    @Throws(RemoteException::class)
    public fun getComplicationSlotMetadataMap(): Map<Int, ComplicationSlotMetadata>
}

/**
 * Static metadata for a [androidx.wear.watchface.ComplicationSlot].
 *
 * @param bounds The complication slot's [ComplicationSlotBounds]. Only non `null` for watch faces
 * with a new enough [androidx.wear.watchface.control.WatchFaceControlService].
 * @param boundsType The [ComplicationSlotBoundsType] of the complication slot.
 * @param supportedTypes The list of [ComplicationType]s accepted by this complication slot. Used
 * during complication data source selection, this list should be non-empty.
 * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] which controls the
 * initial complication data source when the watch face is first installed.
 * @param defaultDataSourceType The default [ComplicationType] for the default  complication data
 * source.
 * @param isInitiallyEnabled At creation a complication slot is either enabled or disabled. This
 * can be overridden by a [ComplicationSlotsUserStyleSetting] (see
 * [ComplicationSlotOverlay.enabled]).
 * Editors need to know the initial state of a complication slot to predict the effects of making a
 * style change.
 * @param fixedComplicationDataSource  Whether or not the complication slot's complication data
 * source is fixed (i.e. can't be changed by the user). This is useful for watch faces built
 * around specific complication  complication data sources.
 * @param complicationConfigExtras Extras to be merged into the Intent sent when invoking the
 *  complication data source chooser activity.
 */
@WatchFaceClientExperimental
public class ComplicationSlotMetadata(
    public val bounds: ComplicationSlotBounds?,
    @ComplicationSlotBoundsType public val boundsType: Int,
    public val supportedTypes: List<ComplicationType>,
    public val defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
    public val defaultDataSourceType: ComplicationType,
    @get:JvmName("isInitiallyEnabled")
    public val isInitiallyEnabled: Boolean,
    public val fixedComplicationDataSource: Boolean,
    public val complicationConfigExtras: Bundle
)

@OptIn(WatchFaceClientExperimental::class)
internal class WatchFaceMetadataClientImpl internal constructor(
    private val context: Context,
    private val service: IWatchFaceControlService,
    private val serviceConnection: ServiceConnection,
    private val watchFaceName: ComponentName
) : WatchFaceMetadataClient {
    private var closed = false
    private val headlessClientDelegate = lazy {
        createHeadlessWatchFaceClient(
            watchFaceName
        ) ?: throw WatchFaceMetadataClient.ServiceStartFailureException(
            "Could not open headless client for ${watchFaceName.flattenToString()}"
        )
    }
    private val headlessClient by headlessClientDelegate

    private fun createHeadlessWatchFaceClient(
        watchFaceName: ComponentName
    ): HeadlessWatchFaceClient? = TraceEvent(
        "WatchFaceMetadataClientImpl.createHeadlessWatchFaceClient"
    ).use {
        requireNotClosed()
        return service.createHeadlessWatchFaceInstance(
            HeadlessWatchFaceInstanceParams(
                watchFaceName,
                androidx.wear.watchface.data.DeviceConfig(false, false, 0, 0),
                1,
                1
            )
        )?.let {
            HeadlessWatchFaceClientImpl(it)
        }
    }

    private fun requireNotClosed() {
        require(!closed) {
            "WatchFaceMetadataClient method called after close"
        }
    }

    override fun getUserStyleSchema(): UserStyleSchema {
        requireNotClosed()
        return if (service.apiVersion >= 3) {
            UserStyleSchema(service.getUserStyleSchema(GetUserStyleSchemaParams(watchFaceName)))
        } else {
            headlessClient.userStyleSchema
        }
    }

    override fun getComplicationSlotMetadataMap(): Map<Int, ComplicationSlotMetadata> {
        requireNotClosed()
        return if (service.apiVersion >= 3) {
            val wireFormat = service.getComplicationSlotMetadata(
                GetComplicationSlotMetadataParams(watchFaceName)
            )
            wireFormat.associateBy(
                { it.id },
                {
                    val perSlotBounds = HashMap<ComplicationType, RectF>()
                    for (i in it.complicationBoundsType.indices) {
                        perSlotBounds[ComplicationType.fromWireType(it.complicationBoundsType[i])] =
                            it.complicationBounds[i]
                    }
                    ComplicationSlotMetadata(
                        ComplicationSlotBounds(perSlotBounds),
                        it.boundsType,
                        it.supportedTypes.map { ComplicationType.fromWireType(it) },
                        DefaultComplicationDataSourcePolicy(
                            it.defaultDataSourcesToTry ?: emptyList(),
                            it.fallbackSystemDataSource
                        ),
                        ComplicationType.fromWireType(it.defaultDataSourceType),
                        it.isInitiallyEnabled,
                        it.isFixedComplicationDataSource,
                        it.complicationConfigExtras
                    )
                }
            )
        } else {
            headlessClient.complicationSlotsState.mapValues {
                ComplicationSlotMetadata(
                    null,
                    it.value.boundsType,
                    it.value.supportedTypes,
                    it.value.defaultDataSourcePolicy,
                    it.value.defaultDataSourceType,
                    it.value.isInitiallyEnabled,
                    it.value.fixedComplicationDataSource,
                    it.value.complicationConfigExtras
                )
            }
        }
    }

    override fun close() = TraceEvent("WatchFaceMetadataClientImpl.close").use {
        closed = true
        if (headlessClientDelegate.isInitialized()) {
            headlessClient.close()
        }
        context.unbindService(serviceConnection)
    }
}
