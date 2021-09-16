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

import androidx.wear.watchface.control.data.ComplicationRenderParams;
import androidx.wear.watchface.control.data.WatchFaceRenderParams;
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat;
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat;

/**
 * Interface for interacting with a stateless headless instance of a watch face.
 *
 * @hide
 */
interface IHeadlessWatchFace {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 8

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
     * Returns the reference preview time for this watch face in milliseconds since the epoch.
     *
     * @since API version 1.
     */
    long getPreviewReferenceTimeMillis() = 2;

    /**
      * Gets the current user style schema which SysUI & companion will use to construct the style
      * configuration UI. Note this could change e.g. seasonally.
      *
      * @since API version 1.
      */
    UserStyleSchemaWireFormat getUserStyleSchema() = 3;

    /**
      * Returns the current {@link ComplicationStateWireFormat} for each complication slot. Note these
      * details can change, typically in response to styling.
      *
      * @since API version 1.
      */
    List<IdAndComplicationStateWireFormat> getComplicationState() = 4;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}) with a screenshot of the
     * Watch Face.
     *
     * @since API version 1.
     * @param params The {@link WatchFaceRenderParams} for this screenshot.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the watch
     *     face with the requested settings
     * TODO(alexclarke): Refactor to return a parcelable rather than a bundle.
     */
    Bundle renderWatchFaceToBitmap(in WatchFaceRenderParams params) = 5;

    /**
     * Request for a {@link Bundle} containing a WebP compressed shared memory backed {@link Bitmap}
     * (see {@link SharedMemoryImage#ashmemCompressedImageBundleToBitmap}).
     *
     * @since API version 1.
     * @param params The {@link ComplicationRenderParams} for this screenshot.
     * @return A bundle containing a compressed shared memory backed {@link Bitmap} of the
     *     complication with the requested settings
     * TODO(alexclarke): Refactor to return a parcelable rather than a bundle.
     */
    Bundle renderComplicationToBitmap(in ComplicationRenderParams params) = 6;

    /**
     * Releases the watch face instance.  It is an error to issue any further commands on any AIDLs
     * associated with this watch face instance.
     *
     * @since API version 1.
     */
    oneway void release() = 7;
}
