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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteShapes(
    public val extraSmall: RemoteCornerBasedShape = RemoteShapeDefaults.ExtraSmall,
    public val small: RemoteCornerBasedShape = RemoteShapeDefaults.Small,
    public val medium: RemoteCornerBasedShape = RemoteShapeDefaults.Medium,
    public val large: RemoteCornerBasedShape = RemoteShapeDefaults.Large,
    public val extraLarge: RemoteCornerBasedShape = RemoteShapeDefaults.ExtraLarge,
) {
    public fun copy(
        extraSmall: RemoteCornerBasedShape = this.extraSmall,
        small: RemoteCornerBasedShape = this.small,
        medium: RemoteCornerBasedShape = this.medium,
        large: RemoteCornerBasedShape = this.large,
        extraLarge: RemoteCornerBasedShape = this.extraLarge,
    ): RemoteShapes =
        RemoteShapes(
            extraSmall = extraSmall,
            small = small,
            medium = medium,
            large = large,
            extraLarge = extraLarge,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteShapes) return false
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
        return "RemoteShapes(" +
            "extraSmall=$extraSmall, " +
            "small=$small, " +
            "medium=$medium, " +
            "large=$large, " +
            "extraLarge=$extraLarge)"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteShapeDefaults {

    public val ExtraSmall: RemoteRoundedCornerShape = RemoteRoundedCornerShape(4.rdp)

    public val Small: RemoteRoundedCornerShape = RemoteRoundedCornerShape(8.rdp)

    public val Medium: RemoteRoundedCornerShape = RemoteRoundedCornerShape(18.rdp)

    public val Large: RemoteRoundedCornerShape = RemoteRoundedCornerShape(26.rdp)

    public val ExtraLarge: RemoteRoundedCornerShape = RemoteRoundedCornerShape(36.rdp)
}

internal val LocalRemoteShapes = staticCompositionLocalOf { RemoteShapes() }
