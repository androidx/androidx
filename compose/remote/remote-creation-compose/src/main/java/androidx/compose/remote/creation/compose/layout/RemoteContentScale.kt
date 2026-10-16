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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.runtime.Immutable

/**
 * Represents the scaling rule to use when drawing an image or painter into a destination rectangle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@kotlin.jvm.JvmInline
public value class RemoteContentScale internal constructor(public val value: Int) {
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
        public val Crop: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_CROP)
        public val Fit: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_FIT)
        public val FillHeight: RemoteContentScale =
            RemoteContentScale(ImageScaling.SCALE_FILL_HEIGHT)
        public val FillWidth: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_FILL_WIDTH)
        public val Inside: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_INSIDE)
        public val None: RemoteContentScale = RemoteContentScale(ImageScaling.SCALE_NONE)
        public val FillBounds: RemoteContentScale =
            RemoteContentScale(ImageScaling.SCALE_FILL_BOUNDS)
    }
}
