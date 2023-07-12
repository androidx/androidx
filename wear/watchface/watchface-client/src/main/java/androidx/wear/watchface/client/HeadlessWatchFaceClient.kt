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

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.RemoteException
import android.support.wearable.watchface.SharedMemoryImage
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleFlavors
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.utility.TraceEvent
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Controls a stateless remote headless watch face. This is mostly intended for use by watch face
 * editor UIs which need to generate screenshots for various styling configurations without
 * affecting the current watchface.
 *
 * Note clients should call [close] when finished.
 */
public interface HeadlessWatchFaceClient : AutoCloseable {
    public companion object {
        internal const val BINDER_KEY = "HeadlessWatchFaceClient"

        @JvmStatic
        public fun createFromBundle(bundle: Bundle): HeadlessWatchFaceClient =
            HeadlessWatchFaceClientImpl(
                IHeadlessWatchFace.Stub.asInterface(bundle.getBinder(BINDER_KEY))
            )
    }

    /** The [Instant] to use when rendering previews. */
    @get:Throws(RemoteException::class) public val previewReferenceInstant: Instant

    /** The watch face's [UserStyleSchema]. */
    @get:Throws(RemoteException::class) public val userStyleSchema: UserStyleSchema

    /**
     * A SHA-1 [MessageDigest] hash of the [UserStyleSchema]. Note that for performance reasons
     * where possible the resource id or url for [Icon]s in the schema are used rather than the
     * image bytes. This means that this hash should be considered insensitive to changes to the
     * contents of icons between APK versions, which the user should account for accordingly.
     *
     * This gives the same result as calling [UserStyleSchema.computeDigestHash] on
     * [userStyleSchema] but is slightly faster since less data is passed over AIDL.
     */
    @Throws(RemoteException::class)
    public fun getUserStyleSchemaDigestHash(): ByteArray = ByteArray(0)

    /**
     * Returns the watch face's [UserStyleFlavors] if any.
     *
     * @throws [RuntimeException] if the watch face threw an exception while trying to service the
     *   request or there was a communication problem with watch face process.
     */
    public fun getUserStyleFlavors(): UserStyleFlavors = UserStyleFlavors()

    /**
     * Map of [androidx.wear.watchface.ComplicationSlot] ids to [ComplicationSlotState] for each
     * [ComplicationSlot] registered with the watch face's [ComplicationSlotsManager]. The
     * ComplicationSlotState is based on the initial state of each
     * [androidx.wear.watchface.ComplicationSlot] plus any overrides from the default style's
     * [ComplicationSlotsUserStyleSetting]. Because the style can't change, ComplicationSlotState is
     * immutable for a headless watch face.
     */
    @get:Throws(RemoteException::class)
    public val complicationSlotsState: Map<Int, ComplicationSlotState>

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param instant The [Instant] to render with
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used.
     * @param slotIdToComplicationData Map of [androidx.wear.watchface.ComplicationSlot] ids to
     *   [ComplicationData] to render with, or if `null` [androidx.wear.watchface.ComplicationSlot]
     *   are not rendered.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
     *   given settings.
     */
    @RequiresApi(27)
    @Throws(RemoteException::class)
    public fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        userStyle: UserStyle?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /** Whether or not the watch face supports [renderWatchFaceToSurface]. */
    public val isRenderWatchFaceToSurfaceSupported: Boolean
        @get:JvmName("isRenderWatchFaceToSurfaceSupported") get() = false

    /**
     * Renders the [androidx.wear.watchface.ComplicationSlot] to a shared memory backed [Bitmap]
     * with the given settings.
     *
     * @param complicationSlotId The id of the complication to render
     * @param renderParameters The [RenderParameters] to draw with
     * @param instant The [Instant] to render with
     * @param complicationData the [ComplicationData] to render with
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
     *   given settings, or `null` if [complicationSlotId] is unrecognized.
     */
    @RequiresApi(27)
    @Throws(RemoteException::class)
    public fun renderComplicationToBitmap(
        complicationSlotId: Int,
        renderParameters: RenderParameters,
        instant: Instant,
        complicationData: ComplicationData,
        userStyle: UserStyle?,
    ): Bitmap?

    /** Callback that observes when the client disconnects. */
    public interface ClientDisconnectListener {
        /**
         * The client disconnected, typically due to the server side crashing. Note this is not
         * called in response to [close] being called on [HeadlessWatchFaceClient].
         */
        public fun onClientDisconnected()
    }

    /** Registers a [ClientDisconnectListener]. */
    @AnyThread
    public fun addClientDisconnectListener(listener: ClientDisconnectListener, executor: Executor)

    /**
     * Removes a [ClientDisconnectListener] previously registered by [addClientDisconnectListener].
     */
    @AnyThread public fun removeClientDisconnectListener(listener: ClientDisconnectListener)

    /** Returns true if the connection to the server side is alive. */
    @AnyThread public fun isConnectionAlive(): Boolean

    /** Stores the underlying connection in a [Bundle]. */
    public fun toBundle(): Bundle
}

internal class HeadlessWatchFaceClientImpl
internal constructor(private val iHeadlessWatchFace: IHeadlessWatchFace) : HeadlessWatchFaceClient {

    private val lock = Any()
    private val listeners = HashMap<HeadlessWatchFaceClient.ClientDisconnectListener, Executor>()

    init {
        iHeadlessWatchFace
            .asBinder()
            .linkToDeath(
                {
                    var listenerCopy:
                        HashMap<HeadlessWatchFaceClient.ClientDisconnectListener, Executor>

                    synchronized(lock) { listenerCopy = HashMap(listeners) }

                    for ((listener, executor) in listenerCopy) {
                        executor.execute { listener.onClientDisconnected() }
                    }
                },
                0
            )
    }

    override val previewReferenceInstant: Instant
        get() = Instant.ofEpochMilli(iHeadlessWatchFace.previewReferenceTimeMillis)

    override val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iHeadlessWatchFace.userStyleSchema)

    override fun getUserStyleSchemaDigestHash(): ByteArray =
        if (iHeadlessWatchFace.apiVersion >= 2) {
            iHeadlessWatchFace.computeUserStyleSchemaDigestHash()
        } else {
            userStyleSchema.getDigestHash()
        }

    override fun getUserStyleFlavors(): UserStyleFlavors = callRemote {
        if (iHeadlessWatchFace.apiVersion >= 3) {
            UserStyleFlavors(iHeadlessWatchFace.userStyleFlavors)
        } else {
            UserStyleFlavors()
        }
    }

    override val complicationSlotsState: Map<Int, ComplicationSlotState>
        get() =
            iHeadlessWatchFace.complicationState.associateBy(
                { it.id },
                { ComplicationSlotState(it.complicationState) }
            )

    @RequiresApi(27)
    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        instant: Instant,
        userStyle: UserStyle?,
        slotIdToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap =
        TraceEvent("HeadlessWatchFaceClientImpl.renderWatchFaceToBitmap").use {
            SharedMemoryImage.ashmemReadImageBundle(
                iHeadlessWatchFace.renderWatchFaceToBitmap(
                    WatchFaceRenderParams(
                        renderParameters.toWireFormat(),
                        instant.toEpochMilli(),
                        userStyle?.toWireFormat(),
                        slotIdToComplicationData?.map {
                            IdAndComplicationDataWireFormat(
                                it.key,
                                it.value.asWireComplicationData()
                            )
                        }
                    )
                )
            )
        }

    override val isRenderWatchFaceToSurfaceSupported = iHeadlessWatchFace.apiVersion >= 4

    @RequiresApi(27)
    override fun renderComplicationToBitmap(
        complicationSlotId: Int,
        renderParameters: RenderParameters,
        instant: Instant,
        complicationData: ComplicationData,
        userStyle: UserStyle?,
    ): Bitmap? =
        TraceEvent("HeadlessWatchFaceClientImpl.renderComplicationToBitmap").use {
            iHeadlessWatchFace
                .renderComplicationToBitmap(
                    ComplicationRenderParams(
                        complicationSlotId,
                        renderParameters.toWireFormat(),
                        instant.toEpochMilli(),
                        complicationData.asWireComplicationData(),
                        userStyle?.toWireFormat(),
                    )
                )
                ?.let { SharedMemoryImage.ashmemReadImageBundle(it) }
        }

    override fun addClientDisconnectListener(
        listener: HeadlessWatchFaceClient.ClientDisconnectListener,
        executor: Executor
    ) {
        synchronized(lock) {
            require(!listeners.contains(listener)) {
                "Don't call addClientDisconnectListener multiple times for the same listener"
            }
            listeners.put(listener, executor)
        }
    }

    override fun removeClientDisconnectListener(
        listener: HeadlessWatchFaceClient.ClientDisconnectListener
    ) {
        synchronized(lock) { listeners.remove(listener) }
    }

    override fun isConnectionAlive() = iHeadlessWatchFace.asBinder().isBinderAlive

    override fun toBundle() =
        Bundle().apply {
            this.putBinder(HeadlessWatchFaceClient.BINDER_KEY, iHeadlessWatchFace.asBinder())
        }

    override fun close() =
        TraceEvent("HeadlessWatchFaceClientImpl.close").use { iHeadlessWatchFace.release() }
}
