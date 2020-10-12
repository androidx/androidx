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
import android.support.wearable.watchface.IWatchFaceCommand;
import android.support.wearable.watchface.WatchFaceStyle;
import androidx.wear.watchface.data.ComplicationDetails;
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
    // Next Id: 14

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int WATCHFACE_SERVICE_API_VERSION = 3;

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
    void reserved1() = 5;

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

    /**
      * Updates the {@link ComplicationDetails} associated with a complication slot.
      *
      * @since API version 3.
      * @param id The complication id
      * @param complicationDetails The {@link ComplicationDetails} to set
      */
    void setComplicationDetails(int id, in ComplicationDetails complicationDetails) = 8;

    /**
      * Registers the user style schema which the companion will use to construct the style
      * configuration UI.
      *
      * @since API version 3.
      * @param styleSchema A {@link StyleSchemaWireFormat}.
      */
    void registerUserStyleSchema(in UserStyleSchemaWireFormat styleSchema) = 9;

    /**
     * Called when the user selects the user style for the watch. For some types of UI widget,
     * (e.g a LIST_VIEW) the Option must be from the list associated with the UserStyleCategory in
     * registerStyleSchema.
     *
     * @since API version 3.
     * @param style A {@link UserStyleWireFormat}.
     */
    void setCurrentUserStyle(in UserStyleWireFormat style) = 10;

    /**
     * Returns the user style (set by {@link #setCurrentUserStyle} if there is one or null
     * otherwise.
     *
     * @since API version 3.
     */
    UserStyleWireFormat getStoredUserStyle() = 11;

    /**
     * Registers whether the watch face is digital or analog with the system.
     *
     * @since API version 3.
     * @param watchFaceType The {@link WatchFaceType} which describes whether the watch face is
     *     digital or analog
     */
    void registerWatchFaceType(int watchFaceType) = 12;

    /**
     * Registers the {@link IWatchFaceCommand} with the system. This is a cleaner alternative to the
     * deprecated wallpaper commands.
     *
     * @since API version 3.
     */
    void registerIWatchFaceCommand(in Bundle bundle) = 13;
}
