/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo

/** Provide the rendering implementation for [SpatialEnvironment] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialEnvironmentFeature {
    /**
     * The preferred spatial environment for the application.
     *
     * If no preference has ever been set by the application, this will be null.
     *
     * Setting this property only sets the preference and does not cause an immediate change unless
     * [SpatialEnvironment.isPreferredSpatialEnvironmentActive] is already true. Once the device
     * enters a state where the XR background can be changed and the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT] capability is available, the
     * preferred spatial environment for the application will be automatically displayed.
     *
     * Setting the preference to null will disable the preferred spatial environment for the
     * application, meaning the default system environment will be displayed instead.
     *
     * If the given [SpatialEnvironment.SpatialEnvironmentPreference] is not null, but all of its
     * properties are null, then the spatial environment will consist of a black skybox and no
     * geometry.
     */
    public var preferredSpatialEnvironment: SpatialEnvironment.SpatialEnvironmentPreference?

    /** Clean up any resources used by this feature. */
    public fun dispose()
}
