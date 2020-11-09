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

import androidx.wear.watchface.control.IInteractiveWatchFaceSysUI;
import androidx.wear.watchface.control.IInteractiveWatchFaceWCS;
import androidx.wear.watchface.control.IHeadlessWatchFace;
import androidx.wear.watchface.control.IPendingInteractiveWatchFaceWCS;
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams;
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams;

/**
 * Interface of a service that allows the user to create watch face instances.
 *
 * @hide
 */
interface IWatchFaceControlService {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 5

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
     * Gets the {@link IInteractiveWatchFaceSysUI} corresponding to the id of an existing watch
     * face instance, or null if there is no such instance. The id is set when the instance is
     * created, see {@link WallpaperInteractiveWatchFaceInstanceParams}.
     */
    IInteractiveWatchFaceSysUI getInteractiveWatchFaceInstanceSysUI(in String id) = 2;

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
     * Either returns an existing IInteractiveWatchFaceWCS instance or othrwise schedules
     * creation of an IInteractiveWatchFace for the next time the wallpaper service connects and
     * calls WatchFaceService.onCreateEngine.
     *
     * @param params The {@link WallpaperInteractiveWatchFaceInstanceParams} for the watchface
     *      instance to be made when WatchFaceService.onCreateEngine is called. If an existing
     *      instance is returned this callback won't fire.
     * @param callback Callback fired when the wathface is created.
     * @return The existing {@link IInteractiveWatchFaceWCS} or null in which the callback will fire
     *      the next time the wallpaper service connects and calls WatchFaceService.onCreateEngine.
     * @since API version 1.
     */
    IInteractiveWatchFaceWCS getOrCreateInteractiveWatchFaceWCS(
            in WallpaperInteractiveWatchFaceInstanceParams params,
            in IPendingInteractiveWatchFaceWCS callback) = 4;
}
