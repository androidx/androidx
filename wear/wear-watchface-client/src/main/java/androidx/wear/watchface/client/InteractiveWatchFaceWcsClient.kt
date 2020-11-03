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
import android.os.IBinder
import android.support.wearable.watchface.ashmemCompressedImageBundleToBitmap
import androidx.annotation.IntRange
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema

/** Controls a stateful remote interactive watch face with an interface tailored for WCS. */
public class InteractiveWatchFaceWcsClient internal constructor(
    private val iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS
) : AutoCloseable {

    /** Constructs an [InteractiveWatchFaceWcsClient] from an [IBinder]. */
    public constructor(binder: IBinder) : this(IInteractiveWatchFaceWCS.Stub.asInterface(binder))

    /**
     * Sends new ComplicationData to the watch face. Note this doesn't have to be a full update,
     * it's possible to update just one complication at a time, but doing so may result in a less
     * visually clean transition.
     */
    public fun updateComplicationData(idToComplicationData: Map<Int, ComplicationData>) {
        iInteractiveWatchFaceWcs.updateComplicationData(
            idToComplicationData.map {
                IdAndComplicationDataWireFormat(it.key, it.value.asWireComplicationData())
            }
        )
    }

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
    public fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        @IntRange(from = 0, to = 100)
        compressionQuality: Int,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idAndComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = iInteractiveWatchFaceWcs.takeWatchFaceScreenshot(
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
    ).ashmemCompressedImageBundleToBitmap()

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long
        get() = iInteractiveWatchFaceWcs.previewReferenceTimeMillis

    /**
     * Sets the watch face's current [UserStyle]. Note this may alter [complicationStateWireFormat].
     */
    public fun setUserStyle(userStyle: UserStyle) {
        iInteractiveWatchFaceWcs.setCurrentUserStyle(userStyle.toWireFormat())
    }

    /** Returns the ID of this watch face instance. */
    public val instanceId: String
        get() = iInteractiveWatchFaceWcs.instanceId

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iInteractiveWatchFaceWcs.userStyleSchema)

    /**
     * Map of complication ids to [ComplicationStateWireFormat] for each complication slot. Note
     * this can change, typically in response to styling.
     */
    public val complicationStateWireFormat: Map<Int, ComplicationStateWireFormat>
        get() = iInteractiveWatchFaceWcs.complicationDetails.associateBy(
            { it.id },
            { it.complicationState }
        )

    /**
     * Releases the watch face instance.  It is an error to issue any further commands on this
     * interface.
     */
    override fun close() {
        iInteractiveWatchFaceWcs.release()
    }

    /** Returns the associated [IBinder]. Allows this interface to be passed over AIDL. */
    public fun asBinder(): IBinder = iInteractiveWatchFaceWcs.asBinder()
}