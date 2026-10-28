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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.wear.compose.material3.Shapes

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container, offering a range of curved shapes (mostly
 * polygonal). The default [Shapes] theme for Material3 is rounded rectangles, with various degrees
 * of corner roundness:
 * - Extra Small
 * - Small
 * - Medium
 * - Large
 * - Extra Large
 *
 * You can customize the shape system for all components in the [RemoteMaterialTheme] or you can do
 * it on a per component basis by overriding the shape parameter for that component. For example, by
 * default, buttons use the shape style "large". If your product requires a smaller amount of
 * roundness, you can override the shape parameter with a different shape value like
 * [RemoteShapes.small].
 *
 * @param extraSmall By default, provides [RemoteShapeDefaults.ExtraSmall], a
 *   [RemoteCornerBasedShape] with 4dp [RemoteCornerSize] (used by bundled Cards).
 * @param small By default, provides [RemoteShapeDefaults.Small], a [RemoteCornerBasedShape] with
 *   8dp [RemoteCornerSize].
 * @param medium By default, provides [RemoteShapeDefaults.Medium], a [RemoteCornerBasedShape] with
 *   18dp [RemoteCornerSize] (used by shape-shifting Buttons and rounded rectangle buttons).
 * @param large By default, provides [RemoteShapeDefaults.Large], a [RemoteCornerBasedShape] with
 *   26dp [RemoteCornerSize] (used by Cards).
 * @param extraLarge By default, provides [RemoteShapeDefaults.ExtraLarge], a
 *   [RemoteCornerBasedShape] with 36dp [RemoteCornerSize].
 */
@Immutable
public class RemoteShapes(
    public val extraSmall: RemoteCornerBasedShape = RemoteShapeDefaults.ExtraSmall,
    public val small: RemoteCornerBasedShape = RemoteShapeDefaults.Small,
    public val medium: RemoteCornerBasedShape = RemoteShapeDefaults.Medium,
    public val large: RemoteCornerBasedShape = RemoteShapeDefaults.Large,
    public val extraLarge: RemoteCornerBasedShape = RemoteShapeDefaults.ExtraLarge,
) {
    /** Returns a copy of this [RemoteShapes], optionally overriding some of the values. */
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

/** Contains the default values used by [RemoteShapes] */
public object RemoteShapeDefaults {

    /** Extra small sized corner shape */
    public val ExtraSmall: RemoteRoundedCornerShape = RemoteRoundedCornerShape(4.rdp)

    /** Small sized corner shape */
    public val Small: RemoteRoundedCornerShape = RemoteRoundedCornerShape(8.rdp)

    /** Medium sized corner shape */
    public val Medium: RemoteRoundedCornerShape = RemoteRoundedCornerShape(18.rdp)

    /** Large sized corner shape */
    public val Large: RemoteRoundedCornerShape = RemoteRoundedCornerShape(26.rdp)

    /** Extra large sized corner shape */
    public val ExtraLarge: RemoteRoundedCornerShape = RemoteRoundedCornerShape(36.rdp)
}

internal val LocalRemoteShapes = staticCompositionLocalOf { RemoteShapes() }
