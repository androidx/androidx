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

package androidx.window.sidecar;

import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Contains information about the layout of display features within the window.
 * @deprecated Use androidx.window.extensions instead of this package.
 */
@Deprecated
public final class SidecarWindowLayoutInfo {

    /**
     * List of display features within the window.
     * <p>NOTE: All display features returned with this container must be cropped to the application
     * window and reported within the coordinate space of the window that was provided by the app.
     * @see SidecarInterface#getWindowLayoutInfo(IBinder)
     */
    @Nullable
    public List<SidecarDisplayFeature> displayFeatures;
}
