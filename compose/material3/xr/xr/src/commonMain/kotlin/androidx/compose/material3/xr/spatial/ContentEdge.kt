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

package androidx.compose.material3.xr.spatial

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

@RestrictTo(LIBRARY_GROUP)
public sealed interface ContentEdge {
    public class Horizontal private constructor(private val displayName: String) : ContentEdge {
        public companion object {
            /** Positioning constant to place an orbiter above the content's top edge. */
            public val Top: Horizontal = Horizontal("Top")
            /** Positioning constant to place an orbiter below the content's bottom edge. */
            public val Bottom: Horizontal = Horizontal("Bottom")
        }

        /** Returns the string representation of the edge. */
        override fun toString(): String {
            return displayName
        }
    }

    /** Represents vertical edges (start or end). */
    public class Vertical private constructor(private val displayName: String) : ContentEdge {
        public companion object {
            /**
             * Positioning constant to place an orbiter at the start of the content's starting edge.
             */
            public val Start: Vertical = Vertical("Start")
            /** Positioning constant to place an orbiter at the end of the content's ending edge. */
            public val End: Vertical = Vertical("End")
        }

        /** Returns the string representation of the edge. */
        override fun toString(): String {
            return displayName
        }
    }

    public companion object {
        /** The top edge. */
        public val Top: Horizontal = Horizontal.Top
        /** The bottom edge. */
        public val Bottom: Horizontal = Horizontal.Bottom
        /** The start edge. */
        public val Start: Vertical = Vertical.Start
        /** The end edge. */
        public val End: Vertical = Vertical.End
    }
}
