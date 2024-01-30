/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.SurfaceControlViewHost.SurfacePackage;
import androidx.wear.watchface.control.data.WatchFaceRenderParams;

/**
 * Interface for rendering into a remove view.
 *
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IRemoteWatchFaceView {
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
     * @since API version 1.
     */
    oneway void renderWatchFace(in WatchFaceRenderParams params) = 2;

    /**
     * @since API version 1.
     */
    SurfacePackage getSurfacePackage() = 3;

    /**
     * @since API version 1.
     */
    oneway void close() = 4;
}
