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

package androidx.xr.scenecore

/** Spatial Visibility states of content within the user's field of view. */
public class SpatialVisibility private constructor(private val name: String) {

    public companion object {
        /** Unknown spatial visibility state. */
        @JvmField public val UNKNOWN: SpatialVisibility = SpatialVisibility("UNKNOWN")

        /** The content is fully outside the user's field of view. */
        @JvmField
        public val OUTSIDE_FIELD_OF_VIEW: SpatialVisibility =
            SpatialVisibility("OUTSIDE_FIELD_OF_VIEW")

        /** The content is partially within the user's field of view, but not fully inside of it. */
        @JvmField
        public val PARTIALLY_WITHIN_FIELD_OF_VIEW: SpatialVisibility =
            SpatialVisibility("PARTIALLY_WITHIN_FIELD_OF_VIEW")

        /** The content is fully within the user's field of view. */
        @JvmField
        public val WITHIN_FIELD_OF_VIEW: SpatialVisibility =
            SpatialVisibility("WITHIN_FIELD_OF_VIEW")
    }

    override fun toString(): String = name
}
