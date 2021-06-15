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
import android.support.wearable.watchface.SharedMemoryImage
import androidx.annotation.AnyThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.toApiComplicationText
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ContentDescriptionLabel
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.TapType
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.ComplicationSlotBoundsType
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleData
import java.util.concurrent.Executor

/**
 * Controls a stateful remote interactive watch face. Typically this will be used for the current
 * active watch face.
 *
 * Note clients should call [close] when finished.
 */
public interface InteractiveWatchFaceClient : AutoCloseable {
    /**
     * Sends new [ComplicationData] to the watch face. Note this doesn't have to be a full update,
     * it's possible to update just one complication at a time, but doing so may result in a less
     * visually clean transition.
     *
     * @param slotIdToComplicationData The [ComplicationData] for each
     * [androidx.wear.watchface.ComplicationSlot].
     */
    public fun updateComplicationData(slotIdToComplicationData: Map<Int, ComplicationData>)

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     * if null then the existing complication data if any is used.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch  face with the
     * given settings.
     */
    @RequiresApi(27)
    public fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long

    /**
     * Renames this instance to [newInstanceId] (must be unique, usually this would be different
     * from the old ID but that's not a requirement). Sets the current [UserStyle] and clears
     * any complication data. Setting the new UserStyle may have a side effect of enabling or
     * disabling complicationSlots, which will be visible via [ComplicationSlotState.isEnabled].
     */
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle)

    /**
     * Renames this instance to [newInstanceId] (must be unique, usually this would be different
     * from the old ID but that's not a requirement). Sets the current [UserStyle] represented as a
     * [UserStyleData> and clears any complication data. Setting the new UserStyle may have a
     * side effect of enabling or disabling complicationSlots, which will be visible via
     * [ComplicationSlotState.isEnabled].
     */
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyleData)

    /** Returns the ID of this watch face instance. */
    public val instanceId: String

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema

    /**
     * Map of [androidx.wear.watchface.ComplicationSlot] ids to [ComplicationSlotState] for each
     * [ComplicationSlot] registered with the  watch face's [ComplicationSlotsManager]. The
     * ComplicationSlotState is based on the initial state of each
     * [androidx.wear.watchface.ComplicationSlot] plus any overrides from a
     * [ComplicationSlotsUserStyleSetting]. As a consequence ComplicationSlotState may update based
     * on style changes.
     */
    public val complicationSlotsState: Map<Int, ComplicationSlotState>

    /**
     * Returns the ID of the [androidx.wear.watchface.ComplicationSlot] at the given coordinates or
     * `null` if there isn't one.
     */
    @SuppressWarnings("AutoBoxing")
    public fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int? =
        complicationSlotsState.asSequence().firstOrNull {
            it.value.isEnabled && when (it.value.boundsType) {
                ComplicationSlotBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                ComplicationSlotBoundsType.BACKGROUND -> false
                ComplicationSlotBoundsType.EDGE -> false
                else -> false
            }
        }?.key

    /**
     * Requests that [ComplicationSlotsManager.displayPressedAnimation] is called for
     * [complicationSlotId].
     */
    public fun displayPressedAnimation(complicationSlotId: Int)

    public companion object {
        /** Indicates a "down" touch event on the watch face. */
        public const val TAP_TYPE_DOWN: Int = IInteractiveWatchFace.TAP_TYPE_DOWN

        /**
         * Indicates that a previous [TAP_TYPE_DOWN] event has been canceled. This generally happens
         * when the watch face is touched but then a move or long press occurs.
         */
        public const val TAP_TYPE_CANCEL: Int = IInteractiveWatchFace.TAP_TYPE_CANCEL

        /**
         * Indicates that an "up" event on the watch face has occurred that has not been consumed by
         * another activity. A [TAP_TYPE_DOWN] always occur first. This event will not occur if a
         * [TAP_TYPE_CANCEL] is sent.
         */
        public const val TAP_TYPE_UP: Int = IInteractiveWatchFace.TAP_TYPE_UP
    }

    /**
     * Sends a tap event to the watch face for processing.
     */
    public fun sendTouchEvent(@Px xPosition: Int, @Px yPosition: Int, @TapType tapType: Int)

    /**
     * Returns the [ContentDescriptionLabel]s describing the watch face, for the use by screen
     * readers.
     */
    public val contentDescriptionLabels: List<ContentDescriptionLabel>

    /** Updates the watch faces [WatchUiState]. */
    public fun setWatchUiState(watchUiState: androidx.wear.watchface.client.WatchUiState)

    /** Triggers watch face rendering into the surface when in ambient mode. */
    public fun performAmbientTick()

    /** Callback that observes when the client disconnects. */
    public interface ClientDisconnectListener {
        /**
         * The client disconnected, typically due to the server side crashing. Note this is not
         * called in response to [close] being called on [InteractiveWatchFaceClient].
         */
        public fun onClientDisconnected()
    }

    /** Registers a [ClientDisconnectListener]. */
    @AnyThread
    public fun addClientDisconnectListener(listener: ClientDisconnectListener, executor: Executor)

    /**
     * Removes a [ClientDisconnectListener] previously registered by [addClientDisconnectListener].
     */
    @AnyThread
    public fun removeClientDisconnectListener(listener: ClientDisconnectListener)

    /** Returns true if the connection to the server side is alive. */
    @AnyThread
    public fun isConnectionAlive(): Boolean
}

/** Controls a stateful remote interactive watch face. */
internal class InteractiveWatchFaceClientImpl internal constructor(
    private val iInteractiveWatchFace: IInteractiveWatchFace
) : InteractiveWatchFaceClient {

    private val lock = Any()
    private val listeners = HashMap<InteractiveWatchFaceClient.ClientDisconnectListener, Executor>()

    init {
        iInteractiveWatchFace.asBinder().linkToDeath(
            {
                var listenerCopy:
                    HashMap<InteractiveWatchFaceClient.ClientDisconnectListener, Executor>

                synchronized(lock) {
                    listenerCopy = HashMap(listeners)
                }

                for ((listener, executor) in listenerCopy) {
                    executor.execute {
                        listener.onClientDisconnected()
                    }
                }
            },
            0
        )
    }

    override fun updateComplicationData(
        slotIdToComplicationData: Map<Int, ComplicationData>
    ) = TraceEvent("InteractiveWatchFaceClientImpl.updateComplicationData").use {
        iInteractiveWatchFace.updateComplicationData(
            slotIdToComplicationData.map {
                IdAndComplicationDataWireFormat(it.key, it.value.asWireComplicationData())
            }
        )
    }

    @RequiresApi(27)
    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = TraceEvent("InteractiveWatchFaceClientImpl.renderWatchFaceToBitmap").use {
        SharedMemoryImage.ashmemReadImageBundle(
            iInteractiveWatchFace.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    renderParameters.toWireFormat(),
                    calendarTimeMillis,
                    userStyle?.toWireFormat(),
                    idAndComplicationData?.map {
                        IdAndComplicationDataWireFormat(
                            it.key,
                            it.value.asWireComplicationData()
                        )
                    }
                )
            )
        )
    }

    override val previewReferenceTimeMillis: Long
        get() = iInteractiveWatchFace.previewReferenceTimeMillis

    override fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle) = TraceEvent(
        "InteractiveWatchFaceClientImpl.updateInstance"
    ).use {
        iInteractiveWatchFace.updateWatchfaceInstance(newInstanceId, userStyle.toWireFormat())
    }

    override fun updateWatchFaceInstance(
        newInstanceId: String,
        userStyle: UserStyleData
    ) = TraceEvent(
        "InteractiveWatchFaceClientImpl.updateInstance"
    ).use {
        iInteractiveWatchFace.updateWatchfaceInstance(
            newInstanceId,
            userStyle.toWireFormat()
        )
    }

    override val instanceId: String
        get() = iInteractiveWatchFace.instanceId

    override val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iInteractiveWatchFace.userStyleSchema)

    override val complicationSlotsState: Map<Int, ComplicationSlotState>
        get() = iInteractiveWatchFace.complicationDetails.associateBy(
            { it.id },
            { ComplicationSlotState(it.complicationState) }
        )

    override fun close() = TraceEvent("InteractiveWatchFaceClientImpl.close").use {
        iInteractiveWatchFace.release()
    }

    override fun displayPressedAnimation(complicationSlotId: Int) = TraceEvent(
        "InteractiveWatchFaceClientImpl.bringAttentionToComplication"
    ).use {
        iInteractiveWatchFace.bringAttentionToComplication(complicationSlotId)
    }

    override fun sendTouchEvent(
        xPosition: Int,
        yPosition: Int,
        @TapType tapType: Int
    ) = TraceEvent("InteractiveWatchFaceClientImpl.sendTouchEvent").use {
        iInteractiveWatchFace.sendTouchEvent(xPosition, yPosition, tapType)
    }

    override val contentDescriptionLabels: List<ContentDescriptionLabel>
        get() = iInteractiveWatchFace.contentDescriptionLabels.map {
            ContentDescriptionLabel(
                it.text.toApiComplicationText(),
                it.bounds,
                it.tapAction
            )
        }

    override fun setWatchUiState(
        watchUiState: androidx.wear.watchface.client.WatchUiState
    ) = TraceEvent(
        "InteractiveWatchFaceClientImpl.setSystemState"
    ).use {
        iInteractiveWatchFace.setWatchUiState(
            WatchUiState(
                watchUiState.inAmbientMode,
                watchUiState.interruptionFilter
            )
        )
    }

    override fun performAmbientTick() = TraceEvent(
        "InteractiveWatchFaceClientImpl.performAmbientTick"
    ).use {
        iInteractiveWatchFace.ambientTickUpdate()
    }

    override fun addClientDisconnectListener(
        listener: InteractiveWatchFaceClient.ClientDisconnectListener,
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
        listener: InteractiveWatchFaceClient.ClientDisconnectListener
    ) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }

    override fun isConnectionAlive() = iInteractiveWatchFace.asBinder().isBinderAlive
}
