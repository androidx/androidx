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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/** Scaling rule used when drawing content into a destination rectangle. */
@Immutable
@JvmInline
public value class RemoteContentScale internal constructor(internal val value: Int) {
    override fun toString(): String =
        when (this) {
            Crop -> "Crop"
            Fit -> "Fit"
            FillHeight -> "FillHeight"
            FillWidth -> "FillWidth"
            Inside -> "Inside"
            None -> "None"
            FillBounds -> "FillBounds"
            else -> "Unknown($value)"
        }

    public companion object {
        /**
         * Scale the source uniformly (maintaining aspect ratio) so that both dimensions are equal
         * to or larger than the corresponding dimension of the destination.
         */
        public val Crop: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_CROP)

        /**
         * Scale the source uniformly (maintaining aspect ratio) so that both dimensions are equal
         * to or less than the corresponding dimension of the destination.
         */
        public val Fit: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_FIT)

        /**
         * Scale the source uniformly (maintaining aspect ratio) so that the height matches the
         * destination height.
         */
        public val FillHeight: RemoteContentScale =
            RemoteContentScale(ImageScaling.SCALE_FILL_HEIGHT)

        /**
         * Scale the source uniformly (maintaining aspect ratio) so that the width matches the
         * destination width.
         */
        public val FillWidth: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_FILL_WIDTH)

        /**
         * Scale the source to fit inside the destination bounds if either dimension is larger than
         * the destination. If the source is smaller than the destination in both dimensions,
         * drawing occurs without scaling.
         */
        public val Inside: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_INSIDE)

        /** Do not apply any scaling to the source. */
        public val None: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_NONE)

        /** Scale the source non-uniformly to match the destination bounds. */
        public val FillBounds: RemoteContentScale =
            RemoteContentScale(ImageScaling.SCALE_FILL_BOUNDS)
    }
}
