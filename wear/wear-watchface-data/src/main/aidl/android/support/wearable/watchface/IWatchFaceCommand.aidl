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

package android.support.wearable.watchface;

import android.support.wearable.complications.ComplicationData;
import androidx.wear.watchface.data.ImmutableSystemState;
import androidx.wear.watchface.data.IndicatorState;
import androidx.wear.watchface.data.SystemState;

/**
 * Interface for controlling the watchface from the wearable device.
 *
 * @hide
 */
interface IWatchFaceCommand {

    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 12

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int WATCHFACE_COMMAND_API_VERSION = 1;

    /**
     * Returns the version number for this API which the client can use to
     * determine which methods are available.
     */
    int getApiVersion() = 1;

    /**
     * Called periodically when the watch is in ambient mode to update the watchface.
     */
    void ambientUpdate() = 2;

    /**
     * Sends the current system state to the Watch Face.
     */
    void setSystemState(in SystemState systemState) = 3;

    /**
     * Sends the current watch indicator state to the Watch Face, only called if
     * {@link WatchFaceStyle#hideNotificationIndicator} is true.
     */
    void setIndicatorState(in IndicatorState indicatorState) = 4;

    /**
     * Sends the immutable system state to the Watch Face.
     */
    void setImmutableSystemState(in ImmutableSystemState immutableSystemState) = 5;

    /**
     * Sends new complication data for the specified complicationId.
     */
    void setComplicationData(in int complicationId, in ComplicationData data) = 6;

    /**
     * Request for a call to {@link IWatchFaceService#setStyle} with the {@link WatchFaceStyle}.
     *
     * Not to be confused with {@link androidx.wear.watchfacestyle.UserStyleCategory}.
     */
    void requestWatchFaceStyle() = 7;

    /**
     * Sets the user style which is a {@link Bundle} containing a map of style category id to
     * option id, see {@link androidx.wear.watchfacestyle.UserStyleCategory#bundleToStyleMap} for
     * details.
     */
    void setUserStyle(in Bundle userStyle) = 8;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}) with a screenshot of the
     * Watch Face with the specified DrawMode (see {@link androidx.wear.watchface.DrawMode}) and
     * calendarTimeMillis.
     *
     * @param drawMode The {@link androidx.wear.watchface.DrawMode} to render with
     * @param compressionQuality The WebP compression quality, 100 = lossless
     * @param calendarTimeMillis The calendar time (millis since the epoch) to render with
     * @param style A {@link Bundle} containing a mapping from {@link UserStyleCategory} to
     *    {@link UserStyleCategory.Option}s. If null then the current style is used.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the watch
     *     face with the requested settings
     */
    Bundle takeWatchfaceScreenshot(in int drawMode,
                                   in int compressionQuality,
                                   in long calendarTimeMillis,
                                   in Bundle style) = 9;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}) with a screenshot of the
     * complication with the specified {@link androidx.wear.watchface.DrawMode}, calendarTimeMillis
     * and {@link ComplicationData}.
     *
     * @param complicationId The watchface's ID of the complication to render
     * @param drawMode The {@link androidx.wear.watchface.DrawMode} to render with
     * @param compressionQuality The WebP compression quality, 100 = lossless
     * @param calendarTimeMillis The calendar time (millis since the epoch) to render with
     * @param complicationData The {@link ComplicationData} to render the complication with, if
     *     null then the last ComplicationData sent to the watch face if any is used
     * @param style A {@link Bundle} containing a mapping from {@link UserStyleCategory} to
     *    {@link UserStyleCategory.Option}s. If null then the current style is used.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the
     *     complication with the requested settings
     */
    Bundle takeComplicationScreenshot(in int complicationId,
                                      in int drawMode,
                                      in int compressionQuality,
                                      in long calendarTimeMillis,
                                      in ComplicationData complicationData,
                                      in Bundle style) = 10;

    /**
     * Forwards a touch event for the WatchFace to process.
     */
    void sendTouchEvent(in int xPos, in int yPos, in int tapType) = 11;
}
