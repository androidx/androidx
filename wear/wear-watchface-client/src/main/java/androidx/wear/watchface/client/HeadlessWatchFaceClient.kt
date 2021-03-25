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
import androidx.annotation.RequiresApi
import androidx.wear.complications.data.ComplicationData
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting

/**
 * Controls a stateless remote headless watch face.  This is mostly intended for use by watch face
 * editor UIs which need to generate screenshots for various styling configurations without
 * affecting the current watchface.
 *
 * Note clients should call [close] when finished.
 */
public interface HeadlessWatchFaceClient : AutoCloseable {
    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema

    /**
     * Map of complication ids to [ComplicationState] for each [Complication] registered with the
     * watch face's [ComplicationsManager]. The ComplicationState is based on the initial state of
     * each Complication plus any overrides from the default style's
     * [ComplicationsUserStyleSetting]. Because the style can't change, ComplicationState is
     * immutable for a headless watch face.
     */
    public val complicationsState: Map<Int, ComplicationState>

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used.
     * @param idToComplicationData Map of complication ids to [ComplicationData] to render with, or
     *     if null complications are not rendered.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
     *     given settings.
     */
    @RequiresApi(27)
    public fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap

    /**
     * Renders the complication to a shared memory backed [Bitmap] with the given settings.
     *
     * @param complicationId The id of the complication to render
     * @param renderParameters The [RenderParameters] to draw with
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with
     * @param complicationData the [ComplicationData] to render with
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
     *     given settings, or `null` if [complicationId] is unrecognized.
     */
    @RequiresApi(27)
    public fun renderComplicationToBitmap(
        complicationId: Int,
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        complicationData: ComplicationData,
        userStyle: UserStyle?,
    ): Bitmap?
}

internal class HeadlessWatchFaceClientImpl internal constructor(
    private val iHeadlessWatchFace: IHeadlessWatchFace
) : HeadlessWatchFaceClient {

    override val previewReferenceTimeMillis: Long
        get() = iHeadlessWatchFace.previewReferenceTimeMillis

    override val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iHeadlessWatchFace.userStyleSchema)

    override val complicationsState: Map<Int, ComplicationState>
        get() = iHeadlessWatchFace.complicationState.associateBy(
            { it.id },
            { ComplicationState(it.complicationState) }
        )

    @RequiresApi(27)
    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = TraceEvent("HeadlessWatchFaceClientImpl.renderWatchFaceToBitmap").use {
        SharedMemoryImage.ashmemReadImageBundle(
            iHeadlessWatchFace.renderWatchFaceToBitmap(
                WatchFaceRenderParams(
                    renderParameters.toWireFormat(),
                    calendarTimeMillis,
                    userStyle?.toWireFormat(),
                    idToComplicationData?.map {
                        IdAndComplicationDataWireFormat(
                            it.key,
                            it.value.asWireComplicationData()
                        )
                    }
                )
            )
        )
    }

    @RequiresApi(27)
    override fun renderComplicationToBitmap(
        complicationId: Int,
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        complicationData: ComplicationData,
        userStyle: UserStyle?,
    ): Bitmap? = TraceEvent("HeadlessWatchFaceClientImpl.renderComplicationToBitmap").use {
        iHeadlessWatchFace.renderComplicationToBitmap(
            ComplicationRenderParams(
                complicationId,
                renderParameters.toWireFormat(),
                calendarTimeMillis,
                complicationData.asWireComplicationData(),
                userStyle?.toWireFormat(),
            )
        )?.let {
            SharedMemoryImage.ashmemReadImageBundle(it)
        }
    }

    override fun close() = TraceEvent("HeadlessWatchFaceClientImpl.close").use {
        iHeadlessWatchFace.release()
    }
}
