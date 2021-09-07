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
import androidx.wear.watchface.control.IWatchfaceReadyListener;
import androidx.wear.watchface.control.data.WatchFaceRenderParams;
import androidx.wear.watchface.data.WatchUiState;
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat;
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat;
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

/**
 * Interface for interacting with an interactive instance of a watch face.
 *
 * @hide
 */
interface IInteractiveWatchFace {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 18

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 2;

    /** Indicates a "down" touch event on the watch face. */
    const int TAP_TYPE_DOWN = 0;

    /**
     * Indicates that a previous {@link #TAP_TYPE_DOWN} event has been canceled. This generally
     * happens when the watch face is touched but then a move or long press occurs.
     */
    const int TAP_TYPE_CANCEL = 1;

    /**
     * Indicates that an "up" event on the watch face has occurred that has not been consumed by
     * another activity. A {@link #TAP_TYPE_DOWN} always occur first. This event will not occur if a
     * {@link #TAP_TYPE_CANCEL} is sent.
     *
     */
    const int TAP_TYPE_UP = 2;

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
     * Sends a list of pairs of complication ID & ComplicationData.
     *
     * @since API version 1.
     */
    oneway void updateComplicationData(
            in List<IdAndComplicationDataWireFormat> complicationData) = 4;

    /**
     * Renames this instance to newInstanceId, sets the current user style
     * ({@link UserStyleWireFormat}) which contains a map of style setting id to option id, and
     * clears complication data.
     *
     * @since API version 1.
     */
    oneway void updateWatchfaceInstance(
            in String newInstanceId, in UserStyleWireFormat userStyle) = 5;

    /**
     * Returns the reference preview time for this watch face in milliseconds since the epoch.
     *
     * @since API version 1.
     */
    long getPreviewReferenceTimeMillis() = 7;

    /**
      * Gets the current user style schema which SysUI & companion will use to construct the style
      * configuration UI. Note this could change e.g. seasonally.
      *
      * @since API version 1.
      */
    UserStyleSchemaWireFormat getUserStyleSchema() = 8;

    /**
      * Returns the current {@link ComplicationState} for each complication slot. Note these
      * details can change, typically in response to styling.
      *
      * @since API version 1.
      * @param allComplicationSlots Map of id to {@link ComplicationState} for each slot.
      */
    List<IdAndComplicationStateWireFormat> getComplicationDetails() = 9;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}) with a screenshot of the
     * Watch Face with the specified DrawMode (see {@link androidx.wear.watchface.DrawMode}) and
     * calendarTimeMillis.
     *
     * @since API version 1.
     * @param params The {@link WatchFaceRenderParams} for this screenshot.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the watch
     *     face with the requested settings
     * TODO(alexclarke): Refactor to return a parcelable rather than a bundle.
     */
    Bundle renderWatchFaceToBitmap(in WatchFaceRenderParams params) = 10;

    /**
     * If there's no {@link IInteractiveWatchFaceSysUI} holding a reference then the
     * instance is diposed of. It is an error to issue any further AIDL commands via this interface.
     *
     * @since API version 1.
     */
    oneway void release() = 11;

    /**
     * Requests the specified complication slot is highlighted for a short period to bring attention
     * to it.
     *
     * @since API version 1.
     */
    oneway void bringAttentionToComplication(in int complicationSlotId) = 12;

    /**
     * Forwards a touch event for the WatchFace to process.
     *
     * @param xPos X Coordinate of the touch event
     * @param yPos Y Coordinate of the touch event
     * @param tapType One of {@link #TAP_TYPE_DOWN}, {@link #TAP_TYPE_CANCEL}, {@link #TAP_TYPE_UP}
     * @since API version 1.
     */
    oneway void sendTouchEvent(in int xPos, in int yPos, in int tapType) = 13;

    /**
     * Called periodically when the watch is in ambient mode to update the watchface.
     *
     * @since API version 1.
     */
    oneway void ambientTickUpdate() = 14;

    /**
     * Sends the current {@link WatchUiState} to the Watch Face.
     *
     * @since API version 1.
     */
    oneway void setWatchUiState(in WatchUiState watchUiState) = 15;

    /**
     * Gets the labels to be read aloud by screen readers. The results will change depending on the
     * current style and complications.  Note the labes include the central time piece in addition
     * to any complications.
     *
     * @since API version 1.
     */
    ContentDescriptionLabel[] getContentDescriptionLabels() = 16;

    /**
     * Adds a listener that will be called when the watch face is ready to render. If the watchface
     * is already ready this will be called immediately.
     *
     * @since API version 2.
     */
    oneway void addWatchfaceReadyListener(in IWatchfaceReadyListener listener) = 17;
}
