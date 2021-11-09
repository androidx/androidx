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

import androidx.wear.watchface.control.IInteractiveWatchFace;
import androidx.wear.watchface.control.IHeadlessWatchFace;
import androidx.wear.watchface.control.IPendingInteractiveWatchFace;
import androidx.wear.watchface.control.data.DefaultProviderPoliciesParams;
import androidx.wear.watchface.control.data.GetComplicationSlotMetadataParams;
import androidx.wear.watchface.control.data.GetUserStyleSchemaParams;
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams;
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat;
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams;
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat;
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat;
import androidx.wear.watchface.editor.IEditorService;

/**
 * Interface of a service that allows the user to create watch face instances.
 *
 * @hide
 */
interface IWatchFaceControlService {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 7

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 3;

    /**
     * Returns the version number for this API which the client can use to determine which methods
     * are available.
     *
     * @since API version 1.
     */
    int getApiVersion() = 1;

    /**
     * Gets the {@link IInteractiveWatchFace} corresponding to the id of an existing watch
     * face instance, or null if there is no such instance. The id is set when the instance is
     * created, see {@link WallpaperInteractiveWatchFaceInstanceParams}.
     */
    IInteractiveWatchFace getInteractiveWatchFaceInstance(in String id) = 2;

    /**
     * Creates a headless WatchFace instance for the specified watchFaceName and returns an {@link
     * IHeadlessWatchFace} to control it or null if watchFaceName is unrecognized. A headless watch
     * face will not render asynchronously however it can to render screen shots (of the specified
     * size) upon request.
     *
     * <p> When finished {@link IHeadlessWatchFace#destroy} should be called to release
     * resources.
     *
     * @param params The {@link HeadlessWatchFaceInstanceParams} for the watch face to create
     * @since API version 1.
     */
    IHeadlessWatchFace createHeadlessWatchFaceInstance(
            in HeadlessWatchFaceInstanceParams params) = 3;

    /**
     * Either returns an existing IInteractiveWatchFace instance or othrwise schedules
     * creation of an IInteractiveWatchFace for the next time the wallpaper service connects and
     * calls WatchFaceService.onCreateEngine.
     *
     * @param params The {@link WallpaperInteractiveWatchFaceInstanceParams} for the watchface
     *      instance to be made when WatchFaceService.onCreateEngine is called. If an existing
     *      instance is returned this callback won't fire.
     * @param callback Callback fired when the wathface is created.
     * @return The existing {@link IInteractiveWatchFace} or null in which the callback will fire
     *      the next time the wallpaper service connects and calls WatchFaceService.onCreateEngine.
     * @since API version 1.
     */
    IInteractiveWatchFace getOrCreateInteractiveWatchFace(
            in WallpaperInteractiveWatchFaceInstanceParams params,
            in IPendingInteractiveWatchFace callback) = 4;

    /**
     * Returns the {@link IEditorService}
     *
     * @since API version 1.
     */
    IEditorService getEditorService() = 5;

    /**
     * Returns an array of {@link IdAndDefaultProviderPolicyWireFormat} describing the default
     * provider policy for the watch face's complications. Note this call does not create the
     * renderer so it's cheaper than creating a headless instance and querying ComplicationState.
     *
     * @since API version 2.
     */
    IdTypeAndDefaultProviderPolicyWireFormat[] getDefaultProviderPolicies(
            in DefaultProviderPoliciesParams params) = 6;

    /**
     * Returns the static {@link UserStyleSchemaWireFormat}
     *
     * @since API version 3.
     */
    UserStyleSchemaWireFormat getUserStyleSchema(in GetUserStyleSchemaParams params) = 7;

    /**
     * Returns the static {@link ComplicationSlotMetadataWireFormat}
     *
     * @since API version 3.
     */
    ComplicationSlotMetadataWireFormat[] getComplicationSlotMetadata(
            in GetComplicationSlotMetadataParams params) = 8;
}
