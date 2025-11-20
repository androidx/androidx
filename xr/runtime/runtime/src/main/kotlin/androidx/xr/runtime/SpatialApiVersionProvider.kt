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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/**
 * Provides the version of Spatial APIs available to Jetpack XR at runtime.
 *
 * This is a service provider interface that can be implemented by different XR runtimes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialApiVersionProvider {
    /**
     * The version of the stable Spatial APIs available on the device.
     *
     * This version number increments for breaking changes in the stable API surface.
     */
    public val spatialApiVersion: Int
    /**
     * The version of the preview Spatial APIs available on the device.
     *
     * This version number increments for breaking changes in the preview API surface.
     */
    public val previewSpatialApiVersion: Int
}
