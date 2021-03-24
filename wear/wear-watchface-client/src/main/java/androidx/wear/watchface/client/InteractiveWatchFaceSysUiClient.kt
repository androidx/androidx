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

import android.app.PendingIntent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.support.wearable.watchface.SharedMemoryImage
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.toApiComplicationText
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IInteractiveWatchFaceSysUI
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import java.util.Objects

/**
 * The type of tap event passed to the watch face.
 * @hide
 */
@IntDef(
    InteractiveWatchFaceSysUiClient.TAP_TYPE_DOWN,
    InteractiveWatchFaceSysUiClient.TAP_TYPE_CANCEL,
    InteractiveWatchFaceSysUiClient.TAP_TYPE_UP
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class TapType

/**
 * Controls a stateful remote interactive watch face with an interface tailored for SysUI the
 * WearOS launcher app. Typically this will be used for the current active watch face.
 *
 * Note clients should call [close] when finished.
 */
public interface InteractiveWatchFaceSysUiClient : AutoCloseable {

    public companion object {
        /** Indicates a "down" touch event on the watch face. */
        public const val TAP_TYPE_DOWN: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_DOWN

        /**
         * Indicates that a previous [TAP_TYPE_DOWN] event has been canceled. This generally happens
         * when the watch face is touched but then a move or long press occurs.
         */
        public const val TAP_TYPE_CANCEL: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_CANCEL

        /**
         * Indicates that an "up" event on the watch face has occurred that has not been consumed by
         * another activity. A [TAP_TYPE_DOWN] always occur first. This event will not occur if a
         * [TAP_TYPE_CANCEL] is sent.
         */
        public const val TAP_TYPE_UP: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_UP
    }

    /**
     * Sends a tap event to the watch face for processing.
     */
    public fun sendTouchEvent(@Px xPosition: Int, @Px yPosition: Int, @TapType tapType: Int)

    /**
     * Describes regions of the watch face for use by a screen reader.
     *
     * @param text [ComplicationText] associated with the region, to be read by the screen reader.
     * @param bounds [Rect] describing the area of the feature on screen.
     * @param tapAction [PendingIntent] to be used if the screen reader's user triggers a tap
     *     action.
     */
    public class ContentDescriptionLabel(
        private val text: ComplicationText,
        public val bounds: Rect,
        public val tapAction: PendingIntent?
    ) {
        /**
         * Returns the text that should be displayed for the given timestamp.
         *
         * @param resources [Resources] from the current [android.content.Context]
         * @param dateTimeMillis milliseconds since epoch, e.g. from [System.currentTimeMillis]
         */
        public fun getTextAt(resources: Resources, dateTimeMillis: Long): CharSequence =
            text.getTextAt(resources, dateTimeMillis)

        override fun equals(other: Any?): Boolean =
            other is ContentDescriptionLabel &&
                text == other.text &&
                bounds == other.bounds &&
                tapAction == other.tapAction

        override fun hashCode(): Int {
            return Objects.hash(
                text,
                bounds,
                tapAction
            )
        }
    }

    /**
     * Returns the [ContentDescriptionLabel]s describing the watch face, for the use by screen
     * readers.
     */
    public val contentDescriptionLabels: List<ContentDescriptionLabel>

    /**
     * Renders the watchface to a shared memory backed [Bitmap] with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     *     if null then the existing complication data if any is used.
     * @return A shared memory backed [Bitmap] containing a screenshot of the watch face with the
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

    /** Updates the watch faces [SystemState]. */
    public fun setSystemState(systemState: SystemState)

    /** Returns the ID of this watch face instance. */
    public val instanceId: String

    /** Triggers watch face rendering into the surface when in ambient mode. */
    public fun performAmbientTick()
}

internal class InteractiveWatchFaceSysUiClientImpl internal constructor(
    private val iInteractiveWatchFaceSysUI: IInteractiveWatchFaceSysUI
) : InteractiveWatchFaceSysUiClient {

    override fun sendTouchEvent(
        xPosition: Int,
        yPosition: Int,
        @TapType tapType: Int
    ) = TraceEvent("InteractiveWatchFaceSysUiClientImpl.sendTouchEvent").use {
        iInteractiveWatchFaceSysUI.sendTouchEvent(xPosition, yPosition, tapType)
    }

    override val contentDescriptionLabels:
        List<InteractiveWatchFaceSysUiClient.ContentDescriptionLabel>
            get() = iInteractiveWatchFaceSysUI.contentDescriptionLabels.map {
                InteractiveWatchFaceSysUiClient.ContentDescriptionLabel(
                    it.text.toApiComplicationText(),
                    it.bounds,
                    it.tapAction
                )
            }

    @RequiresApi(27)
    override fun renderWatchFaceToBitmap(
        renderParameters: RenderParameters,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = TraceEvent("InteractiveWatchFaceSysUiClientImpl.renderWatchFaceToBitmap").use {
        SharedMemoryImage.ashmemReadImageBundle(
            iInteractiveWatchFaceSysUI.renderWatchFaceToBitmap(
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
        get() = iInteractiveWatchFaceSysUI.previewReferenceTimeMillis

    override fun setSystemState(systemState: SystemState) = TraceEvent(
        "InteractiveWatchFaceSysUiClientImpl.setSystemState"
    ).use {
        iInteractiveWatchFaceSysUI.setSystemState(
            androidx.wear.watchface.data.SystemState(
                systemState.inAmbientMode,
                systemState.interruptionFilter
            )
        )
    }

    override val instanceId: String
        get() = iInteractiveWatchFaceSysUI.instanceId

    override fun performAmbientTick() = TraceEvent(
        "InteractiveWatchFaceSysUiClientImpl.performAmbientTick"
    ).use {
        iInteractiveWatchFaceSysUI.ambientTickUpdate()
    }

    override fun close() = TraceEvent("InteractiveWatchFaceSysUiClientImpl.close").use {
        iInteractiveWatchFaceSysUI.release()
    }
}
