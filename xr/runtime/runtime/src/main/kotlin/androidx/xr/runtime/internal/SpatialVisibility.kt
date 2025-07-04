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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Spatial Visibility states of content within the user's field of view. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatialVisibility(@SpatialVisibilityValue public val visibility: Int) {
    public companion object {
        /** Unknown spatial visibility state. */
        public const val UNKNOWN: Int = 0
        /** The content is fully outside the user's field of view. */
        public const val OUTSIDE_FOV: Int = 1
        /** The content is partially within the user's field of view, but not fully inside of it. */
        public const val PARTIALLY_WITHIN_FOV: Int = 2
        /** The content is fully within the user's field of view. */
        public const val WITHIN_FOV: Int = 3
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialVisibility) return false
        if (visibility != other.visibility) return false

        return true
    }

    override fun hashCode(): Int {
        return visibility
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(UNKNOWN, OUTSIDE_FOV, PARTIALLY_WITHIN_FOV, WITHIN_FOV)
    internal annotation class SpatialVisibilityValue
}
