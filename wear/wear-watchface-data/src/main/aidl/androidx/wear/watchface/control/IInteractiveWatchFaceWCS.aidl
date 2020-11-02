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

import androidx.wear.watchface.control.data.WatchfaceScreenshotParams;
import androidx.wear.watchface.data.SystemState;
import androidx.wear.watchface.data.IdAndComplicationDataWireFormat;
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat;
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat;
import androidx.wear.watchface.style.data.UserStyleWireFormat;

/**
 * Interface for interacting with an interactive instance of a watch face from WCS. SysUI can also
 * control the same instance via {@link IInteractiveWatchFaceSysUI}.
 *
 * @hide
 */
interface IInteractiveWatchFaceWCS {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 12

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 1;

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
    oneway void updateComplicationData(in List<IdAndComplicationDataWireFormat> complicationData) = 4;

    /**
     * Sets the current user style ({@link UserStyleWireFormat}) which contains a map of style
     * setting id to option id.
     *
     * @since API version 1.
     */
    oneway void setCurrentUserStyle(in UserStyleWireFormat userStyle) = 5;

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
     * @param params The {@link WatchfaceScreenshotParams} for this screenshot.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the watch
     *     face with the requested settings
     * TODO(alexclarke): Refactor to return a parcelable rather than a bundle.
     */
    Bundle takeWatchFaceScreenshot(in WatchfaceScreenshotParams params) = 10;

    /**
     * If there's no {@link IInteractiveWatchFaceSysUI} holding a reference then the
     * instance is diposed of. It is an error to issue any further AIDL commands via this interface.
     *
     * @since API version 1.
     */
    oneway void release() = 11;
}
