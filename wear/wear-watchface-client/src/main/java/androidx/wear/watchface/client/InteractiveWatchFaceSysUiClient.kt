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
import android.os.IBinder
import android.support.wearable.complications.TimeDependentText
import android.support.wearable.watchface.SharedMemoryImage
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IInteractiveWatchFaceSysUI
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.data.SystemState
import androidx.wear.watchface.style.UserStyle
import java.util.Objects

/**
 * The type of tap event passed to the watch face.
 * @hide
 */
@IntDef(
    InteractiveWatchFaceSysUiClient.TAP_TYPE_TOUCH,
    InteractiveWatchFaceSysUiClient.TAP_TYPE_TOUCH_CANCEL,
    InteractiveWatchFaceSysUiClient.TAP_TYPE_TAP
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
        public const val TAP_TYPE_TOUCH: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_TOUCH

        /**
         * Indicates that a previous TAP_TYPE_TOUCH event has been canceled. This generally happens
         * when the watch face is touched but then a move or long press occurs.
         */
        public const val TAP_TYPE_TOUCH_CANCEL: Int =
            IInteractiveWatchFaceSysUI.TAP_TYPE_TOUCH_CANCEL

        /**
         * Indicates that an "up" event on the watch face has occurred that has not been consumed by
         * another activity. A TAP_TYPE_TOUCH always occur first. This event will not occur if a
         * TAP_TYPE_TOUCH_CANCEL is sent.
         */
        public const val TAP_TYPE_TAP: Int = IInteractiveWatchFaceSysUI.TAP_TYPE_TAP

        /**
         * Constructs an [InteractiveWatchFaceSysUiClient] from the [IBinder] returned by
         * [asBinder].
         */
        @JvmStatic
        public fun createFromBinder(binder: IBinder): InteractiveWatchFaceSysUiClient =
            InteractiveWatchFaceSysUiClientImpl(binder)
    }

    /**
     * Sends a tap event to the watch face for processing.
     */
    public fun sendTouchEvent(xPosition: Int, yPosition: Int, @TapType tapType: Int)

    /** Describes regions of the watch face for use by a screen reader. */
    public class ContentDescriptionLabel(
        /** Text associated with the region, to be read by the screen reader. */
        private val text: TimeDependentText,

        /** Area of the feature on screen. */
        public val bounds: Rect,

        /** [PendingIntent] to be used if the screen reader's user triggers a tap action. */
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
     * Requests for a WebP compressed shared memory backed [Bitmap] containing a screenshot of
     * the watch face with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param compressionQuality The WebP compression quality, 100 = loss less.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the current style is used.
     * @param idAndComplicationData Map of complication ids to [ComplicationData] to render with, or
     *     if null then the existing complication data if any is used.
     * @return A WebP compressed shared memory backed [Bitmap] containing a screenshot of the watch
     *     face with the given settings.
     */
    @RequiresApi(27)
    public fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        @IntRange(from = 0, to = 100)
        compressionQuality: Int,
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

    /** Returns the associated [IBinder]. Allows this interface to be passed over AIDL. */
    public fun asBinder(): IBinder
}

internal class InteractiveWatchFaceSysUiClientImpl internal constructor(
    private val iInteractiveWatchFaceSysUI: IInteractiveWatchFaceSysUI
) : InteractiveWatchFaceSysUiClient {

    constructor(binder: IBinder) : this(IInteractiveWatchFaceSysUI.Stub.asInterface(binder))

    override fun sendTouchEvent(xPosition: Int, yPosition: Int, @TapType tapType: Int) {
        iInteractiveWatchFaceSysUI.sendTouchEvent(xPosition, yPosition, tapType)
    }

    override val contentDescriptionLabels:
        List<InteractiveWatchFaceSysUiClient.ContentDescriptionLabel>
            get() = iInteractiveWatchFaceSysUI.contentDescriptionLabels.map {
                InteractiveWatchFaceSysUiClient.ContentDescriptionLabel(
                    it.text,
                    it.bounds,
                    it.tapAction
                )
            }

    @RequiresApi(27)
    override fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        @IntRange(from = 0, to = 100)
        compressionQuality: Int,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap =
        SharedMemoryImage.ashmemCompressedImageBundleToBitmap(
            iInteractiveWatchFaceSysUI.takeWatchFaceScreenshot(
                WatchfaceScreenshotParams(
                    renderParameters.toWireFormat(),
                    compressionQuality,
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

    override val previewReferenceTimeMillis: Long
        get() = iInteractiveWatchFaceSysUI.previewReferenceTimeMillis

    override fun setSystemState(systemState: SystemState) {
        iInteractiveWatchFaceSysUI.setSystemState(
            SystemState(
                systemState.inAmbientMode,
                systemState.interruptionFilter
            )
        )
    }

    override val instanceId: String
        get() = iInteractiveWatchFaceSysUI.instanceId

    override fun performAmbientTick() {
        iInteractiveWatchFaceSysUI.ambientTickUpdate()
    }

    override fun close() {
        iInteractiveWatchFaceSysUI.release()
    }

    override fun asBinder(): IBinder = iInteractiveWatchFaceSysUI.asBinder()
}
