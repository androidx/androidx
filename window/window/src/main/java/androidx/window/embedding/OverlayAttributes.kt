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

package androidx.window.embedding

import androidx.annotation.RestrictTo

/**
 * The attributes to describe how an overlay container should look like.
 *
 * @constructor creates an overlay attributes.
 * @property bounds The overlay container's [EmbeddingBounds], which defaults to
 *   [EmbeddingBounds.BOUNDS_EXPANDED] if not specified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverlayAttributes
@JvmOverloads
constructor(val bounds: EmbeddingBounds = EmbeddingBounds.BOUNDS_EXPANDED) {

    override fun toString(): String =
        "${OverlayAttributes::class.java.simpleName}: {" + "bounds=$bounds" + "}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayAttributes) return false
        return bounds == other.bounds
    }

    override fun hashCode(): Int = bounds.hashCode()

    /** The [OverlayAttributes] builder. */
    class Builder {

        private var bounds = EmbeddingBounds.BOUNDS_EXPANDED

        /**
         * Sets the overlay bounds, which defaults to [EmbeddingBounds.BOUNDS_EXPANDED] if not
         * specified.
         *
         * @param bounds The [EmbeddingBounds] of the overlay [ActivityStack].
         * @return The [OverlayAttributes] builder.
         */
        fun setBounds(bounds: EmbeddingBounds): Builder = apply { this.bounds = bounds }

        /** Builds [OverlayAttributes]. */
        fun build(): OverlayAttributes = OverlayAttributes(bounds)
    }
}
