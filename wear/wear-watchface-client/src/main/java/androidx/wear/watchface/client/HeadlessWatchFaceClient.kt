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
import androidx.wear.watchface.control.IHeadlessWatchFace
import androidx.wear.watchface.control.data.ComplicationScreenshotParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.data.ComplicationDetails
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema

/** Controls a stateless remote headless watch face. */
public class HeadlessWatchFaceClient internal constructor(
    private val iHeadlessWatchFace: IHeadlessWatchFace
) : AutoCloseable {

    /** Constructs a [HeadlessWatchFaceClient] from a [IBinder]. */
    public constructor(binder: IBinder) : this(IHeadlessWatchFace.Stub.asInterface(binder))

    /** The UTC reference preview time for this watch face in milliseconds since the epoch. */
    public val previewReferenceTimeMillis: Long
        get() = iHeadlessWatchFace.previewReferenceTimeMillis

    /** The watch face's [UserStyleSchema]. */
    public val userStyleSchema: UserStyleSchema
        get() = UserStyleSchema(iHeadlessWatchFace.userStyleSchema)

    /**
     * Map of complication ids to [ComplicationDetails] for each complication slot. Note this can
     * change, typically in response to styling.
     */
    public val complicationDetails: Map<Int, ComplicationDetails>
        get() = iHeadlessWatchFace.complicationDetails.associateBy(
            { it.id },
            { it.complicationDetails }
        )

    /**
     * Requests for a WebP compressed shared memory backed [Bitmap] containing a screenshot of
     * the watch face with the given settings.
     *
     * @param renderParameters The [RenderParameters] to draw with.
     * @param compressionQuality The WebP compression quality, 100 = loss less.
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with.
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used.
     * @param idToComplicationData Map of complication ids to [ComplicationData] to render with, or
     *     if null complications are not rendered.
     * @return A WebP compressed shared memory backed [Bitmap] containing a screenshot of the watch
     *     face with the given settings.
     */
    public fun takeWatchFaceScreenshot(
        renderParameters: RenderParameters,
        @IntRange(from = 0, to = 100)
        compressionQuality: Int,
        calendarTimeMillis: Long,
        userStyle: UserStyle?,
        idToComplicationData: Map<Int, ComplicationData>?
    ): Bitmap = iHeadlessWatchFace.takeWatchFaceScreenshot(
        WatchfaceScreenshotParams(
            renderParameters.toWireFormat(),
            compressionQuality,
            calendarTimeMillis,
            userStyle?.toWireFormat(),
            idToComplicationData?.map {
                IdAndComplicationDataWireFormat(
                    it.key,
                    it.value.asWireComplicationData()
                )
            }
        )
    ).ashmemCompressedImageBundleToBitmap()

    /**
     * Requests for a WebP compressed shared memory backed [Bitmap] containing a screenshot of
     * the complication with the given settings.
     *
     * @param complicationId The id of the complication to render
     * @param renderParameters The [RenderParameters] to draw with
     * @param compressionQuality The WebP compression quality, 100 = loss less
     * @param calendarTimeMillis The UTC time in milliseconds since the epoch to render with
     * @param complicationData the [ComplicationData] to render with
     * @param userStyle Optional [UserStyle] to render with, if null the default style is used
     * @return A WebP compressed shared memory backed [Bitmap] containing a screenshot of the watch
     *     face with the given settings, or `null` if [complicationId] is unrecognized.
     */
    public fun takeComplicationScreenshot(
        complicationId: Int,
        renderParameters: RenderParameters,
        @IntRange(from = 0, to = 100)
        compressionQuality: Int,
        calendarTimeMillis: Long,
        complicationData: ComplicationData,
        userStyle: UserStyle?,
    ): Bitmap? = iHeadlessWatchFace.takeComplicationScreenshot(
        ComplicationScreenshotParams(
            complicationId,
            renderParameters.toWireFormat(),
            compressionQuality,
            calendarTimeMillis,
            complicationData.asWireComplicationData(),
            userStyle?.toWireFormat(),
        )
    ).ashmemCompressedImageBundleToBitmap()

    /**
     * Releases the watch face instance.  It is an error to issue any further commands on this
     * interface.
     */
    override fun close() {
        iHeadlessWatchFace.release()
    }

    /** Returns the associated [IBinder]. Allows this interface to be passed over AIDL. */
    public fun asBinder(): IBinder = iHeadlessWatchFace.asBinder()
}
