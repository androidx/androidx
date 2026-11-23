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

import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Immutable

/**
 * A set of named component spacing values for [RemoteGlimmerTheme].
 *
 * These values are used to ensure consistent spacing across Remote Compose Glimmer components. This
 * includes component paddings, spacing between components, and other spacing elements.
 *
 * @property extraSmall The extra small spacing, typically used for very tight visual coupling.
 *   Example: The space between a leading icon and its adjacent text inside a chip or button.
 * @property small The small spacing, typically used for close relationships within components.
 *   Example: The vertical space between a primary title and a secondary subtitle within a layout.
 * @property medium The medium spacing, typically used as the standard default padding. Example: The
 *   internal padding for a card, or the spacing between related components.
 * @property large The large spacing, used for main structural padding or looser component grouping.
 *   Example: The primary edge padding inside a list item or a large button.
 * @property extraLarge The extra large spacing, used to separate distinct content blocks. Example:
 *   The spacing between items in a scrolling list, or between independent components.
 */
@Immutable
public class RemoteComponentSpacingValues(
    public val extraSmall: RemoteDp = 6.rdp,
    public val small: RemoteDp = 8.rdp,
    public val medium: RemoteDp = 12.rdp,
    public val large: RemoteDp = 16.rdp,
    public val extraLarge: RemoteDp = 20.rdp,
) {

    /**
     * Returns a copy of this [RemoteComponentSpacingValues], optionally overriding some of the
     * values.
     */
    public fun copy(
        extraSmall: RemoteDp = this.extraSmall,
        small: RemoteDp = this.small,
        medium: RemoteDp = this.medium,
        large: RemoteDp = this.large,
        extraLarge: RemoteDp = this.extraLarge,
    ): RemoteComponentSpacingValues =
        RemoteComponentSpacingValues(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteComponentSpacingValues) return false

        if (extraSmall != other.extraSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (extraLarge != other.extraLarge) return false

        return true
    }

    override fun hashCode(): Int {
        var result = extraSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + extraLarge.hashCode()
        return result
    }

    override fun toString(): String {
        return "RemoteComponentSpacingValues(" +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "extraLarge=$extraLarge" +
            ")"
    }
}
