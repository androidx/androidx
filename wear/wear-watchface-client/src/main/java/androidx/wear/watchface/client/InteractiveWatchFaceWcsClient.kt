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
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.wear.complications.data.ComplicationData
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.data.UserStyleWireFormat

/**
 * Controls a stateful remote interactive watch face with an interface tailored for WCS the
 * WearOS system server responsible for watch face management. Typically this will be used for
 * the current active watch face.
 *
 * Note clients should call [close] when finished.
 */
public interface InteractiveWatchFaceWcsClient : AutoCloseable {
    /**
     * Sends new ComplicationData to the watch face. Note this doesn't have to be a full update,
     * it's possible to update just one complication at a time, but doing so may result in a less
     * visually clean transition.
     */
    public fun updateComplicationData(idToComplicationData: Map<Int, ComplicationData>)

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     *     if null then the existing complication data if any is used.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch  face with the
     *     given settings.
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
     * disabling complications, which will be visible via [ComplicationState.isEnabled].
     */
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle)

    /**
     * Renames this instance to [newInstanceId] (must be unique, usually this would be different
     * from the old ID but that's not a requirement). Sets the current [UserStyle] represented as a
     * Map<String, String> and clears any complication data. Setting the new UserStyle may have
     * a side effect of enabling or disabling complications, which will be visible via
     * [ComplicationState.isEnabled].
     */
    public fun updateWatchFaceInstance(newInstanceId: String, userStyle: Map<String, String>)

    /** Returns the ID of this watch face instance. */
    public val instanceId: String

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema

    /**
     * Map of complication ids to [ComplicationState] for each complication slot. Note
     * this can change, typically in response to styling.
     */
    public val complicationsState: Map<Int, ComplicationState>

    /** Returns the ID of the complication at the given coordinates or `null` if there isn't one.*/
    @SuppressWarnings("AutoBoxing")
    public fun getComplicationIdAt(@Px x: Int, @Px y: Int): Int? =
        complicationsState.asSequence().firstOrNull {
            it.value.isEnabled && when (it.value.boundsType) {
                ComplicationBoundsType.ROUND_RECT -> it.value.bounds.contains(x, y)
                ComplicationBoundsType.BACKGROUND -> false
                ComplicationBoundsType.EDGE -> false
                else -> false
            }
        }?.key

    /**
     * Requests that [ComplicationsManager.displayPressedAnimation] is called for [complicationId].
     */
    public fun displayPressedAnimation(complicationId: Int)
}

/** Controls a stateful remote interactive watch face with an interface tailored for WCS. */
internal class InteractiveWatchFaceWcsClientImpl internal constructor(
    private val iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS
) : InteractiveWatchFaceWcsClient {

    override fun updateComplicationData(
        idToComplicationData: Map<Int, ComplicationData>
    ) = TraceEvent("InteractiveWatchFaceWcsClientImpl.updateComplicationData").use {
        iInteractiveWatchFaceWcs.updateComplicationData(
            idToComplicationData.map {
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
    ): Bitmap = TraceEvent("InteractiveWatchFaceWcsClientImpl.renderWatchFaceToBitmap").use {
        SharedMemoryImage.ashmemReadImageBundle(
            iInteractiveWatchFaceWcs.renderWatchFaceToBitmap(
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
        get() = iInteractiveWatchFaceWcs.previewReferenceTimeMillis

    override fun updateWatchFaceInstance(newInstanceId: String, userStyle: UserStyle) = TraceEvent(
        "InteractiveWatchFaceWcsClientImpl.updateInstance"
    ).use {
        iInteractiveWatchFaceWcs.updateWatchfaceInstance(newInstanceId, userStyle.toWireFormat())
    }

    override fun updateWatchFaceInstance(
        newInstanceId: String,
        userStyle: Map<String, String>
    ) = TraceEvent(
        "InteractiveWatchFaceWcsClientImpl.updateInstance"
    ).use {
        iInteractiveWatchFaceWcs.updateWatchfaceInstance(
            newInstanceId,
            UserStyleWireFormat(userStyle)
        )
    }

    override val instanceId: String
        get() = iInteractiveWatchFaceWcs.instanceId

    override val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iInteractiveWatchFaceWcs.userStyleSchema)

    override val complicationsState: Map<Int, ComplicationState>
        get() = iInteractiveWatchFaceWcs.complicationDetails.associateBy(
            { it.id },
            { ComplicationState(it.complicationState) }
        )

    override fun close() = TraceEvent("InteractiveWatchFaceWcsClientImpl.close").use {
        iInteractiveWatchFaceWcs.release()
    }

    override fun displayPressedAnimation(complicationId: Int) = TraceEvent(
        "InteractiveWatchFaceWcsClientImpl.bringAttentionToComplication"
    ).use {
        iInteractiveWatchFaceWcs.bringAttentionToComplication(complicationId)
    }
}
