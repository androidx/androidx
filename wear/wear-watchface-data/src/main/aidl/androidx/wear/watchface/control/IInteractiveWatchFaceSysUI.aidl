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

package androidx.wear.watchface.control;

import android.support.wearable.watchface.accessibility.ContentDescriptionLabel;
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams;
import androidx.wear.watchface.data.SystemState;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

/**
 * Interface for interacting with an interactive instance of a watch face from SysUI. WCS can also
 * control the same instance via {@link IInteractiveWatchFaceWCS}.
 *
 * @hide
 */
interface IInteractiveWatchFaceSysUI {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 10

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 1;

    /** Indicates a "down" touch event on the watch face. */
    const int TAP_TYPE_TOUCH = 0;

    /**
     * Indicates that a previous TAP_TYPE_TOUCH event has been canceled. This generally happens when
     * the watch face is touched but then a move or long press occurs.
     */
    const int TAP_TYPE_TOUCH_CANCEL = 1;

    /**
     * Indicates that an "up" event on the watch face has occurred that has not been consumed by
     * another activity. A TAP_TYPE_TOUCH always occur first. This event will not occur if a
     * TAP_TYPE_TOUCH_CANCEL is sent.
     *
     */
    const int TAP_TYPE_TAP = 2;

    /**
     * Returns the version number for this API which the client can use to determine which methods
     * are available.
     *
     * @since API version 1.
     */
    int getApiVersion() = 1;

    /**
     * Returns the ID parameter set at creation.
     *
     * @since API version 1.
     */
    String getInstanceId() = 2;

    /**
     * Returns the reference preview time for this watch face in milliseconds since the epoch.
     *
     * @since API version 1.
     */
    long getPreviewReferenceTimeMillis() = 3;

    /**
     * Forwards a touch event for the WatchFace to process.
     *
     * @param xPos X Coordinate of the touch event
     * @param yPos Y Coordinate of the touch event
     * @param tapType One of {@link #TAP_TYPE_TOUCH}, {@link #TAP_TYPE_TOUCH_CANCEL},
     *    {@link #TAP_TYPE_TAP}
     * @since API version 1.
     */
    oneway void sendTouchEvent(in int xPos, in int yPos, in int tapType) = 4;

    /**
     * Called periodically when the watch is in ambient mode to update the watchface.
     *
     * @since API version 1.
     */
    oneway void ambientTickUpdate() = 5;

    /**
     * Sends the current system state to the Watch Face.
     *
     * @since API version 1.
     */
    oneway void setSystemState(in SystemState systemState) = 6;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}) with a screenshot of the
     * Watch Face with the specified DrawMode (see {@link androidx.wear.watchface.DrawMode}) and
     * calendarTimeMillis.
     *
     * @since API version 1.
     * @param params The {@link WatchfaceScreenshotParams} for this screenshot.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the watch
     *     face with the requested settings
     * TODO(alexclarke): Refactor to return a parcelable rather than a bundle.
     */
    Bundle takeWatchFaceScreenshot(in WatchfaceScreenshotParams params) = 7;

    /**
     * Gets the labels to be read aloud by screen readers. The results will change depending on the
     * current style and complications.  Note the labes include the central time piece in addition
     * to any complications.
     *
     * @since API version 1.
     */
    ContentDescriptionLabel[] getContentDescriptionLabels() = 8;

    /**
     * If there's no {@link IInteractiveWatchFaceWCS} holding a reference then the instance is
     * diposed of. It is an error to issue any further AIDL commands via this interface.
     *
     * @since API version 1.
     */
    oneway void release() = 9;
}
