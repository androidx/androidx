/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing

import androidx.xr.scenecore.SpatialWindow
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime

/** A test data accessor for the [SpatialWindow]. */
public class SpatialWindowTester internal constructor() {

    internal companion object {
        internal val instance: SpatialWindowTester = SpatialWindowTester()
    }

    /**
     * The preferred main panel aspect ratio determined by taking the panel's width over its height.
     *
     * This reflects the preference set via [SpatialWindow.setPreferredAspectRatio].
     *
     * [SpatialWindow.NO_PREFERRED_ASPECT_RATIO] indicates that there are no preferences for Home
     * Space Mode.
     */
    public val preferredAspectRatio: Float
        get() = checkNotNull(FakeSceneRuntime.instance).lastSetPreferredAspectRatio
}
