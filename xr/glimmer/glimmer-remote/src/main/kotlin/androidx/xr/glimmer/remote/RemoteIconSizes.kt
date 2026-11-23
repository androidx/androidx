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
 * A set of named icon sizes.
 *
 * Sizes can be provided using [LocalRemoteIconSize] to set the size for [RemoteIcon]s within a
 * component, or they can be set explicitly using
 * [androidx.compose.remote.creation.compose.modifier.size].
 *
 * @property small the size of a small icon.
 * @property medium the size of a medium icon. This is the default icon size.
 * @property large the size of a large icon.
 * @see RemoteIcon
 * @see LocalRemoteIconSize
 */
@Immutable
public class RemoteIconSizes(
    public val small: RemoteDp = 32.rdp,
    public val medium: RemoteDp = 40.rdp,
    public val large: RemoteDp = 48.rdp,
) {

    /** Returns a copy of this RemoteIconSizes, optionally overriding some of the values. */
    public fun copy(
        small: RemoteDp = this.small,
        medium: RemoteDp = this.medium,
        large: RemoteDp = this.large,
    ): RemoteIconSizes = RemoteIconSizes(small = small, medium = medium, large = large)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteIconSizes) return false

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
        return "RemoteIconSizes(small=$small, medium=$medium, large=$large)"
    }
}
