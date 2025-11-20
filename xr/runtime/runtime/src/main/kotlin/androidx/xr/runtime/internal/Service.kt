/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/**
 * A class that represents a service that can be loaded dynamically. Each implementation of this
 * interface may require different features that the device must support in order to be loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Service {

    /** The set of features that this service requires. */
    public val requirements: Set<Feature>
}

/** The set of features that a service may require. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Feature private constructor(private val value: Int) {
    public companion object {
        /** The Android platform is running normally (i.e. not Robolectric). */
        @JvmField public val FULLSTACK: Feature = Feature(0)

        /** The device supports OpenXR. */
        @JvmField public val OPEN_XR: Feature = Feature(1)

        /** The device supports Android XR Spatial APIs. */
        @JvmField public val SPATIAL: Feature = Feature(2)

        /** The device supports Android XR Projected APIs. */
        @JvmField public val PROJECTED: Feature = Feature(3)
    }

    override fun toString(): String =
        when (this) {
            FULLSTACK -> "FULLSTACK"
            OPEN_XR -> "OPEN_XR"
            SPATIAL -> "SPATIAL"
            PROJECTED -> "PROJECTED"
            else -> "UNKNOWN"
        }
}
