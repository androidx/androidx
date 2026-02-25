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
 * Provides the Spatial API version that the device supports.
 *
 * This object queries the underlying XR platform to determine which Spatial API versions are
 * supported. It loads all available [SpatialApiVersionProvider] implementations and reports the
 * highest version number found.
 */
public object SpatialApiVersionHelper {
    /**
     * A list of well-known [SpatialApiVersionProvider] implementations.
     *
     * This list includes both real and fake (for testing) providers. The system does not
     * disambiguate between them, allowing tests to override the reported version.
     */
    private val PROVIDERS =
        listOf(
            "androidx.xr.scenecore.spatial.core.SpatialCoreApiVersionProvider",
            "androidx.xr.runtime.testing.FakeSpatialApiVersionProvider",
        )

    /** Lazily loads all available [SpatialApiVersionProvider]s from the [PROVIDERS] list. */
    private val providers by lazy {
        loadProviders(SpatialApiVersionProvider::class.java, PROVIDERS)
    }

    /**
     * The version of the Android XR Spatial APIs available to Jetpack XR at runtime.
     *
     * If the Spatial APIs are available, this value will be one of the constants declared in
     * [SpatialApiVersions].
     *
     * @throws IllegalStateException if no [SpatialApiVersionProvider] service implementation is
     *   found.
     */
    @JvmStatic
    public val spatialApiVersion: Int
        get() =
            providers.maxOfOrNull { it.spatialApiVersion }
                ?: throw IllegalStateException(
                    "Required SpatialApiVersionProvider service not found. " +
                        "Please ensure an implementation is included in the classpath."
                )

    /**
     * The version of the Android XR Preview Spatial APIs available at runtime.
     *
     * This is intended for internal library use to handle preview features and should not be relied
     * upon by applications.
     *
     * @throws IllegalStateException if no [SpatialApiVersionProvider] service implementation is
     *   found.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @JvmStatic
    public val previewSpatialApiVersion: Int
        get() =
            providers.maxOfOrNull { it.previewSpatialApiVersion }
                ?: throw IllegalStateException(
                    "Required SpatialApiVersionProvider service not found. " +
                        "Please ensure an implementation is included in the classpath."
                )
}
