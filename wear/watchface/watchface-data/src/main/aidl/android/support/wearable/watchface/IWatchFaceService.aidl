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

import android.content.ComponentName;
import android.os.Bundle;
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel;
import android.support.wearable.watchface.WatchFaceStyle;
import androidx.wear.watchface.style.data.UserStyleWireFormat;
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat;

/**
 * Interface of a service that allows the watch face to interact with the wearable.
 *
 * @hide
 */
interface IWatchFaceService {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 9

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int WATCHFACE_SERVICE_API_VERSION = 4;

    /**
     * Requests that the style for the provided watch face be set to the given style.
     *
     * @since API version 0.
     */
    void setStyle(in WatchFaceStyle style) = 0;

    /**
     * Sets which complications are currently active on the watch face.
     *
     * @since API version 0.
     */
    void setActiveComplications(in int[] ids, boolean updateAll) = 1;

    /**
     * Sets the default provider for a complication.
     *
     * @since API version 0.
     */
    void setDefaultComplicationProvider(
        int watchFaceComplicationId, in ComponentName provider, int type) = 2;

    /**
     * Sets a system provider as the default provider for a complication.
     * @since API version 0.
     */
    void setDefaultSystemComplicationProvider(
        int watchFaceComplicationId, int systemProvider, int type) = 3;

    /**
     * Sets the labels to be read aloud by screen readers.
     *
     * @since API version 0.
     */
    void setContentDescriptionLabels(in ContentDescriptionLabel[] labels) = 4;

    /** Reserved. Do not use. */
    void reserved5() = 5;

    /**
     * Sets the default provider for a complication, choosing the first element
     * from providersToTry and falling back to the next one if that doesn't
     * exist or finally fallbackSystemProvider if none exist. Note if
     * fallbackSystemProvider is -1 it will be ignored.
     *
     * @since API version 2
     */
    void setDefaultComplicationProviderWithFallbacks(
        int watchFaceComplicationId, in List<ComponentName> providersToTry,
        int fallbackSystemProvider, int type) = 6;

    /**
     * Returns the version number for this API which the client can use to
     * determine which methods are available. Note old implementations without
     * this method will return zero.
     *
     * @since API version 0.
     */
    int getApiVersion() = 7;

    /** Reserved. Do not use. */
    void reserved8() = 8;
}
