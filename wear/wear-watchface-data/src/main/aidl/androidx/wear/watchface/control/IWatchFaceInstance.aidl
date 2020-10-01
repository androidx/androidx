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

import android.view.Surface;
import android.support.wearable.watchface.IWatchFaceService;

/**
 * Interface for interacting with an instance of a watch face.
 *
 * @hide
 */
interface IWatchFaceInstance {
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
     * Initializes the watch face instance without a surface. It will only render screen shots (of
     * the specified size) upon request.
     *
     * @param width The desired width for screenshots.
     * @param height The desired height for screenshots.
     * @since API version 1.
     */
    oneway void initWithoutSurface(
        in IWatchFaceService iWatchFaceService,
        in int width,
        in int height) = 2;

    /**
     * Initializes the watch face instance with a surface into which it will render asynchronously.
     *
     * @param format The PixelFormat of the surface.
     * @param width The width of the surface.
     * @param height The height of the surface.
     * @since API version 1.
     */
    oneway void initWithSurface(
        in IWatchFaceService iWatchFaceService,
        in Surface surface,
        in int format,
        in int width,
        in int height) = 3;

    /**
     * Notifies the watch face that the {@link Surface} on which it is rendering has changed.
     *
     * <p>This will only be called if the watch face was initialized with {@link #initWithSurface}.
     *
     * @since API version 1.
     */
    oneway void onSurfaceChanged(
        in Surface surface,
        in int format,
        in int width,
        in int height) = 4;

    /**
     * Destroys the watch face instance.  It is an error to issue any further commands on any AIDLs
     * associated with this watch face instance.
     *
     * @since API version 1.
     */
    oneway void destroy() = 5;
}