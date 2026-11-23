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

package androidx.xr.glimmer.remote

import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Immutable

/**
 * Remote Compose Glimmer surfaces can use different shapes. Shapes contains different levels of
 * roundedness for different components.
 *
 * @property small a shape with 4 same-sized corners whose size are smaller than [medium]. This
 *   shape is used for components such as cards.
 * @property medium a shape with 4 same-sized corners whose size are bigger than [small] and smaller
 *   than [large]. This is the most commonly used shape, and is used in [surface] by default.
 * @property large a shape with 4 fully rounded corners. This shape is used for components such as
 *   buttons.
 * @see surface
 */
@Immutable
public class RemoteShapes(
    public val small: RemoteShape = RemoteRoundedCornerShape(24.rdp),
    public val medium: RemoteShape = RemoteRoundedCornerShape(36.rdp),
    public val large: RemoteShape = RemoteCircleShape,
) {

    /** Returns a copy of this RemoteShapes, optionally overriding some of the values. */
    public fun copy(
        small: RemoteShape = this.small,
        medium: RemoteShape = this.medium,
        large: RemoteShape = this.large,
    ): RemoteShapes = RemoteShapes(small = small, medium = medium, large = large)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteShapes) return false

        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false

        return true
    }

    override fun hashCode(): Int {
        var result = small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        return result
    }

    override fun toString(): String {
        return "RemoteShapes(small=$small, medium=$medium, large=$large)"
    }
}
